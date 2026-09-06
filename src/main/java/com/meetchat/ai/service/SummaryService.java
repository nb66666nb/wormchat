package com.meetchat.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meetchat.ai.llm.LlmClient;
import com.meetchat.ai.llm.LlmRequest;
import com.meetchat.ai.llm.LlmResponse;
import com.meetchat.entity.query.ChatImMessageQuery;
import com.meetchat.entity.query.BotConversationSummaryQuery;
import com.meetchat.entity.po.BotConversationSummary;
import com.meetchat.entity.po.ChatImMessage;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.redis.RedisUtils;
import com.meetchat.service.BotConversationSummaryService;
import com.meetchat.service.ChatImMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 对话摘要服务
 * 负责对话摘要的加载、生成和缓存管理
 */
@Service("summaryService")
public class SummaryService {

    private static final Logger logger = LoggerFactory.getLogger(SummaryService.class);

    /** 对话摘要缓存Key前缀 */
    private static final String CACHE_KEY_SUMMARY = "ai:summary:";
    /** 对话摘要缓存过期时间（5分钟） */
    private static final long CACHE_TTL_SECONDS = 300;
    /** 对话摘要LLM分析时查询的消息条数 */
    private static final int SUMMARY_MESSAGE_LIMIT = 10;
    /** 单次摘要最大输出长度 */
    private static final int MAX_SUMMARY_LENGTH = 200;
    private static final int MAX_KEY_TOPICS_LENGTH = 100;
    private static final int MAX_EMOTION_TREND_LENGTH = 30;

    @Resource
    private BotConversationSummaryService botConversationSummaryService;

    @Resource
    private ChatImMessageService chatImMessageService;

    @Resource
    private LlmClient llmClient;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 加载对话摘要上下文（带缓存）
     */
    public String loadContext(String sessionId) {
        try {
            // 1. 尝试从缓存读取
            String cacheKey = CACHE_KEY_SUMMARY + sessionId;
            Object cached = redisUtils.get(cacheKey);
            if (cached instanceof String) {
                logger.debug("对话摘要缓存命中: sessionId={}", sessionId);
                return (String) cached;
            }

            // 2. 缓存未命中，查询数据库
            BotConversationSummaryQuery query = new BotConversationSummaryQuery();
            query.setSessionId(sessionId);
            query.setOrderBy("create_time desc");
            query.setPageNo(1);
            query.setPageSize(1);

            List<BotConversationSummary> summaries = botConversationSummaryService.findListByParam(query);
            if (summaries != null && !summaries.isEmpty()) {
                BotConversationSummary latest = summaries.get(0);
                StringBuilder sb = new StringBuilder();
                if (latest.getSummary() != null) {
                    sb.append(latest.getSummary());
                }
                if (latest.getKeyTopics() != null) {
                    sb.append(" 关键话题: ").append(latest.getKeyTopics());
                }
                if (latest.getEmotionTrend() != null) {
                    sb.append(" 情绪趋势: ").append(latest.getEmotionTrend());
                }
                String context = sb.toString();

                // 3. 写入缓存
                if (!context.isEmpty()) {
                    redisUtils.setex(cacheKey, context, CACHE_TTL_SECONDS);
                    logger.debug("对话摘要已缓存: sessionId={}", sessionId);
                }

                return context;
            }

            return null;
        } catch (Exception e) {
            logger.warn("加载对话摘要异常: sessionId={}", sessionId, e);
            return null;
        }
    }

    /**
     * 生成对话摘要（增量滚动模式）
     * 每次只取最近 N 条消息，结合上一次的摘要进行合并精炼，
     * 保证无论对话多长，摘要长度始终可控。
     */
    public void generate(String sessionId, String botId, String userId, String botName) {
        try {
            // 1. 查询最近消息（只取新增部分，避免重复总结）
            ChatImMessageQuery query = new ChatImMessageQuery();
            query.setSessionId(sessionId);
            query.setOrderBy("send_time desc");
            query.setPageNo(1);
            query.setPageSize(SUMMARY_MESSAGE_LIMIT);

            PaginationResultVO<ChatImMessage> result = chatImMessageService.findListByPage(query);
            List<ChatImMessage> messages = result.getList();

            if (messages == null || messages.isEmpty()) {
                return;
            }

            // 2. 加载已有摘要（用于增量合并）
            BotConversationSummary existing = findExistingSummary(sessionId);

            // 3. 拼接为对话文本
            String dialogText = buildDialogText(messages, botId).toString();

            // 4. 构建增量摘要 LLM 请求
            StringBuilder userPrompt = new StringBuilder();
            if (existing != null && existing.getSummary() != null && !existing.getSummary().isEmpty()) {
                userPrompt.append("【历史摘要】\n").append(existing.getSummary());
                if (existing.getKeyTopics() != null) {
                    userPrompt.append("\n历史关键话题：").append(existing.getKeyTopics());
                }
                if (existing.getEmotionTrend() != null) {
                    userPrompt.append("\n历史情绪趋势：").append(existing.getEmotionTrend());
                }
                userPrompt.append("\n\n");
            }
            userPrompt.append("【最新对话】\n").append(dialogText);
            userPrompt.append("\n\n请基于以上信息，输出增量对话摘要。如果有历史摘要，请与最新对话合并精炼；");
            userPrompt.append("如果没有历史摘要，直接总结最新对话。严格控制字数。");

            String systemPrompt = "你是一个对话分析助手。请基于用户提供的【历史摘要】和【最新对话】，生成增量对话摘要。" +
                    "如果有历史摘要，请合并精炼，保留核心信息；如果没有，直接总结最新对话。" +
                    "输出严格的JSON格式（不要包含其他内容，不要使用markdown代码块）：" +
                    "{\"summary\":\"对话摘要(不超过" + MAX_SUMMARY_LENGTH + "字)\"," +
                    "\"keyTopics\":\"关键话题1,关键话题2(不超过" + MAX_KEY_TOPICS_LENGTH + "字)\"," +
                    "\"emotionTrend\":\"情绪趋势(不超过" + MAX_EMOTION_TREND_LENGTH + "字)\"}";

            List<LlmRequest.Message> llmMessages = new ArrayList<>();
            llmMessages.add(new LlmRequest.Message("system", systemPrompt));
            llmMessages.add(new LlmRequest.Message("user", userPrompt.toString()));

            // 5. 调用LLM
            LlmResponse llmResponse = llmClient.chat(llmMessages);
            String responseContent = llmResponse.getContent();

            if (responseContent == null || responseContent.trim().isEmpty()) {
                logger.warn("对话摘要LLM返回为空: sessionId={}", sessionId);
                return;
            }

            // 6. 解析JSON
            String jsonStr = extractJson(responseContent);
            JSONObject summaryJson = JSON.parseObject(jsonStr);

            // 7. 构建摘要对象并限制长度
            BotConversationSummary summary = new BotConversationSummary();
            summary.setSessionId(sessionId);
            summary.setUserId(userId);
            summary.setBotId(botId);
            summary.setSummary(truncate(summaryJson.getString("summary"), MAX_SUMMARY_LENGTH));
            summary.setKeyTopics(truncate(summaryJson.getString("keyTopics"), MAX_KEY_TOPICS_LENGTH));
            summary.setEmotionTrend(truncate(summaryJson.getString("emotionTrend"), MAX_EMOTION_TREND_LENGTH));
            summary.setMessageCount(existing != null && existing.getMessageCount() != null
                    ? existing.getMessageCount() + messages.size() : messages.size());
            summary.setCreateTime(new Date());

            // 设置时间范围
            ChatImMessage newest = messages.get(0);
            ChatImMessage oldest = messages.get(messages.size() - 1);
            summary.setStartTime(existing != null && existing.getStartTime() != null
                    ? existing.getStartTime() : oldest.getSendTime());
            summary.setEndTime(newest.getSendTime());

            // 8. 同 session 只保留一条摘要：存在则更新，不存在则插入
            if (existing != null && existing.getId() != null) {
                summary.setId(existing.getId());
                botConversationSummaryService.updateBotConversationSummaryById(summary, existing.getId());
                logger.info("对话摘要更新成功: sessionId={}, totalMessages={}, summary={}",
                        sessionId, summary.getMessageCount(), summary.getSummary());
            } else {
                botConversationSummaryService.add(summary);
                logger.info("对话摘要生成成功: sessionId={}, messageCount={}, summary={}",
                        sessionId, messages.size(), summary.getSummary());
            }

            // 清除缓存
            clearCache(sessionId);

        } catch (Exception e) {
            logger.warn("生成对话摘要异常: sessionId={}", sessionId, e);
        }
    }

    /**
     * 查找已存在的会话摘要
     */
    private BotConversationSummary findExistingSummary(String sessionId) {
        try {
            BotConversationSummaryQuery query = new BotConversationSummaryQuery();
            query.setSessionId(sessionId);
            query.setOrderBy("create_time desc");
            query.setPageNo(1);
            query.setPageSize(1);
            List<BotConversationSummary> summaries = botConversationSummaryService.findListByParam(query);
            if (summaries != null && !summaries.isEmpty()) {
                return summaries.get(0);
            }
        } catch (Exception e) {
            logger.warn("查找历史摘要异常: sessionId={}", sessionId, e);
        }
        return null;
    }

    /**
     * 构建对话文本
     */
    private StringBuilder buildDialogText(List<ChatImMessage> messages, String botId) {
        StringBuilder dialogText = new StringBuilder();
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatImMessage msg = messages.get(i);
            String role = botId.equals(msg.getSendUserId()) ? "assistant" : "user";
            String content = msg.getMessageContent();
            if (content != null && content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            dialogText.append(role).append(": ").append(content).append("\n");
        }
        return dialogText;
    }

    /**
     * 从LLM回复中提取JSON
     */
    private String extractJson(String content) {
        if (content == null) {
            return "{}";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "");
        }
        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    /**
     * 清除对话摘要缓存
     */
    public void clearCache(String sessionId) {
        try {
            String cacheKey = CACHE_KEY_SUMMARY + sessionId;
            redisUtils.delete(cacheKey);
            logger.debug("对话摘要缓存已清除: sessionId={}", sessionId);
        } catch (Exception e) {
            logger.warn("清除对话摘要缓存异常: sessionId={}", sessionId, e);
        }
    }
}
