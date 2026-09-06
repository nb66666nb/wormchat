package com.meetchat.entity.enums;



/**
 * 加入方式枚举
 */
public enum JoinTypeEnum {

    FREE(0, "自由加入"),
    PASSWORD(1, "密码加入");

    private final Integer type;
    private final String desc;

    JoinTypeEnum(Integer type, String desc) {
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