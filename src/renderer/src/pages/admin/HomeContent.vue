<template>
  <div class="home-content">
    <el-card class="status-card">
      <template #header>
        <span class="card-title">📊 系统状态</span>
      </template>
      <div class="status-grid">
        <div class="status-item">
          <div class="status-icon online">
            <el-icon><User /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-value">128</div>
            <div class="status-label">在线用户</div>
          </div>
        </div>
        <div class="status-item">
          <div class="status-icon meeting">
            <el-icon><Calendar /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-value">24</div>
            <div class="status-label">进行中通话</div>
          </div>
        </div>
        <div class="status-item">
          <div class="status-icon message">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-value">1.2K</div>
            <div class="status-label">今日消息</div>
          </div>
        </div>
        <div class="status-item">
          <div class="status-icon system">
            <el-icon><Monitor /></el-icon>
          </div>
          <div class="status-info">
            <div class="status-value">99.9%</div>
            <div class="status-label">系统可用性</div>
          </div>
        </div>
      </div>
    </el-card>

    <el-card class="quick-actions-card">
      <template #header>
        <span class="card-title">⚡ 快捷操作</span>
      </template>
      <div class="quick-actions">
        <el-button type="warning" @click="clearCache">
          <el-icon><Delete /></el-icon>
          清除缓存
        </el-button>
        <el-button type="danger" @click="restartService">
          <el-icon><Refresh /></el-icon>
          重启服务
        </el-button>
        <el-button type="info" @click="exportLogs">
          <el-icon><Download /></el-icon>
          导出日志
        </el-button>
      </div>
    </el-card>

    <el-card class="recent-activity-card">
      <template #header>
        <span class="card-title">📋 最近活动</span>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="(activity, index) in activities"
          :key="index"
          :timestamp="activity.time"
          placement="top"
          :type="activity.type"
        >
          <el-card>
            <h4>{{ activity.title }}</h4>
            <p>{{ activity.description }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getCurrentInstance } from 'vue'
import { User, Calendar, ChatDotRound, Monitor, Delete, Refresh, Download } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { proxy } = getCurrentInstance()

const activities = ref([
  {
    title: '新用户注册',
    description: '用户 李四 注册成功',
    time: '2026-05-10 14:30',
    type: 'success'
  },
  {
    title: '通话创建',
    description: '张三 创建了产品评审通话',
    time: '2026-05-10 13:20',
    type: 'primary'
  },
  {
    title: '系统维护',
    description: '缓存清理完成',
    time: '2026-05-10 12:00',
    type: 'info'
  }
])

const clearCache = () => {
  ElMessageBox.confirm('确定要清除系统缓存吗？', '提示', {
    type: 'warning'
  }).then(() => {
    ElMessage.success('缓存已清除')
  }).catch(() => {
    ElMessage.info('已取消')
  })
}

const restartService = () => {
  ElMessageBox.confirm('确定要重启服务吗？这将断开所有连接！', '提示', {
    type: 'danger'
  }).then(() => {
    ElMessage.info('服务重启中...')
  }).catch(() => {
    ElMessage.info('已取消')
  })
}

const exportLogs = () => {
  ElMessage.success('日志导出中...')
}
</script>

<style lang="scss" scoped>
.home-content {
  padding: 10px;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #fafafa;
  border-radius: 10px;

  .status-icon {
    width: 48px;
    height: 48px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
    color: #fff;

    &.online {
      background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
    }

    &.meeting {
      background: linear-gradient(135deg, #409eff 0%, #79bbff 100%);
    }

    &.message {
      background: linear-gradient(135deg, #e6a23c 0%, #f5c152 100%);
    }

    &.system {
      background: linear-gradient(135deg, #909399 0%, #b1b3b8 100%);
    }
  }

  .status-info {
    .status-value {
      font-size: 24px;
      font-weight: 600;
      color: #303133;
    }

    .status-label {
      font-size: 13px;
      color: #909399;
    }
  }
}

.quick-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.recent-activity-card {
  margin-top: 20px;

  h4 {
    margin: 0 0 8px 0;
    font-size: 14px;
    color: #303133;
  }

  p {
    margin: 0;
    font-size: 13px;
    color: #909399;
  }
}
</style>
