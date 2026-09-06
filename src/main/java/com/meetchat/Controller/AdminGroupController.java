package com.meetchat.Controller;

import com.meetchat.annotation.GlobalInterceptor;
import com.meetchat.entity.po.GroupInfo;
import com.meetchat.entity.po.GroupMember;
import com.meetchat.entity.po.UserInfo;
import com.meetchat.entity.query.GroupInfoQuery;
import com.meetchat.entity.query.UserInfoQuery;
import com.meetchat.entity.vo.AdminGroupMemberVo;
import com.meetchat.entity.vo.AdminGroupVo;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.exception.BusinessException;
import com.meetchat.mappers.GroupMemberMapper;
import com.meetchat.mappers.UserInfoMapper;
import com.meetchat.service.GroupService;
import com.meetchat.utils.StringTools;
import com.meetchat.webSocket.SessionManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员群聊管理 Controller
 */
@RestController
@RequestMapping("/admin/group")
public class AdminGroupController extends ABaseController {

    @Resource
    private GroupService groupService;

    @Resource
    private GroupMemberMapper<GroupMember, Object> groupMemberMapper;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;

    @Resource
    private SessionManager sessionManager;

    /**
     * 分页查询群聊列表（附带成员数、群主昵称、在线成员数）
     * 支持按群名模糊、状态、群主ID筛选
     */
    @RequestMapping("/groupList")
    @GlobalInterceptor(checkAdmin = true, checkLogin = true)
    public ResponseVO groupList(GroupInfoQuery groupInfoQuery) {
        if (StringTools.isEmpty(groupInfoQuery.getOrderBy())) {
            groupInfoQuery.setOrderBy("create_time desc");
        }
        PaginationResultVO<GroupInfo> resultVO = groupService.findListByPage(groupInfoQuery);
        List<GroupInfo> groupList = resultVO.getList();
        List<AdminGroupVo> voList = new ArrayList<>();
        if (groupList != null && !groupList.isEmpty()) {
            // 批量查询成员数
            List<String> groupIds = new ArrayList<>();
            for (GroupInfo g : groupList) {
                groupIds.add(g.getGroupId());
            }
            Map<String, Integer> memberCountMap = new HashMap<>();
            List<Map<String, Object>> countList = groupMemberMapper.countActiveMembersByGroupIds(groupIds);
            if (countList != null) {
                for (Map<String, Object> row : countList) {
                    memberCountMap.put(String.valueOf(row.get("groupId")),
                            ((Number) row.get("memberCount")).intValue());
                }
            }
            for (GroupInfo g : groupList) {
                AdminGroupVo vo = new AdminGroupVo();
                vo.setGroupId(g.getGroupId());
                vo.setGroupName(g.getGroupName());
                vo.setGroupAvatar(g.getGroupAvatar());
                vo.setOwnerUserId(g.getOwnerUserId());
                vo.setInvitePermission(g.getInvitePermission());
                vo.setAnnouncement(g.getAnnouncement());
                vo.setStatus(g.getStatus());
                vo.setCreateTime(g.getCreateTime());
                vo.setUpdateTime(g.getUpdateTime());
                vo.setMemberCount(memberCountMap.getOrDefault(g.getGroupId(), 0));
                // 群主昵称
                UserInfo owner = userInfoMapper.selectByUserId(g.getOwnerUserId());
                vo.setOwnerNickName(owner != null ? owner.getNickName() : g.getOwnerUserId());
                // 在线成员数
                int onlineCount = 0;
                List<GroupMember> members = groupMemberMapper.selectActiveMembersByGroupId(g.getGroupId());
                if (members != null) {
                    for (GroupMember m : members) {
                        if (sessionManager.isOnline(m.getUserId())) {
                            onlineCount++;
                        }
                    }
                }
                vo.setOnlineMemberCount(onlineCount);
                voList.add(vo);
            }
        }
        PaginationResultVO<AdminGroupVo> voResult = new PaginationResultVO<>(
                resultVO.getTotalCount(), resultVO.getPageSize(), resultVO.getPageNo(),
                resultVO.getPageTotal(), voList);
        return getSuccessResponseVO(voResult);
    }

    /**
     * 查询群聊详情：群信息 + 成员列表（附带用户昵称与在线状态）
     */
    @RequestMapping("/groupDetail")
    @GlobalInterceptor(checkAdmin = true, checkLogin = true)
    public ResponseVO groupDetail(String groupId, HttpServletRequest request) {
        if (StringTools.isEmpty(groupId)) {
            return getBusinessErrorResponseVO(new BusinessException("群ID不能为空"), null);
        }
        GroupInfo groupInfo = groupService.getGroupInfo(groupId);
        if (groupInfo == null) {
            return getBusinessErrorResponseVO(new BusinessException("群聊不存在"), null);
        }

        List<GroupMember> members = groupService.getGroupMembers(groupId);
        List<AdminGroupMemberVo> memberVos = new ArrayList<>();
        int onlineCount = 0;
        if (members != null) {
            for (GroupMember m : members) {
                AdminGroupMemberVo vo = new AdminGroupMemberVo();
                vo.setId(m.getId());
                vo.setGroupId(m.getGroupId());
                vo.setUserId(m.getUserId());
                vo.setRole(m.getRole());
                vo.setNicknameInGroup(m.getNicknameInGroup());
                vo.setJoinTime(m.getJoinTime());
                vo.setStatus(m.getStatus());
                boolean online = sessionManager.isOnline(m.getUserId());
                vo.setOnline(online);
                if (online) {
                    onlineCount++;
                }
                UserInfo user = userInfoMapper.selectByUserId(m.getUserId());
                vo.setNickName(user != null ? user.getNickName() : m.getUserId());
                memberVos.add(vo);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("groupInfo", groupInfo);
        result.put("memberCount", members == null ? 0 : members.size());
        result.put("onlineMemberCount", onlineCount);
        result.put("members", memberVos);
        return getSuccessResponseVO(result);
    }

    /**
     * 封禁/解封群聊（status：0-正常/解封，2-已封禁）
     * 封禁后：群内无法收发消息，在线成员收到 GROUP_BANNED 通知；解封后自动恢复在线成员的群消息接收
     */
    @RequestMapping("/manageGroup")
    @GlobalInterceptor(checkAdmin = true, checkLogin = true)
    public ResponseVO manageGroup(String groupId, Integer status, HttpServletRequest request) {
        if (StringTools.isEmpty(groupId)) {
            return getBusinessErrorResponseVO(new BusinessException("群ID不能为空"), null);
        }
        groupService.manageGroup(groupId, status);
        return getSuccessResponseVO(null);
    }
}
