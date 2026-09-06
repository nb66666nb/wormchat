package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.query.BotConversationSummaryQuery;
import com.meetchat.entity.po.BotConversationSummary;
import com.meetchat.entity.vo.PaginationResultVO;


/**
 *  业务接口
 */
public interface BotConversationSummaryService {

	/**
	 * 根据条件查询列表
	 */
	List<BotConversationSummary> findListByParam(BotConversationSummaryQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(BotConversationSummaryQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<BotConversationSummary> findListByPage(BotConversationSummaryQuery param);

	/**
	 * 新增
	 */
	Integer add(BotConversationSummary bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<BotConversationSummary> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<BotConversationSummary> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(BotConversationSummary bean,BotConversationSummaryQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(BotConversationSummaryQuery param);

	/**
	 * 根据Id查询对象
	 */
	BotConversationSummary getBotConversationSummaryById(Long id);


	/**
	 * 根据Id修改
	 */
	Integer updateBotConversationSummaryById(BotConversationSummary bean,Long id);


	/**
	 * 根据Id删除
	 */
	Integer deleteBotConversationSummaryById(Long id);

}