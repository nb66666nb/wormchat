<template>
  <div class="avatar-upload" :style="{ width: size + 'px', height: size + 'px' }">
    <div
      class="avatar-circle"
      :style="{ width: size + 'px', height: size + 'px', fontSize: size * 0.4 + 'px' }"
      @click="handleClick"
      :class="{ 'is-uploading': uploading }"
    >
      <img
        v-if="avatarFileUrl"
        :src="avatarFileUrl"
        class="avatar-image"
      />
      <span v-else class="avatar-placeholder">{{ placeholderText }}</span>

      <div v-if="editable && !uploading" class="avatar-overlay">
        <el-icon :size="Math.max(14, size * 0.22)"><Edit /></el-icon>
        <span class="overlay-text" v-if="size >= 60">点击上传</span>
      </div>

      <div v-if="uploading" class="avatar-overlay uploading">
        <el-icon :size="Math.max(14, size * 0.22)" class="is-loading"><Loading /></el-icon>
        <span class="overlay-text" v-if="size >= 60">上传中</span>
      </div>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      accept="image/*"
      style="display: none"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Edit, Loading } from '@element-plus/icons-vue'

const props = defineProps({
  userId: { type: [String, Number], default: '' },
  userName: { type: String, default: '' },
  size: { type: Number, default: 80 },
  editable: { type: Boolean, default: true },
  fileId: { type: String, default: '' },
  filePath: { type: String, default: '' }
})

const emit = defineEmits(['update', 'upload-success', 'upload-error'])

const fileInputRef = ref(null)
const uploading = ref(false)
const avatarLocalPath = ref('')

const placeholderText = computed(() => {
  const name = props.userName?.trim()
  if (!name) return 'U'
  const ch = name.charAt(0)
  return ch.toUpperCase()
})

const avatarFileUrl = computed(() => {
  if (!avatarLocalPath.value) return ''
  let p = avatarLocalPath.value
  p = p.replace(/\\/g, '/')
  if (!p.startsWith('/')) p = '/' + p
  return 'file://' + p
})

const loadAvatar = async () => {
  const uid = String(props.userId || '').trim()
  if (!uid && !props.fileId && !props.filePath) {
    avatarLocalPath.value = ''
    return
  }
  if (!window.api) {
    avatarLocalPath.value = ''
    return
  }

  if (uid && window.api.checkAvatar) {
    try {
      const result = await window.api.checkAvatar({
        userId: uid,
        fileId: props.fileId,
        filePath: props.filePath
      })
      if (result && result.exists && result.localPath) {
        avatarLocalPath.value = result.localPath
        return
      }
      if (window.api.downloadAvatar && (props.fileId || props.filePath)) {
        const dl = await window.api.downloadAvatar({
          userId: uid,
          fileId: props.fileId,
          filePath: props.filePath,
          fileType: 1
        })
        if (dl && dl.success && dl.data && dl.data.localPath) {
          avatarLocalPath.value = dl.data.localPath
          return
        }
      }
    } catch (_) {}
  }

  if (window.api.checkMeetingFile && (props.fileId || props.filePath)) {
    try {
      const result = await window.api.checkMeetingFile({
        fileId: props.fileId,
        filePath: props.filePath,
        fileType: 1
      })
      if (result && result.exists && result.localPath) {
        avatarLocalPath.value = result.localPath
        return
      }
      if (window.api.downloadMeetingFile) {
        const localPath = await window.api.downloadMeetingFile({
          fileId: props.fileId,
          filePath: props.filePath,
          fileType: 1
        })
        if (localPath && (localPath.data?.localPath || localPath.localPath)) {
          avatarLocalPath.value = localPath.data?.localPath || localPath.localPath
        }
      }
    } catch (_) {}
  }
  avatarLocalPath.value = ''
}

onMounted(loadAvatar)
watch(() => [props.userId, props.fileId, props.filePath], loadAvatar)

const handleClick = () => {
  if (!props.editable) return
  if (uploading.value) return
  fileInputRef.value?.click()
}

const isImageFile = (file) => {
  if (!file) return false
  if (file.type && file.type.startsWith('image/')) return true
  const name = file.name?.toLowerCase() || ''
  return /\.(png|jpe?g|gif|bmp|webp|svg)$/.test(name)
}

const handleFileChange = async (evt) => {
  const file = evt.target.files?.[0]
  evt.target.value = ''
  if (!file) return
  if (!isImageFile(file)) {
    ElMessage.warning('仅支持图片格式')
    return
  }

  const uid = String(props.userId || '').trim()
  if (!uid) {
    ElMessage.error('用户 ID 不能为空')
    return
  }

  uploading.value = true
  try {
    const arrayBuffer = await file.arrayBuffer()
    const buffer = new Uint8Array(arrayBuffer)

    let result = null
    if (window.api && window.api.uploadAvatar) {
      result = await window.api.uploadAvatar({
        userId: String(uid),
        fileBuffer: Array.from(buffer),
        fileName: file.name
      })
    }

    if (!result || !result.success) {
      if (window.api && window.api.uploadMeetingFile) {
        result = await window.api.uploadMeetingFile({
          fileBuffer: Array.from(buffer),
          fileName: file.name,
          meetingNo: 'avatar_' + uid
        })
      }
    }

    const fileData = (result && result.data) ? result.data : (result && result.fileId ? result : null)
    if (!fileData) {
      throw new Error('上传响应数据为空')
    }

    emit('upload-success', fileData)
    emit('update', fileData)

    await loadAvatar()
    ElMessage.success('头像上传成功')
  } catch (e) {
    console.error('[AvatarUpload] 上传失败:', e.message)
    ElMessage.error('头像上传失败：' + (e.message || '未知错误'))
    emit('upload-error', e)
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.avatar-upload {
  display: inline-block;
  position: relative;
}

.avatar-circle {
  position: relative;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #fff;
  font-weight: 600;
  user-select: none;
  transition: box-shadow 0.2s ease, transform 0.2s ease;

  &:hover {
    box-shadow: 0 4px 14px rgba(102, 126, 234, 0.35);
    transform: translateY(-1px);
  }

  &.is-uploading {
    cursor: wait;
  }
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-placeholder {
  letter-spacing: 0.5px;
}

.avatar-overlay {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 36%;
  background: rgba(0, 0, 0, 0.45);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.2s ease;

  .overlay-text {
    font-size: 10px;
    font-weight: 400;
    line-height: 1;
  }

  &.uploading {
    opacity: 0.8;
    background: rgba(0, 0, 0, 0.6);
  }
}

.avatar-circle:hover .avatar-overlay:not(.uploading) {
  opacity: 0.85;
}
</style>
