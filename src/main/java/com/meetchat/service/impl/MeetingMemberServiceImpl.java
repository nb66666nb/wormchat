package com.meetchat.service.impl;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.contants.Contants;
import com.meetchat.entity.enums.*;
import com.meetchat.entity.po.MeetingInfo;
import com.meetchat.entity.query.MeetingInfoQuery;
import com.meetchat.exception.BusinessException;
import com.meetchat.mappers.MeetingInfoMapper;
import com.meetchat.redis.RedisComponet;
import com.meetchat.service.MeetingInfoService;
import com.meetchat.webSocket.SessionManager;
import com.meetchat.webSocket.netty.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.meetchat.entity.query.MeetingMemberQuery;
import com.meetchat.entity.po.MeetingMember;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.MeetingMemberMapper;
import com.meetchat.service.MeetingMemberService;
import com.meetchat.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
/**
 *  业务接口实现
 */
@Service("meetingMemberService")
public class MeetingMemberServiceImpl implements MeetingMemberService {
	private static final Logger logger = LoggerFactory.getLogger(MeetingMemberServiceImpl.class);

	@Resource
	private MeetingMemberMapper<MeetingMember, MeetingMemberQuery> meetingMemberMapper;
	@Resource
	private MeetingInfoMapper<MeetingInfo, MeetingInfoQuery> meetingInfoMapper;
	@Resource
	private MeetingInfoService meetingInfoService;
	@Resource
	private RedisComponet redisComponet;
	@Resource
	private SessionManager sessionManager;
	@Resource
	private MessageHandler messageHandler;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<MeetingMember> findListByParam(MeetingMemberQuery param) {
		return this.meetingMemberMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(MeetingMemberQuery param) {
		return this.meetingMemberMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<MeetingMember> findListByPage(MeetingMemberQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<MeetingMember> list = this.findListByParam(param);
		PaginationResultVO<MeetingMember> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(MeetingMember bean) {
		return this.meetingMemberMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<MeetingMember> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.meetingMemberMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<MeetingMember> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.meetingMemberMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(MeetingMember bean, MeetingMemberQuery param) {
		StringTools.checkParam(param);
		return this.meetingMemberMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(MeetingMemberQuery param) {
		StringTools.checkParam(param);
		return this.meetingMemberMapper.deleteByParam(param);
	}

	/**
	 * 根据MeetingId获取对象
	 */
	@Override
	public MeetingMember getMeetingMemberByMeetingId(String meetingId) {
		return this.meetingMemberMapper.selectByMeetingId(meetingId);
	}

	/**
	 * 根据MeetingId修改
	 */
	@Override
	public Integer updateMeetingMemberByMeetingId(MeetingMember bean, String meetingId) {
		return this.meetingMemberMapper.updateByMeetingId(bean, meetingId);
	}

	/**
	 * 根据MeetingId删除
	 */
	@Override
	public Integer deleteMeetingMemberByMeetingId(String meetingId) {
		return this.meetingMemberMapper.deleteByMeetingId(meetingId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MeetingMember createMeeting(MeetingMember meetingMember, TokenUserInfoDto tokenUserInfoDto) {
		if(meetingMember.getMeetingName()==null ||meetingMember.getCreateUserId()==null ||meetingMember.getJionType()==null ||meetingMember.getMeetingNo()==null){
			throw new BusinessException("参数错误");
		}
		if(tokenUserInfoDto.getCurrentMeetingId()!=null){
			throw new BusinessException("当前已经参加会议");
		}
		String password= meetingMember.getJionPassword();
		meetingMember.setJionPassword(null);
		if(JoinTypeEnum.PASSWORD.getType().equals(meetingMember.getJionType())){
			meetingMember.setJionPassword(StringTools.encodeByMD5(password));
		}
		Date date = new Date();
		meetingMember.setMeetingId(StringTools.getMeetingRandomId());
		meetingMember.setCreateTime(date);
		meetingMember.setStartTime(date);
		meetingMember.setStatus(MeetingStatusEnum.IN_PROGRESS.getStatus());
		meetingMemberMapper.insert(meetingMember);
		//创建会议成员(创建者)
		MeetingInfo meetingInfo = new MeetingInfo();
		meetingInfo.setMeetingId(meetingMember.getMeetingId());
		meetingInfo.setMeetingStatus(MeetingStatusEnum.IN_PROGRESS.getStatus());
		meetingInfo.setMemberType(MemberTypeEnum.HOST.getType());
		meetingInfo.setLastJoinTime(date);
		meetingInfo.setNickName(tokenUserInfoDto.getNickName());
		meetingInfo.setUserId(tokenUserInfoDto.getUserId());
		meetingInfo.setStatus(UserInMeetingStatusEnum.NORMAL_IN.getStatus());
		meetingInfoMapper.insert(meetingInfo);
		tokenUserInfoDto.setCurrentMeetingId(meetingMember.getMeetingId());
		redisComponet.saveTokenUserInfo(tokenUserInfoDto);
		//channel处理
		MessageSendDto messageSendDto =new MessageSendDto();

		messageSendDto.setMessageType(MessageTypeEnum.CREATE_MEETING.getType());
		messageSendDto.setSendUserId(meetingMember.getMeetingId());
		messageSendDto.setStatus(MessageStatusEnum.DONE_SEND.getStatus());
		messageSendDto.setReceiveUserId(tokenUserInfoDto.getUserId());
		messageSendDto.setSendTime(date.getTime());
		messageSendDto.setMessageSend2Type(UserContactTypeEnum.USER.getType());
		messageSendDto.setMessageContent(Contants.CREATE_MEETING_MESSAGE);
		messageSendDto.setMessageTypeIm(MessageTypeImEnum.MEETING.getType());
		sessionManager.createGroup(meetingMember.getMeetingId());
		sessionManager.joinGroup(tokenUserInfoDto.getUserId(),meetingMember.getMeetingId());
		messageHandler.sendMessage(messageSendDto);
		return meetingMember;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void endMeeting(String meetingNo, TokenUserInfoDto tokenUserInfoDto) {
			if (StringTools.isEmpty(meetingNo) || tokenUserInfoDto == null) {
				throw new BusinessException("参数错误,会议密码不能为空");
			}
			if(tokenUserInfoDto.getCurrentMeetingId()==null){
				throw new BusinessException("当前没有会议");
            }
			// 查询会议
			MeetingMember meetingMember = meetingMemberMapper.selectByMeetingNo(meetingNo);
			if (meetingMember == null) {
				throw new BusinessException("会议不存在");
			}
			// 校验是否是主持人
		    if(!meetingMember.getCreateUserId().equals(tokenUserInfoDto.getUserId())){
				throw new BusinessException("只有主持人才能结束会议");
			}
			// 校验会议状态
		    Boolean i= meetingMember.getStatus().equals(MeetingStatusEnum.IN_PROGRESS.getStatus());
			logger.info("会议状态"+i);
			if(!meetingMember.getStatus().equals(MeetingStatusEnum.IN_PROGRESS.getStatus())){
				throw new BusinessException("会议已结束或未开始");
			}
			//更新会议联系表，Usr状态和会议状态
			MeetingInfoQuery meetingInfoQuery = new MeetingInfoQuery();
			meetingInfoQuery.setMeetingId(meetingMember.getMeetingId());
			meetingInfoQuery.setMeetingStatus(MeetingStatusEnum.IN_PROGRESS.getStatus());
			// 2. 清除所有成员的在线状态
		    List<MeetingInfo> meetingInfoList = meetingInfoMapper.selectList(meetingInfoQuery);
			for (MeetingInfo meetingInfo : meetingInfoList) {
				if (meetingInfo.getUserId().equals(tokenUserInfoDto.getUserId())) {
					continue;
				}
				 TokenUserInfoDto userInfoDto = redisComponet.getTokenUserInfoByUserId(meetingInfo.getUserId());
                 if(userInfoDto!=null){
					 userInfoDto.setCurrentMeetingId(null);
				 }
				 redisComponet.saveTokenUserInfo(userInfoDto);
			}

			MeetingInfo meetingInfo = new MeetingInfo();
			meetingInfo.setMeetingStatus(MeetingStatusEnum.ENDED.getStatus());
			meetingInfoMapper.updateByParam(meetingInfo, meetingInfoQuery);

			// 1. 更新数据库：会议状态 → 已结束
			MeetingMember update = new MeetingMember();
			update.setStatus(MeetingStatusEnum.ENDED.getStatus());
			update.setEndTime(new Date());
			meetingMemberMapper.updateByMeetingId(update, meetingMember.getMeetingId());

			// 3. 广播"会议结束"事件（在 ChannelGroup 关闭前广播）
             MessageSendDto channelMessageSendDto = new MessageSendDto();
			 channelMessageSendDto.setMessageType(MessageTypeEnum.MEETING_END.getType());
			 channelMessageSendDto.setSendUserId(meetingMember.getMeetingId());
			 channelMessageSendDto.setStatus(MessageStatusEnum.DONE_SEND.getStatus());
			 channelMessageSendDto.setSendUserId(null);
			 channelMessageSendDto.setReceiveUserId(meetingMember.getMeetingId());
		     channelMessageSendDto.setSendTime(new Date().getTime());
			 channelMessageSendDto.setMessageContent(Contants.MEETING_END_MESSAGE);
			 channelMessageSendDto.setMessageSend2Type(UserContactTypeEnum.GROUP.getType());
			 channelMessageSendDto.setMessageTypeIm(MessageTypeImEnum.MEETING.getType());
			 messageHandler.sendMessage(channelMessageSendDto);

			 tokenUserInfoDto.setCurrentMeetingId(null);
			 redisComponet.saveTokenUserInfo(tokenUserInfoDto);

	}

	@Override
	public MeetingMember getMeetingByMeetingNo(String meetingNo) {
		return meetingMemberMapper.selectByMeetingNo(meetingNo);
	}

	@Override
	public PaginationResultVO<MeetingMember> getMyMeetings(String userId, Integer pageNo, Integer pageSize) {
		if(userId==null){
			throw new BusinessException("参数错误");
		}
		// 1. 查询用户参与的所有会议ID
		MeetingInfoQuery meetingInfoQuery = new MeetingInfoQuery();
		meetingInfoQuery.setUserId(userId);
		List<MeetingInfo> meetingInfoList = meetingInfoMapper.selectList(meetingInfoQuery);
		logger.info("meetingInfoList"+meetingInfoList);
		List<String> meetingIdList = meetingInfoList.stream()
				.map(MeetingInfo::getMeetingId)
				.collect(Collectors.toList());
		logger.info("meetingIdList"+meetingIdList);
		if(meetingIdList.isEmpty()){
			return new PaginationResultVO<>(0, pageSize, pageNo, new ArrayList<>());
		}

		// 2. 批量查询会议（一次SQL搞定，SQL中已过滤endTime非空并按endTime降序排序）
		List<MeetingMember> meetingMembers = meetingMemberMapper.selectByMeetingIds(meetingIdList);
		logger.info("meetingMembers"+meetingMembers);

		// 3. 手动分页
		int totalCount = meetingMembers.size();
		int fromIndex = (pageNo - 1) * pageSize;
		int toIndex = Math.min(fromIndex + pageSize, totalCount);
		List<MeetingMember> pageList = fromIndex >= totalCount
			? new ArrayList<>()
			: meetingMembers.subList(fromIndex, toIndex);

		return new PaginationResultVO<>(totalCount, pageSize, pageNo, pageList);
    }


}