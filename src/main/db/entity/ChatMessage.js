/**
 * 会议聊天记录实体 — 对齐后端 ChatMessage.java
 */

export const MeetingMessageTypeEnum = {
  TEXT: 1,
  FILE: 2,
  IMAGE: 3,
  VIDEO: 4
}

export const MeetingMessageStatusEnum = {
  SENDING: 0,
  SENT: 1
}

/**
 * 从数据库行转换为驼峰对象
 */
export const rowToEntity = (row) => {
  if (!row) return null
  return {
    messageId: row.message_id,
    meetingId: row.meeting_id,
    meetingNo: row.meeting_no,
    messageSendType: row.message_send_type,
    sendUserId: row.send_user_id,
    sendUserNickName: row.send_user_nick_name,
    receiveUserId: row.receive_user_id,
    messageType: row.message_type,
    messageContent: row.message_content,
    fileSize: row.file_size,
    fileName: row.file_name,
    fileId: row.file_id,
    filePath: row.file_path,
    contentType: row.content_type,
    fileType: row.file_type,
    extendData: row.extend_data,
    status: row.status,
    sendTime: row.send_time,
    createTime: row.create_time
  }
}

/**
 * 从驼峰对象转换为数据库参数
 */
export const entityToParams = (data) => {
  return {
    messageId: data.messageId ?? null,
    meetingId: data.meetingId || '',
    meetingNo: data.meetingNo || '',
    messageSendType: data.messageSendType ?? 0,
    sendUserId: data.sendUserId || '',
    sendUserNickName: data.sendUserNickName || '',
    receiveUserId: data.receiveUserId || '',
    messageType: data.messageType ?? MeetingMessageTypeEnum.TEXT,
    messageContent: data.messageContent || '',
    fileSize: data.fileSize || 0,
    fileName: data.fileName || '',
    fileId: data.fileId || '',
    filePath: data.filePath || '',
    contentType: data.contentType || '',
    fileType: data.fileType ?? 0,
    extendData: data.extendData || '',
    status: data.status ?? MeetingMessageStatusEnum.SENDING,
    sendTime: data.sendTime || Date.now(),
    createTime: data.createTime || Date.now()
  }
}
