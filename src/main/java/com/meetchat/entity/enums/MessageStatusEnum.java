package com.meetchat.entity.enums;

public enum MessageStatusEnum {

    DONE_SEND(2, "已发送"),
    NO_SEND(3, "未发送");
    private Integer status;
    private String desc;
    MessageStatusEnum(Integer status, String desc) {
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
}