package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.query.FriendRequestQuery;
import com.meetchat.entity.po.FriendRequest;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.vo.UserInfoVo;


/**
 * 好友申请表 业务接口
 */
public interface FriendRequestService {

	/**
	 * 根据条件查询列表
	 */
	List<FriendRequest> findListByParam(FriendRequestQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(FriendRequestQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<FriendRequest> findListByPage(FriendRequestQuery param);

	/**
	 * 新增
	 */
	Integer add(FriendRequest bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<FriendRequest> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<FriendRequest> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(FriendRequest bean,FriendRequestQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(FriendRequestQuery param);

	/**
	 * 根据Id查询对象
	 */
	FriendRequest getFriendRequestById(Long id);


	/**
	 * 根据Id修改
	 */
	Integer updateFriendRequestById(FriendRequest bean,Long id);


	/**
	 * 根据Id删除
	 */
	Integer deleteFriendRequestById(Long id);


	/**
	 * 根据RequestUserIdAndTargetUserId查询对象
	 */
	FriendRequest getFriendRequestByRequestUserIdAndTargetUserId(String requestUserId,String targetUserId);


	/**
	 * 根据RequestUserIdAndTargetUserId修改
	 */
	Integer updateFriendRequestByRequestUserIdAndTargetUserId(FriendRequest bean,String requestUserId,String targetUserId);


	/**
	 * 根据RequestUserIdAndTargetUserId删除
	 */
	Integer deleteFriendRequestByRequestUserIdAndTargetUserId(String requestUserId,String targetUserId);
    void  addFriend(String userId,String contactId,String remark);
	UserInfoVo search(String userId,String contactId);
	void manageRequest(String userId,String contactId,Integer status);
	void manageMyRequest(String userId,String contactId,Integer status);
}