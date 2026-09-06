package com.meetchat.service.impl;

import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.ArrayList;

import javax.annotation.Resource;

import com.meetchat.ai.service.AiChatService;
import com.meetchat.entity.AiReplyMessage;
import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.enums.MessageTypeEnum;
import com.meetchat.entity.enums.MessageTypeImEnum;
import com.meetchat.entity.enums.UserContactTypeEnum;
import com.meetchat.entity.enums.GroupStatusEnum;
import com.meetchat.entity.enums.GroupMemberStatusEnum;
import com.meetchat.entity.po.ChatSession;
import com.meetchat.entity.po.UserFriend;
import com.meetchat.entity.po.GroupInfo;
import com.meetchat.entity.po.GroupMember;
import com.meetchat.entity.query.ChatSessionQuery;
import com.meetchat.entity.query.UserFriendQuery;
import com.meetchat.exception.BusinessException;
import com.meetchat.mappers.ChatSessionMapper;
import com.meetchat.mappers.UserFriendMapper;
import com.meetchat.mappers.GroupMemberMapper;
import com.meetchat.service.GroupService;
import com.meetchat.webSocket.MessageDeliveryRetryManager;
import com.meetchat.webSocket.netty.MessageHandler;
import com.meetchat.redis.RedisUtils;
import com.meetchat.utils.SnowflakeIdGenerator;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.meetchat.entity.enums.PageSize;
import com.meetchat.entity.query.ChatImMessageQuery;
import com.meetchat.entity.po.ChatImMessage;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.ChatImMessageMapper;
import com.meetchat.service.ChatImMessageService;
import com.meetchat.utils.StringTools;


/**
 * IM即时通讯消息表 业务接口实现
 */
@Service("chatImMessageService")
public class ChatImMessageServiceImpl implements ChatImMessageService {

	private static final Logger logger = LoggerFactory.getLogger(ChatImMessageServiceImpl.class);

	@Resource
	private ChatImMessageMapper<ChatImMessage, ChatImMessageQuery> chatImMessageMapper;
    @Autowired
    private ChatSessionMapper <ChatSession, ChatSessionQuery> chatSessionMapper;
    @Autowired
    private UserFriendMapper<UserFriend, UserFriendQuery> userFriendMapper;
    @Autowired
    private GroupMemberMapper<GroupMember, Object> groupMemberMapper;
    @Autowired
    private GroupService groupService;
	@Lazy
	@Autowired
	private AiChatService aiChatService;
	@Autowired
	private MessageHandler messageHandler;
	@Autowired
	private MessageDeliveryRetryManager deliveryRetryManager;
	@Autowired
	private SnowflakeIdGenerator snowflakeIdGenerator;
	@Autowired
	private RedisUtils redisUtils;
	@Autowired
	private RabbitTemplate rabbitTemplate;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<ChatImMessage> findListByParam(ChatImMessageQuery param) {
		return this.chatImMessageMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(ChatImMessageQuery param) {
		return this.chatImMessageMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<ChatImMessage> findListByPage(ChatImMessageQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<ChatImMessage> list = this.findListByParam(param);
		PaginationResultVO<ChatImMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(ChatImMessage bean) {
		return this.chatImMessageMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<ChatImMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatImMessageMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<ChatImMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatImMessageMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(ChatImMessage bean, ChatImMessageQuery param) {
		StringTools.checkParam(param);
		return this.chatImMessageMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(ChatImMessageQuery param) {
		StringTools.checkParam(param);
		return this.chatImMessageMapper.deleteByParam(param);
	}

	/**
	 * 根据MessageId获取对象
	 */
	@Override
	public ChatImMessage getChatImMessageByMessageId(Long messageId) {
		return this.chatImMessageMapper.selectByMessageId(messageId);
	}

	/**
	 * 根据MessageId修改
	 */
	@Override
	public Integer updateChatImMessageByMessageId(ChatImMessage bean, Long messageId) {
		return this.chatImMessageMapper.updateByMessageId(bean, messageId);
	}

	/**
	 * 根据MessageId删除
	 */
	@Override
	public Integer deleteChatImMessageByMessageId(Long messageId) {
		return this.chatImMessageMapper.deleteByMessageId(messageId);
	}

	/**
	 * 发送IM消息
	 * 流程：
	 * 1. 参数校验
	 * 2. 幂等校验（messageOnlyId）
	 * 3. 消息入库
	 * 4. 更新会话最后消息
	 * 5. 事务提交后：WebSocket推送给接收方+发送方回声，机器人会话发MQ
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Long sendImMessage(MessageSendDto<String> messageSendDto) {

		if (messageSendDto == null) {
			throw new RuntimeException("消息内容不能为空");
		}
		if (StringTools.isEmpty(messageSendDto.getSessionId())) {
			throw new RuntimeException("会话ID不能为空");
		}
		if (StringTools.isEmpty(messageSendDto.getSendUserId())) {
			throw new RuntimeException("发送人不能为空");
		}

		// 0. 幂等校验：如果前端传了messageOnlyId，先查是否已存在
		if (!StringTools.isEmpty(messageSendDto.getMessageOnlyId())) {
			ChatImMessage existMsg = (ChatImMessage) this.chatImMessageMapper.selectByMessageOnlyId(
					messageSendDto.getMessageOnlyId());
			if (existMsg != null) {
				logger.info("消息幂等命中，跳过重复入库: messageOnlyId={}, messageId={}",
						messageSendDto.getMessageOnlyId(), existMsg.getMessageId());
				return existMsg.getMessageId();
			}
		}

		// 1. 校验会话是否存在
		ChatSession chatSession = chatSessionMapper.selectBySessionId(messageSendDto.getSessionId());
		if (chatSession == null) {
			throw new BusinessException("会话不存在");
		}

		// 2. 校验发送者的会话存在且sessionId匹配（修复原代码NPE风险）
		ChatSession chatSession1 = chatSessionMapper.selectByUserIdAndTargetUserId(
				messageSendDto.getSendUserId(), messageSendDto.getReceiveUserId());
		if (chatSession1 == null) {
			throw new BusinessException("你的会话不存在，无法发送消息");
		}
		if (!chatSession1.getSessionId().equals(messageSendDto.getSessionId())) {
			throw new BusinessException("会话参数有误，请联系管理员");
		}

		// 3. 校验消息类型与接收人前缀匹配
		UserContactTypeEnum contactType = UserContactTypeEnum.getByPrefix(messageSendDto.getReceiveUserId());
		if (contactType == null || !contactType.getType().equals(messageSendDto.getMessageSend2Type())) {
			throw new BusinessException("消息类型错误");
		}

		// 4. 根据联系人类型进行差异化权限校验
		String targetUserId = messageSendDto.getReceiveUserId();
		ChatSession chatSession2;

		if (UserContactTypeEnum.ROBOT.getType().equals(messageSendDto.getMessageSend2Type())) {
			// 机器人会话：校验机器人是否有与用户的会话
			chatSession2 = chatSessionMapper.selectByUserIdAndTargetUserId(
					targetUserId, messageSendDto.getSendUserId());
			if (chatSession2 == null) {
				throw new BusinessException("机器人会话不存在，无法发送消息");
			}
		} else if (UserContactTypeEnum.USER.getType().equals(messageSendDto.getMessageSend2Type())) {
			// 用户会话：校验双向好友关系 + 双向会话
			// 4.1 校验好友关系（双向）
			UserFriend friend1 = userFriendMapper.selectByUserIdAndFriendUserId(
					messageSendDto.getSendUserId(), targetUserId);
			if (friend1 == null) {
				throw new BusinessException("对方不是你的好友，无法发送消息");
			}
			UserFriend friend2 = userFriendMapper.selectByUserIdAndFriendUserId(
					targetUserId, messageSendDto.getSendUserId());
			if (friend2 == null) {
				throw new BusinessException("对方未添加你为好友，无法发送消息");
			}
			// 4.2 校验对方会话存在
			chatSession2 = chatSessionMapper.selectByUserIdAndTargetUserId(
					targetUserId, messageSendDto.getSendUserId());
			if (chatSession2 == null) {
				throw new BusinessException("对方会话不存在，无法发送消息");
			}
		} else if (UserContactTypeEnum.GROUP.getType().equals(messageSendDto.getMessageSend2Type())) {
			// 群聊会话：校验群聊存在、未解散、发送者是群成员
			GroupInfo groupInfo = groupService.getGroupInfo(targetUserId);
			if (groupInfo == null) {
				throw new BusinessException("群聊不存在");
			}
			if (GroupStatusEnum.DISSOLVED.getStatus().equals(groupInfo.getStatus())) {
				throw new BusinessException("该群聊已解散，无法发送消息");
			}
			if (GroupStatusEnum.BANNED.getStatus().equals(groupInfo.getStatus())) {
				throw new BusinessException("该群聊已被封禁，无法发送消息");
			}
			GroupMember senderMember = (GroupMember) groupMemberMapper.selectByGroupIdAndUserId(
					targetUserId, messageSendDto.getSendUserId());
			if (senderMember == null || !GroupMemberStatusEnum.NORMAL.getStatus().equals(senderMember.getStatus())) {
				throw new BusinessException("您不是该群聊的成员，无法发送消息");
			}
			// 群聊不需要对方会话校验
			chatSession2 = null;
		} else {
			throw new BusinessException("不支持的消息类型");
		}

		// 1. 消息入库
		Integer msgType = messageSendDto.getMessageType();
		boolean isGroup = UserContactTypeEnum.GROUP.getType().equals(messageSendDto.getMessageSend2Type());
		List<ChatImMessage> savedMessages = new ArrayList<>();
		Long firstMessageId = null;

		// 1.0 服务端统一分配：分布式消息ID（雪花算法，趋势递增）+ 会话内单调递增序号（Redis INCR）
		//    - 雪花ID替代MySQL自增主键，多节点部署不冲突，且可继续作为离线增量拉取游标
		//    - sessionSeq 用于客户端按会话做顺序校验与空洞检测（Redis故障时降级为null，客户端按messageId排序兜底）
		Long messageId = snowflakeIdGenerator.nextId();
		Long sessionSeq = nextSessionSeq(messageSendDto.getSessionId());

		if (isGroup) {
			// 群聊：单条存储，receiveUserId = groupId（所有成员共享同一条消息）
			// 在线成员通过 sendToGroup 广播接收；离线成员通过 pullOfflineGroupMessages 拉取
			ChatImMessage msg = new ChatImMessage();
			msg.setMessageId(messageId);
			msg.setSessionSeq(sessionSeq);
			msg.setMessageContent(messageSendDto.getMessageContent());
			msg.setSendUserId(messageSendDto.getSendUserId());
			msg.setSendUserName(messageSendDto.getSendUserNickName());
			msg.setReceiveUserId(messageSendDto.getReceiveUserId());
			msg.setMessageSendType(messageSendDto.getMessageSend2Type());
			msg.setSessionId(messageSendDto.getSessionId());
			msg.setSendTime(new Date().getTime());
			msg.setMessageType(messageSendDto.getMessageType());
			msg.setStatus(1);
			msg.setMessageOnlyId(messageSendDto.getMessageOnlyId());
			msg.setDeliveryStatus(0);
			if (msgType != null && (msgType == 32 || msgType == 33 || msgType == 34 || msgType == 35)) {
				msg.setFileId(messageSendDto.getFileId());
				msg.setFileName(messageSendDto.getFileName());
				msg.setFilePath(messageSendDto.getFilePath());
				msg.setFileType(messageSendDto.getFileType());
				msg.setFileSize(messageSendDto.getFileSize());
			}
			this.chatImMessageMapper.insert(msg);
			savedMessages.add(msg);
			firstMessageId = msg.getMessageId();
		} else {
			// 私聊/机器人：单条消息
			ChatImMessage chatImMessage = new ChatImMessage();
			chatImMessage.setMessageId(messageId);
			chatImMessage.setSessionSeq(sessionSeq);
			chatImMessage.setMessageContent(messageSendDto.getMessageContent());
			chatImMessage.setSendUserId(messageSendDto.getSendUserId());
			chatImMessage.setSendUserName(messageSendDto.getSendUserNickName());
			chatImMessage.setReceiveUserId(messageSendDto.getReceiveUserId());
			chatImMessage.setMessageSendType(messageSendDto.getMessageSend2Type());
			chatImMessage.setSessionId(messageSendDto.getSessionId());
			chatImMessage.setSendTime(new Date().getTime());
			chatImMessage.setMessageType(messageSendDto.getMessageType());
			chatImMessage.setStatus(1); // 已发送
			chatImMessage.setMessageOnlyId(messageSendDto.getMessageOnlyId());
			chatImMessage.setDeliveryStatus(0); // 待送达
			if (msgType != null && (msgType == 32 || msgType == 33 || msgType == 34 || msgType == 35)) {
				chatImMessage.setFileId(messageSendDto.getFileId());
				chatImMessage.setFileName(messageSendDto.getFileName());
				chatImMessage.setFilePath(messageSendDto.getFilePath());
				chatImMessage.setFileType(messageSendDto.getFileType());
				chatImMessage.setFileSize(messageSendDto.getFileSize());
			}
			this.chatImMessageMapper.insert(chatImMessage);
			savedMessages.add(chatImMessage);
			firstMessageId = chatImMessage.getMessageId();
		}

		// 2. 更新会话最后消息（群聊和私聊都通过 sessionId 更新）
		String lastMsg = messageSendDto.getMessageContent();
		if (lastMsg != null && lastMsg.length() > 100) {
			lastMsg = lastMsg.substring(0, 100) + "...";
		}
		ChatSession updateSession = new ChatSession();
		updateSession.setLastMessage(lastMsg);
		updateSession.setLastMessageTime(System.currentTimeMillis());
		chatSessionMapper.updateBySessionId(updateSession, messageSendDto.getSessionId());

		// 3. 推送和MQ放到事务提交后执行，保证入库成功才推送
		final Long resultMessageId = firstMessageId;
		final ChatSession senderSession = chatSession1;
		final Integer fileType = msgType;
		final List<ChatImMessage> finalSavedMessages = savedMessages;

		TransactionSynchronizationManager.registerSynchronization(
			new TransactionSynchronization() {
				@Override
			public void afterCommit() {
				try {
					if (isGroup) {
						// 群聊：经 RabbitMQ fanout 广播，各节点将消息投递给本地 ChannelGroup 中的在线成员
						// messageSend2Type=GROUP + receiveUserId=groupId → sessionManager.sendMessage 走 sendToGroup
						ChatImMessage savedMessage = finalSavedMessages.get(0);
						MessageSendDto<String> pushDto = buildPushDto(savedMessage, messageSendDto, fileType);
						messageHandler.sendMessage(pushDto);
					} else {
						// 私聊/机器人：推送给接收方 + 回声给发送方（均经集群广播路由到目标用户所在节点）
						ChatImMessage savedMessage = finalSavedMessages.get(0);
						MessageSendDto<String> pushDto = buildPushDto(savedMessage, messageSendDto, fileType);
						messageHandler.sendMessage(pushDto);

						// 私聊（真人接收方）：登记 ACK 超时重推（3s/6s/12s指数退避，重推幂等，客户端按messageOnlyId去重）
						if (UserContactTypeEnum.USER.getType().equals(messageSendDto.getMessageSend2Type())) {
							deliveryRetryManager.track(pushDto);
						}

						if (senderSession != null) {
							MessageSendDto<String> echoDto = buildPushDto(savedMessage, messageSendDto, fileType);
							echoDto.setReceiveUserId(messageSendDto.getSendUserId());
							messageHandler.sendMessage(echoDto);
						}
					}
				} catch (Exception e) {
					logger.error("IM消息推送异常: messageId={}", resultMessageId, e);
				}

					// 4. 机器人会话：异步发送MQ消息，由消费者处理AI回复
					if (UserContactTypeEnum.ROBOT.getType().equals(messageSendDto.getMessageSend2Type())) {
						try {
							AiReplyMessage aiMessage = new AiReplyMessage(
									messageSendDto.getSessionId(),
									messageSendDto.getSendUserId(),
									messageSendDto.getSendUserNickName(),
									messageSendDto.getMessageContent()
							);
							rabbitTemplate.convertAndSend(
									"ai.reply.exchange",
									"ai.reply.routing",
									aiMessage,
									new MessagePostProcessor() {
										@Override
										public org.springframework.amqp.core.Message postProcessMessage(
												org.springframework.amqp.core.Message message) {
											message.getMessageProperties().setDeliveryMode(
													org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
											return message;
										}
									}
							);
							logger.info("AI回复消息已发送到队列: sessionId={}", messageSendDto.getSessionId());
						} catch (Exception e) {
							logger.error("发送AI回复MQ消息异常: sessionId={}", messageSendDto.getSessionId(), e);
							// MQ发送失败降级为同步调用
							try {
								aiChatService.handleBotChat(
										messageSendDto.getSessionId(),
										messageSendDto.getSendUserId(),
										messageSendDto.getSendUserNickName(),
										messageSendDto.getMessageContent()
								);
							} catch (Exception ex) {
								logger.error("AI回复异常: sessionId={}", messageSendDto.getSessionId(), ex);
							}
						}
					}
				}
			}
		);

		return resultMessageId;
	}

	/**
	 * 构建推送 DTO（从已入库的消息 + 原始发送 DTO 提取字段）
	 */
	private MessageSendDto<String> buildPushDto(ChatImMessage savedMessage,
												MessageSendDto<String> messageSendDto,
												Integer fileType) {
		MessageSendDto<String> pushDto = new MessageSendDto<>();
		pushDto.setMessageId(savedMessage.getMessageId());
		pushDto.setSessionSeq(savedMessage.getSessionSeq());
		pushDto.setSessionId(savedMessage.getSessionId());
		pushDto.setSendUserId(messageSendDto.getSendUserId());
		pushDto.setSendUserNickName(messageSendDto.getSendUserNickName());
		pushDto.setReceiveUserId(savedMessage.getReceiveUserId());
		pushDto.setMessageContent(messageSendDto.getMessageContent());
		pushDto.setMessageType(messageSendDto.getMessageType());
		pushDto.setMessageTypeIm(MessageTypeImEnum.IM.getType());
		pushDto.setMessageSend2Type(messageSendDto.getMessageSend2Type());
		pushDto.setSendTime(savedMessage.getSendTime());
		pushDto.setStatus(1);
		pushDto.setMessageOnlyId(messageSendDto.getMessageOnlyId());
		if (fileType != null && (fileType == 32 || fileType == 33 || fileType == 34 || fileType == 35)) {
			pushDto.setFileId(messageSendDto.getFileId());
			pushDto.setFileName(messageSendDto.getFileName());
			pushDto.setFilePath(messageSendDto.getFilePath());
			pushDto.setFileType(messageSendDto.getFileType());
			pushDto.setFileSize(messageSendDto.getFileSize());
		}
		return pushDto;
	}

	@Override
	public void markDelivered(String messageOnlyId) {
		if (StringTools.isEmpty(messageOnlyId)) return;
		// 按 messageOnlyId 定位消息（雪花 messageId 前端 ACK 回传时会精度丢失，无法精确匹配）
		ChatImMessage msg = chatImMessageMapper.selectByMessageOnlyId(messageOnlyId);
		if (msg == null) {
			logger.warn("ACK消息不存在，忽略: messageOnlyId={}", messageOnlyId);
			return;
		}
		ChatImMessage update = new ChatImMessage();
		update.setDeliveryStatus(1); // 已送达
		chatImMessageMapper.updateByMessageId(update, msg.getMessageId());
		// 收到ACK：取消该消息的超时重推任务（重推与ACK存在竞态时，客户端按messageOnlyId幂等去重）
		deliveryRetryManager.ack(messageOnlyId);
		logger.info("消息已送达(ACK): messageOnlyId={}, messageId={}", messageOnlyId, msg.getMessageId());
	}

	/** 撤回时间限制：发送后 2 分钟内可撤回 */
	private static final long RECALL_TIME_LIMIT_MS = 2L * 60 * 1000;

	/**
	 * 撤回消息
	 *
	 * 校验链：
	 *   1. 消息存在（按 messageOnlyId 查询，避免雪花 messageId 前端精度丢失导致查不到）
	 *   2. 操作者必须是消息发送者本人（防越权撤回他人消息）
	 *   3. 消息发送时间在 RECALL_TIME_LIMIT_MS 内（防超时撤回）
	 *   4. 消息未被重复撤回（幂等）
	 *
	 * 处理：
	 *   - 标记 recalled=1、recall_time、message_content 替换为撤回提示文案
	 *   - 事务提交后构建 MESSAGE_RECALL(48) 通知推送：
	 *       群聊 → 经 messageHandler 广播给群内在线成员
	 *       私聊/机器人 → 推送接收方 + 回声发送方
	 *   - 前端收到通知后按 messageId 把本地对应消息标记为已撤回
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void recallMessage(String messageOnlyId, String operatorUserId) {
		if (StringTools.isEmpty(messageOnlyId)) {
			throw new BusinessException("messageOnlyId不能为空");
		}
		if (StringTools.isEmpty(operatorUserId)) {
			throw new BusinessException("操作者不能为空");
		}

		// 1. 按 messageOnlyId 查询消息（绕开雪花 messageId 前端精度丢失问题）
		ChatImMessage msg = chatImMessageMapper.selectByMessageOnlyId(messageOnlyId);
		if (msg == null) {
			throw new BusinessException("消息不存在或已被删除");
		}

		// 2. 仅发送者本人可撤回
		if (!operatorUserId.equals(msg.getSendUserId())) {
			throw new BusinessException("只能撤回自己发送的消息");
		}

		// 3. 幂等：已撤回则直接成功返回
		if (msg.getRecalled() != null && msg.getRecalled() == 1) {
			logger.info("消息已处于撤回状态，幂等返回: messageOnlyId={}", messageOnlyId);
			return;
		}

		// 4. 时限校验
		Long sendTime = msg.getSendTime();
		if (sendTime == null) {
			throw new BusinessException("消息时间异常，无法撤回");
		}
		if (System.currentTimeMillis() - sendTime > RECALL_TIME_LIMIT_MS) {
			throw new BusinessException("已超过撤回时限（2分钟），无法撤回");
		}

		// 5. 标记撤回：recalled=1 + recall_time + 替换内容为撤回提示文案
		String recallText = (msg.getSendUserName() == null ? "你" : msg.getSendUserName()) + "撤回了一条消息";
		ChatImMessage update = new ChatImMessage();
		update.setRecalled(1);
		update.setRecallTime(System.currentTimeMillis());
		update.setMessageContent(recallText);
		chatImMessageMapper.updateByMessageId(update, msg.getMessageId());

		// 6. 事务提交后推送撤回通知（与 sendImMessage 复用同一套集群广播链路）
		//    推送时用后端内存里精确的 messageId，不经过前端数值解析
		final ChatImMessage finalMsg = msg;
		final String finalRecallText = recallText;
		final Integer messageSend2Type = msg.getMessageSendType();
		TransactionSynchronizationManager.registerSynchronization(
			new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					try {
						MessageSendDto<String> recallDto = new MessageSendDto<>();
						recallDto.setMessageId(finalMsg.getMessageId());
						recallDto.setMessageOnlyId(finalMsg.getMessageOnlyId());
						recallDto.setSessionId(finalMsg.getSessionId());
						recallDto.setSendUserId(finalMsg.getSendUserId());
						recallDto.setSendUserNickName(finalMsg.getSendUserName());
						// receiveUserId 沿用原消息：群聊=groupId（走群广播），私聊=对方userId
						recallDto.setReceiveUserId(finalMsg.getReceiveUserId());
						recallDto.setMessageType(MessageTypeEnum.MESSAGE_RECALL.getType());
						recallDto.setMessageTypeIm(MessageTypeImEnum.IM.getType());
						recallDto.setMessageSend2Type(messageSend2Type);
						recallDto.setMessageContent(finalRecallText);
						recallDto.setSendTime(System.currentTimeMillis());
						recallDto.setStatus(1);

						if (UserContactTypeEnum.GROUP.getType().equals(messageSend2Type)) {
							// 群聊：撤回通知广播给群内所有在线成员
							messageHandler.sendMessage(recallDto);
						} else {
							// 私聊/机器人：推送接收方
							messageHandler.sendMessage(recallDto);
							// 回声发送方（让发送方本地也标记撤回，群聊时发送方已在群广播中收到）
							MessageSendDto<String> echoDto = new MessageSendDto<>();
							echoDto.setMessageId(finalMsg.getMessageId());
							echoDto.setMessageOnlyId(finalMsg.getMessageOnlyId());
							echoDto.setSessionId(finalMsg.getSessionId());
							echoDto.setSendUserId(finalMsg.getSendUserId());
							echoDto.setSendUserNickName(finalMsg.getSendUserName());
							echoDto.setReceiveUserId(finalMsg.getSendUserId());
							echoDto.setMessageType(MessageTypeEnum.MESSAGE_RECALL.getType());
							echoDto.setMessageTypeIm(MessageTypeImEnum.IM.getType());
							echoDto.setMessageSend2Type(messageSend2Type);
							echoDto.setMessageContent(finalRecallText);
							echoDto.setSendTime(System.currentTimeMillis());
							echoDto.setStatus(1);
							messageHandler.sendMessage(echoDto);
						}
						logger.info("消息撤回通知已推送: messageId={}, sessionId={}", finalMsg.getMessageId(), finalMsg.getSessionId());
					} catch (Exception e) {
						logger.error("消息撤回通知推送异常: messageId={}", finalMsg.getMessageId(), e);
					}
				}
			}
		);
	}

	/**
	 * 生成会话内服务端单调递增序号（Redis INCR，原子性天然保证不重不断）
	 * Redis 不可用时返回 null（降级：客户端按趋势递增的雪花 messageId 排序）
	 */
	private Long nextSessionSeq(String sessionId) {
		try {
			return redisUtils.incr("seq:session:" + sessionId);
		} catch (Exception e) {
			logger.error("生成sessionSeq失败，降级为null: sessionId={}", sessionId, e);
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<ChatImMessage> pullOfflineMessages(String receiveUserId, Long lastMessageId) {
		List<ChatImMessage> messages = (List<ChatImMessage>) chatImMessageMapper.selectAfterId(
				receiveUserId, lastMessageId, 200);
		logger.info("离线消息拉取: userId={}, lastMessageId={}, 返回{}条",
				receiveUserId, lastMessageId, messages != null ? messages.size() : 0);
		return messages;
	}

	/**
	 * 拉取用户的群聊离线消息（增量同步）
	 * 群消息单条存储（receive_user_id = groupId），因此按用户所在的所有群ID查询：
	 * 返回 receive_user_id IN (用户所在群) AND message_id > lastMessageId 的消息
	 * 前端按 backendMessageId 去重，重复下发（如自己发送的）无副作用
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<ChatImMessage> pullOfflineGroupMessages(String userId, Long lastMessageId) {
		List<String> groupIds = groupMemberMapper.selectActiveGroupIdsByUserId(userId);
		if (groupIds == null || groupIds.isEmpty()) {
			logger.info("群离线消息拉取: userId={} 未加入任何群，返回0条", userId);
			return new ArrayList<>();
		}
		List<ChatImMessage> messages = (List<ChatImMessage>) chatImMessageMapper.selectGroupMessagesAfterIds(
				groupIds, lastMessageId, 200);
		logger.info("群离线消息拉取: userId={}, 群数={}, lastMessageId={}, 返回{}条",
				userId, groupIds.size(), lastMessageId, messages != null ? messages.size() : 0);
		return messages;
	}
}