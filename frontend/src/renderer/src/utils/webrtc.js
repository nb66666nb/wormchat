/**
 * WebRTC 核心工具模块
 *
 * 功能：
 *   - getLocalStream：获取本地音视频流
 *   - createPeerConnection：创建 RTCPeerConnection
 *   - createOffer / createAnswer / setRemoteDescription
 *   - addIceCandidate
 *   - closeAllConnections：关闭所有 P2P 连接
 *   - stopLocalStream：释放本地媒体
 *
 * Mesh 拓扑架构：每个参会者与所有其他人建立 P2P 连接
 * 信令通过后端中转（复用现有 Netty WebSocket）
 */

// STUN 服务器配置（用于 NAT 穿透）
const ICE_SERVERS = {
  iceServers: [
    { urls: 'stun:stun.l.google.com:19302' },
    { urls: 'stun:stun1.l.google.com:19302' }
  ]
}

// 本地媒体流
let localStream = null

// 所有 P2P 连接映射表：targetUserId → RTCPeerConnection
const peerConnections = new Map()

// 视频元素映射表：targetUserId → HTMLVideoElement
const videoElements = new Map()

// 待绑定远程流缓存：targetUserId → MediaStream（ontrack 触发时视频元素尚未注册）
const pendingRemoteStreams = new Map()

/**
 * 获取本地音视频流
 * @param {Object} constraints - getUserMedia 约束
 * @param {boolean} constraints.audio - 是否启用音频（默认 true）
 * @param {boolean} constraints.video - 是否启用视频（默认 true）
 * @returns {Promise<MediaStream>} 本地媒体流
 */
export const getLocalStream = async (constraints = { audio: true, video: true }) => {
  if (localStream) return localStream
  try {
    localStream = await navigator.mediaDevices.getUserMedia({
      audio: constraints.audio !== false,
      video: constraints.video !== false
    })
    return localStream
  } catch (error) {
    if (error.name === 'NotReadableError' || error.name === 'DevicesNotFoundError') {
      try {
        localStream = await navigator.mediaDevices.getUserMedia({
          audio: true,
          video: false
        })
        return localStream
      } catch (_) {}
    }
    console.error('❌ [WebRTC] 获取本地媒体流失败:', error.name, error.message)
    throw error
  }
}

/**
 * 创建与指定用户的 RTCPeerConnection
 * @param {string} targetUserId - 目标用户 ID
 * @param {Function} onIceCandidate - ICE Candidate 回调 (candidate) => void
 * @param {Function} onTrack - 远程媒体流回调 (stream, userId) => void
 * @param {Function} onConnectionState - 连接状态回调 (state, userId) => void
 * @returns {RTCPeerConnection}
 */
export const createPeerConnection = (targetUserId, onIceCandidate, onTrack, onConnectionState) => {
  if (peerConnections.has(targetUserId)) return peerConnections.get(targetUserId)

  const pc = new RTCPeerConnection(ICE_SERVERS)

  if (localStream) {
    const audioTrack = localStream.getAudioTracks()[0]
    const videoTrack = localStream.getVideoTracks()[0]
    if (audioTrack) pc.addTrack(audioTrack, localStream)
    if (videoTrack) pc.addTrack(videoTrack, localStream)
  }

  pc.onicecandidate = (event) => {
    if (event.candidate && onIceCandidate) onIceCandidate(event.candidate, targetUserId)
  }

  pc.onconnectionstatechange = () => {
    if (onConnectionState) onConnectionState(pc.connectionState, targetUserId)
    if (pc.connectionState === 'closed' || pc.connectionState === 'failed') {
      peerConnections.delete(targetUserId)
    }
  }

  pc.ontrack = (event) => {
    const remoteStream = event.streams[0]
    if (onTrack) onTrack(remoteStream, targetUserId)
    const videoEl = videoElements.get(targetUserId)
    if (videoEl && remoteStream) {
      videoEl.srcObject = remoteStream
    } else {
      pendingRemoteStreams.set(targetUserId, remoteStream)
      scheduleBindRetry(targetUserId, remoteStream)
    }
  }

  peerConnections.set(targetUserId, pc)
  return pc
}

// 延迟重试绑定定时器：targetUserId → timerId
const bindRetryTimers = new Map()

/**
 * 延迟重试将远程流绑定到视频元素（最多重试 20 次，间隔 500ms）
 */
function scheduleBindRetry(userId, stream) {
  let attempts = 0
  const maxAttempts = 20
  const tryBind = () => {
    attempts++
    const videoEl = videoElements.get(userId)
    if (videoEl && stream) {
      videoEl.srcObject = stream
      pendingRemoteStreams.delete(userId)
      bindRetryTimers.delete(userId)
      return
    }
    if (attempts >= maxAttempts) {
      bindRetryTimers.delete(userId)
      return
    }
    bindRetryTimers.set(userId, setTimeout(tryBind, 500))
  }
  bindRetryTimers.set(userId, setTimeout(tryBind, 500))
}

/**
 * 获取与指定用户的 PeerConnection
 */
export const getPeerConnection = (targetUserId) => {
  return peerConnections.get(targetUserId)
}

/**
 * 注册远程视频元素
 * @param {string} userId - 用户 ID
 * @param {HTMLVideoElement} videoElement - 视频元素
 */
export const registerVideoElement = (userId, videoElement) => {
  videoElements.set(userId, videoElement)
  const pendingStream = pendingRemoteStreams.get(userId)
  if (pendingStream && videoElement) {
    videoElement.srcObject = pendingStream
    pendingRemoteStreams.delete(userId)
    const timer = bindRetryTimers.get(userId)
    if (timer) { clearTimeout(timer); bindRetryTimers.delete(userId) }
  }
}

/**
 * 移除远程视频元素
 */
export const unregisterVideoElement = (userId) => {
  videoElements.delete(userId)
}

/**
 * 创建 SDP Offer 并发送给目标用户
 * @param {RTCPeerConnection} pc
 * @param {string} targetUserId
 * @param {Function} sendSignal - 发送信令的函数
 */
export const createOffer = async (pc, targetUserId, sendSignal) => {
  try {
    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)
    sendSignal({
      messageType: 20,
      targetUserId: targetUserId,
      sdp: JSON.stringify({ type: offer.type, sdp: offer.sdp }),
      meetingNo: getMeetingNo()
    })
  } catch (error) {
    console.error(`❌ [WebRTC] 创建 Offer 失败 (${targetUserId}):`, error.message)
  }
}

export const handleOffer = async (pc, sdp, fromUserId, sendSignal) => {
  try {
    const remoteDesc = typeof sdp === 'string' ? JSON.parse(sdp) : sdp
    await pc.setRemoteDescription(new RTCSessionDescription(remoteDesc))
    const answer = await pc.createAnswer()
    await pc.setLocalDescription(answer)
    sendSignal({
      messageType: 21,
      targetUserId: fromUserId,
      sdp: JSON.stringify({ type: answer.type, sdp: answer.sdp }),
      meetingNo: getMeetingNo()
    })
  } catch (error) {
    console.error(`❌ [WebRTC] 处理 Offer 失败 (${fromUserId}):`, error.message)
  }
}

export const handleAnswer = async (pc, sdp) => {
  try {
    const remoteDesc = typeof sdp === 'string' ? JSON.parse(sdp) : sdp
    await pc.setRemoteDescription(new RTCSessionDescription(remoteDesc))
  } catch (error) {
    console.error('❌ [WebRTC] 处理 Answer 失败:', error.message)
  }
}

export const addIceCandidate = async (pc, candidate) => {
  try {
    const candidateObj = typeof candidate === 'string' ? JSON.parse(candidate) : candidate
    await pc.addIceCandidate(new RTCIceCandidate(candidateObj))
  } catch (error) {
    // 静默失败：candidate 不匹配或已过时属正常情况
  }
}

/**
 * 关闭与指定用户的连接
 */
export const closePeerConnection = (targetUserId) => {
  const pc = peerConnections.get(targetUserId)
  if (pc) {
    pc.close()
    peerConnections.delete(targetUserId)
  }
  const videoEl = videoElements.get(targetUserId)
  if (videoEl) {
    videoEl.srcObject = null
    videoElements.delete(targetUserId)
  }
  pendingRemoteStreams.delete(targetUserId)
  const timer = bindRetryTimers.get(targetUserId)
  if (timer) { clearTimeout(timer); bindRetryTimers.delete(targetUserId) }
}

export const closeAllConnections = () => {
  peerConnections.forEach((pc) => { pc.close() })
  peerConnections.clear()
  videoElements.forEach((videoEl) => { videoEl.srcObject = null })
  videoElements.clear()
  pendingRemoteStreams.clear()
  bindRetryTimers.forEach((timer) => clearTimeout(timer))
  bindRetryTimers.clear()
}

export const stopLocalStream = () => {
  if (localStream) {
    localStream.getTracks().forEach(track => track.stop())
    localStream = null
  }
}

export const toggleMicrophone = () => {
  if (localStream) {
    const audioTrack = localStream.getAudioTracks()[0]
    if (audioTrack) {
      audioTrack.enabled = !audioTrack.enabled
      return !audioTrack.enabled
    }
  }
  return false
}

export const toggleCamera = () => {
  if (localStream) {
    const videoTrack = localStream.getVideoTracks()[0]
    if (videoTrack) {
      videoTrack.enabled = !videoTrack.enabled
      return !videoTrack.enabled
    }
  }
  return false
}

/**
 * 获取当前会议号（模块级变量，由 MeetingRoom 设置）
 */
let currentMeetingNo = ''
export const setMeetingNo = (meetingNo) => {
  currentMeetingNo = meetingNo
}
const getMeetingNo = () => currentMeetingNo

/**
 * 获取所有已连接的远端用户 ID
 */
export const getConnectedPeers = () => {
  return Array.from(peerConnections.keys())
}

/**
 * 获取本地媒体流
 */
export const getLocalStreamObject = () => localStream
