package com.meetchat.Controller;

import java.util.List;

import com.meetchat.Controller.ABaseController;
import com.meetchat.annotation.GlobalInterceptor;
import com.meetchat.entity.Dto.FriendWithUserInfoDto;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.enums.ContactRequestStatusEnum;
import com.meetchat.entity.po.FriendRequest;
import com.meetchat.entity.query.FriendRequestQuery;
import com.meetchat.entity.query.UserFriendQuery;
import com.meetchat.entity.po.UserFriend;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.entity.vo.UserInfoVo;
import com.meetchat.mappers.FriendRequestMapper;
import com.meetchat.mappers.UserFriendMapper;
import com.meetchat.service.UserFriendService;
import com.meetchat.service.impl.FriendRequestServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * 好友关系表 Controller
 */
@RestController("userFriendController")
@RequestMapping("/userFriend")
public class UserFriendController extends ABaseController {

	@Resource
	private UserFriendService userFriendService;
    @Resource
    private FriendRequestServiceImpl friendRequestService;
    @Resource
    private UserFriendMapper<UserFriend,UserFriendQuery>userFriendMapper;
    @Resource
    private FriendRequestMapper <FriendRequest,FriendRequestQuery>friendRequestMapper;



	@RequestMapping("/search")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO search(HttpServletRequest request, String contactId) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		 UserInfoVo userInfoVo=friendRequestService.search(tokenUserInfoDto.getUserId(), contactId);
		 return getSuccessResponseVO(userInfoVo);
	}

	//申请好友请求
	@RequestMapping("/addFriend")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO addFriend(HttpServletRequest request, String contactId,String remark) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		friendRequestService.addFriend(tokenUserInfoDto.getUserId(), contactId,remark);
		return getSuccessResponseVO(null);
	}
	//获取好友列表
	@RequestMapping("/getFriendList")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO getFriendList(HttpServletRequest request) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		UserFriendQuery query = new UserFriendQuery();
		query.setUserId(tokenUserInfoDto.getUserId());
		List<UserFriend> list = userFriendMapper.selectList(query);
		return getSuccessResponseVO(list);
	}

	/**
	 * 获取非机器人好友列表（用于创建群聊时选择成员）
	 * 通过单条 SQL JOIN 查询，排除已建立机器人会话的好友，并附带用户昵称
	 */
	@RequestMapping("/getNonBotFriends")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO getNonBotFriends(HttpServletRequest request) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		String userId = tokenUserInfoDto.getUserId();
		List<FriendWithUserInfoDto> result = userFriendMapper.selectNonBotFriends(userId);
		return getSuccessResponseVO(result);
	}
	//获取好友请求列表
	@RequestMapping("/getFriendRequestList")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO getFriendRequestList(HttpServletRequest request) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        String userId=tokenUserInfoDto.getUserId();
		FriendRequestQuery friendRequestQuery = new FriendRequestQuery();
		friendRequestQuery.setTargetUserId(userId);
		friendRequestQuery.setStatus(ContactRequestStatusEnum.TO_DEAL.getStatus());
		List<FriendRequest> list = friendRequestMapper.selectList(friendRequestQuery);

		return getSuccessResponseVO(list);
	}
  //获取自己申请列表
	@RequestMapping("/getMyFriendRequest")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO getMyFriendRequest(HttpServletRequest request) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		String userId= tokenUserInfoDto.getUserId();
		FriendRequestQuery friendRequestQuery = new FriendRequestQuery();
		friendRequestQuery.setRequestUserId(userId);
		List<FriendRequest> list = friendRequestMapper.selectList(friendRequestQuery);
		return getSuccessResponseVO(list);
	}
	//管理好友请求列表
	@RequestMapping("/manageRequest")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO manageRequest(HttpServletRequest request,String contactId,Integer status) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        friendRequestService.manageRequest(tokenUserInfoDto.getUserId(), contactId,status);
        return getSuccessResponseVO(null);
	}
//管理自己的申请
	@RequestMapping("/manageMyRequest")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO manageMyRequest(HttpServletRequest request,String contactId,Integer status) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		friendRequestService.manageMyRequest(tokenUserInfoDto.getUserId(), contactId,status);
		return getSuccessResponseVO(null);
	}
	//删除好友
	@RequestMapping("/deleteFriend")
	@GlobalInterceptor(checkLogin = true)
	public ResponseVO deleteFriend(HttpServletRequest request, String contactId) {
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
		userFriendService.deleteFriend(tokenUserInfoDto.getUserId(), contactId);
		return getSuccessResponseVO(null);
	}
	//获取自己申请列表
	//管理自己申请列表
	//接受或拒绝好友请求
	//删除好友

}