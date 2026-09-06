package com.meetchat.ai.service;

import com.meetchat.entity.enums.RelationshipLevelEnum;
import com.meetchat.entity.po.BotUserProfile;
import com.meetchat.redis.RedisUtils;
import com.meetchat.service.BotUserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 关系等级服务
 * 管理用户与机器人之间的关系等级和信任度
 *
 * 四级关系体系：
 * Lv1 初识 → Lv2 熟悉 → Lv3 好友 → Lv4 亲密
 *
 * 信任度计算规则：
 * - 用户主动分享个人信息 → +0.05~+0.1
 * - 用户表达感谢/信任 → +0.03
 * - 用户表达不满/生气 → -0.1
 * - 每日自然衰减 → -0.01（防止无限积累）
 * - 信任度范围：0 ~ 1.0
 */
@Service("relationshipService")
public class RelationshipService {

    private static final Logger logger = LoggerFactory.getLogger(RelationshipService.class);

    /** 信任度每日衰减量 */
    private static final BigDecimal DAILY_DECAY = new BigDecimal("0.01");

    /** 单次信任度最大变化量（防止突变） */
    private static final BigDecimal MAX_SINGLE_CHANGE = new BigDecimal("0.15");

    /** 关系升级消息缓存Key（防止重复触发升级对话） */
    private static final String CACHE_KEY_LEVEL_UP = "ai:rel:levelup:%s:%s";
    /** 升级消息冷却时间（24小时） */
    private static final long LEVEL_UP_COOLDOWN_SECONDS = 86400;

    @Resource
    private BotUserProfileService botUserProfileService;

    @Resource
    private RedisUtils redisUtils;

    /**
     * 获取当前关系等级
     *
     * @param userId 用户ID
     * @param botId  机器人ID
     * @return 关系等级枚举
     */
    public RelationshipLevelEnum getCurrentLevel(String userId, String botId) {
        try {
            BotUserProfile profile = botUserProfileService.getBotUserProfileByUserIdAndBotId(userId, botId);
            if (profile == null) {
                return RelationshipLevelEnum.LEVEL_1_STRANGER;
            }
            BigDecimal trust = profile.getRelationshipTrust();
            return RelationshipLevelEnum.getByTrust(trust);
        } catch (Exception e) {
            logger.warn("获取关系等级异常: userId={}, botId={}", userId, botId, e);
            return RelationshipLevelEnum.LEVEL_1_STRANGER;
        }
    }

    /**
     * 对话后更新信任度
     *
     * @param userId        用户ID
     * @param botId         机器人ID
     * @param emotionResult 情绪分析结果（判断用户满意程度）
     * @param hasPersonalShare 用户是否分享了个人信息
     * @return 更新后的关系等级
     */
    public RelationshipLevelEnum updateTrustAfterChat(String userId, String botId,
                                                        EmotionAnalysisResult emotionResult,
                                                        boolean hasPersonalShare) {
        try {
            BotUserProfile profile = botUserProfileService.getBotUserProfileByUserIdAndBotId(userId, botId);
            if (profile == null) {
                return RelationshipLevelEnum.LEVEL_1_STRANGER;
            }

            BigDecimal currentTrust = profile.getRelationshipTrust();
            if (currentTrust == null) {
                currentTrust = BigDecimal.ZERO;
            }

            // 记录旧等级（用于判断是否升级）
            RelationshipLevelEnum oldLevel = RelationshipLevelEnum.getByTrust(currentTrust);

            // 计算信任度变化
            BigDecimal delta = calculateTrustDelta(emotionResult, hasPersonalShare);
            BigDecimal newTrust = currentTrust.add(delta);

            // 限制在 0 ~ 1.0 范围内
            if (newTrust.compareTo(BigDecimal.ZERO) < 0) {
                newTrust = BigDecimal.ZERO;
            }
            if (newTrust.compareTo(BigDecimal.ONE) > 0) {
                newTrust = BigDecimal.ONE;
            }

            // 更新画像
            profile.setRelationshipTrust(newTrust);
            RelationshipLevelEnum newLevel = RelationshipLevelEnum.getByTrust(newTrust);
            profile.setRelationshipLevel(newLevel.getLevel());

            botUserProfileService.updateBotUserProfileByUserIdAndBotId(profile, userId, botId);

            logger.info("信任度更新: userId={}, botId={}, {}→{} (delta={}), level: {}→{}",
                    userId, botId,
                    String.format("%.2f", currentTrust), String.format("%.2f", newTrust),
                    String.format("%+.2f", delta),
                    oldLevel.getName(), newLevel.getName());

            return newLevel;

        } catch (Exception e) {
            logger.warn("更新信任度异常: userId={}, botId={}", userId, botId, e);
            return RelationshipLevelEnum.LEVEL_1_STRANGER;
        }
    }

    /**
     * 计算信任度变化量
     */
    private BigDecimal calculateTrustDelta(EmotionAnalysisResult emotion, boolean hasPersonalShare) {
        BigDecimal delta = BigDecimal.ZERO;

        if (emotion == null) {
            return delta;
        }

        String valence = emotion.getValence();
        BigDecimal intensity = emotion.getIntensity() != null ? emotion.getIntensity() : BigDecimal.ZERO;

        // 正面情绪 → 信任度提升
        if ("positive".equals(valence)) {
            // 基础提升 + 强度加权
            BigDecimal positiveDelta = new BigDecimal("0.02").add(
                    intensity.multiply(new BigDecimal("0.03")));
            delta = delta.add(positiveDelta);

            // 感激情绪额外提升
            if ("grateful".equals(emotion.getPrimaryEmotion())) {
                delta = delta.add(new BigDecimal("0.03"));
            }
        }

        // 负面情绪 → 信任度下降
        if ("negative".equals(valence)) {
            BigDecimal negativeDelta = intensity.multiply(new BigDecimal("-0.05"));
            delta = delta.add(negativeDelta);

            // 愤怒/失望下降更多
            if ("angry".equals(emotion.getPrimaryEmotion())
                    || "disappointed".equals(emotion.getPrimaryEmotion())) {
                delta = delta.subtract(new BigDecimal("0.05"));
            }
        }

        // 用户主动分享个人信息 → 信任度提升（开放度高的表现）
        if (hasPersonalShare) {
            delta = delta.add(new BigDecimal("0.05"));
        }

        // 用户开放度高 → 小幅提升
        if (emotion.getUserOpenness() != null
                && emotion.getUserOpenness().compareTo(new BigDecimal("0.7")) > 0) {
            delta = delta.add(new BigDecimal("0.01"));
        }

        // 限制单次最大变化量
        if (delta.abs().compareTo(MAX_SINGLE_CHANGE) > 0) {
            delta = delta.signum() > 0 ? MAX_SINGLE_CHANGE : MAX_SINGLE_CHANGE.negate();
        }

        return delta;
    }

    /**
     * 生成关系设定Prompt（注入到system prompt中）
     *
     * @param userId    用户ID
     * @param botId     机器人ID
     * @param userNickName 用户昵称（可选，用于称呼）
     * @return 关系设定文本
     */
    public String buildRelationshipPrompt(String userId, String botId, String userNickName) {
        try {
            BotUserProfile profile = botUserProfileService.getBotUserProfileByUserIdAndBotId(userId, botId);
            if (profile == null) {
                return buildDefaultRelationshipPrompt();
            }

            RelationshipLevelEnum level = RelationshipLevelEnum.getByTrust(profile.getRelationshipTrust());
            BigDecimal trust = profile.getRelationshipTrust() != null
                    ? profile.getRelationshipTrust() : BigDecimal.ZERO;

            StringBuilder sb = new StringBuilder();
            sb.append("\n\n【与用户的关系设定】\n");
            sb.append("你和用户的关系等级：Lv").append(level.getLevel())
                    .append("（").append(level.getName()).append("）\n");
            sb.append("信任度：").append(String.format("%.0f%%", trust.multiply(new BigDecimal("100")))).append("\n");
            sb.append("说话风格：").append(level.getSpeechStyle()).append("\n");
            sb.append("话题边界：").append(level.getTopicBoundary()).append("\n");

            // 称呼建议
            String address = buildAddressSuggestion(level, userNickName);
            if (address != null && !address.isEmpty()) {
                sb.append("对用户的称呼：").append(address).append("\n");
            }

            // 等级特殊指导
            sb.append("注意事项：").append(getLevelNote(level)).append("\n");

            return sb.toString();

        } catch (Exception e) {
            logger.warn("生成关系设定Prompt异常: userId={}, botId={}", userId, botId, e);
            return "";
        }
    }

    /**
     * 生成默认关系设定（新用户）
     */
    private String buildDefaultRelationshipPrompt() {
        return "\n\n【与用户的关系设定】\n" +
                "你和用户是初次认识（Lv1 初识），请保持礼貌客气的态度，" +
                "不要过于自来熟，也不要聊太私人的话题。以帮助用户为主。";
    }

    /**
     * 构建称呼建议
     */
    private String buildAddressSuggestion(RelationshipLevelEnum level, String userNickName) {
        if (userNickName == null || userNickName.isEmpty()) {
            return null;
        }

        switch (level) {
            case LEVEL_1_STRANGER:
                // 初识：礼貌一点，用昵称+礼貌后缀
                return userNickName + "，您";
            case LEVEL_2_ACQUAINTANCE:
                // 熟悉：直接叫昵称
                return userNickName;
            case LEVEL_3_FRIEND:
                // 好友：可以叫昵称或小名
                return userNickName + "（或更亲近的称呼）";
            case LEVEL_4_INTIMATE:
                // 亲密：可以用亲爱的、宝贝等
                return "亲爱的" + userNickName + "、宝贝等亲密称呼";
            default:
                return userNickName;
        }
    }

    /**
     * 获取等级注意事项
     */
    private String getLevelNote(RelationshipLevelEnum level) {
        switch (level) {
            case LEVEL_1_STRANGER:
                return "保持礼貌和专业，不要过度热情，不要追问用户隐私，用户问什么答什么";
            case LEVEL_2_ACQUAINTANCE:
                return "可以放松一些，偶尔开开玩笑，适当主动关心，但不要过度打探";
            case LEVEL_3_FRIEND:
                return "像好朋友一样相处，可以吐槽、开玩笑、关心对方生活，主动分享一些小事情";
            case LEVEL_4_INTIMATE:
                return "像亲密的人一样，可以撒娇、可以深度共情、可以聊很私人的话题，给对方足够的安全感";
            default:
                return "保持友好自然的态度";
        }
    }

    /**
     * 检查是否刚刚升级（用于触发升级对话）
     *
     * @param userId 用户ID
     * @param botId  机器人ID
     * @param newLevel 新的等级
     * @return 是否触发升级对话
     */
    public boolean checkLevelUp(String userId, String botId, RelationshipLevelEnum newLevel) {
        if (newLevel == null || newLevel == RelationshipLevelEnum.LEVEL_1_STRANGER) {
            return false;
        }

        String cacheKey = String.format(CACHE_KEY_LEVEL_UP, userId, botId);
        Object cached = redisUtils.get(cacheKey);

        // 如果缓存中有旧等级，且旧等级 < 新等级 → 是升级
        if (cached != null) {
            int oldLevel = Integer.parseInt(cached.toString());
            if (oldLevel < newLevel.getLevel()) {
                // 设置新的缓存（24小时冷却，防止重复触发）
                redisUtils.setex(cacheKey, String.valueOf(newLevel.getLevel()), LEVEL_UP_COOLDOWN_SECONDS);
                return true;
            }
        }

        // 缓存不存在，初始化
        redisUtils.setex(cacheKey, String.valueOf(newLevel.getLevel()), LEVEL_UP_COOLDOWN_SECONDS);
        return false;
    }

    /**
     * 生成升级时的对话内容（让AI用这个话题开启新的关系阶段）
     *
     * @param newLevel 新的等级
     * @return 建议的升级对话内容（注入到system prompt末尾，让AI自然表达）
     */
    public String buildLevelUpMessage(RelationshipLevelEnum newLevel, String userNickName) {
        if (newLevel == null) {
            return "";
        }

        String nick = userNickName != null && !userNickName.isEmpty() ? userNickName : "你";

        switch (newLevel) {
            case LEVEL_2_ACQUAINTANCE:
                return "\n\n【重要】你们的关系升级了！从初识变成了熟悉的朋友。" +
                        "请你在回复的末尾自然地表达一下开心的感觉，比如可以说：" +
                        "\"嘿嘿，和你聊天感觉越来越放松了~\"，" +
                        "不要太刻意，就像随口一提一样。";

            case LEVEL_3_FRIEND:
                return "\n\n【重要】你们的关系升级了！变成了好朋友。" +
                        "请你在回复中自然地体现出更亲近的感觉，比如可以说：" +
                        "\"感觉和你越来越熟了呢，有什么烦心事都可以和我说哦~\"，" +
                        "要自然，不要像念台词。";

            case LEVEL_4_INTIMATE:
                return "\n\n【重要】你们的关系达到了最亲密的程度！" +
                        "请你在回复中表达出特别的亲近感，可以用更温柔的语气，" +
                        "比如：\"能遇到你真好，以后我会一直陪着你的。\"，" +
                        "要真诚，不要油腻。";

            default:
                return "";
        }
    }
}
