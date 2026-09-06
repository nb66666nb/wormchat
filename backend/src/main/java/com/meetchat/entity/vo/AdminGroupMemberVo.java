package com.meetchat.entity.vo;

import com.meetchat.entity.po.GroupMember;

/**
 * 管理端群成员 VO：在群成员信息基础上附带用户昵称和在线状态
 */
public class AdminGroupMemberVo extends GroupMember {

    /**
     * 用户昵称（user_info.nick_name）
     */
    private String nickName;

    /**
     * 是否在线（WebSocket 连接是否存活）
     */
    private Boolean online;

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }
}
