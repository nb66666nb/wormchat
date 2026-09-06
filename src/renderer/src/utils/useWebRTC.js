/**
 * useWebRTC - WebRTC 组合式函数
 *
 * 封装所有 WebRTC 相关逻辑：
 *   - 本地媒体流获取
 *   - Mesh P2P 连接管理
 *   - 信令收发处理（Offer/Answer/ICE/Hangup）
 *   - 远程视频元素注册
 *   - 媒体控制（麦克风/摄像头切换）
 *
 * 用法：
 *   const { initWebRTC, connectToAllParticipants, ... } = useWebRTC(meetingNo)
 */

import { ref, nextTick, watchEffect } from 'vue'
import { ElMessage } from 'element-plus'

import {
  getLocalStream,
  createPeerConnection,
  getPeerConnection,
  createOffer,
  handleOffer,
  handleAnswer,
  addIceCandidate,
  closeAllConnections,
  closePeerConnection,
  stopLocalStream,
  toggleMicrophone,
  toggleCamera as webrtcToggleCamera,
  setMeetingNo,
  registerVideoElement,
  unregisterVideoElement,
  getLocalStreamObject
} from '@/utils/webrtc'

export default function useWebRTC(meetingNoRef) {
  // meetingNoRef 可以是字符串常量、ref 或 computed ref
  // 通过 watchEffect 自动追踪变化
  // 本地视频元素引用（由组件通过 ref 传入）
  const localVideoRef = ref(null)

  // 媒体状态
  const isMuted = ref(false)
  const isCameraOff = ref(false)

  // 远程视频元素映射表：userId → HTMLVideoElement
  const remoteVideoRefs = {}

  // 待绑定远程流缓存（onTrack 触发时视频元素尚未注册）
  const pendingStreams = new Map()

  // 延迟重试绑定定时器
  const bindTimers = new Map()

  // 正在建立连接的用户集合（防止重复/并发创建）
  const connectingUsers = new Set()

  // 当前用户 ID（由外部设置）
  let currentUserId = ''

  // 信令发送回调（由外部注入）
  let sendSignalCallback = null

  /**
   * 设置当前用户 ID
   */
  const setCurrentUserId = (uid) => {
    currentUserId = uid
  }

  /**
   * 设置信令发送回调
   */
  const setSendSignalCallback = (cb) => {
    sendSignalCallback = cb
  }

  /**
   * 发送信令消息
   */
  const sendSignal = (data) => {
    if (sendSignalCallback) {
      sendSignalCallback(data)
    }
  }

  // ==================== 初始化 ====================

  /**
   * 初始化 WebRTC：设置会议号、获取本地媒体、绑定本地视频
   */
  const initWebRTC = async () => {
    try {
      // 解析会议号（支持 ref 和普通值）
      const meetingNo = meetingNoRef && meetingNoRef.value !== undefined
        ? meetingNoRef.value : meetingNoRef
      setMeetingNo(meetingNo || '')

      await getLocalStream({ audio: true, video: true })

      await nextTick()
      if (localVideoRef.value) {
        localVideoRef.value.srcObject = getLocalStreamObject()
      }

      // 按软件设置决定初始麦克风/摄像头状态（defaultMicrophone 默认开，defaultCamera 默认关）
      const sw = (() => { try { return JSON.parse(localStorage.getItem('softwareSettings') || '{}') } catch (_) { return {} } })()
      const micOn = sw.defaultMicrophone ?? true
      const camOn = sw.defaultCamera ?? false
      if (!micOn) toggleMute()    // 设置为关闭麦克风 → 静音
      if (!camOn) toggleCamera()  // 设置为关闭摄像头
    } catch (error) {
      console.error('❌ [useWebRTC] 初始化失败:', error.message)
      ElMessage.warning('无法获取摄像头/麦克风权限，请在设置中允许后重试')
    }
  }

  // ==================== Mesh P2P 连接 ====================

  /**
   * 获取当前会议号（解析 ref）
   */
  const getMeetingNo = () => {
    return meetingNoRef && meetingNoRef.value !== undefined
      ? meetingNoRef.value : meetingNoRef
  }

  /**
   * 与指定参会者建立 P2P 连接
   */
  const connectToParticipant = async (targetUser) => {
    const targetUserId = targetUser.userId || targetUser.createUserId
    if (!targetUserId || targetUserId === currentUserId) return
    if (getPeerConnection(targetUserId)) return
    if (connectingUsers.has(targetUserId)) return

    connectingUsers.add(targetUserId)
    const mn = getMeetingNo()

    try {
      createPeerConnection(
        targetUserId,
        (candidate) => {
          sendSignal({
            messageType: 22,
            targetUserId,
            candidate: JSON.stringify({
              candidate: candidate.candidate,
              sdpMid: candidate.sdpMid,
              sdpMLineIndex: candidate.sdpMLineIndex
            }),
            meetingNo: mn
          })
        },
        (stream, userId) => {
          const videoEl = remoteVideoRefs[userId]
          if (videoEl && stream) {
            videoEl.srcObject = stream
            pendingStreams.delete(userId)
            const t = bindTimers.get(userId)
            if (t) { clearTimeout(t); bindTimers.delete(userId) }
          } else {
            pendingStreams.set(userId, stream)
            if (!bindTimers.has(userId)) {
              let attempts = 0
              const tryBind = () => {
                attempts++
                const el = remoteVideoRefs[userId]
                if (el && pendingStreams.has(userId)) {
                  el.srcObject = pendingStreams.get(userId)
                  pendingStreams.delete(userId)
                  bindTimers.delete(userId)
                  return
                }
                if (attempts >= 20) {
                  bindTimers.delete(userId)
                  return
                }
                bindTimers.set(userId, setTimeout(tryBind, 500))
              }
              bindTimers.set(userId, setTimeout(tryBind, 500))
            }
          }
        },
        null
      )

      const pc = getPeerConnection(targetUserId)
      if (pc) {
        await createOffer(pc, targetUserId, sendSignal)
      }
    } catch (e) {
      console.error(`❌ [useWebRTC] 与 ${targetUserId} 建立连接失败:`, e.message)
    } finally {
      connectingUsers.delete(targetUserId)
    }
  }

  /**
   * 连接所有现有参会者
   */
  const connectToAllParticipants = (participantList) => {
    if (!participantList || participantList.length === 0) return
    const others = participantList.filter(p => {
      const uid = p.userId || p.createUserId
      return uid && uid !== currentUserId && !getPeerConnection(uid) && !connectingUsers.has(uid)
    })
    if (others.length === 0) return

    let idx = 0
    const connectNext = async () => {
      if (idx >= others.length) return
      const p = others[idx++]
      await connectToParticipant(p)
      await new Promise(r => setTimeout(r, 200))
      connectNext()
    }
    connectNext()
  }

  // ==================== 信令处理 ====================

  /**
   * 处理收到的 WebRTC 信令消息
   */
  const handleWebRtcSignal = (message) => {
    const type = message.messageType
    const fromUserId = message.sendUserId
    if (!fromUserId || fromUserId === currentUserId) return

    switch (type) {
      case 20:
        handleIncomingOffer(message, fromUserId)
        break
      case 21:
        handleIncomingAnswer(message, fromUserId)
        break
      case 22:
        handleIncomingIceCandidate(message, fromUserId)
        break
      case 23:
        handleIncomingHangup(fromUserId)
        break
    }
  }

  const handleIncomingOffer = async (message, fromUserId) => {
    const mn = getMeetingNo()
    let pc = getPeerConnection(fromUserId)
    if (!pc) {
      pc = createPeerConnection(
        fromUserId,
        (candidate) => {
          sendSignal({
            messageType: 22,
            targetUserId: fromUserId,
            candidate: JSON.stringify({
              candidate: candidate.candidate,
              sdpMid: candidate.sdpMid,
              sdpMLineIndex: candidate.sdpMLineIndex
            }),
            meetingNo: mn
          })
        },
        null,
        null
      )
    }
    const sdp = message.messageContent?.sdp || message.sdp
    await handleOffer(pc, sdp, fromUserId, sendSignal)
  }

  const handleIncomingAnswer = async (message, fromUserId) => {
    const pc = getPeerConnection(fromUserId)
    if (!pc) return
    const sdp = message.messageContent?.sdp || message.sdp
    await handleAnswer(pc, sdp)
  }

  const handleIncomingIceCandidate = async (message, fromUserId) => {
    const pc = getPeerConnection(fromUserId)
    if (!pc) return
    const candidate = message.messageContent?.candidate || message.candidate
    await addIceCandidate(pc, candidate)
  }

  const handleIncomingHangup = (fromUserId) => {
    closePeerConnection(fromUserId)
    unregisterRemoteVideo(fromUserId)
  }

  // ==================== 远程视频元素管理 ====================

  /**
   * 注册远程视频元素（由模板中的 function ref 调用）
   */
  const registerRemoteVideo = (userId, el) => {
    if (!userId) return
    if (!el) {
      unregisterVideoElement(userId)
      delete remoteVideoRefs[userId]
      return
    }
    remoteVideoRefs[userId] = el
    registerVideoElement(userId, el)
    const pendingStream = pendingStreams.get(userId)
    if (pendingStream && el) {
      el.srcObject = pendingStream
      pendingStreams.delete(userId)
      const t = bindTimers.get(userId)
      if (t) { clearTimeout(t); bindTimers.delete(userId) }
    }
  }

  const unregisterRemoteVideo = (userId) => {
    unregisterVideoElement(userId)
    delete remoteVideoRefs[userId]
  }

  // ==================== 媒体控制 ====================

  const toggleMute = () => {
    const muted = toggleMicrophone()
    isMuted.value = muted
    return muted
  }

  const toggleCamera = () => {
    const off = webrtcToggleCamera()
    isCameraOff.value = off
    return off
  }

  // ==================== 清理 ====================

  const cleanup = () => {
    closeAllConnections()
    stopLocalStream()
    pendingStreams.clear()
    bindTimers.forEach((t) => clearTimeout(t))
    bindTimers.clear()
    connectingUsers.clear()
  }

  return {
    // 状态
    localVideoRef,
    isMuted,
    isCameraOff,

    // 配置
    setCurrentUserId,
    setSendSignalCallback,

    // 初始化
    initWebRTC,

    // 连接管理
    connectToParticipant,
    connectToAllParticipants,

    // 信令处理
    handleWebRtcSignal,

    // 视频元素
    registerRemoteVideo,

    // 媒体控制
    toggleMute,
    toggleCamera,

    // 清理
    cleanup
  }
}
