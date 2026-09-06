package com.meetchat.mappers;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IM即时通讯消息表 数据库操作接口
 */
public interface ChatImMessageMapper<T,P> extends BaseMapper<T,P> {

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
	 * 根据MessageOnlyId获取对象（幂等校验用）
	 */
	 T selectByMessageOnlyId(@Param("messageOnlyId") String messageOnlyId);

	/**
	 * 增量拉取：接收用户 messageId > lastMessageId 的所有消息（离线补偿用）
	 */
	 List<T> selectAfterId(@Param("receiveUserId") String receiveUserId,
					  @Param("lastMessageId") Long lastMessageId,
					  @Param("pageSize") Integer pageSize);

	/**
	 * 群消息增量拉取：receive_user_id IN (groupIds) 且 messageId > lastMessageId
	 * 群消息单条存储（receive_user_id = groupId），按用户所在的所有群查询
	 */
	 List<T> selectGroupMessagesAfterIds(@Param("groupIds") List<String> groupIds,
					  @Param("lastMessageId") Long lastMessageId,
					  @Param("pageSize") Integer pageSize);


}
