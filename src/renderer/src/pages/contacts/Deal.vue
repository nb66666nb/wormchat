<template>
  <div class="pending-page">
    <div class="page-header">
      <h1>待处理</h1>
      <span class="count-badge pending">{{ pendingRequests.length }}</span>
    </div>
    <div class="list-container">
      <div
        v-for="request in pendingRequests"
        :key="request.id"
        class="list-item"
      >
        <el-avatar :size="56" icon="UserFilled" />
        <div class="item-info">
          <div class="item-name">{{ request.requestUserId || request.requestUserId}}</div>
          <div class="item-id">{{ request.requestUserId }}</div>
          <div class="item-time">申请时间: {{ formatTime(request.createTime) }}</div>
        </div>
        <div class="item-actions">
          <el-button size="small" type="success" @click="acceptFriendRequest(request)">
            <el-icon><Check /></el-icon>
            通过
          </el-button>
          <el-button size="small" type="danger" @click="rejectFriendRequest(request)">
            <el-icon><Close /></el-icon>
            拒绝
          </el-button>
        </div>
      </div>
      <el-empty v-if="pendingRequests.length === 0" description="暂无待处理申请" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { UserFilled, Check, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElEmpty, ElAvatar, ElButton } from 'element-plus'

const { proxy } = getCurrentInstance()

const pendingRequests = ref([])

// 加载待处理的申请
const loadPendingRequests = async () => {
  const result = await proxy.Request({
    url: proxy.Api.getFriendRequests,
  })
  if (result && result.code === 200) {
    pendingRequests.value = result.data || []
  }
}

// 通过申请
const acceptFriendRequest = async (request) => {
  try {
    await ElMessageBox.confirm('确定要通过该好友申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'success'
    })

    const result = await proxy.Request({
      url: proxy.Api.manageRequest,
      params: {
       contactId: request.requestUserId,
       status:1
      }
    })

    if (result && result.code === 200) {
      ElMessage.success('已通过好友申请')
      await loadPendingRequests()
    } else {
      ElMessage.error(result?.info || '操作失败')
    }
  } catch (error) {
    // 用户取消
  }
}

// 拒绝申请
const rejectFriendRequest = async (request) => {
  try {
    await ElMessageBox.confirm('确定要拒绝该好友申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const result = await proxy.Request({
      url: proxy.Api.manageRequest,
      params: {
       contactId: request.requestUserId,
       status:2
      }
    })

    if (result && result.code === 200) {
      ElMessage.success('已拒绝好友申请')
      await loadPendingRequests()
    } else {
      ElMessage.error(result?.info || '操作失败')
    }
  } catch (error) {
    // 用户取消
  }
}

// 格式化时间
const formatTime = (timestamp) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
}

onMounted(() => {
  loadPendingRequests()
})
</script>

<style lang="css" scoped>
.pending-page {
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

.count-badge.pending {
  background: #fef0f0;
  color: #f56c6c;
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

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.item-id {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.item-time {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}

.item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>
