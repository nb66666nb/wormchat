package com.meetchat.entity.enums;



/**
 * 会议成员类型枚举
 */
public enum MemberTypeEnum {

    HOST(0, "主持人"),
    ATTENDEE(1, "参会者");

    private final Integer type;
    private final String desc;

    MemberTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }
}