<template>
  <div class="admin-shell">
    <!-- 侧边栏（与主应用 Main.vue 风格一致） -->
    <aside class="sidebar">
      <div class="sidebar-header">
        <h2>虫聊管理</h2>
      </div>
      <div class="nav-menu">
        <div
          v-for="item in menus"
          :key="item.key"
          class="nav-item"
          :class="{ active: activeMenu === item.key }"
          @click="activeMenu = item.key"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </div>
      </div>
      <div class="sidebar-footer">
        <div class="online-badge">
          <span class="online-dot" :class="{ on: onlineCount > 0 }"></span>
          <span>{{ onlineCount }} 人在线</span>
        </div>
      </div>
    </aside>

    <!-- 主区域 -->
    <section class="main">
      <header class="topbar">
        <div class="topbar-title">
          <el-icon class="topbar-icon"><component :is="currentMenu.icon" /></el-icon>
          <span>{{ currentMenu.label }}</span>
        </div>
        <div class="topbar-actions">
          <span class="topbar-time">{{ now }}</span>
          <el-button text @click="closeWindow">
            <el-icon><Close /></el-icon>
            关闭
          </el-button>
        </div>
      </header>

      <main class="content">
        <component :is="currentComponent" @online-count="onOnlineCount" @switch="onSwitch" />
      </main>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { Close, DataLine, User, ChatRound } from '@element-plus/icons-vue'
import Dashboard from './Dashboard.vue'
import UserManagement from './UserManagement.vue'
import GroupManagement from './GroupManagement.vue'

const { proxy } = getCurrentInstance()

const menus = [
  { key: 'dashboard', label: '数据概览', icon: DataLine },
  { key: 'users', label: '用户管理', icon: User },
  { key: 'groups', label: '群聊管理', icon: ChatRound }
]

const activeMenu = ref('dashboard')
const onlineCount = ref(0)
const now = ref('')
let timer = null

const currentMenu = computed(() =>
  menus.find(m => m.key === activeMenu.value) || menus[0]
)

const currentComponent = computed(() => {
  if (activeMenu.value === 'users') return UserManagement
  if (activeMenu.value === 'groups') return GroupManagement
  return Dashboard
})

const onOnlineCount = (count) => {
  onlineCount.value = count || 0
}

const onSwitch = (key) => {
  if (key) activeMenu.value = key
}

const updateNow = () => {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  now.value = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const closeWindow = () => window.close()

onMounted(() => {
  updateNow()
  timer = setInterval(updateNow, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="css" scoped>
.admin-shell {
  width: 100vw;
  height: 100vh;
  display: flex;
  background: #f8f9fa;
  overflow: hidden;
  box-sizing: border-box;
}

.sidebar {
  width: 200px;
  min-width: 200px;
  height: 100%;
  background: #ffffff;
  border-right: 1px solid #e8eaed;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: 1px 0 3px rgba(0, 0, 0, 0.04);
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
}

.sidebar-header h2 {
  color: #1a73e8;
  font-size: 18px;
  font-weight: 700;
  margin: 0;
  letter-spacing: -0.3px;
}

.nav-menu {
  flex: 1;
  padding: 16px 12px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 13px 16px;
  margin-bottom: 4px;
  border-radius: 10px;
  color: #5f6368;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
  font-weight: 500;
  font-size: 14px;
}

.nav-item:hover {
  background: #f1f3f4;
  color: #202124;
}

.nav-item.active {
  background: rgba(26, 115, 232, 0.08);
  color: #1a73e8;
  font-weight: 600;
}

.nav-item .el-icon {
  font-size: 19px;
  flex-shrink: 0;
}

.sidebar-footer {
  padding: 12px 16px;
  margin: 8px 8px 10px;
  border-radius: 8px;
  background: #f8f9fa;
  flex-shrink: 0;
}

.online-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #5f6368;
}

.online-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #dadce0;
}

.online-dot.on {
  background: #34a853;
  box-shadow: 0 0 0 0 rgba(52, 168, 83, 0.6);
  animation: pulse 1.8s infinite;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(52, 168, 83, 0.5); }
  70% { box-shadow: 0 0 0 7px rgba(52, 168, 83, 0); }
  100% { box-shadow: 0 0 0 0 rgba(52, 168, 83, 0); }
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
  min-height: 0;
}

.topbar {
  height: 56px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #ffffff;
  border-bottom: 1px solid #e8eaed;
}

.topbar-title {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 16px;
  font-weight: 600;
  color: #202124;
}

.topbar-icon {
  font-size: 18px;
  color: #1a73e8;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 14px;
}

.topbar-time {
  font-size: 13px;
  color: #80868b;
  font-variant-numeric: tabular-nums;
}

.content {
  flex: 1;
  min-height: 0;
  overflow: auto;
  background: #f8f9fa;
  padding: 20px 24px;
}
</style>
