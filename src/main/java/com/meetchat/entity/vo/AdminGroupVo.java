package com.meetchat.entity.vo;

import com.meetchat.entity.po.GroupInfo;

/**
 * 管理端群聊列表 VO：在群信息基础上附带成员数、群主昵称、在线成员数
 */
public class AdminGroupVo extends GroupInfo {

    /**
     * 正常成员数（group_member.status=0）
     */
    private Integer memberCount;

    /**
     * 群主昵称
     */
    private String ownerNickName;

    /**
     * 当前在线成员数
     */
    private Integer onlineMemberCount;

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }

    public String getOwnerNickName() {
        return ownerNickName;
    }

    public void setOwnerNickName(String ownerNickName) {
        this.ownerNickName = ownerNickName;
    }

    public Integer getOnlineMemberCount() {
        return onlineMemberCount;
    }

    public void setOnlineMemberCount(Integer onlineMemberCount) {
        this.onlineMemberCount = onlineMemberCount;
    }
}
