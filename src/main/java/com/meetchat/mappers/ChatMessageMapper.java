package com.meetchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * 会议聊天记录表 数据库操作接口
 */
public interface ChatMessageMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据MessageId更新
	 */
	 Integer updateByMessageId(@Param("bean") T t,@Param("messageId") Long messageId);


	/**
	 * 根据MessageId删除
	 */
	 Integer deleteByMessageId(@Param("messageId") Long messageId);


	/**
	 * 根据MessageId获取对象
	 */
	 T selectByMessageId(@Param("messageId") Long messageId);


	/**
	 * 根据MeetingNo更新
	 */
	 Integer updateByMeetingNo(@Param("bean") T t,@Param("meetingNo") String meetingNo);


	/**
	 * 根据MeetingNo删除
	 */
	 Integer deleteByMeetingNo(@Param("meetingNo") String meetingNo);


	/**
	 * 根据MeetingNo获取对象
	 */
	 T selectByMeetingNo(@Param("meetingNo") String meetingNo);


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


}