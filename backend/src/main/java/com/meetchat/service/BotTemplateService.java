package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.query.BotTemplateQuery;
import com.meetchat.entity.po.BotTemplate;
import com.meetchat.entity.vo.PaginationResultVO;


/**
 *  业务接口
 */
public interface BotTemplateService {

	/**
	 * 根据条件查询列表
	 */
	List<BotTemplate> findListByParam(BotTemplateQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(BotTemplateQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<BotTemplate> findListByPage(BotTemplateQuery param);

	/**
	 * 新增
	 */
	Integer add(BotTemplate bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<BotTemplate> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<BotTemplate> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(BotTemplate bean,BotTemplateQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(BotTemplateQuery param);

	/**
	 * 根据TemplateId查询对象
	 */
	BotTemplate getBotTemplateByTemplateId(String templateId);


	/**
	 * 根据TemplateId修改
	 */
	Integer updateBotTemplateByTemplateId(BotTemplate bean,String templateId);


	/**
	 * 根据TemplateId删除
	 */
	Integer deleteBotTemplateByTemplateId(String templateId);

}