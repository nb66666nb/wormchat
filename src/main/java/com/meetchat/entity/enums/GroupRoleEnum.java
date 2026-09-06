package com.meetchat.entity.enums;

/**
 * 群聊成员角色枚举
 */
public enum GroupRoleEnum {

    OWNER("OWNER", "群主"),
    ADMIN("ADMIN", "管理员"),
    MEMBER("MEMBER", "普通成员");

    private final String role;
    private final String desc;

    GroupRoleEnum(String role, String desc) {
        this.role = role;
        this.desc = desc;
    }

    public String getRole() {
        return role;
    }

    public String getDesc() {
        return desc;
    }

    public static GroupRoleEnum getByRole(String role) {
        if (role == null) return null;
        for (GroupRoleEnum e : values()) {
            if (e.role.equals(role)) return e;
        }
        return null;
    }
}
