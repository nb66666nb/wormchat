package com.meetchat.entity.enums;

public enum ContactRequestStatusEnum {
    TO_DEAL(0, "待处理"),
    ACCEPT(1, "接受"),
    REFUSE(2, "拒绝"),
    SEND_BACK(3,"撤回");
    private Integer status;
    private String  desc;

    ContactRequestStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
    public static ContactRequestStatusEnum getByStatus(Integer status) {
        if (status == null) return null;
        for (ContactRequestStatusEnum e : values()) {
            if (e.getStatus().equals(status)) return e;
        }
        return null;
    }
}