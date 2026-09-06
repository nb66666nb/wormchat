<template>
  <div class="im-chat-panel">
    <!-- 顶部联系人信息栏 -->
    <div class="chat-topbar" v-if="conversation">
      <div class="topbar-left">
        <span class="topbar-name">{{ conversation.userName }}</span>
        <span v-if="isGroupChat" class="topbar-member-count">({{ groupMemberCount }}人)</span>
        <span v-if="isGroupDissolved" class="topbar-dissolved-tag">已解散</span>
      </div>
      <div class="topbar-right">
        <el-button v-if="isGroupChat && !isGroupDissolved" link size="small" class="settings-btn" @click="showGroupSettings = true">
          <el-icon :size="18"><Setting /></el-icon>
          <span>群设置</span>
        </el-button>
      </div>
    </div>

    <!-- 群聊已解散提示条 -->
    <div v-if="isGroupDissolved" class="dissolved-banner">
      <el-icon :size="14"><WarningFilled /></el-icon>
      <span>该群聊已解散，无法发送消息</span>
    </div>

    <!-- 消息列表 -->
    <div class="chat-messages" ref="messagesContainer" @scroll="handleScroll" v-if="conversation">
      <div v-if="messages.length === 0" class="empty-chat">
        <p>开始聊天吧</p>
      </div>

      <div v-else class="message-list" ref="messageListRef">
        <ChatMessageBubble
          v-for="(msg, index) in messages"
          :key="index"
          :msg="msg"
          :current-user-id="currentUserId"
          :participants="participants"
          @preview-image="handlePreviewImage"
          @file-click="handleFileClick"
          @contextmenu="handleBubbleContextMenu"
        />
      </div>
    </div>

    <!-- 右键消息菜单（删除/撤回） -->
    <div
      v-if="contextMenu.visible"
      class="msg-context-menu"
      :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      @click.stop
    >
      <div class="ctx-item" @click="handleContextMenuRecall" v-if="contextMenu.canRecall">
        <el-icon><RefreshLeft /></el-icon>
        <span>撤回</span>
      </div>
      <div class="ctx-item danger" @click="handleContextMenuDelete">
        <el-icon><Delete /></el-icon>
        <span>删除</span>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area" v-if="conversation && !isGroupDissolved">
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
        <!-- 知识上传按钮（非机器人会话禁用） -->
        <el-button
          circle
          size="small"
          class="icon-btn knowledge-btn"
          :title="isBotConversation ? '上传知识文档' : '仅机器人会话可用'"
          :disabled="!isBotConversation"
          @click="showUploadDialog = true"
          :loading="uploadingKnowledge"
        >
          <el-icon><FolderOpened /></el-icon>
        </el-button>

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

    <!-- 知识上传弹窗 -->
    <el-dialog
      v-model="showUploadDialog"
      title="上传知识文档"
      width="520px"
      :close-on-click-modal="false"
      append-to-body
      @open="loadKnowledgeList"
    >
      <div class="upload-dialog-body">
        <el-upload
          ref="knowledgeUploadRef"
          :show-file-list="false"
          :auto-upload="false"
          :on-change="onKnowledgeFileChange"
          drag
          accept=".txt,.json,.md"
        >
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 .txt .json .md，单文件最大 10MB
            </div>
          </template>
        </el-upload>

        <!-- 已上传文档列表 -->
        <div v-if="knowledgeDocs.length > 0" class="uploaded-docs-section">
          <div class="uploaded-docs-header">
            <span class="uploaded-docs-title">已上传文档（{{ knowledgeDocs.length }}个）</span>
            <el-button link size="small" type="danger" @click="clearKnowledge">清空全部</el-button>
          </div>
          <div class="uploaded-docs-list">
            <div v-for="doc in knowledgeDocs" :key="doc.source" class="uploaded-docs-item">
              <el-icon :size="14"><Document /></el-icon>
              <span class="uploaded-docs-name">{{ doc.source }}</span>
              <span class="uploaded-docs-meta">{{ doc.chunks }}片段 · {{ formatSize(doc.total_size) }}</span>
              <el-button link size="small" type="danger" @click.stop="deleteOneKnowledge(doc.source)" title="删除此文档">
                <el-icon :size="14"><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button
          type="primary"
          @click="confirmUpload"
          :disabled="!pendingKnowledgeFile"
          :loading="uploadingKnowledge"
        >
          上传并索引
        </el-button>
      </template>
    </el-dialog>

    <!-- 群聊设置面板 -->
    <GroupSettingsPanel
      v-if="showGroupSettings"
      :group-id="conversation?.userId"
      :session-id="conversation?.sessionId"
      :current-user-id="currentUserId"
      @close="showGroupSettings = false"
      @group-changed="handleGroupChanged"
    />

  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch, onMounted, onUnmounted } from 'vue'
import { Upload, ChatDotRound, Promotion, Document, Close, Loading, Check, WarningFilled, ZoomIn, ZoomOut, RefreshRight, FullScreen, FolderOpened, UploadFilled, Delete, Setting, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatMessageBubble from '@/components/ChatMessageBubble.vue'
import EmojiPanel from '@/components/EmojiPanel.vue'
import GroupSettingsPanel from '@/components/GroupSettingsPanel.vue'
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

// 知识上传
const showUploadDialog = ref(false)
const knowledgeUploadRef = ref(null)
const pendingKnowledgeFile = ref(null)
const uploadingKnowledge = ref(false)
const knowledgeDocs = ref([])

/** 是否为机器人会话（R 前缀 或 botCategory 有值） */
const isBotConversation = computed(() => {
  if (!props.conversation?.userId) return false
  return getContactTypeByPrefix(props.conversation.userId) === 2 || !!props.conversation.botCategory
})

// ==================== 群聊相关 ====================
const showGroupSettings = ref(false)
const groupInfo = ref(null)
const groupMembers = ref([])

/** 是否为群聊会话 */
const isGroupChat = computed(() => {
  // 后端 UserContactTypeEnum: USER=0, GROUP=1, ROBOT=2（G前缀必为群聊）
  return getContactTypeByPrefix(props.conversation?.userId) === 1
})

/** 群聊是否已解散 */
const isGroupDissolved = computed(() => {
  return isGroupChat.value && groupInfo.value?.status === 1
})

/** 群成员数量 */
const groupMemberCount = computed(() => {
  return groupMembers.value.length
})

/** 加载群信息 */
const loadGroupInfo = async () => {
  if (!isGroupChat.value || !props.conversation?.userId) return
  try {
    const groupId = props.conversation.userId
    // 先从本地 SQLite 读取
    let info = await window.api.dbGroupInfoGet(groupId)
    if (!info) {
      // 本地没有，从后端拉取
      const res = await Request({
        url: Api.getGroupInfo,
        params: { groupId },
        json: true,
        showLoading: false,
        showError: false
      })
      if (res && res.code === 200 && res.data) {
        info = res.data
        await window.api.dbGroupInfoSave(info)
      }
    }
    groupInfo.value = info

    // 加载群成员
    let members = await window.api.dbGroupMembersGet(groupId)
    if (!members || members.length === 0) {
      const res = await Request({
        url: Api.getGroupMembers,
        params: { groupId },
        json: true,
        showLoading: false,
        showError: false
      })
      if (res && res.code === 200 && res.data) {
        members = res.data
        await window.api.dbGroupMembersBatchSave(members)
      }
    }
    groupMembers.value = members || []
  } catch (e) {
    console.error('[ImChatPanel] 加载群信息失败:', e.message)
  }
}

/** 群设置变更回调 */
const handleGroupChanged = () => {
  loadGroupInfo()
}

// ==================== 知识上传（弹窗模式） ====================

/** el-upload on-change：选中文件后暂存，等用户点"上传并索引" */
const onKnowledgeFileChange = (file) => {
  const raw = file.raw
  if (!raw) return
  const name = raw.name.toLowerCase()
  if (!name.endsWith('.txt') && !name.endsWith('.json') && !name.endsWith('.md')) {
    ElMessage.warning('仅支持 .txt .json .md 格式')
    return
  }
  if (raw.size > 10 * 1024 * 1024) {
    ElMessage.warning('文件最大 10MB')
    return
  }
  pendingKnowledgeFile.value = raw
  ElMessage.success(`已选择: ${raw.name}`)
}

const confirmUpload = async () => {
  if (!pendingKnowledgeFile.value) return
  uploadingKnowledge.value = true
  try {
    const formData = new FormData()
    formData.append('file', pendingKnowledgeFile.value)
    formData.append('botId', props.conversation.userId)

    // 生产环境适配：通过 window.api.apiRequest 发送 multipart
    const token = localStorage.getItem('token') || ''
    let respData
    if (!import.meta.env.DEV && window.api?.apiRequest) {
      // 生产环境 IPC
      const params = {}
      for (const [k, v] of formData.entries()) params[k] = v
      const baseUrl = (() => {
        try {
          const cfg = JSON.parse(localStorage.getItem('serverConfig') || '{}')
          if (cfg.host) return `http://${cfg.host}:${cfg.httpPort || 6060}/api`
        } catch (_) {}
        return '/api'
      })()
      const { response } = await window.api.apiRequest({
        url: Api.ragUpload,
        baseURL: baseUrl,
        params,
        json: false,
        headers: { token, 'Content-Type': 'multipart/form-data' },
        formData: params
      })
      respData = response?.data || response
    } else {
      // 开发环境 或 fallback
      const baseUrl = import.meta.env.DEV ? '/api' : ''
      const resp = await fetch(baseUrl + Api.ragUpload, { method: 'POST', headers: { token }, body: formData })
      respData = await resp.json()
    }

    if (respData.code === 200) {
      ElMessage.success(`知识已索引（${respData.data?.chunks || 0} 个片段）`)
      showUploadDialog.value = false
      pendingKnowledgeFile.value = null
      loadKnowledgeList()
    } else {
      ElMessage.error(respData.info || '上传失败')
    }
  } catch (err) {
    console.error('[ImChatPanel] 知识上传失败:', err)
    ElMessage.error('上传失败')
  } finally {
    uploadingKnowledge.value = false
  }
}

const loadKnowledgeList = async () => {
  try {
    const res = await Request({ url: Api.ragList, params: { botId: props.conversation.userId }, json: true, showLoading: false })
    if (res && res.code === 200) {
      knowledgeDocs.value = res.data || []
    }
  } catch (err) { console.error('[ImChatPanel] 加载知识列表失败:', err) }
}

const clearKnowledge = async () => {
  try {
    await ElMessageBox.confirm('确定要清空该机器人的全部知识文档吗？', '确认', { type: 'warning' })
    const res = await Request({ url: Api.ragClear, params: { botId: props.conversation.userId }, json: true, showLoading: false })
    if (res && res.code === 200) {
      ElMessage.success(`已清空 ${res.data?.deleted || 0} 条知识`)
      knowledgeDocs.value = []
    }
  } catch (err) { if (err !== 'cancel') console.error('[ImChatPanel] 清空知识失败:', err) }
}

/** 删除单个知识文档 */
const deleteOneKnowledge = async (source) => {
  try {
    await ElMessageBox.confirm(`确定要删除「${source}」吗？`, '确认', { type: 'warning' })
    const res = await Request({ url: Api.ragDeleteOne, params: { botId: props.conversation.userId, source }, json: true, showLoading: false })
    if (res && res.code === 200) {
      ElMessage.success(`已删除「${source}」`)
      loadKnowledgeList()
    }
  } catch (err) { if (err !== 'cancel') console.error('[ImChatPanel] 删除知识失败:', err) }
}

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

/**
 * 当前会话作为"对方参与者"传给 ChatMessageBubble
 *  - 机器人场景：从 conversation.botAvatarPath 解析出 fileId/filePath
 *  - 用户场景：conversation.avatarFileId / avatarFilePath 直接可用
 *  - 兜底：解析 userId 末位数字作为 userName
 */
const participants = computed(() => {
  const conv = props.conversation
  if (!conv) return []
  let avatarFileId = conv.avatarFileId || ''
  let avatarFilePath = conv.avatarFilePath || ''
  // 机器人：botAvatarPath 形如 /avatar/2026/06/30/<fileId>.png，拆出 fileId
  if (!avatarFileId && conv.botAvatarPath && conv.botAvatarPath.startsWith('/avatar/')) {
    const lastSlash = conv.botAvatarPath.lastIndexOf('/')
    const fileName = lastSlash >= 0 ? conv.botAvatarPath.substring(lastSlash + 1) : conv.botAvatarPath
    const dotIdx = fileName.lastIndexOf('.')
    avatarFileId = dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName
    avatarFilePath = conv.botAvatarPath
  }
  return [{
    userId: conv.userId,
    nickName: conv.userName || `用户${String(conv.userId).slice(-4)}`,
    avatarFileId,
    avatarFilePath
  }]
})

// ==================== 右键消息菜单（删除/撤回）====================

/** 撤回时间限制：发送后 2 分钟内可撤回 */
const RECALL_TIME_LIMIT = 2 * 60 * 1000

const contextMenu = ref({
  visible: false,
  x: 0,
  y: 0,
  msg: null,
  canRecall: false
})

/** 关闭右键菜单 */
const closeContextMenu = () => {
  contextMenu.value.visible = false
}

/**
 * 消息气泡右键菜单：定位并显示菜单
 * 删除：对所有消息可用（仅本地删除）
 * 撤回：仅自己发送 + 已送达 + 2 分钟内 + 非系统消息
 */
const handleBubbleContextMenu = (e, msg) => {
  const senderId = msg.sendUserId || msg.senderId
  const isMine = senderId === currentUserId.value
  const sendTime = msg.sendTime || msg.timestamp || 0
  const withinRecall = sendTime > 0 && (Date.now() - sendTime) < RECALL_TIME_LIMIT
  const canRecall = isMine && withinRecall && (msg.status === 1) && !msg.recalled

  // 菜单位置：靠近点击点，靠近底部时上移避免溢出
  const menuWidth = 120
  const menuHeight = canRecall ? 96 : 48
  let x = e.clientX
  let y = e.clientY
  if (x + menuWidth > window.innerWidth) x = window.innerWidth - menuWidth - 4
  if (y + menuHeight > window.innerHeight) y = window.innerHeight - menuHeight - 4

  contextMenu.value = { visible: true, x, y, msg, canRecall }
}

/**
 * 删除消息（仅本地删除，不影响后端和其他端）
 */
const handleContextMenuDelete = async () => {
  const msg = contextMenu.value.msg
  closeContextMenu()
  if (!msg) return
  try {
    await ElMessageBox.confirm('确定删除这条消息吗？（仅本地删除）', '删除消息', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch (_) {
    return
  }
  // 优先用本地主键删除
  const localId = msg.messageId
  if (localId && typeof localId === 'number' && !msg.backendMessageId) {
    await window.api.dbImMessageDelete(localId)
  } else if (msg.backendMessageId) {
    // 兜底：按后端 ID 找本地记录删除（少见路径）
    // 本地主键与后端 ID 不同，这里只按本地主键删；若只有后端 ID 则查不到本地主键时跳过
    await window.api.dbImMessageDelete(localId || msg.backendMessageId)
  }
  // 从 UI 移除
  const idx = messages.value.findIndex(m => m === msg)
  if (idx !== -1) {
    messages.value.splice(idx, 1)
  }
  ElMessage.success('已删除')
}

/**
 * 撤回消息：调用后端撤回接口，成功后本地标记 recalled + 更新 UI
 *
 * 关键：用 messageOnlyId（前端 UUID 字符串）作为关联键，而非 messageId。
 * 雪花 messageId 超出 JS 安全整数范围，前端 JSON 解析会精度丢失，
 * 传回后端无法精确匹配（会报"消息不存在"）。messageOnlyId 不受此影响。
 */
const handleContextMenuRecall = async () => {
  const msg = contextMenu.value.msg
  closeContextMenu()
  if (!msg) return
  const messageOnlyId = msg.messageOnlyId
  if (!messageOnlyId) {
    ElMessage.warning('消息缺少唯一标识，无法撤回')
    return
  }
  try {
    const res = await Request({
      url: Api.recallImMessage,
      params: {
        messageOnlyId: messageOnlyId
      },
      showLoading: false
    })
    if (!res || res.code !== 200) {
      ElMessage.error(res?.info || '撤回失败')
      return
    }
    // 后端撤回成功：本地按 messageOnlyId 标记 recalled=1（立即生效）
    const recallText = `你撤回了一条消息`
    await window.api.dbImMessageMarkRecalledByOnlyId(messageOnlyId, { messageContent: recallText })
    // 更新 UI：把消息改成撤回提示
    const idx = messages.value.findIndex(m => m.messageOnlyId === messageOnlyId)
    if (idx !== -1) {
      messages.value[idx] = {
        ...messages.value[idx],
        recalled: 1,
        messageContent: recallText
      }
    }
    ElMessage.success('已撤回')
  } catch (e) {
    console.error('[ImChatPanel] 撤回失败:', e.message)
    ElMessage.error('撤回失败')
  }
}

const getCurrentTime = () => Date.now()

// 消息列表元素（用于 ResizeObserver 监听内容高度变化，解决图片异步加载撑高后滚动定位失效）
const messageListRef = ref(null)
// 当前是否需要"贴底"（用户在底部附近/刚切换会话/刚收到新消息时为 true，手动上滑后为 false）
const isNearBottom = ref(true)
let resizeObserver = null

const scrollToBottom = (behavior = 'auto') => {
  isNearBottom.value = true
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTo({ top: messagesContainer.value.scrollHeight, behavior })
    }
    // 图片等异步内容加载撑高后，由下方 ResizeObserver 兜底补滚到底
  })
}

// 监听消息列表挂载/尺寸变化：图片加载撑高等场景下，若应贴底则自动补滚
watch(messageListRef, (el) => {
  if (resizeObserver) { resizeObserver.disconnect(); resizeObserver = null }
  if (el && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      if (isNearBottom.value && messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    })
    resizeObserver.observe(el)
  }
}, { flush: 'post' })

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
    console.error('[ImChatPanel] 打开文件失败:', e.message)
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
        // 3. 使用后端返回的 messageId（HTTP 响应是主确认通道）
        const backendMessageId = res.data?.messageId
        params.status = 1
        if (backendMessageId) {
          // 后端 messageId 存入 backendMessageId 字段，本地主键由 SQLite 自增
          params.backendMessageId = backendMessageId
        }
        await window.api.dbImMessageSend(params)
        // 更新 UI 中对应消息的 messageId 和 status
        const idx = messages.value.findIndex(m => m.messageOnlyId === messageOnlyId)
        if (idx !== -1) {
          messages.value[idx].status = 1
          if (backendMessageId) messages.value[idx].messageId = backendMessageId
        }
        markSent(task)
      } catch (e) {
        console.error('[ImChatPanel] 文件消息发送失败:', e.message)
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
    // 使用后端返回的 messageId（HTTP 响应是主确认通道，不再依赖 WebSocket 回声）
    const backendMessageId = res.data?.messageId
    params.status = 1
    if (backendMessageId) {
      // 后端 messageId 存入 backendMessageId 字段，本地主键由 SQLite 自增
      params.backendMessageId = backendMessageId
    }
    await window.api.dbImMessageSend(params)
    // 更新 UI 中对应消息的 messageId 和 status
    const idx = messages.value.findIndex(m => m.messageOnlyId === messageOnlyId)
    if (idx !== -1) {
      messages.value[idx].status = 1
      if (backendMessageId) messages.value[idx].messageId = backendMessageId
    }
  } catch (e) {
    console.error('[ImChatPanel] 发送消息失败:', e.message)
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
    console.error('[ImChatPanel] 加载消息失败:', e.message)
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

  console.log(`[ImChatPanel] 批量检查 ${fileMessages.length} 个文件消息的本地缓存`)

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
      console.warn('[ImChatPanel] 检查文件消息失败:', e.message)
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
  if (!messagesContainer.value) return
  const el = messagesContainer.value
  // 维护"是否贴底"标志：距底部 < 80px 视为贴底（图片加载撑高时据此决定是否补滚）
  isNearBottom.value = el.scrollHeight - el.scrollTop - el.clientHeight < 80
  if (isLoading.value || !hasMore.value) return
  if (el.scrollTop <= 50) {
    loadMessages(false)
  }
}

let removeMessageListener = null
let removeGroupEventListener = null
const setupMessageListener = () => {
  if (window.messageAPI?.onNewMessage) {
    removeMessageListener = window.messageAPI.onNewMessage((msg) => {
      // 撤回通知(messageType=48)：把当前会话 UI 中对应消息改为撤回提示
      if (msg.messageType === 48) {
        if (msg.sessionId !== props.conversation?.sessionId) return
        const idx = messages.value.findIndex(m => {
          // 优先按 messageOnlyId（字符串，不受雪花 messageId 精度丢失影响）精确匹配
          if (msg.messageOnlyId && m.messageOnlyId) return m.messageOnlyId === msg.messageOnlyId
          const bid = m.backendMessageId || m.messageId
          return bid === msg.messageId
        })
        if (idx !== -1) {
          messages.value[idx] = {
            ...messages.value[idx],
            recalled: 1,
            messageContent: msg.messageContent || '对方撤回了一条消息'
          }
        }
        return
      }

      if (msg.messageTypeIm !== 0 || !msg.sessionId) return

      if (window.api?.dbSessionMarkActive) {
        window.api.dbSessionMarkActive(msg.sessionId, {
          lastMessage: msg.messageContent,
          lastMessageTime: msg.sendTime || Date.now()
        })
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
    knowledgeDocs.value = []
    groupInfo.value = null
    groupMembers.value = []
    if (props.conversation) {
      loadMessages(true)
      if (isBotConversation.value) loadKnowledgeList()
      if (isGroupChat.value) loadGroupInfo()
    }
  }
})

onMounted(() => {
  setupMessageListener()
  // 点击页面任意位置关闭右键消息菜单
  window.addEventListener('click', closeContextMenu)
  window.addEventListener('scroll', closeContextMenu, true)
  if (window.messageAPI) {
    window.messageAPI.onFileReady(handleFileReady)
    // 监听群聊系统事件
    removeGroupEventListener = window.messageAPI.onSessionEvent((data) => {
      const mt = data.messageType
      if (mt >= 40 && mt <= 45 && data.sessionId === props.conversation?.sessionId) {
        loadGroupInfo()
      }
    })
  }
  if (props.conversation) {
    loadMessages(true)
    if (isGroupChat.value) loadGroupInfo()
  }
})

onUnmounted(() => {
  if (resizeObserver) { resizeObserver.disconnect(); resizeObserver = null }
  if (removeMessageListener) {
    removeMessageListener()
    removeMessageListener = null
  }
  if (removeGroupEventListener) {
    removeGroupEventListener()
    removeGroupEventListener = null
  }
  window.removeEventListener('click', closeContextMenu)
  window.removeEventListener('scroll', closeContextMenu, true)
})

/** 主进程文件下载完成后更新 UI */
const handleFileReady = (data) => {
  if (!data) return
  const fileId = data.fileId || ''
  const filePath = data.filePath || ''
  if (!fileId && !filePath) return
  const idx = messages.value.findIndex(m => {
    if (fileId && m.fileId === fileId) return true
    if (filePath && m.filePath === filePath) return true
    return false
  })
  if (idx !== -1) {
    messages.value[idx] = {
      ...messages.value[idx],
      _fileStatus: data._fileStatus,
      localPath: data.localPath || messages.value[idx].localPath,
      thumbPath: data.thumbPath || null,
      _error: data._error
    }
  }
}

defineExpose({
  messages,
  clearMessages: () => { messages.value = [] }
})
</script>

<style lang="css" scoped>
.im-chat-panel {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

/* 右键消息菜单 */
.msg-context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 116px;
  background: #ffffff;
  border: 1px solid #e8eaed;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  padding: 4px 0;
  animation: ctxFadeIn 0.12s ease-out;
}
.msg-context-menu .ctx-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  font-size: 13px;
  color: #3c4043;
  cursor: pointer;
  transition: background 0.15s;
}
.msg-context-menu .ctx-item:hover {
  background: #f1f3f4;
}
.msg-context-menu .ctx-item.danger {
  color: #ea4335;
}
.msg-context-menu .ctx-item.danger:hover {
  background: #fef0f0;
}
@keyframes ctxFadeIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 顶部信息栏 */
.chat-topbar {
  padding: 14px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.topbar-name {
  font-size: 15px;
  font-weight: 600;
  color: #202124;
}

.topbar-member-count {
  font-size: 12px;
  color: #9aa0a6;
  font-weight: 400;
}

.topbar-dissolved-tag {
  font-size: 11px;
  color: #ea4335;
  background: rgba(234, 67, 53, 0.1);
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 500;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.settings-btn {
  color: #5f6368;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.settings-btn:hover {
  color: #1a73e8;
}

/* 群聊解散提示条 */
.dissolved-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  background: rgba(234, 67, 53, 0.06);
  border-bottom: 1px solid rgba(234, 67, 53, 0.15);
  color: #ea4335;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
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

.uploaded-docs-section {
  margin-top: 12px;
  border-top: 1px solid #e8eaed;
  padding-top: 12px;
}
.uploaded-docs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.uploaded-docs-title {
  font-size: 13px;
  font-weight: 600;
  color: #3c4043;
}
.uploaded-docs-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
}
.uploaded-docs-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #f8f9fa;
  border-radius: 6px;
  font-size: 12px;
  color: #3c4043;
}
.uploaded-docs-item .el-icon {
  color: #1a73e8;
  flex-shrink: 0;
}
.uploaded-docs-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.uploaded-docs-meta {
  color: #80868b;
  font-size: 11px;
  flex-shrink: 0;
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
