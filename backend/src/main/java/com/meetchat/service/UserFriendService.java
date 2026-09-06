package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.query.UserFriendQuery;
import com.meetchat.entity.po.UserFriend;
import com.meetchat.entity.vo.PaginationResultVO;


/**
 * 好友关系表 业务接口
 */
public interface UserFriendService {

	/**
	 * 根据条件查询列表
	 */
	List<UserFriend> findListByParam(UserFriendQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(UserFriendQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserFriend> findListByPage(UserFriendQuery param);

	/**
	 * 新增
	 */
	Integer add(UserFriend bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserFriend> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<UserFriend> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(UserFriend bean,UserFriendQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(UserFriendQuery param);

	/**
	 * 根据Id查询对象
	 */
	UserFriend getUserFriendById(Long id);


	/**
	 * 根据Id修改
	 */
	Integer updateUserFriendById(UserFriend bean,Long id);


	/**
	 * 根据Id删除
	 */
	Integer deleteUserFriendById(Long id);


	/**
	 * 根据UserIdAndFriendUserId查询对象
	 */
	UserFriend getUserFriendByUserIdAndFriendUserId(String userId,String friendUserId);


	/**
	 * 根据UserIdAndFriendUserId修改
	 */
	Integer updateUserFriendByUserIdAndFriendUserId(UserFriend bean,String userId,String friendUserId);


	/**
	 * 根据UserIdAndFriendUserId删除
	 */
	Integer deleteUserFriendByUserIdAndFriendUserId(String userId,String friendUserId);
	void  deleteFriend(String userId,String contactId);

}