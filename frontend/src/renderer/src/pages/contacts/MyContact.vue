<template>
  <div class="friends-page">
    <div class="page-header">
      <h1>我的好友</h1>
      <span class="count-badge">{{ friends.length }}</span>
    </div>
    <div class="list-container">
      <div
        v-for="friend in friends"
        :key="friend.userId"
        class="list-item"
      >
        <div class="avatar-wrapper">
          <ChatAvatar
            :user-id="friend.friendUserId || friend.userId || ''"
            :file-id="friend.avatarFileId || friend.fileId || ''"
            :file-path="friend.avatarFilePath || friend.filePath || friend.avatarUrl || ''"
            :user-name="friend.remark || friend.nickName || friend.username || ''"
            :size="56"
          />
        </div>
        <div class="item-info">
          <div class="item-name">
            {{ friend.remark }}
            <span v-if="friend.status === 1" class="online-text">在线</span>
            <span :class="['contact-tag', getContactType(friend.friendUserId).class]" v-if="getContactType(friend.friendUserId).label">
              {{ getContactType(friend.friendUserId).label }}
            </span>
          </div>
          <div class="item-id">{{ friend.friendUserId }}</div>
        </div>
        <div class="item-actions">
          <el-button size="small" type="primary" @click="chatWithFriend(friend)">
            <el-icon><ChatDotRound /></el-icon>
            聊天
          </el-button>
          <el-button size="small" type="danger" @click="deleteFriend(friend)">
            <el-icon><MoreFilled /></el-icon>
          </el-button>
        </div>
      </div>
      <el-empty v-if="friends.length === 0" description="暂无好友" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { UserFilled, MoreFilled, ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElEmpty, ElAvatar, ElButton } from 'element-plus'
import ChatAvatar from '@/components/ChatAvatar.vue'

const { proxy } = getCurrentInstance()
const router = useRouter()

const friends = ref([])

const getContactType = (userId) => {
  if (!userId) return { label: '', class: '' }
  const prefix = userId.substring(0, 1).toUpperCase()
  switch (prefix) {
    case 'U':
      return { label: '用户', class: 'tag-user' }
    case 'R':
      return { label: '机器人', class: 'tag-robot' }
    case 'G':
      return { label: '群聊', class: 'tag-group' }
    default:
      return { label: '', class: '' }
  }
}

// 与好友聊天：恢复被隐藏的会话并跳转到聊天页
const chatWithFriend = async (friend) => {
  const targetUserId = friend.friendUserId || friend.userId
  if (!targetUserId) return
  try {
    if (window.api?.dbSessionRestoreByTarget) {
      const restored = await window.api.dbSessionRestoreByTarget(targetUserId)
      if (restored) {
        console.log('[MyContact] 会话已就绪:', restored.sessionId, 'deleted=', restored.deleted)
      } else {
        console.warn('[MyContact] 本地未找到该好友的会话记录:', targetUserId)
      }
    } else {
      console.warn('[MyContact] dbSessionRestoreByTarget 不可用（主进程/preload 未更新，请重启应用）')
    }
  } catch (e) {
    console.error('[MyContact] 恢复会话失败:', e)
  }
  router.push({ path: '/main/chat', query: { targetUserId } })
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

// 删除好友
const deleteFriend = async (friend) => {
  try {
    await ElMessageBox.confirm(`确定要删除 ${friend.remark } 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'danger'
    })

    const result = await proxy.Request({
      url: proxy.Api.deleteContact,
      params: {
        contactId: friend.friendUserId
      }
    })

    if (result && result.code === 200) {
      ElMessage.success('已删除好友')
      await loadFriends()
    } else {
      ElMessage.error(result?.info || '操作失败')
    }
  } catch (error) {
    // 用户取消
  }
}

onMounted(() => {
  loadFriends()
})
</script>

<style lang="css" scoped>
.friends-page {
  height: 100%;
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

.count-badge {
  padding: 2px 8px;
  background: #e8f4fd;
  color: #409eff;
  font-size: 12px;
  font-weight: 500;
  border-radius: 10px;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 10px;
  transition: background-color 0.2s ease;
}

.list-item:hover {
  background: #f5f5f5;
}

.avatar-wrapper {
  position: relative;
}

.online-badge {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #67c23a;
  border: 2px solid #fff;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.online-text {
  font-size: 11px;
  color: #67c23a;
  padding: 1px 6px;
  background: #f0f9eb;
  border-radius: 4px;
}

.contact-tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.tag-user {
  color: #409eff;
  background: #ecf5ff;
}

.tag-robot {
  color: #67c23a;
  background: #f0f9eb;
}

.tag-group {
  color: #e6a23c;
  background: #fdf6ec;
}

.item-id {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>
