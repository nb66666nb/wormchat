<template>
  <div class="settings-container">
    <div class="left-panel">
      <div class="panel-header">
        <h2>设置</h2>
      </div>
      <div class="menu-list">
        <div
          v-for="item in menuItems"
          :key="item.key"
          :class="['menu-item', { active: currentMenu === item.key }]"
          @click="handleMenuClick(item.key)"
        >
          <span class="menu-icon">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </div>

    <div class="right-panel">
      <PersonalInfo v-if="currentMenu === 'personal'" />
      <SoftwareSettings v-else-if="currentMenu === 'software'" />
      <About v-else-if="currentMenu === 'about'" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import PersonalInfo from './PersonalInfo.vue'
import SoftwareSettings from './SoftwareSettings.vue'
import About from './About.vue'

const currentMenu = ref('personal')

const menuItems = [
  { key: 'personal', label: '个人信息', icon: '👤' },
  { key: 'software', label: '软件设置', icon: '⚙️' },
  { key: 'about', label: '关于', icon: 'ℹ️' }
]

const handleMenuClick = (key) => {
  currentMenu.value = key
}
</script>

<style lang="css" scoped>
.settings-container {
  width: 100%;
  height: 100%;
  display: flex;
  background: #f8f9fa;
}

.left-panel {
  width: 240px;
  min-width: 240px;
  height: 100%;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e8eaed;
  flex-shrink: 0;
}

.panel-header {
  padding: 24px 20px 16px;
  flex-shrink: 0;
}

.panel-header h2 {
  margin: 0;
  color: #202124;
  font-size: 17px;
  font-weight: 700;
}

.menu-list {
  padding: 4px 10px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 52px;
  padding: 0 16px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
  color: #5f6368;
  font-weight: 500;
  transition: all 0.2s ease;
}

.menu-item:hover {
  background: #f1f3f4;
  color: #202124;
}

.menu-item.active {
  background: rgba(26, 115, 232, 0.08);
  color: #1a73e8;
  font-weight: 600;
}

.menu-icon {
  font-size: 17px;
  width: 22px;
  text-align: center;
  flex-shrink: 0;
}

.right-panel {
  flex: 1;
  height: 100%;
  overflow-y: auto;
  background: #ffffff;
  padding: 24px;
}
</style>
