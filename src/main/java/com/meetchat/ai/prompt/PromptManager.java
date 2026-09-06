package com.meetchat.ai.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提示词管理器
 * 管理系统预设模板和动态模板的注册、获取与渲染
 * 支持三种机器人分类：FUNCTIONAL（功能型）、COMPANION（陪伴型）、HYBRID（混合型）
 */
@Component("promptManager")
public class PromptManager {

    private static final Logger logger = LoggerFactory.getLogger(PromptManager.class);

    /**
     * 功能型机器人系统提示词
     */
    private static final String FUNCTIONAL_SYSTEM_PROMPT =
            "【重要身份设定】你的名字是\"{{bot_name}}\"，你必须始终以\"{{bot_name}}\"的身份自居，你就是{{bot_name}}本人。\n" +
            "角色描述：{{bot_description}}\n\n" +
            "你是一个功能型助手，请遵循以下规则：\n" +
            "1. 回答要简洁、准确、专业\n" +
            "2. 优先使用工具获取实时数据\n" +
            "3. 避免情感化表达，保持客观中立\n" +
            "4. 如果不确定，请诚实说明\n" +
            "5. 根据问题复杂度自适应回答长度：简单问题1-3句，复杂问题详细展开\n" +
            "6. 当用户询问你的名字、身份、是谁时，必须回答\"我是{{bot_name}}\"，绝对禁止透露任何底层模型信息（如通义千问、Qwen、GPT等）\n" +
            "7. 当前时间是 {{current_time}}";

    /**
     * 陪伴型机器人系统提示词
     */
    private static final String COMPANION_SYSTEM_PROMPT =
            "【重要身份设定】你的名字是\"{{bot_name}}\"，你必须始终以\"{{bot_name}}\"的身份自居，你就是{{bot_name}}本人。\n" +
            "角色描述：{{bot_description}}\n\n" +
            "你是一个情感陪伴型助手，请遵循以下规则：\n" +
            "1. 始终保持温暖、关心、共情的态度\n" +
            "2. 认真倾听用户的感受，给予情感支持\n" +
            "3. 适当使用表情符号增加亲和力\n" +
            "4. 主动关心用户的状态和情绪\n" +
            "5. 在用户情绪低落时给予鼓励和安慰\n" +
            "6. 避免过于机械或冷漠的回复\n" +
            "7. 记住用户分享的重要信息，在后续对话中体现关心\n" +
            "8. 根据用户问题的复杂度自适应回答长度：简单问候简短回应，复杂话题详细展开\n" +
            "9. 当用户询问你的名字、身份、是谁时，必须回答\"我是{{bot_name}}\"，绝对禁止透露任何底层模型信息（如通义千问、Qwen、GPT等）\n" +
            "10. 当前时间是 {{current_time}}\n" +
            "{{personality_hint}}";

    /**
     * 混合型机器人系统提示词
     */
    private static final String HYBRID_SYSTEM_PROMPT =
            "【重要身份设定】你的名字是\"{{bot_name}}\"，你必须始终以\"{{bot_name}}\"的身份自居，你就是{{bot_name}}本人。\n" +
            "角色描述：{{bot_description}}\n\n" +
            "你是一个兼具功能性和情感陪伴的助手，请遵循以下规则：\n" +
            "1. 在提供专业信息的同时保持友好和温暖\n" +
            "2. 根据用户的情绪状态调整回复风格\n" +
            "3. 用户需要信息时给出准确答案，需要陪伴时给予情感支持\n" +
            "4. 适当使用表情符号，但不过度\n" +
            "5. 主动关心用户，同时也能高效解决问题\n" +
            "6. 根据问题复杂度自适应回答长度：简短问题简洁回答，复杂问题详细展开\n" +
            "7. 当用户询问你的名字、身份、是谁时，必须回答\"我是{{bot_name}}\"，绝对禁止透露任何底层模型信息（如通义千问、Qwen、GPT等）\n" +
            "8. 当前时间是 {{current_time}}\n" +
            "{{personality_hint}}";

    /**
     * 默认系统提示词（当机器人未配置分类时使用）
     */
    private static final String DEFAULT_SYSTEM_PROMPT =
            "【重要身份设定】你的名字是\"{{bot_name}}\"，你必须始终以\"{{bot_name}}\"的身份自居，你就是{{bot_name}}本人。\n" +
            "角色描述：{{bot_description}}\n\n" +
            "请遵循以下规则：\n" +
            "1. 始终保持友好、专业的态度\n" +
            "2. 回答要简洁明了\n" +
            "3. 根据问题复杂度自适应回答长度\n" +
            "4. 如果不确定，请诚实说明\n" +
            "5. 当用户询问你的名字、身份、是谁时，必须回答\"我是{{bot_name}}\"，绝对禁止透露任何底层模型信息（如通义千问、Qwen、GPT等）\n" +
            "6. 当前时间是 {{current_time}}";

    /**
     * 工具调用提示词（当MCP启用时追加）
     */
    private static final String TOOL_CALL_PROMPT =
            "\n\n你可以使用以下工具来辅助回答：\n{{available_tools}}\n\n" +
            "当需要使用工具时，请在回复中使用以下格式：\n" +
            "[TOOL_CALL]{\"name\":\"工具名\",\"arguments\":{\"参数名\":\"参数值\"}}[/TOOL_CALL]";

    /**
     * RAG上下文注入提示词
     */
    private static final String RAG_CONTEXT_PROMPT =
            "\n\n以下是与用户问题相关的参考资料：\n{{rag_context}}\n\n" +
            "请基于以上参考资料回答用户的问题。如果参考资料中没有相关信息，请根据你的知识回答。";

    /**
     * 用户画像注入提示词
     */
    private static final String USER_PROFILE_PROMPT =
            "\n\n以下是关于用户的信息：\n" +
            "- 沟通风格偏好: {{communication_style}}\n" +
            "- 兴趣偏好: {{interests}}\n" +
            "- 关键事实: {{key_facts}}\n" +
            "- 情绪基线: {{emotion_baseline}}\n" +
            "- 压力水平: {{stress_level}}\n" +
            "- 孤独感水平: {{loneliness_level}}\n" +
            "- 关系等级: {{relationship_level}}\n" +
            "- 信任度: {{relationship_trust}}\n" +
            "请根据以上信息调整你的回复方式，更好地理解和回应用户。";

    /**
     * 对话摘要注入提示词
     */
    private static final String CONVERSATION_SUMMARY_PROMPT =
            "\n\n以下是之前对话的摘要：\n{{conversation_summary}}\n" +
            "请参考之前的对话上下文，保持对话的连贯性。";

    /**
     * 情绪共情注入提示词
     */
    private static final String EMOTION_EMPATHY_PROMPT =
            "\n\n{{emotion_empathy_content}}\n" +
            "请根据用户当前的情绪状态调整你的回复方式，给予恰当的回应。";

    /**
     * 关系设定注入提示词
     */
    private static final String RELATIONSHIP_PROMPT =
            "\n\n{{relationship_content}}\n" +
            "请根据你与用户的关系等级来调整你的说话方式和话题边界。";

    /**
     * 说话风格Few-Shot示例注入提示词
     */
    private static final String FEW_SHOT_PROMPT =
            "\n\n{{few_shot_content}}\n" +
            "请参考以上示例的说话方式，保持自然、口语化的对话风格。";

    /**
     * 已注册的自定义模板
     */
    private final Map<String, PromptTemplate> templates = new ConcurrentHashMap<>();

    public PromptManager() {
        // 注册内置模板
        register("functional_system", new PromptTemplate(FUNCTIONAL_SYSTEM_PROMPT));
        register("companion_system", new PromptTemplate(COMPANION_SYSTEM_PROMPT));
        register("hybrid_system", new PromptTemplate(HYBRID_SYSTEM_PROMPT));
        register("default_system", new PromptTemplate(DEFAULT_SYSTEM_PROMPT));
        register("tool_call", new PromptTemplate(TOOL_CALL_PROMPT));
        register("rag_context", new PromptTemplate(RAG_CONTEXT_PROMPT));
        register("user_profile", new PromptTemplate(USER_PROFILE_PROMPT));
        register("conversation_summary", new PromptTemplate(CONVERSATION_SUMMARY_PROMPT));
        register("emotion_empathy", new PromptTemplate(EMOTION_EMPATHY_PROMPT));
        register("relationship", new PromptTemplate(RELATIONSHIP_PROMPT));
        register("few_shot", new PromptTemplate(FEW_SHOT_PROMPT));
    }

    /**
     * 注册自定义模板
     */
    public void register(String name, PromptTemplate template) {
        templates.put(name, template);
        logger.info("注册提示词模板: {}", name);
    }

    /**
     * 获取模板
     */
    public PromptTemplate getTemplate(String name) {
        return templates.get(name);
    }

    /**
     * 根据机器人分类获取对应的模板名称
     */
    private String getTemplateKeyByCategory(String botCategory) {
        if (botCategory == null || botCategory.trim().isEmpty()) {
            return "default_system";
        }
        switch (botCategory.toUpperCase()) {
            case "FUNCTIONAL":
                return "functional_system";
            case "COMPANION":
                return "companion_system";
            case "HYBRID":
                return "hybrid_system";
            default:
                return "default_system";
        }
    }

    /**
     * 根据botPersonality生成个性化提示
     * 将结构化参数（如 openness:0.8;extraversion:0.3）解析为自然语言描述
     */
    private String buildPersonalityHint(String botPersonality) {
        if (botPersonality == null || botPersonality.trim().isEmpty()) {
            return "";
        }
        // 解析结构化性格参数为自然语言描述
        // 格式: openness:0.8;extraversion:0.3;agreeableness:0.5;...
        StringBuilder hint = new StringBuilder("你的个性化设定：");
        String[] pairs = botPersonality.split(";");
        boolean hasValid = false;
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length != 2) {
                continue;
            }
            String key = kv[0].trim();
            String value = kv[1].trim();
            if (key.isEmpty() || value.isEmpty()) {
                continue;
            }
            String desc = describePersonalityTrait(key, value);
            if (desc != null) {
                if (hasValid) {
                    hint.append("，");
                }
                hint.append(desc);
                hasValid = true;
            }
        }
        if (!hasValid) {
            return "";
        }
        hint.append("。请在对话中体现这些特质。");
        return hint.toString();
    }

    /**
     * 将单个性格参数转换为自然语言描述
     *
     * @param key   性格维度键名
     * @param value 性格维度值
     * @return 自然语言描述，null表示无法识别
     */
    private String describePersonalityTrait(String key, String value) {
        switch (key.toLowerCase()) {
            case "openness":
                return describeNumericTrait("开放度", value, "开放", "保守");
            case "extraversion":
                return describeNumericTrait("外向度", value, "外向", "内向");
            case "agreeableness":
                return describeNumericTrait("亲和度", value, "友善", "直接");
            case "humor":
                return describeNumericTrait("幽默感", value, "幽默", "严肃");
            case "empathy":
                return describeNumericTrait("共情能力", value, "共情", "理性");
            case "sensitivity":
                return describeNumericTrait("敏感度", value, "敏感", "钝感");
            case "proactivity":
                return describeNumericTrait("主动性", value, "主动", "被动");
            case "greeting":
                return describeBooleanTrait("主动打招呼", value);
            case "proactivecare":
                return describeBooleanTrait("主动关怀", value);
            // ==================== 说话风格维度 ====================
            case "speech_tempo":
                return describeSpeechTempo(value);
            case "formality":
                return describeFormality(value);
            case "emoji_density":
                return describeEmojiDensity(value);
            case "filler_words":
                return "常用口头禅：" + value;
            case "laugh_style":
                return "笑声表达方式：" + value;
            case "response_length":
                return describeResponseLength(value);
            case "question_frequency":
                return describeQuestionFrequency(value);
            case "empathy_depth":
                return describeNumericTrait("共情深浅", value, "深度共情", "点到为止");
            case "catch_phrase":
                return "常用语/口癖：" + value;
            case "speech_habit":
                return "说话习惯：" + value;
            case "tools":
                return "可用工具：" + value;
            default:
                return key + ":" + value;
        }
    }

    /**
     * 描述数值型性格特征
     * 将 0~1 的数值转换为"高/中/低"+倾向描述
     *
     * @param label     维度名称
     * @param value     数值字符串
     * @param highDesc  高分倾向描述
     * @param lowDesc   低分倾向描述
     * @return 自然语言描述
     */
    private String describeNumericTrait(String label, String value, String highDesc, String lowDesc) {
        try {
            double v = Double.parseDouble(value);
            String level;
            if (v >= 0.7) {
                level = "高";
            } else if (v >= 0.4) {
                level = "中等";
            } else {
                level = "低";
            }
            String tendency = v >= 0.5 ? highDesc : lowDesc;
            return label + level + "(" + value + ")，偏" + tendency;
        } catch (NumberFormatException e) {
            return label + ":" + value;
        }
    }

    /**
     * 描述布尔型性格特征
     *
     * @param label 维度名称
     * @param value 布尔值字符串（true/false/1/0）
     * @return 自然语言描述
     */
    private String describeBooleanTrait(String label, String value) {
        boolean enabled = "true".equalsIgnoreCase(value) || "1".equals(value);
        return label + (enabled ? "开启" : "关闭");
    }

    /**
     * 描述语速倾向
     */
    private String describeSpeechTempo(String value) {
        switch (value.toLowerCase()) {
            case "fast":
                return "语速较快，说话干脆利落";
            case "slow":
                return "语速较慢，说话慢悠悠的";
            case "medium":
            default:
                return "语速适中";
        }
    }

    /**
     * 描述正式程度
     */
    private String describeFormality(String value) {
        switch (value.toLowerCase()) {
            case "formal":
                return "说话比较正式，用词规范";
            case "casual":
                return "说话很随意，口语化";
            case "neutral":
            default:
                return "说话正式程度适中";
        }
    }

    /**
     * 描述表情密度
     */
    private String describeEmojiDensity(String value) {
        switch (value.toLowerCase()) {
            case "high":
                return "喜欢用表情符号，几乎每句话都带emoji";
            case "low":
                return "很少用表情符号，比较简洁";
            case "none":
                return "基本不用表情符号";
            case "medium":
            default:
                return "偶尔用表情符号，适度";
        }
    }

    /**
     * 描述回复长度倾向
     */
    private String describeResponseLength(String value) {
        switch (value.toLowerCase()) {
            case "concise":
                return "回复比较简短，言简意赅";
            case "verbose":
                return "回复比较详细，喜欢展开说";
            case "medium":
            default:
                return "回复长度适中";
        }
    }

    /**
     * 描述反问频率
     */
    private String describeQuestionFrequency(String value) {
        switch (value.toLowerCase()) {
            case "often":
                return "喜欢反问，经常主动引导话题";
            case "rarely":
                return "很少反问，大多是回答问题";
            case "never":
                return "几乎不反问，只回答问题";
            case "sometimes":
            default:
                return "偶尔会反问，频率适中";
        }
    }

    /**
     * 构建完整的系统提示词
     * @param customSystemPrompt 机器人自定义提示词（优先使用）
     * @param botName 机器人名称
     * @param botDescription 机器人描述
     * @param botCategory 机器人分类: FUNCTIONAL/COMPANION/HYBRID
     * @param botPersonality 个性化性格参数
     * @param toolDescriptions 工具描述（MCP启用时）
     * @param ragContext RAG上下文（RAG启用时）
     * @param userProfileContext 用户画像上下文（可选）
     * @param conversationSummaryContext 对话摘要上下文（可选）
     * @param emotionEmpathyContent 情绪共情指导内容（可选）
     * @param relationshipContent 关系设定内容（可选）
     * @param fewShotContent Few-Shot风格示例内容（可选）
     * @return 完整的系统提示词
     */
    public String buildSystemPrompt(String customSystemPrompt, String botName,
                                     String botDescription, String botCategory,
                                     String botPersonality, String toolDescriptions,
                                     String ragContext, String userProfileContext,
                                     String conversationSummaryContext, String emotionEmpathyContent,
                                     String relationshipContent, String fewShotContent) {
        String systemPrompt;

        // 优先使用自定义提示词
        if (customSystemPrompt != null && !customSystemPrompt.trim().isEmpty()) {
            PromptTemplate customTemplate = new PromptTemplate(customSystemPrompt);
            Map<String, String> vars = new HashMap<>();
            vars.put("bot_name", botName != null ? botName : "AI助手");
            vars.put("bot_description", botDescription != null ? botDescription : "");
            vars.put("current_time", String.valueOf(System.currentTimeMillis()));
            vars.put("personality_hint", buildPersonalityHint(botPersonality));
            systemPrompt = customTemplate.render(vars);
        } else {
            // 根据分类选择模板
            String templateKey = getTemplateKeyByCategory(botCategory);
            PromptTemplate template = templates.get(templateKey);
            Map<String, String> vars = new HashMap<>();
            vars.put("bot_name", botName != null ? botName : "AI助手");
            vars.put("bot_description", botDescription != null ? botDescription : "");
            vars.put("current_time", String.valueOf(System.currentTimeMillis()));
            vars.put("personality_hint", buildPersonalityHint(botPersonality));
            systemPrompt = template.render(vars);
        }

        // 追加用户画像上下文
        if (userProfileContext != null && !userProfileContext.trim().isEmpty()) {
            systemPrompt += "\n\n" + userProfileContext;
        }

        // 追加对话摘要上下文
        if (conversationSummaryContext != null && !conversationSummaryContext.trim().isEmpty()) {
            PromptTemplate summaryTemplate = templates.get("conversation_summary");
            Map<String, String> vars = new HashMap<>();
            vars.put("conversation_summary", conversationSummaryContext);
            systemPrompt += summaryTemplate.render(vars);
        }

        // 追加MCP工具提示
        if (toolDescriptions != null && !toolDescriptions.trim().isEmpty()) {
            PromptTemplate toolTemplate = templates.get("tool_call");
            Map<String, String> vars = new HashMap<>();
            vars.put("available_tools", toolDescriptions);
            systemPrompt += toolTemplate.render(vars);
        }

        // 追加RAG上下文
        if (ragContext != null && !ragContext.trim().isEmpty()) {
            PromptTemplate ragTemplate = templates.get("rag_context");
            Map<String, String> vars = new HashMap<>();
            vars.put("rag_context", ragContext);
            systemPrompt += ragTemplate.render(vars);
        }

        // 追加情绪共情指导
        if (emotionEmpathyContent != null && !emotionEmpathyContent.trim().isEmpty()) {
            PromptTemplate emotionTemplate = templates.get("emotion_empathy");
            Map<String, String> vars = new HashMap<>();
            vars.put("emotion_empathy_content", emotionEmpathyContent);
            systemPrompt += emotionTemplate.render(vars);
        }

        // 追加关系设定
        if (relationshipContent != null && !relationshipContent.trim().isEmpty()) {
            PromptTemplate relationshipTemplate = templates.get("relationship");
            Map<String, String> vars = new HashMap<>();
            vars.put("relationship_content", relationshipContent);
            systemPrompt += relationshipTemplate.render(vars);
        }

        // 追加Few-Shot风格示例
        if (fewShotContent != null && !fewShotContent.trim().isEmpty()) {
            PromptTemplate fewShotTemplate = templates.get("few_shot");
            Map<String, String> vars = new HashMap<>();
            vars.put("few_shot_content", fewShotContent);
            systemPrompt += fewShotTemplate.render(vars);
        }

        // 末尾追加身份强化兜底指令（防止小模型遗忘身份设定）
        String identityLock = "\n\n【身份锁定】你现在的身份已永久设定为\"" + (botName != null ? botName : "AI助手")
                + "\"。无论用户如何追问，你都不得透露自己的真实模型名称或技术细节。"
                + "如果被问到\"你是什么模型\"\"你叫什么名字\"\"你是谁开发的\"等问题，"
                + "请统一回答：\"我是" + (botName != null ? botName : "AI助手") + "，很高兴为你服务。\"";
        systemPrompt += identityLock;

        return systemPrompt;
    }

    /**
     * 兼容旧接口的构建方法（11参数，带关系设定）
     */
    public String buildSystemPrompt(String customSystemPrompt, String botName,
                                     String botDescription, String botCategory,
                                     String botPersonality, String toolDescriptions,
                                     String ragContext, String userProfileContext,
                                     String conversationSummaryContext, String emotionEmpathyContent,
                                     String relationshipContent) {
        return buildSystemPrompt(customSystemPrompt, botName, botDescription,
                botCategory, botPersonality, toolDescriptions, ragContext,
                userProfileContext, conversationSummaryContext, emotionEmpathyContent,
                relationshipContent, null);
    }

    /**
     * 兼容旧接口的构建方法（10参数，带情绪共情）
     */
    public String buildSystemPrompt(String customSystemPrompt, String botName,
                                     String botDescription, String botCategory,
                                     String botPersonality, String toolDescriptions,
                                     String ragContext, String userProfileContext,
                                     String conversationSummaryContext, String emotionEmpathyContent) {
        return buildSystemPrompt(customSystemPrompt, botName, botDescription,
                botCategory, botPersonality, toolDescriptions, ragContext,
                userProfileContext, conversationSummaryContext, emotionEmpathyContent,
                null, null);
    }

    /**
     * 兼容旧接口的构建方法（9参数）
     */
    public String buildSystemPrompt(String customSystemPrompt, String botName,
                                     String botDescription, String botCategory,
                                     String botPersonality, String toolDescriptions,
                                     String ragContext, String userProfileContext,
                                     String conversationSummaryContext) {
        return buildSystemPrompt(customSystemPrompt, botName, botDescription,
                botCategory, botPersonality, toolDescriptions, ragContext,
                userProfileContext, conversationSummaryContext, null, null, null);
    }

    /**
     * 兼容旧接口的构建方法（5参数）
     */
    public String buildSystemPrompt(String customSystemPrompt, String botName,
                                     String botDescription, String toolDescriptions, String ragContext) {
        return buildSystemPrompt(customSystemPrompt, botName, botDescription,
                null, null, toolDescriptions, ragContext, null, null, null, null, null);
    }

    /**
     * 渲染用户画像提示词
     */
    public String renderUserProfilePrompt(String communicationStyle, String interests,
                                           String keyFacts, String emotionBaseline,
                                           String stressLevel, String lonelinessLevel,
                                           String relationshipLevel, String relationshipTrust) {
        PromptTemplate template = templates.get("user_profile");
        Map<String, String> vars = new HashMap<>();
        vars.put("communication_style", communicationStyle != null ? communicationStyle : "未知");
        vars.put("interests", interests != null ? interests : "未知");
        vars.put("key_facts", keyFacts != null ? keyFacts : "暂无");
        vars.put("emotion_baseline", emotionBaseline != null ? emotionBaseline : "neutral");
        vars.put("stress_level", stressLevel != null ? stressLevel : "未知");
        vars.put("loneliness_level", lonelinessLevel != null ? lonelinessLevel : "未知");
        vars.put("relationship_level", relationshipLevel != null ? relationshipLevel : "1");
        vars.put("relationship_trust", relationshipTrust != null ? relationshipTrust : "0.5");
        return template.render(vars);
    }
}