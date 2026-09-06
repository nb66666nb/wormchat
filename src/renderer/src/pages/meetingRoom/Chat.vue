<template>
  <div class="chat-container">
    <div class="chat-header">
      <h3>聊天</h3>
      <span class="message-count" v-if="messages.length > 0">{{ messages.length }}</span>
    </div>

    <div class="chat-body" ref="messageContainerRef">
      <div v-if="loadingMore" class="loading-more">加载中...</div>
      <div v-if="messages.length === 0" class="empty-state">
        <el-empty description="暂无消息" :image-size="70" />
      </div>

      <transition-group name="message-fade" tag="div" class="message-list">
        <div
          v-for="(msg, index) in messages"
          :key="index"
          :class="['message-item', getMessageClass(msg)]"
        >
          <div v-if="isSystemMessage(msg)" class="system-message">
            <div class="system-content">
              <el-icon><InfoFilled /></el-icon>
              <span class="system-text">{{ formatSystemMessage(msg) }}</span>
            </div>
            <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
          </div>

          <ChatMessageBubble
            v-else
            :msg="msg"
            :current-user-id="myUserId"
            :participants="participants"
            @preview-image="previewImage"
            @file-click="handleFileClick"
          />
        </div>
      </transition-group>
    </div>

    <MessageSend
      :participants="participants"
      :meetingId="meetingId"
      @message-sent="handleMessageSent"
      @file-transfer="handleFileTransfer"
    />

    <!-- 图片/视频预览 -->
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

    <!-- 文件右键菜单 -->
    <div
      v-if="fileContextMenuVisible && fileContextMenuMsg"
      class="file-context-menu"
      :style="{ left: fileContextMenuX + 'px', top: fileContextMenuY + 'px' }"
      @click.stop
    >
      <div
        v-if="fileContextMenuMsg.localPath"
        class="context-menu-item"
        @click="handleOpenFile(fileContextMenuMsg)"
      >
        <el-icon><Document /></el-icon>
        <span>打开文件</span>
      </div>
      <div
        v-if="fileContextMenuMsg.localPath"
        class="context-menu-item"
        @click="handleSaveFileAs(fileContextMenuMsg)"
      >
        <el-icon><FolderOpened /></el-icon>
        <span>另存为...</span>
      </div>
      <div
        v-if="!fileContextMenuMsg.localPath"
        class="context-menu-item disabled"
      >
        <el-icon><Download /></el-icon>
        <span>文件未下载（点击消息区域下载）</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { InfoFilled, Document, Download, PictureFilled, VideoPlay, Headset, CircleCheck, ZoomIn, ZoomOut, RefreshRight, FullScreen, FolderOpened } from '@element-plus/icons-vue'
import MessageSend from './MessageSend.vue'
import ChatMessageBubble from '@/components/ChatMessageBubble.vue'
import UserInfoStore from '@/stores/UserInfoStore'

const props = defineProps({
  participants: { type: Array, default: () => [] },
  meetingId: { type: String, default: '' },
  visible: { type: Boolean, default: false }
})

const emit = defineEmits(['update-message-count'])

const myUserId = UserInfoStore.getUserId()

const messages = ref([])
const messageContainerRef = ref(null)
const messageListenerCount = ref(0)
const lastMessageTime = ref(0)

// 分页加载状态
const PAGE_SIZE = 50
const loadingMore = ref(false)
const hasMoreMessages = ref(true)

// 图片/视频预览
const previewVisible = ref(false)
const previewSrc = ref('')
const previewMediaType = ref(33)
const previewScale = ref(1)
const previewRotate = ref(0)

// 文件右键菜单
const fileContextMenuVisible = ref(false)
const fileContextMenuX = ref(0)
const fileContextMenuY = ref(0)
const fileContextMenuMsg = ref(null)

const zoomIn = () => { previewScale.value = Math.min(previewScale.value + 0.25, 3) }
const zoomOut = () => { previewScale.value = Math.max(previewScale.value - 0.25, 0.5) }
const rotatePreview = () => { previewRotate.value = (previewRotate.value + 90) % 360 }
const resetPreview = () => { previewScale.value = 1; previewRotate.value = 0 }

const openMediaPreview = (path, type) => {
  if (!path) return
  previewSrc.value = 'file://' + path
  previewMediaType.value = type
  previewScale.value = 1
  previewRotate.value = 0
  previewVisible.value = true
}

const isSystemMessage = (msg) => {
  if (msg.messageType >= 20 && msg.messageType <= 23) return false
  if (msg.messageType >= 31 && msg.messageType <= 35) return false
  if (msg.messageType === 30) return false
  return true
}

const isMyMessage = (msg) => (msg.senderId || msg.sendUserId) === myUserId

const getMessageClass = (msg) => {
  if (isSystemMessage(msg)) return 'system'
  return isMyMessage(msg) ? 'mine' : 'other'
}

const formatSystemMessage = (msg) => {
  const name = msg.senderName || msg.sendUserNickName || '系统'
  const map = { 1: `${name} 创建了通话`, 10: `${name} 加入了通话`, 11: `${name} 离开了通话`, 12: '通话已结束' }
  return map[msg.messageType] || '系统通知'
}

const formatTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  return `${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

const scrollToBottom = () => {
  nextTick(() => { if (messageContainerRef.value) messageContainerRef.value.scrollTop = messageContainerRef.value.scrollHeight })
}

const sortMessagesByTime = (list) =>
  [...list].sort((a, b) => (a.timestamp || 0) - (b.timestamp || 0))

const addMessageWithOrder = (newMessage) => {
  newMessage.timestamp = newMessage.timestamp || Date.now()
  messages.value.push(newMessage)
  messages.value = sortMessagesByTime(messages.value)
  lastMessageTime.value = Math.max(lastMessageTime.value, newMessage.timestamp)
}

/**
 * 从数据库加载会议消息（首次加载最新 PAGE_SIZE 条）
 */
const loadMessagesFromDb = async () => {
  if (!window.api || !window.api.dbMeetingMessageList) return
  if (!props.meetingId) return
  try {
    const dbMessages = await window.api.dbMeetingMessageList(props.meetingId, PAGE_SIZE)
    if (dbMessages && Array.isArray(dbMessages)) {
      const filtered = dbMessages.filter(m => {
        const mt = m.messageType
        return !(mt >= 20 && mt <= 23)
      })
      messages.value = sortMessagesByTime(filtered.map(m => ({ ...m, timestamp: m.sendTime || m.timestamp || Date.now() })))
      hasMoreMessages.value = dbMessages.length >= PAGE_SIZE
      if (filtered.length > 0) lastMessageTime.value = Math.max(...filtered.map(m => m.sendTime || m.timestamp || 0))
      nextTick(scrollToBottom)
      checkAllFileMessages()
    }
  } catch (e) {
    console.error('[Chat] 加载消息失败:', e.message)
  }
}

/**
 * 上拉加载更多历史消息
 */
const loadMoreMessages = async () => {
  if (loadingMore.value || !hasMoreMessages.value) return
  if (!window.api || !window.api.dbMeetingMessageList) return
  if (!props.meetingId) return

  loadingMore.value = true
  try {
    // 获取当前最早消息的 sendTime 作为分页游标
    const earliestTime = messages.value.length > 0
      ? Math.min(...messages.value.map(m => m.sendTime || m.timestamp || Date.now()))
      : null

    const olderMessages = await window.api.dbMeetingMessageList(props.meetingId, PAGE_SIZE, earliestTime)
    if (olderMessages && Array.isArray(olderMessages) && olderMessages.length > 0) {
      const filtered = olderMessages.filter(m => {
        const mt = m.messageType
        return !(mt >= 20 && mt <= 23)
      })
      if (filtered.length < PAGE_SIZE) hasMoreMessages.value = false

      // 记住当前滚动位置
      const container = messageContainerRef.value
      const prevScrollHeight = container ? container.scrollHeight : 0

      // prepend 到数组头部
      messages.value = sortMessagesByTime([
        ...filtered.map(m => ({ ...m, timestamp: m.sendTime || m.timestamp || Date.now() })),
        ...messages.value
      ])

      // 保持滚动位置不变（不跳到顶部）
      nextTick(() => {
        if (container) {
          const newScrollHeight = container.scrollHeight
          container.scrollTop = newScrollHeight - prevScrollHeight
        }
      })
    } else {
      hasMoreMessages.value = false
    }
  } catch (e) {
    console.error('[Chat] 加载更多消息失败:', e.message)
  } finally {
    loadingMore.value = false
  }
}

/**
 * 监听滚动到顶部时自动加载更多
 */
const handleScroll = () => {
  const container = messageContainerRef.value
  if (!container) return
  if (container.scrollTop <= 50) {
    loadMoreMessages()
  }
}

// ==================== 自动检查本地文件缓存 ====================

/**
 * 检查所有文件消息是否已在本地存储
 * - 图片/视频：自动显示缩略图，无需用户点击
 * - 其他文件：标记为"已下载"，点击直接用系统程序打开
 */
const checkAllFileMessages = async () => {
  if (!window.api || !window.api.checkMeetingFile) return

  const fileMessages = messages.value.filter(m => {
    const mt = m.messageType
    return mt === 32 || mt === 33 || mt === 34 || mt === 35
  })
  if (fileMessages.length === 0) return

  console.log(`🔍 [Chat] 批量检查 ${fileMessages.length} 个文件消息的本地缓存`)

  for (const msg of fileMessages) {
    try {
      const info = getFileInfo(msg)
      if (!info.fileId && !info.filePath) continue
      if (msg.localPath) continue

      const result = await window.api.checkMeetingFile({
        fileId: info.fileId,
        filePath: info.filePath,
        fileType: info.fileType
      })
      if (result && result.exists && result.localPath) {
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
      console.warn('检查文件消息失败:', e.message)
    }
  }
}

const handleNewMessage = (message) => {
  const mt = message.messageType
  if (mt >= 20 && mt <= 23) return
  messageListenerCount.value++
  addMessageWithOrder({ ...message, timestamp: message.timestamp || Date.now() })
  scrollToBottom()
  emit('update-message-count', messageListenerCount.value)

  // 新文件消息也快速检查本地缓存（主进程可能还在异步下载，但本地已有则立即显示）
  if ((mt === 32 || mt === 33 || mt === 34 || mt === 35) && !message.localPath && window.api && window.api.checkMeetingFile) {
    const info = getFileInfo(message)
    if (info.fileId || info.filePath) {
      window.api.checkMeetingFile({
        fileId: info.fileId, filePath: info.filePath, fileType: info.fileType
      }).then(result => {
        if (result && result.exists && result.localPath) {
          const idx = messages.value.findIndex(m =>
            (m.timestamp === message.timestamp) && (m.messageType === message.messageType)
          )
          if (idx !== -1) {
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
  }
}

const handleClearMessages = () => { messages.value = []; lastMessageTime.value = 0; messageListenerCount.value = 0; hasMoreMessages.value = true }

const handleMessageSent = () => {}
const handleFileTransfer = () => {}

// ==================== 文件处理 ====================

/**
 * 从消息中获取文件信息（MessageSendDto 平铺字段）
 */
const getFileInfo = (msg) => {
  if (msg.fileId || msg.filePath || msg.fileName) {
    return {
      fileId: msg.fileId || '',
      fileName: msg.fileName || msg.messageContent || '未知文件',
      fileSize: msg.fileSize || 0,
      fileType: msg.fileType || 5,
      filePath: msg.filePath || '',
      contentType: msg.contentType || ''
    }
  }
  try {
    const raw = msg.messageContent || msg.content || '{}'
    const data = typeof raw === 'string' ? JSON.parse(raw) : raw
    return {
      fileId: data.fileId || '',
      fileName: data.fileName || '未知文件',
      fileSize: data.fileSize || 0,
      fileType: data.fileType || 5,
      filePath: data.filePath || '',
      contentType: data.contentType || ''
    }
  } catch (_) {
    return { fileId: '', fileName: '未知文件', fileSize: 0, fileType: 5, filePath: '', contentType: '' }
  }
}

const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const previewImage = (msg) => {
  if (msg.localPath) openMediaPreview(msg.localPath, 33)
}

// ==================== 文件右键菜单 ====================

const showFileContextMenu = (event, msg) => {
  fileContextMenuX.value = event.clientX
  fileContextMenuY.value = event.clientY
  fileContextMenuMsg.value = msg
  fileContextMenuVisible.value = true
}

const hideFileContextMenu = () => {
  fileContextMenuVisible.value = false
  fileContextMenuMsg.value = null
}

// 统一的文件打开逻辑：图片/视频→预览窗口，其他→系统默认程序
const handleOpenFile = async (msg) => {
  if (!msg) return
  const info = getFileInfo(msg)
  let localPath = msg.localPath
  if (!localPath && window.api && window.api.checkMeetingFile) {
    try {
      const result = await window.api.checkMeetingFile({
        fileId: info.fileId, filePath: info.filePath, fileType: info.fileType
      })
      if (result && result.exists && result.localPath) {
        localPath = result.localPath
      }
    } catch (_) {}
  }
  if (!localPath) return
  hideFileContextMenu()
  const mt = msg.messageType
  if (mt === 33) { openMediaPreview(localPath, 33); return }
  if (mt === 34) { openMediaPreview(localPath, 34); return }
  if (window.api && window.api.openFile) {
    try {
      await window.api.openFile(localPath)
    } catch (_) {}
  }
}

// 另存为：将已下载的文件复制到用户指定位置
const handleSaveFileAs = async (msg) => {
  if (!msg || !msg.localPath) return
  if (!window.api || !window.api.saveFileAs) return
  hideFileContextMenu()
  try {
    const info = getFileInfo(msg)
    const result = await window.api.saveFileAs({
      sourcePath: msg.localPath,
      fileName: info.fileName
    })
    if (result && result.success) {
      console.log('✅ 文件已保存到:', result.savedPath)
    }
  } catch (e) {
    console.warn('另存为失败:', e.message)
  }
}

const handleFileClick = async (msg) => {
  if (!msg || msg._fileStatus === 'downloading') return
  const info = getFileInfo(msg)
  if (!info.fileId && !info.filePath) return

  // 已下载 → 走统一的打开逻辑
  if (msg.localPath) {
    await handleOpenFile(msg)
    return
  }

  // 无 localPath → 先通过 IPC 快速检查本地是否有缓存
  if (window.api && window.api.checkMeetingFile) {
    try {
      const result = await window.api.checkMeetingFile({
        fileId: info.fileId, filePath: info.filePath, fileType: info.fileType
      })
      if (result && result.exists && result.localPath) {
        const idx = messages.value.indexOf(msg)
        if (idx !== -1) {
          messages.value[idx] = {
            ...messages.value[idx],
            localPath: result.localPath,
            thumbPath: result.thumbPath || null,
            _fileStatus: 'ready'
          }
        }
        await handleFileClick({ ...msg, localPath: result.localPath, thumbPath: result.thumbPath })
        return
      }
    } catch (_) {}
  }

  // 未下载 → 先下载
  const idx = messages.value.findIndex(m => {
    const mInfo = getFileInfo(m)
    if (info.fileId && mInfo.fileId === info.fileId) return true
    if (m.timestamp && msg.timestamp && m.timestamp === msg.timestamp) {
      if ((m.content && msg.content && m.content === msg.content) ||
          (m.messageContent && msg.messageContent && m.messageContent === msg.messageContent)) return true
    }
    return false
  })
  if (idx !== -1) {
    messages.value[idx] = { ...messages.value[idx], _fileStatus: 'downloading' }
  }
  try {
    const result = await window.api.downloadMeetingFile({
      fileId: info.fileId,
      filePath: info.filePath,
      fileType: info.fileType
    })
    if (result && result.success) {
      if (idx !== -1) {
        messages.value[idx] = {
          ...messages.value[idx],
          _fileStatus: 'ready',
          localPath: result.localPath || result.data?.localPath || '',
          thumbPath: result.thumbPath || result.data?.thumbPath || ''
        }
      }
    }
  } catch (e) {
    if (idx !== -1) {
      messages.value[idx] = { ...messages.value[idx], _fileStatus: 'error', _error: e.message }
    }
  }
}

// ==================== 文件就绪监听（主进程异步处理完成后触发）====================

/**
 * 主进程完成文件下载+缩略图后，通过 file-ready IPC 通知 renderer 更新消息
 */
const handleFileReady = (data) => {
  if (!data) return
  const info = getFileInfo(data)
  if (!info.fileId && !info.filePath) return
  const idx = messages.value.findIndex(m => {
    const mInfo = getFileInfo(m)
    if (info.fileId && mInfo.fileId === info.fileId) return true
    if (m.timestamp && data.timestamp && m.timestamp === data.timestamp) {
      if ((m.content && data.content && m.content === data.content) ||
          (m.messageContent && data.messageContent && m.messageContent === data.messageContent)) return true
    }
    return false
  })
  if (idx !== -1) {
    messages.value[idx] = {
      ...messages.value[idx],
      _fileStatus: data._fileStatus,
      localPath: data.localPath,
      thumbPath: data.thumbPath,
      _error: data._error
    }
  }
}

let unsubscribeNewMessage = null
let unsubscribeFileReady = null

watch(messages, scrollToBottom, { deep: true })

watch(() => props.visible, async (val) => {
  if (val) await loadMessagesFromDb()
  else { messages.value = []; lastMessageTime.value = 0; hasMoreMessages.value = true }
}, { immediate: true })

onMounted(() => {
  if (window.messageAPI) {
    unsubscribeNewMessage = window.messageAPI.onNewMessage(handleNewMessage)
    unsubscribeFileReady = window.messageAPI.onFileReady(handleFileReady)
  }
  document.addEventListener('click', hideFileContextMenu)
  // 监听滚动，上拉加载更多
  if (messageContainerRef.value) {
    messageContainerRef.value.addEventListener('scroll', handleScroll)
  }
})

onUnmounted(() => {
  if (unsubscribeNewMessage) unsubscribeNewMessage()
  if (unsubscribeFileReady) unsubscribeFileReady()
  document.removeEventListener('click', hideFileContextMenu)
  if (messageContainerRef.value) {
    messageContainerRef.value.removeEventListener('scroll', handleScroll)
  }
})
</script>

<style lang="scss" scoped>
.chat-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
  overflow: hidden;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;

  h3 {
    margin: 0;
    font-size: 15px;
    font-weight: 600;
    color: #202124;
  }

  .message-count {
    font-size: 12px;
    color: #5f6368;
    background: #f1f3f4;
    padding: 3px 10px;
    border-radius: 12px;
    font-weight: 500;
  }
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 18px 80px 18px;
  background: #f8f9fa;
  min-width: 0;
  box-sizing: border-box;

  &::-webkit-scrollbar { width: 5px; }
  &::-webkit-scrollbar-thumb { background-color: #dadce0; border-radius: 3px; }
  &::-webkit-scrollbar-track { background-color: transparent; }
}

.empty-state {
  display: flex; justify-content: center; align-items: center; height: 100%;
}

.loading-more {
  text-align: center; padding: 10px 0; color: #9aa0a6; font-size: 13px;
}

.message-list {
  display: flex; flex-direction: column; gap: 12px;
  width: 100%; min-width: 0; box-sizing: border-box;
}

.message-item {
  display: flex; width: 100%; min-width: 0; box-sizing: border-box;
  &.system { justify-content: center; }
  &:has(.is-mine) { justify-content: flex-end; }
}

/* 系统消息 */
.message-item.system {
  .system-message {
    display: flex; flex-direction: column; align-items: center; max-width: 75%;
    .system-content {
      display: flex; align-items: center; gap: 6px;
      background: #ffffff; padding: 7px 14px; border-radius: 16px;
      border: 1px solid #e8eaed; box-shadow: 0 1px 4px rgba(0,0,0,0.04);
      .el-icon { color: #1a73e8; font-size: 12px; }
      .system-text { color: #5f6368; font-size: 12px; font-weight: 500; line-height: 1.4; }
    }
    .message-time { font-size: 11px; color: #9aa0a6; margin-top: 5px; }
  }
}

.message-fade-enter-active, .message-fade-leave-active { transition: all 0.25s ease; }
.message-fade-enter-from, .message-fade-leave-to { opacity: 0; transform: translateX(-16px); }

/* ============ 图片/视频预览窗口 ============ */
:deep(.media-preview-dialog) {
  .el-dialog {
    margin: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    max-width: none !important;
    background: #1a1a1a;
    border-radius: 0;
    box-shadow: none;
  }
  .el-dialog__body {
    padding: 0;
    height: 100vh;
    position: relative;
    overflow: hidden;
  }
  .el-dialog__header {
    display: none;
  }
  .el-dialog__headerbtn {
    position: fixed;
    top: 20px;
    right: 24px;
    z-index: 9999;
    font-size: 28px;
    background: rgba(255, 255, 255, 0.15);
    width: 40px;
    height: 40px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    .el-dialog__close {
      color: #fff;
      font-size: 22px;
    }
    &:hover { background: rgba(255, 255, 255, 0.25); }
  }
}

.preview-wrapper {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #1a1a1a;
  overflow: auto;
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
  display: block;
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
  align-items: center;
  gap: 12px;
  padding: 10px 18px;
  background: rgba(0, 0, 0, 0.55);
  border-radius: 30px;
  backdrop-filter: blur(8px);
  z-index: 1000;
}

.preview-toolbar .tool-btn {
  width: 38px;
  height: 38px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  &:hover:not(:disabled) { background: rgba(255, 255, 255, 0.2); color: #fff; }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
  .el-icon { font-size: 16px; }
}

.preview-toolbar .scale-label {
  color: #fff;
  font-size: 13px;
  min-width: 44px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

/* ============ 文件右键菜单 ============ */
.file-context-menu {
  position: fixed;
  z-index: 10000;
  min-width: 140px;
  background: #ffffff;
  border: 1px solid #e4e7eb;
  border-radius: 6px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12), 0 2px 6px rgba(0, 0, 0, 0.06);
  padding: 4px 0;
  overflow: hidden;

  .context-menu-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 7px 14px;
    cursor: pointer;
    font-size: 12px;
    color: #3c4043;
    font-weight: 400;
    transition: background 0.12s ease;
    user-select: none;

    .el-icon {
      font-size: 13px;
      color: #5f6368;
      flex-shrink: 0;
    }

    &:hover {
      background: #f5f6f8;
      color: #1a73e8;

      .el-icon {
        color: #1a73e8;
      }
    }

    &.disabled {
      opacity: 0.55;
      cursor: not-allowed;

      &:hover {
        background: transparent;
        color: #3c4043;

        .el-icon {
          color: #5f6368;
        }
      }
    }
  }
}
</style>
