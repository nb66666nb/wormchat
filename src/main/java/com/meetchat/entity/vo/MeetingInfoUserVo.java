package com.meetchat.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

public class MeetingInfoUserVo {
    private String userId;
    private String NickName;
    private Integer memberType;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastJoinTime;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNickName() {
        return NickName;
    }

    public void setNickName(String nickName) {
        NickName = nickName;
    }

    public Integer getMemberType() {
        return memberType;
    }

    public void setMemberType(Integer memberType) {
        this.memberType = memberType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getLastJoinTime() {
        return lastJoinTime;
    }

    public void setLastJoinTime(Date lastJoinTime) {
        this.lastJoinTime = lastJoinTime;
    }
}