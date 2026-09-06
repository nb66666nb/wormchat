package com.meetchat.mappers;

import com.meetchat.entity.Dto.FriendWithUserInfoDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 好友关系表 数据库操作接口
 */
public interface UserFriendMapper<T,P> extends BaseMapper<T,P> {

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
	 * 根据UserIdAndFriendUserId更新
	 */
	 Integer updateByUserIdAndFriendUserId(@Param("bean") T t,@Param("userId") String userId,@Param("friendUserId") String friendUserId);


	/**
	 * 根据UserIdAndFriendUserId删除
	 */
	 Integer deleteByUserIdAndFriendUserId(@Param("userId") String userId,@Param("friendUserId") String friendUserId);


	/**
	 * 根据UserIdAndFriendUserId获取对象
	 */
	 T selectByUserIdAndFriendUserId(@Param("userId") String userId,@Param("friendUserId") String friendUserId);

	/**
	 * 查询非机器人好友列表（单条 SQL，消除 N+1 查询）
	 * 通过 LEFT JOIN chat_session 排除 bot_category 不为空的好友，
	 * LEFT JOIN user_info 获取好友昵称
	 */
	List<FriendWithUserInfoDto> selectNonBotFriends(@Param("userId") String userId);

}