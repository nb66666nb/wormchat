package com.meetchat.entity.enums;

/**
 * 群聊状态枚举
 */
public enum GroupStatusEnum {

    ACTIVE(0, "正常"),
    DISSOLVED(1, "已解散"),
    BANNED(2, "已封禁");

    private final Integer status;
    private final String desc;

    GroupStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public static GroupStatusEnum getByStatus(Integer status) {
        if (status == null) return null;
        for (GroupStatusEnum e : values()) {
            if (e.status.equals(status)) return e;
        }
        return null;
    }
}
