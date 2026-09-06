package com.meetchat.mappers;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 *  数据库操作接口
 */
public interface MeetingMemberMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据MeetingId更新
	 */
	 Integer updateByMeetingId(@Param("bean") T t,@Param("meetingId") String meetingId);


	/**
	 * 根据MeetingId删除
	 */
	 Integer deleteByMeetingId(@Param("meetingId") String meetingId);


	/**
	 * 根据MeetingId获取对象
	 */
	 T selectByMeetingId(@Param("meetingId") String meetingId);

     T selectByMeetingNo(@Param("meetingNo") String meetingNo);

	/**
	 * 根据MeetingId列表批量查询
	 */
	 List<T> selectByMeetingIds(@Param("meetingIds") List<String> meetingIds);
}