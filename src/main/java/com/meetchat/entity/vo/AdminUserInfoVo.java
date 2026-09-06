package com.meetchat.entity.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.meetchat.entity.po.UserInfo;

/**
 * 管理端用户列表 VO：在用户信息基础上附带在线状态，且不返回密码
 */
public class AdminUserInfoVo extends UserInfo {

    /**
     * 是否在线（WebSocket 连接是否存活）
     */
    private Boolean online;

    public Boolean getOnline() {
        return online;
    }

    public void setOnline(Boolean online) {
        this.online = online;
    }

    /**
     * 管理端不返回密码
     */
    @Override
    @JsonIgnore
    public String getPassword() {
        return super.getPassword();
    }
}
