import WebSocket from 'ws'
import { BrowserWindow, app, Notification } from 'electron'
import path from 'path'
import fs from 'fs'
const NODE_ENV = process.env.NODE_ENV
import { getData, getUserData, getUserId } from "./store"
import { MessageType, MessageTypeText, FILE_MESSAGE_TYPES, MessageTypeIm, MessageTypeImText, GroupStatusEnum, GroupRoleEnum } from "./messageType"
import { processFileMessage } from './file.js'
import { initDatabase } from './db/Database'
import * as chatMessageService from './db/service/ChatMessageService'
import * as chatImMessageService from './db/service/ChatImMessageService'
import * as chatSessionService from './db/service/ChatSessionService'
import * as groupService from './db/service/GroupService'

const getWindow = (name) => {
    const windows = BrowserWindow.getAllWindows();
    return windows.find(w => w.getTitle() === name) || windows[0];
}

let ws = null
let maxReConnectTimes = null;
let lockReconnect = false;

let wsUrl = null;
let sender = null;
let needReconnect = null;
let heartBeatTimer = null;
let isManualClose = false;

const initWs = (config, _sender) => {
    const token = config.token || getUserData('token') || ''
    const host = config.host || 'localhost'
    const port = config.port || 6061
    const path = config.path || '/ws'
    wsUrl = `ws://${host}:${port}${path}?token=${token}`
    sender = _sender;
    needReconnect = true;
    maxReConnectTimes = 5;
    isManualClose = false;
    console.log('初始化WebSocket连接:', wsUrl)
    createWs();
}

const closeWs = () => {
    console.log('=== 开始主动关闭WebSocket ===')
    console.log('设置 needReconnect = false')
    needReconnect = false;
    console.log('设置 isManualClose = true')
    isManualClose = true;

    if (heartBeatTimer) {
        console.log('清除心跳定时器')
        clearInterval(heartBeatTimer);
        heartBeatTimer = null;
    }

    if (ws) {
        console.log('关闭WebSocket连接, 当前状态:', ws.readyState)
        try {
            ws.close(1000, '用户主动关闭');
            ws = null;
            console.log('=== WebSocket已关闭 ===')
        } catch (e) {
            console.error('关闭WebSocket时出错:', e.message)
        }
    } else {
        console.log('WebSocket实例为null，无需关闭')
    }
}

const reconnect = (type) => {
    console.log('--- reconnect 被调用 ---')
    console.log('type:', type)
    console.log('isManualClose:', isManualClose)
    console.log('needReconnect:', needReconnect)

    if (isManualClose) {
        console.log("✅ 检测到主动关闭，不进行重连")
        return;
    }

    if (!needReconnect) {
        console.log("⚠️ 无需重连 (needReconnect=false)")
        return;
    }

    if (ws != null) {
        ws.close()
    }

    if (lockReconnect) {
        console.log("⚠️ 重连锁定中，跳过")
        return;
    }

    console.log(type + "准备重连");
    lockReconnect = true;

    if (maxReConnectTimes > 0) {
        console.log('准备重连，剩余重连次数' + maxReConnectTimes, new Date().getTime())
        maxReConnectTimes--

        setTimeout(function () {
            createWs()
            lockReconnect = false;
        }, 5000)
    } else {
        console.log('TCP连接已超时')
    }
}

const sendMessageToRenderer = (message) => {
    const allWindows = BrowserWindow.getAllWindows()
    console.log(`📢 [主进程] 当前所有窗口数量: ${allWindows.length}`)

    const mti = message.messageTypeIm
    const isImMessage = mti === MessageTypeIm.IM
    const isSessionEvent = mti === MessageTypeIm.SESSION

    const targetWindows = []
    allWindows.forEach((w, i) => {
        if (w.isDestroyed()) return
        const url = w.webContents.getURL()
        const isMeetingWindow = url.includes('/meeting-room') || w.getTitle() === '通话进行中'
        const isMainWindow = !isMeetingWindow

        console.log(`📢 [主进程] 窗口 ${i + 1}: 标题="${w.getTitle()}", 是会议窗口=${isMeetingWindow}, 是主窗口=${isMainWindow}`)

        if (isImMessage && isMainWindow) {
            // IM消息 → 主窗口
            targetWindows.push(w)
        } else if (isSessionEvent && isMainWindow) {
            // 会话系统事件 → 主窗口
            targetWindows.push(w)
        } else if (!isImMessage && !isSessionEvent && isMeetingWindow) {
            // 会议消息 → 会议窗口
            targetWindows.push(w)
        }
    })

    const channelName = isImMessage ? 'IM' : isSessionEvent ? '会话事件' : '会议'
    console.log(`📢 [主进程] 消息通道=${channelName}, 目标窗口数量: ${targetWindows.length}`)

    const safeMessage = JSON.parse(JSON.stringify(message))

    targetWindows.forEach((window, index) => {
        if (window && !window.isDestroyed()) {
            window.webContents.send('new-message', safeMessage)
            console.log(`✅ [主进程] 窗口 ${index + 1} 消息发送成功`)
        }
    })

    if (targetWindows.length === 0) {
        const targetWindowName = isSessionEvent ? '主窗口(会话事件)' : isImMessage ? '主窗口' : '会议窗口'
        console.log(`⚠️ [主进程] 没有找到目标窗口（${targetWindowName}），消息未发送`)
    }
}

/**
 * 通过 WebSocket 发送消息到服务器（供渲染进程使用）
 * @param {Object} data - 要发送的消息对象，会被 JSON 序列化
 * @returns {boolean} 是否发送成功
 */
const sendMessage = (data) => {
    if (ws && ws.readyState === WebSocket.OPEN) {
        const jsonStr = typeof data === 'string' ? data : JSON.stringify(data)
        ws.send(jsonStr)
        return true
    }
    console.warn('❌ [主进程] WebSocket 未连接，无法发送消息')
    return false
}

/**
 * 解析 extendData 中的会话对象
 * @param {string} extendData - 后端 ChatSession 的 JSON 字符串
 * @returns {Object|null}
 */
const parseSessionData = (extendData) => {
    if (!extendData) return null
    try {
        const raw = typeof extendData === 'string' ? JSON.parse(extendData) : extendData
        if (!raw || !raw.sessionId) return null
        return {
            sessionId: raw.sessionId,
            userId: raw.userId,
            targetUserId: raw.targetUserId,
            targetNickName: raw.targetNickName,
            sessionType: raw.sessionType,
            botCategory: raw.botCategory,
            templateId: raw.templateId,
            botPersonality: raw.botPersonality,
            botAvatarPath: raw.botAvatarPath,
            botName: raw.botName,
            botDescription: raw.botDescription,
            botSystemPrompt: raw.botSystemPrompt,
            botWelcomeMsg: raw.botWelcomeMsg,
            lastMessage: raw.lastMessage,
            lastMessageTime: raw.lastMessageTime,
            isTop: raw.isTop ?? 0,
            deleted: raw.deleted ?? 0
        }
    } catch (e) {
        console.error('[DB] 解析会话数据失败:', e.message)
        return null
    }
}

/**
 * 处理会话系统事件：messageTypeIm=2 只操作 chat_session 表，不进入消息表
 * 策略：先按 sessionId 查本地 → 已有则 UPDATE，无则 INSERT（后端驱动 + 前端镜像）
 */
const processSessionEvent = (message) => {
    try {
        // 1. 解析会话对象：优先从 extendData（后端 ChatSession JSON），退化用消息根字段
        const fromExtend = parseSessionData(message.extendData)
        const sessionData = fromExtend || (
            message.sessionId
                ? {
                    sessionId: message.sessionId,
                    userId: message.userId || '',
                    targetUserId: message.targetUserId || message.sendUserId || '',
                    targetNickName: message.targetNickName || message.sendUserNickName || '',
                    sessionType: message.sessionType ?? 0,
                    botCategory: message.botCategory,
                    templateId: message.templateId,
                    botPersonality: message.botPersonality,
                    botAvatarPath: message.botAvatarPath,
                    botName: message.botName,
                    botDescription: message.botDescription,
                    botSystemPrompt: message.botSystemPrompt,
                    botWelcomeMsg: message.botWelcomeMsg,
                    lastMessage: message.messageContent,
                    lastMessageTime: message.sendTime || Date.now(),
                    isTop: message.isTop ?? 0,
                    deleted: message.deleted ?? 0
                }
                : null
        )

        if (!sessionData) {
            console.warn('[DB] 会话系统事件: 未能解析出 sessionId，跳过同步')
            return null
        }

        // 1.5 类型校正：按 target_user_id 前缀修正 sessionType，防止后端脏数据
        // 把用户单聊会话误标成群聊（群ID以 G 开头、用户ID为纯数字、机器人ID以 R 开头）
        sessionData.sessionType = chatSessionService.normalizeSessionType(
            sessionData.sessionType, sessionData.targetUserId, sessionData.sessionId)

        // 2. 按 sessionId + userId 查本地 → 有则 UPDATE，无则 INSERT
        const existing = chatSessionService.getSession(sessionData.sessionId, sessionData.userId)
        if (existing) {
            // 镜像：以后端下发为准全量覆盖（保留 sessionId 不变）
            const { sessionId, ...changes } = sessionData
            chatSessionService.serviceUpdateById(sessionId, sessionData.userId, changes)
            console.log('[DB] 会话系统事件: UPDATE chat_session, sessionId=', sessionId,
                'deleted=', sessionData.deleted, 'isTop=', sessionData.isTop)
        } else {
            chatSessionService.insertSession(sessionData)
            console.log('[DB] 会话系统事件: INSERT chat_session, sessionId=', sessionData.sessionId)
        }

        return sessionData
    } catch (e) {
        console.error('[DB] 会话系统事件处理失败:', e.message)
        return null
    }
}

/**
 * 根据 messageTypeIm 将消息走不同分支
 * messageTypeIm=0 → chat_im_message 表
 * messageTypeIm=1 → chat_message 表
 * messageTypeIm=2 → 只操作 chat_session 表（会话系统事件）
 */
const saveMessageToDb = (message) => {
    try {
        const mt = message.messageType
        const mti = message.messageTypeIm

        // 会话系统事件：独立分支，不走下面的 IM / 会议 消息存库
        if (mti === MessageTypeIm.SESSION) {
            processSessionEvent(message)
            return
        }

        // 会话通知（旧兼容）：解析 extendData 中的会话对象，存入 chat_session 表


        if (mti === MessageTypeIm.IM) {
            // 会话通知消息(SESSION_MESSAGE=36)：不存入消息表
            if (mt === MessageType.SESSION_MESSAGE) {
                console.log('[DB] IM通道的会话通知消息(36)，跳过存入消息表')
                return
            }

            // 自己发送的消息回声：通过 messageOnlyId 确认送达，不重复入库
            if (message.messageOnlyId) {
                const confirmed = chatImMessageService.confirmByMessageOnlyId(
                    message.messageOnlyId,
                    {
                        messageId: message.messageId,
                        sendTime: message.sendTime,
                        fileId: message.fileId,
                        filePath: message.filePath
                    }
                )
                if (confirmed) {
                    console.log('[DB] IM消息回声确认: messageOnlyId=', message.messageOnlyId, '→ status=1')
                    return
                }
                // confirmByMessageOnlyId 失败（消息尚未存入本地SQLite），以 status=1 存库
                // 防止竞态：WS回声先于 dbImMessageSend 到达时，消息不会被丢失
                console.log('[DB] IM消息回声未找到本地记录，直接存库: messageOnlyId=', message.messageOnlyId)
                // 关键：这是他人发来的消息（非自己回声）时，需要递增未读数并激活会话。
                // 自己发送的消息回声 sendUserId === getUserId()，不应递增未读。
                const isEcho = message.sendUserId === getUserId()
                chatImMessageService.receiveMessage({
                    // 后端的 messageId 映射到 backendMessageId，本地主键由 SQLite 自增
                    backendMessageId: message.messageId,
                    sessionId: message.sessionId || '',
                    sendUserId: message.sendUserId || '',
                    sendUserName: message.sendUserNickName || '',
                    sendUserAvatar: message.sendUserAvatar || '',
                    receiveUserId: message.receiveUserId || '',
                    messageType: message.messageType,
                    messageContent: typeof message.messageContent === 'string'
                        ? message.messageContent
                        : JSON.stringify(message.messageContent),
                    fileName: message.fileName || '',
                    fileSize: message.fileSize || 0,
                    fileId: message.fileId || '',
                    filePath: message.filePath || '',
                    contentType: message.contentType || '',
                    fileType: message.fileType ?? 0,
                    messageSendType: message.messageSend2Type ?? 0,
                    messageOnlyId: message.messageOnlyId,
                    status: 1,
                    sendTime: message.sendTime || Date.now(),
                    recalled: message.recalled ?? 0,
                    recallTime: message.recallTime ?? 0
                }, true, !isEcho)
                return
            }

            // 过滤: sendUserId 与 receiveUserId 相同且无 messageOnlyId 的消息（系统回执等）不存库
            if (message.sendUserId && message.receiveUserId &&
                message.sendUserId === message.receiveUserId) {
                console.log('[DB] IM消息: sendUserId 与 receiveUserId 相同且无messageOnlyId，跳过存库')
                return
            }

            // IM消息附带会话信息：仅同步会话元数据（不含 lastMessage/lastMessageTime，
            // 因为这两个字段依赖 receiveMessage → markActiveBySessionId 来写入正确值。
            // 若在此先写入 extendData 中的 lastMessage（在后端更新 DB 之前发送 WebSocket
            // 的场景下会是用户的旧提问），紧接着 markActiveBySessionId 虽然会再次写入 AI 回复，
            // 但两阶段写入存在窗口期会导致会话列表显示错误内容。保险策略：只写元数据，
            // lastMessage/lastMessageTime 完全由 markActiveBySessionId 统一维护。）
            const attachedSession = parseSessionData(message.extendData)
            if (attachedSession) {
                const { lastMessage, lastMessageTime, sessionId, ...meta } = attachedSession
                // 类型校正：按 target_user_id 前缀修正 sessionType，防止后端脏数据把用户单聊误标成群聊
                if (meta.sessionType !== undefined) {
                    meta.sessionType = chatSessionService.normalizeSessionType(
                        meta.sessionType, meta.targetUserId, sessionId)
                }
                const existing = chatSessionService.getSession(attachedSession.sessionId, attachedSession.userId)
                if (existing) {
                    chatSessionService.serviceUpdateById(attachedSession.sessionId, attachedSession.userId, meta)
                    console.log('[DB] IM消息附带会话: UPDATE chat_session(meta), sessionId=', attachedSession.sessionId)
                } else {
                    chatSessionService.insertSession(attachedSession)
                    console.log('[DB] IM消息附带会话: INSERT chat_session, sessionId=', attachedSession.sessionId)
                }
            }

            // IM消息 → chat_im_message 表
            // incrUnread=true：实时收到的他人消息，新入库时递增未读数（由 receiveMessage 内部去重保证不重复递增）
            chatImMessageService.receiveMessage({
                // 后端的 messageId 映射到 backendMessageId，本地主键由 SQLite 自增
                backendMessageId: message.messageId,
                sessionId: message.sessionId || '',
                sendUserId: message.sendUserId || '',
                sendUserName: message.sendUserNickName || '',
                sendUserAvatar: message.sendUserAvatar || '',
                receiveUserId: message.receiveUserId || '',
                messageType: message.messageType,
                messageContent: typeof message.messageContent === 'string'
                    ? message.messageContent
                    : JSON.stringify(message.messageContent),
                fileName: message.fileName || '',
                fileSize: message.fileSize || 0,
                fileId: message.fileId || '',
                filePath: message.filePath || '',
                contentType: message.contentType || '',
                fileType: message.fileType ?? 0,
                messageSendType: message.messageSend2Type ?? 0,
                status: message.status ?? 1,
                sendTime: message.sendTime || Date.now(),
                recalled: message.recalled ?? 0,
                recallTime: message.recallTime ?? 0
            }, true, true)
            console.log('[DB] IM消息已存入 chat_im_message 表, unread+1(若为新消息)')
        }
        else if (mti === MessageTypeIm.MEETING) {
            // 会议消息 → chat_message 表
            // 过滤: sendUserId 与 receiveUserId 相同的消息（系统通知/自己发自己）不存库
            if (message.sendUserId && message.receiveUserId &&
                message.sendUserId === message.receiveUserId) {
                console.log('[DB] 会议消息: sendUserId 与 receiveUserId 相同，跳过存库')
                return
            }

            const content = message.messageContent
            let messageContent = ''
            let meetingNo = ''

            if (typeof content === 'object' && content !== null) {
                messageContent = content.content || JSON.stringify(content)
                meetingNo = content.meetingNo || message.extendData || ''
            } else {
                messageContent = content || ''
                meetingNo = message.extendData || ''
            }

            chatMessageService.receiveMessage({
                messageId: message.messageId || Date.now(),
                meetingId: message.meetingId || message.receiveUserId || '',
                meetingNo: meetingNo,
                messageSendType: message.messageSend2Type ?? 0,
                sendUserId: message.sendUserId || '',
                sendUserNickName: message.sendUserNickName || '',
                receiveUserId: message.receiveUserId || '',
                messageType: message.messageType,
                messageContent: messageContent,
                fileSize: message.fileSize || 0,
                fileName: message.fileName || '',
                fileId: message.fileId || '',
                filePath: message.filePath || '',
                contentType: message.contentType || '',
                fileType: message.fileType ?? 0,
                extendData: message.extendData || '',
                status: message.status ?? 1,
                sendTime: message.sendTime || Date.now()
            })
            console.log('[DB] 会议消息已存入 chat_message 表')
        } else {
            console.warn('[DB] 未知消息通道类型 messageTypeIm=', mti, '，消息未存库')
        }
    } catch (e) {
        console.error('[DB] 消息存库失败:', e.message)
    }
}

/**
 * 处理消息撤回通知：把本地 chat_im_message 表中对应消息标记为 recalled=1
 * 收到后端推送的撤回通知（messageType=48）时调用
 * 注意：撤回通知不存为新消息，只更新已有消息的 recalled 标记
 */
const processRecall = (message) => {
    try {
        const extra = {}
        if (message.messageContent) {
            extra.messageContent = message.messageContent
        }
        // 优先按 messageOnlyId（字符串，不受雪花 messageId 精度丢失影响）精确匹配
        if (message.messageOnlyId) {
            chatImMessageService.markRecalledByMessageOnlyId(message.messageOnlyId, extra)
            console.log('[DB] 消息撤回已更新本地: messageOnlyId=', message.messageOnlyId)
            return
        }
        // 兜底：按 backendMessageId 匹配
        if (message.messageId) {
            chatImMessageService.markRecalledByBackendMessageId(message.messageId, extra)
            console.log('[DB] 消息撤回已更新本地: backendMessageId=', message.messageId)
            return
        }
        console.warn('[DB] 撤回通知缺少 messageOnlyId/messageId，跳过本地更新')
    } catch (e) {
        console.error('[DB] 消息撤回本地更新失败:', e.message)
    }
}

/**
 * 处理强制下线通知：阻止重连 + 通知渲染进程登出
 */
const handleForceOffline = (message) => {
    const reason = (typeof message.messageContent === 'string' ? message.messageContent : '') || '你已被管理员强制下线'
    console.warn('🚪 [主进程] 收到强制下线通知:', reason)
    // 阻止自动重连
    isManualClose = true
    needReconnect = false
    if (heartBeatTimer) {
        clearInterval(heartBeatTimer)
        heartBeatTimer = null
    }
    // 主动关闭 WebSocket（不再重连）
    try {
        if (ws) {
            ws.close(1000, 'force offline')
            ws = null
        }
    } catch (e) {
        console.error('关闭WebSocket时出错:', e.message)
    }
    // 通知所有窗口登出
    BrowserWindow.getAllWindows().forEach((w) => {
        if (w.isDestroyed()) return
        w.webContents.send('force-offline', { reason })
    })
}

// 是否需要通知：仅他人发来的 IM 聊天消息
// 排除：非IM通道 / 会话通知(36) / 撤回通知(48) / 群事件(40-45) / 自己发的(含回声)
const shouldNotify = (message) => {
    const mt = message.messageType
    const mti = message.messageTypeIm
    if (mti !== MessageTypeIm.IM) return false
    if (mt === MessageType.SESSION_MESSAGE) return false
    // 撤回通知不提醒：对方撤回消息不应弹桌面通知/响提示音
    if (mt === MessageType.MESSAGE_RECALL) return false
    if (mt >= 40 && mt <= 45) return false
    if (!message.sendUserId || message.sendUserId === getUserId()) return false
    return true
}

// 按软件设置触发桌面通知 + 提示音（仅在窗口未聚焦时）
const triggerNotifications = (message, mainWindow) => {
    let cfg = {}
    try { cfg = JSON.parse(getData('softwareSettings') || '{}') } catch (_) {}
    const sender = message.sendUserNickName || message.sendUserId || '新消息'
    let body = ''
    const mt = message.messageType
    if (mt === 32) body = '[图片]'
    else if (mt === 33) body = '[视频]'
    else if (mt === 34) body = '[语音]'
    else if (mt === 35) body = '[文件] ' + (message.fileName || '')
    else body = typeof message.messageContent === 'string' ? message.messageContent : '收到一条新消息'
    if (body.length > 50) body = body.slice(0, 50) + '…'

    const desktop = cfg.desktopNotification ?? true
    const sound = cfg.soundNotification ?? true
    if (desktop) {
        try {
            const n = new Notification({ title: sender, body, silent: true })
            n.on('click', () => {
                if (mainWindow && !mainWindow.isDestroyed()) { mainWindow.show(); mainWindow.focus() }
            })
            n.show()
        } catch (e) { console.warn('桌面通知失败:', e.message) }
    }
    if (sound && mainWindow && !mainWindow.isDestroyed()) {
        try { mainWindow.webContents.send('play-notification-sound') } catch (_) {}
    }
}

const handleMessage = (message) => {
    const mt = message.messageType;
    const mti = message.messageTypeIm;

    // 强制下线通知：最高优先级拦截，不进入存库/转发逻辑
    if (mt === MessageType.FORCE_OFFLINE) {
        handleForceOffline(message)
        return
    }

    console.log('==========================================')
    console.log('📨 [主进程] 收到服务器消息')
    console.log('📨 [主进程] 消息通道类型:', mti, MessageTypeImText?.[mti] || (mti === MessageTypeIm.IM ? 'IM消息' : mti === MessageTypeIm.MEETING ? '会议消息' : mti === MessageTypeIm.SESSION ? '会话系统事件' : '未知'))
    console.log('📨 [主进程] 消息类型:', mt, MessageTypeText[mt] || '未知')
    console.log('📨 [主进程] 原始消息:', JSON.stringify(message))

    const processedMessage = {
        ...message,
        messageTypeText: MessageTypeText[message.messageType] || '未知消息',
        timestamp: Date.now()
    }

    // ===== 群聊系统事件(messageType 40-45)：优先判断，更新本地群信息/群成员，再广播到渲染进程 =====
    // 注意：群事件 messageTypeIm=SESSION(2)，若不先于会话事件分支判断，
    // 会被下面的 processSessionEvent 拦截导致群成员/群状态更新逻辑永远不执行
    if (mt >= 40 && mt <= 45) {
        processGroupEvent(processedMessage)
        broadcastSessionEvent(processedMessage)
        console.log('==========================================')
        return
    }

    // ===== 会话系统事件(messageTypeIm=2 或 messageType=36)：只操作会话表，不走消息存库与文件处理 =====
    // 后端创建机器人会话时，推送的 SESSION_MESSAGE(36) 使用 messageTypeIm=IM(0)，
    // 需要在此拦截，按会话事件处理（upsert chat_session + 广播 session-event）
    if (mti === MessageTypeIm.SESSION || mt === MessageType.SESSION_MESSAGE) {
        // 1. 本地镜像：upsert 到 chat_session 表
        processSessionEvent(processedMessage)

        // 2. 广播到渲染进程（使用独立的 session-event 事件，让前端懒同步）
        broadcastSessionEvent(processedMessage)
        console.log('==========================================')
        return
    }

    // ===== 消息撤回通知(messageType=48)：更新本地消息 recalled 标记，不存为新消息 =====
    // 后端推送撤回时：messageId=被撤回消息的后端ID，messageContent=撤回提示文案，sendUserId=撤回发起人
    if (mt === MessageType.MESSAGE_RECALL) {
        processRecall(processedMessage)
        // 撤回通知走 IM 通道转发到主窗口，渲染进程据此更新 UI（把对应消息改成"已撤回"提示）
        sendMessageToRenderer(processedMessage)
        console.log('==========================================')
        return
    }

    // 按 messageTypeIm 分流存入数据库（IM/会议消息走原有逻辑）
    saveMessageToDb(processedMessage)

    // IM消息送达确认：收到并存储后，向后端发送ACK（不对自己发的消息回声发ACK）
    // 用 messageOnlyId 作为关联键（雪花 messageId 精度丢失，无法精确匹配后端重推任务）
    if (mti === MessageTypeIm.IM && message.messageOnlyId && message.sendUserId !== getUserId()) {
        sendMessage({
            messageType: MessageType.MESSAGE_ACK,
            messageId: message.messageId,
            messageOnlyId: message.messageOnlyId
        })
        console.log('📨 [主进程] IM消息ACK已发送: messageOnlyId=', message.messageOnlyId)
    }

    // 文件消息（32-35）：渐进式投递
    if (isFileMessage(mt)) {
        console.log('📁 [主进程] 文件消息，启动渐进式投递')

        // 先投递 "downloading" 状态
        sendMessageToRenderer({ ...processedMessage, _fileStatus: 'downloading' })

        // 异步处理文件（下载 + 缩略图）
        processFileMessage(message).then(({ localPath, thumbPath }) => {
            console.log('✅ [主进程] 文件处理完成:', localPath)
            sendFileReady({ ...processedMessage, _fileStatus: 'ready', localPath, thumbPath })
        }).catch(err => {
            console.error('❌ [主进程] 文件处理失败:', err.message)
            sendFileReady({ ...processedMessage, _fileStatus: 'error', _error: err.message })
        })
        return
    }

    // 非文件消息：直接投递
    console.log('📨 [主进程] 开始广播消息到渲染进程...')
    sendMessageToRenderer(processedMessage)
}

// ==================== 文件消息辅助 ====================

/** 判断是否为文件类型消息 */
const isFileMessage = (type) => FILE_MESSAGE_TYPES.includes(type)

/** 发送文件就绪通知（独立 IPC 通道，不污染 new-message） */
const sendFileReady = (data) => {
    const safe = JSON.parse(JSON.stringify(data))
    const mti = data.messageTypeIm
    const isImMessage = mti === MessageTypeIm.IM

    BrowserWindow.getAllWindows().forEach(w => {
        if (w.isDestroyed()) return
        const url = w.webContents.getURL()
        const isMeetingWindow = url.includes('/meeting-room') || w.getTitle() === '通话进行中'
        const isMainWindow = !isMeetingWindow

        if ((isImMessage && isMainWindow) || (!isImMessage && isMeetingWindow)) {
            w.webContents.send('file-ready', safe)
        }
    })
}

/** 会话系统事件广播：走独立的 session-event IPC 通道，只发给主窗口 */
const broadcastSessionEvent = (message) => {
    const safe = JSON.parse(JSON.stringify(message))
    let delivered = 0
    BrowserWindow.getAllWindows().forEach((w, i) => {
        if (w.isDestroyed()) return
        const url = w.webContents.getURL()
        const isMeetingWindow = url.includes('/meeting-room') || w.getTitle() === '通话进行中'
        const isMainWindow = !isMeetingWindow
        if (isMainWindow) {
            w.webContents.send('session-event', safe)
            delivered++
            console.log(`🧭 [主进程] 窗口 ${i + 1} 会话事件发送成功: sessionId=${message.sessionId}`)
        }
    })
    if (delivered === 0) {
        console.log('⚠️ [主进程] 会话系统事件: 没有主窗口可投递，已仅完成本地镜像')
    }
}

/**
 * 处理群聊系统事件（messageType 40-45）
 * 解析 extendData 中的群信息/成员信息，更新本地 SQLite
 */
const processGroupEvent = (message) => {
    try {
        const mt = message.messageType
        const sessionId = message.sessionId || ''
        let data = {}
        try {
            data = typeof message.extendData === 'string'
                ? JSON.parse(message.extendData)
                : (message.extendData || {})
        } catch (_) { data = {} }

        const groupId = data.groupId || sessionId

        switch (mt) {
            case MessageType.GROUP_MEMBER_JOIN: {
                // 新成员加入：保存群信息 + 成员
                if (data.groupInfo) {
                    groupService.saveGroupInfo(data.groupInfo)
                }
                if (data.members && Array.isArray(data.members)) {
                    groupService.batchSaveMembers(data.members)
                }
                // 单个成员加入
                if (data.member) {
                    groupService.saveMember(data.member)
                }
                console.log('[DB] 群聊事件: 成员加入, groupId=', groupId)
                break
            }
            case MessageType.GROUP_MEMBER_LEAVE: {
                // 成员主动退出
                if (data.userId) {
                    groupService.removeMember(groupId, data.userId)
                }
                console.log('[DB] 群聊事件: 成员退出, groupId=', groupId, 'userId=', data.userId)
                break
            }
            case MessageType.GROUP_MEMBER_KICKED: {
                // 成员被踢出
                if (data.targetUserId) {
                    groupService.kickMember(groupId, data.targetUserId)
                }
                // 如果被踢的是自己，软删除会话
                if (data.targetUserId === getUserId()) {
                    chatSessionService.deleteSession(groupId, getUserId())
                }
                console.log('[DB] 群聊事件: 成员被踢, groupId=', groupId, 'targetUserId=', data.targetUserId)
                break
            }
            case MessageType.GROUP_ROLE_CHANGE: {
                // 角色变更
                if (data.targetUserId && data.role) {
                    groupService.updateMemberRole(groupId, data.targetUserId, data.role)
                }
                console.log('[DB] 群聊事件: 角色变更, groupId=', groupId, 'targetUserId=', data.targetUserId, 'role=', data.role)
                break
            }
            case MessageType.GROUP_DISSOLVED: {
                // 群聊解散
                groupService.dissolveGroup(groupId)
                console.log('[DB] 群聊事件: 群聊已解散, groupId=', groupId)
                break
            }
            case MessageType.GROUP_SETTINGS_UPDATE: {
                // 群设置更新
                if (data.groupInfo) {
                    const { groupId: _gid, ...changes } = data.groupInfo
                    groupService.updateGroupInfo(groupId, changes)
                }
                console.log('[DB] 群聊事件: 群设置更新, groupId=', groupId)
                break
            }
            default:
                console.warn('[DB] 未知群聊事件类型:', mt)
        }
    } catch (e) {
        console.error('[DB] 群聊事件处理失败:', e.message)
    }
}

// ==================== 本地数据库检查 ====================

/** 本地数据库标记文件目录 */
const LOCAL_DB_DIR = () => {
  const dir = path.join(app.getPath('userData'), 'meetchat-localFiles')
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
  return dir
}

/** 标记文件路径 */
const DB_INIT_MARKER = () => path.join(LOCAL_DB_DIR(), 'db-initialized.json')

/**
 * 检查本地数据库是否已初始化
 * 通过标记文件 meetchat-localFiles/db-initialized.json 判断
 */
const isLocalDatabaseInitialized = () => {
  const markerPath = DB_INIT_MARKER()
  if (!fs.existsSync(markerPath)) return false
  try {
    const content = JSON.parse(fs.readFileSync(markerPath, 'utf-8'))
    return content.initialized === true
  } catch (_) {
    return false
  }
}

/**
 * 写入数据库初始化标记
 */
export const markDatabaseInitialized = () => {
  const markerPath = DB_INIT_MARKER()
  const userId = getUserId()
  fs.writeFileSync(markerPath, JSON.stringify({
    initialized: true,
    userId: userId,
    initTime: Date.now()
  }, null, 2))
  console.log('[DB] 数据库初始化标记已写入:', markerPath)
}

/**
 * WS连接成功后检查并初始化本地数据库
 * 主进程直接初始化SQLite数据库，无需通知渲染进程
 */
const checkAndInitLocalDatabase = () => {
  const initialized = isLocalDatabaseInitialized()
  console.log('[DB] 本地数据库初始化状态:', initialized)

  if (!initialized) {
    const success = initDatabase()
    if (success) {
      markDatabaseInitialized()
      console.log('[DB] 本地数据库初始化完成，标记文件已写入')
    }
  }
}

const createWs = () => {
    if (wsUrl == null) {
        return
    }

    if (heartBeatTimer) {
        clearInterval(heartBeatTimer);
        heartBeatTimer = null;
    }

    console.log('创建新的WebSocket连接...')
    ws = new WebSocket(wsUrl)

    ws.onopen = function (params) {
        console.log('✅ 客户端连接成功')
        ws.send('heart beat')
        maxReConnectTimes = 5

        // WS连接成功后，检查本地数据库是否已初始化
        checkAndInitLocalDatabase()

        // 会话列表同步（解决离线期间的会话变更：被邀请入群/被踢/退群/群解散）
        chatSessionService.syncSessions().then(({ sessions, removedSessionIds }) => {
            if ((sessions && sessions.length > 0) || (removedSessionIds && removedSessionIds.length > 0)) {
                console.log(`🔄 [主进程] 会话同步完成: ${sessions.length}条, 清理${removedSessionIds.length}条无效群会话`)
                // 通知渲染进程刷新会话列表（复用 session-event 通道）
                broadcastSessionEvent({
                    messageType: MessageType.SESSION_MESSAGE,
                    messageTypeIm: MessageTypeIm.SESSION,
                    sessionId: '',
                    sendTime: Date.now(),
                    messageContent: '会话列表已同步',
                    extendData: JSON.stringify(sessions || [])
                })
            }
        }).catch((err) => {
            console.error('[DB] 会话同步异常:', err && err.message ? err.message : err)
        })

        // 拉取私聊离线消息（增量同步：本地最新 backend_message_id 之后的所有消息）
        chatImMessageService.pullOfflineMessages().then((messages) => {
            if (messages && messages.length > 0) {
                console.log(`📥 [主进程] 离线消息已保存到本地DB: ${messages.length} 条`)
                // 逐条处理离线消息
                for (const msg of messages) {
                    // 1. 向后端发送ACK，告诉它已收到
                    // 关键：用 messageOnlyId（字符串）作为关联键，避免雪花 messageId 精度丢失
                    if (msg.messageOnlyId) {
                        sendMessage({
                            messageType: MessageType.MESSAGE_ACK,
                            messageId: msg.backendMessageId,
                            messageOnlyId: msg.messageOnlyId
                        })
                    }

                    // 2. 通知渲染进程（Vue）更新UI
                    // 确保消息对象有 messageTypeIm 字段（IM消息为0）
                    const messageToRenderer = {
                        ...msg,
                        messageTypeIm: msg.messageTypeIm ?? MessageTypeIm.IM,
                        messageTypeText: MessageTypeText[msg.messageType] || '未知消息',
                        timestamp: Date.now()
                    }
                    sendMessageToRenderer(messageToRenderer)
                }
                console.log(`📨 [主进程] 离线消息ACK已发送且通知渲染进程: ${messages.length} 条`)
            } else {
                console.log('📭 [主进程] 无离线消息')
            }
        }).catch((err) => {
            console.error('[DB] 离线消息拉取异常:', err && err.message ? err.message : err, err && err.stack)
        })

        // 拉取群聊离线消息（群消息单条存储 receive_user_id=groupId，按所在群增量拉取）
        chatImMessageService.pullOfflineGroupMessages().then((messages) => {
            if (messages && messages.length > 0) {
                console.log(`📥 [主进程] 群离线消息已保存到本地DB: ${messages.length} 条`)
                for (const msg of messages) {
                    // ACK：仅对他人发送的消息（自己发送的已通过HTTP响应确认）
                    if (msg.messageOnlyId && msg.sendUserId !== getUserId()) {
                        sendMessage({
                            messageType: MessageType.MESSAGE_ACK,
                            messageId: msg.backendMessageId,
                            messageOnlyId: msg.messageOnlyId
                        })
                    }
                    // 通知渲染进程更新UI
                    const messageToRenderer = {
                        ...msg,
                        messageTypeIm: msg.messageTypeIm ?? MessageTypeIm.IM,
                        messageTypeText: MessageTypeText[msg.messageType] || '未知消息',
                        timestamp: Date.now()
                    }
                    sendMessageToRenderer(messageToRenderer)
                }
                console.log(`📨 [主进程] 群离线消息ACK已发送且通知渲染进程: ${messages.length} 条`)
            } else {
                console.log('📭 [主进程] 无群离线消息')
            }
        }).catch((err) => {
            console.error('[DB] 群离线消息拉取异常:', err && err.message ? err.message : err)
        })
    }

    ws.onmessage = async function (e) {
        let mainWindow = getWindow("main");
        console.log('收到服务器消息', e.data)
        const message = JSON.parse(e.data);
        // 窗口未聚焦时，按软件设置弹桌面通知 + 播放提示音 + 任务栏闪烁
        // 关键：仅对"需要提醒的消息"（他人发来的聊天消息）生效。
        // 自己发送的消息回声、撤回通知、会话通知、群事件都不应触发任何提醒（含任务栏闪烁）
        if (mainWindow && !mainWindow.isFocused() && shouldNotify(message)) {
            mainWindow.flashFrame(true)
            triggerNotifications(message, mainWindow)
        }
        const leaveGroupUserId = message.extendData;
        const messageType = message.messageType;

        switch (messageType) {
            }
        handleMessage(message)
    }

    ws.onclose = function (evt) {
        console.log('📤 onclose事件触发, code:', evt.code, 'reason:', evt.reason)
        console.log('当前 isManualClose:', isManualClose)
        reconnect('onclose')
    }

    ws.onerror = function (evt) {
        console.log('❌ onerror事件触发')
        reconnect('onerror')
    }

    heartBeatTimer = setInterval(() => {
        if (ws != null && ws.readyState == 1) {
            ws.send('heart beat')
        }
    }, 1000 * 5);
}

export {
    initWs,
    closeWs,
    sendMessage
}
