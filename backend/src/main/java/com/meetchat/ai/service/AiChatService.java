package com.meetchat.ai.service;

import com.meetchat.ai.agent.AgentExecutor;
import com.meetchat.ai.agent.AgentResult;
import com.meetchat.ai.llm.LlmRequest;
import com.meetchat.ai.prompt.PromptManager;
import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.enums.MessageTypeImEnum;
import com.meetchat.entity.enums.MessageTypeEnum;
import com.meetchat.entity.enums.RelationshipLevelEnum;
import com.meetchat.entity.enums.UserContactTypeEnum;
import com.meetchat.entity.po.ChatImMessage;
import com.meetchat.entity.po.ChatSession;
import com.meetchat.entity.query.ChatImMessageQuery;
import com.meetchat.utils.JsonUtils;
import com.meetchat.ai.ratelimit.AiDegradation;
import com.meetchat.ai.ratelimit.AiRateLimiter;
import com.meetchat.redis.RedisUtils;
import com.meetchat.service.BotConversationSummaryService;
import com.meetchat.service.ChatImMessageService;
import com.meetchat.service.ChatSessionService;
import com.meetchat.webSocket.SessionManager;
import lombok.var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * AI聊天服务 - 对外统一入口（重构后）
 * 整合Agent、LLM、RAG、MCP、Prompt Engineering
 *
 * 职责：主流程编排，委托给子服务处理具体业务
 *
 * 调用流程:
 * 1. 用户发送消息 → ChatImMessageController
 * 2. 消息入库 → ChatImMessageService
 * 3. 判断是否为机器人会话 → AiChatService.handleBotChat
 *    3.1 获取会话信息（缓存）
 *    3.2 ProfileService加载用户画像
 *    3.3 SummaryService加载对话摘要
 *    3.4 加载聊天历史（缓存）
 * 4. Agent执行 → AgentExecutor (ReAct循环)
 * 5. AI回复入库 → ChatImMessageService
 * 6. 推送AI回复给用户 → SessionManager
 * 7. EmotionService记录情绪日志
 * 8. ProfileService更新用户画像
 * 9. 更新会话最后消息
 * 10. SummaryService定期生成对话摘要
 */
@Service("aiChatService")
public class AiChatService {

    private static final Logger logger = LoggerFactory.getLogger(AiChatService.class);

    /** 聊天历史最大条数 */
    private static final int CHAT_HISTORY_LIMIT = 20;

    /** 对话摘要生成阈值（每多少条消息触发一次） */
    private static final int SUMMARY_TRIGGER_INTERVAL = 10;

    /** 会话信息缓存Key前缀 */
    private static final String CACHE_KEY_SESSION = "ai:session:";
    /** 聊天历史缓存Key前缀 */
    private static final String CACHE_KEY_HISTORY = "ai:history:";
    /** 会话缓存过期时间（30分钟） */
    private static final long CACHE_TTL_SESSION_SECONDS = 1800;
    /** 聊天历史缓存过期时间（10分钟） */
    private static final long CACHE_TTL_HISTORY_SECONDS = 600;

    @Resource
    private AgentExecutor agentExecutor;

    @Resource
    private ChatSessionService chatSessionService;

    @Lazy
    @Autowired
    private ChatImMessageService chatImMessageService;

    @Resource
    private BotConversationSummaryService botConversationSummaryService;

    @Resource
    private PromptManager promptManager;

    @Resource
    private SessionManager sessionManager;

    @Resource
    private RedisUtils redisUtils;

    // ==================== 子服务依赖 ====================
    @Resource
    private ProfileService profileService;

    @Resource
    private SummaryService summaryService;

    @Resource
    private EmotionService emotionService;

    @Resource
    private EmpathyStrategyService empathyStrategyService;

    @Resource
    private RelationshipService relationshipService;

    @Resource
    private SpeechStyleService speechStyleService;

    @Resource
    private AiRateLimiter rateLimiter;

    @Resource
    private AiDegradation degradation;

    /**
     * 处理机器人对话（主流程编排）
     */
    public Long handleBotChat(String sessionId, String sendUserId, String sendUserName, String userMessage) {
        // 0. 限流检查（提前失败，避免不必要的资源消耗）
        String limitReply = degradation.checkUserMessageRateLimit(sendUserId);
        if (limitReply != null) {
            logger.warn("用户消息被限流: userId={}", sendUserId);
            return saveDegradedReply(sessionId, sendUserId, sendUserName, limitReply);
        }

        String concurrencyReply = degradation.checkAgentConcurrency(sessionId);
        if (concurrencyReply != null) {
            logger.warn("Agent并发被限流: sessionId={}", sessionId);
            return saveDegradedReply(sessionId, sendUserId, sendUserName, concurrencyReply);
        }

        String llmQpsReply = degradation.checkLlmQps();
        if (llmQpsReply != null) {
            logger.warn("LLM QPS被限流");
            return saveDegradedReply(sessionId, sendUserId, sendUserName, llmQpsReply);
        }

        // 1. 获取会话信息（带缓存）
        ChatSession session = getChatSessionWithCache(sessionId);
        if (session == null) {
            logger.error("会话不存在: {}", sessionId);
            return null;
        }

        String botName = session.getBotName();
        String botDescription = session.getBotDescription();
        String botSystemPrompt = session.getBotSystemPrompt();
        String botCategory = session.getBotCategory();
        String botPersonality = session.getBotPersonality();
        String botId = session.getTargetUserId();

        // 2. 加载用户画像（委托给ProfileService）
        String userProfileContext = profileService.loadUserProfileContext(sendUserId, botId);

        // 3. 加载对话摘要（委托给SummaryService）
        String conversationSummaryContext = summaryService.loadContext(sessionId);

        // 4. 加载聊天历史（带缓存）
        List<LlmRequest.Message> chatHistory = loadChatHistory(sessionId, botId, CHAT_HISTORY_LIMIT);

        // 4.5 分析用户情绪并生成共情指导
        EmotionAnalysisResult emotionResult = emotionService.analyzeEmotion(userMessage);
        String emotionEmpathyContent = empathyStrategyService.buildEmpathyPrompt(
                emotionResult, botCategory);
        logger.debug("情绪分析结果: emotion={}, intensity={}, empathyLen={}",
                emotionResult.getPrimaryEmotion(), emotionResult.getIntensity(),
                emotionEmpathyContent != null ? emotionEmpathyContent.length() : 0);

        // 4.6 生成关系设定
        String relationshipContent = relationshipService.buildRelationshipPrompt(
                sendUserId, botId, sendUserName);
        logger.debug("关系设定生成: userId={}, botId={}, len={}",
                sendUserId, botId,
                relationshipContent != null ? relationshipContent.length() : 0);

        // 4.7 检查是否有待触发的关系升级消息
        RelationshipLevelEnum currentLevel = relationshipService.getCurrentLevel(sendUserId, botId);
        boolean hasLevelUpPending = relationshipService.checkLevelUp(sendUserId, botId, currentLevel);
        if (hasLevelUpPending) {
            String levelUpMsg = relationshipService.buildLevelUpMessage(currentLevel, sendUserName);
            if (levelUpMsg != null && !levelUpMsg.isEmpty()) {
                relationshipContent += levelUpMsg;
                logger.info("触发关系升级消息: userId={}, botId={}, newLevel={}",
                        sendUserId, botId, currentLevel.getName());
            }
        }

        // 4.8 生成说话风格Few-Shot示例
        String fewShotContent = speechStyleService.buildFewShotExamples(
                botCategory, botPersonality, 3);
        logger.debug("Few-Shot示例生成: category={}, len={}",
                botCategory, fewShotContent != null ? fewShotContent.length() : 0);

        // 5. Agent执行
        AgentResult result = agentExecutor.execute(
                sessionId, botName, botDescription, botSystemPrompt,
                botCategory, botPersonality,
                userProfileContext, conversationSummaryContext,
                emotionEmpathyContent, relationshipContent, fewShotContent,
                userMessage, chatHistory
        );

        // 6. 将AI回复存入数据库
        ChatImMessage aiMessage = new ChatImMessage();
        aiMessage.setSessionId(sessionId);
        aiMessage.setSendUserId(botId);
        aiMessage.setSendUserName(botName);
        aiMessage.setReceiveUserId(sendUserId);
        aiMessage.setMessageType(30);
        aiMessage.setMessageContent(result.getContent());
        aiMessage.setMessageSendType(2);
        aiMessage.setStatus(1);
        aiMessage.setSendTime(System.currentTimeMillis());
        chatImMessageService.add(aiMessage);

        // AI回复入库后，聊天历史已变更，立即清除缓存
        clearHistoryCache(sessionId);

        // 6.1 推送AI回复给用户
        pushAiReplyToUser(sessionId, botId, botName, sendUserId, aiMessage, session);

        // 7. 记录情绪日志（委托给EmotionService）
        emotionService.recordLog(sendUserId, botId, sessionId, userMessage, aiMessage.getMessageId());

        // 8. 更新用户画像（委托给ProfileService）
        profileService.updateAfterChat(sendUserId, botId, sessionId, userMessage, result.getContent());

        // 8.1 更新信任度和关系等级
        boolean hasPersonalShare = detectPersonalShare(userMessage);
        relationshipService.updateTrustAfterChat(sendUserId, botId, emotionResult, hasPersonalShare);

        // 9. 更新会话最后消息
        updateSessionLastMessage(sessionId, result.getContent());

        // 10. 定期生成对话摘要
        triggerSummaryGeneration(sessionId, botId, sendUserId, botName);

        logger.info("AI回复完成: sessionId={}, messageId={}, iterations={}, category={}",
                sessionId, aiMessage.getMessageId(), result.getIterations(), botCategory);

        return aiMessage.getMessageId();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取会话信息（带缓存）
     */
    private ChatSession getChatSessionWithCache(String sessionId) {
        String cacheKey = CACHE_KEY_SESSION + sessionId;
        Object cached = redisUtils.get(cacheKey);
        if (cached instanceof ChatSession) {
            return (ChatSession) cached;
        }

        ChatSession session = chatSessionService.getChatSessionBySessionId(sessionId);
        if (session != null) {
            redisUtils.setex(cacheKey, session, CACHE_TTL_SESSION_SECONDS);
        }
        return session;
    }

    /**
     * 加载聊天历史（带缓存）
     */
    private List<LlmRequest.Message> loadChatHistory(String sessionId, String botUserId, int limit) {
        String cacheKey = CACHE_KEY_HISTORY + sessionId;
        Object cached = redisUtils.get(cacheKey);
        if (cached instanceof List) {
            @SuppressWarnings("unchecked")
            List<LlmRequest.Message> history = (List<LlmRequest.Message>) cached;
            return history;
        }

        List<LlmRequest.Message> history = new ArrayList<>();
        try {
            ChatImMessageQuery query = new ChatImMessageQuery();
            query.setSessionId(sessionId);
            query.setOrderBy("send_time desc");
            query.setPageNo(1);
            query.setPageSize(limit + 1);

            var result = chatImMessageService.findListByPage(query);
            List<ChatImMessage> messages = result.getList();

            if (messages != null && !messages.isEmpty()) {
                for (int i = messages.size() - 1; i >= 1; i--) {
                    ChatImMessage msg = messages.get(i);
                    String role = botUserId.equals(msg.getSendUserId()) ? "assistant" : "user";
                    history.add(new LlmRequest.Message(role, msg.getMessageContent()));
                }
            }

            if (!history.isEmpty()) {
                redisUtils.setex(cacheKey, history, CACHE_TTL_HISTORY_SECONDS);
            }

        } catch (Exception e) {
            logger.warn("加载聊天历史异常，将使用空历史", e);
        }

        return history;
    }

    /**
     * 推送AI回复给用户
     */
    private void pushAiReplyToUser(String sessionId, String botId, String botName,
                                    String sendUserId, ChatImMessage aiMessage, ChatSession session) {
        try {
            MessageSendDto<String> pushDto = new MessageSendDto<>();
            pushDto.setMessageId(aiMessage.getMessageId());
            pushDto.setSessionId(sessionId);
            pushDto.setSendUserId(botId);
            pushDto.setReceiveUserId(sendUserId);
            pushDto.setSendUserNickName(botName);
            pushDto.setMessageContent(aiMessage.getMessageContent());
            pushDto.setMessageType(MessageTypeEnum.CHAT_MESSAGE.getType());
            pushDto.setMessageTypeIm(MessageTypeImEnum.IM.getType());
            pushDto.setMessageSend2Type(UserContactTypeEnum.ROBOT.getType());
            pushDto.setSendTime(aiMessage.getSendTime());
            pushDto.setStatus(1);
            pushDto.setExtendData(JsonUtils.convertObj2Json(session));
            sessionManager.sendMessage(pushDto);
        } catch (Exception e) {
            logger.error("推送AI回复异常: sessionId={}", sessionId, e);
        }
    }

    /**
     * 更新会话最后消息
     */
    private void updateSessionLastMessage(String sessionId, String content) {
        ChatSession updateSession = new ChatSession();
        String lastMsg = content;
        if (lastMsg != null && lastMsg.length() > 100) {
            lastMsg = lastMsg.substring(0, 100) + "...";
        }
        updateSession.setLastMessage(lastMsg);
        updateSession.setLastMessageTime(System.currentTimeMillis());
        chatSessionService.updateChatSessionBySessionId(updateSession, sessionId);
        clearSessionCache(sessionId);
    }

    /**
     * 触发对话摘要生成
     */
    private void triggerSummaryGeneration(String sessionId, String botId, String userId, String botName) {
        try {
            ChatImMessageQuery countQuery = new ChatImMessageQuery();
            countQuery.setSessionId(sessionId);
            Integer messageCount = chatImMessageService.findCountByParam(countQuery);
            if (messageCount != null && messageCount > 0 && messageCount % SUMMARY_TRIGGER_INTERVAL == 0) {
                summaryService.generate(sessionId, botId, userId, botName);
            }
        } catch (Exception e) {
            logger.warn("生成对话摘要异常: sessionId={}", sessionId, e);
        }
    }

    /**
     * 检测用户是否在分享个人信息
     * 用于判断是否提升信任度
     */
    private boolean detectPersonalShare(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return false;
        }
        String lower = userMessage.toLowerCase();
        // 个人信息分享的关键词模式
        return lower.contains("我是") || lower.contains("我叫")
                || lower.contains("我今年") || lower.contains("我在")
                || lower.contains("我的工作") || lower.contains("我的家人")
                || lower.contains("我喜欢") && lower.length() > 20
                || lower.contains("我觉得") && lower.length() > 30
                || lower.contains("我最近") && lower.length() > 20;
    }

    /**
     * 清除会话缓存
     */
    private void clearSessionCache(String sessionId) {
        try {
            String cacheKey = CACHE_KEY_SESSION + sessionId;
            redisUtils.delete(cacheKey);
        } catch (Exception e) {
            logger.warn("清除会话缓存异常: sessionId={}", sessionId, e);
        }
    }

    /**
     * 清除聊天历史缓存
     */
    private void clearHistoryCache(String sessionId) {
        try {
            String cacheKey = CACHE_KEY_HISTORY + sessionId;
            redisUtils.delete(cacheKey);
        } catch (Exception e) {
            logger.warn("清除聊天历史缓存异常: sessionId={}", sessionId, e);
        }
    }

    /**
     * 保存降级回复（限流/过载时将提示文本作为AI回复入库并推送）
     *
     * @param sessionId   会话ID
     * @param sendUserId  发送消息的用户ID
     * @param sendUserName 发送消息的用户昵称
     * @param replyText   降级提示文本
     * @return 消息ID，失败返回null
     */
    private Long saveDegradedReply(String sessionId, String sendUserId, String sendUserName, String replyText) {
        try {
            ChatSession session = getChatSessionWithCache(sessionId);
            if (session == null) {
                logger.error("降级回复时会话不存在: sessionId={}", sessionId);
                return null;
            }

            String botId = session.getTargetUserId();
            String botName = session.getBotName();

            // 降级回复入库
            ChatImMessage aiMessage = new ChatImMessage();
            aiMessage.setSessionId(sessionId);
            aiMessage.setSendUserId(botId);
            aiMessage.setSendUserName(botName);
            aiMessage.setReceiveUserId(sendUserId);
            aiMessage.setMessageType(MessageTypeEnum.CHAT_MESSAGE.getType());
            aiMessage.setMessageContent(replyText);
            aiMessage.setMessageSendType(UserContactTypeEnum.ROBOT.getType());
            aiMessage.setStatus(1);
            aiMessage.setSendTime(System.currentTimeMillis());
            chatImMessageService.add(aiMessage);

            // 推送降级回复给用户
            pushAiReplyToUser(sessionId, botId, botName, sendUserId, aiMessage, session);

            // 更新会话最后消息
            updateSessionLastMessage(sessionId, replyText);

            logger.info("降级回复已保存: sessionId={}, messageId={}", sessionId, aiMessage.getMessageId());
            return aiMessage.getMessageId();

        } catch (Exception e) {
            logger.error("保存降级回复异常: sessionId={}", sessionId, e);
            return null;
        }
    }

    /**
     * 判断是否为机器人会话
     */
    public boolean isBotSession(String sessionId) {
        ChatSession session = chatSessionService.getChatSessionBySessionId(sessionId);
        return session != null && session.getBotName() != null && !session.getBotName().isEmpty();
    }
}
