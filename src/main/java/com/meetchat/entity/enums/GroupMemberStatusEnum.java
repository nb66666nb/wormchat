package com.meetchat.entity.enums;

/**
 * 群成员状态枚举
 */
public enum GroupMemberStatusEnum {

    NORMAL(0, "正常"),
    LEFT(1, "主动退出"),
    KICKED(2, "被踢出");

    private final Integer status;
    private final String desc;

    GroupMemberStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public static GroupMemberStatusEnum getByStatus(Integer status) {
        if (status == null) return null;
        for (GroupMemberStatusEnum e : values()) {
            if (e.status.equals(status)) return e;
        }
        return null;
    }
}
