package com.meetchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * 聊天会话表 数据库操作接口
 */
public interface ChatSessionMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据SessionId更新
	 */
	 Integer updateBySessionId(@Param("bean") T t,@Param("sessionId") String sessionId);


	/**
	 * 根据SessionId删除
	 */
	 Integer deleteBySessionId(@Param("sessionId") String sessionId);


	/**
	 * 根据SessionId获取对象
	 */
	 T selectBySessionId(@Param("sessionId") String sessionId);


	/**
	 * 根据UserIdAndTargetUserId更新
	 */
	 Integer updateByUserIdAndTargetUserId(@Param("bean") T t,@Param("userId") String userId,@Param("targetUserId") String targetUserId);


	/**
	 * 根据UserIdAndTargetUserId删除
	 */
	 Integer deleteByUserIdAndTargetUserId(@Param("userId") String userId,@Param("targetUserId") String targetUserId);


	/**
	 * 根据UserIdAndTargetUserId获取对象
	 */
	 T selectByUserIdAndTargetUserId(@Param("userId") String userId,@Param("targetUserId") String targetUserId);

}