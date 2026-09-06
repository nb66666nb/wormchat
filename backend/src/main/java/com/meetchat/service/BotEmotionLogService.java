package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.query.BotEmotionLogQuery;
import com.meetchat.entity.po.BotEmotionLog;
import com.meetchat.entity.vo.PaginationResultVO;


/**
 *  业务接口
 */
public interface BotEmotionLogService {

	/**
	 * 根据条件查询列表
	 */
	List<BotEmotionLog> findListByParam(BotEmotionLogQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(BotEmotionLogQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<BotEmotionLog> findListByPage(BotEmotionLogQuery param);

	/**
	 * 新增
	 */
	Integer add(BotEmotionLog bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<BotEmotionLog> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<BotEmotionLog> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(BotEmotionLog bean,BotEmotionLogQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(BotEmotionLogQuery param);

	/**
	 * 根据Id查询对象
	 */
	BotEmotionLog getBotEmotionLogById(Long id);


	/**
	 * 根据Id修改
	 */
	Integer updateBotEmotionLogById(BotEmotionLog bean,Long id);


	/**
	 * 根据Id删除
	 */
	Integer deleteBotEmotionLogById(Long id);

}