package com.meetchat.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meetchat.ai.llm.LlmClient;
import com.meetchat.ai.llm.LlmRequest;
import com.meetchat.ai.llm.LlmResponse;
import com.meetchat.ai.prompt.PromptManager;
import com.meetchat.entity.query.ChatImMessageQuery;
import com.meetchat.entity.po.BotUserProfile;
import com.meetchat.entity.po.ChatImMessage;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.redis.RedisUtils;
import com.meetchat.service.BotUserProfileService;
import com.meetchat.service.ChatImMessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户画像服务
 * 负责用户画像的加载、更新和LLM特征分析
 */
@Service("profileService")
public class ProfileService {

    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    /** 用户画像缓存Key前缀 */
    private static final String CACHE_KEY_PROFILE = "ai:profile:%s:%s";
    /** 用户画像缓存过期时间（1小时） */
    private static final long CACHE_TTL_SECONDS = 3600;
    /** LLM分析频率（每5次对话分析一次） */
    private static final int PROFILE_ANALYSIS_INTERVAL = 5;
    /** LLM分析时查询的消息条数 */
    private static final int PROFILE_MESSAGE_LIMIT = 10;
    /** keyFacts最大保留条数 */
    private static final int MAX_KEY_FACTS = 20;

    @Resource
    private BotUserProfileService botUserProfileService;

    @Resource
    private ChatImMessageService chatImMessageService;

    @Resource
    private PromptManager promptManager;

    @Resource
    private LlmClient llmClient;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 加载用户画像上下文（带缓存）
     */
    public String loadUserProfileContext(String userId, String botId) {
        try {
            // 1. 尝试从缓存读取
            String cacheKey = String.format(CACHE_KEY_PROFILE, userId, botId);
            Object cached = redisUtils.get(cacheKey);
            if (cached instanceof String) {
                logger.debug("用户画像缓存命中: userId={}, botId={}", userId, botId);
                return (String) cached;
            }

            // 2. 缓存未命中，查询数据库
            BotUserProfile profile = botUserProfileService.getBotUserProfileByUserIdAndBotId(userId, botId);
            if (profile == null) {
                return null;
            }

            String context = promptManager.renderUserProfilePrompt(
                    profile.getCommunicationStyle(),
                    profile.getInterests(),
                    profile.getKeyFacts(),
                    profile.getEmotionBaseline(),
                    profile.getStressLevel() != null ? profile.getStressLevel().toPlainString() : null,
                    profile.getLonelinessLevel() != null ? profile.getLonelinessLevel().toPlainString() : null,
                    profile.getRelationshipLevel() != null ? profile.getRelationshipLevel().toString() : null,
                    profile.getRelationshipTrust() != null ? profile.getRelationshipTrust().toPlainString() : null
            );

            // 3. 写入缓存
            if (context != null && !context.isEmpty()) {
                redisUtils.setex(cacheKey, context, CACHE_TTL_SECONDS);
                logger.debug("用户画像已缓存: userId={}, botId={}", userId, botId);
            }

            return context;
        } catch (Exception e) {
            logger.warn("加载用户画像异常: userId={}, botId={}", userId, botId, e);
            return null;
        }
    }

    /**
     * 对话后更新用户画像
     */
    public void updateAfterChat(String userId, String botId, String sessionId,
                                 String userMessage, String aiReply) {
        try {
            BotUserProfile profile = botUserProfileService.getBotUserProfileByUserIdAndBotId(userId, botId);
            if (profile == null) {
                return;
            }

            // 更新交互次数
            int count = profile.getInteractionCount() != null ? profile.getInteractionCount() : 0;
            int newCount = count + 1;
            profile.setInteractionCount(newCount);
            // 更新最后交互时间
            profile.setLastInteractionTime(System.currentTimeMillis());

            // 每N次对话调用LLM分析用户特征
            if (newCount % PROFILE_ANALYSIS_INTERVAL == 0) {
                analyzeAndUpdateUserProfile(profile, userId, botId, sessionId);
            }

            botUserProfileService.updateBotUserProfileByUserIdAndBotId(profile, userId, botId);
            // 清除缓存
            clearCache(userId, botId);
        } catch (Exception e) {
            logger.warn("更新用户画像异常: userId={}, botId={}", userId, botId, e);
        }
    }

    /**
     * 调用LLM分析用户特征并更新画像
     */
    private void analyzeAndUpdateUserProfile(BotUserProfile profile, String userId, String botId, String sessionId) {
        try {
            // 查询最近消息
            ChatImMessageQuery query = new ChatImMessageQuery();
            query.setSessionId(sessionId);
            query.setOrderBy("send_time desc");
            query.setPageNo(1);
            query.setPageSize(PROFILE_MESSAGE_LIMIT);

            PaginationResultVO<ChatImMessage> result = chatImMessageService.findListByPage(query);
            List<ChatImMessage> messages = result.getList();

            if (messages == null || messages.isEmpty()) {
                return;
            }

            // 拼接对话文本
            StringBuilder dialogText = buildDialogText(messages, botId);

            // 构建LLM请求
            String systemPrompt = "你是一个用户行为分析助手。分析以下用户与AI的对话，提取用户特征。" +
                    "输出严格的JSON格式（不要包含其他内容，不要使用markdown代码块）：" +
                    "{\"communicationStyle\":\"warm或concise或balanced\"," +
                    "\"emotionBaseline\":\"positive或neutral或negative\"," +
                    "\"keyFacts\":\"关键信息1;关键信息2;关键信息3\"}\n" +
                    "说明：\n" +
                    "- communicationStyle: 用户沟通风格(warm=热情温暖, concise=简洁直接, balanced=平衡)\n" +
                    "- emotionBaseline: 用户情绪基线(positive=积极, neutral=中性, negative=消极)\n" +
                    "- keyFacts: 用户分享的客观关键事实，用分号分隔，如\"用户是程序员;用户最近压力大\"";

            List<LlmRequest.Message> llmMessages = new ArrayList<>();
            llmMessages.add(new LlmRequest.Message("system", systemPrompt));
            llmMessages.add(new LlmRequest.Message("user", "请分析以下对话：\n\n" + dialogText.toString()));

            // 调用LLM
            LlmResponse llmResponse = llmClient.chat(llmMessages);
            String responseContent = llmResponse.getContent();

            if (responseContent == null || responseContent.trim().isEmpty()) {
                logger.warn("用户画像分析LLM返回为空: userId={}", userId);
                return;
            }

            // 解析JSON
            String jsonStr = extractJson(responseContent);
            JSONObject profileJson = JSON.parseObject(jsonStr);

            // 更新沟通风格
            String communicationStyle = profileJson.getString("communicationStyle");
            if (communicationStyle != null && !communicationStyle.trim().isEmpty()) {
                profile.setCommunicationStyle(communicationStyle.trim());
            }

            // 更新情绪基线
            String emotionBaseline = profileJson.getString("emotionBaseline");
            if (emotionBaseline != null && !emotionBaseline.trim().isEmpty()) {
                profile.setEmotionBaseline(emotionBaseline.trim());
            }

            // 关键事实追加（不覆盖），去重，最多保留MAX_KEY_FACTS条
            String newKeyFacts = profileJson.getString("keyFacts");
            if (newKeyFacts != null && !newKeyFacts.trim().isEmpty()) {
                String existingKeyFacts = profile.getKeyFacts();
                String mergedKeyFacts = mergeKeyFacts(existingKeyFacts, newKeyFacts, MAX_KEY_FACTS);
                profile.setKeyFacts(mergedKeyFacts);
            }

            logger.info("用户画像LLM分析完成: userId={}, communicationStyle={}, emotionBaseline={}, keyFacts={}",
                    userId, profile.getCommunicationStyle(), profile.getEmotionBaseline(), profile.getKeyFacts());

        } catch (Exception e) {
            logger.warn("LLM分析用户特征异常: userId={}", userId, e);
        }
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
     * 合并关键事实，去重并保留最多maxCount条
     */
    private String mergeKeyFacts(String existingFacts, String newFacts, int maxCount) {
        List<String> allFacts = new ArrayList<>();

        if (existingFacts != null && !existingFacts.trim().isEmpty()) {
            for (String fact : existingFacts.split(";")) {
                String trimmed = fact.trim();
                if (!trimmed.isEmpty() && !allFacts.contains(trimmed)) {
                    allFacts.add(trimmed);
                }
            }
        }

        if (newFacts != null && !newFacts.trim().isEmpty()) {
            for (String fact : newFacts.split(";")) {
                String trimmed = fact.trim();
                if (!trimmed.isEmpty() && !allFacts.contains(trimmed)) {
                    allFacts.add(trimmed);
                }
            }
        }

        if (allFacts.size() > maxCount) {
            allFacts = new ArrayList<>(allFacts.subList(allFacts.size() - maxCount, allFacts.size()));
        }

        return String.join(";", allFacts);
    }

    /**
     * 清除用户画像缓存
     */
    public void clearCache(String userId, String botId) {
        try {
            String cacheKey = String.format(CACHE_KEY_PROFILE, userId, botId);
            redisUtils.delete(cacheKey);
            logger.debug("用户画像缓存已清除: userId={}, botId={}", userId, botId);
        } catch (Exception e) {
            logger.warn("清除用户画像缓存异常: userId={}, botId={}", userId, botId, e);
        }
    }
}
