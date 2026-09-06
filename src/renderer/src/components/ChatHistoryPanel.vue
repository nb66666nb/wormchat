<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    direction="rtl"
    size="420px"
    :before-close="handleClose"
    class="chat-history-drawer"
  >
    <div class="chat-history-panel" ref="scrollRef" @scroll="handleScroll">
      <div v-if="loadingMore && messages.length === 0" class="loading-tip">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="messages.length === 0" class="empty-tip">
        <el-empty description="暂无聊天记录" :image-size="80" />
      </div>

      <div v-else class="message-list">
        <div v-if="loadingMore" class="loading-top">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>加载更多...</span>
        </div>

        <div
          v-for="msg in messages"
          :key="msg.messageId"
          class="message-item"
          :class="{ 'is-mine': msg.sendUserId === currentUserId }"
        >
          <ChatMessageBubble
            :msg="msg"
            :current-user-id="currentUserId"
            :participants="participants"
            @preview-image="handlePreviewImage"
            @file-click="handleFileClick"
          />
        </div>

        <div v-if="noMore && messages.length > 0" class="no-more-tip">没有更多了</div>
      </div>
    </div>

    <!-- 图片预览 -->
    <el-dialog
      v-model="previewVisible"
      :show-close="true"
      :show-header="false"
      custom-class="media-preview-dialog"
      append-to-body
      fullscreen
    >
      <div class="preview-wrapper">
        <img :src="previewSrc" alt="preview" class="preview-img" />
      </div>
    </el-dialog>
  </el-drawer>
</template>

<script setup>
import { ref, computed, nextTick, getCurrentInstance } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import ChatMessageBubble from '@/components/ChatMessageBubble.vue'

const { proxy } = getCurrentInstance()

const props = defineProps({
  meetingNo: { type: String, default: '' },
  meetingName: { type: String, default: '' },
  participants: { type: Array, default: () => [] }
})

const visible = ref(false)
const messages = ref([])
const loadingMore = ref(false)
const noMore = ref(false)
const scrollRef = ref(null)
const pageNo = ref(1)
const pageSize = 20

// 图片预览
const previewVisible = ref(false)
const previewSrc = ref('')

const currentUserId = computed(() => {
  try {
    const info = JSON.parse(localStorage.getItem('userInfo') || '{}')
    return info.userId || ''
  } catch (_) { return '' }
})

const drawerTitle = computed(() => `${props.meetingName || '通话'} - 聊天记录`)

const open = () => {
  messages.value = []
  pageNo.value = 1
  noMore.value = false
  visible.value = true
  nextTick(() => fetchMessages())
}

const handleClose = () => {
  visible.value = false
}

const fetchMessages = async (skipLoading = false) => {
  if (noMore.value) return
  if (!skipLoading) loadingMore.value = true
  try {
    const result = await proxy.Request({
      url: proxy.Api.getChatMessages,
      params: { meetingNo: props.meetingNo, pageNo: pageNo.value, pageSize }
    })
    if (result) {
      const data = result.data || {}
      const list = data.list || []
      // 后端按 send_time DESC 返回，反转后 prepend 到列表头部
      // 避免全量 sort，O(n) 即可
      const reversed = list.slice().reverse()
      messages.value.unshift(...reversed)

      const totalCount = data.totalCount || 0
      if (messages.value.length >= totalCount || list.length < pageSize) {
        noMore.value = true
      } else {
        pageNo.value++
      }
    }
  } catch (e) {
    console.error('获取聊天记录失败:', e)
  } finally {
    if (!skipLoading) loadingMore.value = false
  }
}

const handleScroll = () => {
  const el = scrollRef.value
  if (!el) return
  if (el.scrollTop <= 60) {
    // 先触发 loading 状态变更，等 DOM 更新后再捕获 scroll 基准值
    loadingMore.value = true
    nextTick(() => {
      const scrollTopBefore = el.scrollTop
      const scrollHeightBefore = el.scrollHeight

      fetchMessages(true).then(() => {
        nextTick(() => {
          const scrollHeightAfter = el.scrollHeight
          el.scrollTop = scrollTopBefore + (scrollHeightAfter - scrollHeightBefore)
        })
      })
    })
  }
}

const handlePreviewImage = (msg) => {
  let src = ''
  if (msg.localPath) {
    src = 'file://' + msg.localPath
  } else if (msg.filePath) {
    try {
      const config = JSON.parse(localStorage.getItem('serverConfig') || '{}')
      src = `http://${config.host}:${config.httpPort || 6060}/api${msg.filePath}`
    } catch (_) {}
  }
  if (src) {
    previewSrc.value = src
    previewVisible.value = true
  }
}

const handleFileClick = (msg) => {
  // 历史记录中文件点击：通过 HTTP 下载
  if (msg.filePath) {
    try {
      const config = JSON.parse(localStorage.getItem('serverConfig') || '{}')
      const url = `http://${config.host}:${config.httpPort || 6060}/api${msg.filePath}`
      window.open(url, '_blank')
    } catch (_) {}
  }
}

defineExpose({ open })
</script>

<style lang="scss" scoped>
.chat-history-panel {
  height: 100%;
  overflow-y: auto;
  padding: 0 16px 16px;
  background: #f8f9fa;

  &::-webkit-scrollbar { width: 5px; }
  &::-webkit-scrollbar-thumb { background: #dadce0; border-radius: 3px; }
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-item {
  display: flex;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;

  &.is-mine {
    justify-content: flex-end;
  }
}

.loading-tip, .loading-top {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 16px 0;
  font-size: 13px;
  color: #999;
}

.empty-tip {
  padding-top: 60px;
}

.no-more-tip {
  text-align: center;
  padding: 12px 0;
  font-size: 12px;
  color: #ccc;
}

/* 图片预览 */
.preview-wrapper {
  width: 100%;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #1a1a1a;
}

.preview-img {
  max-width: 90vw;
  max-height: 90vh;
  object-fit: contain;
}
</style>

<style>
.media-preview-dialog .el-dialog {
  margin: 0 !important;
  width: 100vw !important;
  height: 100vh !important;
  max-width: none !important;
  background: #1a1a1a;
  border-radius: 0;
}
.media-preview-dialog .el-dialog__body {
  padding: 0;
  height: 100vh;
  overflow: hidden;
}
.media-preview-dialog .el-dialog__header {
  display: none;
}
</style>
