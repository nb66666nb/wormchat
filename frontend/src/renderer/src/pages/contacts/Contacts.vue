<template>
  <div class="contacts-container">
    <!-- 左侧导航面板 -->
    <div class="left-panel">
      <div class="panel-header">
        <h2>通讯录</h2>
      </div>
      <div class="menu-list">
        <div
          v-for="item in menuItems"
          :key="item.key"
          :class="['menu-item', { active: activeTab === item.key }]"
          @click="goTab(item.key)"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </div>

        <!-- 我的群聊（可展开父菜单） -->
        <div
          :class="['menu-item', 'menu-item-parent', { active: isGroupTabActive }]"
          @click="groupMenuExpanded = !groupMenuExpanded"
        >
          <el-icon :size="18"><UserFilled /></el-icon>
          <span>我的群聊</span>
          <el-icon class="expand-arrow" :class="{ expanded: groupMenuExpanded }">
            <ArrowRight />
          </el-icon>
        </div>
        <div v-show="groupMenuExpanded" class="sub-menu-list">
          <div
            v-for="sub in groupSubMenuItems"
            :key="sub.key"
            :class="['sub-menu-item', { active: activeTab === sub.key }]"
            @click="goTab(sub.key)"
          >
            <span>{{ sub.label }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧内容面板（子路由） -->
    <div class="right-panel">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search as SearchIcon, User, Message, ChatLineSquare, UserFilled, ArrowRight } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 普通菜单项
const menuItems = [
  { key: 'search', label: '搜索用户', icon: SearchIcon },
  { key: 'friends', label: '我的好友', icon: User },
  { key: 'myRequests', label: '我的申请', icon: Message },
  { key: 'pendingRequests', label: '待处理', icon: ChatLineSquare },
]

// 我的群聊子菜单
const groupSubMenuItems = [
  { key: 'myCreatedGroups', label: '我创建的群聊' },
  { key: 'myJoinedGroups', label: '我加入的群聊' },
]

// 当前激活标签：从路由路径取最后一段（路由驱动）
const activeTab = computed(() => route.path.split('/').pop() || 'search')

// 群聊子菜单是否激活（父项联动高亮）
const isGroupTabActive = computed(() =>
  groupSubMenuItems.some((sub) => sub.key === activeTab.value)
)

// 群聊菜单展开状态：进入群聊子路由时自动展开
const groupMenuExpanded = ref(isGroupTabActive.value)
watch(isGroupTabActive, (val) => {
  if (val) groupMenuExpanded.value = true
})

// 菜单跳转
const goTab = (key) => {
  router.push(`/main/contacts/${key}`)
}
</script>

<style lang="css" scoped>
.contacts-container {
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
  flex-shrink: 0;
  border-right: 1px solid #e8eaed;
}

.panel-header {
  padding: 24px 20px 16px;
  border-bottom: 0;
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
  color: #5f6368;
  font-size: 14px;
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

.menu-item-parent .expand-arrow {
  margin-left: auto;
  transition: transform 0.2s ease;
  font-size: 12px;
}

.menu-item-parent .expand-arrow.expanded {
  transform: rotate(90deg);
}

.sub-menu-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-left: 26px;
}

.sub-menu-item {
  display: flex;
  align-items: center;
  height: 44px;
  padding: 0 16px;
  border-radius: 8px;
  cursor: pointer;
  color: #5f6368;
  font-size: 13px;
  transition: all 0.2s ease;
}

.sub-menu-item:hover {
  background: #f1f3f4;
  color: #202124;
}

.sub-menu-item.active {
  background: rgba(26, 115, 232, 0.08);
  color: #1a73e8;
  font-weight: 600;
}

.right-panel {
  flex: 1;
  height: 100%;
  background: #ffffff;
  overflow-y: auto;
  padding: 24px;
}
</style>
