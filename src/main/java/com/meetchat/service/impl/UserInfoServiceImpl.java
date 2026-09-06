package com.meetchat.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.config.AppConfig;
import com.meetchat.entity.contants.Contants;
import com.meetchat.entity.enums.*;
import com.meetchat.entity.po.MeetingInfo;
import com.meetchat.entity.po.MeetingMember;
import com.meetchat.entity.query.MeetingInfoQuery;
import com.meetchat.entity.query.MeetingMemberQuery;
import com.meetchat.entity.vo.MeetingInfoUserVo;
import com.meetchat.entity.vo.UserInfoVo;
import com.meetchat.exception.BusinessException;
import com.meetchat.mappers.MeetingInfoMapper;
import com.meetchat.mappers.MeetingMemberMapper;
import com.meetchat.redis.RedisComponet;
import com.meetchat.webSocket.SessionManager;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meetchat.entity.query.UserInfoQuery;
import com.meetchat.entity.po.UserInfo;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.UserInfoMapper;
import com.meetchat.service.UserInfoService;
import com.meetchat.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 *  业务接口实现
 */
@Service("userInfoService")
public class UserInfoServiceImpl implements UserInfoService {
	private static final Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);
	@Resource
	private AppConfig appConfig;

	@Resource
	private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;
    @Resource
    private RedisComponet redisComponet;
    @Resource
    private MeetingInfoServiceImpl meetingInfoService;
    @Resource
    private MeetingMemberMapper<MeetingMember,MeetingMemberQuery> meetingMemberMapper;
    @Resource
    private MeetingMemberServiceImpl meetingMemberService;
    @Resource
    private SessionManager sessionManager;
    @Resource
    private MeetingInfoMapper <MeetingInfo,MeetingInfoQuery> meetingInfoMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserInfo> findListByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserInfoQuery param) {
		return this.userInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserInfo> list = this.findListByParam(param);
		PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserInfo bean) {
		return this.userInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserInfoQuery param) {
		StringTools.checkParam(param);
		return this.userInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据UserId获取对象
	 */
	@Override
	public UserInfo getUserInfoByUserId(String userId) {
		return this.userInfoMapper.selectByUserId(userId);
	}

	/**
	 * 根据UserId修改
	 */
	@Override
	public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
		return this.userInfoMapper.updateByUserId(bean, userId);
	}

	/**
	 * 根据UserId删除
	 */
	@Override
	public Integer deleteUserInfoByUserId(String userId) {
		return this.userInfoMapper.deleteByUserId(userId);
	}

	/**
	 * 根据Email获取对象
	 */
	@Override
	public UserInfo getUserInfoByEmail(String email) {
		return this.userInfoMapper.selectByEmail(email);
	}

	/**
	 * 根据Email修改
	 */
	@Override
	public Integer updateUserInfoByEmail(UserInfo bean, String email) {
		return this.userInfoMapper.updateByEmail(bean, email);
	}

	/**
	 * 根据Email删除
	 */
	@Override
	public Integer deleteUserInfoByEmail(String email) {
		return this.userInfoMapper.deleteByEmail(email);
	}

	@Override
	public void register(String email, String password,  String nickName) {

		// 1. 基本参数校验
		if (StringTools.isEmpty(email) ){
			throw new BusinessException("邮箱不能为空");
		}
		if (StringTools.isEmpty(password)) {
			throw new BusinessException("密码不能为空");
		}
		if (password.length() < 6 || password.length() > 20) {
			throw new BusinessException("密码长度应为 6-20 位");
		}
		// 2. 检查邮箱是否已注册
		UserInfo existUser = userInfoMapper.selectByEmail(email);
		if (existUser != null) {
			throw new BusinessException("该邮箱已被注册");
		}
		// 4. 构建用户对象
		UserInfo userInfo = new UserInfo();
		String Id= StringTools.getUserRandomId();
		String userId= "U"+ Id;
		String meetingNo= "G"+ Id;
		userInfo.setUserId(userId);
		userInfo.setNickName(nickName);
		userInfo.setEmail(email);
		userInfo.setMeetingNo(meetingNo);
		// 密码加密存储：BCrypt（自带随机盐，替代历史MD5方案，防彩虹表）
		userInfo.setPassword(StringTools.encodeByBCrypt(password));
		userInfo.setSex(UserSexEnum.UNKNOWN.getStatus()); // 默认保密
		userInfo.setStatus(UserStatusEnum.NORMAL.getStatus()); // 正常状态
		userInfo.setCreateTime(new Date());
		// 5. 保存到数据库
		try {
			userInfoMapper.insert(userInfo);
		} catch (org.springframework.dao.DuplicateKeyException e) {
			throw new BusinessException("该邮箱已被注册");
		} catch (Exception e) {
			logger.error("注册入库失败: userId={}, email={}", userInfo.getUserId(), userInfo.getEmail(), e);
			throw new BusinessException("注册失败，请稍后重试");
		}
	}
	@Override
	public UserInfoVo login(String email, String password) {
		// 1. 参数校验
		if (StringTools.isEmpty(email)) {
			throw new BusinessException("邮箱不能为空");
		}
		if (StringTools.isEmpty(password) ){
			throw new BusinessException("密码不能为空");
		}
		// 3. 查询用户
		UserInfo userInfo = userInfoMapper.selectByEmail(email);
		if (userInfo == null) {

			throw new BusinessException("邮箱或密码错误");
		}

		// 4. 校验用户状态
		if (userInfo.getStatus() != null && userInfo.getStatus() == 0) {
			throw new BusinessException("账号已被禁用，请联系管理员");
		}

		// 5. 校验密码（兼容存量MD5密文，命中后透明升级为BCrypt）
		if (!StringTools.checkPassword(password, userInfo.getPassword())) {

			throw new BusinessException("邮箱或密码错误");

		}
		// 存量MD5密码在登录成功时透明升级为BCrypt，用户无感知完成迁移
		if (StringTools.isLegacyMd5Password(userInfo.getPassword())) {
			userInfo.setPassword(StringTools.encodeByBCrypt(password));
			logger.info("存量MD5密码已透明升级为BCrypt: userId={}", userInfo.getUserId());
		}

		// 检测该账号是否已有有效会话（先登录者优先），已有则拒绝本次新登录，
		// 保证同一账号不能同时多处登录
		TokenUserInfoDto oldTokenUserInfo = redisComponet.getTokenUserInfoByUserId(userInfo.getUserId());
		if (oldTokenUserInfo != null && oldTokenUserInfo.getToken() != null) {
			throw new BusinessException("该账号已在其他设备登录");
		}

		// 生成新 token 并保存
        String token=StringTools.generateSecureToken();
		TokenUserInfoDto tokenUserInfoDto = getTokenUserInfoDto(userInfo);
		tokenUserInfoDto.setToken(token);
        redisComponet.saveTokenUserInfo(tokenUserInfoDto);

		// 8. 更新最后登录时间（密码升级与最后登录时间合并为一次update）
		userInfo.setLastLoginTime(System.currentTimeMillis());
		userInfoMapper.updateByEmail(userInfo, userInfo.getEmail());

		// 9. 构建返回对象
		UserInfoVo userInfoVo = new UserInfoVo();
		userInfoVo.setToken(tokenUserInfoDto.getToken());
		userInfoVo.setUserName(userInfo.getNickName());
		userInfoVo.setAdmin(tokenUserInfoDto.getAdmin());
		userInfoVo.setEmail(userInfo.getEmail());
		userInfoVo.setSex(userInfo.getSex());
		userInfoVo.setUserId(userInfo.getUserId());
		userInfoVo.setMeetingNo(userInfo.getMeetingNo());
		return userInfoVo;


	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void logout(TokenUserInfoDto tokenUserInfoDto) {
		//强制推出删除自己的会议
	    String meetingId=tokenUserInfoDto.getCurrentMeetingId();
		if(meetingId ==null){
			return;
		}
		MeetingMember meetingMember = meetingMemberMapper.selectByMeetingId(meetingId);
		logger.info("检查会议ID"+meetingMember.getCreateUserId()+"      "+tokenUserInfoDto.getUserId());
		if(meetingMember.getCreateUserId().equals(tokenUserInfoDto.getUserId()) ){
			logger.info("创建者退出，结束会议");
			meetingMemberService.endMeeting(meetingMember.getMeetingNo(),tokenUserInfoDto);
		}
		else{
			logger.info("普通成员退出，离开会议");
			meetingInfoService.leaveMeeting(meetingMember.getMeetingNo(),tokenUserInfoDto.getUserId());
		}
		sessionManager.kickUser(tokenUserInfoDto.getUserId());

	}

	@Override
	public void manageUser(String userId, Integer status) {
		UserInfo userInfo = userInfoMapper.selectByUserId(userId);
		if(userInfo == null){
			throw new BusinessException("用户不存在");
		}
		UserStatusEnum userStatusEnum = UserStatusEnum.getByStatus(status);
		if(userStatusEnum==null){
			throw new BusinessException("状态不合法");
		}
		userInfo.setStatus(status);
		userInfoMapper.updateByUserId(userInfo, userId);
		// 封禁时：先通知用户下线，再清除登录态，最后断开 WebSocket
		if (UserStatusEnum.DISABLE.getStatus().equals(status)) {
			sendForceOfflineNotice(userId, "你的账号已被管理员封禁");
			clearLoginToken(userId);
		}
		// 强制断开该用户的 WebSocket 连接
		sessionManager.kickUser(userId);
	}

	@Override
	public void forceOffline(String userId) {
		UserInfo userInfo = userInfoMapper.selectByUserId(userId);
		if (userInfo == null) {
			throw new BusinessException("用户不存在");
		}
		// 1. 先发强制下线通知（用户还在线才能收到，前端据此停止重连并登出）
		sendForceOfflineNotice(userId, "你已被管理员强制下线");
		// 2. 清除 Redis 登录态（HTTP 接口失效 + WS 重连失败）
		clearLoginToken(userId);
		// 3. 断开 WebSocket 连接
		sessionManager.kickUser(userId);
	}

	/**
	 * 发送强制下线通知给指定用户（单点推送）
	 */
	private void sendForceOfflineNotice(String userId, String reason) {
		try {
			MessageSendDto dto = new MessageSendDto();
			dto.setMessageType(MessageTypeEnum.FORCE_OFFLINE.getType());
			dto.setReceiveUserId(userId);
			dto.setMessageSend2Type(UserContactTypeEnum.USER.getType());
			dto.setSendTime(System.currentTimeMillis());
			dto.setMessageContent(reason);
			sessionManager.sendToUser(userId, dto);
		} catch (Exception e) {
			logger.error("发送强制下线通知失败: userId={}", userId, e);
		}
	}

	/**
	 * 清除用户在 Redis 中的登录态
	 */
	private void clearLoginToken(String userId) {
		TokenUserInfoDto tokenUserInfo = redisComponet.getTokenUserInfoByUserId(userId);
		if (tokenUserInfo != null && tokenUserInfo.getToken() != null) {
			redisComponet.delRedis(Contants.TOKEN_KEY_REDIS + tokenUserInfo.getToken());
			redisComponet.delRedis(Contants.USER_TOKEN_KEY_REDIS + userId);
		}
	}

	private TokenUserInfoDto getTokenUserInfoDto(UserInfo userInfo) {
		TokenUserInfoDto tokenUserInfoDto = new TokenUserInfoDto();
		tokenUserInfoDto.setUserId(userInfo.getUserId());
		tokenUserInfoDto.setNickName(userInfo.getNickName());

		String adminEmails = appConfig.getAdminEmails();
		if (!StringTools.isEmpty(adminEmails) && ArrayUtils.contains(adminEmails.split(","), userInfo.getEmail())) {
			tokenUserInfoDto.setAdmin(true);
		} else {
			tokenUserInfoDto.setAdmin(false);
		}
		return tokenUserInfoDto;
	}
}