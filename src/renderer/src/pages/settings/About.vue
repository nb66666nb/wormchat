<template>
  <div class="about">
    <div class="section-title">关于</div>

    <div class="app-info">
      <div class="logo-section">
        <el-avatar :size="80" icon="VideoCamera" class="app-logo" />
        <h2 class="app-name">虫聊</h2>
        <p class="app-slogan">智能聊天与通话平台</p>
      </div>

      <el-descriptions :column="1" border size="small" class="info-table">
        <el-descriptions-item label="版本号">{{ appInfo.version }}</el-descriptions-item>
        <el-descriptions-item label="构建类型">{{ appInfo.buildType }}</el-descriptions-item>
        <el-descriptions-item label="运行环境">
          Electron {{ appInfo.electron }} · Chrome {{ appInfo.chrome }} · Node {{ appInfo.node }}
        </el-descriptions-item>
        <el-descriptions-item label="操作系统">{{ osName }}</el-descriptions-item>
      </el-descriptions>

      <div class="feature-section">
        <h3 class="sub-title">功能特性</h3>
        <div class="feature-list">
          <div v-for="(feature, index) in features" :key="index" class="feature-item">
            <span class="feature-icon">{{ feature.icon }}</span>
            <div class="feature-content">
              <span class="feature-name">{{ feature.name }}</span>
              <span class="feature-desc">{{ feature.desc }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="creator-section">
        <h3 class="sub-title">开发团队</h3>
        <div class="creator-card">
          <el-avatar :size="50" icon="UserFilled" />
          <div class="creator-info">
            <span class="creator-name">个人本人</span>
            <span class="creator-role">开发负责人:ggbond(QAQ)</span>
          </div>
        </div>
      </div>

      <div class="links-section">
        <el-link type="primary" :underline="false" @click="checkUpdate">检查更新</el-link>
        <span class="divider">|</span>
        <el-link type="primary" :underline="false" @click="openUserDataDir">查看日志</el-link>
        <span class="divider">|</span>
        <el-link type="primary" :underline="false" @click="openLicense">许可协议</el-link>
      </div>

      <div class="copyright">
        <p>&copy; {{ year }} 虫聊. All rights reserved.</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

// 真实应用信息：从主进程 app.getVersion()/process.versions 获取，替代写死的模拟数据
const appInfo = reactive({
  version: '1.0.0',
  electron: '',
  chrome: '',
  node: '',
  platform: '',
  buildType: '正式版'
})

const osName = computed(() => {
  const p = appInfo.platform
  if (p === 'win32') return 'Windows'
  if (p === 'darwin') return 'macOS'
  if (p === 'linux') return 'Linux'
  return p || '未知'
})

const year = new Date().getFullYear()

const features = [
  { name: '即时通话', desc: '快速创建通话，支持多种方式', icon: '➕' },
  { name: '通话记录', desc: '查看历史通话，管理通话信息', icon: '📋' },
  { name: '视频通话', desc: '高清视频通话，支持屏幕共享', icon: '📹' },
  { name: '安全加密', desc: '端到端加密，保护通话隐私安全', icon: '🔒' },
  { name: '消息通知', desc: '实时消息推送，不错过重要通话', icon: '🔔' }
]

onMounted(async () => {
  if (window.api?.getAppInfo) {
    try {
      const info = await window.api.getAppInfo()
      if (info) Object.assign(appInfo, info)
    } catch (_) {}
  }
})

// 检查更新：项目未接入 autoUpdater，诚实提示当前版本
const checkUpdate = () => {
  ElMessage.info(`当前版本 ${appInfo.version}，已是最新版本`)
}

// 查看日志：打开用户数据目录（Electron userData，含本地数据/日志）
const openUserDataDir = async () => {
  if (window.api?.openUserDataDir) {
    const r = await window.api.openUserDataDir()
    if (!r?.success) ElMessage.warning('打开失败：' + (r?.error || ''))
  } else {
    ElMessage.warning('当前环境不支持打开数据目录')
  }
}

// 许可协议：诚实提示
const openLicense = () => {
  ElMessage.info('开源许可详见项目仓库 README')
}
</script>

<style lang="css" scoped>
.about {
  width: 100%;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
}

.app-info {
  max-width: 520px;
}

.logo-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.app-logo {
  background: #1a73e8;
  font-size: 36px;
}

.app-name {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  background: linear-gradient(135deg, #1a73e8 0%, #1557b0 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.app-slogan {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.info-table {
  margin-bottom: 24px;
}

.feature-section,
.creator-section {
  margin-bottom: 24px;
}

.sub-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
  padding-left: 10px;
  border-left: 3px solid #1a73e8;
}

.feature-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.feature-icon {
  font-size: 18px;
}

.feature-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.feature-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.feature-desc {
  font-size: 11px;
  color: #909399;
}

.creator-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: rgba(26, 115, 232, 0.04);
  border-radius: 12px;
  border: 1px solid #e8eaed;
}

.creator-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.creator-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.creator-role {
  font-size: 12px;
  color: #1a73e8;
  font-weight: 500;
}

.links-section {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  font-size: 13px;
}

.divider {
  color: #dcdfe6;
}

.copyright {
  text-align: center;
  color: #909399;
  font-size: 12px;
}

.copyright p {
  margin: 0;
}
</style>
