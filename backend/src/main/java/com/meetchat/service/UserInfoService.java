package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.query.UserInfoQuery;
import com.meetchat.entity.po.UserInfo;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.vo.UserInfoVo;

import javax.validation.constraints.NotEmpty;


/**
 *  业务接口
 */
public interface UserInfoService {

	/**
	 * 根据条件查询列表
	 */
	List<UserInfo> findListByParam(UserInfoQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(UserInfoQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param);

	/**
	 * 新增
	 */
	Integer add(UserInfo bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<UserInfo> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<UserInfo> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(UserInfo bean,UserInfoQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(UserInfoQuery param);

	/**
	 * 根据UserId查询对象
	 */
	UserInfo getUserInfoByUserId(String userId);


	/**
	 * 根据UserId修改
	 */
	Integer updateUserInfoByUserId(UserInfo bean,String userId);


	/**
	 * 根据UserId删除
	 */
	Integer deleteUserInfoByUserId(String userId);


	/**
	 * 根据Email查询对象
	 */
	UserInfo getUserInfoByEmail(String email);


	/**
	 * 根据Email修改
	 */
	Integer updateUserInfoByEmail(UserInfo bean,String email);


	/**
	 * 根据Email删除
	 */
	Integer deleteUserInfoByEmail(String email);
    void register(String email,
				  String password,

				  String nickName);
    UserInfoVo login(String email, String password);
    void logout(TokenUserInfoDto tokenUserInfoDto);

	void manageUser(String userId,Integer status);

	/**
	 * 管理员强制用户下线
	 * 流程：发送 FORCE_OFFLINE 通知 → 清除 Redis 登录态 → 断开 WebSocket
	 * @param userId 目标用户ID
	 */
	void forceOffline(String userId);
	}