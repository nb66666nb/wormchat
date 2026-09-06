<template>
  <div class="search-page">
    <div class="page-header">
      <h1>搜索用户</h1>
    </div>
    <div class="search-container">
      <div class="search-box-wrapper">
        <el-input
          v-model="searchUserId"
          placeholder="输入用户ID搜索"
          class="search-input"
          @keyup.enter="searchUser"
        >
          <template #append>
            <el-button @click="searchUser" type="primary" :loading="searching">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>

      <!-- 搜索结果 -->
      <div v-if="searchedUser" class="search-result-card">
        <div class="result-header">
          <span class="result-title">搜索结果</span>
          <el-button size="small" @click="clearSearch">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="user-card">
          <div class="avatar-wrapper">
            <ChatAvatar
              :user-id="searchedUser.userId || searchedUser.id || ''"
              :file-id="searchedUser.avatarFileId || searchedUser.fileId || ''"
              :file-path="searchedUser.avatarFilePath || searchedUser.filePath || searchedUser.avatarUrl || ''"
              :user-name="searchedUser.nickName || searchedUser.username || searchedUser.userName || '用户'"
              :size="80"
              class="user-avatar"
            />
          </div>
          <div class="user-info">
            <h3 class="user-name">{{ searchedUser.nickName || searchedUser.username }}</h3>
            <p class="user-id">用户ID: {{ searchedUser.userId }}</p>
          </div>
          <div class="user-actions">
            <el-button
              v-if="!isFriend(searchedUser.userId)"
              type="primary"
              :disabled="hasPendingRequest(searchedUser.userId)"
              @click="sendFriendRequest(searchedUser)"
            >
              {{ hasPendingRequest(searchedUser.userId) ? '已发送请求' : '添加好友' }}
            </el-button>
            <el-button
              v-else
              type="success"
              disabled
            >
              已是好友
            </el-button>
          </div>
        </div>
      </div>

      <div v-else-if="searchAttempted" class="empty-state">
        <el-empty description="未找到该用户" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getCurrentInstance } from 'vue'
import { Search, Close } from '@element-plus/icons-vue'
import ChatAvatar from '@/components/ChatAvatar.vue'
import { ElMessage, ElMessageBox, ElEmpty } from 'element-plus'

const { proxy } = getCurrentInstance()

// 搜索相关
const searchUserId = ref('')
const searchedUser = ref(null)
const searching = ref(false)
const searchAttempted = ref(false)

// 用于判断好友关系的数据
const friends = ref([])
const myRequests = ref([])

// 初始化加载数据
const initData = async () => {
  await loadFriends()
  await loadMyRequests()
}

// 加载好友列表
const loadFriends = async () => {
  const result = await proxy.Request({
    url: proxy.Api.getMyFriends
  })
  if (result && result.code === 200) {
    friends.value = result.data || []
  }
}

// 加载我发起的申请
const loadMyRequests = async () => {
  const result = await proxy.Request({
    url: proxy.Api.getMyFriendRequests
  })
  if (result && result.code === 200) {
    myRequests.value = result.data || []
  }
}

// 搜索用户
const searchUser = async () => {
  if (!searchUserId.value.trim()) {
    ElMessage.warning('请输入用户ID')
    return
  }

  searching.value = true
  searchAttempted.value = true

  const result = await proxy.Request({
    url: proxy.Api.searchUserById,
    params: {
      contactId: searchUserId.value.trim()
    }
  })

  searching.value = false

  if (result && result.code === 200) {
    searchedUser.value = result.data
    await initData()
  } else {
    searchedUser.value = null
    ElMessage.error(result?.info || '未找到该用户')
  }
}

// 清除搜索
const clearSearch = () => {
  searchUserId.value = ''
  searchedUser.value = null
  searchAttempted.value = false
}

// 判断是否是好友
const isFriend = (userId) => {
  return friends.value.some(f => f.userId === userId)
}

// 判断是否有待处理的申请
const hasPendingRequest = (userId) => {
  return myRequests.value.some(r => r.targetUserId === userId && r.status === 0)
}

// 发送好友请求
const sendFriendRequest = async (user) => {
  try {
    const remark = await proxy.$prompt(
      `请输入备注消息（让对方了解你是谁）`,
      `添加好友 - ${user.nickName || user.username}`,
      {
        confirmButtonText: '发送请求',
        cancelButtonText: '取消',
        type: 'info',
        inputType: 'textarea',
        inputPlaceholder: '请输入备注消息...',
        inputAttributes: {
          rows: 3,
          maxlength: 200
        },
        beforeConfirm: (value) => {
          if (!value.trim()) {
            ElMessage.warning('请输入备注消息')
            return false
          }
          return true
        }
      }
    )

    const result = await proxy.Request({
      url: proxy.Api.addContact,
      params: {
        contactId: user.userId,
        remark: remark.value.trim()
      }
    })

    if (result && result.code === 200) {
      ElMessage.success('好友请求已发送')
      await loadMyRequests()
    } else {
      ElMessage.error(result?.info || '发送失败')
    }
  } catch (error) {
    // 用户取消
  }
}

// 安全初始化：避免 setup 阶段未捕获错误导致页面空白
;(async () => {
  try {
    await initData()
  } catch (e) {
    console.warn('Search 初始化失败:', e.message)
  }
})()
</script>

<style lang="css" scoped>
.search-page {
  height: 100%;
}

.search-container {
  max-width: 500px;
}

.search-box-wrapper {
  margin-bottom: 24px;
}

.search-input {
  --el-input-hover-border-color: #1a73e8;
  --el-input-focus-border-color: #1a73e8;
}

.search-result-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
}

.result-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.page-header h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
}

.avatar-wrapper {
  position: relative;
}

.user-avatar {
  background: #1a73e8;
}

.online-badge {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #67c23a;
  border: 2px solid #fff;
}

.user-info {
  flex: 1;
}

.user-name {
  margin: 0 0 8px 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.user-id {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #909399;
}

.user-status {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.user-status.online {
  color: #67c23a;
}

.user-actions {
  flex-shrink: 0;
}

.empty-state {
  padding: 60px 0;
}
</style>
