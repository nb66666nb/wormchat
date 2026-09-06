<template>
  <div class="chat-page">
    <!-- 左侧会话列表 -->
    <div class="conversation-panel">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-icon :size="16" color="#b0b0b0"><Search /></el-icon>
        <input
          v-model="searchText"
          type="text"
          placeholder="搜索"
          class="search-input"
        />
      </div>

      <!-- 添加机器人按钮 -->
      <div class="add-bot-btn" @click="showBotPanel = true">
        <el-icon :size="16"><Plus /></el-icon>
        <span>添加机器人</span>
      </div>

      <!-- 创建群聊按钮 -->
      <div class="add-group-btn" @click="showGroupPanel = true">
        <el-icon :size="16"><UserFilled /></el-icon>
        <span>创建群聊</span>
      </div>

      <div class="list-body">
        <div v-if="filteredConversations.length === 0" class="empty-state">
          <p>{{ searchText ? '无搜索结果' : '暂无会话' }}</p>
        </div>

        <div
          v-for="conv in filteredConversations"
          :key="conv.sessionId"
          :class="['conversation-item', { active: selectedUserId === conv.userId, top: conv.isTop }]"
          @click="handleSelect(conv)"
          @contextmenu.prevent="handleContextMenu($event, conv)"
        >
          <div class="conv-avatar-wrap">
            <ChatAvatar
              :user-id="conv.userId"
              :file-id="conv.avatarFileId"
              :file-path="conv.avatarFilePath"
              :user-name="conv.userName"
              :size="44"
            />
            <span v-if="conv.unreadCount > 0" class="unread-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
          </div>
          <div class="conv-content">
            <div class="conv-row">
              <div class="conv-title-group">
                <span class="conv-name">{{ conv.userName }}</span>
                <span v-if="isGroupSession(conv)" class="type-badge group-badge">群聊</span>
                <span v-if="isBotSession(conv)" class="type-badge bot-badge">机器人</span>
                <span v-if="conv.isTop" class="top-badge">置顶</span>
              </div>
              <span class="conv-time">{{ formatTime(conv.lastTime) }}</span>
            </div>
            <div class="conv-preview-row">
              <span class="conv-preview">{{ getPreviewText(conv.lastMessage) }}</span>
              <span v-if="conv.muted" class="muted-icon">🔕</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天面板 -->
    <div class="chat-area">
      <ImChatPanel v-if="selectedUser" :conversation="selectedUser" ref="chatPanelRef" />
      <div v-else class="empty-hint">
        <div class="empty-icon">
          <el-icon :size="56" color="#c0c0c0"><ChatDotRound /></el-icon>
        </div>
        <p>选择一个会话开始聊天</p>
      </div>
    </div>

    <!-- 右键菜单 -->
    <Teleport to="body">
      <div
        v-if="contextMenu.visible"
        class="context-menu"
        :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
      >
        <div class="context-menu-item" @click="handleToggleTop">
          <el-icon><Top /></el-icon>
          <span>{{ contextMenu.conv?.isTop ? '取消置顶' : '置顶聊天' }}</span>
        </div>
        <div class="context-menu-item" @click="handleHideSession">
          <el-icon><Hide /></el-icon>
          <span>隐藏聊天</span>
        </div>
        <div class="context-menu-item danger" @click="handleDeleteSession">
          <el-icon><Delete /></el-icon>
          <span>删除聊天</span>
        </div>
      </div>
    </Teleport>

    <!-- 机器人配置面板 -->
    <Teleport to="body">
      <Transition name="bot-panel">
        <div v-if="showBotPanel" class="bot-panel-overlay" @click.self="showBotPanel = false">
          <div class="bot-panel">
            <div class="bot-panel-header">
              <h3>创建 AI 机器人</h3>
              <button class="close-btn" @click="showBotPanel = false">
                <el-icon :size="18"><Close /></el-icon>
              </button>
            </div>

            <div class="bot-panel-body">
              <!-- 机器人类型 -->
              <div class="form-item">
                <label>机器人类型</label>
                <el-select v-model="botForm.category" placeholder="请选择机器人类型" style="width: 100%">
                  <el-option label="功能型 - 通话助手/日程管理/知识问答" value="FUNCTIONAL" />
                  <el-option label="陪伴型 - 情感陪伴/树洞倾听/成长伙伴" value="COMPANION" />
                  <el-option label="混合型 - 私人秘书/职场教练" value="HYBRID" />
                </el-select>
              </div>

              <!-- ─── 基础配置 ─── -->
              <div class="section-divider">基础配置</div>

              <!-- 头像上传 -->
              <div class="form-item">
                <label>机器人头像</label>
                <div class="avatar-upload">
                  <el-upload
                    :show-file-list="false"
                    :before-upload="validateAvatar"
                    :auto-upload="false"
                    accept="image/*"
                    @change="handleAvatarChange"
                  >
                    <div v-if="!botForm.avatarPreview" class="avatar-placeholder">
                      <el-icon :size="28" color="#9aa0a6"><Camera /></el-icon>
                      <span>上传头像</span>
                    </div>
                    <img v-else :src="botForm.avatarPreview" class="avatar-preview-img" />
                  </el-upload>
                  <p class="upload-hint">支持 JPG、PNG，建议 200x200</p>
                </div>
              </div>

              <!-- 角色名称 -->
              <div class="form-item">
                <label>角色名称</label>
                <el-input
                  v-model="botForm.name"
                  placeholder="给机器人起个名字"
                  maxlength="20"
                  show-word-limit
                  clearable
                />
              </div>

              <!-- 角色描述 -->
              <div class="form-item">
                <label>角色描述</label>
                <el-input
                  v-model="botForm.description"
                  type="textarea"
                  :rows="3"
                  placeholder="描述机器人的角色、能力和行为特点..."
                  maxlength="500"
                  show-word-limit
                  resize="none"
                />
              </div>

              <!-- 系统提示词（可选） -->
              <div class="form-item">
                <label>系统提示词 <span class="optional">(可选)</span></label>
                <el-input
                  v-model="botForm.systemPrompt"
                  type="textarea"
                  :rows="3"
                  placeholder="定义机器人的回复风格和约束条件..."
                  maxlength="1000"
                  show-word-limit
                  resize="none"
                />
              </div>

              <!-- 欢迎语 -->
              <div class="form-item">
                <label>欢迎语</label>
                <el-input
                  v-model="botForm.welcomeMessage"
                  placeholder="用户首次对话时发送的欢迎消息"
                  maxlength="100"
                  show-word-limit
                  clearable
                />
              </div>

              <!-- ─── 性格调参 ─── -->
              <div class="section-divider" @click="showPersonality = !showPersonality" style="cursor: pointer">
                性格调参（高级）
                <el-icon :size="12" :class="{ 'rotate-icon': showPersonality }"><ArrowRight /></el-icon>
              </div>

              <Transition name="collapse">
                <div v-if="showPersonality" class="personality-section">
                  <div class="slider-item">
                    <div class="slider-label">
                      <span>开放度</span>
                      <span class="slider-value">{{ botForm.openness }}</span>
                    </div>
                    <el-slider v-model="botForm.openness" :min="0" :max="1" :step="0.1" :show-tooltip="false" />
                  </div>
                  <div class="slider-item">
                    <div class="slider-label">
                      <span>外向度</span>
                      <span class="slider-value">{{ botForm.extraversion }}</span>
                    </div>
                    <el-slider v-model="botForm.extraversion" :min="0" :max="1" :step="0.1" :show-tooltip="false" />
                  </div>
                  <div class="slider-item">
                    <div class="slider-label">
                      <span>亲和度</span>
                      <span class="slider-value">{{ botForm.agreeableness }}</span>
                    </div>
                    <el-slider v-model="botForm.agreeableness" :min="0" :max="1" :step="0.1" :show-tooltip="false" />
                  </div>
                  <div class="slider-item">
                    <div class="slider-label">
                      <span>幽默度</span>
                      <span class="slider-value">{{ botForm.humor }}</span>
                    </div>
                    <el-slider v-model="botForm.humor" :min="0" :max="1" :step="0.1" :show-tooltip="false" />
                  </div>
                </div>
              </Transition>

              <!-- ─── 陪伴型专属 ─── -->
              <Transition name="collapse">
                <div v-if="botForm.category === 'COMPANION' || botForm.category === 'HYBRID'">
                  <div class="section-divider companion-divider">陪伴型专属</div>
                  <div class="personality-section">
                    <div class="slider-item">
                      <div class="slider-label">
                        <span>共情能力</span>
                        <span class="slider-value">{{ botForm.empathyLevel }}</span>
                      </div>
                      <el-slider v-model="botForm.empathyLevel" :min="0" :max="1" :step="0.1" :show-tooltip="false" />
                    </div>
                    <div class="slider-item">
                      <div class="slider-label">
                        <span>情感敏感度</span>
                        <span class="slider-value">{{ botForm.emotionalSensitivity }}</span>
                      </div>
                      <el-slider v-model="botForm.emotionalSensitivity" :min="0" :max="1" :step="0.1" :show-tooltip="false" />
                    </div>
                    <div class="slider-item">
                      <div class="slider-label">
                        <span>主动性</span>
                        <span class="slider-value">{{ botForm.proactivity }}</span>
                      </div>
                      <el-slider v-model="botForm.proactivity" :min="0" :max="1" :step="0.1" :show-tooltip="false" />
                    </div>
                    <div class="checkbox-group">
                      <el-checkbox v-model="botForm.greetingEnabled">启用定时问候</el-checkbox>
                      <el-checkbox v-model="botForm.proactiveCareEnabled">启用主动关心</el-checkbox>
                      <el-checkbox v-model="botForm.followUpEnabled">启用话题跟进</el-checkbox>
                    </div>
                  </div>
                </div>
              </Transition>

              <!-- ─── 功能配置 ─── -->
              <Transition name="collapse">
                <div v-if="botForm.category === 'FUNCTIONAL' || botForm.category === 'HYBRID'">
                  <div class="section-divider functional-divider">功能配置</div>
                  <div class="personality-section">
                    <div class="checkbox-group">
                      <el-checkbox v-model="botForm.toolMeetingQuery">通话查询</el-checkbox>
                      <el-checkbox v-model="botForm.toolScheduleManage">日程管理</el-checkbox>
                      <el-checkbox v-model="botForm.toolDataAnalysis">数据分析</el-checkbox>
                    </div>
                  </div>
                </div>
              </Transition>
            </div>

            <div class="bot-panel-footer">
              <el-button @click="showBotPanel = false">取消</el-button>
              <el-button type="primary" @click="handleCreateBot" :disabled="!isBotFormValid">创建机器人</el-button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- 创建群聊对话框 -->
    <Teleport to="body">
      <Transition name="bot-panel">
        <div v-if="showGroupPanel" class="bot-panel-overlay" @click.self="showGroupPanel = false">
          <div class="bot-panel" style="width: 420px">
            <div class="bot-panel-header">
              <h3>创建群聊</h3>
              <button class="close-btn" @click="showGroupPanel = false">
                <el-icon :size="18"><Close /></el-icon>
              </button>
            </div>

            <div class="bot-panel-body">
              <!-- 群名称 -->
              <div class="form-item">
                <label>群名称</label>
                <el-input
                  v-model="groupForm.name"
                  placeholder="请输入群名称"
                  maxlength="30"
                  show-word-limit
                  clearable
                />
              </div>

              <!-- 群公告 -->
              <div class="form-item">
                <label>群公告 <span class="optional-tag">可选</span></label>
                <el-input
                  v-model="groupForm.announcement"
                  type="textarea"
                  placeholder="请输入群公告内容"
                  maxlength="200"
                  show-word-limit
                  :autosize="{ minRows: 2, maxRows: 4 }"
                />
              </div>

              <!-- 邀请权限 -->
              <div class="form-item form-item-inline">
                <div class="inline-label">
                  <span>允许普通成员邀请好友入群</span>
                  <span class="inline-desc">关闭后仅群主和管理员可邀请</span>
                </div>
                <el-switch v-model="groupForm.allowMemberInvite" />
              </div>

              <!-- 选择好友 -->
              <div class="form-item">
                <label>选择群成员（{{ groupForm.selectedIds.length }}人）</label>
                <div v-if="friendList.length === 0" class="empty-friends">
                  <span>暂无好友，请先添加好友</span>
                </div>
                <div v-else class="friend-select-list">
                  <div
                    v-for="friend in friendList"
                    :key="friend.friendId"
                    :class="['friend-select-item', { selected: groupForm.selectedIds.includes(friend.friendId) }]"
                    @click="toggleSelectFriend(friend.friendId)"
                  >
                    <ChatAvatar
                      :user-id="friend.friendId"
                      :file-id="friend.avatarFileId"
                      :file-path="friend.avatarFilePath"
                      :user-name="getFriendDisplayName(friend)"
                      :size="32"
                    />
                    <span class="friend-name">{{ getFriendDisplayName(friend) }}</span>
                    <el-icon v-if="groupForm.selectedIds.includes(friend.friendId)" class="check-icon" :size="16"><Check /></el-icon>
                  </div>
                </div>
              </div>
            </div>

            <div class="bot-panel-footer">
              <el-button @click="showGroupPanel = false">取消</el-button>
              <el-button type="primary" @click="handleCreateGroup" :disabled="!isGroupFormValid">创建群聊</el-button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { Search, ChatDotRound, Plus, Close, Camera, ArrowRight, Top, Hide, Delete, UserFilled, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatAvatar from '@/components/ChatAvatar.vue'
import ImChatPanel from './ImChatPanel.vue'
import Request from '@/utils/Request'
import Api from '@/utils/Api'

const selectedUserId = ref('')
const selectedUser = ref(null)
const chatPanelRef = ref(null)
const searchText = ref('')
const showBotPanel = ref(false)
const showPersonality = ref(false)
const showGroupPanel = ref(false)

// 群聊表单
const groupForm = reactive({
  name: '',
  announcement: '',
  allowMemberInvite: true,
  selectedIds: []
})
const friendList = ref([])

const route = useRoute()

// 当前登录用户ID
const currentUserId = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return info.userId || ''
  } catch (_) { return '' }
})

// 会话列表（从本地数据库加载）
const conversations = ref([])

// 右键菜单状态
const contextMenu = reactive({
  visible: false,
  x: 0,
  y: 0,
  conv: null
})

/** 从本地数据库加载会话列表 */
const loadSessionList = async () => {
  const userId = currentUserId.value
  if (!userId) return
  try {
    const list = await window.api.dbSessionList(userId)
    conversations.value = (list || []).map(s => {
      // 解析 botAvatarPath 形如 "/avatar/2026/06/30/abc123.png" -> { fileId: "abc123", filePath: "/avatar/..." }
      // 注意：ChatAvatar 需要 fileId 才能命中 downloadAvatar 的缓存查找
      let avatarFileId = ''
      let avatarFilePath = ''
      if (s.botAvatarPath && typeof s.botAvatarPath === 'string') {
        avatarFilePath = s.botAvatarPath
        const lastSlash = s.botAvatarPath.lastIndexOf('/')
        const fileName = lastSlash >= 0 ? s.botAvatarPath.substring(lastSlash + 1) : s.botAvatarPath
        const dotIdx = fileName.lastIndexOf('.')
        avatarFileId = dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName
      }
      return {
        sessionId: s.sessionId,
        userId: s.targetUserId,
        userName: s.targetNickName || s.targetUserId,
        avatarFileId,
        avatarFilePath,
        lastMessage: s.lastMessage || '',
        lastTime: s.lastMessageTime || 0,
        unreadCount: s.unreadCount || 0,
        muted: false,
        isTop: s.isTop === 1,
        sessionType: s.sessionType,
        botCategory: s.botCategory,
        botAvatarPath: s.botAvatarPath || ''
      }
    })
  } catch (e) {
    console.error('[ChatPage] 加载会话列表失败:', e.message)
  }
}

/** 从路由 query 读取 targetUserId 并选中对应会话（通讯录"聊天"按钮跳转） */
const selectConversationByTarget = () => {
  const targetUserId = route.query.targetUserId
  if (!targetUserId) return
  const target = String(targetUserId)
  const conv = conversations.value.find((c) => String(c.userId) === target)
  if (conv) {
    handleSelect(conv)
  } else {
    console.warn('[ChatPage] 未找到目标会话: targetUserId=', target, '（会话可能未同步或已被删除）')
  }
}

/** 监听会话系统事件（懒刷新） */
let removeSessionListener = null
const setupSessionEventListener = () => {
  if (window.messageAPI?.onSessionEvent) {
    removeSessionListener = window.messageAPI.onSessionEvent(() => {
      loadSessionList()
    })
  }
}

/** 监听 IM 新消息：收到消息后刷新会话列表，让被软删的会话重新显现 */
let removeImMessageListener = null
const setupImMessageListener = () => {
  if (window.messageAPI?.onNewMessage) {
    removeImMessageListener = window.messageAPI.onNewMessage((msg) => {
      // 仅处理 IM 消息（messageTypeIm=0），且消息属于某个会话
      if (msg.messageTypeIm !== 0 || !msg.sessionId) return
      // 主进程已经通过 markActiveBySessionId 清除软删除，这里只需触发 UI 重新拉取
      loadSessionList()
    })
  }
}

// 机器人表单
const botForm = reactive({
  category: 'FUNCTIONAL',
  name: '',
  description: '',
  systemPrompt: '',
  welcomeMessage: '',
  avatarFile: null,
  avatarPreview: '',
  // 性格调参
  openness: 0.5,
  extraversion: 0.5,
  agreeableness: 0.5,
  humor: 0.5,
  // 陪伴型专属
  empathyLevel: 0.8,
  emotionalSensitivity: 0.7,
  proactivity: 0.6,
  greetingEnabled: true,
  proactiveCareEnabled: true,
  followUpEnabled: true,
  // 功能配置
  toolMeetingQuery: true,
  toolScheduleManage: true,
  toolDataAnalysis: false
})

// 表单验证
const isBotFormValid = computed(() => {
  return botForm.name.trim().length > 0 &&
         botForm.description.trim().length > 0 &&
         botForm.category !== ''
})

// 构建personality字符串（传给后端）
const buildPersonality = () => {
  const parts = []
  parts.push(`openness:${botForm.openness}`)
  parts.push(`extraversion:${botForm.extraversion}`)
  parts.push(`agreeableness:${botForm.agreeableness}`)
  parts.push(`humor:${botForm.humor}`)

  if (botForm.category === 'COMPANION' || botForm.category === 'HYBRID') {
    parts.push(`empathy:${botForm.empathyLevel}`)
    parts.push(`sensitivity:${botForm.emotionalSensitivity}`)
    parts.push(`proactivity:${botForm.proactivity}`)
    parts.push(`greeting:${botForm.greetingEnabled}`)
    parts.push(`proactiveCare:${botForm.proactiveCareEnabled}`)
    parts.push(`followUp:${botForm.followUpEnabled}`)
  }

  if (botForm.category === 'FUNCTIONAL' || botForm.category === 'HYBRID') {
    const tools = []
    if (botForm.toolMeetingQuery) tools.push('meetingQuery')
    if (botForm.toolScheduleManage) tools.push('scheduleManage')
    if (botForm.toolDataAnalysis) tools.push('dataAnalysis')
    parts.push(`tools:${tools.join(',')}`)
  }

  return parts.join(';')
}

// 头像校验（before-upload：合法返回 true，非法返回 false = 拒绝该文件）
const validateAvatar = (file) => {
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请上传图片文件')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return false
  }
  return true // 允许，后续由 @change 捕获 + handleCreateBot 上传
}

// 头像选中事件（@change）：从 el-upload 的 uploadFile.raw 拿到原始 File
const handleAvatarChange = (uploadFile) => {
  const file = uploadFile?.raw
  if (!file) return

  // 释放旧的 blob URL，避免内存泄漏
  if (botForm.avatarPreview && botForm.avatarPreview.startsWith('blob:')) {
    try { URL.revokeObjectURL(botForm.avatarPreview) } catch (_) {}
  }
  botForm.avatarFile = file
  botForm.avatarPreview = URL.createObjectURL(file)
  console.log('[ChatPage] 头像已选择:', file.name, `${Math.round(file.size / 1024)}KB`)
}

// 重置机器人表单
const resetBotForm = () => {
  Object.assign(botForm, {
    category: 'FUNCTIONAL',
    name: '',
    description: '',
    systemPrompt: '',
    welcomeMessage: '',
    avatarFile: null,
    avatarPreview: '',
    openness: 0.5,
    extraversion: 0.5,
    agreeableness: 0.5,
    humor: 0.5,
    empathyLevel: 0.8,
    emotionalSensitivity: 0.7,
    proactivity: 0.6,
    greetingEnabled: true,
    proactiveCareEnabled: true,
    followUpEnabled: true,
    toolMeetingQuery: true,
    toolScheduleManage: true,
    toolDataAnalysis: false
  })
  showPersonality.value = false
}

// 创建机器人（先上传头像，再调用后端 /chatSession/createRobot 接口）
const handleCreateBot = async () => {
  if (!isBotFormValid.value) {
    ElMessage.warning('请填写机器人类型、角色名称和描述')
    return
  }

  // 1. 若选择了头像，先把文件上传到后端 /api/avatar/upload，拿到 filePath / fileId
  let uploadedAvatar = null
  if (botForm.avatarFile) {
    try {
      const arrayBuffer = await botForm.avatarFile.arrayBuffer()
      const fileBuffer = Array.from(new Uint8Array(arrayBuffer))
      const result = await window.api.uploadAvatar({
        userId: currentUserId.value,
        fileBuffer,
        fileName: botForm.avatarFile.name || `bot_avatar_${Date.now()}.png`,
        scope: 'bot'
      })
      if (result && result.success && result.data) {
        uploadedAvatar = {
          fileId: result.data.fileId,
          filePath: result.data.filePath,
          localPath: result.data.localPath
        }
        console.log('[ChatPage] 机器人头像上传成功:', uploadedAvatar)
      } else {
        ElMessage.warning('头像上传失败，将跳过头像创建机器人')
      }
    } catch (e) {
      console.error('[ChatPage] 头像上传异常:', e)
      ElMessage.warning('头像上传失败: ' + e.message)
    }
  }

  // 2. 构建符合后端 CreateRobotDto 字段名的请求参数
  //    性格参数由后端 buildPersonalityString() 自动构建，前端只需传原始字段
  //    注意：若用户选择了头像但上传失败，botAvatarPath 留空，避免把 blob URL 发到后端
  const params = {
    category: botForm.category,
    botName: botForm.name,
    botAvatarPath: uploadedAvatar?.filePath || '',
    botAvatarFileId: uploadedAvatar?.fileId || '',
    botDescription: botForm.description,
    botSystemPrompt: botForm.systemPrompt || '',
    botWelcomeMsg: botForm.welcomeMessage || `你好！我是${botForm.name}`,
    templateId: '',
    // 性格调参
    openness: botForm.openness,
    extraversion: botForm.extraversion,
    agreeableness: botForm.agreeableness,
    humor: botForm.humor,
    // 陪伴型专属
    empathyLevel: botForm.empathyLevel,
    emotionalSensitivity: botForm.emotionalSensitivity,
    proactivity: botForm.proactivity,
    greetingEnabled: botForm.greetingEnabled,
    proactiveCareEnabled: botForm.proactiveCareEnabled,
    followUpEnabled: botForm.followUpEnabled,
    // 功能配置
    toolMeetingQuery: botForm.toolMeetingQuery,
    toolScheduleManage: botForm.toolScheduleManage,
    toolDataAnalysis: botForm.toolDataAnalysis
  }

  // 3. 后端使用 @RequestBody，需要 JSON 内容类型
  const result = await Request({
    url: Api.createRobot,
    params,
    json: true,
    showError: false
  })

  // 无论成功失败，都关闭面板并重置表单
  showBotPanel.value = false
  resetBotForm()

  if (result && result.code === 200) {
    ElMessage.success(`机器人 "${params.botName}" 创建成功`)
    // 不本地 unshift：后端通过 WebSocket 推送会话创建事件，
    // wsClient 会自动镜像到 SQLite，ChatPage 的 session-event / new-message
    // 监听器会触发 loadSessionList() 刷新列表
  } else {
    ElMessage.error(result?.info || '创建机器人失败，请稍后重试')
  }
}

// ==================== 群聊相关 ====================

const isGroupFormValid = computed(() => {
  return groupForm.name.trim().length > 0 && groupForm.selectedIds.length > 0
})

/** 获取好友显示名称（优先备注名，其次昵称） */
const getFriendDisplayName = (friend) => {
  if (friend.remarkName && friend.remarkName.trim()) return friend.remarkName
  return friend.nickName || friend.friendId
}

/** 加载非机器人好友列表 */
const loadFriendList = async () => {
  try {
    const res = await Request({
      url: Api.getNonBotFriends,
      json: true,
      showLoading: false,
      showError: false
    })
    if (res && res.code === 200) {
      friendList.value = res.data || []
    }
  } catch (e) {
    console.error('[ChatPage] 加载好友列表失败:', e.message)
  }
}

/** 切换好友选中状态 */
const toggleSelectFriend = (friendId) => {
  const idx = groupForm.selectedIds.indexOf(friendId)
  if (idx === -1) {
    groupForm.selectedIds.push(friendId)
  } else {
    groupForm.selectedIds.splice(idx, 1)
  }
}

/** 创建群聊 */
const handleCreateGroup = async () => {
  if (!isGroupFormValid.value) {
    ElMessage.warning('请输入群名称并至少选择一名好友')
    return
  }

  const result = await Request({
    url: Api.createGroup,
    params: {
      groupName: groupForm.name.trim(),
      creatorUserId: currentUserId.value,
      memberIds: groupForm.selectedIds,
      announcement: groupForm.announcement.trim(),
      invitePermission: groupForm.allowMemberInvite ? 0 : 1
    },
    json: true,
    showError: false
  })

  showGroupPanel.value = false

  if (result && result.code === 200) {
    ElMessage.success(`群聊 "${groupForm.name}" 创建成功`)
  } else {
    ElMessage.error(result?.info || '创建群聊失败，请稍后重试')
  }

  // 重置表单
  groupForm.name = ''
  groupForm.announcement = ''
  groupForm.allowMemberInvite = true
  groupForm.selectedIds = []
}

/** 监听群聊面板打开，加载好友列表 */
import { watch } from 'vue'
watch(showGroupPanel, (val) => {
  if (val) loadFriendList()
})

const filteredConversations = computed(() => {
  // 按 sessionId 去重（防止脏数据导致同一会话显示多次）
  const seen = new Set()
  const unique = conversations.value.filter(conv => {
    const key = conv.sessionId || conv.userId
    if (!key || seen.has(key)) return false
    seen.add(key)
    return true
  })
  if (!searchText.value.trim()) {
    return [...unique].sort((a, b) => {
      // 置顶会话始终在前
      if (a.isTop !== b.isTop) return a.isTop ? -1 : 1
      return (b.lastTime || 0) - (a.lastTime || 0)
    })
  }
  const keyword = searchText.value.trim().toLowerCase()
  return unique.filter(conv =>
    conv.userName.toLowerCase().includes(keyword) ||
    (conv.lastMessage && conv.lastMessage.toLowerCase().includes(keyword))
  ).sort((a, b) => {
    if (a.isTop !== b.isTop) return a.isTop ? -1 : 1
    return (b.lastTime || 0) - (a.lastTime || 0)
  })
})

const getPreviewText = (msg) => {
  if (!msg) return ''
  const maxLen = 30
  return msg.length > maxLen ? msg.slice(0, maxLen) + '...' : msg
}

const formatTime = (ts) => {
  if (!ts) return ''
  const now = new Date()
  const date = new Date(ts)
  const isToday = now.toDateString() === date.toDateString()

  if (isToday) {
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
  }

  const yesterday = new Date(now)
  yesterday.setDate(yesterday.getDate() - 1)
  if (yesterday.toDateString() === date.toDateString()) {
    return '昨天'
  }

  const diffDays = Math.floor((now - ts) / 86400000)
  if (diffDays < 7) {
    const weekDays = ['日', '一', '二', '三', '四', '五', '六']
    return `周${weekDays[date.getDay()]}`
  }

  return `${date.getMonth() + 1}/${date.getDate()}`
}

// 会话类型标识：群聊 / 机器人（普通用户无需标识）
const isGroupSession = (conv) => Number(conv?.sessionType) === 1
const isBotSession = (conv) => Number(conv?.sessionType) === 2

const handleSelect = (conv) => {
  selectedUserId.value = conv.userId
  selectedUser.value = conv
  // 打开会话时清零未读数
  if (conv.sessionId && conv.unreadCount > 0) {
    window.api.dbSessionResetUnread(conv.sessionId)
    conv.unreadCount = 0
  }
}

// ===== 右键菜单 =====
const handleContextMenu = (e, conv) => {
  contextMenu.visible = true
  contextMenu.x = e.clientX
  contextMenu.y = e.clientY
  contextMenu.conv = conv
}

const closeContextMenu = () => {
  contextMenu.visible = false
  contextMenu.conv = null
}

// 点击页面其他地方关闭右键菜单
const onPageClick = () => {
  if (contextMenu.visible) closeContextMenu()
}

// 置顶 / 取消置顶
const handleToggleTop = async () => {
  const conv = contextMenu.conv
  if (!conv || !conv.sessionId) return
  try {
    const newTop = !conv.isTop
    await window.api.dbSessionToggleTop(conv.sessionId, newTop)
    ElMessage.success(newTop ? '已置顶' : '已取消置顶')
    closeContextMenu()
    await loadSessionList()
  } catch (e) {
    console.error('[ChatPage] 置顶失败:', e.message)
    ElMessage.error('操作失败')
  }
}

// 隐藏聊天
const handleHideSession = async () => {
  const conv = contextMenu.conv
  if (!conv || !conv.sessionId) return
  try {
    await window.api.dbSessionHide(conv.sessionId)
    ElMessage.success('已隐藏')
    closeContextMenu()
    if (selectedUserId.value === conv.userId) {
      selectedUserId.value = ''
      selectedUser.value = null
    }
    await loadSessionList()
  } catch (e) {
    console.error('[ChatPage] 隐藏失败:', e.message)
    ElMessage.error('操作失败')
  }
}

// 删除聊天
const handleDeleteSession = async () => {
  const conv = contextMenu.conv
  if (!conv || !conv.sessionId) return
  try {
    await ElMessageBox.confirm(
      `确定要删除与 "${conv.userName}" 的聊天记录吗？此操作不可恢复。`,
      '删除聊天',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await window.api.dbSessionDestroy(conv.sessionId)
    ElMessage.success('已删除')
    closeContextMenu()
    if (selectedUserId.value === conv.userId) {
      selectedUserId.value = ''
      selectedUser.value = null
    }
    await loadSessionList()
  } catch (e) {
    if (e === 'cancel' || e === 'close') {
      closeContextMenu()
      return
    }
    console.error('[ChatPage] 删除失败:', e.message)
    ElMessage.error('删除失败')
  }
}

onMounted(async () => {
  await loadSessionList()
  selectConversationByTarget()
  setupSessionEventListener()
  setupImMessageListener()
  document.addEventListener('click', onPageClick)
})

onUnmounted(() => {
  if (removeSessionListener) {
    removeSessionListener()
    removeSessionListener = null
  }
  if (removeImMessageListener) {
    removeImMessageListener()
    removeImMessageListener = null
  }
  document.removeEventListener('click', onPageClick)
})
</script>

<style lang="css" scoped>
.chat-page {
  width: 100%;
  height: 100%;
  display: flex;
  background: #f8f9fa;
}

/* ===== 会话列表面板 ===== */
.conversation-panel {
  width: 240px;
  min-width: 240px;
  height: 100%;
  background: #ffffff;
  border-right: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* 搜索栏 */
.search-bar {
  margin: 0 10px 6px;
  padding: 7px 14px;
  background: #f1f3f4;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.search-bar:focus-within {
  background: #ffffff;
  box-shadow: 0 0 0 1px #1a73e8;
}

.search-input {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 13px;
  color: #202124;
  outline: none;
  line-height: 20px;
}

.search-input::placeholder {
  color: #9aa0a6;
}

/* 列表区域 */
.list-body {
  flex: 1;
  overflow-y: auto;
  padding: 0 6px;
}

.list-body::-webkit-scrollbar {
  width: 4px;
}

.list-body::-webkit-scrollbar-thumb {
  background-color: #dadce0;
  border-radius: 3px;
}

.empty-state {
  display: flex;
  justify-content: center;
  padding: 80px 20px;
}

.empty-state p {
  font-size: 13px;
  color: #9aa0a6;
  margin: 0;
}

/* 会话项 - 统一高度 56px */
.conversation-item {
  display: flex;
  align-items: center;
  gap: 11px;
  height: 56px;
  padding: 0 10px;
  cursor: pointer;
  transition: background 0.15s ease;
  border-radius: 10px;
}

.conversation-item:hover {
  background: #f1f3f4;
}

.conversation-item.active {
  background: rgba(26, 115, 232, 0.08);
}

.conversation-item.top {
  background: rgba(26, 115, 232, 0.04);
}

.top-badge {
  font-size: 10px;
  color: #1a73e8;
  background: rgba(26, 115, 232, 0.08);
  border: 1px solid rgba(26, 115, 232, 0.2);
  border-radius: 3px;
  padding: 1px 4px;
  margin-left: 6px;
  flex-shrink: 0;
  line-height: 1.4;
}

/* 右键菜单 */
.context-menu {
  position: fixed;
  z-index: 9999;
  min-width: 140px;
  background: #ffffff;
  border: 1px solid #e8eaed;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  padding: 4px 0;
  user-select: none;
}

.context-menu-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 14px;
  font-size: 13px;
  color: #202124;
  cursor: pointer;
  transition: background 0.15s ease;
}

.context-menu-item:hover {
  background: #f1f3f4;
}

.context-menu-item.danger {
  color: #ea4335;
}

.context-menu-item.danger:hover {
  background: rgba(234, 67, 53, 0.08);
}

.conv-avatar-wrap {
  position: relative;
  flex-shrink: 0;
  line-height: 0;
}

.unread-badge {
  position: absolute;
  top: -2px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  background: #ea4335;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  line-height: 1;
}

.conv-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.conv-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  line-height: 18px;
}

.conv-title-group {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.conv-title-group .conv-name {
  flex: 1;
  min-width: 0;
}

.conv-name {
  font-size: 14px;
  font-weight: 500;
  color: #202124;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-badge {
  font-size: 10px;
  border-radius: 3px;
  padding: 1px 4px;
  flex-shrink: 0;
  line-height: 1.4;
}

.group-badge {
  color: #34a853;
  background: rgba(52, 168, 83, 0.08);
  border: 1px solid rgba(52, 168, 83, 0.25);
}

.bot-badge {
  color: #9334e6;
  background: rgba(147, 51, 234, 0.08);
  border: 1px solid rgba(147, 51, 234, 0.25);
}

.conv-time {
  font-size: 11px;
  color: #9aa0a6;
  flex-shrink: 0;
  margin-left: 8px;
}

.conv-preview-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.conv-preview {
  font-size: 12px;
  color: #5f6368;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.muted-icon {
  font-size: 11px;
  flex-shrink: 0;
  opacity: 0.5;
}

/* ===== 聊天区域 ===== */
.chat-area {
  flex: 1;
  height: 100%;
  min-width: 0;
  background: #ffffff;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
}

.empty-icon {
  opacity: 0.25;
}

.empty-hint p {
  margin: 0;
  font-size: 13px;
  color: #9aa0a6;
}

/* ===== 添加机器人按钮 ===== */
.add-bot-btn {
  margin: 0 10px 8px;
  padding: 8px 14px;
  background: rgba(26, 115, 232, 0.06);
  border: 1px dashed #1a73e8;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #1a73e8;
  font-size: 13px;
  font-weight: 500;
}

.add-bot-btn:hover {
  background: rgba(26, 115, 232, 0.12);
  border-color: #1557b0;
}

/* ===== 创建群聊按钮 ===== */
.add-group-btn {
  margin: 0 10px 8px;
  padding: 8px 14px;
  background: rgba(52, 168, 83, 0.06);
  border: 1px dashed #34a853;
  border-radius: 10px;
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #34a853;
  font-size: 13px;
  font-weight: 500;
}

.add-group-btn:hover {
  background: rgba(52, 168, 83, 0.12);
  border-color: #2d8e47;
}

/* ===== 机器人配置面板 ===== */
.bot-panel-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.bot-panel {
  width: 460px;
  max-height: 85vh;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.bot-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;

  h3 {
    font-size: 17px;
    font-weight: 600;
    color: #202124;
    margin: 0;
  }
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #5f6368;
  transition: all 0.2s ease;

  &:hover {
    background: #f1f3f4;
    color: #202124;
  }
}

.bot-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;

  &::-webkit-scrollbar { width: 5px; }
  &::-webkit-scrollbar-thumb { background-color: #dadce0; border-radius: 3px; }
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;

  label {
    font-size: 13px;
    font-weight: 600;
    color: #202124;
  }

  .optional {
    font-weight: 400;
    color: #9aa0a6;
  }

  .optional-tag {
    font-weight: 400;
    font-size: 12px;
    color: #9aa0a6;
    margin-left: 4px;
  }
}

.form-item-inline {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f8f9fa;
  border-radius: 8px;

  .inline-label {
    display: flex;
    flex-direction: column;
    gap: 2px;

    span:first-child {
      font-size: 13px;
      font-weight: 600;
      color: #202124;
    }

    .inline-desc {
      font-size: 12px;
      color: #9aa0a6;
      font-weight: 400;
    }
  }
}

/* 头像上传 */
.avatar-upload {
  display: flex;
  align-items: center;
  gap: 16px;

  .avatar-placeholder,
  :deep(.el-upload) .avatar-preview-img {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    object-fit: cover;
  }

  .avatar-placeholder {
    background: #f1f3f4;
    border: 2px dashed #dadce0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    cursor: pointer;
    transition: all 0.2s ease;

    span { font-size: 11px; color: #9aa0a6; }

    &:hover {
      border-color: #1a73e8;
      background: rgba(26, 115, 232, 0.04);
    }
  }

  .upload-hint {
    font-size: 11px;
    color: #9aa0a6;
    margin: 0;
  }

  :deep(.el-upload) {
    line-height: 0;
  }
}

.bot-panel-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px 20px;
  border-top: 1px solid #e8eaed;
  flex-shrink: 0;
}

/* 面板过渡动画 */
.bot-panel-enter-active,
.bot-panel-leave-active {
  transition: all 0.25s ease;
}

.bot-panel-enter-from,
.bot-panel-leave-to {
  opacity: 0;
}

.bot-panel-enter-from .bot-panel,
.bot-panel-leave-to .bot-panel {
  transform: scale(0.95) translateY(20px);
}

/* ===== 分隔线 ===== */
.section-divider {
  font-size: 12px;
  font-weight: 600;
  color: #9aa0a6;
  padding: 10px 0 6px;
  border-top: 1px solid #e8eaed;
  display: flex;
  align-items: center;
  gap: 6px;
  user-select: none;
}

.section-divider .rotate-icon {
  transform: rotate(90deg);
  transition: transform 0.2s ease;
}

.companion-divider {
  color: #e67e22;
  border-top-color: #fdebd0;
}

.functional-divider {
  color: #1a73e8;
  border-top-color: #d2e3fc;
}

/* ===== 性格调参区域 ===== */
.personality-section {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px 0;
}

.slider-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.slider-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #5f6368;
}

.slider-value {
  font-size: 12px;
  color: #1a73e8;
  font-weight: 500;
  min-width: 28px;
  text-align: right;
}

.slider-item :deep(.el-slider__runway) {
  height: 6px;
  border-radius: 3px;
}

.slider-item :deep(.el-slider__bar) {
  height: 6px;
  border-radius: 3px;
}

.slider-item :deep(.el-slider__button-wrapper) {
  top: -16px;
}

.slider-item :deep(.el-slider__button) {
  width: 16px;
  height: 16px;
}

/* ===== 复选框组 ===== */
.checkbox-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px 0;
}

.checkbox-group :deep(.el-checkbox__label) {
  font-size: 13px;
  color: #5f6368;
}

/* ===== 折叠动画 ===== */
.collapse-enter-active,
.collapse-leave-active {
  transition: all 0.25s ease;
  overflow: hidden;
}

.collapse-enter-from,
.collapse-leave-to {
  opacity: 0;
  max-height: 0;
}

.collapse-enter-to,
.collapse-leave-from {
  opacity: 1;
  max-height: 500px;
}

/* ===== 群聊创建对话框 ===== */
.empty-friends {
  padding: 24px;
  text-align: center;
  color: #9aa0a6;
  font-size: 13px;
}

.friend-select-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 280px;
  overflow-y: auto;
  padding: 2px;
}

.friend-select-list::-webkit-scrollbar {
  width: 4px;
}

.friend-select-list::-webkit-scrollbar-thumb {
  background-color: #dadce0;
  border-radius: 2px;
}

.friend-select-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s ease;
  border: 1px solid transparent;
}

.friend-select-item:hover {
  background: #f1f3f4;
}

.friend-select-item.selected {
  background: rgba(52, 168, 83, 0.08);
  border-color: rgba(52, 168, 83, 0.2);
}

.friend-name {
  flex: 1;
  font-size: 13px;
  color: #202124;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.check-icon {
  color: #34a853;
  flex-shrink: 0;
}
</style>
