<template>
  <div class="groups-page">
    <div class="page-header">
      <h1>{{ mode === 'created' ? '我创建的群聊' : '我加入的群聊' }}</h1>
      <span class="count-badge">{{ groups.length }}</span>
    </div>
    <div class="list-container">
      <div v-for="group in groups" :key="group.groupId" class="list-item">
        <div class="avatar-wrapper">
          <ChatAvatar
            :user-id="group.groupId || ''"
            :file-id="''"
            :file-path="group.groupAvatar || ''"
            :user-name="group.groupName || '群聊'"
            :size="56"
          />
        </div>
        <div class="item-info">
          <div class="item-name">
            {{ group.groupName }}
            <span class="contact-tag tag-group">群聊</span>
            <span v-if="mode === 'joined' && group.memberRole === 'ADMIN'" class="contact-tag tag-admin">管理员</span>
          </div>
          <div class="item-id">{{ group.groupId }}</div>
          <div class="item-meta">
            <span>群主：{{ group.ownerUserId }}</span>
            <span>创建于 {{ formatTime(group.createTime) }}</span>
          </div>
        </div>
        <div class="item-actions">
          <el-button size="small" type="primary" @click="chatWithGroup(group)">
            <el-icon><ChatDotRound /></el-icon>
            聊天
          </el-button>
        </div>
      </div>
      <el-empty v-if="!loading && groups.length === 0"
        :description="mode === 'created' ? '暂未创建群聊' : '暂未加入任何群聊'" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { getCurrentInstance } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound } from '@element-plus/icons-vue'
import { ElEmpty } from 'element-plus'
import ChatAvatar from '@/components/ChatAvatar.vue'

const props = defineProps({
  // created=我创建的群聊（OWNER），joined=我加入的群聊（非OWNER）
  mode: {
    type: String,
    default: 'created',
    validator: (v) => ['created', 'joined'].includes(v)
  }
})

const { proxy } = getCurrentInstance()
const router = useRouter()

const groups = ref([])
const loading = ref(false)

// 与群聊聊天：恢复被隐藏的会话并跳转到聊天页
const chatWithGroup = async (group) => {
  const targetUserId = group.groupId
  if (!targetUserId) return
  try {
    if (window.api?.dbSessionRestoreByTarget) {
      const restored = await window.api.dbSessionRestoreByTarget(targetUserId)
      if (restored) {
        console.log('[MyGroups] 会话已就绪:', restored.sessionId, 'deleted=', restored.deleted)
      } else {
        console.warn('[MyGroups] 本地未找到该群聊的会话记录:', targetUserId)
      }
    } else {
      console.warn('[MyGroups] dbSessionRestoreByTarget 不可用（主进程/preload 未更新，请重启应用）')
    }
  } catch (e) {
    console.error('[MyGroups] 恢复会话失败:', e)
  }
  router.push({ path: '/main/chat', query: { targetUserId } })
}

// 加载群聊列表
const loadGroups = async () => {
  loading.value = true
  try {
    const result = await proxy.Request({
      url: props.mode === 'created' ? proxy.Api.myCreatedGroups : proxy.Api.myJoinedGroups
    })
    if (result && result.code === 200) {
      groups.value = result.data || []
    }
  } finally {
    loading.value = false
  }
}

// 时间格式化
const formatTime = (ts) => {
  if (!ts) return '-'
  const d = new Date(Number(ts))
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

onMounted(() => {
  loadGroups()
})

// 同组件复用于两个子路由时，mode 变化重新拉取
watch(() => props.mode, () => {
  loadGroups()
})
</script>

<style lang="css" scoped>
.groups-page {
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
  font-weight: 700;
  color: #202124;
}

.count-badge {
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 11px;
  background: rgba(26, 115, 232, 0.1);
  color: #1a73e8;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.list-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  border: 1px solid #e8eaed;
  background: #fff;
  transition: all 0.2s ease;
}

.list-item:hover {
  border-color: #c6dafc;
  background: #f8faff;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #202124;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contact-tag {
  flex-shrink: 0;
  padding: 1px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
}

.tag-group {
  background: rgba(26, 115, 232, 0.1);
  color: #1a73e8;
}

.tag-admin {
  background: rgba(234, 67, 53, 0.1);
  color: #ea4335;
}

.item-id {
  margin-top: 3px;
  font-size: 12px;
  color: #9aa0a6;
}

.item-meta {
  margin-top: 4px;
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #5f6368;
}

.item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  align-items: center;
}
</style>
