package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.query.ChatSessionQuery;
import com.meetchat.entity.po.ChatSession;
import com.meetchat.entity.vo.PaginationResultVO;


/**
 * 聊天会话表 业务接口
 */
public interface ChatSessionService {

	/**
	 * 根据条件查询列表
	 */
	List<ChatSession> findListByParam(ChatSessionQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(ChatSessionQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<ChatSession> findListByPage(ChatSessionQuery param);

	/**
	 * 新增
	 */
	Integer add(ChatSession bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<ChatSession> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<ChatSession> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(ChatSession bean,ChatSessionQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(ChatSessionQuery param);

	/**
	 * 根据SessionId查询对象
	 */
	ChatSession getChatSessionBySessionId(String sessionId);


	/**
	 * 根据SessionId修改
	 */
	Integer updateChatSessionBySessionId(ChatSession bean,String sessionId);


	/**
	 * 根据SessionId删除
	 */
	Integer deleteChatSessionBySessionId(String sessionId);


	/**
	 * 根据UserIdAndTargetUserId查询对象
	 */
	ChatSession getChatSessionByUserIdAndTargetUserId(String userId,String targetUserId);


	/**
	 * 根据UserIdAndTargetUserId修改
	 */
	Integer updateChatSessionByUserIdAndTargetUserId(ChatSession bean,String userId,String targetUserId);


	/**
	 * 根据UserIdAndTargetUserId删除
	 */
	Integer deleteChatSessionByUserIdAndTargetUserId(String userId,String targetUserId);
    void createSession(String userId,String targetUserId,Integer contactType,String targetNickName,String botAvatarPath,String botDescription,String botSystemPrompt,String botWelcomeMsg,String botCategory,String botPersonality,String templateId,String botName);

	/**
	 * 创建好友间的双向会话
	 * - userA 的会话：userId=userId1, targetUserId=userId2, targetNickName=nickName2
	 * - userB 的会话：userId=userId2, targetUserId=userId1, targetNickName=nickName1
	 * 如果会话已存在则跳过
	 */
	void createFriendSessions(String userId1, String nickName1, String userId2, String nickName2);

	/**
	 * 创建机器人（添加好友 + 创建会话）
	 * @param userId 用户ID
	 * @param botName 机器人名称
	 * @param botAvatarPath 机器人头像路径
	 * @param botDescription 机器人描述
	 * @param botSystemPrompt 系统提示词
	 * @param botWelcomeMsg 欢迎语
	 * @param botCategory 机器人分类: FUNCTIONAL/COMPANION/HYBRID
	 * @param templateId 关联模板ID
	 * @param botPersonality 个性化性格参数
	 */
	void createRobot(String userId, String botName, String botAvatarPath,
	                 String botDescription, String botSystemPrompt, String botWelcomeMsg,
	                 String botCategory, String templateId, String botPersonality);
}