<template>
  <div
    class="chat-avatar"
    :style="{ width: size + 'px', height: size + 'px', fontSize: Math.round(size * 0.42) + 'px' }"
  >
    <img
      v-if="avatarFileUrl"
      :src="avatarFileUrl"
      class="chat-avatar-img"
      @error="handleImgError"
    />
    <span v-else class="chat-avatar-placeholder" :style="{ background: bgColor }">
      {{ placeholderText }}
    </span>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'

const props = defineProps({
  userId: { type: [String, Number], default: '' },
  fileId: { type: String, default: '' },
  filePath: { type: String, default: '' },
  fileType: { type: [Number, String], default: 1 },
  userName: { type: String, default: '' },
  size: { type: Number, default: 32 }
})

const avatarLocalPath = ref('')
const imgLoadFailed = ref(false)

const placeholderText = computed(() => {
  const name = (props.userName || '').trim()
  if (name) {
    const ch = name.charAt(0)
    return ch.toUpperCase()
  }
  if (props.userId) return String(props.userId).slice(-1).toUpperCase()
  return 'U'
})

const colorPalette = [
  '#67c23a', '#409eff', '#e6a23c', '#f56c6c',
  '#909399', '#8e44ad', '#16a085', '#d35400',
  '#2980b9', '#c0392b', '#27ae60', '#8c7ae6'
]

const bgColor = computed(() => {
  let hash = 0
  const str = (String(props.userId || '') || props.userName || 'user')
  for (let i = 0; i < str.length; i++) hash = (hash * 31 + str.charCodeAt(i)) & 0xfffffff
  return colorPalette[hash % colorPalette.length]
})

const avatarFileUrl = computed(() => {
  if (!avatarLocalPath.value || imgLoadFailed.value) return ''
  let p = avatarLocalPath.value
  p = p.replace(/\\/g, '/')
  if (!p.startsWith('/')) p = '/' + p
  return 'file://' + p
})

const loadAvatar = async () => {
  imgLoadFailed.value = false
  const uid = String(props.userId || '').trim()
  const hasFallback = props.fileId || props.filePath

  // 判断是否为机器人头像：filePath 以 /avatar/ 开头即视为机器人头像
  // （机器人头像走独立缓存目录 bot_<fileId>.<ext>）
  const isBotAvatar = typeof props.filePath === 'string' && props.filePath.startsWith('/avatar/')
  const scope = isBotAvatar ? 'bot' : 'user'

  if (!uid && !hasFallback) {
    avatarLocalPath.value = ''
    return
  }
  if (!window.api) {
    avatarLocalPath.value = ''
    return
  }

  // 优先：按 userId 查找头像（跨用户场景，参会者头像）
  if (uid && window.api.checkAvatar) {
    try {
      const result = await window.api.checkAvatar({
        userId: uid,
        fileId: props.fileId,
        filePath: props.filePath,
        scope
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
          fileType: props.fileType,
          scope
        })
        if (dl && dl.success && dl.data && dl.data.localPath) {
          avatarLocalPath.value = dl.data.localPath
          return
        }
      }
    } catch (_) {}
  }

  // 回退：按 fileId/filePath 检查文件消息系统
  if (window.api.checkMeetingFile && (props.fileId || props.filePath)) {
    try {
      const result = await window.api.checkMeetingFile({
        fileId: props.fileId,
        filePath: props.filePath,
        fileType: props.fileType
      })
      if (result && result.exists && result.localPath) {
        avatarLocalPath.value = result.localPath
        return
      }
      if (window.api.downloadMeetingFile) {
        const lp = await window.api.downloadMeetingFile({
          fileId: props.fileId,
          filePath: props.filePath,
          fileType: props.fileType
        })
        if (lp && (lp.data?.localPath || lp.localPath)) {
          avatarLocalPath.value = lp.data?.localPath || lp.localPath
          return
        }
      }
    } catch (_) {}
  }

  avatarLocalPath.value = ''
}

const handleImgError = () => {
  imgLoadFailed.value = true
}

onMounted(loadAvatar)
watch(() => [props.userId, props.fileId, props.filePath], loadAvatar)
</script>

<style scoped>
.chat-avatar {
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: #e8eaed;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.chat-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  image-rendering: auto;
  -webkit-backface-visibility: hidden;
  backface-visibility: hidden;
}

.chat-avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 500;
  user-select: none;
  letter-spacing: 0.5px;
}
</style>
