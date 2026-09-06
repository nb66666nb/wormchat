<template>
  <!-- 已撤回消息 — 居中提示条 -->
  <div v-if="isRecalled" class="recall-message">
    <div class="system-message-divider" />
    <div class="system-message-content">
      <span class="system-text">{{ recallText }}</span>
    </div>
    <div class="system-message-divider" />
  </div>

  <!-- 会话创建成功的系统消息 (mt=36) — 居中提示条 -->
  <div v-else-if="messageType === 36" class="system-message">
    <div class="system-message-divider" />
    <div class="system-message-content">
      <el-icon class="system-icon"><Promotion /></el-icon>
      <span class="system-text">{{ textContent }}</span>
    </div>
    <div class="system-message-divider" />
  </div>

  <div v-else :class="['chat-message', isMine ? 'is-mine' : 'other']">
    <!-- 头像区 -->
    <div class="avatar-section">
      <ChatAvatar
        :file-id="avatarInfo.fileId"
        :file-path="avatarInfo.filePath"
        :user-name="avatarInfo.userName"
        :user-id="avatarInfo.userId"
        :size="32"
      />
      <span class="sender-name">{{ isMine ? '我' : avatarInfo.userName }}</span>
    </div>

    <!-- 消息内容区 -->
    <div class="message-content-wrapper">
      <!-- 发送状态指示器（仅自己发的消息） -->
      <div v-if="isMine && messageStatus === 0" class="sending-indicator">
        <span class="sending-spinner" />
      </div>
      <div v-else-if="isMine && messageStatus === 2" class="send-failed-indicator" title="发送失败">
        <el-icon><WarningFilled /></el-icon>
      </div>

      <!-- 消息气泡 -->
      <div class="message-bubble" @contextmenu.prevent="handleContextMenu">
        <!-- 文字消息 -->
        <p v-if="messageType === 30 || messageType === 1" class="message-text">{{ textContent }}</p>

        <!-- 图片消息 -->
        <div v-else-if="messageType === 33 || messageType === 3" class="image-message" @click="$emit('preview-image', msg)">
          <img v-if="imageSrc" :src="imageSrc" class="chat-image" />
          <div v-else class="file-placeholder">
            <el-icon><PictureFilled /></el-icon>
            <span>{{ fileInfo.fileName }}</span>
            <span class="hint">图片</span>
          </div>
        </div>

        <!-- 视频消息 -->
        <div v-else-if="messageType === 34 || messageType === 4" class="video-message">
          <video v-if="videoSrc" :src="videoSrc" controls preload="metadata" class="chat-video" />
          <div v-else class="file-placeholder">
            <el-icon><VideoPlay /></el-icon>
            <span>{{ fileInfo.fileName }}</span>
            <span class="hint">视频</span>
          </div>
        </div>

        <!-- 音频消息 -->
        <div v-else-if="messageType === 35" class="audio-message">
          <audio v-if="msg.localPath" :src="'file://' + msg.localPath" controls preload="none" class="chat-audio" />
          <div v-else class="file-placeholder">
            <el-icon><Headset /></el-icon>
            <span>{{ fileInfo.fileName }}</span>
            <span class="hint">音频</span>
          </div>
        </div>

        <!-- 通用文件消息 -->
        <div v-else-if="messageType === 32 || messageType === 2" class="file-message" @click="$emit('file-click', msg)">
          <el-icon class="file-icon"><Document /></el-icon>
          <div class="file-info">
            <span class="file-name">{{ fileInfo.fileName }}</span>
            <span class="file-size">{{ formatFileSize(fileInfo.fileSize) }}</span>
          </div>
          <el-icon class="download-icon"><Download /></el-icon>
        </div>

        <!-- 其他 -->
        <p v-else class="message-text">{{ textContent }}</p>
      </div>

      <span class="message-time">{{ displayTime }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ChatDotRound, Document, Download, PictureFilled, VideoPlay, Headset, Promotion, WarningFilled } from '@element-plus/icons-vue'
import ChatAvatar from '@/components/ChatAvatar.vue'

const props = defineProps({
  msg: { type: Object, required: true },
  currentUserId: { type: String, default: '' },
  participants: { type: Array, default: () => [] }
})

const emit = defineEmits(['preview-image', 'file-click', 'contextmenu'])

const messageType = computed(() => props.msg.messageType)
const messageStatus = computed(() => props.msg.status ?? 1)
const isMine = computed(() => {
  const senderId = props.msg.sendUserId || props.msg.senderId
  return senderId === props.currentUserId
})

/** 是否已撤回：recalled=1 时整条消息渲染为居中提示条 */
const isRecalled = computed(() => props.msg.recalled === 1)

/** 撤回提示文案：优先用后端/本地已写入的 messageContent，兜底用发送者昵称 */
const recallText = computed(() => {
  const c = props.msg.messageContent || props.msg.content || ''
  if (c) return c
  const name = isMine.value ? '你' : (avatarInfo.value.userName || '对方')
  return `${name}撤回了一条消息`
})

/**
 * 右键菜单：阻止浏览器默认菜单，向父组件抛出 contextmenu 事件
 * 父组件据此弹出"删除/撤回"菜单
 */
const handleContextMenu = (e) => {
  // 已撤回的消息不弹右键菜单
  if (isRecalled.value) return
  // 系统消息不弹右键菜单
  if (messageType.value === 36) return
  // 向父组件抛出 contextmenu 事件，携带鼠标坐标和消息对象
  emit('contextmenu', e, props.msg)
}

const textContent = computed(() => props.msg.messageContent || props.msg.content || '')

const avatarInfo = computed(() => {
  const userId = props.msg.sendUserId || props.msg.senderId || ''
  const myId = props.currentUserId

  if (userId === myId) {
    let info = {}
    try { info = JSON.parse(localStorage.getItem('userInfo') || '{}') } catch (_) {}
    return {
      fileId: info.avatarFileId || info.fileId || '',
      filePath: info.avatarFilePath || info.filePath || '',
      userName: info.userName || info.nickName || '我',
      userId: myId
    }
  }

  const p = props.participants?.find(p => p.userId === userId || p.id === userId)
  if (p) {
    return {
      fileId: p.avatarFileId || p.fileId || p.avatarId || '',
      filePath: p.avatarFilePath || p.filePath || p.avatarUrl || '',
      userName: p.nickName || p.nickname || p.userName || `用户${userId.slice(-4)}`,
      userId
    }
  }

  return {
    fileId: '',
    filePath: '',
    userName: props.msg.sendUserNickName || props.msg.senderName || `用户${userId.slice(-4)}`,
    userId
  }
})

const fileInfo = computed(() => {
  if (props.msg.fileId || props.msg.filePath || props.msg.fileName) {
    return {
      fileId: props.msg.fileId || '',
      fileName: props.msg.fileName || props.msg.messageContent || '未知文件',
      fileSize: props.msg.fileSize || 0,
      fileType: props.msg.fileType || 5,
      filePath: props.msg.filePath || '',
      contentType: props.msg.contentType || ''
    }
  }
  try {
    const raw = props.msg.messageContent || props.msg.content || '{}'
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
})

const imageSrc = computed(() => {
  if (props.msg.localPath) return 'file://' + props.msg.localPath
  if (props.msg.thumbPath) return 'file://' + props.msg.thumbPath
  // 历史记录模式：通过 HTTP URL 获取
  if (fileInfo.value.filePath) {
    try {
      const config = JSON.parse(localStorage.getItem('serverConfig') || '{}')
      return `http://${config.host}:${config.httpPort || 6060}/api${fileInfo.value.filePath}`
    } catch (_) {}
  }
  return ''
})

const videoSrc = computed(() => {
  if (props.msg.localPath) return 'file://' + props.msg.localPath
  if (fileInfo.value.filePath) {
    try {
      const config = JSON.parse(localStorage.getItem('serverConfig') || '{}')
      return `http://${config.host}:${config.httpPort || 6060}/api${fileInfo.value.filePath}`
    } catch (_) {}
  }
  return ''
})

const displayTime = computed(() => {
  const t = props.msg.sendTime || props.msg.timestamp
  if (!t) return ''
  const d = new Date(t)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
})

const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return ''
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<style lang="scss" scoped>
.chat-message {
  display: flex;
  gap: 10px;
  max-width: 82%;
  animation: slideIn 0.25s ease-out;
  min-width: 0;

  &.is-mine {
    flex-direction: row-reverse;
    align-self: flex-end;
    .message-content-wrapper { align-items: flex-end; }
    .message-bubble {
      background: #1a73e8; color: #fff;
      box-shadow: 0 2px 8px rgba(26,115,232,0.18);
      .message-text { color: #fff; }
    }
  }

  &.other {
    align-self: flex-start;
    .message-bubble {
      background: #ffffff; border: 1px solid #e8eaed;
      box-shadow: 0 1px 4px rgba(0,0,0,0.04);
      .message-text { color: #202124; }
    }
  }
}

.avatar-section {
  display: flex; flex-direction: column; align-items: center; gap: 4px; flex-shrink: 0;
  .sender-name {
    font-size: 10px; color: #9aa0a6; max-width: 56px; overflow: hidden;
    text-overflow: ellipsis; white-space: nowrap; text-align: center; font-weight: 500;
  }
}

.message-content-wrapper {
  display: flex; flex-direction: column; gap: 4px; min-width: 0; max-width: 100%;

  .message-bubble {
    padding: 9px 13px; border-radius: 14px; word-break: break-word; line-height: 1.5;
    max-width: 100%; min-width: 0; box-sizing: border-box; overflow: hidden;
    .message-text { margin: 0; font-size: 13px; line-height: 1.55; }
  }

  .message-time { font-size: 10px; color: #bdc1c6; padding: 0 3px; }
}

/* 图片消息 */
.chat-message:has(.image-message) .message-bubble {
  padding: 0; background: transparent; border: none; box-shadow: none;
}
.image-message {
  max-width: 320px; min-width: 0;
  .chat-image {
    width: 100%; max-width: 100%; max-height: 260px; border-radius: 10px; cursor: pointer;
    display: block; object-fit: cover; box-shadow: 0 2px 8px rgba(0,0,0,0.12);
    &:hover { opacity: 0.92; }
  }
}

/* 视频消息 */
.chat-message:has(.video-message) .message-bubble {
  padding: 0; background: transparent; border: none; box-shadow: none;
}
.video-message {
  max-width: 340px; min-width: 0;
  .chat-video {
    width: 100%; max-width: 100%; max-height: 240px; border-radius: 10px;
    display: block; box-shadow: 0 2px 8px rgba(0,0,0,0.12);
  }
}

/* 音频消息 */
.audio-message {
  max-width: 100%; min-width: 0;
  .chat-audio { width: 100%; max-width: 260px; height: 36px; }
}

/* 文件消息 */
.file-message {
  display: flex; align-items: center; gap: 10px; cursor: pointer; padding: 4px 0;
  min-width: 0; max-width: 100%; box-sizing: border-box;
  .file-icon { font-size: 28px; color: #1a73e8; flex-shrink: 0; }
  .file-info {
    flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; overflow: hidden;
    .file-name { font-size: 13px; font-weight: 600; color: inherit; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 100%; }
    .file-size { font-size: 11px; color: #9aa0a6; }
  }
  .download-icon { font-size: 16px; color: #9aa0a6; flex-shrink: 0; }
  &:hover .download-icon { color: #1a73e8; }
}

.is-mine .file-message {
  .file-icon { color: #90caf9; }
  .file-info .file-size { color: rgba(255,255,255,0.7); }
  .download-icon { color: rgba(255,255,255,0.7); }
  &:hover .download-icon { color: #fff; }
}

/* 文件占位符 */
.file-placeholder {
  display: flex; flex-direction: column; align-items: center; gap: 8px;
  padding: 18px 20px; border-radius: 10px; cursor: pointer;
  background: rgba(26,115,232,0.06); border: 1px solid #90caf9;
  min-width: 170px; max-width: 100%; box-sizing: border-box;
  .el-icon { font-size: 32px; color: #1a73e8; }
  span { font-size: 12px; color: #5f6368; }
  .hint { font-size: 11px; color: #9aa0a6; }
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 系统消息 (mt=36) — 居中淡灰提示条 ===== */
.system-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
  padding: 10px 0;
  align-self: center;
  animation: systemFadeIn 0.3s ease-out;
}

/* ===== 已撤回消息 — 居中提示条（复用系统消息样式）===== */
.recall-message {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
  padding: 6px 0;
  align-self: center;
  animation: systemFadeIn 0.3s ease-out;
}

.system-message-divider {
  flex: 0 1 80px;
  height: 1px;
  background: linear-gradient(to right, transparent, #dadce0, transparent);
}

.system-message-content {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  background: rgba(154, 160, 166, 0.12);
  border: 1px solid rgba(154, 160, 166, 0.18);
  border-radius: 999px;
  font-size: 11px;
  color: #5f6368;
  white-space: nowrap;
  max-width: 70%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.system-message-content .system-icon {
  font-size: 12px;
  color: #1a73e8;
  flex-shrink: 0;
}

.system-message-content .system-text {
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== 发送状态指示器 ===== */
.sending-indicator {
  display: flex;
  align-items: center;
  padding-right: 2px;
  flex-shrink: 0;
}

.sending-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(26, 115, 232, 0.2);
  border-top-color: #1a73e8;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.send-failed-indicator {
  display: flex;
  align-items: center;
  color: #ea4335;
  font-size: 14px;
  padding-right: 2px;
  flex-shrink: 0;
  cursor: default;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes systemFadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
