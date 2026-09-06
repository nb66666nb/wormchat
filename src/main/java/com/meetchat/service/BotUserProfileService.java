package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.query.BotUserProfileQuery;
import com.meetchat.entity.po.BotUserProfile;
import com.meetchat.entity.vo.PaginationResultVO;


/**
 *  业务接口
 */
public interface BotUserProfileService {

	/**
	 * 根据条件查询列表
	 */
	List<BotUserProfile> findListByParam(BotUserProfileQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(BotUserProfileQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<BotUserProfile> findListByPage(BotUserProfileQuery param);

	/**
	 * 新增
	 */
	Integer add(BotUserProfile bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<BotUserProfile> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<BotUserProfile> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(BotUserProfile bean,BotUserProfileQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(BotUserProfileQuery param);

	/**
	 * 根据Id查询对象
	 */
	BotUserProfile getBotUserProfileById(Long id);


	/**
	 * 根据Id修改
	 */
	Integer updateBotUserProfileById(BotUserProfile bean,Long id);


	/**
	 * 根据Id删除
	 */
	Integer deleteBotUserProfileById(Long id);


	/**
	 * 根据UserIdAndBotId查询对象
	 */
	BotUserProfile getBotUserProfileByUserIdAndBotId(String userId,String botId);


	/**
	 * 根据UserIdAndBotId修改
	 */
	Integer updateBotUserProfileByUserIdAndBotId(BotUserProfile bean,String userId,String botId);


	/**
	 * 根据UserIdAndBotId删除
	 */
	Integer deleteBotUserProfileByUserIdAndBotId(String userId,String botId);

}