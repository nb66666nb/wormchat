package com.meetchat.service;

import java.util.List;

import com.meetchat.entity.query.ChatMessageQuery;
import com.meetchat.entity.po.ChatMessage;
import com.meetchat.entity.vo.PaginationResultVO;


/**
 * 会议聊天记录表 业务接口
 */
public interface ChatMessageService {

	/**
	 * 根据条件查询列表
	 */
	List<ChatMessage> findListByParam(ChatMessageQuery param);

	/**
	 * 根据条件查询列表
	 */
	Integer findCountByParam(ChatMessageQuery param);

	/**
	 * 分页查询
	 */
	PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery param);

	/**
	 * 新增
	 */
	Integer add(ChatMessage bean);

	/**
	 * 批量新增
	 */
	Integer addBatch(List<ChatMessage> listBean);

	/**
	 * 批量新增/修改
	 */
	Integer addOrUpdateBatch(List<ChatMessage> listBean);

	/**
	 * 多条件更新
	 */
	Integer updateByParam(ChatMessage bean,ChatMessageQuery param);

	/**
	 * 多条件删除
	 */
	Integer deleteByParam(ChatMessageQuery param);

	/**
	 * 根据MessageId查询对象
	 */
	ChatMessage getChatMessageByMessageId(Long messageId);


	/**
	 * 根据MessageId修改
	 */
	Integer updateChatMessageByMessageId(ChatMessage bean,Long messageId);


	/**
	 * 根据MessageId删除
	 */
	Integer deleteChatMessageByMessageId(Long messageId);


	/**
	 * 根据MeetingNo查询对象
	 */
	ChatMessage getChatMessageByMeetingNo(String meetingNo);


	/**
	 * 根据MeetingNo修改
	 */
	Integer updateChatMessageByMeetingNo(ChatMessage bean,String meetingNo);


	/**
	 * 根据MeetingNo删除
	 */
	Integer deleteChatMessageByMeetingNo(String meetingNo);


	/**
	 * 根据MeetingId查询对象
	 */
	ChatMessage getChatMessageByMeetingId(String meetingId);


	/**
	 * 根据MeetingId修改
	 */
	Integer updateChatMessageByMeetingId(ChatMessage bean,String meetingId);


	/**
	 * 根据MeetingId删除
	 */
	Integer deleteChatMessageByMeetingId(String meetingId);

}