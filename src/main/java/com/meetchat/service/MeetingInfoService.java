package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.po.MeetingMember;
import com.meetchat.entity.query.MeetingInfoQuery;
import com.meetchat.entity.po.MeetingInfo;
import com.meetchat.entity.vo.MeetingInfoUserVo;
import com.meetchat.entity.vo.MeetingMemberVO;
import com.meetchat.entity.vo.PaginationResultVO;


/**
 *  业务接口
 */
public interface MeetingInfoService {

	/**
	 * 根据条件查询列表
	 */
	List<MeetingInfo> findListByParam(MeetingInfoQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(MeetingInfoQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<MeetingInfo> findListByPage(MeetingInfoQuery param);

	/**
	 * 新增
	 */
	Integer add(MeetingInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<MeetingInfo> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<MeetingInfo> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(MeetingInfo bean,MeetingInfoQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(MeetingInfoQuery param);

	/**
	 * 根据MeetingIdAndUserId查询对象
	 */
	MeetingInfo getMeetingInfoByMeetingIdAndUserId(String meetingId,String userId);


	/**
	 * 根据MeetingIdAndUserId修改
	 */
	Integer updateMeetingInfoByMeetingIdAndUserId(MeetingInfo bean,String meetingId,String userId);


	/**
	 * 根据MeetingIdAndUserId删除
	 */
	Integer deleteMeetingInfoByMeetingIdAndUserId(String meetingId,String userId);
	MeetingMember joinMeeting(String meetingNo, String password, TokenUserInfoDto tokenUserInfo);
    void leaveMeeting(String meetingNo,String userId);
	List<MeetingInfoUserVo> getMeetingMembers(String userId);
	void kickUser(String meetingNo,String kickUserId,TokenUserInfoDto tokenUserInfoDto,Integer status);
	void sendMessage(MessageSendDto messageSendDto,TokenUserInfoDto tokenUserInfoDto);
}