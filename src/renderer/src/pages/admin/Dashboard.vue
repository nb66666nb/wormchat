<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div v-for="card in statCards" :key="card.key" class="stat-card">
        <div class="stat-icon" :class="card.theme">
          <el-icon><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- 主体区：最近注册用户 + 在线用户 -->
    <div class="grid">
      <div class="panel">
        <div class="panel-head">
          <span class="panel-title">最近注册用户</span>
          <el-button text type="primary" @click="goUsers">查看全部</el-button>
        </div>
        <div class="panel-body">
          <el-table :data="recentUsers" size="small" v-loading="loadingRecent">
            <el-table-column prop="nickName" label="昵称" min-width="110" />
            <el-table-column prop="email" label="邮箱" min-width="170" show-overflow-tooltip />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? '正常' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="在线" width="80">
              <template #default="{ row }">
                <span class="dot" :class="row.online ? 'on' : 'off'"></span>
                <span :class="row.online ? 'txt-on' : 'txt-off'">{{ row.online ? '在线' : '离线' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="panel">
        <div class="panel-head">
          <span class="panel-title">当前在线用户</span>
          <el-button text type="primary" @click="goUsers">管理</el-button>
        </div>
        <div class="panel-body">
          <div v-if="!onlineUserIds.length && !loadingOnline" class="empty-tip">暂无在线用户</div>
          <div v-else class="online-chips">
            <span v-for="uid in onlineUserIds.slice(0, 60)" :key="uid" class="online-chip">{{ uid }}</span>
            <span v-if="onlineUserIds.length > 60" class="more-tip">等 {{ onlineUserIds.length }} 人</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { Connection, User, ChatRound, CircleClose } from '@element-plus/icons-vue'

const emit = defineEmits(['online-count', 'switch'])
const { proxy } = getCurrentInstance()

const recentUsers = ref([])
const onlineUserIds = ref([])
const loadingRecent = ref(false)
const loadingOnline = ref(false)

const statCards = reactive([
  { key: 'online', label: '在线用户', value: '-', theme: 'green', icon: Connection },
  { key: 'total', label: '注册用户', value: '-', theme: 'blue', icon: User },
  { key: 'group', label: '群聊总数', value: '-', theme: 'yellow', icon: ChatRound },
  { key: 'banned', label: '封禁用户', value: '-', theme: 'red', icon: CircleClose }
])

const loadOnline = async () => {
  loadingOnline.value = true
  const r = await proxy.Request({ url: proxy.Api.adminOnlineUsers, showLoading: false })
  loadingOnline.value = false
  if (r && r.code === 200) {
    onlineUserIds.value = r.data.onlineUserIds || []
    statCards[0].value = r.data.onlineCount || 0
    emit('online-count', r.data.onlineCount || 0)
  }
}

const loadTotal = async () => {
  const r = await proxy.Request({
    url: proxy.Api.adminUserList,
    params: { pageNo: 1, pageSize: 1 },
    showLoading: false
  })
  if (r && r.code === 200) statCards[1].value = r.data.totalCount || 0
}

const loadBanned = async () => {
  const r = await proxy.Request({
    url: proxy.Api.adminUserList,
    params: { pageNo: 1, pageSize: 1, status: 0 },
    showLoading: false
  })
  if (r && r.code === 200) statCards[3].value = r.data.totalCount || 0
}

const loadGroupTotal = async () => {
  const r = await proxy.Request({
    url: proxy.Api.adminGroupList,
    params: { pageNo: 1, pageSize: 1 },
    showLoading: false
  })
  if (r && r.code === 200) statCards[2].value = r.data.totalCount || 0
}

const loadRecent = async () => {
  loadingRecent.value = true
  const r = await proxy.Request({
    url: proxy.Api.adminUserList,
    params: { pageNo: 1, pageSize: 8, orderBy: 'create_time desc' },
    showLoading: false
  })
  loadingRecent.value = false
  if (r && r.code === 200) recentUsers.value = r.data.list || []
}

const goUsers = () => emit('switch', 'users')

onMounted(() => {
  loadOnline()
  loadTotal()
  loadBanned()
  loadGroupTotal()
  loadRecent()
})
</script>

<style lang="css" scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  transition: all 0.2s ease;
}

.stat-card:hover {
  border-color: #c6dafc;
  background: #f8faff;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.stat-icon.green { background: rgba(52, 168, 83, 0.1); color: #34a853; }
.stat-icon.blue { background: rgba(26, 115, 232, 0.1); color: #1a73e8; }
.stat-icon.yellow { background: rgba(251, 188, 4, 0.14); color: #f9ab00; }
.stat-icon.red { background: rgba(234, 67, 53, 0.1); color: #ea4335; }

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #202124;
  line-height: 1.1;
}

.stat-label {
  font-size: 13px;
  color: #5f6368;
  margin-top: 3px;
}

.grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 14px;
}

.panel {
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid #e8eaed;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #202124;
}

.panel-body {
  padding: 14px 18px;
  flex: 1;
  overflow: auto;
}

.dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 5px;
  vertical-align: middle;
}

.dot.on { background: #34a853; }
.dot.off { background: #dadce0; }

.txt-on { color: #34a853; font-size: 12px; }
.txt-off { color: #9aa0a6; font-size: 12px; }

.empty-tip {
  text-align: center;
  color: #9aa0a6;
  font-size: 13px;
  padding: 30px 0;
}

.online-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.online-chip {
  padding: 3px 10px;
  border-radius: 4px;
  background: rgba(52, 168, 83, 0.1);
  color: #34a853;
  font-size: 12px;
  font-weight: 500;
}

.more-tip {
  font-size: 12px;
  color: #9aa0a6;
  align-self: center;
}
</style>
