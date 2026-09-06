package com.meetchat.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.xml.crypto.Data;

import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.enums.MessageTypeEnum;
import com.meetchat.entity.enums.MessageTypeImEnum;
import com.meetchat.entity.enums.UserContactTypeEnum;
import com.meetchat.entity.po.BotTemplate;
import com.meetchat.entity.po.BotUserProfile;
import com.meetchat.exception.BusinessException;
import com.meetchat.service.BotTemplateService;
import com.meetchat.service.BotUserProfileService;
import com.meetchat.service.UserFriendService;
import com.meetchat.utils.JsonUtils;
import com.meetchat.webSocket.SessionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meetchat.entity.enums.PageSize;
import com.meetchat.entity.query.ChatSessionQuery;
import com.meetchat.entity.po.ChatSession;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.ChatSessionMapper;
import com.meetchat.service.ChatSessionService;
import com.meetchat.utils.StringTools;


/**
 * 聊天会话表 业务接口实现
 */
@Service("chatSessionService")
public class ChatSessionServiceImpl implements ChatSessionService {

	@Resource
	private ChatSessionMapper<ChatSession, ChatSessionQuery> chatSessionMapper;

	@Resource
	private UserFriendService userFriendService;

	@Resource
	private BotTemplateService botTemplateService;

	@Resource
	private BotUserProfileService botUserProfileService;

	@Resource
	private SessionManager sessionManager;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<ChatSession> findListByParam(ChatSessionQuery param) {
		return this.chatSessionMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(ChatSessionQuery param) {
		return this.chatSessionMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<ChatSession> findListByPage(ChatSessionQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<ChatSession> list = this.findListByParam(param);
		PaginationResultVO<ChatSession> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(ChatSession bean) {
		return this.chatSessionMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<ChatSession> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatSessionMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<ChatSession> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatSessionMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(ChatSession bean, ChatSessionQuery param) {
		StringTools.checkParam(param);
		return this.chatSessionMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(ChatSessionQuery param) {
		StringTools.checkParam(param);
		return this.chatSessionMapper.deleteByParam(param);
	}

	/**
	 * 根据SessionId获取对象
	 */
	@Override
	public ChatSession getChatSessionBySessionId(String sessionId) {
		return this.chatSessionMapper.selectBySessionId(sessionId);
	}

	/**
	 * 根据SessionId修改
	 */
	@Override
	public Integer updateChatSessionBySessionId(ChatSession bean, String sessionId) {
		return this.chatSessionMapper.updateBySessionId(bean, sessionId);
	}

	/**
	 * 根据SessionId删除
	 */
	@Override
	public Integer deleteChatSessionBySessionId(String sessionId) {
		return this.chatSessionMapper.deleteBySessionId(sessionId);
	}

	/**
	 * 根据UserIdAndTargetUserId获取对象
	 */
	@Override
	public ChatSession getChatSessionByUserIdAndTargetUserId(String userId, String targetUserId) {
		return this.chatSessionMapper.selectByUserIdAndTargetUserId(userId, targetUserId);
	}

	/**
	 * 根据UserIdAndTargetUserId修改
	 */
	@Override
	public Integer updateChatSessionByUserIdAndTargetUserId(ChatSession bean, String userId, String targetUserId) {
		return this.chatSessionMapper.updateByUserIdAndTargetUserId(bean, userId, targetUserId);
	}

	/**
	 * 根据UserIdAndTargetUserId删除
	 */
	@Override
	public Integer deleteChatSessionByUserIdAndTargetUserId(String userId, String targetUserId) {
		return this.chatSessionMapper.deleteByUserIdAndTargetUserId(userId, targetUserId);
	}

	@Override
	public void createSession(String userId, String targetUserId, Integer contactType, String targetNickName,String botAvatarPath,String botDescription,String botSystemPrompt,String botWelcomeMsg,String botCategory,String botPersonality,String templateId,String botName) {
		if(userId==null){
			throw new BusinessException("userId不能为空");
		}
	    // 1. 按会话类型区分去重检查：同一用户对同一目标、同一类型的会话只能有一条
		// 修复：原实现按 (userId, targetUserId) 全局查重且不区分 session_type，
		//      命中群聊/机器人会话时复用分支不会修正 sessionType，导致用户会话被误标成群聊。
		ChatSessionQuery dedupQuery = new ChatSessionQuery();
		dedupQuery.setUserId(userId);
		dedupQuery.setTargetUserId(targetUserId);
		dedupQuery.setSessionType(contactType);
		List<ChatSession> dedupList = chatSessionMapper.selectList(dedupQuery);
		ChatSession existingSession = (dedupList != null && !dedupList.isEmpty()) ? dedupList.get(0) : null;
		if(existingSession!=null){
			// 会话已存在，更新后复用，避免唯一索引冲突或重复记录
			// 同时修正 sessionType 与对方昵称，确保复用后类型正确
			ChatSession updateSession = new ChatSession();
			updateSession.setSessionType(contactType);
			updateSession.setLastMessageTime(new Date().getTime());
			updateSession.setLastMessage("会话成功创建");
			if(targetNickName!=null){
				updateSession.setTargetNickName(targetNickName);
			}
			// 使用按 (user_id, target_user_id) 定位的更新，避免仅按 session_id 更新导致串表
			chatSessionMapper.updateByUserIdAndTargetUserId(updateSession, userId, targetUserId);
			MessageSendDto messageSendDto=new MessageSendDto();
			messageSendDto.setSessionId(existingSession.getSessionId());
			messageSendDto.setSendUserId(null);
			messageSendDto.setReceiveUserId(userId);
			messageSendDto.setSendTime(new Date().getTime());
			messageSendDto.setMessageContent("会话成功创建");
			messageSendDto.setSendUserNickName(null);
			messageSendDto.setMessageType(MessageTypeEnum.SESSION_MESSAGE.getType());
			messageSendDto.setMessageTypeIm(MessageTypeImEnum.IM.getType());
			messageSendDto.setExtendData(JsonUtils.convertObj2Json(existingSession));
			sessionManager.sendMessage(messageSendDto);
			return;
		}
		if(UserContactTypeEnum.USER.getType().equals(contactType)){
		    ChatSession	chatSessionU=new ChatSession();
			// 基于两个 userId 生成确定性 sessionId，双向会话自动相同，无需查询
			String sessionId = StringTools.getUserSessionId(userId, targetUserId);
			chatSessionU.setSessionId(sessionId);
            chatSessionU.setUserId(userId);
            chatSessionU.setTargetUserId(targetUserId);
            chatSessionU.setTargetNickName(targetNickName);
            chatSessionU.setSessionType(contactType);
            chatSessionMapper.insert(chatSessionU);
			MessageSendDto messageSendDto = new MessageSendDto();
			messageSendDto.setSessionId(sessionId);
			messageSendDto.setSendUserId(null);
			messageSendDto.setReceiveUserId(userId);
			messageSendDto.setSendTime(new Date().getTime());
			messageSendDto.setMessageContent("会话成功创建");
			messageSendDto.setSendUserNickName(null);
			messageSendDto.setMessageType(MessageTypeEnum.SESSION_MESSAGE.getType());
			messageSendDto.setMessageTypeIm(MessageTypeImEnum.IM.getType());
			messageSendDto.setExtendData(JsonUtils.convertObj2Json(chatSessionU));
			sessionManager.sendMessage(messageSendDto);

		}
		if (UserContactTypeEnum.GROUP.getType().equals(contactType)) {
			ChatSession chatSessionG = new ChatSession();
            // 群聊会话：sessionId = targetUserId（即 groupId），所有成员共享同一个 sessionId
            String sessionId = targetUserId;
            chatSessionG.setSessionId(sessionId);
            chatSessionG.setUserId(userId);
            chatSessionG.setTargetUserId(targetUserId);
            chatSessionG.setTargetNickName(targetNickName);
            chatSessionG.setSessionType(contactType);
            chatSessionMapper.insert(chatSessionG);
			MessageSendDto messageSendDtoG = new MessageSendDto();
			messageSendDtoG.setSessionId(sessionId);
			messageSendDtoG.setSendUserId(null);
			messageSendDtoG.setReceiveUserId(userId);
			messageSendDtoG.setSendTime(new Date().getTime());
			messageSendDtoG.setMessageContent("会话成功创建");
			messageSendDtoG.setSendUserNickName(null);
			messageSendDtoG.setMessageType(MessageTypeEnum.SESSION_MESSAGE.getType());
			messageSendDtoG.setMessageTypeIm(MessageTypeImEnum.IM.getType());
			messageSendDtoG.setExtendData(JsonUtils.convertObj2Json(chatSessionG));

			sessionManager.sendMessage(messageSendDtoG);
		}
		if(UserContactTypeEnum.ROBOT.getType().equals(contactType)){
			ChatSession chatSessionR = new ChatSession();
            String sessionId = StringTools.getBotSessionId();
            chatSessionR.setSessionId(sessionId);
            chatSessionR.setUserId(userId);
            chatSessionR.setTargetUserId(targetUserId);
            chatSessionR.setTargetNickName(targetNickName);
            chatSessionR.setSessionType(contactType);
			chatSessionR.setBotName(botName);
			chatSessionR.setBotCategory(botCategory);
			chatSessionR.setBotPersonality(botPersonality);
			chatSessionR.setTemplateId(templateId);
			chatSessionR.setBotDescription(botDescription);

			chatSessionR.setBotAvatarPath(botAvatarPath);
			chatSessionR.setBotSystemPrompt(botSystemPrompt);
			chatSessionR.setBotWelcomeMsg(botWelcomeMsg);
			chatSessionR.setLastMessage(botWelcomeMsg);
			chatSessionR.setLastMessageTime(new Date().getTime());
            chatSessionMapper.insert(chatSessionR);
			MessageSendDto messageSendDtoR = new MessageSendDto();
			messageSendDtoR.setSessionId(sessionId);
			messageSendDtoR.setSendUserId(null);
			messageSendDtoR.setReceiveUserId(userId);
			messageSendDtoR.setSendTime(new Date().getTime());
			messageSendDtoR.setMessageContent("会话成功创建");
			messageSendDtoR.setSendUserNickName(null);
			messageSendDtoR.setMessageType(MessageTypeEnum.SESSION_MESSAGE.getType());
			messageSendDtoR.setMessageTypeIm(MessageTypeImEnum.IM.getType());
			messageSendDtoR.setExtendData(JsonUtils.convertObj2Json(chatSessionR));
			sessionManager.sendMessage(messageSendDtoR);
			MessageSendDto messageSendDtoR2 = new MessageSendDto();
			messageSendDtoR2.setSessionId(sessionId);
			messageSendDtoR2.setSendUserId(targetUserId);
			messageSendDtoR2.setReceiveUserId(userId);
			messageSendDtoR2.setSendTime(new Date().getTime());
			messageSendDtoR2.setMessageContent(botWelcomeMsg);
			messageSendDtoR2.setSendUserNickName(targetNickName);
			messageSendDtoR2.setMessageType(MessageTypeEnum.CHAT_MESSAGE.getType());
			messageSendDtoR2.setMessageTypeIm(MessageTypeImEnum.IM.getType());
			messageSendDtoR2.setExtendData(JsonUtils.convertObj2Json(chatSessionR));
			sessionManager.sendMessage(messageSendDtoR2);
		}

	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void createFriendSessions(String userId1, String nickName1, String userId2, String nickName2) {
		if (StringTools.isEmpty(userId1) || StringTools.isEmpty(userId2)) {
			throw new BusinessException("用户ID不能为空");
		}
		if (userId1.equals(userId2)) {
			throw new BusinessException("不能与自己建立会话");
		}

		// 双向会话使用同一个 sessionId
		// createSession 内部会先查询对方→自己的会话，若已存在则复用其 sessionId
		createSession(userId1, userId2, UserContactTypeEnum.USER.getType(), nickName2,
				null, null, null, null, null, null, null, null);
		createSession(userId2, userId1, UserContactTypeEnum.USER.getType(), nickName1,
				null, null, null, null, null, null, null, null);
	}

	/**
	 * 创建机器人（添加好友 + 创建会话 + 初始化用户画像）
	 * 流程：
	 * 1. 如果指定了templateId，从BotTemplate加载默认配置（头像、提示词、欢迎语等）
	 * 2. 用户自定义参数覆盖模板默认值
	 * 3. 生成机器人ID，添加为好友（双向）
	 * 4. 创建机器人会话（包含botCategory和botPersonality）
	 * 5. 初始化BotUserProfile（用户画像），根据botCategory设置初始值
	 *    - 陪伴型/混合型：启用情绪基线、关怀计划
	 *    - 功能型：设置沟通风格为简洁
	 * 事务保证：所有操作同时成功或同时失败
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void createRobot(String userId, String botName, String botAvatarPath,
	                        String botDescription, String botSystemPrompt, String botWelcomeMsg,
	                        String botCategory, String templateId, String botPersonality) {
		if (StringTools.isEmpty(userId)) {
			throw new BusinessException("用户ID不能为空");
		}
		if (StringTools.isEmpty(botName)) {
			throw new BusinessException("机器人名称不能为空");
		}
		if (StringTools.isEmpty(botCategory)) {
			throw new BusinessException("机器人类型不能为空");
		}

		// 1. 如果指定了模板，加载模板默认值
		BotTemplate template = null;
		if (!StringTools.isEmpty(templateId)) {
			template = botTemplateService.getBotTemplateByTemplateId(templateId);
		}

		// 2. 模板默认值 + 用户自定义覆盖
		String finalAvatarPath = botAvatarPath;
		String finalSystemPrompt = botSystemPrompt;
		String finalWelcomeMsg = botWelcomeMsg;
		String finalDescription = botDescription;
		String finalCategory = botCategory;
		String finalPersonality = botPersonality;

		if (template != null) {
			// 用户未填的字段用模板默认值填充
			if (StringTools.isEmpty(finalAvatarPath)) {
				finalAvatarPath = template.getDefaultAvatarPath();
			}
			if (StringTools.isEmpty(finalSystemPrompt)) {
				finalSystemPrompt = template.getDefaultSystemPrompt();
			}
			if (StringTools.isEmpty(finalWelcomeMsg)) {
				finalWelcomeMsg = template.getDefaultWelcomeMsg();
			}
			if (StringTools.isEmpty(finalDescription)) {
				finalDescription = template.getTemplateName() + " - " + template.getTemplateCategory();
			}
			// 如果用户没有自定义personality，使用模板的性格参数
			if (StringTools.isEmpty(finalPersonality) && template.getOpenness() != null) {
				StringBuilder sb = new StringBuilder();
				sb.append("openness:").append(template.getOpenness()).append(";");
				if (template.getExtraversion() != null) sb.append("extraversion:").append(template.getExtraversion()).append(";");
				if (template.getAgreeableness() != null) sb.append("agreeableness:").append(template.getAgreeableness()).append(";");
				if (template.getHumor() != null) sb.append("humor:").append(template.getHumor()).append(";");
				if (template.getEmpathyLevel() != null) sb.append("empathy:").append(template.getEmpathyLevel()).append(";");
				if (template.getEmotionalSensitivity() != null) sb.append("sensitivity:").append(template.getEmotionalSensitivity()).append(";");
				if (template.getProactivity() != null) sb.append("proactivity:").append(template.getProactivity()).append(";");
				if (template.getGreetingEnabled() != null) sb.append("greeting:").append(template.getGreetingEnabled() == 1).append(";");
				if (template.getProactiveCareEnabled() != null) sb.append("proactiveCare:").append(template.getProactiveCareEnabled() == 1).append(";");
				if (template.getEnabledTools() != null) sb.append("tools:").append(template.getEnabledTools()).append(";");
				if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ';') {
					sb.setLength(sb.length() - 1);
				}
				finalPersonality = sb.toString();
			}
		}

		// 3. 生成机器人ID
		String botId = StringTools.getBotRandomId();

		// 4. 添加为好友（双向）
		com.meetchat.entity.po.UserFriend userToBot = new com.meetchat.entity.po.UserFriend();
		userToBot.setUserId(userId);
		userToBot.setFriendUserId(botId);
		userToBot.setRemarkName(botName);
		userFriendService.add(userToBot);

		com.meetchat.entity.po.UserFriend botToUser = new com.meetchat.entity.po.UserFriend();
		botToUser.setUserId(botId);
		botToUser.setFriendUserId(userId);
		botToUser.setRemarkName(userId);
		userFriendService.add(botToUser);

		// 5. 创建机器人会话（使用模板回退后的 final* 变量，确保模板默认值生效）
		createSession(userId, botId, UserContactTypeEnum.ROBOT.getType(), botName,
				finalAvatarPath, finalDescription, finalSystemPrompt, finalWelcomeMsg,
				finalCategory, finalPersonality, templateId, botName);

		// 5.1 创建机器人→用户的反向会话（用于 sendImMessage 权限校验：要求 bot→user 方向会话存在）
		// 与 USER 类型 createFriendSessions 双向创建会话的模式保持一致
		// 直接 insert，不走 createSession()，避免向无 WS 连接的机器人推送会话创建消息和重复欢迎语
		ChatSession existingReverse = chatSessionMapper.selectByUserIdAndTargetUserId(botId, userId);
		if (existingReverse == null) {
			ChatSession botToUserSession = new ChatSession();
			botToUserSession.setSessionId(StringTools.getBotSessionId());
			botToUserSession.setUserId(botId);
			botToUserSession.setTargetUserId(userId);
			botToUserSession.setTargetNickName(userId);
			botToUserSession.setSessionType(UserContactTypeEnum.ROBOT.getType());
			botToUserSession.setBotName(botName);
			botToUserSession.setBotCategory(finalCategory);
			botToUserSession.setBotPersonality(finalPersonality);
			botToUserSession.setTemplateId(templateId);
			botToUserSession.setBotDescription(finalDescription);
			botToUserSession.setBotAvatarPath(finalAvatarPath);
			botToUserSession.setBotSystemPrompt(finalSystemPrompt);
			botToUserSession.setBotWelcomeMsg(finalWelcomeMsg);
			botToUserSession.setLastMessage(finalWelcomeMsg);
			botToUserSession.setLastMessageTime(new Date().getTime());
			chatSessionMapper.insert(botToUserSession);
		}

		// 6. 初始化用户画像（BotUserProfile）- 根据botCategory定制
		BotUserProfile userProfile = new BotUserProfile();
		userProfile.setUserId(userId);
		userProfile.setBotId(botId);
		userProfile.setRelationshipLevel(1);
		userProfile.setRelationshipTrust(new java.math.BigDecimal("0.5"));
		userProfile.setInteractionCount(0);
		userProfile.setTotalChatDuration(0);
		userProfile.setConsecutiveNegativeDays(0);
		userProfile.setCrisisFlag(0);

		// 根据机器人类型设置不同的初始画像
		if ("COMPANION".equals(finalCategory)) {
			// 陪伴型：关注情绪、关怀
			userProfile.setEmotionBaseline("neutral");
			userProfile.setCommunicationStyle("warm");
			userProfile.setSupportPreference("emotional");
			userProfile.setCarePlan("daily_check_in");
		} else if ("FUNCTIONAL".equals(finalCategory)) {
			// 功能型：简洁高效
			userProfile.setEmotionBaseline("neutral");
			userProfile.setCommunicationStyle("concise");
			userProfile.setSupportPreference("informational");
		} else if ("HYBRID".equals(finalCategory)) {
			// 混合型：兼顾功能和情感
			userProfile.setEmotionBaseline("neutral");
			userProfile.setCommunicationStyle("balanced");
			userProfile.setSupportPreference("adaptive");
			userProfile.setCarePlan("weekly_check_in");
		}

		// 如果有模板，用模板参数覆盖
		if (template != null) {
			if (template.getEmpathyLevel() != null) {
				userProfile.setEmotionBaseline("neutral");
			}
			if (template.getResponseStyle() != null) {
				userProfile.setCommunicationStyle(template.getResponseStyle());
			}
		}

		botUserProfileService.add(userProfile);
	}
}