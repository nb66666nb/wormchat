<template>
  <div class="log-management">
    <div class="page-header">
      <h2>📋 系统日志</h2>
      <div class="header-actions">
        <el-button type="primary" @click="refreshLogs">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button type="success" @click="exportLogs">
          <el-icon><Download /></el-icon>
          导出
        </el-button>
        <el-button type="danger" @click="clearLogs">
          <el-icon><Delete /></el-icon>
          清空
        </el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-select v-model="logLevel" placeholder="日志级别" clearable style="width: 150px">
        <el-option label="全部" :value="''" />
        <el-option label="INFO" :value="'info'" />
        <el-option label="WARNING" :value="'warning'" />
        <el-option label="ERROR" :value="'error'" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        style="width: 340px"
      />
      <el-input
        v-model="searchQuery"
        placeholder="搜索日志内容"
        clearable
        style="width: 200px"
      />
      <el-button type="primary" @click="searchLogs">搜索</el-button>
    </div>

    <div class="table-container">
      <el-table :data="filteredLogs" stripe style="width: 100%" height="500">
        <el-table-column prop="timestamp" label="时间" width="180" />
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)" size="small">
              {{ row.level.toUpperCase() }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="user" label="用户" width="120" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="message" label="日志内容" min-width="300" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="viewDetail(row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 日志详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="日志详情" width="700px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="时间">{{ currentLog?.timestamp }}</el-descriptions-item>
        <el-descriptions-item label="级别">
          <el-tag :type="getLevelType(currentLog?.level)">
            {{ currentLog?.level?.toUpperCase() }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="模块">{{ currentLog?.module }}</el-descriptions-item>
        <el-descriptions-item label="用户">{{ currentLog?.user }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog?.ip }}</el-descriptions-item>
        <el-descriptions-item label="日志内容">
          <pre style="margin: 0; white-space: pre-wrap;">{{ currentLog?.message }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { Refresh, Download, Delete } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { proxy } = getCurrentInstance()

const logs = ref([
  {
    timestamp: '2026-05-10 14:30:25',
    level: 'info',
    module: 'UserService',
    user: '张三',
    ip: '192.168.1.100',
    message: '用户登录成功'
  },
  {
    timestamp: '2026-05-10 14:25:10',
    level: 'warning',
    module: 'MeetingService',
    user: '李四',
    ip: '192.168.1.101',
    message: '通话参与人数超过阈值警告'
  },
  {
    timestamp: '2026-05-10 14:20:05',
    level: 'error',
    module: 'WebSocketService',
    user: '系统',
    ip: '127.0.0.1',
    message: 'WebSocket连接异常断开'
  },
  {
    timestamp: '2026-05-10 14:15:30',
    level: 'info',
    module: 'MeetingService',
    user: '王五',
    ip: '192.168.1.102',
    message: '创建新通话: 产品评审会'
  }
])

const logLevel = ref('')
const dateRange = ref(null)
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(4)
const detailDialogVisible = ref(false)
const currentLog = ref(null)

const filteredLogs = computed(() => {
  return logs.value
})

const getLevelType = (level) => {
  const map = {
    'info': 'success',
    'warning': 'warning',
    'error': 'danger'
  }
  return map[level] || 'info'
}

const loadLogs = () => {
  proxy.$message.info('加载日志列表')
}

const refreshLogs = () => {
  loadLogs()
  proxy.$message.success('刷新成功')
}

const searchLogs = () => {
  proxy.$message.info(`搜索: ${searchQuery.value}`)
}

const exportLogs = () => {
  ElMessageBox.confirm('确定要导出日志吗？', '提示', {
    type: 'info'
  }).then(() => {
    ElMessage.success('日志导出中...')
  }).catch(() => {})
}

const clearLogs = () => {
  ElMessageBox.confirm('确定要清空所有日志吗？此操作不可恢复！', '提示', {
    type: 'danger'
  }).then(() => {
    logs.value = []
    total.value = 0
    ElMessage.success('日志已清空')
  }).catch(() => {})
}

const viewDetail = (log) => {
  currentLog.value = log
  detailDialogVisible.value = true
}

const handlePageChange = () => {
  loadLogs()
}

onMounted(() => {
  loadLogs()
})
</script>

<style lang="scss" scoped>
.log-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 20px;
    color: #303133;
  }

  .header-actions {
    display: flex;
    gap: 10px;
  }
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.table-container {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

pre {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  font-size: 13px;
}
</style>