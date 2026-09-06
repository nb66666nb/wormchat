package com.meetchat.entity.enums;

/**
 * 消息通道类型枚举
 * 用于区分消息是IM消息还是会议消息，前端据此存入不同的数据库表
 */
public enum MessageTypeImEnum {

    /** IM即时通讯消息 → chat_im_message表 */
    IM(0, "IM消息"),

    /** 会议消息 → chat_message表 */
    MEETING(1, "会议消息"),
    SESSION(2,"会话消息");

    private final Integer type;
    private final String desc;

    MessageTypeImEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static MessageTypeImEnum getByType(Integer type) {
        if (type == null) return null;
        for (MessageTypeImEnum e : values()) {
            if (e.type.equals(type)) return e;
        }
        return null;
    }
}