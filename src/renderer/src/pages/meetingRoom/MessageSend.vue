<template>
  <div class="message-send-container">
    <!-- 多文件传输卡片列表 -->
    <div v-if="tasks.length > 0" class="transfer-list">
      <div
        v-for="task in tasks"
        :key="task.id"
        class="transfer-card"
        :class="cardClass(task)"
      >
        <!-- 右上角叉号 -->
        <div class="card-close" @click="handleRemoveTask(task)">
          <el-icon :size="12"><Close /></el-icon>
        </div>

        <div class="card-body">
          <!-- 图片缩略图 -->
          <div v-if="task.previewUrl" class="card-thumb">
            <img :src="task.previewUrl" alt="" />
          </div>
          <!-- 文件图标 -->
          <div v-else class="card-icon">
            <el-icon :size="24"><Document /></el-icon>
          </div>

          <div class="card-info">
            <div class="card-name" :title="task.fileName">{{ task.fileName }}</div>
            <div class="card-size">{{ formatSize(task.fileSize) }}</div>

            <!-- 进度条（上传中） -->
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

            <!-- 上传完成，等待发送 -->
            <div v-else-if="task.status === S.READY" class="card-status ready">
              <el-icon :size="12"><Check /></el-icon>
              <span>上传完成，等待发送</span>
            </div>

            <!-- 发送中 -->
            <div v-else-if="task.status === S.SENDING" class="card-status sending">
              <el-icon class="is-loading" :size="12"><Loading /></el-icon>
              <span>发送中...</span>
            </div>

            <!-- 已发送 -->
            <div v-else-if="task.status === S.SENT" class="card-status sent">
              <el-icon :size="12"><Check /></el-icon>
              <span>已发送</span>
            </div>

            <!-- 失败 -->
            <div v-else-if="task.status === S.FAILED" class="card-status failed">
              <el-icon :size="12"><WarningFilled /></el-icon>
              <span>{{ task.error || '上传失败' }}</span>
              <el-button type="primary" link size="small" @click.stop="retryTask(task)">重试</el-button>
            </div>

            <!-- 已取消 -->
            <div v-else-if="task.status === S.CANCELLED" class="card-status cancelled">
              <span>已取消</span>
            </div>

            <!-- 秒传标记 -->
            <div v-if="task.fastPass" class="fast-pass-tag">秒传</div>
          </div>
        </div>
      </div>
    </div>

    <div class="send-row">
      <el-upload
        :show-file-list="false"
        :before-upload="handleBeforeUpload"
        :http-request="handleUploadRequest"
        accept="image/*,video/*,.pdf,.doc,.docx,.xls,.xlsx,.txt,.zip,.rar"
        multiple
      >
        <el-button circle size="small" class="icon-btn" title="发送文件">
          <el-icon><Upload /></el-icon>
        </el-button>
      </el-upload>

      <el-button circle size="small" class="icon-btn" title="表情" @click="toggleEmoji">
        <el-icon><ChatDotRound /></el-icon>
      </el-button>

      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="2"
        :placeholder="hasUploading ? '文件上传中...' : '输入消息...'"
        @keyup.enter.exact="handleSend('broadcast')"
        resize="none"
        class="message-input"
      />

      <el-dropdown trigger="click" @command="handleMessageType" placement="top-end">
        <el-button
          type="primary"
          :disabled="(!inputMessage.trim() && !hasReadyToSend) || sending"
          class="send-btn"
        >
          <el-icon style="margin-right:4px"><Promotion /></el-icon>
          {{ sending ? '发送中' : '群发' }}
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="broadcast">
              <el-icon><ChatLineSquare /></el-icon>
              <span>群发</span>
            </el-dropdown-item>
            <el-dropdown-item command="private">
              <el-icon><User /></el-icon>
              <span>私发</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <!-- 表情面板 -->
    <EmojiPanel :visible="showEmoji" @select="insertEmoji" />

    <!-- 私发对话框 -->
    <el-dialog v-model="showPrivateDialog" title="选择私发对象" width="320px" :append-to-body="true">
      <div class="member-list">
        <div
          v-for="member in filteredParticipants" :key="member.userId"
          class="member-item" :class="{ selected: selectedMemberId === member.userId }"
          @click="selectMember(member)"
        >
          <el-avatar :size="32" :icon="UserFilled" />
          <span class="member-name">{{ member.nickName || member.nickname || member.userName || '用户' }}</span>
          <el-icon v-if="selectedMemberId === member.userId" class="check-icon"><Check /></el-icon>
        </div>
      </div>
      <template #footer>
        <el-button @click="showPrivateDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmPrivateSend" :disabled="!selectedMemberId">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, getCurrentInstance } from "vue"
const { proxy } = getCurrentInstance();
import { Upload, ChatDotRound, Promotion, ChatLineSquare, User, UserFilled, Check, Document, Close, Loading, WarningFilled } from '@element-plus/icons-vue'
import UserInfoStore from '@/stores/UserInfoStore'
import { ElMessage } from 'element-plus'
import { useChunkUploadManager } from '@/utils/useChunkUploadManager'
import EmojiPanel from '@/components/EmojiPanel.vue'

const props = defineProps({
  participants: { type: Array, default: () => [] },
  meetingId: { type: String, default: '' }
})

const emit = defineEmits(['message-sent', 'file-transfer'])
const currentUserId = UserInfoStore.getUserId()

const filteredParticipants = computed(() =>
  props.participants.filter(m => (m.userId || m.id) !== currentUserId)
)

// 文字消息状态
const inputMessage = ref('')
const showPrivateDialog = ref(false)
const selectedMemberId = ref(null)
const sending = ref(false)

// 表情面板
const showEmoji = ref(false)
const toggleEmoji = () => { showEmoji.value = !showEmoji.value }
const insertEmoji = (emoji) => {
  inputMessage.value += emoji
  showEmoji.value = false
}

// 多文件上传管理器
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

// ==================== 工具函数 ====================

const getCurrentUser = () => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return { userId: info.userId || '', nickName: info.nickname || info.userName || '我' }
  } catch (_) { return { userId: '', nickName: '我' } }
}

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

// ==================== 文件上传 ====================

const handleBeforeUpload = () => true

const handleUploadRequest = async ({ file }) => {
  const rawFile = file.raw || file
  const messageType = getFileMessageType(rawFile)
  addTask(rawFile, props.meetingId, messageType)
}

/** 点击叉号 */
const handleRemoveTask = (task) => {
  if (isUploading(task)) {
    // 上传中 → 取消上传
    cancelTask(task)
  } else if (task.status === S.READY) {
    // 等待发送 → 移除（不发送）
    removeTask(task)
  } else {
    // 已发送/失败/取消 → 从列表移除
    removeTask(task)
  }
}

/** 重试失败的任务 */
const retryTask = (task) => {
  task.status = S.UPLOADING
  task.progress = 0
  task.error = null
  task.phase = ''
  // 重新上传
  const file = task.file
  if (file) {
    const messageType = task.messageType
    removeTask(task)
    addTask(file, task.meetingNo, messageType)
  }
}

// ==================== 发送消息 ====================

const buildMessageParams = (type, targetUserId, content, messageType, fileData = {}) => ({
  messageSend2Type: type,
  meetingId: props.meetingId,
  sendUserId: getCurrentUser().userId,
  sendUserNickName: getCurrentUser().nickName,
  messageContent: content,
  receiveUserId: type === 1 ? props.meetingId : targetUserId,
  messageType,
  sendTime: Date.now(),
  status: 1,
  fileSize: fileData.fileSize || 0,
  fileName: fileData.fileName || '',
  fileId: fileData.fileId || '',
  filePath: fileData.filePath || '',
  fileType: fileData.fileType || 5,
  contentType: fileData.contentType || '',
  extendData: ''
})

/** 按顺序发送所有已就绪的文件 */
const sendReadyFiles = async (type = 1, targetUserId = null) => {
  const readyTasks = getReadyTasks()
  if (readyTasks.length === 0) return

  sending.value = true
  try {
    for (const task of readyTasks) {
      if (task.status !== S.READY) continue  // 可能已被取消

      markSending(task)
      try {
        const result = await proxy.Request({
          url: proxy.Api.sendMessage,
          params: buildMessageParams(
            type,
            targetUserId,
            task.fileName,
            task.messageType,
            task.fileData
          ),
          showLoading: false
        })
        if (result && result.code === 200) {
          markSent(task)
        } else {
          markSendFailed(task)
        }
      } catch (e) {
        markSendFailed(task)
      }
    }
  } finally {
    sending.value = false
    // 立即清理已发送的任务
    clearFinished()
  }
}

/** 发送按钮：文字消息 或 触发文件发送 */
const handleSend = async (type = 'broadcast', targetUserId = null) => {
  const content = inputMessage.value.trim()

  // 有待发送文件 → 发送文件消息
  if (hasReadyToSend.value) {
    await sendReadyFiles(type === 'broadcast' ? 1 : 0, targetUserId)
    return
  }

  // 纯文字消息
  if (!content || sending.value) return
  sending.value = true
  try {
    const result = await proxy.Request({
      url: proxy.Api.sendMessage,
      params: buildMessageParams(type === 'broadcast' ? 1 : 0, targetUserId, content, 30),
      showLoading: false
    })
    if (result && result.code === 200) inputMessage.value = ''
  } catch (_) {} finally { sending.value = false }
}

const handleMessageType = (command) => {
  if (command === 'broadcast') handleSend('broadcast')
  else if (command === 'private') {
    if (filteredParticipants.value.length === 0) { handleSend('broadcast'); return }
    showPrivateDialog.value = true; selectedMemberId.value = null
  }
}

const selectMember = (m) => { selectedMemberId.value = m.userId }
const confirmPrivateSend = () => {
  if (selectedMemberId.value) { handleSend('private', selectedMemberId.value); showPrivateDialog.value = false }
}
</script>

<style lang="scss" scoped>
.message-send-container {
  padding: 12px 16px 24px 16px;
  margin-top: 12px;
  border-top: 1px solid #e8eaed;
  background: #f8f9fa;
  border-radius: 0 0 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* ==================== 传输卡片列表 ==================== */
.transfer-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
  padding-right: 2px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background-color: #dadce0; border-radius: 2px; }
}

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

  &.card-uploading { border-color: #1a73e8; }
  &.card-ready { border-color: #67c23a; }
  &.card-sent { border-color: #dadce0; opacity: 0.7; }
  &.card-failed { border-color: #f56c6c; }
  &.card-cancelled { border-color: #dadce0; opacity: 0.5; }

  /* 右上角叉号 */
  .card-close {
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

    &:hover {
      background: #fee2e2;
      color: #d93025;
    }
  }

  .card-body {
    display: flex;
    align-items: center;
    gap: 10px;
    width: 100%;
    min-width: 0;
  }

  .card-thumb {
    width: 40px;
    height: 40px;
    flex-shrink: 0;
    border-radius: 6px;
    overflow: hidden;
    background: #f1f3f4;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
  }

  .card-icon {
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

  .card-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .card-name {
    font-size: 13px;
    font-weight: 500;
    color: #202124;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .card-size {
    font-size: 11px;
    color: #9aa0a6;
  }

  .card-progress {
    margin-top: 2px;
  }

  .card-status {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 11px;
    margin-top: 1px;

    &.uploading { color: #1a73e8; }
    &.ready { color: #67c23a; }
    &.sending { color: #1a73e8; }
    &.sent { color: #67c23a; }
    &.failed { color: #f56c6c; }
    &.cancelled { color: #9aa0a6; }

    .chunk-info {
      color: #9aa0a6;
      margin-left: 4px;
    }
  }

  .fast-pass-tag {
    display: inline-block;
    padding: 0 5px;
    font-size: 10px;
    color: #67c23a;
    background: #f0f9eb;
    border-radius: 3px;
    font-weight: 500;
    width: fit-content;
  }
}

/* ==================== 发送行 ==================== */
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

/* ==================== 私发对话框 ==================== */
.member-list {
  max-height: 320px; overflow-y: auto;
  &::-webkit-scrollbar { width: 5px; }
  &::-webkit-scrollbar-thumb { background-color: #dadce0; border-radius: 3px; }
}

.member-item {
  display: flex; align-items: center; gap: 11px; padding: 11px 14px;
  cursor: pointer; border-radius: 8px; transition: background 0.2s ease;
  &:hover { background: #f1f3f4; }
  &.selected { background: rgba(26, 115, 232, 0.1); }
  .member-name { flex: 1; font-size: 14px; color: #2c3e50; font-weight: 500; }
  .check-icon { color: #1a73e8; font-size: 17px; }
}

:deep(.el-dropdown-menu__item) { display: flex; align-items: center; gap: 9px; padding: 9px 18px; }
</style>
