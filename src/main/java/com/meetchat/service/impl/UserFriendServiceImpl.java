package com.meetchat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import com.meetchat.ai.rag.MysqlDocumentStore;
import com.meetchat.entity.enums.UserContactTypeEnum;
import com.meetchat.entity.po.BotConversationSummary;
import com.meetchat.entity.po.BotEmotionLog;
import com.meetchat.entity.po.BotUserProfile;
import com.meetchat.entity.po.ChatImMessage;
import com.meetchat.entity.po.ChatSession;
import com.meetchat.entity.query.BotConversationSummaryQuery;
import com.meetchat.entity.query.BotEmotionLogQuery;
import com.meetchat.entity.query.BotUserProfileQuery;
import com.meetchat.entity.query.ChatImMessageQuery;
import com.meetchat.entity.query.ChatSessionQuery;
import com.meetchat.mappers.BotConversationSummaryMapper;
import com.meetchat.mappers.BotEmotionLogMapper;
import com.meetchat.mappers.BotUserProfileMapper;
import com.meetchat.mappers.ChatImMessageMapper;
import com.meetchat.mappers.ChatSessionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.meetchat.entity.po.FriendRequest;
import com.meetchat.entity.query.FriendRequestQuery;
import com.meetchat.exception.BusinessException;
import com.meetchat.mappers.FriendRequestMapper;
import org.springframework.stereotype.Service;

import com.meetchat.entity.enums.PageSize;
import com.meetchat.entity.query.UserFriendQuery;
import com.meetchat.entity.po.UserFriend;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.UserFriendMapper;
import com.meetchat.service.UserFriendService;
import com.meetchat.utils.StringTools;


/**
 * 好友关系表 业务接口实现
 */
@Service("userFriendService")
public class UserFriendServiceImpl implements UserFriendService {

	private static final Logger logger = LoggerFactory.getLogger(UserFriendServiceImpl.class);

	@Resource
	private UserFriendMapper<UserFriend, UserFriendQuery> userFriendMapper;
	@Resource
	private FriendRequestMapper<FriendRequest, FriendRequestQuery> friendRequestMapper;
	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;
	@Resource
	private ChatImMessageMapper<ChatImMessage, ChatImMessageQuery> chatImMessageMapper;
	@Resource
	private BotUserProfileMapper<BotUserProfile, BotUserProfileQuery> botUserProfileMapper;
	@Resource
	private BotConversationSummaryMapper<BotConversationSummary, BotConversationSummaryQuery> botConversationSummaryMapper;
	@Resource
	private BotEmotionLogMapper<BotEmotionLog, BotEmotionLogQuery> botEmotionLogMapper;
	@Resource
	private MysqlDocumentStore mysqlDocumentStore;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<UserFriend> findListByParam(UserFriendQuery param) {
		return this.userFriendMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(UserFriendQuery param) {
		return this.userFriendMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<UserFriend> findListByPage(UserFriendQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<UserFriend> list = this.findListByParam(param);
		PaginationResultVO<UserFriend> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(UserFriend bean) {
		return this.userFriendMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<UserFriend> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userFriendMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<UserFriend> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.userFriendMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(UserFriend bean, UserFriendQuery param) {
		StringTools.checkParam(param);
		return this.userFriendMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(UserFriendQuery param) {
		StringTools.checkParam(param);
		return this.userFriendMapper.deleteByParam(param);
	}

	/**
	 * 根据Id获取对象
	 */
	@Override
	public UserFriend getUserFriendById(Long id) {
		return this.userFriendMapper.selectById(id);
	}

	/**
	 * 根据Id修改
	 */
	@Override
	public Integer updateUserFriendById(UserFriend bean, Long id) {
		return this.userFriendMapper.updateById(bean, id);
	}

	/**
	 * 根据Id删除
	 */
	@Override
	public Integer deleteUserFriendById(Long id) {
		return this.userFriendMapper.deleteById(id);
	}

	/**
	 * 根据UserIdAndFriendUserId获取对象
	 */
	@Override
	public UserFriend getUserFriendByUserIdAndFriendUserId(String userId, String friendUserId) {
		return this.userFriendMapper.selectByUserIdAndFriendUserId(userId, friendUserId);
	}

	/**
	 * 根据UserIdAndFriendUserId修改
	 */
	@Override
	public Integer updateUserFriendByUserIdAndFriendUserId(UserFriend bean, String userId, String friendUserId) {
		return this.userFriendMapper.updateByUserIdAndFriendUserId(bean, userId, friendUserId);
	}

	/**
	 * 根据UserIdAndFriendUserId删除
	 */
	@Override
	public Integer deleteUserFriendByUserIdAndFriendUserId(String userId, String friendUserId) {
		return this.userFriendMapper.deleteByUserIdAndFriendUserId(userId, friendUserId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void deleteFriend(String userId, String contactId) {
		UserContactTypeEnum contactType = UserContactTypeEnum.getByPrefix(contactId);

		if (contactType == UserContactTypeEnum.ROBOT) {
			deleteRobot(userId, contactId);
			return;
		}

		UserFriend userFriend = userFriendMapper.selectByUserIdAndFriendUserId(userId, contactId);
		UserFriend userFriend1 = userFriendMapper.selectByUserIdAndFriendUserId(contactId, userId);
		FriendRequest friendRequest = friendRequestMapper.selectByRequestUserIdAndTargetUserId(userId, contactId);
		FriendRequest friendRequest1 = friendRequestMapper.selectByRequestUserIdAndTargetUserId(contactId, userId);

		if (userFriend == null || userFriend1 == null) {
			throw new BusinessException("该用户不是你的好友");
		}
		userFriendMapper.deleteByUserIdAndFriendUserId(userId, contactId);
		userFriendMapper.deleteByUserIdAndFriendUserId(contactId, userId);
		if (friendRequest != null) {
			friendRequestMapper.deleteByRequestUserIdAndTargetUserId(userId, contactId);
		}
		if (friendRequest1 != null) {
			friendRequestMapper.deleteByRequestUserIdAndTargetUserId(contactId, userId);
		}
	}

	private void deleteRobot(String userId, String botId) {
		UserFriend userFriend = userFriendMapper.selectByUserIdAndFriendUserId(userId, botId);
		if (userFriend == null) {
			throw new BusinessException("该机器人不是你的好友");
		}

		// 1. 删除双向好友关系
		userFriendMapper.deleteByUserIdAndFriendUserId(userId, botId);

		// 2. 删除双向会话 + 聊天记录
		ChatSession userSession = chatSessionMapper.selectByUserIdAndTargetUserId(userId, botId);
		if (userSession != null) {
			String userSessionId = userSession.getSessionId();
			// 删除用户侧聊天记录
			ChatImMessageQuery msgQuery = new ChatImMessageQuery();
			msgQuery.setSessionId(userSessionId);
			chatImMessageMapper.deleteByParam(msgQuery);
			logger.info("删除机器人会话消息: sessionId={}", userSessionId);
			// 删除用户侧会话
			chatSessionMapper.deleteBySessionId(userSessionId);
		}

		// 3. 删除机器人侧会话（机器人作为 user_id，用户作为 target_user_id）
		ChatSession botSession = chatSessionMapper.selectByUserIdAndTargetUserId(botId, userId);
		if (botSession != null) {
			String botSessionId = botSession.getSessionId();
			ChatImMessageQuery msgQuery = new ChatImMessageQuery();
			msgQuery.setSessionId(botSessionId);
			chatImMessageMapper.deleteByParam(msgQuery);
			logger.info("删除机器人侧会话消息: sessionId={}", botSessionId);
			chatSessionMapper.deleteBySessionId(botSessionId);
		}

		// 4. 删除用户画像
		BotUserProfileQuery profileQuery = new BotUserProfileQuery();
		profileQuery.setUserId(userId);
		profileQuery.setBotId(botId);
		botUserProfileMapper.deleteByParam(profileQuery);

		// 5. 删除对话摘要
		BotConversationSummaryQuery summaryQuery = new BotConversationSummaryQuery();
		summaryQuery.setUserId(userId);
		summaryQuery.setBotId(botId);
		botConversationSummaryMapper.deleteByParam(summaryQuery);

		// 6. 删除情绪日志
		BotEmotionLogQuery emotionQuery = new BotEmotionLogQuery();
		emotionQuery.setUserId(userId);
		emotionQuery.setBotId(botId);
		botEmotionLogMapper.deleteByParam(emotionQuery);

		// 7. 删除 RAG 知识库（该机器人的所有知识文档）
		try {
			int deletedRag = mysqlDocumentStore.deleteByBotId(botId);
			logger.info("删除机器人 RAG 知识库: botId={}, deleted={}", botId, deletedRag);
		} catch (Exception e) {
			logger.warn("删除机器人 RAG 知识库失败: botId={}", botId, e);
		}

		logger.info("机器人删除完成: userId={}, botId={}", userId, botId);
	}
}