package com.meetchat.service.impl;

import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.enums.MessageTypeEnum;
import com.meetchat.entity.enums.MessageTypeImEnum;
import com.meetchat.entity.enums.UserContactTypeEnum;
import com.meetchat.entity.enums.GroupRoleEnum;
import com.meetchat.entity.enums.GroupMemberStatusEnum;
import com.meetchat.entity.enums.GroupStatusEnum;
import com.meetchat.entity.enums.PageSize;
import com.meetchat.entity.po.ChatSession;
import com.meetchat.entity.po.GroupInfo;
import com.meetchat.entity.po.GroupMember;
import com.meetchat.entity.query.GroupInfoQuery;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.exception.BusinessException;
import com.meetchat.mappers.ChatSessionMapper;
import com.meetchat.mappers.GroupInfoMapper;
import com.meetchat.mappers.GroupMemberMapper;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.service.ChatSessionService;
import com.meetchat.service.GroupService;
import com.meetchat.utils.JsonUtils;
import com.meetchat.utils.StringTools;
import com.meetchat.webSocket.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;

/**
 * 群聊业务实现
 */
@Service("groupService")
public class GroupServiceImpl implements GroupService {

    private static final Logger logger = LoggerFactory.getLogger(GroupServiceImpl.class);

    @Resource
    private GroupInfoMapper<GroupInfo, GroupInfoQuery> groupInfoMapper;

    @Resource
    private GroupMemberMapper<GroupMember, Object> groupMemberMapper;

    @Resource
    private ChatSessionService chatSessionService;

    @Resource
    private ChatSessionMapper<ChatSession, Object> chatSessionMapper;

    @Resource
    private SessionManager sessionManager;

    // ==================== 核心方法 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupInfo createGroup(String groupName, String creatorUserId, List<String> memberIds,
                                  String announcement, Integer invitePermission) {
        if (StringTools.isEmpty(groupName)) {
            throw new BusinessException("群名称不能为空");
        }
        if (StringTools.isEmpty(creatorUserId)) {
            throw new BusinessException("创建者ID不能为空");
        }

        // 1. 生成群ID
        String groupId = "G" + StringTools.getRandomNumber(10);
        long now = System.currentTimeMillis();

        // 2. 创建群信息
        GroupInfo groupInfo = new GroupInfo();
        groupInfo.setGroupId(groupId);
        groupInfo.setGroupName(groupName);
        groupInfo.setOwnerUserId(creatorUserId);
        groupInfo.setInvitePermission(invitePermission != null ? invitePermission : 0);
        groupInfo.setAnnouncement(announcement != null ? announcement : "");
        groupInfo.setStatus(GroupStatusEnum.ACTIVE.getStatus());
        groupInfo.setCreateTime(now);
        groupInfo.setUpdateTime(now);
        groupInfoMapper.insert(groupInfo);

        // 3. 添加群主为成员
        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroupId(groupId);
        ownerMember.setUserId(creatorUserId);
        ownerMember.setRole(GroupRoleEnum.OWNER.getRole());
        ownerMember.setNicknameInGroup("");
        ownerMember.setJoinTime(now);
        ownerMember.setStatus(GroupMemberStatusEnum.NORMAL.getStatus());
        groupMemberMapper.insert(ownerMember);

        // 4. 添加初始成员
        List<String> allMemberIds = new ArrayList<>();
        allMemberIds.add(creatorUserId);
        if (memberIds != null) {
            for (String memberId : memberIds) {
                if (StringTools.isEmpty(memberId) || memberId.equals(creatorUserId)) {
                    continue;
                }
                GroupMember member = new GroupMember();
                member.setGroupId(groupId);
                member.setUserId(memberId);
                member.setRole(GroupRoleEnum.MEMBER.getRole());
                member.setNicknameInGroup("");
                member.setJoinTime(now);
                member.setStatus(GroupMemberStatusEnum.NORMAL.getStatus());
                groupMemberMapper.insert(member);
                allMemberIds.add(memberId);
            }
        }

        // 5. 为每个成员创建群聊会话（复用 ChatSessionService.createSession）
        //    createSession 内部已处理：会话去重 + insert + 通过 sessionManager.sendMessage 推送 SESSION_MESSAGE
        for (String userId : allMemberIds) {
            chatSessionService.createSession(userId, groupId, UserContactTypeEnum.GROUP.getType(),
                    groupName, null, null, null, null, null, null, null, null);
        }

        // 6. 初始化群聊 ChannelGroup，并将在线成员加入（离线成员上线时由 SessionManager.bind 自动加入）
        sessionManager.createGroup(groupId);
        for (String userId : allMemberIds) {
            sessionManager.joinGroup(userId, groupId);
        }

        logger.info("群聊创建成功: groupId={}, groupName={}, owner={}, memberCount={}",
                groupId, groupName, creatorUserId, allMemberIds.size());

        return groupInfo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMember(String groupId, String inviterUserId, List<String> memberIds) {
        GroupInfo groupInfo = getGroupInfoOrThrow(groupId);
        checkGroupNotDissolved(groupInfo);

        // 权限校验
        GroupMember inviter = getMemberOrThrow(groupId, inviterUserId);
        if (inviter.getStatus() != GroupMemberStatusEnum.NORMAL.getStatus()) {
            throw new BusinessException("您不在该群聊中");
        }
        if (groupInfo.getInvitePermission() == 1) {
            // 仅群主/管理员可邀请
            if (!GroupRoleEnum.OWNER.getRole().equals(inviter.getRole()) && !GroupRoleEnum.ADMIN.getRole().equals(inviter.getRole())) {
                throw new BusinessException("仅群主和管理员可以邀请成员");
            }
        }

        if (memberIds == null || memberIds.isEmpty()) {
            throw new BusinessException("请选择要邀请的成员");
        }

        long now = System.currentTimeMillis();
        List<String> newMemberIds = new ArrayList<>();

        for (String memberId : memberIds) {
            if (StringTools.isEmpty(memberId) || memberId.equals(inviterUserId)) {
                continue;
            }
            // 检查是否已在群中
            GroupMember existing = (GroupMember) groupMemberMapper.selectByGroupIdAndUserId(groupId, memberId);
            if (existing != null && existing.getStatus() == GroupMemberStatusEnum.NORMAL.getStatus()) {
                continue; // 已在群中，跳过
            }

            GroupMember member = new GroupMember();
            member.setGroupId(groupId);
            member.setUserId(memberId);
            member.setRole(GroupRoleEnum.MEMBER.getRole());
            member.setNicknameInGroup("");
            member.setJoinTime(now);
            member.setStatus(GroupMemberStatusEnum.NORMAL.getStatus());
            if (existing != null) {
                groupMemberMapper.updateByGroupIdAndUserId(member, groupId, memberId);
            } else {
                groupMemberMapper.insert(member);
            }

            // 为新成员创建群聊会话（复用 createSession）
            chatSessionService.createSession(memberId, groupId, UserContactTypeEnum.GROUP.getType(),
                    groupInfo.getGroupName(), null, null, null, null, null, null, null, null);

            newMemberIds.add(memberId);
        }

        if (newMemberIds.isEmpty()) {
            throw new BusinessException("所选成员已在群中");
        }

        // 新成员加入 ChannelGroup（在线的加入成功；离线的上线时由 bind 自动加入）
        for (String memberId : newMemberIds) {
            sessionManager.joinGroup(memberId, groupId);
        }

        // 广播 GROUP_MEMBER_JOIN 事件给所有群成员（sendToGroup，含新加入的在线成员）
        List<GroupMember> allMembers = getGroupMemberList(groupId);
        MessageSendDto dto = buildGroupEventDto(MessageTypeEnum.GROUP_MEMBER_JOIN, groupId, now);
        Map<String, Object> ext = new HashMap<>();
        ext.put("groupId", groupId);
        ext.put("newMemberIds", newMemberIds);
        ext.put("groupInfo", groupInfo);
        ext.put("members", allMembers);
        dto.setExtendData(JsonUtils.convertObj2Json(ext));
        sessionManager.sendMessage(dto);

        logger.info("邀请成员入群: groupId={}, inviter={}, newMembers={}", groupId, inviterUserId, newMemberIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickMember(String groupId, String operatorUserId, String targetUserId) {
        GroupInfo groupInfo = getGroupInfoOrThrow(groupId);
        checkGroupNotDissolved(groupInfo);

        // 权限校验
        GroupMember operator = getMemberOrThrow(groupId, operatorUserId);
        if (!GroupRoleEnum.OWNER.getRole().equals(operator.getRole()) && !GroupRoleEnum.ADMIN.getRole().equals(operator.getRole())) {
            throw new BusinessException("仅群主和管理员可以踢出成员");
        }

        GroupMember target = getMemberOrThrow(groupId, targetUserId);
        if (GroupRoleEnum.OWNER.getRole().equals(target.getRole())) {
            throw new BusinessException("不能踢出群主");
        }
        if (GroupRoleEnum.ADMIN.getRole().equals(target.getRole()) && !GroupRoleEnum.OWNER.getRole().equals(operator.getRole())) {
            throw new BusinessException("管理员不能踢出其他管理员");
        }

        // 更新成员状态为被踢出
        groupMemberMapper.updateStatus(groupId, targetUserId, GroupMemberStatusEnum.KICKED.getStatus());

        long now = System.currentTimeMillis();

        // 广播 GROUP_MEMBER_KICKED 事件（先广播再移除，被踢者仍能收到通知以删除本地会话）
        MessageSendDto dto = buildGroupEventDto(MessageTypeEnum.GROUP_MEMBER_KICKED, groupId, now);
        Map<String, Object> ext = new HashMap<>();
        ext.put("groupId", groupId);
        ext.put("targetUserId", targetUserId);
        ext.put("operatorUserId", operatorUserId);
        dto.setExtendData(JsonUtils.convertObj2Json(ext));
        sessionManager.sendMessage(dto);

        // 将被踢者移出 ChannelGroup，并删除其群聊会话（离线上线后 syncSessions 也拉不到）
        sessionManager.leaveGroup(targetUserId, groupId);
        chatSessionMapper.deleteByUserIdAndTargetUserId(targetUserId, groupId);

        logger.info("踢出群成员: groupId={}, operator={}, target={}", groupId, operatorUserId, targetUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAdmin(String groupId, String operatorUserId, String targetUserId, boolean isAdmin) {
        GroupInfo groupInfo = getGroupInfoOrThrow(groupId);
        checkGroupNotDissolved(groupInfo);

        // 权限校验：仅群主可操作
        GroupMember operator = getMemberOrThrow(groupId, operatorUserId);
        if (!GroupRoleEnum.OWNER.getRole().equals(operator.getRole())) {
            throw new BusinessException("仅群主可以设置管理员");
        }

        GroupMember target = getMemberOrThrow(groupId, targetUserId);
        if (GroupRoleEnum.OWNER.getRole().equals(target.getRole())) {
            throw new BusinessException("不能修改群主的角色");
        }

        String newRole = isAdmin ? GroupRoleEnum.ADMIN.getRole() : GroupRoleEnum.MEMBER.getRole();
        groupMemberMapper.updateRole(groupId, targetUserId, newRole);

        long now = System.currentTimeMillis();

        // 广播 GROUP_ROLE_CHANGE 事件给所有群成员（sendToGroup）
        MessageSendDto dto = buildGroupEventDto(MessageTypeEnum.GROUP_ROLE_CHANGE, groupId, now);
        Map<String, Object> ext = new HashMap<>();
        ext.put("groupId", groupId);
        ext.put("targetUserId", targetUserId);
        ext.put("role", newRole);
        ext.put("operatorUserId", operatorUserId);
        dto.setExtendData(JsonUtils.convertObj2Json(ext));
        sessionManager.sendMessage(dto);

        logger.info("角色变更: groupId={}, target={}, newRole={}", groupId, targetUserId, newRole);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroupSettings(String groupId, String operatorUserId, String groupName,
                                     String groupAvatar, String announcement, Integer invitePermission) {
        GroupInfo groupInfo = getGroupInfoOrThrow(groupId);
        checkGroupNotDissolved(groupInfo);

        // 权限校验
        GroupMember operator = getMemberOrThrow(groupId, operatorUserId);
        if (!GroupRoleEnum.OWNER.getRole().equals(operator.getRole()) && !GroupRoleEnum.ADMIN.getRole().equals(operator.getRole())) {
            throw new BusinessException("仅群主和管理员可以修改群设置");
        }

        long now = System.currentTimeMillis();
        GroupInfo update = new GroupInfo();
        if (groupName != null) update.setGroupName(groupName);
        if (groupAvatar != null) update.setGroupAvatar(groupAvatar);
        if (announcement != null) update.setAnnouncement(announcement);
        if (invitePermission != null) update.setInvitePermission(invitePermission);
        update.setUpdateTime(now);
        groupInfoMapper.updateByGroupId(update, groupId);

        // 同步更新所有成员的会话中的群名
        if (groupName != null && !groupName.isEmpty()) {
            ChatSession sessionUpdate = new ChatSession();
            sessionUpdate.setTargetNickName(groupName);
            // 更新所有该群会话的 targetNickName
            List<GroupMember> allMembers = getGroupMemberList(groupId);
            for (GroupMember m : allMembers) {
                chatSessionMapper.updateByUserIdAndTargetUserId(sessionUpdate, m.getUserId(), groupId);
            }
        }

        // 广播 GROUP_SETTINGS_UPDATE 事件给所有群成员（sendToGroup）
        GroupInfo updated = (GroupInfo) groupInfoMapper.selectByGroupId(groupId);
        MessageSendDto dto = buildGroupEventDto(MessageTypeEnum.GROUP_SETTINGS_UPDATE, groupId, now);
        Map<String, Object> ext = new HashMap<>();
        ext.put("groupId", groupId);
        ext.put("groupInfo", updated);
        ext.put("operatorUserId", operatorUserId);
        dto.setExtendData(JsonUtils.convertObj2Json(ext));
        sessionManager.sendMessage(dto);

        logger.info("群设置更新: groupId={}, operator={}", groupId, operatorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dissolveGroup(String groupId, String operatorUserId) {
        GroupInfo groupInfo = getGroupInfoOrThrow(groupId);

        // 权限校验：仅群主可解散
        if (!groupInfo.getOwnerUserId().equals(operatorUserId)) {
            throw new BusinessException("仅群主可以解散群聊");
        }

        long now = System.currentTimeMillis();

        // 更新群状态为已解散
        GroupInfo update = new GroupInfo();
        update.setStatus(GroupStatusEnum.DISSOLVED.getStatus());
        update.setUpdateTime(now);
        groupInfoMapper.updateByGroupId(update, groupId);

        // 广播 GROUP_DISSOLVED 事件（先广播，让在线成员收到通知并标记本地群状态）
        MessageSendDto dto = buildGroupEventDto(MessageTypeEnum.GROUP_DISSOLVED, groupId, now);
        Map<String, Object> ext = new HashMap<>();
        ext.put("groupId", groupId);
        ext.put("operatorUserId", operatorUserId);
        dto.setExtendData(JsonUtils.convertObj2Json(ext));
        sessionManager.sendMessage(dto);

        // 清空并移除 ChannelGroup，删除所有成员的该群会话（群聊会话 sessionId=groupId 共享，一次删除）
        sessionManager.dissolveGroup(groupId);
        chatSessionMapper.deleteBySessionId(groupId);

        logger.info("群聊已解散: groupId={}, owner={}", groupId, operatorUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveGroup(String groupId, String userId) {
        GroupInfo groupInfo = getGroupInfoOrThrow(groupId);
        checkGroupNotDissolved(groupInfo);

        GroupMember member = getMemberOrThrow(groupId, userId);
        if (GroupRoleEnum.OWNER.getRole().equals(member.getRole())) {
            throw new BusinessException("群主不能退出群聊，请先解散群聊");
        }

        groupMemberMapper.updateStatus(groupId, userId, GroupMemberStatusEnum.LEFT.getStatus());

        long now = System.currentTimeMillis();

        // 广播 GROUP_MEMBER_LEAVE 事件（先广播再移除，退群者仍能收到通知以删除本地会话）
        MessageSendDto dto = buildGroupEventDto(MessageTypeEnum.GROUP_MEMBER_LEAVE, groupId, now);
        Map<String, Object> ext = new HashMap<>();
        ext.put("groupId", groupId);
        ext.put("userId", userId);
        dto.setExtendData(JsonUtils.convertObj2Json(ext));
        sessionManager.sendMessage(dto);

        // 将退群者移出 ChannelGroup，并删除其群聊会话
        sessionManager.leaveGroup(userId, groupId);
        chatSessionMapper.deleteByUserIdAndTargetUserId(userId, groupId);

        logger.info("成员退出群聊: groupId={}, userId={}", groupId, userId);
    }

    @Override
    public GroupInfo getGroupInfo(String groupId) {
        if (StringTools.isEmpty(groupId)) {
            return null;
        }
        return (GroupInfo) groupInfoMapper.selectByGroupId(groupId);
    }

    @Override
    public List<GroupMember> getGroupMembers(String groupId) {
        if (StringTools.isEmpty(groupId)) {
            return new ArrayList<>();
        }
        return getGroupMemberList(groupId);
    }

    /**
     * 获取我创建的群聊列表（role=OWNER，群未解散）
     */
    @Override
    public List<GroupInfo> getMyCreatedGroups(String userId) {
        return groupMemberMapper.selectGroupsByUserId(userId, true);
    }

    /**
     * 获取我加入的群聊列表（role!=OWNER，群未解散）
     */
    @Override
    public List<GroupInfo> getMyJoinedGroups(String userId) {
        return groupMemberMapper.selectGroupsByUserId(userId, false);
    }

    // ==================== 管理员功能 ====================

    /**
     * 分页查询群聊列表（管理员）
     */
    @Override
    public PaginationResultVO<GroupInfo> findListByPage(GroupInfoQuery param) {
        Integer count = groupInfoMapper.selectCount(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();
        SimplePage page = new SimplePage(param.getPageNo(), count == null ? 0 : count, pageSize);
        param.setSimplePage(page);
        List<GroupInfo> list = groupInfoMapper.selectList(param);
        return new PaginationResultVO<>(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
    }

    /**
     * 管理员封禁/解封群聊
     * 封禁：群状态置为已封禁、广播 GROUP_BANNED 事件、清空群 ChannelGroup（成员无法收发该群消息，会话数据保留）
     * 解封：群状态恢复为正常、将在线成员重新加入群 ChannelGroup
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void manageGroup(String groupId, Integer status) {
        GroupInfo groupInfo = getGroupInfoOrThrow(groupId);
        if (GroupStatusEnum.DISSOLVED.getStatus().equals(groupInfo.getStatus())) {
            throw new BusinessException("该群聊已解散，无法进行此操作");
        }
        if (!GroupStatusEnum.ACTIVE.getStatus().equals(status)
                && !GroupStatusEnum.BANNED.getStatus().equals(status)) {
            throw new BusinessException("状态不合法，仅支持封禁(2)/解封(0)");
        }
        if (status.equals(groupInfo.getStatus())) {
            throw new BusinessException(GroupStatusEnum.BANNED.getStatus().equals(status) ? "该群聊已被封禁" : "该群聊已是正常状态");
        }

        long now = System.currentTimeMillis();
        GroupInfo update = new GroupInfo();
        update.setStatus(status);
        update.setUpdateTime(now);
        groupInfoMapper.updateByGroupId(update, groupId);

        if (GroupStatusEnum.BANNED.getStatus().equals(status)) {
            // 封禁：先广播事件（在线成员收到通知），再清空 ChannelGroup
            MessageSendDto dto = buildGroupEventDto(MessageTypeEnum.GROUP_BANNED, groupId, now);
            Map<String, Object> ext = new HashMap<>();
            ext.put("groupId", groupId);
            ext.put("status", status);
            ext.put("operator", "ADMIN");
            dto.setExtendData(JsonUtils.convertObj2Json(ext));
            sessionManager.sendMessage(dto);
            sessionManager.dissolveGroup(groupId);
            logger.info("管理员封禁群聊: groupId={}", groupId);
        } else {
            // 解封：重建 ChannelGroup，将在线成员重新加入
            sessionManager.createGroup(groupId);
            List<GroupMember> members = getGroupMemberList(groupId);
            for (GroupMember member : members) {
                sessionManager.joinGroup(member.getUserId(), groupId);
            }
            logger.info("管理员解封群聊: groupId={}, 已重新加入在线成员 {} 人", groupId, members.size());
        }
    }

    // ==================== 私有辅助方法 ====================

    private GroupInfo getGroupInfoOrThrow(String groupId) {
        GroupInfo info = (GroupInfo) groupInfoMapper.selectByGroupId(groupId);
        if (info == null) {
            throw new BusinessException("群聊不存在");
        }
        return info;
    }

    private void checkGroupNotDissolved(GroupInfo groupInfo) {
        if (groupInfo.getStatus() == null) {
            return;
        }
        if (GroupStatusEnum.DISSOLVED.getStatus().equals(groupInfo.getStatus())) {
            throw new BusinessException("该群聊已解散");
        }
        if (GroupStatusEnum.BANNED.getStatus().equals(groupInfo.getStatus())) {
            throw new BusinessException("该群聊已被封禁");
        }
    }

    private GroupMember getMemberOrThrow(String groupId, String userId) {
        GroupMember member = (GroupMember) groupMemberMapper.selectByGroupIdAndUserId(groupId, userId);
        if (member == null) {
            throw new BusinessException("您不是该群聊的成员");
        }
        return member;
    }

    @SuppressWarnings("unchecked")
    private List<GroupMember> getGroupMemberList(String groupId) {
        return (List<GroupMember>) groupMemberMapper.selectActiveMembersByGroupId(groupId);
    }

    /**
     * 构建群聊系统事件消息（广播形式）
     * receiveUserId = groupId 且 messageSend2Type = GROUP
     * → sessionManager.sendMessage 走 sendToGroup，一次广播给所有在线成员
     */
    private MessageSendDto buildGroupEventDto(MessageTypeEnum messageType, String groupId, Long sendTime) {
        MessageSendDto dto = new MessageSendDto();
        dto.setSessionId(groupId);
        dto.setSendUserId(null);
        dto.setReceiveUserId(groupId);
        dto.setSendTime(sendTime);
        dto.setMessageContent(messageType.getDesc());
        dto.setMessageType(messageType.getType());
        dto.setMessageTypeIm(MessageTypeImEnum.SESSION.getType());
        dto.setMessageSend2Type(UserContactTypeEnum.GROUP.getType());
        return dto;
    }
}
