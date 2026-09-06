package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.query.ChatImMessageQuery;
import com.meetchat.entity.po.ChatImMessage;
import com.meetchat.entity.vo.PaginationResultVO;


/**
 * IM即时通讯消息表 业务接口
 */
public interface ChatImMessageService {

	/**
	 * 根据条件查询列表
	 */
	List<ChatImMessage> findListByParam(ChatImMessageQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(ChatImMessageQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<ChatImMessage> findListByPage(ChatImMessageQuery param);

	/**
	 * 新增
	 */
	Integer add(ChatImMessage bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<ChatImMessage> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<ChatImMessage> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(ChatImMessage bean,ChatImMessageQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(ChatImMessageQuery param);

	/**
	 * 根据MessageId查询对象
	 */
	ChatImMessage getChatImMessageByMessageId(Long messageId);


	/**
	 * 根据MessageId修改
	 */
	Integer updateChatImMessageByMessageId(ChatImMessage bean,Long messageId);


	/**
	 * 根据MessageId删除
	 */
	Integer deleteChatImMessageByMessageId(Long messageId);

	/**
	 * 发送IM消息
	 * @param messageSendDto 消息发送DTO
	 * @return 后端消息ID
	 */
	Long sendImMessage(MessageSendDto<String> messageSendDto);

	/**
	 * 标记消息已送达（接收方ACK回调）
	 * 用 messageOnlyId 作为关联键：雪花 messageId 前端 ACK 回传时会精度丢失，无法精确匹配
	 * @param messageOnlyId 前端唯一消息标识
	 */
	void markDelivered(String messageOnlyId);

	/**
	 * 撤回消息
	 * 校验：仅发送者本人可撤回、发送时间在限定时长内（默认2分钟）
	 * 处理：标记 recalled=1 + recall_time，事务提交后推送撤回通知给接收方 + 回声发送方
	 *
	 * 注意：用 messageOnlyId（前端生成的 UUID 字符串）作为关联键，
	 *       而非 messageId。原因是雪花 messageId 超出 JS 安全整数范围，
	 *       前端 JSON 解析会精度丢失，传回后端无法精确匹配。
	 *       messageOnlyId 是字符串 UUID，不受数值精度影响。
	 *
	 * @param messageOnlyId 待撤回消息的前端唯一标识
	 * @param operatorUserId 操作者（撤回发起人）用户ID
	 */
	void recallMessage(String messageOnlyId, String operatorUserId);

	/**
	 * 拉取离线消息（增量拉取 messageId > lastMessageId）
	 * @param receiveUserId 当前用户ID
	 * @param lastMessageId 本地最新消息ID
	 * @return 未送达的消息列表
	 */
	List<ChatImMessage> pullOfflineMessages(String receiveUserId, Long lastMessageId);

	/**
	 * 拉取用户的群聊离线消息（增量拉取 messageId > lastMessageId）
	 * 群消息单条存储（receive_user_id = groupId），按用户所在的所有群查询
	 * @param userId 当前用户ID
	 * @param lastMessageId 本地最新群消息ID
	 * @return 群聊离线消息列表
	 */
	List<ChatImMessage> pullOfflineGroupMessages(String userId, Long lastMessageId);

}