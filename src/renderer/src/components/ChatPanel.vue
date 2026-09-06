<template>
  <div class="chat-panel">
    <!-- 顶部联系人信息栏 -->
    <div class="chat-topbar" v-if="conversation">
      <span class="topbar-name">{{ conversation.userName }}</span>
    </div>

    <!-- 消息列表 -->
    <div class="chat-messages" ref="messagesContainer" @scroll="handleScroll" v-if="conversation">
      <div v-if="messages.length === 0" class="empty-chat">
        <p>开始聊天吧</p>
      </div>

      <div v-else class="message-list">
        <ChatMessageBubble
          v-for="(msg, index) in messages"
          :key="index"
          :msg="msg"
          :current-user-id="currentUserId"
          @preview-image="handlePreviewImage"
          @file-click="handleFileClick"
        />
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area" v-if="conversation">
      <!-- 表情面板 -->
      <EmojiPanel
        :visible="showEmojiPanel"
        @select="handleEmojiSelect"
        class="emoji-panel-wrap"
      />

      <!-- 多文件传输卡片列表（与会议室 MessageSend 一致） -->
      <div v-if="tasks.length > 0" class="transfer-list">
        <div
          v-for="task in tasks"
          :key="task.id"
          class="transfer-card"
          :class="cardClass(task)"
        >
          <div class="card-close" @click="handleRemoveTask(task)">
            <el-icon :size="12"><Close /></el-icon>
          </div>

          <div class="card-body">
            <div v-if="task.previewUrl" class="card-thumb">
              <img :src="task.previewUrl" alt="" />
            </div>
            <div v-else class="card-icon">
              <el-icon :size="24"><Document /></el-icon>
            </div>

            <div class="card-info">
              <div class="card-name" :title="task.fileName">{{ task.fileName }}</div>
              <div class="card-size">{{ formatSize(task.fileSize) }}</div>

              <template v-if="isUploading(task)">
                <el-progress
                  :percentage="task.progress"
                  :stroke-width="4"
                  :show-text="false"
                  color="#1a73e8"
                  class="card-progress"
                />
                <div class="card-status uploading">
                  <el-icon class="is-loading" :size="12"><Loading /></el-icon>
                  <span>{{ phaseText(task) }}</span>
                  <span v-if="task.totalChunks > 0" class="chunk-info">
                    {{ task.completedChunks }}/{{ task.totalChunks }}
                  </span>
                </div>
              </template>

              <div v-else-if="task.status === S.READY" class="card-status ready">
                <el-icon :size="12"><Check /></el-icon>
                <span>上传完成，等待发送</span>
              </div>

              <div v-else-if="task.status === S.SENDING" class="card-status sending">
                <el-icon class="is-loading" :size="12"><Loading /></el-icon>
                <span>发送中...</span>
              </div>

              <div v-else-if="task.status === S.SENT" class="card-status sent">
                <el-icon :size="12"><Check /></el-icon>
                <span>已发送</span>
              </div>

              <div v-else-if="task.status === S.FAILED" class="card-status failed">
                <el-icon :size="12"><WarningFilled /></el-icon>
                <span>{{ task.error || '发送失败' }}</span>
                <el-button type="primary" link size="small" @click.stop="retryTask(task)">重试</el-button>
              </div>

              <div v-else-if="task.status === S.CANCELLED" class="card-status cancelled">
                <span>已取消</span>
              </div>

              <div v-if="task.fastPass" class="fast-pass-tag">秒传</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 发送行（与 MessageSend 的 send-row 一致） -->
      <div class="send-row">
        <el-upload
          :show-file-list="false"
          :before-upload="handleFileSelect"
          :http-request="handleUploadRequest"
          accept="image/*,video/*,.pdf,.doc,.docx,.xls,.xlsx,.txt,.zip,.rar"
          multiple
        >
          <el-button circle size="small" class="icon-btn" title="发送文件">
            <el-icon><Upload /></el-icon>
          </el-button>
        </el-upload>

        <el-button circle size="small" class="icon-btn" title="表情" @click="toggleEmojiPanel">
          <el-icon><ChatDotRound /></el-icon>
        </el-button>

        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="2"
          :placeholder="hasUploading ? '文件上传中...' : '输入消息...'"
          @keyup.enter.exact="handleSend"
          resize="none"
          class="message-input"
        />

        <el-button
          type="primary"
          :disabled="(!inputMessage.trim() && !hasReadyToSend) || isSending"
          class="send-btn"
          @click="handleSend"
        >
          <el-icon style="margin-right:4px"><Promotion /></el-icon>
          {{ isSending ? '发送中' : '发送' }}
        </el-button>
      </div>
    </div>

    <!-- 图片/视频预览弹窗（与会议室 Chat.vue 一致） -->
    <el-dialog
      v-model="previewVisible"
      :show-close="true"
      :show-header="false"
      custom-class="media-preview-dialog"
      append-to-body
      fullscreen
    >
      <div class="preview-wrapper">
        <div class="preview-content" :style="{ transform: `scale(${previewScale}) rotate(${previewRotate}deg)` }">
          <img v-if="previewMediaType === 33" :src="previewSrc" alt="preview" class="preview-img" />
          <video v-else-if="previewMediaType === 34" :src="previewSrc" controls autoplay class="preview-video"></video>
        </div>
      </div>
      <div class="preview-toolbar">
        <el-button circle @click="zoomOut" :disabled="previewScale <= 0.5" class="tool-btn">
          <el-icon><ZoomOut /></el-icon>
        </el-button>
        <span class="scale-label">{{ Math.round(previewScale * 100) }}%</span>
        <el-button circle @click="zoomIn" :disabled="previewScale >= 3" class="tool-btn">
          <el-icon><ZoomIn /></el-icon>
        </el-button>
        <el-button circle @click="rotatePreview" class="tool-btn">
          <el-icon><RefreshRight /></el-icon>
        </el-button>
        <el-button circle @click="resetPreview" class="tool-btn">
          <el-icon><FullScreen /></el-icon>
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { Upload, ChatDotRound, Promotion, Document, Close, Loading, Check, WarningFilled, ZoomIn, ZoomOut, RefreshRight, FullScreen } from '@element-plus/icons-vue'
import ChatMessageBubble from '@/components/ChatMessageBubble.vue'
import EmojiPanel from '@/components/EmojiPanel.vue'
import Request from '@/utils/Request'
import Api from '@/utils/Api'
import { useChunkUploadManager } from '@/utils/useChunkUploadManager'

const getContactTypeByPrefix = (id) => {
  if (!id) return 0
  const prefix = String(id).charAt(0).toUpperCase()
  if (prefix === 'G') return 1
  if (prefix === 'R') return 2
  return 0
}

const props = defineProps({
  conversation: {
    type: Object,
    default: null
  }
})

const messages = ref([])
const inputMessage = ref('')
const isSending = ref(false)
const showEmojiPanel = ref(false)
const messagesContainer = ref(null)
const isLoading = ref(false)
const hasMore = ref(true)
const currentCursor = ref(null)
const PAGE_SIZE = 20

// 多文件并发上传管理器（与会议室共用）
const {
  tasks,
  hasUploading,
  hasReadyToSend,
  TASK_STATUS: S,
  addTask,
  cancelTask,
  removeTask,
  clearFinished,
  getReadyTasks,
  markSending,
  markSent,
  markSendFailed
} = useChunkUploadManager()

// ==================== 图片/视频预览 ====================
const previewVisible = ref(false)
const previewSrc = ref('')
const previewMediaType = ref(33)
const previewScale = ref(1)
const previewRotate = ref(0)

const zoomIn = () => { previewScale.value = Math.min(previewScale.value + 0.25, 3) }
const zoomOut = () => { previewScale.value = Math.max(previewScale.value - 0.25, 0.5) }
const rotatePreview = () => { previewRotate.value = (previewRotate.value + 90) % 360 }
const resetPreview = () => { previewScale.value = 1; previewRotate.value = 0 }

const currentUserId = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return info.userId || 'me'
  } catch (_) { return 'me' }
})

const getCurrentTime = () => Date.now()

const scrollToBottom = (behavior = 'auto') => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTo({ top: messagesContainer.value.scrollHeight, behavior })
    }
  })
}

const scrollToPosition = (scrollHeight) => {
  nextTick(() => {
    if (messagesContainer.value) {
      const newScrollHeight = messagesContainer.value.scrollHeight
      messagesContainer.value.scrollTop = newScrollHeight - scrollHeight
    }
  })
}

const toggleEmojiPanel = () => {
  showEmojiPanel.value = !showEmojiPanel.value
}

const handleEmojiSelect = (emoji) => {
  inputMessage.value += emoji
  showEmojiPanel.value = false
}

// ==================== 文件类型 & 上传 ====================

const getFileMessageType = (file) => {
  const type = file.type || ''
  const name = file.name || ''
  if (type.startsWith('image/') || /\.(jpg|jpeg|png|gif|bmp|webp)$/i.test(name)) return 33
  if (type.startsWith('video/') || /\.(mp4|avi|mov|wmv|flv|mkv|webm)$/i.test(name)) return 34
  if (type.startsWith('audio/') || /\.(mp3|wav|aac|ogg|flac)$/i.test(name)) return 35
  return 32
}

const formatSize = (bytes) => {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
}

const isUploading = (task) =>
  task.status === S.HASHING || task.status === S.UPLOADING || task.status === S.MERGING

const phaseText = (task) => {
  const map = { hashing: '计算指纹...', uploading: '上传中', merging: '合并中...' }
  return map[task.phase] || '准备中...'
}

const cardClass = (task) => ({
  'card-uploading': isUploading(task),
  'card-ready': task.status === S.READY,
  'card-sent': task.status === S.SENT,
  'card-failed': task.status === S.FAILED,
  'card-cancelled': task.status === S.CANCELLED
})

const handleFileSelect = () => true

const handleUploadRequest = async ({ file }) => {
  if (!props.conversation) return
  const rawFile = file.raw || file
  const messageType = getFileMessageType(rawFile)
  // sessionId 作为 meetingNo 透传（后端 meetingNo 字段可选）
  addTask(rawFile, props.conversation.sessionId, messageType)
}

const handleRemoveTask = (task) => {
  if (isUploading(task)) cancelTask(task)
  else removeTask(task)
}

const retryTask = (task) => {
  task.status = S.UPLOADING
  task.progress = 0
  task.error = null
  task.phase = ''
  const file = task.file
  if (file) {
    const messageType = task.messageType
    removeTask(task)
    addTask(file, task.meetingNo, messageType)
  }
}

// ==================== 预览 & 打开文件 ====================

const handlePreviewImage = (msg) => {
  // 优先 localPath（本地缓存），其次 HTTP URL
  let src = ''
  if (msg.localPath) src = 'file://' + msg.localPath
  else if (msg.thumbPath) src = 'file://' + msg.thumbPath
  else if (msg.filePath) {
    try {
      const config = JSON.parse(localStorage.getItem('serverConfig') || '{}')
      src = `http://${config.host}:${config.httpPort || 6060}/api${msg.filePath}`
    } catch (_) {}
  }
  if (!src) return
  previewSrc.value = src
  previewMediaType.value = msg.messageType || 33
  previewScale.value = 1
  previewRotate.value = 0
  previewVisible.value = true
}

const handleFileClick = async (msg) => {
  const fileId = msg.fileId
  const filePath = msg.filePath
  if (!fileId && !filePath) return
  try {
    if (window.api?.checkMeetingFile && window.api?.openFile) {
      const res = await window.api.checkMeetingFile({ fileId, filePath, fileType: msg.fileType || 5 })
      if (res?.exists && res.localPath) {
        await window.api.openFile(res.localPath)
        return
      }
      if (window.api?.downloadMeetingFile) {
        const dl = await window.api.downloadMeetingFile({ fileId, filePath, fileType: msg.fileType || 5 })
        if (dl?.success && dl.data?.localPath) {
          await window.api.openFile(dl.data.localPath)
          return
        }
      }
    }
    // 后端 HTTP 直链兜底
    if (filePath) {
      try {
        const config = JSON.parse(localStorage.getItem('serverConfig') || '{}')
        const url = `http://${config.host}:${config.httpPort || 6060}/api${filePath}`
        if (window.api?.openFile) {
          // HTTP URL 交给系统浏览器打开
          const { shell } = window.electron || {}
          if (shell?.openExternal) shell.openExternal(url)
        }
      } catch (_) {}
    }
  } catch (e) {
    console.error('[ChatPanel] 打开文件失败:', e.message)
  }
}

// ==================== 发送消息 ====================

const getCurrentUserNickName = () => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return info.nickName || info.userName || '我'
  } catch (_) { return '我' }
}

const buildImMessageParams = (content, messageType, fileData = {}) => ({
  sessionId: props.conversation.sessionId,
  messageContent: content,
  messageType,
  sendUserId: currentUserId.value,
  sendUserNickName: getCurrentUserNickName(),
  receiveUserId: props.conversation.userId,
  messageSend2Type: getContactTypeByPrefix(props.conversation.userId),
  messageTypeIm: 1,   // 1 = IM 消息
  sendTime: getCurrentTime(),
  status: 0,
  fileSize: fileData.fileSize || 0,
  fileName: fileData.fileName || '',
  fileId: fileData.fileId || '',
  filePath: fileData.filePath || '',
  fileType: fileData.fileType || 5,
  contentType: fileData.contentType || '',
  extendData: fileData.fileId ? JSON.stringify({
    fileId: fileData.fileId,
    fileName: fileData.fileName,
    filePath: fileData.filePath,
    fileSize: fileData.fileSize,
    fileType: fileData.fileType,
    contentType: fileData.contentType
  }) : ''
})

/** 发送所有已就绪的文件（多文件按顺序） */
const sendReadyFiles = async () => {
  const readyTasks = getReadyTasks()
  if (readyTasks.length === 0) return false

  isSending.value = true
  try {
    for (const task of readyTasks) {
      if (task.status !== S.READY) continue
      markSending(task)
      const messageOnlyId = `mid_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
      const params = buildImMessageParams(task.fileName, task.messageType, task.fileData)
      params.messageOnlyId = messageOnlyId

      // 1. 立即 push 到 UI（status=0 发送中，转圈）
      messages.value.push({
        ...params,
        senderName: params.sendUserNickName,
        localPath: task.previewUrl || null,
        status: 0
      })
      scrollToBottom()

      try {
        // 2. 调用后端 IM 消息发送接口
        const res = await Request({
          url: Api.sendImMessage,
          params,
          showLoading: false
        })
        if (!res) throw new Error('后端发送失败')
        // 3. 写入本地数据库（HTTP已成功，直接 status=1）
        params.status = 1
        await window.api.dbImMessageSend(params)
        markSent(task)
      } catch (e) {
        console.error('[ChatPanel] 文件消息发送失败:', e.message)
        markSendFailed(task)
        const idx = messages.value.findIndex(m => m.messageOnlyId === messageOnlyId)
        if (idx !== -1) messages.value[idx].status = 2
      }
    }
    return true
  } finally {
    isSending.value = false
    clearFinished()
  }
}

const handleSend = async () => {
  const content = inputMessage.value.trim()
  if (isSending.value || !props.conversation) return

  showEmojiPanel.value = false

  // 1. 有待发送的文件 → 先发文件
  if (hasReadyToSend.value) {
    await sendReadyFiles()
    // 如果还有文字，跟着发
    if (content) {
      inputMessage.value = ''
      await sendTextMessage(content)
    }
    return
  }

  // 2. 纯文字消息
  if (!content) return
  inputMessage.value = ''
  await sendTextMessage(content)
}

const sendTextMessage = async (content) => {
  const messageOnlyId = `mid_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
  const params = buildImMessageParams(content, 30)
  params.messageOnlyId = messageOnlyId

  isSending.value = true
  // 立即 push 到 UI（status=0 发送中，转圈）
  messages.value.push({
    ...params,
    senderName: params.sendUserNickName,
    status: 0
  })
  scrollToBottom()

  try {
    const res = await Request({
      url: Api.sendImMessage,
      params,
      showLoading: false
    })
    if (!res) throw new Error('后端发送失败')
    // 写入本地数据库（HTTP已成功，直接 status=1）
    params.status = 1
    await window.api.dbImMessageSend(params)
  } catch (e) {
    console.error('[ChatPanel] 发送消息失败:', e.message)
    // 发送失败：标记 status=2
    const idx = messages.value.findIndex(m => m.messageOnlyId === messageOnlyId)
    if (idx !== -1) messages.value[idx].status = 2
  } finally {
    isSending.value = false
  }
}

// ==================== 消息加载与监听 ====================

const loadMessages = async (reset = false) => {
  if (!props.conversation?.sessionId || isLoading.value) return
  if (!reset && !hasMore.value) return

  isLoading.value = true
  try {
    const beforeTime = reset ? null : currentCursor.value
    const list = await window.api.dbImMessageList(props.conversation.sessionId, PAGE_SIZE, beforeTime)

    const sorted = (list || []).reverse()

    if (reset) {
      messages.value = sorted
    } else {
      const oldScrollHeight = messagesContainer.value?.scrollHeight || 0
      messages.value = [...sorted, ...messages.value]
      scrollToPosition(oldScrollHeight)
    }

    if (sorted.length > 0) {
      currentCursor.value = sorted[0].sendTime
      hasMore.value = sorted.length === PAGE_SIZE
    } else {
      hasMore.value = false
    }

    if (reset) {
      scrollToBottom()
    }
    // 加载完成后检查文件消息的本地缓存
    checkAllFileMessages()
  } catch (e) {
    console.error('[ChatPanel] 加载消息失败:', e.message)
  } finally {
    isLoading.value = false
  }
}

/**
 * 批量检查所有文件消息的本地缓存
 * - 图片/视频：自动显示本地缩略图
 * - 其他文件：标记"已下载"，点击直接用系统程序打开
 */
const checkAllFileMessages = async () => {
  if (!window.api?.checkMeetingFile) return

  const fileMessages = messages.value.filter(m => {
    const mt = m.messageType
    return mt === 32 || mt === 33 || mt === 34 || mt === 35
  })
  if (fileMessages.length === 0) return

  console.log(`[ChatPanel] 批量检查 ${fileMessages.length} 个文件消息的本地缓存`)

  for (const msg of fileMessages) {
    try {
      if (msg.localPath) continue
      if (!msg.fileId && !msg.filePath) continue

      const result = await window.api.checkMeetingFile({
        fileId: msg.fileId,
        filePath: msg.filePath,
        fileType: msg.fileType || 5
      })
      if (result?.exists && result.localPath) {
        const idx = messages.value.indexOf(msg)
        if (idx !== -1) {
          messages.value[idx] = {
            ...messages.value[idx],
            localPath: result.localPath,
            thumbPath: result.thumbPath || null,
            _fileStatus: 'ready'
          }
        }
      }
    } catch (e) {
      console.warn('[ChatPanel] 检查文件消息失败:', e.message)
    }
  }
}

/**
 * 单条文件消息快速检查本地缓存（新消息接收时）
 */
const quickCheckFileMessage = (msg) => {
  if (!window.api?.checkMeetingFile) return
  if (!msg.fileId && !msg.filePath) return

  window.api.checkMeetingFile({
    fileId: msg.fileId,
    filePath: msg.filePath,
    fileType: msg.fileType || 5
  }).then(result => {
    if (result?.exists && result.localPath) {
      const idx = messages.value.findIndex(m =>
        m.messageOnlyId
          ? m.messageOnlyId === msg.messageOnlyId
          : (m.sendTime === msg.sendTime && m.messageType === msg.messageType)
      )
      if (idx !== -1 && !messages.value[idx].localPath) {
        messages.value[idx] = {
          ...messages.value[idx],
          localPath: result.localPath,
          thumbPath: result.thumbPath || null,
          _fileStatus: 'ready'
        }
      }
    }
  }).catch(() => {})
}

const handleScroll = () => {
  if (!messagesContainer.value || isLoading.value || !hasMore.value) return
  if (messagesContainer.value.scrollTop <= 50) {
    loadMessages(false)
  }
}

let removeMessageListener = null
const setupMessageListener = () => {
  if (window.messageAPI?.onNewMessage) {
    removeMessageListener = window.messageAPI.onNewMessage((msg) => {
      if (msg.messageTypeIm !== 0 || !msg.sessionId) return

      if (window.api?.dbSessionMarkActive) {
        window.api.dbSessionMarkActive(msg.sessionId, {
          lastMessage: msg.messageContent,
          lastMessageTime: msg.sendTime || Date.now()
        })
      }

      // 当前会话的新消息 → 清零未读
      if (msg.sessionId === props.conversation?.sessionId) {
        if (window.api?.dbSessionResetUnread) {
          window.api.dbSessionResetUnread(msg.sessionId)
        }
      }

      if (msg.sessionId !== props.conversation?.sessionId) return

      if (msg.messageOnlyId) {
        const existIndex = messages.value.findIndex(
          (m) => m.messageOnlyId === msg.messageOnlyId
        )
        if (existIndex !== -1) {
          messages.value[existIndex].status = msg.status ?? 1
          messages.value[existIndex].messageId = msg.messageId || messages.value[existIndex].messageId
          messages.value[existIndex].sendTime = msg.sendTime || messages.value[existIndex].sendTime
          // 文件字段也可能带过来
          if (msg.fileId) messages.value[existIndex].fileId = msg.fileId
          if (msg.filePath) messages.value[existIndex].filePath = msg.filePath
          return
        }
      }

      messages.value.push({
        ...msg,
        senderName: msg.sendUserNickName || '',
        messageSend2Type: msg.messageSend2Type ?? 0
      })
      scrollToBottom('smooth')

      // 新文件消息快速检查本地缓存
      const mt = msg.messageType
      if ((mt === 32 || mt === 33 || mt === 34 || mt === 35) && !msg.localPath) {
        quickCheckFileMessage(msg)
      }
    })
  }
}

watch(() => props.conversation?.sessionId, (newSessionId, oldSessionId) => {
  if (newSessionId !== oldSessionId) {
    messages.value = []
    currentCursor.value = null
    hasMore.value = true
    if (props.conversation) {
      loadMessages(true)
    }
  }
})

onMounted(() => {
  setupMessageListener()
  if (props.conversation) {
    loadMessages(true)
  }
})

onUnmounted(() => {
  if (removeMessageListener) {
    removeMessageListener()
    removeMessageListener = null
  }
})

defineExpose({
  messages,
  clearMessages: () => { messages.value = [] }
})
</script>

<style lang="css" scoped>
.chat-panel {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

/* 顶部信息栏 */
.chat-topbar {
  padding: 14px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
}

.topbar-name {
  font-size: 15px;
  font-weight: 600;
  color: #202124;
}

/* 消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px 16px 8px;
  background: #f8f9fa;
}

.chat-messages::-webkit-scrollbar {
  width: 5px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background-color: #dadce0;
  border-radius: 3px;
}

.empty-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.empty-chat p {
  font-size: 13px;
  color: #9aa0a6;
  margin: 0;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

/* 表情面板 */
.emoji-panel-wrap {
  position: absolute;
  bottom: 100%;
  left: 16px;
  margin-bottom: 4px;
  z-index: 10;
}

/* 输入区域 */
.chat-input-area {
  position: relative;
  padding: 12px 16px 24px 16px;
  margin-top: 12px;
  border-top: 1px solid #e8eaed;
  background: #f8f9fa;
  border-radius: 0 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* ==================== 传输卡片列表（与 MessageSend 一致）==================== */
.transfer-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 2px;
}
.transfer-list::-webkit-scrollbar { width: 4px; }
.transfer-list::-webkit-scrollbar-thumb { background-color: #dadce0; border-radius: 2px; }

.transfer-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 28px 8px 10px;
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: border-color 0.2s;
}
.transfer-card.card-uploading { border-color: #1a73e8; }
.transfer-card.card-ready { border-color: #67c23a; }
.transfer-card.card-sent { border-color: #dadce0; opacity: 0.7; }
.transfer-card.card-failed { border-color: #f56c6c; }
.transfer-card.card-cancelled { border-color: #dadce0; opacity: 0.5; }

.transfer-card .card-close {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  color: #9aa0a6;
  transition: all 0.15s;
}
.transfer-card .card-close:hover { background: #fee2e2; color: #d93025; }

.transfer-card .card-body {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-width: 0;
}
.transfer-card .card-thumb {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  background: #f1f3f4;
}
.transfer-card .card-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.transfer-card .card-icon {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 6px;
  background: #e8f0fe;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1a73e8;
}
.transfer-card .card-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.transfer-card .card-name {
  font-size: 13px;
  font-weight: 500;
  color: #202124;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.transfer-card .card-size { font-size: 11px; color: #9aa0a6; }
.transfer-card .card-progress { margin-top: 2px; }
.transfer-card .card-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  margin-top: 1px;
}
.transfer-card .card-status.uploading { color: #1a73e8; }
.transfer-card .card-status.ready { color: #67c23a; }
.transfer-card .card-status.sending { color: #1a73e8; }
.transfer-card .card-status.sent { color: #67c23a; }
.transfer-card .card-status.failed { color: #f56c6c; }
.transfer-card .card-status.cancelled { color: #9aa0a6; }
.transfer-card .card-status .chunk-info { color: #9aa0a6; margin-left: 4px; }
.transfer-card .fast-pass-tag {
  display: inline-block;
  padding: 0 5px;
  font-size: 10px;
  color: #67c23a;
  background: #f0f9eb;
  border-radius: 3px;
  font-weight: 500;
  width: fit-content;
}

/* 发送行（与 MessageSend 的 send-row 一致） */
.send-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.icon-btn {
  width: 32px; height: 32px; flex-shrink: 0;
  border: 1px solid #e8eaed; background: #ffffff; color: #5f6368; border-radius: 8px;
  transition: all 0.2s ease;

  &:hover { background: #f1f3f4; color: #1a73e8; border-color: #1a73e8; }
}

.message-input {
  flex: 1;
  :deep(.el-textarea__inner) {
    border-radius: 8px; font-size: 13px; padding: 8px 12px; resize: none;
    line-height: 1.5; min-height: 36px; max-height: 72px;
    border: 1px solid #e8eaed; background: #ffffff;
    &:focus { box-shadow: 0 0 0 2px rgba(26, 115, 232, 0.15); border-color: #1a73e8; }
  }
  :deep(.el-textarea__inner::-webkit-scrollbar) { width: 4px; }
  :deep(.el-textarea__inner::-webkit-scrollbar-thumb) { background-color: #dadce0; border-radius: 2px; }
}

.send-btn {
  height: 36px; padding: 0 18px; border-radius: 8px;
  background: linear-gradient(135deg, #1a73e8 0%, #1557b0 100%); border: none;
  font-size: 13px; font-weight: 500; flex-shrink: 0; color: #ffffff;
  display: flex; align-items: center;
  &:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(26, 115, 232, 0.25); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

/* ==================== 媒体预览弹窗 ==================== */
.preview-wrapper {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #1a1a1a;
  overflow: hidden;
}
.preview-content {
  transition: transform 0.15s ease-out;
  display: flex;
  justify-content: center;
  align-items: center;
}
.preview-img {
  max-width: 90vw;
  max-height: 82vh;
  object-fit: contain;
  user-select: none;
}
.preview-video {
  max-width: 92vw;
  max-height: 82vh;
  background: #000;
}
.preview-toolbar {
  position: fixed;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 28px;
  backdrop-filter: blur(8px);
  z-index: 10;
}
.preview-toolbar .tool-btn {
  width: 38px;
  height: 38px;
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border: none;
  &:hover { background: rgba(255, 255, 255, 0.2); }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}
.preview-toolbar .scale-label {
  color: #fff;
  font-size: 13px;
  min-width: 44px;
  text-align: center;
}
</style>

<style>
.media-preview-dialog .el-dialog {
  margin: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  max-width: none !important;
  background: #1a1a1a;
  border-radius: 0;
}
.media-preview-dialog .el-dialog__body {
  padding: 0;
  height: 100vh;
  overflow: hidden;
}
.media-preview-dialog .el-dialog__header {
  display: none;
}
</style>
