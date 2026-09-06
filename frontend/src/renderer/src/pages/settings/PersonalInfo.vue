<template>
  <div class="personal-info">
    <div class="section-title">个人信息</div>

    <div class="avatar-section">
      <AvatarUpload
        :user-id="userInfo.userId"
        :file-id="avatarInfo.fileId"
        :file-path="avatarInfo.filePath"
        :user-name="userInfo.nickname || userInfo.userName"
        :size="80"
        @update="handleAvatarUpdate"
      />
      <div class="avatar-hint">点击头像可上传图片</div>
    </div>

    <el-form :model="userInfo" label-width="80px" size="small" class="info-form">
      <el-form-item label="用户ID">
        <el-input v-model="userInfo.userId" disabled />
      </el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="userInfo.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="邮箱">
        <el-input v-model="userInfo.email" disabled />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSave">保存修改</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="logout-section">
      <el-button type="danger" plain @click="handleLogout" class="logout-btn">
        退出登录
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, onMounted } from "vue"
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import AvatarUpload from '../../components/AvatarUpload.vue'

const { proxy } = getCurrentInstance()
const router = useRouter()

const userInfo = reactive({
  userId: '',
  nickname: '',
  email: ''
})

const avatarInfo = reactive({
  fileId: '',
  filePath: ''
})

onMounted(() => {
  const storedInfo = localStorage.getItem('userInfo')
  if (storedInfo) {
    try {
      const data = JSON.parse(storedInfo)
      Object.assign(userInfo, {
        userId: data.userId || '',
        nickname: data.nickname || data.userName || '',
        email: data.email || ''
      })
      avatarInfo.fileId = data.avatarFileId || data.fileId || ''
      avatarInfo.filePath = data.avatarFilePath || data.filePath || ''
    } catch (e) {}
  }
})

const handleAvatarUpdate = (fileData) => {
  if (!fileData) return
  avatarInfo.fileId = fileData.fileId || ''
  avatarInfo.filePath = fileData.filePath || ''
  const storedInfo = localStorage.getItem('userInfo')
  try {
    const data = storedInfo ? JSON.parse(storedInfo) : {}
    data.avatarFileId = avatarInfo.fileId
    data.avatarFilePath = avatarInfo.filePath
    localStorage.setItem('userInfo', JSON.stringify(data))
    window.dispatchEvent(new CustomEvent('user-info-updated'))
  } catch (e) {}
}

const handleSave = () => {
  const storedInfo = localStorage.getItem('userInfo')
  try {
    const data = storedInfo ? JSON.parse(storedInfo) : {}
    data.nickname = userInfo.nickname
    data.avatarFileId = avatarInfo.fileId
    data.avatarFilePath = avatarInfo.filePath
    localStorage.setItem('userInfo', JSON.stringify(data))
  } catch (e) {}
  ElMessage.success('个人信息已保存')
}

const handleReset = () => {
  onMounted()
  ElMessage.info('已重置为原始信息')
}

const handleLogout = async () => {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    const result = await proxy.Request({
      url: proxy.Api.logout,
      params: {}
    })
    if (!result) {
      return
    }
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    router.push('/')
    ElMessage.success('已退出登录')
  }).catch(() => {})
}
</script>

<style lang="css" scoped>
.personal-info {
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

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.avatar-hint {
  font-size: 12px;
  color: #9aa0a6;
}

.info-form {
  max-width: 400px;
}

.logout-section {
  margin-top: 32px;
  padding-top: 20px;
  border-top: 1px solid #eee;
  text-align: center;
}

.logout-btn {
  width: 200px;
}
</style>
