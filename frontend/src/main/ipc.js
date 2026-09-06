import { ipcMain, BrowserWindow, screen, shell, dialog, app } from "electron"
import { join } from 'path'
import { existsSync, copyFileSync } from 'fs'
import { initUserId, setUserData, getUserData, getData, setData, getUserId } from './store'
import { setUserInfo } from './tray'
import { is } from '@electron-toolkit/utils'
import { initWs, closeWs, sendMessage } from './wsClient'
import { chatSessionService, chatImMessageService, chatMessageService, groupService, initDatabase } from './db'
let mainWindow = null
let cachedMeetingData = null

export const setMainWindow = (window) => {
  mainWindow = window
}

export const registerChangeWindowSize = () => {
  ipcMain.on('change-window-size', () => {
    if (mainWindow) {
      const { width, height } = screen.getPrimaryDisplay().workAreaSize
      const newWidth = 800
      const newHeight = 520
      const x = Math.round((width - newWidth) / 2)
      const y = Math.round((height - newHeight) / 2)
      mainWindow.setResizable(true)
      mainWindow.setSize(newWidth, newHeight)
      mainWindow.center()
      // 保持窗口可自由调整大小，仅设置最小尺寸避免缩太小导致界面错乱
      mainWindow.setMinimumSize(600, 480)
    }
  })
}

export const registerSaveUserInfo = () => {
  ipcMain.on('save-user-info', (event, userInfo) => {
    if (userInfo && userInfo.userId) {
      initUserId(userInfo.userId)
      Object.keys(userInfo).forEach(key => {
        setUserData(key, userInfo[key])
      })
      setUserInfo(userInfo)

      // 保存服务器配置到主进程存储（供会议窗口使用）
      if (userInfo.wsHost) {
        setData('serverConfig', JSON.stringify({
          host: userInfo.wsHost,
          httpPort: userInfo.httpPort || '6060',
          wsPort: userInfo.wsPort || '6061',
          wsPath: '/ws'
        }))
      }

      initWs({
        host: userInfo.wsHost || 'localhost',
        port: userInfo.wsPort || 6061,
        path: '/ws',
        token: userInfo.token
      })
    }
  })
}

export const registerOpenMeetingWindow = () => {
  ipcMain.on('open-meeting-window', (event, meetingData) => {
    console.log('收到打开会议窗口请求, meetingData:', meetingData)

    // 安全序列化：确保数据不包含不可克隆的属性
    cachedMeetingData = JSON.parse(JSON.stringify(meetingData))

    try {
      const { width: screenWidth, height: screenHeight } = screen.getPrimaryDisplay().workAreaSize
      const windowWidth = 900
      const windowHeight = 650
      const x = Math.round((screenWidth - windowWidth) / 2)
      const y = Math.round((screenHeight - windowHeight) / 2)

      const meetingWindow = new BrowserWindow({
        width: windowWidth,
        height: windowHeight,
        x,
        y,
        resizable: true,
        show: false,
        autoHideMenuBar: true,
        title: '通话进行中',
        closable: false,  // 禁止用户通过窗口按钮关闭
        webPreferences: {
          preload: join(__dirname, '../preload/index.js'),
          sandbox: false,
          webSecurity: false
        }
      })

      // 监听关闭事件，阻止用户手动关闭
      meetingWindow.on('close', (event) => {
        console.log('🚫 [主进程] 用户尝试关闭会议窗口，已阻止')
        event.preventDefault()
      })

      console.log('会议窗口已创建')

      // 开发模式下自动打开开发者工具
      if (is.dev) {
        meetingWindow.webContents.openDevTools()
      }

      meetingWindow.on('ready-to-show', () => {
        console.log('会议窗口准备显示')
        meetingWindow.show()
      })

      if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
        console.log('开发模式加载URL:', `${process.env['ELECTRON_RENDERER_URL']}#/meeting-room`)
        meetingWindow.loadURL(`${process.env['ELECTRON_RENDERER_URL']}#/meeting-room`)
      } else {
        console.log('生产模式加载文件')
        meetingWindow.loadFile(join(__dirname, '../renderer/index.html'), {
          hash: '/meeting-room'
        })
      }

      meetingWindow.webContents.once('did-finish-load', () => {
        console.log('会议窗口页面加载完成，发送会议数据')

        // 注入服务器配置到会议窗口（新窗口不共享登录页的 localStorage）
        const serverConfig = getData('serverConfig')
        if (serverConfig) {
          meetingWindow.webContents.executeJavaScript(`
            localStorage.setItem('serverConfig', ${JSON.stringify(serverConfig)});
          `)
        }

        if (meetingData) {
          meetingWindow.webContents.send('meeting-data', meetingData)
        }
      })

      meetingWindow.webContents.once('did-fail-load', (_event, _errorCode, _errorDescription) => {
        console.error('会议窗口页面加载失败:', _errorDescription)
      })

      meetingWindow.on('closed', () => {
        console.log('🚪 [主进程] 会议窗口已关闭')
      })
    } catch (error) {
      console.error('创建会议窗口时发生错误:', error)
    }
  })

  ipcMain.handle('get-meeting-data', () => {
    // 安全序列化：确保数据不包含不可克隆的属性
    return cachedMeetingData ? JSON.parse(JSON.stringify(cachedMeetingData)) : null
  })

  ipcMain.handle('close-meeting-window', async (event, delay) => {
    console.log(`==========================================`)
    console.log(`⏱️ [主进程] 收到关闭会议窗口请求，延迟 ${delay} 毫秒`)

    return new Promise((resolve) => {
      setTimeout(() => {
        const allWindows = BrowserWindow.getAllWindows()
        console.log(`⏱️ [主进程] 当前窗口数量: ${allWindows.length}`)

        allWindows.forEach((w, i) => {
          const url = w.webContents.getURL()
          console.log(`⏱️ [主进程] 窗口 ${i + 1}: URL="${url}", 标题="${w.getTitle()}", 销毁=${w.isDestroyed()}, 可见=${w.isVisible()}`)
        })

        const meetingWindows = allWindows.filter(w => {
          const url = w.webContents.getURL()
          return (url.includes('/meeting-room') || w.getTitle() === '通话进行中') && !w.isDestroyed()
        })

        console.log(`⏱️ [主进程] 找到会议窗口数量: ${meetingWindows.length}`)

        if (meetingWindows.length > 0) {
          console.log(`⏱️ [主进程] 强制关闭会议窗口`)
          // 直接销毁窗口，不触发close事件
          meetingWindows[0].destroy()
          console.log('✅ [主进程] 会议窗口已销毁')
        } else {
          console.log('❌ [主进程] 未找到会议窗口')
        }
        resolve(true)
      }, delay)
    })
  })
}

export const registerLogout = () => {
  ipcMain.on('logout', () => {
    console.log('用户退出登录，关闭WebSocket连接')
    closeWs()
  })
}

export const registerMessageHandlers = () => {
  // ==================== 本地数据库 IPC ====================

  // 初始化数据库
  ipcMain.handle('db-init', () => {
    return initDatabase()
  })

  // ---- ChatSession ----

  ipcMain.handle('db-session-list', (_event, userId) => {
    return chatSessionService.getSessionList(userId)
  })

  ipcMain.handle('db-session-get', (_event, sessionId) => {
    return chatSessionService.getSession(sessionId, getUserId())
  })

  ipcMain.handle('db-session-get-by-target', (_event, userId, targetUserId) => {
    return chatSessionService.getSessionByTargetUserId(userId, targetUserId)
  })

  ipcMain.handle('db-session-list-by-type', (_event, userId, sessionType) => {
    return chatSessionService.getSessionListByType(userId, sessionType)
  })

  ipcMain.handle('db-session-save', (_event, data) => {
    return chatSessionService.saveSession(data)
  })

  ipcMain.handle('db-session-batch-save', (_event, sessions) => {
    chatSessionService.batchSaveSessions(sessions)
    return true
  })

  ipcMain.handle('db-session-update-last-message', (_event, sessionId, lastMessage, lastMessageTime) => {
    chatSessionService.updateLastMessage(sessionId, getUserId(), lastMessage, lastMessageTime)
    return true
  })

  ipcMain.handle('db-session-toggle-top', (_event, sessionId, isTop) => {
    chatSessionService.toggleTop(sessionId, getUserId(), isTop)
    return true
  })

  ipcMain.handle('db-session-delete', (_event, sessionId) => {
    chatSessionService.deleteSession(sessionId, getUserId())
    return true
  })

  // 隐藏聊天：软删除会话
  ipcMain.handle('db-session-hide', (_event, sessionId) => {
    chatSessionService.deleteSession(sessionId, getUserId())
    return true
  })

  // 恢复被软删除的会话（好友/群聊点击"聊天"时调用）
  ipcMain.handle('db-session-restore-by-target', (_event, targetUserId) => {
    return chatSessionService.restoreSessionByTargetUserId(getUserId(), targetUserId)
  })

  // 删除聊天：删除该会话的所有消息 + 软删除会话
  ipcMain.handle('db-session-destroy', (_event, sessionId) => {
    chatImMessageService.deleteMessagesBySessionId(sessionId)
    chatSessionService.deleteSession(sessionId, getUserId())
    return true
  })

  // 新消息到达时刷新会话：更新 lastMessage / lastMessageTime / 清除软删除标记
  ipcMain.handle('db-session-mark-active', (_event, sessionId, update) => {
    return chatSessionService.markActiveBySessionId(sessionId, getUserId(), update)
  })

  // 未读数清零（打开会话时调用）
  ipcMain.handle('db-session-reset-unread', (_event, sessionId) => {
    chatSessionService.resetUnread(sessionId, getUserId())
    return true
  })

  // ---- ChatImMessage ----

  ipcMain.handle('db-im-message-list', (_event, sessionId, pageSize, beforeTime) => {
    return chatImMessageService.getMessageList(sessionId, pageSize, beforeTime || null)
  })

  ipcMain.handle('db-im-message-latest', (_event, sessionId) => {
    return chatImMessageService.getLatestMessage(sessionId)
  })

  ipcMain.handle('db-im-message-send', (_event, data) => {
    return chatImMessageService.sendMessage(data)
  })

  ipcMain.handle('db-im-message-receive', (_event, data) => {
    return chatImMessageService.receiveMessage(data)
  })

  ipcMain.handle('db-im-message-sync', (_event, messages) => {
    chatImMessageService.syncMessages(messages)
    return true
  })

  ipcMain.handle('db-im-message-update-status', (_event, messageId, status) => {
    chatImMessageService.updateMessageStatus(messageId, status)
    return true
  })

  ipcMain.handle('db-im-message-list-by-type', (_event, sessionId, messageType) => {
    return chatImMessageService.getMessagesByType(sessionId, messageType)
  })

  ipcMain.handle('db-im-message-count', (_event, sessionId) => {
    return chatImMessageService.countMessages(sessionId)
  })

  ipcMain.handle('db-im-message-delete-by-session', (_event, sessionId) => {
    chatImMessageService.deleteMessagesBySessionId(sessionId)
    return true
  })

  // 删除单条消息（仅本地删除，不影响后端和其他端）
  ipcMain.handle('db-im-message-delete', (_event, messageId) => {
    chatImMessageService.deleteMessage(messageId)
    return true
  })

  // 标记消息为已撤回（按本地主键，自己发起撤回时本地立即生效）
  ipcMain.handle('db-im-message-mark-recalled', (_event, messageId, extra) => {
    chatImMessageService.markRecalledByMessageId(messageId, extra || {})
    return true
  })

  // 标记消息为已撤回（按 messageOnlyId，绕开雪花 messageId 精度丢失问题）
  ipcMain.handle('db-im-message-mark-recalled-by-only-id', (_event, messageOnlyId, extra) => {
    chatImMessageService.markRecalledByMessageOnlyId(messageOnlyId, extra || {})
    return true
  })

  // 标记消息为已撤回（按后端 messageId，收到后端撤回通知时调用）
  ipcMain.handle('db-im-message-mark-recalled-by-backend', (_event, backendMessageId, extra) => {
    chatImMessageService.markRecalledByBackendMessageId(backendMessageId, extra || {})
    return true
  })

  // ---- ChatMessage (会议聊天) ----

  ipcMain.handle('db-meeting-message-list', (_event, meetingNo, pageSize, beforeTime) => {
    return chatMessageService.getMessageList(meetingNo, pageSize, beforeTime || null)
  })

  ipcMain.handle('db-meeting-message-send', (_event, data) => {
    return chatMessageService.sendMessage(data)
  })

  ipcMain.handle('db-meeting-message-receive', (_event, data) => {
    return chatMessageService.receiveMessage(data)
  })

  ipcMain.handle('db-meeting-message-sync', (_event, messages) => {
    chatMessageService.syncMessages(messages)
    return true
  })

  ipcMain.handle('db-meeting-message-update-status', (_event, messageId, status) => {
    chatMessageService.updateMessageStatus(messageId, status)
    return true
  })

  ipcMain.handle('db-meeting-message-list-by-type', (_event, meetingNo, messageType) => {
    return chatMessageService.getMessagesByType(meetingNo, messageType)
  })

  ipcMain.handle('db-meeting-message-count', (_event, meetingNo) => {
    return chatMessageService.countMessages(meetingNo)
  })

  ipcMain.handle('db-meeting-message-delete-by-meeting', (_event, meetingNo) => {
    chatMessageService.deleteMessagesByMeetingNo(meetingNo)
    return true
  })

  // ---- GroupInfo / GroupMember ----

  ipcMain.handle('db-group-info-get', (_event, groupId) => {
    return groupService.getGroupInfo(groupId)
  })

  ipcMain.handle('db-group-info-save', (_event, data) => {
    return groupService.saveGroupInfo(data)
  })

  ipcMain.handle('db-group-info-update', (_event, groupId, changes) => {
    return groupService.updateGroupInfo(groupId, changes)
  })

  ipcMain.handle('db-group-dissolve', (_event, groupId) => {
    groupService.dissolveGroup(groupId)
    return true
  })

  ipcMain.handle('db-group-is-dissolved', (_event, groupId) => {
    return groupService.isGroupDissolved(groupId)
  })

  ipcMain.handle('db-group-members-get', (_event, groupId) => {
    return groupService.getGroupMembers(groupId)
  })

  ipcMain.handle('db-group-member-get', (_event, groupId, userId) => {
    return groupService.getMember(groupId, userId)
  })

  ipcMain.handle('db-group-member-save', (_event, data) => {
    return groupService.saveMember(data)
  })

  ipcMain.handle('db-group-members-batch-save', (_event, members) => {
    groupService.batchSaveMembers(members)
    return true
  })

  ipcMain.handle('db-group-member-update-role', (_event, groupId, userId, role) => {
    groupService.updateMemberRole(groupId, userId, role)
    return true
  })

  ipcMain.handle('db-group-member-remove', (_event, groupId, userId) => {
    groupService.removeMember(groupId, userId)
    return true
  })

  ipcMain.handle('db-group-member-kick', (_event, groupId, userId) => {
    groupService.kickMember(groupId, userId)
    return true
  })

  ipcMain.handle('db-group-my-role', (_event, groupId, userId) => {
    return groupService.getMyRole(groupId, userId)
  })

  ipcMain.handle('db-group-sync', (_event, groupData, members) => {
    groupService.syncGroupData(groupData, members)
    return true
  })
}

/**
 * 服务器连接测试（走主进程 Node.js，无 CORS 限制）
 */
export const registerServerTest = () => {
  ipcMain.handle('test-server-connection', async (event, { host, httpPort }) => {
    const url = `http://${host}:${httpPort || 6060}/api/account/getImage`
    console.log('🔌 [主进程] 测试服务器连接:', url)

    try {
      const http = require('http')
      return new Promise((resolve) => {
        const req = http.get(url, { timeout: 5000 }, (res) => {
          let data = ''
          res.on('data', chunk => data += chunk)
          res.on('end', () => {
            try {
              const json = JSON.parse(data)
              if (json.code === 200) {
                resolve({ success: true, msg: '连接成功！服务器正常响应' })
              } else {
                resolve({ success: false, msg: `服务器响应异常: ${json.info || '未知'}` })
              }
            } catch (e) {
              resolve({ success: false, msg: `响应解析失败: ${e.message}` })
            }
          })
        })
        req.on('error', (e) => {
          resolve({ success: false, msg: `连接失败: ${e.message}` })
        })
        req.on('timeout', () => {
          req.destroy()
          resolve({ success: false, msg: '连接超时，请检查 IP 和端口是否正确' })
        })
      })
    } catch (e) {
      return { success: false, msg: `请求异常: ${e.message}` }
    }
  })
}

/**
 * 通用 API 请求代理（生产环境渲染进程通过 IPC 调用此方法，绕过 CORS）
 * 支持表单（application/x-www-form-urlencoded）和 JSON（application/json）两种内容类型
 */
export const registerApiProxy = () => {
  ipcMain.handle('api-request', async (event, { url, baseURL, params, json, headers, responseType }) => {
    const fullUrl = `${baseURL}${url}`
    console.log('🔌 [主进程] API 代理请求:', fullUrl, json ? '(JSON)' : '(Form)')

    return new Promise((resolve) => {
      const http = require('http')

      // 根据内容类型构建 POST 数据
      let postData, contentType
      if (json) {
        // JSON 模式：序列化为 JSON 字符串（保留数字/布尔类型）
        postData = JSON.stringify(params || {})
        contentType = 'application/json'
      } else {
        // 表单模式：URL 编码
        const querystring = require('querystring')
        postData = params ? querystring.stringify(params) : ''
        contentType = 'application/x-www-form-urlencoded;charset=UTF-8'
      }

      const urlObj = new URL(fullUrl)

      const options = {
        hostname: urlObj.hostname,
        port: urlObj.port,
        path: urlObj.pathname,
        method: 'POST',
        timeout: 10000,
        headers: {
          'Content-Type': contentType,
          'Content-Length': Buffer.byteLength(postData),
          'X-Requested-With': 'XMLHttpRequest',
          'token': headers?.token || ''
        }
      }

      const req = http.request(options, (res) => {
        let data = ''
        res.on('data', chunk => data += chunk)
        res.on('end', () => {
          try {
            const json = JSON.parse(data)
            resolve({ data: json })
          } catch (e) {
            resolve({ data: null, error: `响应解析失败: ${e.message}` })
          }
        })
      })

      req.on('error', (e) => {
        resolve({ data: null, error: `请求失败: ${e.message}` })
      })

      req.on('timeout', () => {
        req.destroy()
        resolve({ data: null, error: '请求超时' })
      })

      req.write(postData)
      req.end()
    })
  })
}

/**
 * WebRTC 信令中转：渲染进程 → 主进程 → WebSocket → 服务器
 * 渲染进程通过 IPC 将 WebRTC 信令消息发到主进程，主进程通过 WebSocket 发送给服务器
 */
export const registerWebRtcSignaling = () => {
  ipcMain.on('webrtc-signal', (event, signalData) => {
    console.log('📡 [主进程] 收到 WebRTC 信令消息:', JSON.stringify(signalData))
    const success = sendMessage(signalData)
    if (!success) {
      console.error('❌ [主进程] WebRTC 信令发送失败：WebSocket 未连接')
    }
  })
}

/**
 * 获取服务器配置（供会议窗口使用）
 * 新 BrowserWindow 不共享登录页的 localStorage，通过 IPC 获取
 */
export const registerGetServerConfig = () => {
  ipcMain.handle('get-server-config', () => {
    const config = getData('serverConfig')
    if (config) {
      try { return JSON.parse(config) } catch (_) { return null }
    }
    return null
  })
}

// ==================== 文件传输 IPC ====================

import {
  uploadFile,
  downloadFile,
  checkFile,
  getLocalFilePath,
  saveFileMessage,
  uploadAvatar,
  checkAvatar,
  downloadAvatar,
  chunkUpload,
  cancelChunkUpload,
  saveTempFile,
  cleanupTempFile
} from './file'

/**
 * 文件操作 IPC 注册
 * 所有文件相关的操作都通过主进程进行，渲染层只发起请求
 */
export const registerFileHandlers = () => {
  // 上传文件到后端
  ipcMain.handle('file-upload', async (event, { fileBuffer, filePath, fileName, meetingNo }) => {
    try {
      const token = getUserData('token') || getData('token') || ''
      console.log('🆙 [IPC] 文件上传请求, fileName:', fileName,
        'fileBuffer?', !!(fileBuffer && (fileBuffer.byteLength || fileBuffer.length)),
        'filePath?', !!filePath,
        'token?', token ? '已设置 (' + token.substring(0, 8) + '...)' : '空')
      const result = await uploadFile({ fileBuffer, filePath, fileName, meetingNo, token })
      return { success: true, data: result }
    } catch (e) {
      console.error('❌ [IPC] 文件上传失败:', e.message)
      return { success: false, error: e.message }
    }
  })

  // 从后端下载文件到本地
  ipcMain.handle('file-download', async (event, { fileId, filePath, fileType }) => {
    try {
      const localPath = await downloadFile({ fileId, filePath, fileType })
      return { success: true, data: { localPath } }
    } catch (e) {
      console.error('❌ [IPC] 文件下载失败:', e.message)
      return { success: false, error: e.message }
    }
  })

  // 检查文件是否在本地缓存中（返回完整信息：exists, localPath, thumbPath）
  ipcMain.handle('file-check', async (event, { fileId, filePath, fileType }) => {
    const result = checkFile({ fileId, filePath, fileType })
    return result
  })

  // 用系统默认程序打开本地文件
  ipcMain.handle('file-open', async (event, filePath) => {
    if (!filePath) return { success: false, error: '文件路径为空' }
    try {
      await shell.openPath(filePath)
      return { success: true }
    } catch (e) {
      console.error('❌ [IPC] 打开文件失败:', filePath, e.message)
      return { success: false, error: e.message }
    }
  })

  // 将已下载文件另存为到用户指定位置
  ipcMain.handle('file-save-as', async (event, { sourcePath, fileName }) => {
    if (!sourcePath) return { success: false, error: '源文件路径为空' }
    if (!existsSync(sourcePath)) return { success: false, error: '源文件不存在' }
    try {
      const result = await dialog.showSaveDialog(BrowserWindow.getFocusedWindow(), {
        title: '另存为',
        defaultPath: fileName || sourcePath.split(/[\\/]/).pop()
      })
      if (result.canceled || !result.filePath) {
        return { success: false, canceled: true }
      }
      copyFileSync(sourcePath, result.filePath)
      return { success: true, savedPath: result.filePath }
    } catch (e) {
      console.error('❌ [IPC] 另存为失败:', sourcePath, e.message)
      return { success: false, error: e.message }
    }
  })

  // 保存文件消息到本地
  ipcMain.on('file-message-save', (event, message) => {
    saveFileMessage(message)
  })

  // ==================== 分片上传 IPC ====================

  // 分片上传（完整流程：hash → check → chunk → merge）
  ipcMain.handle('file-chunk-upload', async (event, { filePath, fileName, meetingNo, chunkSize, concurrency }) => {
    try {
      const result = await chunkUpload({ filePath, fileName, meetingNo, chunkSize, concurrency })
      return { success: true, data: result }
    } catch (e) {
      console.error('❌ [IPC] 分片上传失败:', e.message)
      return { success: false, error: e.message }
    }
  })

  // 取消分片上传
  ipcMain.handle('file-chunk-cancel', async (event, { uploadId }) => {
    try {
      const result = await cancelChunkUpload({ uploadId })
      return { success: true, data: result }
    } catch (e) {
      return { success: false, error: e.message }
    }
  })

  // 保存临时文件（渲染进程大文件 → 主进程本地路径）
  ipcMain.handle('file-save-temp', async (event, { fileBuffer, fileName }) => {
    try {
      const filePath = saveTempFile({ fileBuffer, fileName })
      return { success: true, filePath }
    } catch (e) {
      return { success: false, error: e.message }
    }
  })

  // 清理临时上传文件
  ipcMain.handle('file-cleanup-temp', async (event, { filePath }) => {
    cleanupTempFile(filePath)
    return { success: true }
  })

  // ==================== 头像 IPC（以 userId 为键，不同于文件消息的 fileId）====================
  // scope: 'user' (默认) | 'bot' — 机器人头像走独立缓存目录，避免覆盖用户头像

  ipcMain.handle('avatar-upload', async (event, { userId, fileBuffer, fileName, scope }) => {
    try {
      const token = getUserData('token') || getData('token') || ''
      const result = await uploadAvatar({ userId, fileBuffer, fileName, token, scope })
      return { success: true, data: result }
    } catch (e) {
      return { success: false, error: e.message }
    }
  })

  ipcMain.handle('avatar-check', async (event, { userId, fileId, filePath, scope }) => {
    return checkAvatar({ userId, fileId, filePath, scope })
  })

  ipcMain.handle('avatar-download', async (event, { userId, fileId, filePath, fileType, scope }) => {
    try {
      const localPath = await downloadAvatar({ userId, fileId, filePath, fileType, scope })
      return { success: true, data: { localPath } }
    } catch (e) {
      return { success: false, error: e.message }
    }
  })
}

/**
 * 软件设置 IPC：同步设置到主进程 store，并应用需要主进程处理的项
 * 渲染进程保存设置时调用 settings-update，确保每项设置真正生效（开机自启等）
 */
export const registerSettingsHandlers = () => {
  // 设置更新：同步到主进程 store，供 wsClient 通知/声音、主窗口 close/minimize 到托盘等读取
  ipcMain.handle('settings-update', async (_event, settings) => {
    try {
      setData('softwareSettings', JSON.stringify(settings))
      // 开机自启动：Electron 原生能力，立即生效
      try {
        app.setLoginItemSettings({ openAtLogin: !!settings.autoStart })
      } catch (e) {
        console.warn('⚠️ [设置] 开机自启应用失败:', e.message)
      }
      return { success: true }
    } catch (e) {
      return { success: false, error: e.message }
    }
  })

  // 获取应用真实信息（版本/运行环境），替代 About 页写死的模拟数据
  ipcMain.handle('get-app-info', () => ({
    version: app.getVersion(),
    electron: process.versions.electron,
    chrome: process.versions.chrome,
    node: process.versions.node,
    platform: process.platform,
    buildType: is.dev ? '开发版' : '正式版'
  }))

  // 打开用户数据目录（查看日志/本地数据）
  ipcMain.handle('open-user-data-dir', async () => {
    try {
      await shell.openPath(app.getPath('userData'))
      return { success: true }
    } catch (e) {
      return { success: false, error: e.message }
    }
  })
}
