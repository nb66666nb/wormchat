/**
 * ChatMessage 业务逻辑层 — 对齐后端 Service 层
 */
import * as mapper from '../mapper/ChatMessageMapper'

/**
 * 分页加载会议消息（游标分页，按时间倒序）
 * 使用 meetingId（会议ID）作为查询条件，与 Chat.vue 传入参数一致
 */
export const getMessageList = (meetingId, pageSize = 50, beforeTime = null) => {
  return mapper.listByMeetingId(meetingId, pageSize, beforeTime)
}

/**
 * 发送会议消息
 */
export const sendMessage = (data) => {
  const id = mapper.insert(data)
  return mapper.getById(id)
}

/**
 * 接收会议消息
 */
export const receiveMessage = (data) => {
  if (data.messageId) {
    const existing = mapper.getById(data.messageId)
    if (existing) {
      mapper.updateById(data.messageId, data)
      return existing
    }
  }
  const newId = mapper.insert({ ...data, status: 1 })
  return mapper.getById(newId)
}

/**
 * 批量同步会议消息
 */
export const syncMessages = (messages) => {
  const list = messages.map((m) => ({ ...m, status: 1 }))
  mapper.batchPut(list)
}

/**
 * 更新消息状态
 */
export const updateMessageStatus = (messageId, status) => {
  mapper.updateStatus(messageId, status)
}

/**
 * 按类型查询会议消息
 */
export const getMessagesByType = (meetingNo, messageType) => {
  return mapper.listByMeetingNoAndType(meetingNo, messageType)
}

/**
 * 统计会议消息数（按 meetingId）
 */
export const countMessages = (meetingId) => {
  return mapper.countByMeetingId(meetingId)
}

/**
 * 删除会议的所有消息（按 meetingId）
 */
export const deleteMessagesByMeetingId = (meetingId) => {
  mapper.deleteByMeetingId(meetingId)
}
