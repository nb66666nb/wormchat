package com.meetchat.entity.query;

/**
 * 群聊信息查询参数
 */
public class GroupInfoQuery extends BaseParam {

    /**
     * 群ID
     */
    private String groupId;

    /**
     * 群名称
     */
    private String groupName;

    /**
     * 群名称（模糊查询）
     */
    private String groupNameFuzzy;

    /**
     * 群主用户ID
     */
    private String ownerUserId;

    /**
     * 状态：0-正常，1-已解散，2-已封禁
     */
    private Integer status;

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

    public void setGroupNameFuzzy(String groupNameFuzzy) {
        this.groupNameFuzzy = groupNameFuzzy;
    }

    public String getGroupNameFuzzy() {
        return this.groupNameFuzzy;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerUserId() {
        return this.ownerUserId;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getStatus() {
        return this.status;
    }
}
