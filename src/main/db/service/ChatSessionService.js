/**
 * ChatSession 业务逻辑层 — 对齐后端 Service 层
 */
import http from 'http'
import * as mapper from '../mapper/ChatSessionMapper'
import { getData, getUserData, getUserId } from '../../store'

/**
 * 依据会话对方ID前缀校正会话类型，防止后端脏数据把用户单聊误标成群聊。
 *
 * 类型推断规则（与后端 createSession 保持一致）：
 *   - target_user_id 以 "G" 开头 → 群聊（target 为 groupId）
 *   - target_user_id 以 "R" 开头 → 机器人（target 为 botId）
 *   - 其他（用户ID为纯数字）     → 单聊
 *
 * 兜底：当 target_user_id 为空时，再依据 sessionId 前缀判断
 *   （单聊 S、群聊 SG/G、机器人 SB），仍无法判断则保留原值。
 *
 * @param {number|undefined} sessionType 后端下发的会话类型
 * @param {string} targetUserId 对方用户ID / 群ID / 机器人ID
 * @param {string} sessionId 会话ID（target 为空时的辅助判断）
 * @returns {number} 校正后的会话类型
 */
export const normalizeSessionType = (sessionType, targetUserId = '', sessionId = '') => {
  const t = targetUserId || ''
  if (/^G/.test(t)) return 1 // 群聊
  if (/^R/.test(t)) return 2 // 机器人
  if (t && !/^G/.test(t) && !/^R/.test(t)) return 0 // 用户ID（纯数字）→ 单聊
  // targetUserId 缺失，退化为按 sessionId 前缀判断
  const sid = sessionId || ''
  if (/^SG|^G/.test(sid)) return 1
  if (/^SB/.test(sid)) return 2
  if (/^S/.test(sid)) return 0
  // 无法判断，保留原值（避免误改）
  return typeof sessionType === 'number' ? sessionType : 0
}

/**
 * 获取会话列表（置顶在前，其余按时间倒序）
 */
export const getSessionList = (userId) => {
  return mapper.listByUserId(userId)
}

/**
 * 新消息到达时刷新会话的最近时间/最近消息，并清除软删除标记使其在列表中显现
 * @param {string} sessionId
 * @param {string} userId 当前操作用户ID
 * @param {Object} update 至少包含 lastMessage、lastMessageTime
 */
export const markActiveBySessionId = (sessionId, userId, update = {}) => {
  if (!sessionId) return false
  const existing = mapper.getById(sessionId, userId)
  if (!existing) return false
  const changes = {
    lastMessage: update.lastMessage ?? existing.lastMessage ?? '',
    lastMessageTime: update.lastMessageTime ?? Date.now(),
    deleted: 0
  }
  mapper.updateById(sessionId, userId, changes)
  return mapper.getById(sessionId, userId)
}

/**
 * 获取某个会话
 */
export const getSession = (sessionId, userId) => {
  return mapper.getById(sessionId, userId)
}

/**
 * 根据对方用户ID查找会话
 */
export const getSessionByTargetUserId = (userId, targetUserId) => {
  return mapper.getByTargetUserId(userId, targetUserId)
}

/**
 * 根据会话类型查询
 */
export const getSessionListByType = (userId, sessionType) => {
  return mapper.listByUserIdAndType(userId, sessionType)
}

/**
 * 创建或更新会话（如果已存在则更新）
 */
export const saveSession = (data) => {
  const existing = mapper.getById(data.sessionId, data.userId)
  if (existing) {
    mapper.updateById(data.sessionId, data.userId, data)
    return existing
  }
  mapper.insert(data)
  return mapper.getById(data.sessionId, data.userId)
}

/**
 * 纯插入（调用方已确认本地无此 sessionId）
 */
export const insertSession = (data) => {
  mapper.insert(data)
  return mapper.getById(data.sessionId, data.userId)
}

/**
 * 按 sessionId 更新指定字段；自动从 changes 中去掉主键 sessionId，避免误改
 */
export const serviceUpdateById = (sessionId, userId, changes) => {
  const { sessionId: _sid, ...rest } = changes || {}
  if (Object.keys(rest).length === 0) return
  mapper.updateById(sessionId, userId, rest)
  return mapper.getById(sessionId, userId)
}

/**
 * 批量保存会话（同步时用）
 */
export const batchSaveSessions = (sessions) => {
  mapper.batchInsert(sessions)
}

/**
 * 更新会话最后消息
 */
export const updateLastMessage = (sessionId, userId, lastMessage, lastMessageTime) => {
  mapper.updateLastMessage(sessionId, userId, lastMessage, lastMessageTime)
}

/**
 * 置顶/取消置顶会话
 */
export const toggleTop = (sessionId, userId, isTop) => {
  mapper.updateTop(sessionId, userId, isTop ? 1 : 0)
}

/**
 * 未读数 +1
 */
export const incrementUnread = (sessionId, userId) => {
  mapper.incrementUnread(sessionId, userId)
}

/**
 * 未读数清零（打开会话时调用）
 */
export const resetUnread = (sessionId, userId) => {
  mapper.resetUnread(sessionId, userId)
}

/**
 * 软删除会话
 */
export const deleteSession = (sessionId, userId) => {
  mapper.deleteById(sessionId, userId)
}

/**
 * 按对方ID恢复被软删除的会话（好友/群聊点击"聊天"时调用）
 * @param {string} userId 当前用户ID
 * @param {string} targetUserId 对方用户ID / 群ID
 * @returns 恢复后的会话，若不存在返回 null
 */
export const restoreSessionByTargetUserId = (userId, targetUserId) => {
  if (!targetUserId) return null
  const existing = mapper.getByTargetUserIdIncludeDeleted(userId, targetUserId)
  if (!existing) {
    console.log(`[DB] 恢复会话: 未找到本地会话 (userId=${userId}, targetUserId=${targetUserId})`)
    return null
  }
  if (existing.deleted) {
    mapper.updateById(existing.sessionId, userId, { deleted: 0 })
    console.log(`[DB] 恢复会话: sessionId=${existing.sessionId} 已取消软删除`)
  }
  return mapper.getById(existing.sessionId, userId)
}

/**
 * 会话列表同步（用户上线时调用，解决离线期间的会话变更）
 * 1. 拉取后端该用户的全部会话
 * 2. 以后端为准 upsert 本地（保留本地 unreadCount/isTop/deleted 等本地字段）
 * 3. 本地存在、后端已不存在的群聊会话（被踢/退群/解散）→ 本地软删除
 *
 * @returns {Promise<{sessions: Array, removedSessionIds: Array}>} 同步结果
 */
export const syncSessions = async () => {
  const currentUserId = getUserId()
  const remoteSessions = await httpGet('/api/chatSession/syncSessions')
  if (!Array.isArray(remoteSessions)) {
    return { sessions: [], removedSessionIds: [] }
  }

  // 后端会话按 sessionId 建立索引
  const remoteMap = new Map()
  for (const s of remoteSessions) {
    if (s && s.sessionId) remoteMap.set(s.sessionId, s)
  }

  // upsert：已有则更新后端字段（保留本地 unreadCount/isTop/deleted），无则插入
  let updated = 0
  let inserted = 0
  for (const s of remoteSessions) {
    if (!s || !s.sessionId) continue
    const sessionData = {
      sessionId: s.sessionId,
      userId: s.userId || currentUserId || '',
      targetUserId: s.targetUserId || '',
      targetNickName: s.targetNickName || '',
      // 类型校正：以后端类型为基准，但若与 target_user_id 前缀不符（脏数据），按前缀修正
      sessionType: normalizeSessionType(s.sessionType, s.targetUserId, s.sessionId),
      botCategory: s.botCategory ?? null,
      templateId: s.templateId ?? null,
      botPersonality: s.botPersonality ?? null,
      botAvatarPath: s.botAvatarPath ?? null,
      botName: s.botName ?? null,
      botDescription: s.botDescription ?? null,
      botSystemPrompt: s.botSystemPrompt ?? null,
      botWelcomeMsg: s.botWelcomeMsg ?? null,
      lastMessage: s.lastMessage ?? '',
      lastMessageTime: s.lastMessageTime ?? 0
    }
    // 双重去重：先按 sessionId + userId 查，再按 (userId, targetUserId) 查
    // 防止后端脏数据（同一对用户返回多条）导致本地重复
    let existing = mapper.getById(s.sessionId, s.userId || currentUserId)
    if (!existing) {
      existing = mapper.getByTargetUserId(sessionData.userId, sessionData.targetUserId)
      if (existing && existing.sessionId !== s.sessionId) {
        // sessionId 不一致说明是脏数据，以本地为准更新
        mapper.updateById(existing.sessionId, existing.userId, sessionData)
        updated++
        continue
      }
    }
    if (existing) {
      // 保留本地字段：unreadCount（未读数）、isTop（置顶）、deleted（软删除）
      const { sessionId, ...changes } = sessionData
      mapper.updateById(existing.sessionId, existing.userId, changes)
      updated++
    } else {
      mapper.insert({ ...sessionData, unreadCount: 0, isTop: 0, deleted: 0 })
      inserted++
    }
  }

  // 类型自愈：本地 sessionType=1（群聊）但 target_user_id 不是群ID（群ID以 G 开头）的会话，
  // 属于"用户单聊被误标成群聊"的脏数据，统一修正为单聊（sessionType=0）。
  // 根因：后端 createSession 去重不区分会话类型，添加好友时复用了群聊记录且未修正类型。
  let fixedType = 0
  if (currentUserId) {
    const localGroupSessions = mapper.listByUserIdAndType(currentUserId, 1) || []
    for (const local of localGroupSessions) {
      if (local.deleted) continue // 已软删除的跳过
      const target = local.targetUserId || ''
      // 群聊 target_user_id 必为群ID（G 开头）；若不以 G 开头则不是群聊，是被误标的用户会话
      if (!/^G/.test(target)) {
        mapper.updateById(local.sessionId, currentUserId, { sessionType: 0 })
        fixedType++
        console.log(`[DB] 会话类型自愈: sessionId=${local.sessionId} 由群聊(1)修正为单聊(0), target=${target}`)
      }
    }
  }

  // 清理：本地群聊会话（sessionType=1）中后端已不存在的 → 软删除
  // 场景：离线期间被踢出群/退群/群解散（后端已删除对应会话）
  const removedSessionIds = []
  if (currentUserId) {
    const localGroupSessions = mapper.listByUserIdAndType(currentUserId, 1) || []
    for (const local of localGroupSessions) {
      if (local.deleted) continue // 已软删除的跳过
      if (!remoteMap.has(local.sessionId)) {
        mapper.deleteById(local.sessionId, currentUserId)
        removedSessionIds.push(local.sessionId)
      }
    }
  }

  console.log(`[DB] 会话同步完成: 后端${remoteSessions.length}条, 更新${updated}, 新增${inserted}, 类型修正${fixedType}, 清理${removedSessionIds.length}`)
  return { sessions: remoteSessions, removedSessionIds }
}

/**
 * 主进程 HTTP GET 公共方法（带 token）
 */
const httpGet = (path) => {
  const token = getUserData('token') || ''
  let serverConfig = { host: 'localhost', httpPort: '6060' }
  try {
    const raw = getData('serverConfig')
    if (raw) serverConfig = JSON.parse(raw)
  } catch (_) {}

  return new Promise((resolve) => {
    const options = {
      hostname: serverConfig.host,
      port: serverConfig.httpPort || 6060,
      path: path,
      method: 'GET',
      timeout: 10000,
      headers: { 'token': token }
    }
    const req = http.request(options, (res) => {
      let data = ''
      res.on('data', chunk => data += chunk)
      res.on('end', () => {
        try {
          const result = JSON.parse(data)
          if (result.code === 200) {
            resolve(result.data || [])
          } else {
            resolve([])
          }
        } catch (e) {
          console.error('[DB] 会话同步HTTP响应解析失败:', e.message)
          resolve([])
        }
      })
    })
    req.on('error', (e) => {
      console.error('[DB] 会话同步HTTP请求失败:', e.message)
      resolve([])
    })
    req.on('timeout', () => {
      req.destroy()
      console.error('[DB] 会话同步HTTP请求超时')
      resolve([])
    })
    req.end()
  })
}
