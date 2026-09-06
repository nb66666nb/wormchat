package com.meetchat.entity.po;

import java.io.Serializable;

/**
 * 群成员表
 */
public class GroupMember implements Serializable {

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 群ID
     */
    private String groupId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 角色：OWNER-群主，ADMIN-管理员，MEMBER-普通成员
     */
    private String role;

    /**
     * 群昵称
     */
    private String nicknameInGroup;

    /**
     * 加入时间戳(ms)
     */
    private Long joinTime;

    /**
     * 状态：0-正常，1-主动退出，2-被踢出
     */
    private Integer status;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return this.userId;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return this.role;
    }

    public void setNicknameInGroup(String nicknameInGroup) {
        this.nicknameInGroup = nicknameInGroup;
    }

    public String getNicknameInGroup() {
        return this.nicknameInGroup;
    }

    public void setJoinTime(Long joinTime) {
        this.joinTime = joinTime;
    }

    public Long getJoinTime() {
        return this.joinTime;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return this.status;
    }

    @Override
    public String toString() {
        return "群ID:" + (groupId == null ? "空" : groupId)
                + "，用户ID:" + (userId == null ? "空" : userId)
                + "，角色:" + (role == null ? "空" : role)
                + "，状态:" + (status == null ? "空" : status);
    }
}
