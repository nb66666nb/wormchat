package com.meetchat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.meetchat.entity.enums.PageSize;
import com.meetchat.entity.query.BotEmotionLogQuery;
import com.meetchat.entity.po.BotEmotionLog;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.BotEmotionLogMapper;
import com.meetchat.service.BotEmotionLogService;
import com.meetchat.utils.StringTools;


/**
 *  业务接口实现
 */
@Service("botEmotionLogService")
public class BotEmotionLogServiceImpl implements BotEmotionLogService {

	@Resource
	private BotEmotionLogMapper<BotEmotionLog, BotEmotionLogQuery> botEmotionLogMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<BotEmotionLog> findListByParam(BotEmotionLogQuery param) {
		return this.botEmotionLogMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(BotEmotionLogQuery param) {
		return this.botEmotionLogMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<BotEmotionLog> findListByPage(BotEmotionLogQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<BotEmotionLog> list = this.findListByParam(param);
		PaginationResultVO<BotEmotionLog> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(BotEmotionLog bean) {
		return this.botEmotionLogMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<BotEmotionLog> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.botEmotionLogMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<BotEmotionLog> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.botEmotionLogMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(BotEmotionLog bean, BotEmotionLogQuery param) {
		StringTools.checkParam(param);
		return this.botEmotionLogMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(BotEmotionLogQuery param) {
		StringTools.checkParam(param);
		return this.botEmotionLogMapper.deleteByParam(param);
	}

	/**
	 * 根据Id获取对象
	 */
	@Override
	public BotEmotionLog getBotEmotionLogById(Long id) {
		return this.botEmotionLogMapper.selectById(id);
	}

	/**
	 * 根据Id修改
	 */
	@Override
	public Integer updateBotEmotionLogById(BotEmotionLog bean, Long id) {
		return this.botEmotionLogMapper.updateById(bean, id);
	}

	/**
	 * 根据Id删除
	 */
	@Override
	public Integer deleteBotEmotionLogById(Long id) {
		return this.botEmotionLogMapper.deleteById(id);
	}
}