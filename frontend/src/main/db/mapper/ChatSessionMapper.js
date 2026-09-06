/**
 * ChatSession 数据访问层 — 对齐后端 Mapper 层
 * 使用 better-sqlite3 同步API
 */
import { getDb } from '../Database'
import { rowToEntity, entityToParams } from '../entity/ChatSession'

/**
 * 根据 sessionId + userId 查询（多用户存储场景：同一会话对不同用户分别存储）
 */
export const getById = (sessionId, userId) => {
  const db = getDb()
  const row = db.prepare('SELECT * FROM chat_session WHERE session_id = ? AND user_id = ?').get(sessionId, userId)
  return rowToEntity(row)
}

/**
 * 根据用户ID查询所有会话（未删除，置顶在前，其余按时间倒序）
 */
export const listByUserId = (userId) => {
  const db = getDb()
  const rows = db.prepare(
    'SELECT * FROM chat_session WHERE user_id = ? AND deleted = 0 ORDER BY is_top DESC, last_message_time DESC'
  ).all(userId)
  return rows.map(rowToEntity)
}

/**
 * 根据用户ID和会话类型查询
 */
export const listByUserIdAndType = (userId, sessionType) => {
  const db = getDb()
  const rows = db.prepare(
    'SELECT * FROM chat_session WHERE user_id = ? AND session_type = ? AND deleted = 0 ORDER BY last_message_time DESC'
  ).all(userId, sessionType)
  return rows.map(rowToEntity)
}

/**
 * 根据用户ID查询置顶会话
 */
export const listTopByUserId = (userId) => {
  const db = getDb()
  const rows = db.prepare(
    'SELECT * FROM chat_session WHERE user_id = ? AND is_top = 1 AND deleted = 0 ORDER BY last_message_time DESC'
  ).all(userId)
  return rows.map(rowToEntity)
}

/**
 * 根据对方用户ID查询会话
 */
export const getByTargetUserId = (userId, targetUserId) => {
  const db = getDb()
  const row = db.prepare(
    'SELECT * FROM chat_session WHERE user_id = ? AND target_user_id = ? AND deleted = 0'
  ).get(userId, targetUserId)
  return rowToEntity(row)
}

/**
 * 根据对方用户ID查询会话（含已软删除的记录，用于"聊天"按钮恢复被隐藏的会话）
 */
export const getByTargetUserIdIncludeDeleted = (userId, targetUserId) => {
  const db = getDb()
  const row = db.prepare(
    'SELECT * FROM chat_session WHERE user_id = ? AND target_user_id = ?'
  ).get(userId, targetUserId)
  return rowToEntity(row)
}

/**
 * 插入一条记录（有 UNIQUE 约束冲突时静默忽略，由调用方先查后插保证幂等）
 */
export const insert = (data) => {
  const db = getDb()
  const p = entityToParams(data)
  db.prepare(`
    INSERT OR IGNORE INTO chat_session (
      session_id, user_id, target_user_id, target_nick_name, session_type,
      bot_category, template_id, bot_personality, bot_avatar_path, bot_name,
      bot_description, bot_system_prompt, bot_welcome_msg,
      last_message, last_message_time, is_top, unread_count, deleted
    ) VALUES (
      $sessionId, $userId, $targetUserId, $targetNickName, $sessionType,
      $botCategory, $templateId, $botPersonality, $botAvatarPath, $botName,
      $botDescription, $botSystemPrompt, $botWelcomeMsg,
      $lastMessage, $lastMessageTime, $isTop, $unreadCount, $deleted
    )
  `).run(p)
}

/**
 * 批量插入（事务）
 */
export const batchInsert = (list) => {
  const db = getDb()
  const stmt = db.prepare(`
    INSERT OR REPLACE INTO chat_session (
      session_id, user_id, target_user_id, target_nick_name, session_type,
      bot_category, template_id, bot_personality, bot_avatar_path, bot_name,
      bot_description, bot_system_prompt, bot_welcome_msg,
      last_message, last_message_time, is_top, unread_count, deleted
    ) VALUES (
      $sessionId, $userId, $targetUserId, $targetNickName, $sessionType,
      $botCategory, $templateId, $botPersonality, $botAvatarPath, $botName,
      $botDescription, $botSystemPrompt, $botWelcomeMsg,
      $lastMessage, $lastMessageTime, $isTop, $unreadCount, $deleted
    )
  `)
  const insertMany = db.transaction((items) => {
    for (const item of items) {
      stmt.run(entityToParams(item))
    }
  })
  insertMany(list)
}

/**
 * 更新一条记录（仅限本用户记录）
 */
export const updateById = (sessionId, userId, changes) => {
  const db = getDb()
  const sets = []
  const params = {}
  for (const [key, value] of Object.entries(changes)) {
    const col = key.replace(/[A-Z]/g, (m) => '_' + m.toLowerCase())
    sets.push(`${col} = $${key}`)
    params[key] = value
  }
  if (sets.length === 0) return
  params.sessionId = sessionId
  params.userId = userId
  db.prepare(`UPDATE chat_session SET ${sets.join(', ')} WHERE session_id = $sessionId AND user_id = $userId`).run(params)
}

/**
 * 软删除（仅限本用户记录）
 */
export const deleteById = (sessionId, userId) => {
  const db = getDb()
  db.prepare('UPDATE chat_session SET deleted = 1 WHERE session_id = ? AND user_id = ?').run(sessionId, userId)
}

/**
 * 置顶/取消置顶（仅限本用户记录）
 */
export const updateTop = (sessionId, userId, isTop) => {
  const db = getDb()
  db.prepare('UPDATE chat_session SET is_top = ? WHERE session_id = ? AND user_id = ?').run(isTop, sessionId, userId)
}

/**
 * 更新最后消息（仅限本用户记录）
 */
export const updateLastMessage = (sessionId, userId, lastMessage, lastMessageTime) => {
  const db = getDb()
  db.prepare('UPDATE chat_session SET last_message = ?, last_message_time = ? WHERE session_id = ? AND user_id = ?')
    .run(lastMessage, lastMessageTime, sessionId, userId)
}

/**
 * 统计用户未删除的会话数
 */
export const countByUserId = (userId) => {
  const db = getDb()
  const row = db.prepare('SELECT COUNT(*) as cnt FROM chat_session WHERE user_id = ? AND deleted = 0').get(userId)
  return row.cnt
}

/**
 * 未读数 +1（仅限本用户记录）
 */
export const incrementUnread = (sessionId, userId) => {
  const db = getDb()
  db.prepare('UPDATE chat_session SET unread_count = unread_count + 1 WHERE session_id = ? AND user_id = ?').run(sessionId, userId)
}

/**
 * 未读数清零（仅限本用户记录）
 */
export const resetUnread = (sessionId, userId) => {
  const db = getDb()
  db.prepare('UPDATE chat_session SET unread_count = 0 WHERE session_id = ? AND user_id = ?').run(sessionId, userId)
}
