package com.meetchat.mappers;

import org.apache.ibatis.annotations.Param;

/**
 * 好友申请表 数据库操作接口
 */
public interface FriendRequestMapper<T,P> extends BaseMapper<T,P> {

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
	 * 根据RequestUserIdAndTargetUserId更新
	 */
	 Integer updateByRequestUserIdAndTargetUserId(@Param("bean") T t,@Param("requestUserId") String requestUserId,@Param("targetUserId") String targetUserId);


	/**
	 * 根据RequestUserIdAndTargetUserId删除
	 */
	 Integer deleteByRequestUserIdAndTargetUserId(@Param("requestUserId") String requestUserId,@Param("targetUserId") String targetUserId);


	/**
	 * 根据RequestUserIdAndTargetUserId获取对象
	 */
	 T selectByRequestUserIdAndTargetUserId(@Param("requestUserId") String requestUserId,@Param("targetUserId") String targetUserId);


}