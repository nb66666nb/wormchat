<template>
  <div class="meeting-room">
    <!-- 关闭倒计时遮罩 -->
    <div v-if="showClosingOverlay" class="closing-overlay">
      <div class="closing-content">
        <el-icon class="closing-icon" size="48"><VideoCameraFilled /></el-icon>
        <h3 class="closing-title">{{ closingTitle }}</h3>
        <p class="closing-countdown">窗口将在 <span class="countdown-number">{{ closingCountdown }}</span> 秒后关闭</p>
      </div>
    </div>

    <!-- 顶部标题栏 -->
    <div class="room-header">
      <div class="header-left">
        <h2 class="room-title">{{ meetingInfo.meetingName || '通话进行中' }}</h2>
        <span class="room-info">通话号：{{ meetingInfo.meetingNo }}</span>
      </div>
      <!-- 右侧工具按钮组 -->
      <div class="header-toolbar">
        <el-badge :value="participants.length" :max="99" :hidden="showParticipants">
          <button
            :class="['toolbar-btn', { active: showParticipants }]"
            @click="toggleParticipants"
            title="参会者"
          >
            <el-icon><User /></el-icon>
            <span>参会者</span>
          </button>
        </el-badge>
        <el-badge :value="messageCount" :hidden="messageCount === 0 || showChat" :max="99">
          <button
            :class="['toolbar-btn', { active: showChat }]"
            @click="toggleChat"
            title="聊天"
          >
            <el-icon><ChatDotRound /></el-icon>
            <span>聊天</span>
          </button>
        </el-badge>
        <button class="toolbar-btn" @click="showMeetingInfo" title="通话信息">
          <el-icon><InfoFilled /></el-icon>
        </button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="room-body">
      <!-- 视频区域 -->
      <div class="video-area">
        <div class="video-grid">
          <!-- 本地视频 -->
          <div class="video-cell local-cell">
            <video ref="localVideoRef" autoplay playsinline muted class="video-fill" />
            <div class="video-cell-footer">
              <span class="user-name">我</span>
              <span v-if="isMuted" class="status-badge muted">已静音</span>
            </div>
          </div>
          <!-- 远程视频 -->
          <div
            v-for="(p, idx) in remoteParticipants"
            :key="p.userId || idx"
            class="video-cell"
          >
            <video
              :ref="el => registerRemoteVideo(p.userId, el)"
              autoplay playsinline
              class="video-fill"
            />
            <div class="video-cell-footer">
              <span class="user-name">{{ p.nickName || p.userId }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 参会者侧滑面板 -->
      <transition name="slide-right-panel">
        <div v-if="showParticipants" class="side-panel participants-side-panel">
          <div class="side-panel-header">
            <span class="panel-title">参会者 ({{ participants.length }})</span>
            <button v-if="isCreator" class="manage-btn" @click="showManageMenu = !showManageMenu">
              <el-icon><Setting /></el-icon>
              <span>管理</span>
            </button>
            <!-- 管理下拉菜单 -->
            <div v-if="showManageMenu && isCreator" class="manage-dropdown">
              <div
                v-for="(participant, index) in participants"
                :key="'m-' + index"
                v-show="participant.userId !== currentUserId && participant.status !== 1 && participant.status !== 3"
                class="manage-item"
              >
                <span class="manage-name">{{ participant.nickName || participant.nickname || `用户${index + 1}` }}</span>
                <div class="manage-actions">
                  <el-button type="warning" size="small" text @click="handleKick(participant); showManageMenu = false">踢出</el-button>
                  <el-button type="danger" size="small" text @click="handleBlack(participant); showManageMenu = false">{{ participant.status === 3 ? '已拉黑' : '拉黑' }}</el-button>
                </div>
              </div>
              <div v-if="!participants.some(p => p.userId !== currentUserId && p.status !== 1 && p.status !== 3)" class="manage-empty">无可管理成员</div>
            </div>
          </div>
          <div class="side-panel-body">
            <div
              v-for="(participant, index) in participants"
              :key="index"
              :class="['participant-item', { offline: participant.status === 1 || participant.status === 3 }]"
            >
              <ChatAvatar
                :file-id="participant.avatarFileId || participant.fileId || ''"
                :file-path="participant.avatarFilePath || participant.filePath || ''"
                :user-name="participant.nickName || participant.nickname || ''"
                :user-id="participant.userId || participant.id || ''"
                :size="36"
              />
              <div class="participant-info">
                <div class="participant-name">
                  {{ participant.nickName || participant.nickname || `用户${index + 1}` }}
                  <span v-if="participant.memberType === 0" class="role-badge host">主持</span>
                  <span v-else-if="participant.status === 2" class="status-dot online"></span>
                </div>
                <div class="participant-role">
                  {{ getMemberTypeText(participant.memberType) }}
                  <span v-if="participant.status === 3" class="status-text blacked">(已拉黑)</span>
                  <span v-else-if="participant.status === 1" class="status-text offline">(已退出)</span>
                </div>
              </div>
            </div>
            <el-empty v-if="participants.length === 0" description="暂无参会者" :image-size="60" />
          </div>
        </div>
      </transition>

      <!-- 聊天侧滑面板 -->
      <transition name="slide-right-panel">
        <div v-if="showChat" class="side-panel chat-side-panel">
          <Chat :meetingId="meetingInfo.meetingId" :participants="participants" :visible="showChat" @update-message-count="handleMessageCountUpdate" />
        </div>
      </transition>
    </div>

    <!-- 底部控制栏 -->
    <div class="control-bar">
      <div class="ctrl-item" :class="{ off: isMuted }" @click="toggleMute">
        <el-icon class="ctrl-icon"><Microphone /></el-icon>
      </div>
      <div class="ctrl-item" :class="{ off: isCameraOff }" @click="toggleCamera">
        <el-icon class="ctrl-icon"><VideoCamera /></el-icon>
      </div>
      <div
        class="ctrl-item end-item"
        @click="isCreator ? handleEndMeeting() : handleLeaveMeeting()"
      >
        <el-icon class="ctrl-icon"><PhoneFilled /></el-icon>
      </div>
      <div class="ctrl-item" @click="showInvite">
        <el-icon class="ctrl-icon"><Plus /></el-icon>
      </div>
    </div>

    <InvitePanel ref="invitePanelRef" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, getCurrentInstance } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  VideoCameraFilled,
  Microphone,
  VideoCamera,
  ChatDotRound,
  InfoFilled,
  Plus,
  User,
  Close,
  PhoneFilled,
  Setting
} from '@element-plus/icons-vue'
import Chat from './Chat.vue'
import InvitePanel from './InvitePanel.vue'
import UserInfoStore from '@/stores/UserInfoStore'
import ChatAvatar from '@/components/ChatAvatar.vue'
import useWebRTC from '@/utils/useWebRTC'

const { proxy } = getCurrentInstance()

const props = defineProps({
  meetingData: {
    type: Object,
    default: () => ({})
  }
})

const meetingInfo = reactive({
  meetingId: '', meetingNo: '', meetingName: '', createTime: null,
  createUserId: '', jionType: 0, jionPassword: '', startTime: null,
  endTime: null, status: null
})

const participants = ref([])
const showChat = ref(false)
const showParticipants = ref(false)
const showManageMenu = ref(false)
const messageCount = ref(0)
const invitePanelRef = ref(null)

// 关闭倒计时
const showClosingOverlay = ref(false)
const closingTitle = ref('')
const closingCountdown = ref(0)
let closingTimer = null

// 当前用户
const currentUserId = UserInfoStore.getUserId()

// WebRTC
const webrtc = useWebRTC(computed(() => meetingInfo.meetingNo || ''))
const { localVideoRef, isMuted, isCameraOff, registerRemoteVideo } = webrtc

// 远程参与者（过滤自己）
const remoteParticipants = computed(() => {
  return participants.value.filter(p => {
    const pid = p.userId || p.createUserId || p.contactId
    return pid !== currentUserId
  })
})

const isCreator = computed(() => meetingInfo.createUserId === currentUserId)

// ==================== 参会者管理 ====================

const loadParticipants = async () => {
  if (!meetingInfo.meetingNo) return
  const result = await proxy.Request({
    url: proxy.Api.getMembers,
    params: { meetingNo: meetingInfo.meetingNo }
  })
  if (result && result.code === 200) {
    const oldCount = participants.value.length
    participants.value = result.data || []
    if (participants.value.length > oldCount) {
      webrtc.connectToAllParticipants(participants.value)
    }
  }
}

const getMemberTypeText = (memberType) => memberType === 0 ? '主持人' : '参会者'

const handleKick = async (member) => {
  try {
    await ElMessageBox.confirm(`确定要将 ${member.nickName || member.nickname || '该用户'} 踢出通话吗？`, '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    const result = await proxy.Request({ url: proxy.Api.kickUser, params: { meetingNo: meetingInfo.meetingNo, kickUserId: member.userId, staus: 1 } })
    if (result?.code === 200) { ElMessage.success('已踢出该成员'); loadParticipants() }
    else { ElMessage.error(result?.info || '踢出失败') }
  } catch (_) {}
}

const handleBlack = async (member) => {
  try {
    await ElMessageBox.confirm(`确定要将 ${member.nickName || member.nickname || '该用户'} 拉黑吗？`, '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    const result = await proxy.Request({ url: proxy.Api.kickUser, params: { meetingNo: meetingInfo.meetingNo, kickUserId: member.userId, staus: 3 } })
    if (result?.code === 200) { ElMessage.success('已拉黑该成员'); loadParticipants() }
    else { ElMessage.error(result?.info || '拉黑失败') }
  } catch (_) {}
}

// ==================== 信令发送 ====================
const sendSignal = (data) => {
  if (window.api?.sendMeetingSignal) window.api.sendMeetingSignal(data)
  else if (window.electron?.ipcRenderer) window.electron.ipcRenderer.send('webrtc-signal', data)
}

// ==================== 消息监听 ====================
const setupMessageListener = () => {
  if (!window.messageAPI) return
  return window.messageAPI.onNewMessage((message) => {
    const type = message.messageType
    if (type >= 20 && type <= 23) { webrtc.handleWebRtcSignal(message); return }
    switch (type) {
      case 10: case 11: loadParticipants(); break
      case 12:
        if (!showClosingOverlay.value) { webrtc.cleanup(); startClosingCountdown(5, '通话已结束') }
        break
      case 2: handleKickMessage(message); break
    }
  })
}
let cleanupListener = null

// ==================== 媒体控制 ====================
const toggleMute = () => { const m = webrtc.toggleMute(); ElMessage.info(m ? '已静音' : '已取消静音') }
const toggleCamera = () => { const off = webrtc.toggleCamera(); ElMessage.info(off ? '摄像头已关闭' : '摄像头已开启') }

const toggleChat = () => {
  showChat.value = !showChat.value
  if (showChat.value) { messageCount.value = 0; showParticipants.value = false }
}
const toggleParticipants = () => {
  showParticipants.value = !showParticipants.value
  if (showParticipants.value) showChat.value = false
}
const handleMessageCountUpdate = (count) => { if (!showChat.value) messageCount.value = count }

// ==================== 通话生命周期 ====================
const handleEndMeeting = async () => {
  try {
    await ElMessageBox.confirm('确定要结束通话吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const result = await proxy.Request({ url: proxy.Api.endMeeting, params: { meetingNo: meetingInfo.meetingNo } })
    if (!result || result.code !== 200) { ElMessage.error(result?.info || '结束通话失败'); return }
    ElMessage.success('通话已结束')
    webrtc.cleanup()
    startClosingCountdown(3, '通话已结束')
  } catch (_) {}
}

const handleLeaveMeeting = async () => {
  try {
    await ElMessageBox.confirm('确定要退出通话吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    const result = await proxy.Request({ url: proxy.Api.leaveMeeting, params: { meetingNo: meetingInfo.meetingNo } })
    if (!result || result.code !== 200) { ElMessage.error(result?.info || '退出通话失败'); return }
    ElMessage.success('已退出通话')
    participants.value.forEach(p => {
      const pid = p.userId || p.createUserId
      if (pid && pid !== currentUserId) sendSignal({ messageType: 23, targetUserId: pid, meetingNo: meetingInfo.meetingNo })
    })
    webrtc.cleanup()
    startClosingCountdown(3, '已退出通话')
  } catch (_) {}
}

const handleKickMessage = (message) => {
  if (showClosingOverlay.value) return
  if (message.extendData === currentUserId) { webrtc.cleanup(); startClosingCountdown(5, '您已被踢出通话') }
  else { loadParticipants() }
}

const startClosingCountdown = (seconds, title) => {
  if (closingTimer) clearInterval(closingTimer)
  showClosingOverlay.value = true; closingTitle.value = title; closingCountdown.value = seconds
  closingTimer = setInterval(() => {
    closingCountdown.value--
    if (closingCountdown.value <= 0) { clearInterval(closingTimer); closingTimer = null; window.api?.closeMeetingWindow?.(0) }
  }, 1000)
}

const showInvite = () => invitePanelRef.value?.show()

const showMeetingInfo = () => {
  const info = [
    { label: '通话名称', value: meetingInfo.meetingName || '-' },
    { label: '通话号', value: meetingInfo.meetingNo || '-' },
    { label: '创建时间', value: meetingInfo.createTime || '-' },
    { label: '开始时间', value: meetingInfo.startTime || '-' },
    { label: '加入类型', value: meetingInfo.jionType === 0 ? '公开' : '加密' },
  ]
  let html = '<table style="width:100%;border-collapse:collapse;">'
  info.forEach(i => { html += `<tr style="border-bottom:1px solid #eee;"><td style="padding:8px 0;color:#909399;width:90px;">${i.label}</td><td style="padding:8px 0;color:#303133;">${i.value}</td></tr>` })
  html += '</table>'
  ElMessageBox({ title: '通话信息', message: html, confirmButtonText: '关闭', customStyle: 'width:380px', dangerouslyUseHTMLString: true })
}

// ==================== 生命周期 ====================
onMounted(async () => {
  if (props.meetingData && Object.keys(props.meetingData).length > 0) Object.assign(meetingInfo, props.meetingData)
  if (window.electron) {
    window.electron.ipcRenderer.on('meeting-data', (_e, d) => d && Object.assign(meetingInfo, d))
    try { const d = await window.electron.ipcRenderer.invoke('get-meeting-data'); if (d) Object.assign(meetingInfo, d) } catch (_) {}
  }
  webrtc.setCurrentUserId(currentUserId)
  webrtc.setSendSignalCallback(sendSignal)
  await webrtc.initWebRTC()
  cleanupListener = setupMessageListener()
  await loadParticipants()
})

onUnmounted(() => {
  webrtc.cleanup()
  if (cleanupListener && typeof cleanupListener === 'function') cleanupListener()
})
</script>

<style lang="css" scoped>
.meeting-room {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  color: #202124;
  overflow: hidden;
  position: relative;
}

/* ========== 顶部标题栏 ========== */
.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #ffffff;
  border-bottom: 1px solid #e8eaed;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  z-index: 20;
  flex-shrink: 0;
}

.header-left { display: flex; align-items: baseline; gap: 14px; }
.room-title { margin: 0; font-size: 17px; font-weight: 600; color: #202124; letter-spacing: -0.3px; }
.room-info { font-size: 13px; color: #5f6368; }

/* 右侧工具栏 */
.header-toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
}

.toolbar-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 14px;
  border: none;
  background: transparent;
  color: #5f6368;
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.toolbar-btn:hover {
  background: rgba(26, 115, 232, 0.08);
  color: #1a73e8;
}

.toolbar-btn.active {
  background: rgba(26, 115, 232, 0.10);
  color: #1a73e8;
  font-weight: 600;
}

.toolbar-btn .el-icon { font-size: 16px; }

/* ========== 主内容区 ========== */
.room-body {
  flex: 1;
  position: relative;
  overflow: hidden;
}

/* ========== 视频区域 ========== */
.video-area {
  width: 100%;
  height: 100%;
  padding: 16px;
  padding-bottom: 110px;
  transition: margin-right 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  overflow-y: auto;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 12px;
  height: fit-content;
}

.video-cell {
  position: relative;
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  aspect-ratio: 16 / 9;
  min-height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e8eaed;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.05);
  transition: all 0.25s ease;
}

.video-cell:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.video-cell.local-cell {
  border-color: #1a73e8;
  box-shadow: 0 2px 12px rgba(26, 115, 232, 0.12);
}

.video-fill {
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #e8eaed;
}

.video-cell-footer {
  position: absolute;
  bottom: 10px;
  left: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(32, 33, 36, 0.80);
  backdrop-filter: blur(8px);
  padding: 5px 10px;
  border-radius: 8px;
}

.user-name {
  font-size: 12px;
  color: #ffffff;
  font-weight: 500;
}

.status-badge.muted {
  font-size: 10px;
  color: #ea4335;
  font-weight: 600;
}

/* ========== 侧滑面板 ========== */
.side-panel {
  position: absolute !important;
  top: 0;
  right: 0;
  height: 100%;
  background: #ffffff;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.10);
  z-index: 30;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.participants-side-panel {
  width: 300px;
}

.chat-side-panel {
  width: 360px;
}

.side-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
  position: relative;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #202124;
}

/* 管理按钮 */
.manage-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: none;
  background: rgba(26, 115, 232, 0.08);
  color: #1a73e8;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.manage-btn:hover {
  background: rgba(26, 115, 232, 0.15);
}

.manage-btn .el-icon { font-size: 14px; }

/* 管理下拉菜单 */
.manage-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  margin-top: 6px;
  width: 240px;
  max-height: 280px;
  overflow-y: auto;
  background: #ffffff;
  border-radius: 10px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.12), 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #e8eaed;
  z-index: 30;
}

.manage-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  transition: background 0.15s;
}

.manage-item:hover { background: #f8f9fa; }

.manage-name {
  font-size: 13px;
  color: #202124;
  font-weight: 500;
}

.manage-actions {
  display: flex;
  gap: 6px;
}

.manage-empty {
  padding: 20px 14px;
  text-align: center;
  font-size: 13px;
  color: #9aa0a6;
}

.side-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

/* 参会者项样式 */
.participant-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 8px;
  transition: background-color 0.15s;
}

.participant-item:hover { background: #f8f9fa; }
.participant-item.offline { opacity: 0.55; }

.participant-info { flex: 1; min-width: 0; }

.participant-name {
  font-size: 13px;
  color: #202124;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.participant-role {
  font-size: 11px;
  color: #5f6368;
  margin-top: 2px;
}

.role-badge.host {
  display: inline-block;
  padding: 1px 6px;
  font-size: 10px;
  border-radius: 4px;
  margin-left: 4px;
  vertical-align: middle;
  background: #fef0f0;
  color: #f56c6c;
}

.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-left: 4px;
  background: #67c23a;
}

.status-text { margin-left: 4px; font-size: 11px; }
.status-text.blacked { color: #f56c6c; }
.status-text.offline { color: #9aa0a6; }

.participant-actions {
  display: flex;
  gap: 2px;
  flex-shrink: 0;
}

/* ========== 底部控制栏 ========== */
.control-bar {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 12px;
  padding: 16px 32px 20px;
  z-index: 25;
}

.ctrl-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 50px;
  height: 50px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.ctrl-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.20);
}
.ctrl-item:active { transform: scale(0.94); }

/* 麦克风/摄像头 - 深灰 */
.ctrl-item.off { background: #5f6368; }
.ctrl-item.off:hover { background: #4a4c4e; }

/* 结束通话 - 红色 */
.ctrl-item.end-item {
  background: #ea4335;
  box-shadow: 0 2px 10px rgba(234, 67, 53, 0.35);
}
.ctrl-item.end-item:hover {
  background: #d93025;
  box-shadow: 0 4px 16px rgba(234, 67, 53, 0.45);
}

/* 邀请 - 蓝色 */
.ctrl-item:last-child {
  background: #1a73e8;
  box-shadow: 0 2px 8px rgba(26, 115, 232, 0.30);
}
.ctrl-item:last-child:hover {
  background: #1557b0;
  box-shadow: 0 4px 12px rgba(26, 115, 232, 0.40);
}

.ctrl-icon {
  font-size: 22px;
  color: #ffffff;
}

.ctrl-item:not(.off):not(.end-item):not(:last-child) .ctrl-icon { color: #3c4043; }

/* ========== 侧滑动画 ========== */
.slide-right-panel-enter-active {
  transition: transform 0.35s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.3s ease;
}
.slide-right-panel-leave-active {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.25s ease;
}
.slide-right-panel-enter-from {
  transform: translateX(100%);
  opacity: 0;
}
.slide-right-panel-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* ========== 关闭倒计时遮罩 ========== */
.closing-overlay {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  animation: fadeIn 0.3s ease;
}
.closing-content {
  text-align: center;
  color: #fff;
  padding: 48px 72px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  backdrop-filter: blur(12px);
}
.closing-icon { margin-bottom: 20px; color: #ea4335; }
.closing-title { margin: 0 0 20px 0; font-size: 22px; font-weight: 600; }
.closing-countdown { margin: 0; font-size: 16px; color: rgba(255, 255, 255, 0.85); }
.countdown-number {
  display: inline-block;
  min-width: 40px;
  padding: 8px 16px;
  background: rgba(234, 67, 53, 0.25);
  border-radius: 12px;
  font-size: 28px;
  font-weight: 600;
  color: #ff8a80;
  animation: pulse 1s ease-in-out infinite;
}

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.08); } }
</style>
