package com.meetchat.service.impl;

import java.util.ArrayList;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.contants.Contants;
import com.meetchat.entity.enums.*;
import com.meetchat.entity.po.ChatMessage;
import com.meetchat.entity.po.MeetingMember;
import com.meetchat.entity.vo.MeetingInfoUserVo;
import com.meetchat.exception.BusinessException;
import com.meetchat.mappers.ChatMessageMapper;
import com.meetchat.mappers.MeetingMemberMapper;
import com.meetchat.redis.RedisComponet;
import com.meetchat.utils.CopyTools;
import com.meetchat.webSocket.SessionManager;
import com.meetchat.webSocket.netty.MessageHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.meetchat.entity.query.MeetingInfoQuery;
import com.meetchat.entity.po.MeetingInfo;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.MeetingInfoMapper;
import com.meetchat.service.MeetingInfoService;
import com.meetchat.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 *  业务接口实现
 */
@Service("meetingInfoService")
public  class MeetingInfoServiceImpl implements MeetingInfoService {

	@Resource
	private MeetingInfoMapper<MeetingInfo, MeetingInfoQuery> meetingInfoMapper;
	@Resource
	private MeetingMemberMapper<MeetingMember, MeetingInfoQuery> meetingMemberMapper;
	@Resource
	private RedisComponet redisComponet;
	@Resource
	private MessageHandler messageHandler;
    @Resource
    private SessionManager sessionManager;
	@Resource
	private ChatMessageMapper<ChatMessage, ?> chatMessageMapper;



	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<MeetingInfo> findListByParam(MeetingInfoQuery param) {
		return this.meetingInfoMapper.selectList(param);
	}
     private  final static Logger logger = LoggerFactory.getLogger(MeetingInfoServiceImpl.class);
	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(MeetingInfoQuery param) {
		return this.meetingInfoMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<MeetingInfo> findListByPage(MeetingInfoQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<MeetingInfo> list = this.findListByParam(param);
		PaginationResultVO<MeetingInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(MeetingInfo bean) {
		return this.meetingInfoMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<MeetingInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.meetingInfoMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<MeetingInfo> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.meetingInfoMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(MeetingInfo bean, MeetingInfoQuery param) {
		StringTools.checkParam(param);
		return this.meetingInfoMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(MeetingInfoQuery param) {
		StringTools.checkParam(param);
		return this.meetingInfoMapper.deleteByParam(param);
	}

	/**
	 * 根据MeetingIdAndUserId获取对象
	 */
	@Override
	public MeetingInfo getMeetingInfoByMeetingIdAndUserId(String meetingId, String userId) {
		return this.meetingInfoMapper.selectByMeetingIdAndUserId(meetingId, userId);
	}

	/**
	 * 根据MeetingIdAndUserId修改
	 */
	@Override
	public Integer updateMeetingInfoByMeetingIdAndUserId(MeetingInfo bean, String meetingId, String userId) {
		return this.meetingInfoMapper.updateByMeetingIdAndUserId(bean, meetingId, userId);
	}

	/**
	 * 根据MeetingIdAndUserId删除
	 */
	@Override
	public Integer deleteMeetingInfoByMeetingIdAndUserId(String meetingId, String userId) {
		return this.meetingInfoMapper.deleteByMeetingIdAndUserId(meetingId, userId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MeetingMember joinMeeting(String meetingNo, String password, TokenUserInfoDto tokenUserInfo) {
		MeetingMember meetingMember = meetingMemberMapper.selectByMeetingNo(meetingNo);
		String meetingId = meetingMember.getMeetingId();
		MeetingInfo meetingInfo = meetingInfoMapper.selectByMeetingIdAndUserId(meetingId, tokenUserInfo.getUserId());
		if (tokenUserInfo.getCurrentMeetingId() != null) {
			throw new BusinessException("您已加入某个会议");
		}
		tokenUserInfo.setCurrentMeetingId(meetingId);
		if (meetingInfo != null) {

			if (UserInMeetingStatusEnum.BLACK_OUT.getStatus().equals(meetingInfo.getStatus())) {
				throw new BusinessException("您已被拉黑");
			}
			if (UserInMeetingStatusEnum.NORMAL_IN.getStatus().equals(meetingInfo.getStatus())) {
				throw new BusinessException("您已加入该会议");
			}
			if (!MeetingStatusEnum.IN_PROGRESS.getStatus().equals(meetingInfo.getStatus())) {
				throw new BusinessException("会议已结束");
			}
			meetingInfo.setStatus(UserInMeetingStatusEnum.NORMAL_IN.getStatus());
			meetingInfo.setLastJoinTime(new Date());
			meetingInfoMapper.updateByMeetingIdAndUserId(meetingInfo, meetingId, tokenUserInfo.getUserId());

			redisComponet.saveTokenUserInfo(tokenUserInfo);

			//channel
			sessionManager.joinGroup(tokenUserInfo.getUserId(), meetingId);
			//发送消息
			messageHandler.sendMessage(sysMessageSendDto(meetingId, tokenUserInfo.getNickName() + Contants.MEETING_JOIN_SUCCESS, MessageTypeEnum.MEETING_JOIN.getType()));
		} else {
			MeetingInfo meetingInfoNew = new MeetingInfo();
			if (!MeetingStatusEnum.IN_PROGRESS.getStatus().equals(meetingMember.getStatus())) {
				throw new BusinessException("会议已结束");
			}

			if (JoinTypeEnum.FREE.getType().equals(meetingMember.getJionType())) {
				meetingInfoNew.setMeetingId(meetingId);
				meetingInfoNew.setUserId(tokenUserInfo.getUserId());
				meetingInfoNew.setMemberType(MemberTypeEnum.ATTENDEE.getType());
				meetingInfoNew.setNickName(tokenUserInfo.getNickName());
				meetingInfoNew.setLastJoinTime(new Date());
				meetingInfoNew.setMeetingStatus(MeetingStatusEnum.IN_PROGRESS.getStatus());
				meetingInfoNew.setStatus(UserInMeetingStatusEnum.NORMAL_IN.getStatus());
				meetingInfoMapper.insert(meetingInfoNew);
				redisComponet.saveTokenUserInfo(tokenUserInfo);
				//channel
				sessionManager.joinGroup(tokenUserInfo.getUserId(), meetingId);
				//发送消息
				messageHandler.sendMessage(sysMessageSendDto(meetingId, tokenUserInfo.getNickName() + Contants.MEETING_JOIN_SUCCESS, MessageTypeEnum.MEETING_JOIN.getType()));
			} else {
				if (!meetingMember.getJionPassword().equals(StringTools.encodeByMD5(password))) {
					throw new BusinessException("密码错误");
				}
				meetingInfoNew.setMeetingId(meetingId);
				meetingInfoNew.setUserId(tokenUserInfo.getUserId());
				meetingInfoNew.setMemberType(MemberTypeEnum.ATTENDEE.getType());
				meetingInfoNew.setNickName(tokenUserInfo.getNickName());
				meetingInfoNew.setLastJoinTime(new Date());
				meetingInfoNew.setMeetingStatus(MeetingStatusEnum.IN_PROGRESS.getStatus());
				meetingInfoNew.setStatus(UserInMeetingStatusEnum.NORMAL_IN.getStatus());
				meetingInfoMapper.insert(meetingInfoNew);
				redisComponet.saveTokenUserInfo(tokenUserInfo);
				//channel
				sessionManager.joinGroup(tokenUserInfo.getUserId(), meetingId);
				//发送消息
				messageHandler.sendMessage(sysMessageSendDto(meetingId, tokenUserInfo.getNickName() + Contants.MEETING_JOIN_SUCCESS, MessageTypeEnum.MEETING_JOIN.getType()));
			}
		}
		return meetingMember;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void leaveMeeting(String meetingNo, String userId) {
		TokenUserInfoDto tokenUserInfo = redisComponet.getTokenUserInfoByUserId(userId);
		if (StringTools.isEmpty(meetingNo) || tokenUserInfo == null) {
			throw new BusinessException("会议号码不能为空");
		}
		if (tokenUserInfo.getCurrentMeetingId() == null) {
			throw new BusinessException("您未加入任何会议");
		}

		tokenUserInfo.setCurrentMeetingId(null);

		// 检查会议是否存在
		MeetingMember meetingMember = meetingMemberMapper.selectByMeetingNo(meetingNo);
		if (meetingMember == null) {
			throw new BusinessException("会议不存在");
		}

		// 检查是否是成员
		MeetingInfo meetingInfo = meetingInfoMapper.selectByMeetingIdAndUserId(meetingMember.getMeetingId(), tokenUserInfo.getUserId());
		if (meetingInfo == null) {
			throw new BusinessException("你不是该会议的成员");
		}
		//检查会议状态
		if (!meetingInfo.getMeetingStatus().equals(MeetingStatusEnum.IN_PROGRESS.getStatus())) {
			throw new BusinessException("会议已结束或未开始");
		}

		//检查用户状态
		if (!UserInMeetingStatusEnum.NORMAL_IN.getStatus().equals(meetingInfo.getStatus())) {
			throw new BusinessException("您已离开该会议(未参加或被拉黑)");
		}


		MeetingInfo meetingInfo1 = new MeetingInfo();
		meetingInfo1.setStatus(UserInMeetingStatusEnum.NORMAL_OUT.getStatus());
		redisComponet.saveTokenUserInfo(tokenUserInfo);
		// 1. 更新数据库成员状态为离开
		meetingInfoMapper.updateByMeetingIdAndUserId(meetingInfo1, meetingMember.getMeetingId(), tokenUserInfo.getUserId());

		// 2. Channel 操作：将用户从会议室 ChannelGroup 移除
		sessionManager.leaveGroup(tokenUserInfo.getUserId(), meetingMember.getMeetingId());
		// 3. 广播"有人离开"事件给剩余成员
		messageHandler.sendMessage(sysMessageSendDto(meetingMember.getMeetingId(), tokenUserInfo.getNickName() + Contants.MEETING_LEAVE_SUCCESS, MessageTypeEnum.MEETING_LEAVE.getType()));




	}

	@Override
	public List<MeetingInfoUserVo> getMeetingMembers(String meetingNo) {
		if (StringTools.isEmpty(meetingNo)) {
			throw new BusinessException("会议号码不能为空");
		}
		MeetingMember meetingMember =meetingMemberMapper.selectByMeetingNo(meetingNo);
		MeetingInfoQuery meetingInfoQuery = new MeetingInfoQuery();
		meetingInfoQuery.setMeetingId(meetingMember.getMeetingId());

         List<MeetingInfo> members = meetingInfoMapper.selectList(meetingInfoQuery);
		 List<MeetingInfoUserVo> result = new ArrayList<>();
		for (MeetingInfo member : members) {
			MeetingInfoUserVo vo = new MeetingInfoUserVo();
			vo.setUserId(member.getUserId());
			vo.setNickName(member.getNickName());
			vo.setMemberType(member.getMemberType());
			vo.setStatus(member.getStatus());
			if (member.getLastJoinTime() != null) {
				vo.setLastJoinTime(member.getLastJoinTime());
			}
			result.add(vo);
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void kickUser(String meetingNo, String kickUserId, TokenUserInfoDto tokenUserInfoDto,Integer status) {
		MeetingMember meetingMember = meetingMemberMapper.selectByMeetingNo(meetingNo);
		MeetingInfo meetingInfo = meetingInfoMapper.selectByMeetingIdAndUserId(meetingMember.getMeetingId(), tokenUserInfoDto.getUserId());
		MeetingInfo kickUserInfo= meetingInfoMapper.selectByMeetingIdAndUserId(meetingMember.getMeetingId(), kickUserId);
		if(meetingMember ==null ||meetingInfo==null){
			throw new BusinessException("你没有创建或添加该会议");
        }
		if(!meetingInfo.getMeetingStatus().equals(MeetingStatusEnum.IN_PROGRESS.getStatus())|| !meetingMember.getStatus().equals(MeetingStatusEnum.IN_PROGRESS.getStatus())) {
			throw new BusinessException("会议已结束或未开始");
		}

		if(!meetingMember.getCreateUserId().equals(tokenUserInfoDto.getUserId())) {
			throw new BusinessException("你没有权限踢人(非房主)");
		}
		if(kickUserInfo ==null) {
			throw new BusinessException("该用户不是会议成员,无法踢出");
		}
		if(!UserInMeetingStatusEnum.NORMAL_IN.getStatus().equals(kickUserInfo.getStatus())){
			throw new BusinessException("该用户不在会议中（正常退出或被拉黑或被踢出）");
		}
		UserInMeetingStatusEnum statusEnum = UserInMeetingStatusEnum.getByStatus(status);

		if(statusEnum ==null){
			throw new BusinessException("输入状态错误");
		}
		TokenUserInfoDto userInfoDto = redisComponet.getTokenUserInfoByUserId(kickUserId);
		userInfoDto.setCurrentMeetingId(null);
		//发送消息
		MessageSendDto messageSendDto = sysMessageSendDto(meetingMember.getMeetingId(), tokenUserInfoDto.getNickName()+"踢出了"+kickUserInfo.getNickName(), MessageTypeEnum.MEETING_KICK.getType());
		messageSendDto.setExtendData(kickUserId);
		messageHandler.sendMessage(messageSendDto);

		//更新数据库
		MeetingInfo meetingInfoUpdate= new MeetingInfo();
		meetingInfoUpdate.setStatus(status);
		meetingInfoMapper.updateByMeetingIdAndUserId(meetingInfoUpdate, meetingMember.getMeetingId(), kickUserId);
		sessionManager.leaveGroup(userInfoDto.getUserId(), meetingMember.getMeetingId());
		redisComponet.saveTokenUserInfo(userInfoDto);
	}

	@Override
	public void sendMessage(MessageSendDto messageSendDto, TokenUserInfoDto tokenUserInfoDto) {
		if(tokenUserInfoDto==null){
			throw new BusinessException("用户信息为空");
        }
		String userId=tokenUserInfoDto.getUserId();
		String meetingId =tokenUserInfoDto.getCurrentMeetingId();
		if(meetingId ==null){
			throw new BusinessException("当前会议为空");
        }

		if(messageSendDto.getMeetingId()!=null &&!meetingId.equals(messageSendDto.getMeetingId())){
			throw new BusinessException("消息所属会议与当前会议不匹配");
		}

		MeetingMember meetingMember = meetingMemberMapper.selectByMeetingId(meetingId);
		MeetingInfoQuery meetingInfoQuery = new MeetingInfoQuery();
		meetingInfoQuery.setMeetingId(meetingId);
		List<MeetingInfo> membersList = meetingInfoMapper.selectList(meetingInfoQuery);
		if(meetingMember ==null){
			throw new BusinessException("当前会议不存在");
        }
		List<String> userIdList = membersList.stream().map(item -> item.getUserId()).collect(Collectors.toList());
		if(messageSendDto.getMessageSend2Type().equals(UserContactTypeEnum.USER.getType() )&&!userIdList.contains(messageSendDto.getReceiveUserId())){
			throw new BusinessException("该用户不是会议成员");
        }

		if(messageSendDto.getMessageSend2Type().equals(UserContactTypeEnum.USER.getType())){
			if(EnumSet.of(MessageTypeEnum.FILE_MESSAGE, MessageTypeEnum.AUDIO_MESSAGE,MessageTypeEnum.IMAGE_MESSAGE,MessageTypeEnum.VIDEO_MESSAGE).contains(messageSendDto.getMessageType())) {

				MessageSendDto messageSendDto2 = new MessageSendDto();
				messageSendDto2.setSendUserId(userId);
				messageSendDto2.setReceiveUserId(userId);
				messageSendDto2.setExtendData(messageSendDto.getReceiveUserId());
				messageSendDto2.setMessageContent(messageSendDto.getMessageContent());
				messageSendDto2.setStatus(messageSendDto.getStatus());
				messageSendDto2.setMessageSend2Type(messageSendDto.getMessageSend2Type());
				messageSendDto2.setSendTime(messageSendDto.getSendTime());
				messageSendDto2.setSendUserNickName(messageSendDto.getSendUserNickName());
				messageSendDto2.setMessageType(messageSendDto.getMessageType());
				messageSendDto2.setFileId(messageSendDto.getFileId());
				messageSendDto2.setFileName(messageSendDto.getFileName());
				messageSendDto2.setFilePath(messageSendDto.getFilePath());
				messageSendDto2.setFileSize(messageSendDto.getFileSize());
				messageSendDto2.setFileType(messageSendDto.getFileType());
				messageSendDto2.setMessageTypeIm(MessageTypeImEnum.MEETING.getType());
				messageHandler.sendMessage(messageSendDto2);
			}else {
				MessageSendDto messageSendDto2 = new MessageSendDto();
				messageSendDto2.setSendUserId(userId);
				messageSendDto2.setReceiveUserId(userId);
				messageSendDto2.setExtendData(messageSendDto.getReceiveUserId());
				messageSendDto2.setMessageContent(messageSendDto.getMessageContent());
				messageSendDto2.setStatus(messageSendDto.getStatus());
				messageSendDto2.setMessageSend2Type(messageSendDto.getMessageSend2Type());
				messageSendDto2.setSendTime(messageSendDto.getSendTime());
				messageSendDto2.setSendUserNickName(messageSendDto.getSendUserNickName());
				messageSendDto2.setMessageType(messageSendDto.getMessageType());
				messageSendDto2.setMessageTypeIm(MessageTypeImEnum.MEETING.getType());
				messageHandler.sendMessage(messageSendDto2);

			}
		}
		messageSendDto.setMessageTypeIm(MessageTypeImEnum.MEETING.getType());
		messageHandler.sendMessage(messageSendDto);

		// 存储聊天记录到数据库
		saveChatMessage(messageSendDto, meetingId, meetingMember.getMeetingNo());
	}

	/**
	 * 将消息保存到chat_message表
	 */
	private void saveChatMessage(MessageSendDto<?> dto, String meetingId, String meetingNo) {
		try {
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setMeetingId(meetingId);
			chatMessage.setMeetingNo(meetingNo);
			chatMessage.setMessageSendType(dto.getMessageSend2Type());
			chatMessage.setSendUserId(dto.getSendUserId());
			chatMessage.setSendUserNickName(dto.getSendUserNickName());
			chatMessage.setReceiveUserId(dto.getReceiveUserId());
			chatMessage.setMessageType(dto.getMessageType());
			chatMessage.setMessageContent(dto.getMessageContent() != null ? dto.getMessageContent().toString() : null);
			chatMessage.setFileSize(dto.getFileSize());
			chatMessage.setFileName(dto.getFileName());
			chatMessage.setFileId(dto.getFileId());
			chatMessage.setFilePath(dto.getFilePath());
			chatMessage.setContentType(dto.getContentType());
			chatMessage.setFileType(dto.getFileType());
			chatMessage.setExtendData(dto.getExtendData());
			chatMessage.setStatus(dto.getStatus());
			chatMessage.setSendTime(dto.getSendTime());
			chatMessageMapper.insert(chatMessage);
		} catch (Exception e) {
			logger.error("保存聊天记录失败: {}", e.getMessage(), e);
		}
	}

	//获取系统发送给会议的消息
	public MessageSendDto sysMessageSendDto(String meetingId,String messageContent,Integer messageType) {
		MessageSendDto messageSendDto = new MessageSendDto();
        messageSendDto.setSendUserId(null);
		messageSendDto.setExtendData(null);
		messageSendDto.setReceiveUserId(meetingId);
		messageSendDto.setMessageContent(messageContent);
		messageSendDto.setSendTime(new Date().getTime());
		messageSendDto.setStatus(MessageStatusEnum.DONE_SEND.getStatus());
		messageSendDto.setMessageSend2Type(UserContactTypeEnum.GROUP.getType());
		messageSendDto.setMessageType(messageType);
		messageSendDto.setMessageTypeIm(MessageTypeImEnum.MEETING.getType());
        return messageSendDto;
	}





}