package com.meetchat.entity.enums;

/**
 * 会议状态枚举
 */
public enum MeetingStatusEnum {

    NOT_STARTED(0, "未开始"),
    IN_PROGRESS(1, "进行中"),
    ENDED(2, "已结束");

    private final Integer status;
    private final String desc;

    MeetingStatusEnum(Integer status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public Integer getStatus() {
        return status;
    }

    public String getDesc() {
        return desc;
    }

    public static MeetingStatusEnum getByStatus(Integer status) {
        if (status == null) return null;
        for (MeetingStatusEnum e : values()) {
            if (e.getStatus().equals(status)) return e;
        }
        return null;
    }
}