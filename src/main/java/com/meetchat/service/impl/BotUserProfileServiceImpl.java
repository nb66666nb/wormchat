package com.meetchat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.meetchat.entity.enums.PageSize;
import com.meetchat.entity.query.BotUserProfileQuery;
import com.meetchat.entity.po.BotUserProfile;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.BotUserProfileMapper;
import com.meetchat.service.BotUserProfileService;
import com.meetchat.utils.StringTools;


/**
 *  业务接口实现
 */
@Service("botUserProfileService")
public class BotUserProfileServiceImpl implements BotUserProfileService {

	@Resource
	private BotUserProfileMapper<BotUserProfile, BotUserProfileQuery> botUserProfileMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<BotUserProfile> findListByParam(BotUserProfileQuery param) {
		return this.botUserProfileMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(BotUserProfileQuery param) {
		return this.botUserProfileMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<BotUserProfile> findListByPage(BotUserProfileQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<BotUserProfile> list = this.findListByParam(param);
		PaginationResultVO<BotUserProfile> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(BotUserProfile bean) {
		return this.botUserProfileMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<BotUserProfile> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.botUserProfileMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<BotUserProfile> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.botUserProfileMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(BotUserProfile bean, BotUserProfileQuery param) {
		StringTools.checkParam(param);
		return this.botUserProfileMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(BotUserProfileQuery param) {
		StringTools.checkParam(param);
		return this.botUserProfileMapper.deleteByParam(param);
	}

	/**
	 * 根据Id获取对象
	 */
	@Override
	public BotUserProfile getBotUserProfileById(Long id) {
		return this.botUserProfileMapper.selectById(id);
	}

	/**
	 * 根据Id修改
	 */
	@Override
	public Integer updateBotUserProfileById(BotUserProfile bean, Long id) {
		return this.botUserProfileMapper.updateById(bean, id);
	}

	/**
	 * 根据Id删除
	 */
	@Override
	public Integer deleteBotUserProfileById(Long id) {
		return this.botUserProfileMapper.deleteById(id);
	}

	/**
	 * 根据UserIdAndBotId获取对象
	 */
	@Override
	public BotUserProfile getBotUserProfileByUserIdAndBotId(String userId, String botId) {
		return this.botUserProfileMapper.selectByUserIdAndBotId(userId, botId);
	}

	/**
	 * 根据UserIdAndBotId修改
	 */
	@Override
	public Integer updateBotUserProfileByUserIdAndBotId(BotUserProfile bean, String userId, String botId) {
		return this.botUserProfileMapper.updateByUserIdAndBotId(bean, userId, botId);
	}

	/**
	 * 根据UserIdAndBotId删除
	 */
	@Override
	public Integer deleteBotUserProfileByUserIdAndBotId(String userId, String botId) {
		return this.botUserProfileMapper.deleteByUserIdAndBotId(userId, botId);
	}
}