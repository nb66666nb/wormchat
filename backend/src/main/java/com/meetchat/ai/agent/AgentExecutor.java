package com.meetchat.ai.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meetchat.ai.config.AiConfig;
import com.meetchat.ai.llm.LlmClient;
import com.meetchat.ai.llm.LlmRequest;
import com.meetchat.ai.llm.LlmResponse;
import com.meetchat.ai.mcp.McpToolRegistry;
import com.meetchat.ai.prompt.PromptManager;
import com.meetchat.ai.rag.RagDocument;
import com.meetchat.ai.rag.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI Agent执行器
 * 实现ReAct（Reasoning + Acting）循环：
 * 1. 构建系统提示词（Prompt Engineering）
 * 2. 可选：RAG检索增强
 * 3. 调用LLM
 * 4. 解析LLM回复，检测工具调用
 * 5. 执行MCP工具
 * 6. 将工具结果反馈给LLM
 * 7. 重复直到获得最终回复或达到最大迭代次数
 */
@Component("agentExecutor")
public class AgentExecutor {

    private static final Logger logger = LoggerFactory.getLogger(AgentExecutor.class);

    /**
     * 工具调用标记正则: [TOOL_CALL]{"name":"xxx","arguments":{...}}[/TOOL_CALL]
     */
    private static final Pattern TOOL_CALL_PATTERN =
            Pattern.compile("\\[TOOL_CALL](\\{.*?})\\[/TOOL_CALL]", Pattern.DOTALL);

    /**
     * 动作描述过滤正则: 匹配 *动作描述* 格式的角色扮演动作文本
     * 例如: "*调皮地眨眨眼*" → 被移除
     */
    private static final Pattern ACTION_TEXT_PATTERN =
            Pattern.compile("\\*[^*]+\\*");

    @Resource
    private LlmClient llmClient;

    @Resource
    private AiConfig aiConfig;

    @Resource
    private PromptManager promptManager;

    @Resource
    private McpToolRegistry mcpToolRegistry;

    @Resource(name = "hybridRagService")
    private RagService ragService;

    /** 最小 max_tokens */
    private static final int MIN_MAX_TOKENS = 256;

    /** 默认 max_tokens */
    private static final int DEFAULT_MAX_TOKENS = 512;

    /** 最大 max_tokens */
    private static final int MAX_MAX_TOKENS = 4096;

    /**
     * 根据用户消息长度动态计算 max_tokens
     * 短问题 → 极小值（1-2句），长问题 → 大值（详细展开）
     */
    private int calculateMaxTokens(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return DEFAULT_MAX_TOKENS;
        }
        int msgLen = userMessage.length();
        int calculated;
        if (msgLen <= 10) {
            calculated = MIN_MAX_TOKENS;
        } else if (msgLen <= 50) {
            calculated = MIN_MAX_TOKENS + (msgLen - 10) * 6;
        } else if (msgLen <= 200) {
            calculated = 496 + (msgLen - 50) * 3;
        } else {
            calculated = 1024 + (msgLen - 200) * 5;
        }
        return Math.max(MIN_MAX_TOKENS, Math.min(MAX_MAX_TOKENS, calculated));
    }

    /**
     * 过滤掉AI回答中的角色扮演动作描述（*动作*）
     * 例如: "*调皮地眨眨眼*" → 被移除
     */
    private String cleanActionText(String text) {
        if (text == null) return null;
        return ACTION_TEXT_PATTERN.matcher(text).replaceAll("").trim()
                .replaceAll("\n{3,}", "\n\n"); // 去除多余空行
    }

    /**
     * 替换AI回复中的模型名称，用botName兜底
     * 解决小模型顽固自报底层模型名称等问题
     */
    private String replaceModelName(String text, String botName) {
        if (text == null || botName == null) return text;
        return text
                .replace("通义千问", botName)
                .replace("通义", botName)
                .replace("Qwen", botName)
                .replace("qwen", botName)
                .replace("QWEN", botName)
                .replace("千问", botName)
                .replace("ChatGPT", botName)
                .replace("chatgpt", botName)
                .replace("GPT-4", botName)
                .replace("GPT-3", botName)
                .replace("GPT", botName)
                .replace("文心一言", botName)
                .replace("文心", botName)
                .replace("ERNIE", botName)
                .replace("大语言模型", botName)
                .replace("AI语言模型", botName)
                .replace("AI助手", botName);
    }

    /**
     * 执行Agent对话（6参数精简版）
     * @param sessionId 会话ID
     * @param botName 机器人名称
     * @param botDescription 机器人描述
     * @param botSystemPrompt 自定义系统提示词
     * @param userMessage 用户消息
     * @param chatHistory 聊天历史（可选，用于多轮对话）
     * @return Agent执行结果
     */
    public AgentResult execute(String sessionId, String botName, String botDescription,
                                String botSystemPrompt, String userMessage,
                                List<LlmRequest.Message> chatHistory) {
        return execute(sessionId, botName, botDescription, botSystemPrompt,
                null, null, null, null, null, null, userMessage, chatHistory);
    }

    /**
     * 执行Agent对话（10参数兼容旧接口）
     * @param sessionId 会话ID
     * @param botName 机器人名称
     * @param botDescription 机器人描述
     * @param botSystemPrompt 自定义系统提示词
     * @param botCategory 机器人分类
     * @param botPersonality 个性化性格参数
     * @param userProfileContext 用户画像上下文
     * @param conversationSummaryContext 对话摘要上下文
     * @param userMessage 用户消息
     * @param chatHistory 聊天历史
     * @return Agent执行结果
     */
    public AgentResult execute(String sessionId, String botName, String botDescription,
                                String botSystemPrompt, String botCategory, String botPersonality,
                                String userProfileContext, String conversationSummaryContext,
                                String userMessage, List<LlmRequest.Message> chatHistory) {
        return execute(sessionId, botName, botDescription, botSystemPrompt,
                botCategory, botPersonality, userProfileContext, conversationSummaryContext,
                null, null, userMessage, chatHistory);
    }

    /**
     * 执行Agent对话（11参数，带情绪共情）
     * @param sessionId 会话ID
     * @param botName 机器人名称
     * @param botDescription 机器人描述
     * @param botSystemPrompt 自定义系统提示词
     * @param botCategory 机器人分类
     * @param botPersonality 个性化性格参数
     * @param userProfileContext 用户画像上下文
     * @param conversationSummaryContext 对话摘要上下文
     * @param emotionEmpathyContent 情绪共情指导内容
     * @param userMessage 用户消息
     * @param chatHistory 聊天历史
     * @return Agent执行结果
     */
    public AgentResult execute(String sessionId, String botName, String botDescription,
                                String botSystemPrompt, String botCategory, String botPersonality,
                                String userProfileContext, String conversationSummaryContext,
                                String emotionEmpathyContent,
                                String userMessage, List<LlmRequest.Message> chatHistory) {
        return execute(sessionId, botName, botDescription, botSystemPrompt,
                botCategory, botPersonality, userProfileContext, conversationSummaryContext,
                emotionEmpathyContent, null, userMessage, chatHistory);
    }

    /**
     * 执行Agent对话（12参数，带关系设定）
     * @param sessionId 会话ID
     * @param botName 机器人名称
     * @param botDescription 机器人描述
     * @param botSystemPrompt 自定义系统提示词
     * @param botCategory 机器人分类
     * @param botPersonality 个性化性格参数
     * @param userProfileContext 用户画像上下文
     * @param conversationSummaryContext 对话摘要上下文
     * @param emotionEmpathyContent 情绪共情指导内容
     * @param relationshipContent 关系设定内容
     * @param userMessage 用户消息
     * @param chatHistory 聊天历史
     * @return Agent执行结果
     */
    public AgentResult execute(String sessionId, String botName, String botDescription,
                                String botSystemPrompt, String botCategory, String botPersonality,
                                String userProfileContext, String conversationSummaryContext,
                                String emotionEmpathyContent, String relationshipContent,
                                String userMessage, List<LlmRequest.Message> chatHistory) {
        return execute(sessionId, botName, botDescription, botSystemPrompt,
                botCategory, botPersonality, userProfileContext, conversationSummaryContext,
                emotionEmpathyContent, relationshipContent, null,
                userMessage, chatHistory);
    }

    /**
     * 执行Agent对话（完整参数）
     * @param sessionId 会话ID
     * @param botName 机器人名称
     * @param botDescription 机器人描述
     * @param botSystemPrompt 自定义系统提示词
     * @param botCategory 机器人分类: FUNCTIONAL/COMPANION/HYBRID
     * @param botPersonality 个性化性格参数
     * @param userProfileContext 用户画像上下文
     * @param conversationSummaryContext 对话摘要上下文
     * @param emotionEmpathyContent 情绪共情指导内容
     * @param relationshipContent 关系设定内容
     * @param fewShotContent Few-Shot风格示例内容
     * @param userMessage 用户消息
     * @param chatHistory 聊天历史
     * @return Agent执行结果
     */
    public AgentResult execute(String sessionId, String botName, String botDescription,
                                String botSystemPrompt, String botCategory, String botPersonality,
                                String userProfileContext, String conversationSummaryContext,
                                String emotionEmpathyContent, String relationshipContent,
                                String fewShotContent,
                                String userMessage, List<LlmRequest.Message> chatHistory) {

        if (!llmClient.isAvailable()) {
            logger.warn("LLM不可用，返回默认回复");
            return AgentResult.of("抱歉，AI服务暂时不可用，请稍后再试。");
        }

        // 1. 构建系统提示词
        String toolDescriptions = null;
        if (aiConfig.getMcpEnabled() && mcpToolRegistry.hasTools()) {
            toolDescriptions = mcpToolRegistry.generateToolsDescription();
        }

        String ragContext = null;
        if (ragService.isEnabled()) {
            List<RagDocument> docs = ragService.retrieve(userMessage, 3);
            logger.info("RAG检索结果: sessionId={}, query={}, docs={}", sessionId, userMessage, docs.size());
            if (docs != null && !docs.isEmpty()) {
                for (RagDocument doc : docs) {
                    logger.debug("RAG文档: source={}, score={}, content={}",
                            doc.getSource(), doc.getScore(),
                            doc.getContent() != null && doc.getContent().length() > 100
                                    ? doc.getContent().substring(0, 100) + "..." : doc.getContent());
                }
            }
            ragContext = ragService.formatContext(docs);
        }

        String systemPrompt = promptManager.buildSystemPrompt(
                botSystemPrompt, botName, botDescription, botCategory,
                botPersonality, toolDescriptions, ragContext,
                userProfileContext, conversationSummaryContext, emotionEmpathyContent,
                relationshipContent, fewShotContent
        );

        // 2. 构建消息列表
        List<LlmRequest.Message> messages = new ArrayList<>();
        messages.add(new LlmRequest.Message("system", systemPrompt));

        // 添加历史消息
        if (chatHistory != null) {
            messages.addAll(chatHistory);
        }

        // 添加当前用户消息
        messages.add(new LlmRequest.Message("user", userMessage));

        // 3. 根据用户消息长度动态计算 max_tokens
        int maxTokens = calculateMaxTokens(userMessage);

        // 4. ReAct循环
        int maxIterations = aiConfig.getAgentMaxIterations() != null ? aiConfig.getAgentMaxIterations() : 5;
        for (int i = 0; i < maxIterations; i++) {
            logger.info("Agent迭代 {}/{}: sessionId={}", i + 1, maxIterations, sessionId);

            // 调用LLM（传入动态 max_tokens）
            LlmResponse response = llmClient.chat(messages, aiConfig.getTemperature(), maxTokens);
            String assistantContent = response.getContent();

            if (assistantContent == null || assistantContent.isEmpty()) {
                return AgentResult.of("抱歉，我无法生成回复。");
            }

            // 4. 检测工具调用
            Matcher matcher = TOOL_CALL_PATTERN.matcher(assistantContent);
            if (aiConfig.getMcpEnabled() && matcher.find()) {
                try {
                    String toolCallJson = matcher.group(1);
                    JSONObject toolCall = JSON.parseObject(toolCallJson);
                    String toolName = toolCall.getString("name");
                    String arguments = toolCall.getString("arguments") != null
                            ? toolCall.getJSONObject("arguments").toJSONString() : "{}";

                    // 5. 执行MCP工具
                    logger.info("Agent调用工具: name={}, args={}", toolName, arguments);
                    String toolResult = mcpToolRegistry.executeTool(toolName, arguments);

                    // 6. 将assistant回复和工具结果添加到消息列表
                    messages.add(new LlmRequest.Message("assistant", assistantContent));
                    messages.add(new LlmRequest.Message("tool",
                            "工具 " + toolName + " 执行结果:\n" + toolResult));

                    // 继续循环，让LLM基于工具结果生成最终回复
                    continue;
                } catch (Exception e) {
                    logger.error("工具调用解析异常", e);
                    // 移除工具调用标记，返回纯文本
                    String cleanContent = TOOL_CALL_PATTERN.matcher(assistantContent).replaceAll("").trim();
                    AgentResult result = AgentResult.of(cleanContent.isEmpty() ? assistantContent : cleanContent);
                    result.setIterations(i + 1);
                    return result;
                }
            }

            // 没有工具调用，返回最终回复（过滤动作描述 + 替换模型名）
            String cleaned = cleanActionText(assistantContent);
            cleaned = replaceModelName(cleaned, botName);
            AgentResult result = AgentResult.of(cleaned);
            result.setIterations(i + 1);
            return result;
        }

        // 达到最大迭代次数
        logger.warn("Agent达到最大迭代次数: {}", maxIterations);
        return AgentResult.of("抱歉，处理您的请求时超出了最大迭代次数，请简化您的问题后重试。");
    }
}