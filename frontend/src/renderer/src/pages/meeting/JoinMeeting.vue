<template>
  <div class="join-container">
    <div class="search-section">
      <el-input
        v-model="searchMeetingNo"
        placeholder="请输入通话号"
        size="small"
        clearable
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" size="small" :icon="Search" @click="handleSearch" class="search-btn">搜索</el-button>
    </div>

    <div v-if="meetingInfo" class="meeting-info">
      <div class="info-item">
        <span class="info-label">通话名称：</span>
        <span class="info-value">{{ meetingInfo.meetingName }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">通话号：</span>
        <span class="info-value">{{ meetingInfo.meetingNo }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">创建人ID：</span>
        <span class="info-value">{{ meetingInfo.createUserId }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">创建时间：</span>
        <span class="info-value">{{ meetingInfo.createTime }}</span>
      </div>
      <div v-if="meetingInfo.startTime" class="info-item">
        <span class="info-label">开始时间：</span>
        <span class="info-value">{{ meetingInfo.startTime }}</span>
      </div>
      <div v-if="meetingInfo.endTime" class="info-item">
        <span class="info-label">结束时间：</span>
        <span class="info-value">{{ meetingInfo.endTime }}</span>
      </div>
      <div class="info-item">
        <span class="info-label">加入方式：</span>
        <el-tag :type="meetingInfo.jionType === 0 ? 'success' : 'warning'" size="small">
          {{ meetingInfo.jionType === 0 ? '公开' : '需要密码' }}
        </el-tag>
      </div>
      <div class="info-item">
        <span class="info-label">通话状态：</span>
        <el-tag :type="getStatusType(meetingInfo.status)" size="small">
          {{ getStatusText(meetingInfo.status) }}
        </el-tag>
      </div>
    </div>

    <div v-if="meetingInfo && meetingInfo.status !== 2" class="join-form">
      <el-button type="primary" @click="handleJoin">加入通话</el-button>
    </div>

    <div v-if="meetingInfo && meetingInfo.status === 2" class="join-form ended-tip">
      <span>该通话已结束，无法加入</span>
    </div>

    <el-dialog
      v-model="showPasswordDialog"
      title="请输入通话密码"
      width="360px"
      :close-on-click-modal="false"
    >
      <el-input
        v-model="inputPassword"
        type="password"
        placeholder="请输入通话密码"
        show-password
        @keyup.enter="confirmJoinWithPassword"
      />
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmJoinWithPassword" :disabled="!inputPassword.trim()">确认</el-button>
      </template>
    </el-dialog>

    <div v-if="!meetingInfo && hasSearched" class="no-result">
      <el-empty description="未找到该通话" :image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, getCurrentInstance } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const { proxy } = getCurrentInstance()

const searchMeetingNo = ref('')
const meetingInfo = ref(null)
const hasSearched = ref(false)
const showPasswordDialog = ref(false)
const inputPassword = ref('')

const joinForm = reactive({
  meetingId: '',
  meetingNo: '',
  password: ''
})

const handleSearch = async () => {
  if (!searchMeetingNo.value) {
    ElMessage.warning('请输入通话号')
    return
  }

  hasSearched.value = true
  meetingInfo.value = null

  const result = await proxy.Request({
    url: proxy.Api.getMeetingInfo,
    params: {
      meetingNo: searchMeetingNo.value
    }
  })

  if (!result) {
    return
  }

  if (result.code !== 200) {
    ElMessage.error(result.info || '查询失败')
    return
  }

  meetingInfo.value = result.data
  joinForm.meetingId = result.data.meetingId
  joinForm.meetingNo = result.data.meetingNo
}

const getStatusType = (status) => {
  const typeMap = { 0: 'info', 1: 'success', 2: 'danger' }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = { 0: '未开始', 1: '进行中', 2: '已结束' }
  return textMap[status] || '未知'
}

const handleJoin = async () => {
  if (!joinForm.meetingId) {
    ElMessage.warning('请先搜索通话')
    return
  }

  if (meetingInfo.value.jionType === 0) {
    await doJoin('')
  } else {
    inputPassword.value = ''
    showPasswordDialog.value = true
  }
}

const confirmJoinWithPassword = async () => {
  const password = inputPassword.value.trim()
  if (!password) {
    ElMessage.warning('请输入通话密码')
    return
  }
  showPasswordDialog.value = false
  await doJoin(password)
}

const doJoin = async (password) => {
  const params = {
    meetingNo: meetingInfo.value?.meetingNo
  }
  if (password) {
    params.password = password
  }
  console.log(params)

 const result = await proxy.Request({
    url: proxy.Api.joinMeeting,
    params
  })

  if (!result) {
    return
  }

  if (result.code !== 200) {
    ElMessage.error(result.info || '加入通话失败')
    return
  }

  ElMessage.success('加入通话成功')

  const meetingData = {
    meetingId: meetingInfo.value?.meetingId,
    meetingNo: meetingInfo.value?.meetingNo,
    meetingName: meetingInfo.value?.meetingName,
    createTime: meetingInfo.value?.createTime,
    createUserId: meetingInfo.value?.createUserId,
    jionType: meetingInfo.value?.jionType,
    jionPassword: meetingInfo.value?.jionPassword,
    startTime: meetingInfo.value?.startTime,
    endTime: meetingInfo.value?.endTime,
    status: meetingInfo.value?.status
  }

  window.electron.ipcRenderer.send('open-meeting-window', meetingData)
}
</script>

<style lang="css" scoped>
.join-container {
  width: 100%;
  max-width: 360px;
}

.search-section {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.search-section .el-input {
  flex: 1;
}

.search-btn {
  flex-shrink: 0;
}

.meeting-info {
  padding: 12px 16px;
  background: #f8f9fa;
  border-bottom: 1px solid #eee;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  font-size: 13px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  color: #909399;
  min-width: 80px;
  flex-shrink: 0;
}

.info-value {
  color: #303133;
}

.join-form {
  padding: 12px 16px;
}

.ended-tip {
  text-align: center;
  color: #f56c6c;
  font-size: 13px;
}

.no-result {
  display: flex;
  justify-content: center;
  padding: 20px;
}
</style>
