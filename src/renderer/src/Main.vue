<template>
  <div class="main-container">
    <div class="sidebar">
      <div class="sidebar-header">
        <div class="brand">
          <img src="@/assets/logo.svg" alt="虫聊" class="brand-logo" />
          <h2>虫聊</h2>
        </div>
      </div>
      <div class="nav-menu">
        <div
          class="nav-item chat-nav-btn"
          :class="{ active: currentRoute === 'chat' }"
          @click="goTo('chat')"
        >
          <el-icon><ChatLineRound /></el-icon>
          <span>聊天</span>
        </div>
        <div
          class="nav-item"
          :class="{ active: currentRoute === 'meeting' }"
          @click="goTo('meeting')"
        >
          <el-icon><VideoCamera /></el-icon>
          <span>通话</span>
        </div>
        <div
          class="nav-item"
          :class="{ active: currentRoute === 'contacts' }"
          @click="goTo('contacts')"
        >
          <el-icon><User /></el-icon>
          <span>通讯录</span>
        </div>
        <div
          class="nav-item"
          :class="{ active: currentRoute === 'settings' }"
          @click="goTo('settings')"
        >
          <el-icon><Setting /></el-icon>
          <span>设置</span>
        </div>
      </div>
      <div class="sidebar-footer">
        <div class="footer-avatar" @click="handleAvatarClick">
          <ChatAvatar
            v-if="userId"
            :user-id="userId"
            :file-id="avatarFileId"
            :file-path="avatarFilePath"
            :user-name="userName"
            :size="36"
          />
          <span v-else>{{ avatarText }}</span>
        </div>
        <div class="footer-info">
          <span class="footer-name">{{ userName || '用户' }}</span>
          <span class="footer-id">{{ userId ? 'ID: ' + userId : '未登录' }}</span>
        </div>
      </div>
    </div>
    <div class="content">
      <router-view />
    </div>

    <el-dialog
      v-model="avatarPreviewVisible"
      :show-close="true"
      :close-on-click-modal="true"
      class="avatar-preview-dialog"
      width="auto"
      align-center
      top="5vh"
    >
      <div class="avatar-preview-wrapper">
        <img
          v-if="avatarLocalPath"
          :src="'file://' + (avatarLocalPath || '').replace(/\\/g, '/').replace(/^([^/])/, '/$1')"
          class="avatar-preview-image"
        />
        <div v-else class="avatar-preview-placeholder">
          <el-icon :size="80"><User /></el-icon>
          <p>暂无头像</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { VideoCamera, User, Setting, ChatLineRound } from '@element-plus/icons-vue'
import ChatAvatar from '@/components/ChatAvatar.vue'

const router = useRouter()
const route = useRoute()

const userName = ref('用户')
const userId = ref('')
const avatarFileId = ref('')
const avatarFilePath = ref('')
const avatarLocalPath = ref('')
const avatarPreviewVisible = ref(false)

const currentRoute = computed(() => {
  const path = route.path
  if (path === '/main' || path === '/main/') {
    return 'meeting'
  }
  // 优先匹配顶层路由名（contacts/settings/chat/meeting），处理嵌套路由情况
  const pathWithoutMain = path.replace(/^\/main(\/|$)/, '')
  const topLevelRoutes = ['meeting', 'contacts', 'settings', 'chat']
  for (const route of topLevelRoutes) {
    if (pathWithoutMain === route || pathWithoutMain.startsWith(route + '/')) {
      return route
    }
  }
  const parts = path.split('/')
  return parts[parts.length - 1] || 'meeting'
})

const avatarText = computed(() => {
  const name = userName.value?.trim()
  if (!name) return 'U'
  const ch = name.charAt(0)
  return ch.toUpperCase()
})

const avatarFileUrl = computed(() => {
  if (!avatarLocalPath.value) return ''
  let p = avatarLocalPath.value
  p = p.replace(/\\/g, '/')
  if (!p.startsWith('/')) p = '/' + p
  return 'file://' + p
})

const goTo = (path) => {
  router.push('/main/' + path)
}

const loadUserInfo = () => {
  try {
    const storedInfo = localStorage.getItem('userInfo')
    if (storedInfo) {
      const data = JSON.parse(storedInfo)
      userName.value = data.nickname || data.userName || data.username || '用户'
      userId.value = data.userId || data.id || ''
      avatarFileId.value = data.avatarFileId || data.fileId || ''
      avatarFilePath.value = data.avatarFilePath || data.filePath || ''
      loadAvatar()
    }
  } catch (e) {
    console.warn('读取用户信息失败:', e.message)
  }
}

const loadAvatar = async () => {
  const uid = String(userId.value || '').trim()
  if (!uid && !avatarFileId.value && !avatarFilePath.value) {
    avatarLocalPath.value = ''
    return
  }
  if (!window.api) {
    avatarLocalPath.value = ''
    return
  }

  if (uid && window.api.checkAvatar) {
    try {
      const result = await window.api.checkAvatar({
        userId: uid,
        fileId: avatarFileId.value,
        filePath: avatarFilePath.value
      })
      if (result && result.exists && result.localPath) {
        avatarLocalPath.value = result.localPath
        return
      }
      if (window.api.downloadAvatar && (avatarFileId.value || avatarFilePath.value)) {
        const dl = await window.api.downloadAvatar({
          userId: uid,
          fileId: avatarFileId.value,
          filePath: avatarFilePath.value,
          fileType: 1
        })
        if (dl && dl.success && dl.data && dl.data.localPath) {
          avatarLocalPath.value = dl.data.localPath
          return
        }
      }
    } catch (e) {
      console.warn('按 userId 加载头像失败:', e.message)
    }
  }

  if (window.api.checkMeetingFile && (avatarFileId.value || avatarFilePath.value)) {
    try {
      const result = await window.api.checkMeetingFile({
        fileId: avatarFileId.value,
        filePath: avatarFilePath.value,
        fileType: 1
      })
      if (result && result.exists && result.localPath) {
        avatarLocalPath.value = result.localPath
        return
      }
      if (window.api.downloadMeetingFile) {
        const lp = await window.api.downloadMeetingFile({
          fileId: avatarFileId.value,
          filePath: avatarFilePath.value,
          fileType: 1
        })
        if (lp && (lp.data?.localPath || lp.localPath)) {
          avatarLocalPath.value = lp.data?.localPath || lp.localPath
        }
      }
    } catch (e) {
      console.warn('加载头像失败:', e.message)
    }
  }
  avatarLocalPath.value = ''
}

const handleAvatarClick = () => {
  avatarPreviewVisible.value = true
}



onMounted(() => {
  loadUserInfo()
  window.addEventListener('storage', loadUserInfo)
  window.addEventListener('user-info-updated', loadUserInfo)
})

onUnmounted(() => {
  window.removeEventListener('storage', loadUserInfo)
  window.removeEventListener('user-info-updated', loadUserInfo)
})
</script>

<style lang="css" scoped>
.main-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  background: #f8f9fa;
  overflow: hidden;
  box-sizing: border-box;
}

.sidebar {
  width: 200px;
  min-width: 200px;
  height: 100%;
  background: #ffffff;
  border-right: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: 1px 0 3px rgba(0, 0, 0, 0.04);
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
}

.sidebar-header h2 {
  color: #1a73e8;
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  letter-spacing: -0.3px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-logo {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.nav-menu {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 13px 16px;
  margin-bottom: 4px;
  border-radius: 10px;
  color: #5f6368;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  font-weight: 500;
  font-size: 14px;
}

.nav-item:hover {
  background: #f1f3f4;
  color: #202124;
}

.nav-item.active {
  background: rgba(26, 115, 232, 0.08);
  color: #1a73e8;
  font-weight: 600;
}

.nav-item .el-icon {
  font-size: 19px;
  flex-shrink: 0;
}

.nav-item span {
  font-size: 14px;
  flex-shrink: 0;
}

.nav-divider {
  height: 1px;
  background: #e8eaed;
  margin: 8px 16px;
}

.ai-assistant-btn {
  border: 1px solid #e8eaed;
  background: rgba(26, 115, 232, 0.04);
}

.ai-assistant-btn:hover {
  background: rgba(26, 115, 232, 0.10);
  border-color: #1a73e8;
  color: #1a73e8;
}

.chat-nav-btn:hover {
  background: #f1f3f4;
}

.chat-nav-btn.active {
  background: rgba(26, 115, 232, 0.08);
  color: #1a73e8;
  font-weight: 600;
}

.sidebar-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  margin: 8px 8px 10px;
  border-radius: 8px;
  background: #f8f9fa;
  transition: background 0.15s ease;
  flex-shrink: 0;
}

.sidebar-footer:hover {
  background: #f1f3f4;
}

.footer-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #dadce0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #5f6368;
  font-weight: 500;
  font-size: 14px;
  flex-shrink: 0;
  overflow: hidden;
  cursor: pointer;
  transition: opacity 0.15s ease;
}

.footer-avatar:hover {
  opacity: 0.75;
}

.footer-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.footer-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
  flex: 1;
}

.footer-name {
  font-size: 13px;
  font-weight: 500;
  color: #202124;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.2;
}

.footer-id {
  font-size: 11px;
  color: #80868b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.2;
}

.content {
  flex: 1;
  overflow: auto;
  background: #f8f9fa;
  color: #202124;
  min-width: 0;
}

.avatar-preview-dialog :deep(.el-dialog) {
  background: transparent;
  box-shadow: none;
  margin: 0 !important;
}

.avatar-preview-dialog :deep(.el-dialog__header) {
  display: none;
}

.avatar-preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  background: transparent;
}

.avatar-preview-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  max-width: 80vw;
  max-height: 80vh;
}

.avatar-preview-image {
  max-width: 80vw;
  max-height: 80vh;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  display: block;
  object-fit: contain;
}

.avatar-preview-placeholder {
  width: 300px;
  height: 300px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  gap: 16px;
}

.avatar-preview-placeholder p {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

/* AI 助手侧边面板 */
.ai-assistant-overlay {
  position: fixed;
  top: 0;
  right: 0;
  width: 420px;
  height: 100vh;
  background: #ffffff;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  display: flex;
  flex-direction: column;
}

.ai-assistant-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.ai-assistant-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e8eaed;
  background: #ffffff;
}

.ai-assistant-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #202124;
}

.close-btn {
  border: 1px solid #e8eaed;
  background: #ffffff;
  color: #5f6368;
}

.close-btn:hover {
  background: #f1f3f4;
  color: #202124;
}

/* 滑入动画 */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: transform 0.3s ease;
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}
</style>
