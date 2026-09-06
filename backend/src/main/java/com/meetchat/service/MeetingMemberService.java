package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.query.MeetingMemberQuery;
import com.meetchat.entity.po.MeetingMember;
import com.meetchat.entity.vo.PaginationResultVO;


/**
 *  业务接口
 */
public interface MeetingMemberService {

	/**
	 * 根据条件查询列表
	 */
	List<MeetingMember> findListByParam(MeetingMemberQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(MeetingMemberQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<MeetingMember> findListByPage(MeetingMemberQuery param);

	/**
	 * 新增
	 */
	Integer add(MeetingMember bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<MeetingMember> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<MeetingMember> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(MeetingMember bean,MeetingMemberQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(MeetingMemberQuery param);

	/**
	 * 根据MeetingId查询对象
	 */
	MeetingMember getMeetingMemberByMeetingId(String meetingId);


	/**
	 * 根据MeetingId修改
	 */
	Integer updateMeetingMemberByMeetingId(MeetingMember bean,String meetingId);


	/**
	 * 根据MeetingId删除
	 */
	Integer deleteMeetingMemberByMeetingId(String meetingId);
    MeetingMember createMeeting(MeetingMember meetingMember, TokenUserInfoDto tokenUserInfoDto);
    void endMeeting(String meetingNo, TokenUserInfoDto tokenUserInfoDto);
	MeetingMember getMeetingByMeetingNo(String meetingNo);
	PaginationResultVO<MeetingMember> getMyMeetings(String userId, Integer pageNo, Integer pageSize);
}