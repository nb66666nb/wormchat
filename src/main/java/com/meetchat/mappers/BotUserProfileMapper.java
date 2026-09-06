package com.meetchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 *  数据库操作接口
 */
public interface BotUserProfileMapper<T,P> extends BaseMapper<T,P> {

	/**
	 * 根据Id更新
	 */
	 Integer updateById(@Param("bean") T t,@Param("id") Long id);


	/**
	 * 根据Id删除
	 */
	 Integer deleteById(@Param("id") Long id);


	/**
	 * 根据Id获取对象
	 */
	 T selectById(@Param("id") Long id);


	/**
	 * 根据UserIdAndBotId更新
	 */
	 Integer updateByUserIdAndBotId(@Param("bean") T t,@Param("userId") String userId,@Param("botId") String botId);


	/**
	 * 根据UserIdAndBotId删除
	 */
	 Integer deleteByUserIdAndBotId(@Param("userId") String userId,@Param("botId") String botId);


	/**
	 * 根据UserIdAndBotId获取对象
	 */
	 T selectByUserIdAndBotId(@Param("userId") String userId,@Param("botId") String botId);


}