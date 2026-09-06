package com.meetchat.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.meetchat.entity.enums.PageSize;
import com.meetchat.entity.query.ChatMessageQuery;
import com.meetchat.entity.po.ChatMessage;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.ChatMessageMapper;
import com.meetchat.service.ChatMessageService;
import com.meetchat.utils.StringTools;


/**
 * 会议聊天记录表 业务接口实现
 */
@Service("chatMessageService")
public class ChatMessageServiceImpl implements ChatMessageService {

	@Resource
	private ChatMessageMapper<ChatMessage, ChatMessageQuery> chatMessageMapper;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<ChatMessage> findListByParam(ChatMessageQuery param) {
		return this.chatMessageMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(ChatMessageQuery param) {
		return this.chatMessageMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<ChatMessage> findListByPage(ChatMessageQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<ChatMessage> list = this.findListByParam(param);
		PaginationResultVO<ChatMessage> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(ChatMessage bean) {
		return this.chatMessageMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<ChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<ChatMessage> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.chatMessageMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(ChatMessage bean, ChatMessageQuery param) {
		StringTools.checkParam(param);
		return this.chatMessageMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(ChatMessageQuery param) {
		StringTools.checkParam(param);
		return this.chatMessageMapper.deleteByParam(param);
	}

	/**
	 * 根据MessageId获取对象
	 */
	@Override
	public ChatMessage getChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.selectByMessageId(messageId);
	}

	/**
	 * 根据MessageId修改
	 */
	@Override
	public Integer updateChatMessageByMessageId(ChatMessage bean, Long messageId) {
		return this.chatMessageMapper.updateByMessageId(bean, messageId);
	}

	/**
	 * 根据MessageId删除
	 */
	@Override
	public Integer deleteChatMessageByMessageId(Long messageId) {
		return this.chatMessageMapper.deleteByMessageId(messageId);
	}

	/**
	 * 根据MeetingNo获取对象
	 */
	@Override
	public ChatMessage getChatMessageByMeetingNo(String meetingNo) {
		return this.chatMessageMapper.selectByMeetingNo(meetingNo);
	}

	/**
	 * 根据MeetingNo修改
	 */
	@Override
	public Integer updateChatMessageByMeetingNo(ChatMessage bean, String meetingNo) {
		return this.chatMessageMapper.updateByMeetingNo(bean, meetingNo);
	}

	/**
	 * 根据MeetingNo删除
	 */
	@Override
	public Integer deleteChatMessageByMeetingNo(String meetingNo) {
		return this.chatMessageMapper.deleteByMeetingNo(meetingNo);
	}

	/**
	 * 根据MeetingId获取对象
	 */
	@Override
	public ChatMessage getChatMessageByMeetingId(String meetingId) {
		return this.chatMessageMapper.selectByMeetingId(meetingId);
	}

	/**
	 * 根据MeetingId修改
	 */
	@Override
	public Integer updateChatMessageByMeetingId(ChatMessage bean, String meetingId) {
		return this.chatMessageMapper.updateByMeetingId(bean, meetingId);
	}

	/**
	 * 根据MeetingId删除
	 */
	@Override
	public Integer deleteChatMessageByMeetingId(String meetingId) {
		return this.chatMessageMapper.deleteByMeetingId(meetingId);
	}
}