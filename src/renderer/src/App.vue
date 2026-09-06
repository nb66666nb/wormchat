<template>
  <router-view />
</template>

<script setup>
import { onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
let offForceOffline = null

let offSound = null

// 用 Web Audio 合成提示音（无需音频文件，离线可用）
let audioCtx = null
const playNotificationSound = () => {
  try {
    audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)()
    if (audioCtx.state === 'suspended') audioCtx.resume()
    const o = audioCtx.createOscillator()
    const g = audioCtx.createGain()
    o.type = 'sine'
    o.frequency.setValueAtTime(880, audioCtx.currentTime)
    o.frequency.exponentialRampToValueAtTime(1320, audioCtx.currentTime + 0.08)
    g.gain.setValueAtTime(0.0001, audioCtx.currentTime)
    g.gain.exponentialRampToValueAtTime(0.25, audioCtx.currentTime + 0.02)
    g.gain.exponentialRampToValueAtTime(0.0001, audioCtx.currentTime + 0.35)
    o.connect(g)
    g.connect(audioCtx.destination)
    o.start()
    o.stop(audioCtx.currentTime + 0.36)
  } catch (_) {}
}

const handleForceOffline = (data) => {
  const reason = data?.reason || '你已被管理员强制下线'
  // 清除本地登录态
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  // 通知主进程关闭 WebSocket（幂等，已关闭则无影响）
  try { window.electron?.ipcRenderer?.invoke('logout') } catch (_) {}
  ElMessage.error(reason)
  // 延迟跳转，让消息提示先显示
  setTimeout(() => router.push('/'), 200)
}

onMounted(() => {
  if (window.messageAPI?.onForceOffline) {
    offForceOffline = window.messageAPI.onForceOffline(handleForceOffline)
  }
  if (window.messageAPI?.onPlayNotificationSound) {
    offSound = window.messageAPI.onPlayNotificationSound(playNotificationSound)
  }
})

onUnmounted(() => {
  if (offForceOffline) offForceOffline()
  if (offSound) offSound()
})
</script>

<style lang="scss" scoped></style>
