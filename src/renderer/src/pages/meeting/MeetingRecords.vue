<template>
  <div class="records-container" ref="scrollRef" @scroll="handleScroll">
    <div class="records-list">
      <div v-for="record in records" :key="record.meetingId" class="record-item">
        <div class="record-info">
          <h4>{{ record.meetingName || '未命名通话' }}</h4>
          <p class="record-time">
            <span v-if="record.startTime">开始：{{ record.startTime }}</span>
            <span v-if="record.endTime" class="end-time">结束：{{ record.endTime }}</span>
          </p>
          <p class="record-meta">
            <span>通话号：{{ record.meetingNo }}</span>
            <span class="creator-id">发起人：{{ record.createUserId }}</span>
          </p>
        </div>
        <div class="record-actions">
          <span class="status-tag" :class="statusClass(record.status)">{{ statusText(record.status) }}</span>
          <el-button type="primary" link size="small" @click="openChatHistory(record)" class="chat-btn">
            <el-icon><ChatDotRound /></el-icon>
            聊天记录
          </el-button>
        </div>
      </div>

      <div v-if="loading" class="loading-tip">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      <div v-else-if="noMore && records.length > 0" class="no-more-tip">没有更多了</div>
      <el-empty v-if="!loading && records.length === 0" description="暂无通话记录" />
    </div>

    <ChatHistoryPanel ref="chatHistoryRef" :meeting-no="activeMeetingNo" :meeting-name="activeMeetingName" />
  </div>
</template>

<script setup>
import { ref, onMounted, getCurrentInstance } from 'vue'
import { Loading, ChatDotRound } from '@element-plus/icons-vue'
import ChatHistoryPanel from '@/components/ChatHistoryPanel.vue'

const { proxy } = getCurrentInstance()

const records = ref([])
const loading = ref(false)
const noMore = ref(false)
const scrollRef = ref(null)

const pageNo = ref(1)
const pageSize = 15

const chatHistoryRef = ref(null)
const activeMeetingNo = ref('')
const activeMeetingName = ref('')

const statusClass = (status) => {
  if (status === 1) return 'ongoing'
  if (status === 2) return 'finished'
  return ''
}

const statusText = (status) => {
  if (status === 1) return '进行中'
  if (status === 2) return '已结束'
  return '未知'
}

const openChatHistory = (record) => {
  activeMeetingNo.value = record.meetingNo
  activeMeetingName.value = record.meetingName
  chatHistoryRef.value.open()
}

const fetchRecords = async () => {
  if (loading.value || noMore.value) return
  loading.value = true
  try {
    const result = await proxy.Request({
      url: proxy.Api.getMyMeetings,
      params: { pageNo: pageNo.value, pageSize }
    })
    if (result) {
      const data = result.data || {}
      const list = data.list || []
      // 过滤掉 endTime 为空的记录
      const filtered = list.filter(item => item.endTime != null)
      records.value.push(...filtered)

      const totalCount = data.totalCount || 0
      if (records.value.length >= totalCount || list.length < pageSize) {
        noMore.value = true
      } else {
        pageNo.value++
      }
    }
  } catch (e) {
    console.error('获取通话记录失败:', e)
  } finally {
    loading.value = false
  }
}

const handleScroll = () => {
  const el = scrollRef.value
  if (!el) return
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 80) {
    fetchRecords()
  }
}

onMounted(() => {
  fetchRecords()
})
</script>

<style lang="scss" scoped>
.records-container {
  width: 100%;
  height: 100%;
  overflow-y: auto;
  padding: 12px;
  box-sizing: border-box;

  &::-webkit-scrollbar { width: 5px; }
  &::-webkit-scrollbar-thumb { background: #dadce0; border-radius: 3px; }
}

.records-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.record-item {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 14px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background 0.2s;

  &:hover { background: #eef1f5; }
}

.record-info {
  flex: 1; min-width: 0;

  h4 {
    margin: 0 0 6px 0;
    font-size: 14px;
    font-weight: 500;
    color: #1a1a1a;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .record-time {
    margin: 0 0 4px 0;
    font-size: 12px;
    color: #666;
    display: flex;
    gap: 16px;

    .end-time { color: #999; }
  }

  .record-meta {
    margin: 0;
    font-size: 11px;
    color: #aaa;
    display: flex;
    gap: 16px;

    .creator-id { color: #999; }
  }
}

.record-actions {
  flex-shrink: 0;
  margin-left: 12px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.status-tag {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;

  &.ongoing { background: #e8f5e9; color: #2e7d32; }
  &.finished { background: #f5f5f5; color: #9e9e9e; }
}

.chat-btn {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 2px;
}

.loading-tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 0;
  font-size: 13px;
  color: #999;
}

.no-more-tip {
  text-align: center;
  padding: 12px 0;
  font-size: 12px;
  color: #ccc;
}
</style>
