/**
 * IM即时通讯消息实体 — 对齐后端 ChatImMessage.java
 */

export const ImMessageTypeEnum = {
  TEXT: 30,
  FILE: 32,
  IMAGE: 33,
  VIDEO: 34,
  AUDIO: 35,
  // 36 已被后端 SESSION_MESSAGE 占用，表情消息暂用 37
  EMOJI: 37
}

export const ImMessageSendTypeEnum = {
  PRIVATE: 0,
  GROUP: 1,
  ROBOT: 2
}

export const ImMessageStatusEnum = {
  SENDING: 0,
  SENT: 1
}

/**
 * 从数据库行转换为驼峰对象
 *
 * 字段说明：
 *   messageId         - 本地 SQLite 自增主键，仅用于本地查询/排序
 *   backendMessageId  - 后端全局自增 messageId，用于增量同步游标和 ACK
 */
export const rowToEntity = (row) => {
  if (!row) return null
  return {
    messageId: row.message_id,
    backendMessageId: row.backend_message_id || 0,
    sessionId: row.session_id,
    sendUserId: row.send_user_id,
    sendUserName: row.send_user_name,
    sendUserAvatar: row.send_user_avatar,
    receiveUserId: row.receive_user_id,
    messageType: row.message_type,
    messageContent: row.message_content,
    fileName: row.file_name,
    fileSize: row.file_size,
    fileId: row.file_id,
    filePath: row.file_path,
    contentType: row.content_type,
    fileType: row.file_type,
    messageSendType: row.message_send_type,
    messageOnlyId: row.message_only_id || '',
    status: row.status,
    sendTime: row.send_time,
    recalled: row.recalled ?? 0,
    recallTime: row.recall_time ?? 0
  }
}

export const entityToParams = (data) => {
  return {
    // messageId 不在 INSERT 时写入，让 SQLite 自增；仅用于 UPDATE 定位
    messageId: data.messageId ?? null,
    backendMessageId: data.backendMessageId ?? 0,
    sessionId: data.sessionId || '',
    sendUserId: data.sendUserId || '',
    sendUserName: data.sendUserName || '',
    sendUserAvatar: data.sendUserAvatar || '',
    receiveUserId: data.receiveUserId || '',
    messageType: data.messageType ?? ImMessageTypeEnum.TEXT,
    messageContent: data.messageContent || '',
    fileName: data.fileName || '',
    fileSize: data.fileSize || 0,
    fileId: data.fileId || '',
    filePath: data.filePath || '',
    contentType: data.contentType || '',
    fileType: data.fileType ?? 0,
    messageSendType: data.messageSendType ?? ImMessageSendTypeEnum.PRIVATE,
    messageOnlyId: data.messageOnlyId || '',
    status: data.status ?? ImMessageStatusEnum.SENDING,
    sendTime: data.sendTime || Date.now(),
    recalled: data.recalled ?? 0,
    recallTime: data.recallTime ?? 0
  }
}
