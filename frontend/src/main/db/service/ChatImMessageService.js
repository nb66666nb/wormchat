/**
 * ChatImMessage 业务逻辑层 — 对齐后端 Service 层
 *
 * 字段语义（重要）：
 *   messageId         - 本地 SQLite 自增主键，与后端无关，仅用于本地查询/排序
 *   backendMessageId  - 后端全局自增 messageId，用于 ACK、增量同步游标、与后端通信
 *   messageOnlyId     - 前端生成的 UUID，用于幂等去重
 *
 * 消息发送/接收流程中，调用方传入的 `messageId` 字段一律视为后端的 messageId，
 * 由本层映射为 backendMessageId 存储，避免污染本地主键。
 */
import http from 'http'
import * as mapper from '../mapper/ChatImMessageMapper'
import * as sessionService from './ChatSessionService'
import { getData, getUserData, getUserId } from '../../store'

/**
 * 分页加载会话消息（游标分页，按时间倒序）
 */
export const getMessageList = (sessionId, pageSize = 20, beforeTime = null) => {
  return mapper.listBySessionId(sessionId, pageSize, beforeTime)
}

/**
 * 获取会话最新消息
 */
export const getLatestMessage = (sessionId) => {
  const list = mapper.listLatestBySessionId(sessionId, 1)
  return list.length > 0 ? list[0] : null
}

/**
 * 发送消息（本地保存 + 更新会话）
 * 若 messageOnlyId 已存在（WS回声先到达），则更新而非重复插入
 *
 * @param {Object} data 消息对象
 *   - messageOnlyId: 前端生成的UUID（必传，用于幂等）
 *   - backendMessageId: 后端返回的 messageId（可选，WS回声/HTTP响应到达时填充）
 *   - 其它业务字段
 * @returns {Object} 入库后的消息实体
 */
export const sendMessage = (data, updateSession = true) => {
  // 防止重复：WS回声可能先于 dbImMessageSend 到达并已通过 receiveMessage 存库
  if (data.messageOnlyId) {
    const existing = mapper.getByMessageOnlyId(data.messageOnlyId)
    if (existing) {
      const changes = {}
      if (data.status !== undefined) changes.status = data.status
      // 关键：后端返回的 messageId 写入 backendMessageId，不写本地主键
      if (!existing.backendMessageId && data.backendMessageId) {
        changes.backendMessageId = data.backendMessageId
      }
      if (Object.keys(changes).length > 0) {
        mapper.updateByMessageOnlyId(data.messageOnlyId, changes)
      }
      return mapper.getByMessageOnlyId(data.messageOnlyId)
    }
  }

  // 入库前剥离 messageId（本地主键由 SQLite 自增分配）
  const { messageId, ...payload } = data
  const id = mapper.insert(payload)

  if (updateSession && data.sessionId) {
    // chat_session 表的 user_id 是「会话归属者」= 当前登录用户
    sessionService.updateLastMessage(data.sessionId, getUserId(), data.messageContent, data.sendTime)
  }

  return mapper.getById(id)
}

/**
 * 接收新消息：本地入库 + 激活会话（清除软删除 + 更新最后消息）
 * 使用 markActiveBySessionId 而非 updateLastMessage，使被软删的会话在收到新消息时重新出现在列表中
 *
 * 去重优先级：
 *   1. backendMessageId（后端推送/离线拉取的消息都有）
 *   2. messageOnlyId（WS回声可能没带 backendMessageId 的情况）
 *
 * 未读数递增：仅在「真正新插入」且 incrUnread=true 时执行，避免重复消息导致未读数虚高。
 * 调用方约定：
 *   - 实时收到他人消息 / 离线拉取：传 incrUnread=true
 *   - 自己发送消息的回声（messageOnlyId 竞态存库）：保持默认 false
 *
 * @param {Object} data 消息对象
 *   - backendMessageId: 后端 messageId
 *   - messageOnlyId: 前端UUID（可选）
 *   - 其它业务字段
 * @param {boolean} updateSession 是否更新会话最后消息
 * @param {boolean} incrUnread 是否对新插入的消息递增未读数（仅在消息真正入库时生效）
 */
export const receiveMessage = (data, updateSession = true, incrUnread = false) => {
  // 1. 优先按 backendMessageId 去重
  if (data.backendMessageId) {
    const existing = mapper.getByBackendMessageId(data.backendMessageId)
    if (existing) {
      mapper.updateByBackendMessageId(data.backendMessageId, data)
      return existing
    }
  }

  // 2. 退化按 messageOnlyId 去重（WS回声等无 backendMessageId 的情况）
  if (data.messageOnlyId) {
    const existing = mapper.getByMessageOnlyId(data.messageOnlyId)
    if (existing) {
      const changes = { status: 1 }
      if (!existing.backendMessageId && data.backendMessageId) {
        changes.backendMessageId = data.backendMessageId
      }
      mapper.updateByMessageOnlyId(data.messageOnlyId, changes)
      return mapper.getByMessageOnlyId(data.messageOnlyId)
    }
  }

  // 3. 新消息：剥离本地主键，让 SQLite 自增
  const { messageId, ...payload } = { ...data, status: 1 }
  const id = mapper.insert(payload)

  if (updateSession && data.sessionId) {
    // chat_session 表的 user_id 是「会话归属者」= 当前登录用户，
    // 不能用 receiveUserId（群聊时为 groupId、单聊时为对方ID）定位，否则匹配不到记录。
    sessionService.markActiveBySessionId(data.sessionId, getUserId(), {
      lastMessage: data.messageContent,
      lastMessageTime: data.sendTime
    })
  }

  // 仅对真正新入库的消息递增未读数（渲染层打开会话时清零）
  if (incrUnread && data.sessionId) {
    sessionService.incrementUnread(data.sessionId, getUserId())
  }

  return mapper.getById(id)
}

/**
 * 批量同步消息（从后端拉取）
 * 入参的 messageId 字段一律视为后端 messageId，映射到 backendMessageId 存储
 */
export const syncMessages = (messages) => {
  const list = messages.map((m) => {
    const { messageId, ...rest } = m
    return {
      ...rest,
      backendMessageId: messageId || 0,
      status: 1
    }
  })
  mapper.batchPut(list)
}

/**
 * 更新消息状态（按本地主键 message_id）
 */
export const updateMessageStatus = (messageId, status) => {
  mapper.updateStatus(messageId, status)
}

/**
 * 根据 messageOnlyId 确认消息送达：
 *   - 更新 status=1
 *   - 把后端返回的 messageId 写入 backendMessageId（而非本地主键）
 *   - 补充后端返回的 fileId / filePath 等字段
 *
 * @param {string} messageOnlyId 前端生成的UUID
 * @param {Object} backendData 后端回声数据，messageId 字段是后端的全局ID
 * @returns {Object|null} 更新后的实体，未找到时返回 false
 */
export const confirmByMessageOnlyId = (messageOnlyId, backendData = {}) => {
  if (!messageOnlyId) return false
  const existing = mapper.getByMessageOnlyId(messageOnlyId)
  if (!existing) return false

  const changes = { status: 1 }
  // 关键：后端的 messageId 写入 backendMessageId 字段
  if (backendData.messageId) changes.backendMessageId = backendData.messageId
  if (backendData.sendTime) changes.sendTime = backendData.sendTime
  if (backendData.fileId) changes.fileId = backendData.fileId
  if (backendData.filePath) changes.filePath = backendData.filePath

  mapper.updateByMessageOnlyId(messageOnlyId, changes)
  return mapper.getByMessageOnlyId(messageOnlyId)
}

/**
 * 按类型查询会话中的消息
 */
export const getMessagesByType = (sessionId, messageType) => {
  return mapper.listBySessionIdAndType(sessionId, messageType)
}

/**
 * 统计会话消息数
 */
export const countMessages = (sessionId) => {
  return mapper.countBySessionId(sessionId)
}

/**
 * 删除会话的所有消息
 */
export const deleteMessagesBySessionId = (sessionId) => {
  mapper.deleteBySessionId(sessionId)
}

/**
 * 删除单条消息（仅本地删除，不影响后端和其他端）
 * 用于右键菜单"删除消息"功能
 * @param {number} messageId 本地 SQLite 自增主键
 */
export const deleteMessage = (messageId) => {
  mapper.deleteByMessageId(messageId)
}

/**
 * 标记消息为已撤回（按后端 messageId）
 * 收到后端撤回通知时调用，保留记录并标记 recalled=1
 * @param {number} backendMessageId 后端全局 messageId
 * @param {Object} extra 额外字段（messageContent 替换为撤回提示文案）
 */
export const markRecalledByBackendMessageId = (backendMessageId, extra = {}) => {
  mapper.markRecalledByBackendMessageId(backendMessageId, extra)
}

/**
 * 标记消息为已撤回（按本地主键，自己发起撤回时本地立即生效）
 * @param {number} messageId 本地 SQLite 自增主键
 * @param {Object} extra 额外字段
 */
export const markRecalledByMessageId = (messageId, extra = {}) => {
  mapper.markRecalledByMessageId(messageId, extra)
}

/**
 * 按 messageOnlyId 标记消息为已撤回
 * messageOnlyId 是前端 UUID 字符串，不受雪花 messageId 精度丢失影响，
 * 用于自己发起撤回时本地立即生效（比按本地主键更可靠，因为新消息 UI 的 messageId
 * 可能被发送流程覆盖成后端 ID）
 * @param {string} messageOnlyId 前端唯一消息标识
 * @param {Object} extra 额外字段
 */
export const markRecalledByMessageOnlyId = (messageOnlyId, extra = {}) => {
  mapper.markRecalledByMessageOnlyId(messageOnlyId, extra)
}

/**
 * 获取本地最大 backend_message_id（用于增量拉取游标）
 * @deprecated 可能被自己发送的消息污染，建议使用 getMaxBackendMessageIdByReceiver
 */
export const getMaxBackendMessageId = () => {
  return mapper.getMaxBackendMessageId()
}

/**
 * 按接收方获取本地最大 backend_message_id（正确的增量拉取游标）
 * 仅统计当前用户作为接收方的消息
 */
export const getMaxBackendMessageIdByReceiver = (receiveUserId) => {
  return mapper.getMaxBackendMessageIdByReceiver(receiveUserId)
}

/**
 * 从后端拉取离线消息（增量同步）
 * 使用主进程 http 模块直接请求，不依赖渲染进程
 *
 * 增量游标：本地最大的 backend_message_id
 * 后端语义：返回 receive_user_id = 当前用户 AND message_id > 游标 的消息
 *
 * @returns {Promise<Array>} 拉取到的消息列表（已入库）
 */
export const pullOfflineMessages = async () => {
  console.log('[DB] pullOfflineMessages 开始执行')
  const currentUserId = getUserId()
  console.log('[DB] 当前用户ID:', currentUserId)

  let lastMessageId = 0
  try {
    // 关键：只统计当前用户作为接收方的消息，避免自己发送的消息污染游标
    lastMessageId = currentUserId
      ? (mapper.getMaxBackendMessageIdByReceiver(currentUserId) || 0)
      : 0
  } catch (e) {
    console.error('[DB] getMaxBackendMessageIdByReceiver 失败:', e.message)
  }
  console.log('[DB] 增量游标 lastMessageId =', lastMessageId, '(仅统计 receive_user_id=' + currentUserId + ')')

  const messages = await httpGet('/api/chatImMessage/pullOffline?lastMessageId=' + lastMessageId)
  if (messages && messages.length > 0) {
    for (const msg of messages) {
      // 关键：后端返回的 msg.messageId 是后端全局ID，存入 backendMessageId
      // 不传本地主键 messageId，让 SQLite 自增
      // incrUnread=true：离线拉取的都是他人发来的消息，新入库时递增未读数
      receiveMessage({
        backendMessageId: msg.messageId,
        sessionId: msg.sessionId || '',
        sendUserId: msg.sendUserId || '',
        sendUserName: msg.sendUserName || '',
        sendUserAvatar: msg.sendUserAvatar || '',
        receiveUserId: msg.receiveUserId || '',
        messageType: msg.messageType,
        messageContent: msg.messageContent,
        fileName: msg.fileName || '',
        fileSize: msg.fileSize || 0,
        fileId: msg.fileId || '',
        filePath: msg.filePath || '',
        contentType: msg.contentType || '',
        fileType: msg.fileType ?? 0,
        messageSendType: msg.messageSendType ?? 0,
        messageOnlyId: msg.messageOnlyId || '',
        status: 1,
        sendTime: msg.sendTime || Date.now(),
        recalled: msg.recalled ?? 0,
        recallTime: msg.recallTime ?? 0
      }, true, true)
    }
    console.log(`[DB] 离线消息拉取完成: ${messages.length} 条`)
  } else {
    console.log('[DB] 后端返回空数据，无离线消息')
  }
  return messages || []
}

/**
 * 从后端拉取群聊离线消息（增量同步）
 * 群消息单条存储（receive_user_id = groupId），后端按用户所在的所有群返回增量消息
 *
 * 增量游标：本地群消息（receive_user_id 为 G 开头）中排除自己发送后的最大 backend_message_id
 * 重复下发（如自己发送的、实时已收到的）由 receiveMessage 的 backendMessageId 去重兜底
 *
 * @returns {Promise<Array>} 拉取到的群消息列表（已入库）
 */
export const pullOfflineGroupMessages = async () => {
  console.log('[DB] pullOfflineGroupMessages 开始执行')
  const currentUserId = getUserId()

  let lastMessageId = 0
  try {
    lastMessageId = currentUserId
      ? (mapper.getMaxGroupBackendMessageId(currentUserId) || 0)
      : 0
  } catch (e) {
    console.error('[DB] getMaxGroupBackendMessageId 失败:', e.message)
  }
  console.log('[DB] 群消息增量游标 lastMessageId =', lastMessageId)

  const messages = await httpGet('/api/chatImMessage/pullOfflineGroupMessages?lastMessageId=' + lastMessageId)
  if (messages && messages.length > 0) {
    for (const msg of messages) {
      receiveMessage({
        backendMessageId: msg.messageId,
        sessionId: msg.sessionId || '',
        sendUserId: msg.sendUserId || '',
        sendUserName: msg.sendUserName || '',
        sendUserAvatar: msg.sendUserAvatar || '',
        receiveUserId: msg.receiveUserId || '',
        messageType: msg.messageType,
        messageContent: msg.messageContent,
        fileName: msg.fileName || '',
        fileSize: msg.fileSize || 0,
        fileId: msg.fileId || '',
        filePath: msg.filePath || '',
        contentType: msg.contentType || '',
        fileType: msg.fileType ?? 0,
        messageSendType: msg.messageSendType ?? 0,
        messageOnlyId: msg.messageOnlyId || '',
        status: 1,
        sendTime: msg.sendTime || Date.now(),
        recalled: msg.recalled ?? 0,
        recallTime: msg.recallTime ?? 0
      }, true, true)
    }
    console.log(`[DB] 群离线消息拉取完成: ${messages.length} 条`)
  } else {
    console.log('[DB] 后端返回空数据，无群离线消息')
  }
  return messages || []
}

/**
 * 主进程 HTTP GET 公共方法（带 token）
 * @param {string} path 请求路径（以 / 开头）
 * @returns {Promise<Array|Object|null>} 响应 data，失败返回 []
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
      headers: {
        'token': token
      }
    }
    console.log('[DB] HTTP请求:', `http://${options.hostname}:${options.port}${options.path}`)

    const req = http.request(options, (res) => {
      let data = ''
      res.on('data', chunk => data += chunk)
      res.on('end', () => {
        console.log('[DB] HTTP响应: statusCode=' + res.statusCode + ', body=' + data.substring(0, 300))
        try {
          const result = JSON.parse(data)
          if (result.code === 200) {
            resolve(result.data || [])
          } else {
            console.warn('[DB] 拉取响应异常: code=' + result.code + ', info=' + (result.info || '无'))
            resolve([])
          }
        } catch (e) {
          console.error('[DB] HTTP响应解析失败:', e.message)
          resolve([])
        }
      })
    })

    req.on('error', (e) => {
      console.error('[DB] HTTP请求失败:', e.message)
      resolve([])
    })
    req.on('timeout', () => {
      req.destroy()
      console.error('[DB] HTTP请求超时')
      resolve([])
    })
    req.end()
  })
}
