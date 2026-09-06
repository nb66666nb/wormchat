package com.meetchat.entity.enums;

import java.math.BigDecimal;

/**
 * 机器人与用户的关系等级
 * 四级递进体系：初识 → 熟悉 → 好友 → 亲密
 *
 * 关系等级影响：
 * - 说话方式（礼貌→随意→亲密）
 * - 话题边界（公事→兴趣→情感→私密）
 * - 称呼方式（您→你→昵称→亲爱的）
 */
public enum RelationshipLevelEnum {

    LEVEL_1_STRANGER(1, "初识",
            new BigDecimal("0.0"), new BigDecimal("0.3"),
            "礼貌、客气、保持距离",
            "您",
            "不聊私人话题，以帮助用户为主"),

    LEVEL_2_ACQUAINTANCE(2, "熟悉",
            new BigDecimal("0.3"), new BigDecimal("0.6"),
            "放松、偶尔开玩笑",
            "你",
            "可以聊兴趣爱好、日常生活"),

    LEVEL_3_FRIEND(3, "好友",
            new BigDecimal("0.6"), new BigDecimal("0.85"),
            "随意、吐槽、关心日常",
            "你/朋友式称呼",
            "可以聊情感、烦恼、个人困扰"),

    LEVEL_4_INTIMATE(4, "亲密",
            new BigDecimal("0.85"), new BigDecimal("1.0"),
            "撒娇、深度共情、亲密无间",
            "昵称/亲爱的",
            "可以聊很私人的事，深度情感支持");

    private final int level;
    private final String name;
    private final BigDecimal trustMin;
    private final BigDecimal trustMax;
    private final String speechStyle;
    private final String formOfAddress;
    private final String topicBoundary;

    RelationshipLevelEnum(int level, String name,
                           BigDecimal trustMin, BigDecimal trustMax,
                           String speechStyle, String formOfAddress,
                           String topicBoundary) {
        this.level = level;
        this.name = name;
        this.trustMin = trustMin;
        this.trustMax = trustMax;
        this.speechStyle = speechStyle;
        this.formOfAddress = formOfAddress;
        this.topicBoundary = topicBoundary;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getTrustMin() {
        return trustMin;
    }

    public BigDecimal getTrustMax() {
        return trustMax;
    }

    public String getSpeechStyle() {
        return speechStyle;
    }

    public String getFormOfAddress() {
        return formOfAddress;
    }

    public String getTopicBoundary() {
        return topicBoundary;
    }

    /**
     * 根据信任度计算关系等级
     *
     * @param trust 信任度 0-1
     * @return 关系等级
     */
    public static RelationshipLevelEnum getByTrust(BigDecimal trust) {
        if (trust == null) {
            return LEVEL_1_STRANGER;
        }
        for (RelationshipLevelEnum level : values()) {
            if (trust.compareTo(level.trustMin) >= 0
                    && trust.compareTo(level.trustMax) < 0) {
                return level;
            }
        }
        // 1.0 以上按最高级算
        if (trust.compareTo(BigDecimal.ONE) >= 0) {
            return LEVEL_4_INTIMATE;
        }
        return LEVEL_1_STRANGER;
    }

    /**
     * 根据等级数字获取枚举
     */
    public static RelationshipLevelEnum getByLevel(Integer level) {
        if (level == null) return null;
        for (RelationshipLevelEnum e : values()) {
            if (e.level == level) return e;
        }
        return null;
    }
}
