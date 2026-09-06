import { contextBridge, webUtils } from 'electron'
import { electronAPI } from '@electron-toolkit/preload'

const api = {
  changeWindowSize: () => {
    electronAPI.ipcRenderer.send('change-window-size')
  },
  saveUserInfo: (userInfo) => {
    electronAPI.ipcRenderer.send('save-user-info', userInfo)
  },
  closeMeetingWindow: (delay) => {
    return electronAPI.ipcRenderer.invoke('close-meeting-window', delay)
  },
  // WebRTC 信令
  sendMeetingSignal: (signalData) => {
    electronAPI.ipcRenderer.send('webrtc-signal', signalData)
  },
  // 服务器连接测试
  testServerConnection: (config) => {
    return electronAPI.ipcRenderer.invoke('test-server-connection', config)
  },
  // 通用 API 请求代理（生产环境经过主进程，绕过 CORS）
  apiRequest: (requestConfig) => {
    return electronAPI.ipcRenderer.invoke('api-request', requestConfig)
  },
  // 文件操作（通过主进程 → Node.js 处理）
  // 从 File 对象获取本地磁盘路径（Electron 32+ 替代已移除的 file.path）。
  // 仅对来自磁盘的 File（如 <input type=file> 选择、拖拽）有效；JS 构造的 File 返回空串。
  getPathForFile: (file) => {
    try { return webUtils.getPathForFile(file) } catch (_) { return '' }
  },
  // 软件设置（同步主进程 store + 开机自启等，确保每项设置真正生效）
  updateSettings: (settings) => electronAPI.ipcRenderer.invoke('settings-update', settings),
  getAppInfo: () => electronAPI.ipcRenderer.invoke('get-app-info'),
  openUserDataDir: () => electronAPI.ipcRenderer.invoke('open-user-data-dir'),
  uploadMeetingFile: (params) => {
    return electronAPI.ipcRenderer.invoke('file-upload', params)
  },
  downloadMeetingFile: (params) => {
    return electronAPI.ipcRenderer.invoke('file-download', params)
  },
  checkMeetingFile: (params) => {
    return electronAPI.ipcRenderer.invoke('file-check', params)
  },
  // 分片上传（完整流程：hash → check → chunk → merge）
  chunkUploadFile: (params) => {
    return electronAPI.ipcRenderer.invoke('file-chunk-upload', params)
  },
  // 取消分片上传
  cancelChunkUpload: (params) => {
    return electronAPI.ipcRenderer.invoke('file-chunk-cancel', params)
  },
  // 保存临时文件（大文件先存本地，再流式分片上传）
  saveTempFile: (params) => {
    return electronAPI.ipcRenderer.invoke('file-save-temp', params)
  },
  // 清理临时上传文件
  cleanupTempFile: (params) => {
    return electronAPI.ipcRenderer.invoke('file-cleanup-temp', params)
  },
  // 用系统默认程序打开本地文件
  openFile: (filePath) => {
    return electronAPI.ipcRenderer.invoke('file-open', filePath)
  },
  // 将已下载的文件另存为到用户指定位置（返回保存后的路径）
  saveFileAs: (params) => {
    return electronAPI.ipcRenderer.invoke('file-save-as', params)
  },
  // 头像操作（以 userId 为键，不同于文件消息的 fileId）
  uploadAvatar: (params) => {
    return electronAPI.ipcRenderer.invoke('avatar-upload', params)
  },
  checkAvatar: (params) => {
    return electronAPI.ipcRenderer.invoke('avatar-check', params)
  },
  downloadAvatar: (params) => {
    return electronAPI.ipcRenderer.invoke('avatar-download', params)
  },

  // ==================== 本地数据库操作 ====================

  // 初始化数据库
  dbInit: () => {
    return electronAPI.ipcRenderer.invoke('db-init')
  },

  // ChatSession
  dbSessionList: (userId) => {
    return electronAPI.ipcRenderer.invoke('db-session-list', userId)
  },
  dbSessionGet: (sessionId) => {
    return electronAPI.ipcRenderer.invoke('db-session-get', sessionId)
  },
  dbSessionGetByTarget: (userId, targetUserId) => {
    return electronAPI.ipcRenderer.invoke('db-session-get-by-target', userId, targetUserId)
  },
  dbSessionListByType: (userId, sessionType) => {
    return electronAPI.ipcRenderer.invoke('db-session-list-by-type', userId, sessionType)
  },
  dbSessionSave: (data) => {
    return electronAPI.ipcRenderer.invoke('db-session-save', data)
  },
  dbSessionBatchSave: (sessions) => {
    return electronAPI.ipcRenderer.invoke('db-session-batch-save', sessions)
  },
  dbSessionUpdateLastMessage: (sessionId, lastMessage, lastMessageTime) => {
    return electronAPI.ipcRenderer.invoke('db-session-update-last-message', sessionId, lastMessage, lastMessageTime)
  },
  dbSessionToggleTop: (sessionId, isTop) => {
    return electronAPI.ipcRenderer.invoke('db-session-toggle-top', sessionId, isTop)
  },
  dbSessionDelete: (sessionId) => {
    return electronAPI.ipcRenderer.invoke('db-session-delete', sessionId)
  },
  dbSessionHide: (sessionId) => {
    return electronAPI.ipcRenderer.invoke('db-session-hide', sessionId)
  },
  dbSessionRestoreByTarget: (targetUserId) => {
    return electronAPI.ipcRenderer.invoke('db-session-restore-by-target', targetUserId)
  },
  dbSessionDestroy: (sessionId) => {
    return electronAPI.ipcRenderer.invoke('db-session-destroy', sessionId)
  },
  dbSessionMarkActive: (sessionId, update) => {
    return electronAPI.ipcRenderer.invoke('db-session-mark-active', sessionId, update)
  },
  dbSessionResetUnread: (sessionId) => {
    return electronAPI.ipcRenderer.invoke('db-session-reset-unread', sessionId)
  },

  // ChatImMessage
  dbImMessageList: (sessionId, pageSize, beforeTime) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-list', sessionId, pageSize, beforeTime)
  },
  dbImMessageLatest: (sessionId) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-latest', sessionId)
  },
  dbImMessageSend: (data) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-send', data)
  },
  dbImMessageReceive: (data) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-receive', data)
  },
  dbImMessageSync: (messages) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-sync', messages)
  },
  dbImMessageUpdateStatus: (messageId, status) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-update-status', messageId, status)
  },
  dbImMessageListByType: (sessionId, messageType) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-list-by-type', sessionId, messageType)
  },
  dbImMessageCount: (sessionId) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-count', sessionId)
  },
  dbImMessageDeleteBySession: (sessionId) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-delete-by-session', sessionId)
  },
  dbImMessageDelete: (messageId) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-delete', messageId)
  },
  dbImMessageMarkRecalled: (messageId, extra) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-mark-recalled', messageId, extra)
  },
  dbImMessageMarkRecalledByOnlyId: (messageOnlyId, extra) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-mark-recalled-by-only-id', messageOnlyId, extra)
  },
  dbImMessageMarkRecalledByBackend: (backendMessageId, extra) => {
    return electronAPI.ipcRenderer.invoke('db-im-message-mark-recalled-by-backend', backendMessageId, extra)
  },

  // ChatMessage (会议聊天)
  dbMeetingMessageList: (meetingNo, pageSize, beforeTime) => {
    return electronAPI.ipcRenderer.invoke('db-meeting-message-list', meetingNo, pageSize, beforeTime)
  },
  dbMeetingMessageSend: (data) => {
    return electronAPI.ipcRenderer.invoke('db-meeting-message-send', data)
  },
  dbMeetingMessageReceive: (data) => {
    return electronAPI.ipcRenderer.invoke('db-meeting-message-receive', data)
  },
  dbMeetingMessageSync: (messages) => {
    return electronAPI.ipcRenderer.invoke('db-meeting-message-sync', messages)
  },
  dbMeetingMessageUpdateStatus: (messageId, status) => {
    return electronAPI.ipcRenderer.invoke('db-meeting-message-update-status', messageId, status)
  },
  dbMeetingMessageListByType: (meetingNo, messageType) => {
    return electronAPI.ipcRenderer.invoke('db-meeting-message-list-by-type', meetingNo, messageType)
  },
  dbMeetingMessageCount: (meetingNo) => {
    return electronAPI.ipcRenderer.invoke('db-meeting-message-count', meetingNo)
  },
  dbMeetingMessageDeleteByMeeting: (meetingNo) => {
    return electronAPI.ipcRenderer.invoke('db-meeting-message-delete-by-meeting', meetingNo)
  },

  // GroupInfo / GroupMember
  dbGroupInfoGet: (groupId) => {
    return electronAPI.ipcRenderer.invoke('db-group-info-get', groupId)
  },
  dbGroupInfoSave: (data) => {
    return electronAPI.ipcRenderer.invoke('db-group-info-save', data)
  },
  dbGroupInfoUpdate: (groupId, changes) => {
    return electronAPI.ipcRenderer.invoke('db-group-info-update', groupId, changes)
  },
  dbGroupDissolve: (groupId) => {
    return electronAPI.ipcRenderer.invoke('db-group-dissolve', groupId)
  },
  dbGroupIsDissolved: (groupId) => {
    return electronAPI.ipcRenderer.invoke('db-group-is-dissolved', groupId)
  },
  dbGroupMembersGet: (groupId) => {
    return electronAPI.ipcRenderer.invoke('db-group-members-get', groupId)
  },
  dbGroupMemberGet: (groupId, userId) => {
    return electronAPI.ipcRenderer.invoke('db-group-member-get', groupId, userId)
  },
  dbGroupMemberSave: (data) => {
    return electronAPI.ipcRenderer.invoke('db-group-member-save', data)
  },
  dbGroupMembersBatchSave: (members) => {
    return electronAPI.ipcRenderer.invoke('db-group-members-batch-save', members)
  },
  dbGroupMemberUpdateRole: (groupId, userId, role) => {
    return electronAPI.ipcRenderer.invoke('db-group-member-update-role', groupId, userId, role)
  },
  dbGroupMemberRemove: (groupId, userId) => {
    return electronAPI.ipcRenderer.invoke('db-group-member-remove', groupId, userId)
  },
  dbGroupMemberKick: (groupId, userId) => {
    return electronAPI.ipcRenderer.invoke('db-group-member-kick', groupId, userId)
  },
  dbGroupMyRole: (groupId, userId) => {
    return electronAPI.ipcRenderer.invoke('db-group-my-role', groupId, userId)
  },
  dbGroupSync: (groupData, members) => {
    return electronAPI.ipcRenderer.invoke('db-group-sync', groupData, members)
  }
}

const messageAPI = {
  onNewMessage: (callback) => {
    const handler = (_event, message) => {
      console.log('🔔 [Preload] 收到新消息事件:', JSON.stringify(message))
      callback(message)
    }
    electronAPI.ipcRenderer.on('new-message', handler)
    console.log('🔔 [Preload] 新消息监听器已注册')
    return () => {
      electronAPI.ipcRenderer.removeListener('new-message', handler)
      console.log('🔔 [Preload] 新消息监听器已移除')
    }
  },
  onFileReady: (callback) => {
    const handler = (_event, data) => {
      console.log('📁 [Preload] 文件就绪事件:', data.fileId)
      callback(data)
    }
    electronAPI.ipcRenderer.on('file-ready', handler)
    return () => {
      electronAPI.ipcRenderer.removeListener('file-ready', handler)
    }
  },
  onSessionEvent: (callback) => {
    const handler = (_event, data) => {
      console.log('🧭 [Preload] 会话系统事件:', data.sessionId)
      callback(data)
    }
    electronAPI.ipcRenderer.on('session-event', handler)
    return () => {
      electronAPI.ipcRenderer.removeListener('session-event', handler)
    }
  },
  // 分片上传进度监听
  onChunkUploadProgress: (callback) => {
    const handler = (_event, data) => {
      callback(data)
    }
    electronAPI.ipcRenderer.on('chunk-upload-progress', handler)
    return () => {
      electronAPI.ipcRenderer.removeListener('chunk-upload-progress', handler)
    }
  },
  // 提示音播放（主进程收到他人新消息且窗口未聚焦时触发，由渲染进程用 Web Audio 合成播放）
  onPlayNotificationSound: (callback) => {
    const handler = () => callback()
    electronAPI.ipcRenderer.on('play-notification-sound', handler)
    return () => {
      electronAPI.ipcRenderer.removeListener('play-notification-sound', handler)
    }
  },
  // 强制下线通知（管理员将用户踢下线时，主进程通知渲染进程登出）
  onForceOffline: (callback) => {
    const handler = (_event, data) => {
      console.log('🚪 [Preload] 收到强制下线事件:', data && data.reason)
      callback(data)
    }
    electronAPI.ipcRenderer.on('force-offline', handler)
    return () => {
      electronAPI.ipcRenderer.removeListener('force-offline', handler)
    }
  }
}

const customElectronAPI = {
  ...electronAPI,
  ipcRenderer: {
    ...electronAPI.ipcRenderer,
    invoke: (channel, ...args) => electronAPI.ipcRenderer.invoke(channel, ...args)
  }
}

if (process.contextIsolated) {
  try {
    contextBridge.exposeInMainWorld('electron', customElectronAPI)
    contextBridge.exposeInMainWorld('api', api)
    contextBridge.exposeInMainWorld('messageAPI', messageAPI)
  } catch (error) {
    console.error(error)
  }
} else {
  window.electron = customElectronAPI
  window.api = api
  window.messageAPI = messageAPI
}
