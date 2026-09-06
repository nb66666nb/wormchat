package com.meetchat.Controller;

import com.meetchat.annotation.GlobalInterceptor;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.po.UserInfo;
import com.meetchat.entity.query.UserInfoQuery;
import com.meetchat.entity.vo.AdminUserInfoVo;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.mappers.UserInfoMapper;
import com.meetchat.service.impl.UserInfoServiceImpl;
import com.meetchat.webSocket.SessionManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminUserController extends ABaseController {
    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    @Resource
    private UserInfoServiceImpl userInfoService;
    @Resource
    private SessionManager sessionManager;

    /**
     * 分页查询用户列表（附带在线状态）
     */
    @RequestMapping("/userList")
    @GlobalInterceptor(checkAdmin = true, checkLogin = true)
    public ResponseVO getUserList(UserInfoQuery userInfoQuery) {
        userInfoQuery.setOrderBy("create_time desc");
        PaginationResultVO<UserInfo> resultVO = userInfoService.findListByPage(userInfoQuery);
        List<AdminUserInfoVo> voList = new ArrayList<>();
        if (resultVO.getList() != null) {
            for (UserInfo userInfo : resultVO.getList()) {
                AdminUserInfoVo vo = new AdminUserInfoVo();
                vo.setUserId(userInfo.getUserId());
                vo.setNickName(userInfo.getNickName());
                vo.setEmail(userInfo.getEmail());
                vo.setSex(userInfo.getSex());
                vo.setStatus(userInfo.getStatus());
                vo.setLastLoginTime(userInfo.getLastLoginTime());
                vo.setLastOffTime(userInfo.getLastOffTime());
                vo.setMeetingNo(userInfo.getMeetingNo());
                vo.setCreateTime(userInfo.getCreateTime());
                vo.setOnline(sessionManager.isOnline(userInfo.getUserId()));
                voList.add(vo);
            }
        }
        PaginationResultVO<AdminUserInfoVo> voResult = new PaginationResultVO<>(
                resultVO.getTotalCount(), resultVO.getPageSize(), resultVO.getPageNo(),
                resultVO.getPageTotal(), voList);
        return getSuccessResponseVO(voResult);
    }

    /**
     * 查询单个用户在线状态
     */
    @RequestMapping("/checkOnline")
    @GlobalInterceptor(checkAdmin = true, checkLogin = true)
    public ResponseVO checkOnline(String userId, HttpServletRequest request) {
        UserInfo userInfo = userInfoMapper.selectByUserId(userId);
        if (userInfo == null) {
            return getBusinessErrorResponseVO(new com.meetchat.exception.BusinessException("用户不存在"), null);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("online", sessionManager.isOnline(userId));
        return getSuccessResponseVO(result);
    }

    /**
     * 查询当前在线用户数及在线用户ID列表
     */
    @RequestMapping("/onlineUsers")
    @GlobalInterceptor(checkAdmin = true, checkLogin = true)
    public ResponseVO onlineUsers(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        result.put("onlineCount", sessionManager.getOnlineCount());
        result.put("onlineUserIds", sessionManager.getOnlineUserIds());
        return getSuccessResponseVO(result);
    }

    /**
     * 封禁/解封账号（status：0-禁用，1-正常）
     * 封禁时同时清除登录态并强制下线
     */
    @RequestMapping("/mangeUser")
    @GlobalInterceptor(checkAdmin = true, checkLogin = true)
    public ResponseVO manageUser(String userId, HttpServletRequest request, Integer status) {
        checkNotSelf(request, userId, "不能封禁/解封自己");
        userInfoService.manageUser(userId, status);
        return getSuccessResponseVO(null);
    }

    /**
     * 强制用户下线（发下线通知 + 清除登录态 + 断开 WebSocket）
     */
    @RequestMapping("/kickUser")
    @GlobalInterceptor(checkAdmin = true, checkLogin = true)
    public ResponseVO kickUser(String userId, HttpServletRequest request) {
        checkNotSelf(request, userId, "不能强制自己下线");
        userInfoService.forceOffline(userId);
        return getSuccessResponseVO(null);
    }

    /**
     * 自我保护：管理员不能对自己执行封禁/下线操作
     */
    private void checkNotSelf(HttpServletRequest request, String targetUserId, String msg) {
        TokenUserInfoDto current = getTokenUserInfo(request);
        if (current != null && current.getUserId() != null && current.getUserId().equals(targetUserId)) {
            throw new com.meetchat.exception.BusinessException(msg);
        }
    }
}
