/**
 * ChatImMessage 数据访问层 — 对齐后端 Mapper 层
 * 使用 better-sqlite3 同步API
 *
 * 字段语义：
 *   message_id         - 本地 SQLite 自增主键（INSERT 时由 SQLite 分配）
 *   backend_message_id - 后端全局自增 messageId（用于增量同步游标、ACK、去重）
 *   message_only_id    - 前端生成的 UUID（用于幂等去重）
 */
import { getDb } from '../Database'
import { rowToEntity, entityToParams } from '../entity/ChatImMessage'

/**
 * 根据本地主键 message_id 查询
 */
export const getById = (messageId) => {
  const db = getDb()
  const row = db.prepare('SELECT * FROM chat_im_message WHERE message_id = ?').get(messageId)
  return rowToEntity(row)
}

/**
 * 根据后端 messageId（backend_message_id）查询
 * 用于：拉取离线消息后去重、ACK 时定位
 */
export const getByBackendMessageId = (backendMessageId) => {
  const db = getDb()
  const row = db.prepare('SELECT * FROM chat_im_message WHERE backend_message_id = ?').get(backendMessageId)
  return rowToEntity(row)
}

/**
 * 根据会话ID分页查询消息（按发送时间倒序）
 * @param {string} sessionId 会话ID
 * @param {number} pageSize 每页条数
 * @param {number} beforeTime 游标：加载此时间之前的消息
 */
export const listBySessionId = (sessionId, pageSize = 20, beforeTime = null) => {
  const db = getDb()
  let rows
  if (beforeTime) {
    rows = db.prepare(
      'SELECT * FROM chat_im_message WHERE session_id = ? AND send_time < ? ORDER BY send_time DESC LIMIT ?'
    ).all(sessionId, beforeTime, pageSize)
  } else {
    rows = db.prepare(
      'SELECT * FROM chat_im_message WHERE session_id = ? ORDER BY send_time DESC LIMIT ?'
    ).all(sessionId, pageSize)
  }
  return rows.map(rowToEntity)
}

/**
 * 根据会话ID查询最新N条消息
 */
export const listLatestBySessionId = (sessionId, limit = 1) => {
  const db = getDb()
  const rows = db.prepare(
    'SELECT * FROM chat_im_message WHERE session_id = ? ORDER BY send_time DESC LIMIT ?'
  ).all(sessionId, limit)
  return rows.map(rowToEntity)
}

/**
 * 根据会话ID和消息类型查询
 */
export const listBySessionIdAndType = (sessionId, messageType) => {
  const db = getDb()
  const rows = db.prepare(
    'SELECT * FROM chat_im_message WHERE session_id = ? AND message_type = ? ORDER BY send_time DESC'
  ).all(sessionId, messageType)
  return rows.map(rowToEntity)
}

/**
 * 根据发送人查询消息
 */
export const listBySendUserId = (sendUserId) => {
  const db = getDb()
  const rows = db.prepare(
    'SELECT * FROM chat_im_message WHERE send_user_id = ? ORDER BY send_time DESC'
  ).all(sendUserId)
  return rows.map(rowToEntity)
}

/**
 * 统计会话的消息数
 */
export const countBySessionId = (sessionId) => {
  const db = getDb()
  const row = db.prepare('SELECT COUNT(*) as cnt FROM chat_im_message WHERE session_id = ?').get(sessionId)
  return row.cnt
}

/**
 * 插入一条记录
 * 注意：message_id 不在 INSERT 列中，由 SQLite 自增分配
 * @returns {number} 新行的本地 message_id
 */
export const insert = (data) => {
  const db = getDb()
  const p = entityToParams(data)
  try {
    const result = db.prepare(`
      INSERT INTO chat_im_message (
        backend_message_id, session_id, send_user_id, send_user_name, send_user_avatar,
        receive_user_id, message_type, message_content,
        file_name, file_size, file_id, file_path, content_type, file_type,
        message_send_type, message_only_id, status, send_time, recalled, recall_time
      ) VALUES (
        $backendMessageId, $sessionId, $sendUserId, $sendUserName, $sendUserAvatar,
        $receiveUserId, $messageType, $messageContent,
        $fileName, $fileSize, $fileId, $filePath, $contentType, $fileType,
        $messageSendType, $messageOnlyId, $status, $sendTime, $recalled, $recallTime
      )
    `).run(p)
    return Number(result.lastInsertRowid)
  } catch (e) {
    // 竞态场景：另一线程已插入相同 backend_message_id，返回已有行 ID
    if (e.message?.includes('UNIQUE constraint failed') && data.backendMessageId) {
      const existing = db.prepare(
        'SELECT message_id FROM chat_im_message WHERE backend_message_id = ?'
      ).get(data.backendMessageId)
      if (existing) return Number(existing.message_id)
    }
    throw e
  }
}

/**
 * 批量插入/更新（事务，OR REPLACE）
 * 用 backend_message_id 作为冲突键（UNIQUE 索引保证）
 */
export const batchPut = (list) => {
  const db = getDb()
  const stmt = db.prepare(`
    INSERT OR REPLACE INTO chat_im_message (
      backend_message_id, session_id, send_user_id, send_user_name, send_user_avatar,
      receive_user_id, message_type, message_content,
      file_name, file_size, file_id, file_path, content_type, file_type,
      message_send_type, message_only_id, status, send_time, recalled, recall_time
    ) VALUES (
      $backendMessageId, $sessionId, $sendUserId, $sendUserName, $sendUserAvatar,
      $receiveUserId, $messageType, $messageContent,
      $fileName, $fileSize, $fileId, $filePath, $contentType, $fileType,
      $messageSendType, $messageOnlyId, $status, $sendTime, $recalled, $recallTime
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
 * 根据本地主键 message_id 更新一条记录
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
  db.prepare(`UPDATE chat_im_message SET ${sets.join(', ')} WHERE message_id = $messageId`).run(params)
}

/**
 * 根据后端 messageId（backend_message_id）更新一条记录
 */
export const updateByBackendMessageId = (backendMessageId, changes) => {
  const db = getDb()
  const sets = []
  const params = {}
  for (const [key, value] of Object.entries(changes)) {
    const col = key.replace(/[A-Z]/g, (m) => '_' + m.toLowerCase())
    sets.push(`${col} = $${key}`)
    params[key] = value
  }
  if (sets.length === 0) return
  params.backendMessageId = backendMessageId
  db.prepare(`UPDATE chat_im_message SET ${sets.join(', ')} WHERE backend_message_id = $backendMessageId`).run(params)
}

/**
 * 更新消息状态（按本地主键）
 */
export const updateStatus = (messageId, status) => {
  const db = getDb()
  db.prepare('UPDATE chat_im_message SET status = ? WHERE message_id = ?').run(status, messageId)
}

/**
 * 根据 messageOnlyId 查询消息
 */
export const getByMessageOnlyId = (messageOnlyId) => {
  const db = getDb()
  const row = db.prepare('SELECT * FROM chat_im_message WHERE message_only_id = ?').get(messageOnlyId)
  return rowToEntity(row)
}

/**
 * 根据 messageOnlyId 更新消息
 */
export const updateByMessageOnlyId = (messageOnlyId, changes) => {
  const db = getDb()
  const sets = []
  const params = {}
  for (const [key, value] of Object.entries(changes)) {
    const col = key.replace(/[A-Z]/g, (m) => '_' + m.toLowerCase())
    sets.push(`${col} = $${key}`)
    params[key] = value
  }
  if (sets.length === 0) return
  params.messageOnlyId = messageOnlyId
  db.prepare(`UPDATE chat_im_message SET ${sets.join(', ')} WHERE message_only_id = $messageOnlyId`).run(params)
}

/**
 * 根据会话ID删除所有消息
 */
export const deleteBySessionId = (sessionId) => {
  const db = getDb()
  db.prepare('DELETE FROM chat_im_message WHERE session_id = ?').run(sessionId)
}

/**
 * 根据本地主键 message_id 删除单条消息（仅本地删除，不影响后端）
 * @param {number} messageId 本地 SQLite 自增主键
 */
export const deleteByMessageId = (messageId) => {
  const db = getDb()
  db.prepare('DELETE FROM chat_im_message WHERE message_id = ?').run(messageId)
}

/**
 * 按后端 messageId（backend_message_id）标记消息为已撤回
 * 撤回后保留记录，UI 渲染为"xxx撤回了一条消息"提示条
 * @param {number} backendMessageId 后端全局 messageId
 * @param {Object} extra 额外字段（如 messageContent 替换为撤回提示）
 */
export const markRecalledByBackendMessageId = (backendMessageId, extra = {}) => {
  const db = getDb()
  const sets = ['recalled = 1', 'recall_time = $recallTime']
  const params = { recallTime: Date.now(), backendMessageId }
  if (extra.messageContent !== undefined) {
    sets.push('message_content = $messageContent')
    params.messageContent = extra.messageContent
  }
  db.prepare(
    `UPDATE chat_im_message SET ${sets.join(', ')} WHERE backend_message_id = $backendMessageId`
  ).run(params)
}

/**
 * 按本地主键 message_id 标记消息为已撤回（用于自己发起的撤回，本地立即生效）
 */
export const markRecalledByMessageId = (messageId, extra = {}) => {
  const db = getDb()
  const sets = ['recalled = 1', 'recall_time = $recallTime']
  const params = { recallTime: Date.now(), messageId }
  if (extra.messageContent !== undefined) {
    sets.push('message_content = $messageContent')
    params.messageContent = extra.messageContent
  }
  db.prepare(
    `UPDATE chat_im_message SET ${sets.join(', ')} WHERE message_id = $messageId`
  ).run(params)
}

/**
 * 按 messageOnlyId 标记消息为已撤回
 * messageOnlyId 是前端生成的 UUID 字符串，不受雪花 messageId 精度丢失影响，
 * 作为撤回的本地关联键最可靠
 * @param {string} messageOnlyId 前端唯一消息标识
 * @param {Object} extra 额外字段（如 messageContent 替换为撤回提示）
 */
export const markRecalledByMessageOnlyId = (messageOnlyId, extra = {}) => {
  const db = getDb()
  const sets = ['recalled = 1', 'recall_time = $recallTime']
  const params = { recallTime: Date.now(), messageOnlyId }
  if (extra.messageContent !== undefined) {
    sets.push('message_content = $messageContent')
    params.messageContent = extra.messageContent
  }
  db.prepare(
    `UPDATE chat_im_message SET ${sets.join(', ')} WHERE message_only_id = $messageOnlyId`
  ).run(params)
}

/**
 * 获取本地数据库中最大的 backend_message_id（用于增量拉取离线消息的游标）
 * 说明：用 backend_message_id（后端全局递增ID），而不是本地主键 message_id
 * 注意：此函数统计所有消息（包括自己发送的），可能导致游标偏高
 * @returns {number} 最大的 backend_message_id，无消息时返回 0
 */
export const getMaxBackendMessageId = () => {
  const db = getDb()
  const row = db.prepare('SELECT MAX(backend_message_id) as maxId FROM chat_im_message').get()
  return row?.maxId || 0
}

/**
 * 按接收方获取最大 backend_message_id
 * 仅统计 receive_user_id = ? 的消息，避免自己发送的消息污染游标
 * 这才是拉取离线消息时应使用的正确游标
 * @param {string} receiveUserId 当前用户ID（作为接收方）
 * @returns {number} 最大的 backend_message_id，无消息时返回 0
 */
export const getMaxBackendMessageIdByReceiver = (receiveUserId) => {
  const db = getDb()
  const row = db.prepare(
    'SELECT MAX(backend_message_id) as maxId FROM chat_im_message WHERE receive_user_id = ?'
  ).get(receiveUserId)
  return row?.maxId || 0
}

/**
 * 获取群聊离线消息的增量拉取游标
 * 群消息单条存储（receive_user_id = groupId，G开头），排除自己发送的消息，
 * 避免自己发的消息抬高游标导致漏拉其他成员的群消息
 * @param {string} currentUserId 当前用户ID
 * @returns {number} 最大的 backend_message_id，无消息时返回 0
 */
export const getMaxGroupBackendMessageId = (currentUserId) => {
  const db = getDb()
  const row = db.prepare(
    "SELECT MAX(backend_message_id) as maxId FROM chat_im_message WHERE receive_user_id LIKE 'G%' AND send_user_id != ?",
  ).get(currentUserId)
  return row?.maxId || 0
}
