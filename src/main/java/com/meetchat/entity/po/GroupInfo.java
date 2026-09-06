package com.meetchat.entity.po;

import java.io.Serializable;

/**
 * 群聊信息表
 */
public class GroupInfo implements Serializable {

    /**
     * 群ID（主键）
     */
    private String groupId;

    /**
     * 群名称
     */
    private String groupName;

    /**
     * 群头像路径
     */
    private String groupAvatar;

    /**
     * 群主用户ID
     */
    private String ownerUserId;

    /**
     * 邀请权限：0-所有人可邀请，1-仅群主/管理员可邀请
     */
    private Integer invitePermission;

    /**
     * 群公告
     */
    private String announcement;

    /**
     * 状态：0-正常，1-已解散
     */
    private Integer status;

    /**
     * 创建时间戳(ms)
     */
    private Long createTime;

    /**
     * 更新时间戳(ms)
     */
    private Long updateTime;

    /**
     * 当前用户在该群中的角色（非表字段，查询"我的群聊列表"时由 group_member.role 填充）
     */
    private String memberRole;

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getGroupId() {
        return this.groupId;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupName() {
        return this.groupName;
    }

    public void setGroupAvatar(String groupAvatar) {
        this.groupAvatar = groupAvatar;
    }

    public String getGroupAvatar() {
        return this.groupAvatar;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerUserId() {
        return this.ownerUserId;
    }

    public void setInvitePermission(Integer invitePermission) {
        this.invitePermission = invitePermission;
    }

    public Integer getInvitePermission() {
        return this.invitePermission;
    }

    public void setAnnouncement(String announcement) {
        this.announcement = announcement;
    }

    public String getAnnouncement() {
        return this.announcement;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return this.status;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getCreateTime() {
        return this.createTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public Long getUpdateTime() {
        return this.updateTime;
    }

    public void setMemberRole(String memberRole) {
        this.memberRole = memberRole;
    }

    public String getMemberRole() {
        return this.memberRole;
    }

    @Override
    public String toString() {
        return "群ID:" + (groupId == null ? "空" : groupId)
                + "，群名称:" + (groupName == null ? "空" : groupName)
                + "，群主:" + (ownerUserId == null ? "空" : ownerUserId)
                + "，状态:" + (status == null ? "空" : status);
    }
}
