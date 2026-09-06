/**
 * ChatMessage 数据访问层 — 对齐后端 Mapper 层
 * 使用 better-sqlite3 同步API
 */
import { getDb } from '../Database'
import { rowToEntity, entityToParams } from '../entity/ChatMessage'

/**
 * 根据主键查询
 */
export const getById = (messageId) => {
  const db = getDb()
  const row = db.prepare('SELECT * FROM chat_message WHERE message_id = ?').get(messageId)
  return rowToEntity(row)
}

/**
 * 根据会议号分页查询消息（按发送时间倒序）
 * @param {string} meetingNo 会议号
 * @param {number} pageSize 每页条数
 * @param {number} beforeTime 游标：加载此时间之前的消息
 */
export const listByMeetingNo = (meetingNo, pageSize = 50, beforeTime = null) => {
  const db = getDb()
  let rows
  if (beforeTime) {
    rows = db.prepare(
      'SELECT * FROM chat_message WHERE meeting_no = ? AND send_time < ? ORDER BY send_time DESC LIMIT ?'
    ).all(meetingNo, beforeTime, pageSize)
  } else {
    rows = db.prepare(
      'SELECT * FROM chat_message WHERE meeting_no = ? ORDER BY send_time DESC LIMIT ?'
    ).all(meetingNo, pageSize)
  }
  return rows.map(rowToEntity)
}

/**
 * 根据会议ID分页查询消息（按发送时间倒序）
 * meetingId 是 Chat.vue 传入的参数，对应 meeting_id 列
 * @param {string} meetingId 会议ID
 * @param {number} pageSize 每页条数
 * @param {number} beforeTime 游标：加载此时间之前的消息
 */
export const listByMeetingId = (meetingId, pageSize = 50, beforeTime = null) => {
  const db = getDb()
  let rows
  if (beforeTime) {
    rows = db.prepare(
      'SELECT * FROM chat_message WHERE meeting_id = ? AND send_time < ? ORDER BY send_time DESC LIMIT ?'
    ).all(meetingId, beforeTime, pageSize)
  } else {
    rows = db.prepare(
      'SELECT * FROM chat_message WHERE meeting_id = ? ORDER BY send_time DESC LIMIT ?'
    ).all(meetingId, pageSize)
  }
  return rows.map(rowToEntity)
}

/**
 * 根据会议号和消息类型查询
 */
export const listByMeetingNoAndType = (meetingNo, messageType) => {
  const db = getDb()
  const rows = db.prepare(
    'SELECT * FROM chat_message WHERE meeting_no = ? AND message_type = ? ORDER BY send_time DESC'
  ).all(meetingNo, messageType)
  return rows.map(rowToEntity)
}

/**
 * 根据发送人查询消息
 */
export const listBySendUserId = (sendUserId) => {
  const db = getDb()
  const rows = db.prepare(
    'SELECT * FROM chat_message WHERE send_user_id = ? ORDER BY send_time DESC'
  ).all(sendUserId)
  return rows.map(rowToEntity)
}

/**
 * 统计会议的消息数
 */
export const countByMeetingNo = (meetingNo) => {
  const db = getDb()
  const row = db.prepare('SELECT COUNT(*) as cnt FROM chat_message WHERE meeting_no = ?').get(meetingNo)
  return row.cnt
}

/**
 * 统计会议ID的消息数
 */
export const countByMeetingId = (meetingId) => {
  const db = getDb()
  const row = db.prepare('SELECT COUNT(*) as cnt FROM chat_message WHERE meeting_id = ?').get(meetingId)
  return row.cnt
}

/**
 * 插入一条记录
 */
export const insert = (data) => {
  const db = getDb()
  const p = entityToParams(data)
  const result = db.prepare(`
    INSERT INTO chat_message (
      message_id, meeting_id, meeting_no, message_send_type,
      send_user_id, send_user_nick_name, receive_user_id,
      message_type, message_content,
      file_size, file_name, file_id, file_path, content_type, file_type,
      extend_data, status, send_time, create_time
    ) VALUES (
      $messageId, $meetingId, $meetingNo, $messageSendType,
      $sendUserId, $sendUserNickName, $receiveUserId,
      $messageType, $messageContent,
      $fileSize, $fileName, $fileId, $filePath, $contentType, $fileType,
      $extendData, $status, $sendTime, $createTime
    )
  `).run(p)
  return p.messageId ?? result.lastInsertRowid
}

/**
 * 批量插入/更新（事务，OR REPLACE）
 */
export const batchPut = (list) => {
  const db = getDb()
  const stmt = db.prepare(`
    INSERT OR REPLACE INTO chat_message (
      message_id, meeting_id, meeting_no, message_send_type,
      send_user_id, send_user_nick_name, receive_user_id,
      message_type, message_content,
      file_size, file_name, file_id, file_path, content_type, file_type,
      extend_data, status, send_time, create_time
    ) VALUES (
      $messageId, $meetingId, $meetingNo, $messageSendType,
      $sendUserId, $sendUserNickName, $receiveUserId,
      $messageType, $messageContent,
      $fileSize, $fileName, $fileId, $filePath, $contentType, $fileType,
      $extendData, $status, $sendTime, $createTime
    )
  `)
  const putMany = db.transaction((items) => {
    for (const item of items) {
      stmt.run(entityToParams(item))
    }
  })
  putMany(list)
}

/**
 * 更新一条记录
 */
export const updateById = (messageId, changes) => {
  const db = getDb()
  const sets = []
  const params = {}
  for (const [key, value] of Object.entries(changes)) {
    const col = key.replace(/[A-Z]/g, (m) => '_' + m.toLowerCase())
    sets.push(`${col} = $${key}`)
    params[key] = value
  }
  if (sets.length === 0) return
  params.messageId = messageId
  db.prepare(`UPDATE chat_message SET ${sets.join(', ')} WHERE message_id = $messageId`).run(params)
}

/**
 * 更新消息状态
 */
export const updateStatus = (messageId, status) => {
  const db = getDb()
  db.prepare('UPDATE chat_message SET status = ? WHERE message_id = ?').run(status, messageId)
}

/**
 * 根据会议号删除所有消息
 */
export const deleteByMeetingNo = (meetingNo) => {
  const db = getDb()
  db.prepare('DELETE FROM chat_message WHERE meeting_no = ?').run(meetingNo)
}

/**
 * 根据会议ID删除所有消息
 */
export const deleteByMeetingId = (meetingId) => {
  const db = getDb()
  db.prepare('DELETE FROM chat_message WHERE meeting_id = ?').run(meetingId)
}
