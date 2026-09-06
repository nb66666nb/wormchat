<template>
  <div class="meeting-container">
    <!-- 左侧按钮区域 -->
    <div class="main-panel">
      <div class="panel-title">
        <h2>通话</h2>
      </div>
      <div class="button-grid">
        <div
          :class="['action-btn', { active: currentPanel === 'create' }]"
          @click="handleCreateMeeting"
        >
          <div class="btn-icon">
            <el-icon><Plus /></el-icon>
          </div>
          <span class="btn-text">创建通话</span>
        </div>
        <div
          :class="['action-btn', { active: currentPanel === 'records' }]"
          @click="handleMeetingRecords"
        >
          <div class="btn-icon">
            <el-icon><Document /></el-icon>
          </div>
          <span class="btn-text">通话记录</span>
        </div>
        <div
          :class="['action-btn', { active: currentPanel === 'join' }]"
          @click="handleJoinMeeting"
        >
          <div class="btn-icon">
            <el-icon><VideoCamera /></el-icon>
          </div>
          <span class="btn-text">加入通话</span>
        </div>
      </div>
    </div>

    <!-- 中垂线分隔 -->
    <div class="divider"></div>

    <!-- 右侧内容区域 -->
    <div class="right-panel">
      <div class="content-header">
        <h1>{{ panelTitle }}</h1>
      </div>
      <div class="content-body">
        <component :is="currentComponent" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, markRaw } from 'vue'
import { Plus, Document, VideoCamera } from '@element-plus/icons-vue'
import MeetingCreate from './MeetingCreate.vue'
import MeetingRecords from './MeetingRecords.vue'
import JoinMeeting from './JoinMeeting.vue'

const currentPanel = ref('create')

const panelTitle = computed(() => {
  const titles = {
    create: '创建通话',
    records: '通话记录',
    join: '加入通话',
  }
  return titles[currentPanel.value] || '通话管理'
})

const currentComponent = computed(() => {
  const components = {
    create: markRaw(MeetingCreate),
    records: markRaw(MeetingRecords),
    join: markRaw(JoinMeeting),
  }
  return components[currentPanel.value] || markRaw(MeetingCreate)
})

const handleCreateMeeting = () => {
  currentPanel.value = 'create'
}

const handleMeetingRecords = () => {
  currentPanel.value = 'records'
}

const handleJoinMeeting = () => {
  currentPanel.value = 'join'
}
</script>

<style lang="css" scoped>
.meeting-container {
  width: 100%;
  height: 100%;
  display: flex;
  background: #f8f9fa;
  overflow: hidden;
}

/* 左侧面板 */
.main-panel {
  width: 240px;
  min-width: 240px;
  height: 100%;
  background: #ffffff;
  border-right: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.panel-title {
  padding: 24px 20px 16px;
  flex-shrink: 0;
}

.panel-title h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: #202124;
}

.button-grid {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 4px 10px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 52px;
  padding: 0 16px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.action-btn:hover {
  background: #f1f3f4;
}

.action-btn.active {
  background: rgba(26, 115, 232, 0.08);
}

.btn-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(26, 115, 232, 0.08);
  border-radius: 9px;
  color: #1a73e8;
  font-size: 18px;
  flex-shrink: 0;
}

.btn-text {
  color: #202124;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}

/* 中垂线分隔 */
.divider {
  width: 1px;
  height: 100%;
  background: #e8eaed;
  flex-shrink: 0;
}

/* 右侧内容区域 */
.right-panel {
  flex: 1;
  min-width: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #ffffff;
  overflow: hidden;
}

.content-header {
  padding: 20px 24px;
  border-bottom: 1px solid #e8eaed;
  background: #ffffff;
  flex-shrink: 0;
}

.content-header h1 {
  font-size: 18px;
  font-weight: 600;
  color: #202124;
  margin: 0;
}

.content-body {
  flex: 1;
  display: flex;
  align-items: flex-start;
  justify-content: flex-start;
  padding: 20px 24px;
  overflow-y: auto;
}
</style>
