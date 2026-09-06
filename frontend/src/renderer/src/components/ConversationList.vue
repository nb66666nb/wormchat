<template>
  <div class="conversation-list">
    <div class="list-header">
      <h3>消息</h3>
      <span class="count-badge" v-if="conversations.length > 0">{{ conversations.length }}</span>
    </div>

    <div class="list-body" v-if="conversations.length > 0">
      <div
        v-for="conv in sortedConversations"
        :key="conv.userId"
        :class="['conversation-item', { active: activeConversation?.userId === conv.userId }]"
        @click="handleSelectConversation(conv)"
      >
        <ChatAvatar
          :user-id="conv.userId"
          :file-id="conv.avatarFileId"
          :file-path="conv.avatarFilePath"
          :user-name="conv.userName"
          :size="42"
          class="conv-avatar"
        />
        <div class="conv-content">
          <div class="conv-header">
            <span class="conv-name">{{ conv.userName }}</span>
            <span class="conv-time" v-if="conv.lastTime">{{ formatTime(conv.lastTime) }}</span>
          </div>
          <div class="conv-preview">
            <span class="preview-text">{{ getPreviewText(conv.lastMessage) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="empty-state" v-else>
      <el-icon :size="48" color="#dadce0"><ChatLineRound /></el-icon>
      <p class="empty-text">暂无会话</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ChatLineRound } from '@element-plus/icons-vue'
import ChatAvatar from '@/components/ChatAvatar.vue'

const emit = defineEmits(['select'])

// 模拟会话数据（实际应从后端或本地存储获取）
const conversations = ref([
  {
    userId: 'user1',
    userName: '张三',
    avatarFileId: '',
    avatarFilePath: '',
    lastMessage: '你好，明天的通话准备好了吗？',
    lastTime: Date.now() - 1000 * 60 * 5
  },
  {
    userId: 'user2',
    userName: '李四',
    avatarFileId: '',
    avatarFilePath: '',
    lastMessage: '收到，我会准时参加',
    lastTime: Date.now() - 1000 * 60 * 30
  },
  {
    userId: 'user3',
    userName: '王五',
    avatarFileId: '',
    avatarFilePath: '',
    lastMessage: '项目文档已经发给你了，请查收',
    lastTime: Date.now() - 1000 * 60 * 60 * 2
  }
])

const activeConversation = ref(null)

const sortedConversations = computed(() => {
  return [...conversations.value].sort((a, b) => (b.lastTime || 0) - (a.lastTime || 0))
})

const getPreviewText = (msg) => {
  if (!msg) return '暂无消息'
  const maxLen = 28
  return msg.length > maxLen ? msg.slice(0, maxLen) + '...' : msg
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const now = Date.now()
  const diff = now - timestamp
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 7) return `${days}天前`

  const date = new Date(timestamp)
  return `${date.getMonth() + 1}/${date.getDate()}`
}

const handleSelectConversation = (conv) => {
  activeConversation.value = conv
  emit('select', conv)
}

defineExpose({
  conversations,
  activeConversation
})
</script>

<style lang="css" scoped>
.conversation-list {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #202124;
}

.count-badge {
  font-size: 12px;
  color: #5f6368;
  background: #f1f3f4;
  padding: 2px 10px;
  border-radius: 12px;
  font-weight: 500;
}

.list-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.list-body::-webkit-scrollbar {
  width: 5px;
}

.list-body::-webkit-scrollbar-thumb {
  background-color: #dadce0;
  border-radius: 3px;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 18px;
  cursor: pointer;
  transition: background 0.15s ease;
}

.conversation-item:hover {
  background: #f8f9fa;
}

.conversation-item.active {
  background: rgba(26, 115, 232, 0.06);
}

.conv-avatar {
  flex-shrink: 0;
}

.conv-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.conv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conv-name {
  font-size: 14px;
  font-weight: 500;
  color: #202124;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-time {
  font-size: 11px;
  color: #9aa0a6;
  flex-shrink: 0;
  margin-left: 8px;
}

.conv-preview {
  overflow: hidden;
}

.preview-text {
  font-size: 13px;
  color: #5f6368;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px 20px;
}

.empty-text {
  font-size: 14px;
  color: #9aa0a6;
  margin: 0;
}
</style>
