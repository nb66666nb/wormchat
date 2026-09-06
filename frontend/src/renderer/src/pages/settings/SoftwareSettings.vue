<template>
  <div class="software-settings">
    <div class="section-title">软件设置</div>

    <div class="settings-group">
      <div class="group-title">通用设置</div>
      <div class="setting-item">
        <span class="setting-label">开机自启动</span>
        <el-switch v-model="settings.autoStart" />
      </div>
      <div class="setting-item">
        <span class="setting-label">启动时最小化到托盘</span>
        <el-switch v-model="settings.minimizeToTray" />
      </div>
      <div class="setting-item">
        <span class="setting-label">关闭窗口时最小化到托盘</span>
        <el-switch v-model="settings.closeToTray" />
      </div>
    </div>

    <div class="settings-group">
      <div class="group-title">通知设置</div>
      <div class="setting-item">
        <span class="setting-label">桌面通知</span>
        <el-switch v-model="settings.desktopNotification" />
      </div>
      <div class="setting-item">
        <span class="setting-label">声音提醒</span>
        <el-switch v-model="settings.soundNotification" />
      </div>
    </div>

    <div class="settings-group">
      <div class="group-title">通话设置</div>
      <div class="setting-item">
        <span class="setting-label">默认开启摄像头</span>
        <el-switch v-model="settings.defaultCamera" />
      </div>
      <div class="setting-item">
        <span class="setting-label">默认开启麦克风</span>
        <el-switch v-model="settings.defaultMicrophone" />
      </div>
    </div>

    <div class="action-buttons">
      <el-button type="primary" @click="handleSave">保存设置</el-button>
      <el-button @click="handleReset">恢复默认</el-button>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

// 默认设置（与各消费方兜底保持一致：useWebRTC 默认麦克风开/摄像头关）
const DEFAULT_SETTINGS = {
  autoStart: false,
  minimizeToTray: true,
  closeToTray: true,
  desktopNotification: true,
  soundNotification: true,
  defaultCamera: false,
  defaultMicrophone: true
}

const settings = reactive({ ...DEFAULT_SETTINGS })

// 从 localStorage 读取已保存设置（拒绝模拟数据：每次打开都反映真实保存值）
onMounted(() => {
  try {
    const raw = localStorage.getItem('softwareSettings')
    if (raw) Object.assign(settings, DEFAULT_SETTINGS, JSON.parse(raw))
  } catch (_) {}
})

// 落地：渲染层存一份 + 同步主进程触发开机自启/托盘/通知/声音
const applySettings = async () => {
  localStorage.setItem('softwareSettings', JSON.stringify(settings))
  if (window.api?.updateSettings) {
    const r = await window.api.updateSettings({ ...settings })
    if (!r?.success) throw new Error(r?.error || '未知错误')
  }
}

const handleSave = async () => {
  try {
    await applySettings()
    ElMessage.success('设置已保存并生效')
  } catch (e) {
    ElMessage.warning('设置未能生效：' + e.message)
  }
}

const handleReset = async () => {
  Object.assign(settings, DEFAULT_SETTINGS)
  try {
    await applySettings()
    ElMessage.info('已恢复默认设置并生效')
  } catch (e) {
    ElMessage.warning('恢复默认失败：' + e.message)
  }
}
</script>

<style lang="css" scoped>
.software-settings {
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

.settings-group {
  margin-bottom: 24px;
}

.group-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-label {
  font-size: 13px;
  color: #303133;
}

.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 12px;
}
</style>
