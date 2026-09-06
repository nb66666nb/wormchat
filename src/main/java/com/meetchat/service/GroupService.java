package com.meetchat.service;

import com.meetchat.entity.po.GroupInfo;
import com.meetchat.entity.po.GroupMember;
import com.meetchat.entity.query.GroupInfoQuery;
import com.meetchat.entity.vo.PaginationResultVO;

import java.util.List;

/**
 * 群聊业务接口
 */
public interface GroupService {

    /**
     * 创建群聊
     * @param groupName 群名称
     * @param creatorUserId 创建者用户ID（自动成为群主）
     * @param memberIds 初始成员用户ID列表
     * @param announcement 群公告
     * @param invitePermission 邀请权限：0=所有人可邀请，1=仅群主/管理员可邀请
     * @return 群信息
     */
    GroupInfo createGroup(String groupName, String creatorUserId, List<String> memberIds,
                          String announcement, Integer invitePermission);

    /**
     * 邀请成员加入群聊
     * @param groupId 群ID
     * @param inviterUserId 邀请者用户ID
     * @param memberIds 被邀请的用户ID列表
     */
    void inviteMember(String groupId, String inviterUserId, List<String> memberIds);

    /**
     * 踢出群成员
     * @param groupId 群ID
     * @param operatorUserId 操作者用户ID
     * @param targetUserId 被踢出的用户ID
     */
    void kickMember(String groupId, String operatorUserId, String targetUserId);

    /**
     * 设置/取消管理员
     * @param groupId 群ID
     * @param operatorUserId 操作者用户ID（仅群主）
     * @param targetUserId 目标用户ID
     * @param isAdmin true=设为管理员，false=取消管理员
     */
    void setAdmin(String groupId, String operatorUserId, String targetUserId, boolean isAdmin);

    /**
     * 更新群设置
     * @param groupId 群ID
     * @param operatorUserId 操作者用户ID
     * @param groupName 群名称（null表示不修改）
     * @param groupAvatar 群头像（null表示不修改）
     * @param announcement 群公告（null表示不修改）
     * @param invitePermission 邀请权限（null表示不修改）
     */
    void updateGroupSettings(String groupId, String operatorUserId, String groupName,
                             String groupAvatar, String announcement, Integer invitePermission);

    /**
     * 解散群聊
     * @param groupId 群ID
     * @param operatorUserId 操作者用户ID（仅群主）
     */
    void dissolveGroup(String groupId, String operatorUserId);

    /**
     * 退出群聊
     * @param groupId 群ID
     * @param userId 用户ID
     */
    void leaveGroup(String groupId, String userId);

    /**
     * 获取群信息
     */
    GroupInfo getGroupInfo(String groupId);

    /**
     * 获取群成员列表
     */
    List<GroupMember> getGroupMembers(String groupId);

    /**
     * 获取我创建的群聊列表（role=OWNER）
     */
    List<GroupInfo> getMyCreatedGroups(String userId);

    /**
     * 获取我加入的群聊列表（role!=OWNER）
     */
    List<GroupInfo> getMyJoinedGroups(String userId);

    /**
     * 分页查询群聊列表（管理员）
     * @param param 查询参数（群名模糊、状态、群主等）
     */
    PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery param);

    /**
     * 管理员封禁/解封群聊
     * @param groupId 群ID
     * @param status 目标状态：0-正常（解封），2-已封禁
     */
    void manageGroup(String groupId, Integer status);
}
