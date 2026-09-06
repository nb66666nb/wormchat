package com.meetchat.entity.enums;

import com.meetchat.utils.StringTools;

public enum UserInMeetingStatusEnum {
    NO_IN(0, "未进入会议"),
    NORMAL_OUT(1, "正常退出或被提出"),
    NORMAL_IN(2, "正常进入"),
    BLACK_OUT(3,"被拉黑");
    private Integer status;
    private String desc;
    UserInMeetingStatusEnum(Integer status, String desc) {
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
    public static UserInMeetingStatusEnum getByStatus(String status) {
        try {
            if (StringTools.isEmpty(status)) {
                return null;
            }
            return UserInMeetingStatusEnum.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static UserInMeetingStatusEnum getByStatus(Integer status) {
        for (UserInMeetingStatusEnum item : UserInMeetingStatusEnum.values()) {
            if (item.getStatus().equals(status)) {
                return item;
            }
        }
        return null;
    }
}