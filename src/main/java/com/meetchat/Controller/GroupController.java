package com.meetchat.Controller;

import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.exception.BusinessException;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.service.GroupService;
import com.meetchat.utils.StringTools;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 群聊管理 Controller
 */
@RestController
@RequestMapping("/group")
public class GroupController extends ABaseController {

    @Resource
    private GroupService groupService;

    /**
     * 创建群聊
     */
    @RequestMapping("/create")
    public ResponseVO create(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            return getBusinessErrorResponseVO(new BusinessException("请先登录"), null);
        }
        String groupName = (String) params.get("groupName");
        String creatorUserId = tokenUserInfoDto.getUserId();
        if (StringTools.isEmpty(creatorUserId)) {
            creatorUserId = (String) params.get("creatorUserId");
        }
        @SuppressWarnings("unchecked")
        List<String> memberIds = (List<String>) params.get("memberIds");
        String announcement = (String) params.get("announcement");
        Integer invitePermission = params.get("invitePermission") != null
                ? ((Number) params.get("invitePermission")).intValue() : null;

        if (StringTools.isEmpty(groupName)) {
            return getBusinessErrorResponseVO(new BusinessException("群名称不能为空"), null);
        }
        if (memberIds == null || memberIds.isEmpty()) {
            return getBusinessErrorResponseVO(new BusinessException("请至少选择一名群成员"), null);
        }

        try {
            Object result = groupService.createGroup(groupName, creatorUserId, memberIds,
                    announcement, invitePermission);
            return getSuccessResponseVO(result);
        } catch (BusinessException e) {
            return getBusinessErrorResponseVO(e, null);
        }
    }

    /**
     * 邀请成员
     */
    @RequestMapping("/invite")
    public ResponseVO invite(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            return getBusinessErrorResponseVO(new BusinessException("请先登录"), null);
        }
        String groupId = (String) params.get("groupId");
        String inviterUserId = tokenUserInfoDto.getUserId();
        if (StringTools.isEmpty(inviterUserId)) {
            inviterUserId = (String) params.get("inviterUserId");
        }
        @SuppressWarnings("unchecked")
        List<String> memberIds = (List<String>) params.get("memberIds");

        if (StringTools.isEmpty(groupId)) {
            return getBusinessErrorResponseVO(new BusinessException("群ID不能为空"), null);
        }
        if (memberIds == null || memberIds.isEmpty()) {
            return getBusinessErrorResponseVO(new BusinessException("请选择要邀请的成员"), null);
        }

        try {
            groupService.inviteMember(groupId, inviterUserId, memberIds);
            return getSuccessResponseVO(null);
        } catch (BusinessException e) {
            return getBusinessErrorResponseVO(e, null);
        }
    }

    /**
     * 踢出成员
     */
    @RequestMapping("/kick")
    public ResponseVO kick(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            return getBusinessErrorResponseVO(new BusinessException("请先登录"), null);
        }
        String groupId = (String) params.get("groupId");
        String operatorUserId = tokenUserInfoDto.getUserId();
        if (StringTools.isEmpty(operatorUserId)) {
            operatorUserId = (String) params.get("operatorUserId");
        }
        String targetUserId = (String) params.get("targetUserId");

        if (StringTools.isEmpty(groupId) || StringTools.isEmpty(targetUserId)) {
            return getBusinessErrorResponseVO(new BusinessException("群ID和目标用户ID不能为空"), null);
        }

        try {
            groupService.kickMember(groupId, operatorUserId, targetUserId);
            return getSuccessResponseVO(null);
        } catch (BusinessException e) {
            return getBusinessErrorResponseVO(e, null);
        }
    }

    /**
     * 设置/取消管理员
     */
    @RequestMapping("/setAdmin")
    public ResponseVO setAdmin(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            return getBusinessErrorResponseVO(new BusinessException("请先登录"), null);
        }
        String groupId = (String) params.get("groupId");
        String operatorUserId = tokenUserInfoDto.getUserId();
        if (StringTools.isEmpty(operatorUserId)) {
            operatorUserId = (String) params.get("operatorUserId");
        }
        String targetUserId = (String) params.get("targetUserId");
        Boolean isAdmin = (Boolean) params.get("isAdmin");

        if (StringTools.isEmpty(groupId) || StringTools.isEmpty(targetUserId) || isAdmin == null) {
            return getBusinessErrorResponseVO(new BusinessException("参数不完整"), null);
        }

        try {
            groupService.setAdmin(groupId, operatorUserId, targetUserId, isAdmin);
            return getSuccessResponseVO(null);
        } catch (BusinessException e) {
            return getBusinessErrorResponseVO(e, null);
        }
    }

    /**
     * 更新群设置
     */
    @RequestMapping("/updateSettings")
    public ResponseVO updateSettings(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            return getBusinessErrorResponseVO(new BusinessException("请先登录"), null);
        }
        String groupId = (String) params.get("groupId");
        String operatorUserId = tokenUserInfoDto.getUserId();
        if (StringTools.isEmpty(operatorUserId)) {
            operatorUserId = (String) params.get("operatorUserId");
        }
        String groupName = (String) params.get("groupName");
        String groupAvatar = (String) params.get("groupAvatar");
        String announcement = (String) params.get("announcement");
        Integer invitePermission = params.get("invitePermission") != null
                ? ((Number) params.get("invitePermission")).intValue() : null;

        if (StringTools.isEmpty(groupId)) {
            return getBusinessErrorResponseVO(new BusinessException("群ID不能为空"), null);
        }

        try {
            groupService.updateGroupSettings(groupId, operatorUserId, groupName,
                    groupAvatar, announcement, invitePermission);
            return getSuccessResponseVO(null);
        } catch (BusinessException e) {
            return getBusinessErrorResponseVO(e, null);
        }
    }

    /**
     * 解散群聊
     */
    @RequestMapping("/dissolve")
    public ResponseVO dissolve(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            return getBusinessErrorResponseVO(new BusinessException("请先登录"), null);
        }
        String groupId = (String) params.get("groupId");
        String operatorUserId = tokenUserInfoDto.getUserId();
        if (StringTools.isEmpty(operatorUserId)) {
            operatorUserId = (String) params.get("operatorUserId");
        }

        if (StringTools.isEmpty(groupId)) {
            return getBusinessErrorResponseVO(new BusinessException("群ID不能为空"), null);
        }

        try {
            groupService.dissolveGroup(groupId, operatorUserId);
            return getSuccessResponseVO(null);
        } catch (BusinessException e) {
            return getBusinessErrorResponseVO(e, null);
        }
    }

    /**
     * 退出群聊
     */
    @RequestMapping("/leave")
    public ResponseVO leave(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            return getBusinessErrorResponseVO(new BusinessException("请先登录"), null);
        }
        String groupId = (String) params.get("groupId");
        String userId = tokenUserInfoDto.getUserId();
        if (StringTools.isEmpty(userId)) {
            userId = (String) params.get("userId");
        }

        if (StringTools.isEmpty(groupId)) {
            return getBusinessErrorResponseVO(new BusinessException("群ID不能为空"), null);
        }

        try {
            groupService.leaveGroup(groupId, userId);
            return getSuccessResponseVO(null);
        } catch (BusinessException e) {
            return getBusinessErrorResponseVO(e, null);
        }
    }

    /**
     * 获取群信息
     */
    @RequestMapping("/getInfo")
    public ResponseVO getInfo(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String groupId = (String) params.get("groupId");
        if (StringTools.isEmpty(groupId)) {
            return getBusinessErrorResponseVO(new BusinessException("群ID不能为空"), null);
        }
        return getSuccessResponseVO(groupService.getGroupInfo(groupId));
    }

    /**
     * 获取群成员列表
     */
    @RequestMapping("/getMembers")
    public ResponseVO getMembers(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String groupId = (String) params.get("groupId");
        if (StringTools.isEmpty(groupId)) {
            return getBusinessErrorResponseVO(new BusinessException("群ID不能为空"), null);
        }
        return getSuccessResponseVO(groupService.getGroupMembers(groupId));
    }

    /**
     * 我创建的群聊列表（role=OWNER）
     */
    @RequestMapping("/myCreated")
    public ResponseVO myCreated(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            return getBusinessErrorResponseVO(new BusinessException("请先登录"), null);
        }
        return getSuccessResponseVO(groupService.getMyCreatedGroups(tokenUserInfoDto.getUserId()));
    }

    /**
     * 我加入的群聊列表（role!=OWNER）
     */
    @RequestMapping("/myJoined")
    public ResponseVO myJoined(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            return getBusinessErrorResponseVO(new BusinessException("请先登录"), null);
        }
        return getSuccessResponseVO(groupService.getMyJoinedGroups(tokenUserInfoDto.getUserId()));
    }
}
