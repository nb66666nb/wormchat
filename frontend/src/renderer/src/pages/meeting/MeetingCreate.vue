<template>
  <div class="meeting-create-page">
    <div class="create-form">
      <div class="form-section">
        <label class="section-label">通话主题</label>
        <el-input
          v-model="meetingTopic"
          placeholder="请输入通话主题"
        />
      </div>

      <div class="form-section">
        <label class="section-label">通话号</label>
        <el-radio-group v-model="meetingIdType">
          <el-radio value="random">系统随机</el-radio>
        </el-radio-group>
      </div>

      <div class="form-section">
        <label class="section-label">通话权限</label>
        <el-switch
          v-model="isPublic"
          :active-value="true"
          :inactive-value="false"
          active-text="公开"
          inactive-text="设置密码"
        />
      </div>

      <div class="form-section" v-show="!isPublic">
        <label class="section-label">通话密码</label>
        <el-input
          v-model="meetingPassword"
          type="password"
          placeholder="请输入通话密码"
          show-password
        />
      </div>

      <div class="form-actions">
        <el-button type="primary" @click="handleCreate" :loading="creating">
          创建通话
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { ElMessage } from 'element-plus'

const { proxy } = getCurrentInstance()
const creating = ref(false)
const meetingIdType = ref('random')
const isPublic = ref(false)
const meetingPassword = ref('')
const meetingTopic = ref('')

const validateForm = () => {
  if (!meetingTopic.value.trim()) {
    ElMessage.warning('请输入通话主题')
    return false
  }
  if (!isPublic.value && !meetingPassword.value.trim()) {
    ElMessage.warning('请输入通话密码')
    return false
  }
  return true
}

const handleCreate = async () => {
  if (!validateForm()) return

  creating.value = true

  const result = await proxy.Request({
    url: proxy.Api.createMeeting,
    params: {
      joinType: isPublic.value ? 0 : 1,
      meetingNoType: 1,
      password: isPublic.value ? null : meetingPassword.value,
      meetingNickName: meetingTopic.value
    }
  })

  creating.value = false

  if (!result) return

  if (result.code !== 200) {
    ElMessage.error(result.info || '创建通话失败')
    return
  }

  ElMessage.success('通话创建成功')

  const meetingData = {
    meetingId: result.data?.meetingId || null,
    meetingNo: result.data?.meetingNo || null,
    meetingName: result.data?.meetingName || meetingTopic.value,
    createTime: result.data?.createTime || null,
    createUserId: result.data?.createUserId || null,
    jionType: result.data?.jionType || (isPublic.value ? 0 : 1),
    jionPassword: result.data?.jionPassword || null,
    startTime: result.data?.startTime || null,
    endTime: result.data?.endTime || null,
    status: result.data?.status || null
  }

  window.electron.ipcRenderer.send('open-meeting-window', meetingData)

  // 重置表单
  meetingTopic.value = ''
  meetingPassword.value = ''
  isPublic.value = false
}
</script>

<style lang="css" scoped>
.meeting-create-page {
  width: 100%;
}

.create-form {
  max-width: 400px;
}

.form-section {
  margin-bottom: 20px;
}

.section-label {
  display: block;
  font-size: 13px;
  color: #5f6368;
  margin-bottom: 8px;
  font-weight: 500;
}

.form-actions {
  margin-top: 28px;
}
</style>
