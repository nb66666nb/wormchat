<template>
  <div class="requests-page">
    <div class="page-header">
      <h1>我的申请</h1>
      <span class="count-badge">{{ myRequests.length }}</span>
    </div>
    <div class="list-container">
      <div
        v-for="request in myRequests"
        :key="request.id"
        class="list-item"
      >
        <ChatAvatar
          :user-id="request.targetUserId || request.userId || ''"
          :file-id="request.avatarFileId || request.fileId || ''"
          :file-path="request.avatarFilePath || request.filePath || request.avatarUrl || ''"
          :user-name="request.targetNickName || request.targetUsername || ''"
          :size="56"
        />
        <div class="item-info">
          <div class="item-name">{{ request.targetNickName || request.targetUsername }}</div>
          <div class="item-id">{{ request.targetUserId }}</div>
          <div :class="['status-badge', getStatusClass(request.status)]">
            {{ getRequestStatusText(request.status) }}
          </div>
        </div>
        <div class="item-actions">
          <el-button
            v-if="request.status === 0"
            size="small"
            type="warning"
            @click="cancelFriendRequest(request)"
          >
            <el-icon><Close /></el-icon>
            取消
          </el-button>
        </div>
      </div>
      <el-empty v-if="myRequests.length === 0" description="暂无申请记录" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { UserFilled, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElEmpty, ElAvatar, ElButton } from 'element-plus'
import ChatAvatar from '@/components/ChatAvatar.vue'

const { proxy } = getCurrentInstance()

const myRequests = ref([])

// 加载我发起的申请
const loadMyRequests = async () => {
  const result = await proxy.Request({
    url: proxy.Api.getMyFriendRequests
  })
  if (result && result.code === 200) {
    myRequests.value = result.data || []
  }
}

// 取消申请
const cancelFriendRequest = async (request) => {
  try {
    await ElMessageBox.confirm('确定要撤销该申请吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const result = await proxy.Request({
      url: proxy.Api.manageMyRequest,
      params: {
        contactId: request.targetUserId,
        status: 3
      }
    })

    if (result && result.code === 200) {
      ElMessage.success('已取消申请')
      await loadMyRequests()
    } else {
      ElMessage.error(result?.info || '操作失败')
    }
  } catch (error) {
    // 用户取消
  }
}

// 获取申请状态文本
const getRequestStatusText = (status) => {
  const statusMap = {
    0: '待审核',
    1: '已通过',
    2: '已拒绝',
    3: '已取消'
  }
  return statusMap[status] || '未知'
}

// 获取状态样式类
const getStatusClass = (status) => {
  const classMap = {
    0: 'pending',
    1: 'success',
    2: 'danger',
    3: 'warning'
  }
  return classMap[status] || ''
}

onMounted(() => {
  loadMyRequests()
})
</script>

<style lang="css" scoped>
.requests-page {
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

.item-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 11px;
  border-radius: 4px;
  margin-top: 4px;
}

.status-badge.pending {
  background: #fff7e6;
  color: #e6a23c;
}

.status-badge.success {
  background: #f0f9eb;
  color: #67c23a;
}

.status-badge.danger {
  background: #fef0f0;
  color: #f56c6c;
}

.status-badge.warning {
  background: #f5f0ff;
  color: #909399;
}
</style>
