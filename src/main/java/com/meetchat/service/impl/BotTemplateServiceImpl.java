package com.meetchat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.meetchat.entity.enums.PageSize;
import com.meetchat.entity.query.BotTemplateQuery;
import com.meetchat.entity.po.BotTemplate;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.BotTemplateMapper;
import com.meetchat.service.BotTemplateService;
import com.meetchat.utils.StringTools;


/**
 *  业务接口实现
 */
@Service("botTemplateService")
public class BotTemplateServiceImpl implements BotTemplateService {

	@Resource
	private BotTemplateMapper<BotTemplate, BotTemplateQuery> botTemplateMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<BotTemplate> findListByParam(BotTemplateQuery param) {
		return this.botTemplateMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(BotTemplateQuery param) {
		return this.botTemplateMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<BotTemplate> findListByPage(BotTemplateQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<BotTemplate> list = this.findListByParam(param);
		PaginationResultVO<BotTemplate> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(BotTemplate bean) {
		return this.botTemplateMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<BotTemplate> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.botTemplateMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<BotTemplate> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.botTemplateMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(BotTemplate bean, BotTemplateQuery param) {
		StringTools.checkParam(param);
		return this.botTemplateMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(BotTemplateQuery param) {
		StringTools.checkParam(param);
		return this.botTemplateMapper.deleteByParam(param);
	}

	/**
	 * 根据TemplateId获取对象
	 */
	@Override
	public BotTemplate getBotTemplateByTemplateId(String templateId) {
		return this.botTemplateMapper.selectByTemplateId(templateId);
	}

	/**
	 * 根据TemplateId修改
	 */
	@Override
	public Integer updateBotTemplateByTemplateId(BotTemplate bean, String templateId) {
		return this.botTemplateMapper.updateByTemplateId(bean, templateId);
	}

	/**
	 * 根据TemplateId删除
	 */
	@Override
	public Integer deleteBotTemplateByTemplateId(String templateId) {
		return this.botTemplateMapper.deleteByTemplateId(templateId);
	}
}