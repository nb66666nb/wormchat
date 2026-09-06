package com.meetchat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.meetchat.entity.enums.PageSize;
import com.meetchat.entity.query.BotConversationSummaryQuery;
import com.meetchat.entity.po.BotConversationSummary;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.BotConversationSummaryMapper;
import com.meetchat.service.BotConversationSummaryService;
import com.meetchat.utils.StringTools;


/**
 *  业务接口实现
 */
@Service("botConversationSummaryService")
public class BotConversationSummaryServiceImpl implements BotConversationSummaryService {

	@Resource
	private BotConversationSummaryMapper<BotConversationSummary, BotConversationSummaryQuery> botConversationSummaryMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<BotConversationSummary> findListByParam(BotConversationSummaryQuery param) {
		return this.botConversationSummaryMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(BotConversationSummaryQuery param) {
		return this.botConversationSummaryMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<BotConversationSummary> findListByPage(BotConversationSummaryQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<BotConversationSummary> list = this.findListByParam(param);
		PaginationResultVO<BotConversationSummary> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(BotConversationSummary bean) {
		return this.botConversationSummaryMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<BotConversationSummary> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.botConversationSummaryMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<BotConversationSummary> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.botConversationSummaryMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(BotConversationSummary bean, BotConversationSummaryQuery param) {
		StringTools.checkParam(param);
		return this.botConversationSummaryMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(BotConversationSummaryQuery param) {
		StringTools.checkParam(param);
		return this.botConversationSummaryMapper.deleteByParam(param);
	}

	/**
	 * 根据Id获取对象
	 */
	@Override
	public BotConversationSummary getBotConversationSummaryById(Long id) {
		return this.botConversationSummaryMapper.selectById(id);
	}

	/**
	 * 根据Id修改
	 */
	@Override
	public Integer updateBotConversationSummaryById(BotConversationSummary bean, Long id) {
		return this.botConversationSummaryMapper.updateById(bean, id);
	}

	/**
	 * 根据Id删除
	 */
	@Override
	public Integer deleteBotConversationSummaryById(Long id) {
		return this.botConversationSummaryMapper.deleteById(id);
	}
}