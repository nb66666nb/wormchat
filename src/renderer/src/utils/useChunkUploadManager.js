/**
 * useChunkUploadManager - 多文件并发上传管理器
 *
 * 类似微信体验：
 *   - 多个文件同时上传，每个文件独立进度
 *   - 上传完成后自动按添加顺序调用 sendMessage
 *   - 每个文件可独立取消
 *   - 小文件（<10MB）走原接口，大文件走分片
 */

import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

/** 分片上传阈值：10MB 以下走原接口 */
const CHUNK_THRESHOLD = 10 * 1024 * 1024

/** 任务状态枚举 */
const TASK_STATUS = {
  HASHING: 'hashing',       // 计算指纹
  UPLOADING: 'uploading',   // 上传中
  MERGING: 'merging',       // 合并中
  READY: 'ready',           // 上传完成，等待发送
  SENDING: 'sending',       // 正在发送消息
  SENT: 'sent',             // 已发送
  FAILED: 'failed',         // 失败
  CANCELLED: 'cancelled'    // 已取消
}

export function useChunkUploadManager() {
  /** 所有上传任务列表（响应式） */
  const tasks = reactive([])

  /** 是否有任务正在上传 */
  const hasUploading = ref(false)

  /** 是否有任务等待发送 */
  const hasReadyToSend = ref(false)

  let progressUnsubscribe = null
  let taskIdCounter = 0

  /**
   * 根据进度事件数据匹配任务
   * 优先用 uploadId 匹配，fallback 用 fileName 匹配
   */
  const findTaskByProgress = (data) => {
    // 1. uploadId 精确匹配
    if (data.uploadId) {
      const byId = tasks.find(t => t.uploadId === data.uploadId)
      if (byId) return byId
    }
    // 2. fileName + 无 uploadId 的任务（hashing 阶段还没有 uploadId）
    if (data.fileName) {
      return tasks.find(t =>
        t.fileName === data.fileName &&
        !t.uploadId &&
        t.status !== TASK_STATUS.CANCELLED &&
        t.status !== TASK_STATUS.READY &&
        t.status !== TASK_STATUS.SENT
      )
    }
    return null
  }

  onMounted(() => {
    if (window.messageAPI?.onChunkUploadProgress) {
      progressUnsubscribe = window.messageAPI.onChunkUploadProgress((data) => {
        const task = findTaskByProgress(data)
        if (!task || task.status === TASK_STATUS.CANCELLED) return

        // 收到 uploadId，绑定到 task
        if (data.uploadId && !task.uploadId) {
          task.uploadId = data.uploadId
        }

        if (data.phase) task.phase = data.phase
        if (data.progress !== undefined) task.progress = data.progress
        if (data.completedCount !== undefined) task.completedChunks = data.completedCount
        if (data.totalChunks !== undefined) task.totalChunks = data.totalChunks
        if (data.fastPass) task.fastPass = true
      })
    }
  })

  onUnmounted(() => {
    if (progressUnsubscribe) progressUnsubscribe()
  })

  /** 更新全局状态 */
  const refreshGlobalState = () => {
    hasUploading.value = tasks.some(t =>
      t.status === TASK_STATUS.HASHING ||
      t.status === TASK_STATUS.UPLOADING ||
      t.status === TASK_STATUS.MERGING
    )
    hasReadyToSend.value = tasks.some(t => t.status === TASK_STATUS.READY)
  }

  /**
   * 添加文件上传任务
   * @param {File} file - 文件对象
   * @param {string} meetingNo - 会议号
   * @param {number} messageType - 消息类型 (32/33/34/35)
   * @returns {Object} 任务对象
   */
  const addTask = (file, meetingNo, messageType) => {
    const task = reactive({
      id: ++taskIdCounter,
      file,
      fileName: file.name,
      fileSize: file.size,
      meetingNo,
      messageType,
      status: TASK_STATUS.UPLOADING,
      phase: '',
      progress: 0,
      completedChunks: 0,
      totalChunks: 0,
      fastPass: false,
      uploadId: '',
      fileData: null,       // 上传成功后的文件信息
      error: null,
      previewUrl: null,
      tempFilePath: null,
      _abortController: null  // 用于取消小文件上传
    })

    // 图片预览
    if (messageType === 33) {
      task.previewUrl = URL.createObjectURL(file)
    }

    tasks.push(task)
    refreshGlobalState()

    // 异步开始上传
    _startUpload(task)

    return task
  }

  /** 开始上传（自动选择小文件/分片） */
  const _startUpload = async (task) => {
    try {
      let fileData

      if (task.fileSize < CHUNK_THRESHOLD) {
        // 小文件：走原有接口
        task.phase = 'uploading'
        task.progress = 30
        fileData = await _smallFileUpload(task.file, task.meetingNo)
      } else {
        // 大文件：走分片上传
        // Electron 32+ 已移除 File.path，必须用 webUtils.getPathForFile 获取本地磁盘路径
        // 拿到路径后主进程可直接流式切片上传，渲染进程无需把文件内容读进内存
        let filePath = ''
        if (window.api?.getPathForFile) {
          try { filePath = window.api.getPathForFile(task.file) } catch (_) {}
        }

        if (!filePath) {
          // fallback：拿不到本地路径（如剪贴板粘贴的文件），通过 IPC 落盘临时文件后再分片
          filePath = await _saveToTempFile(task.file)
          if (task.status === TASK_STATUS.CANCELLED) return
          task.tempFilePath = filePath
        }

        // 调用主进程分片上传
        const result = await window.api.chunkUploadFile({
          filePath,
          fileName: task.fileName,
          meetingNo: task.meetingNo,
          concurrency: 3
        })

        if (task.status === TASK_STATUS.CANCELLED) return  // 上传期间可能被取消

        if (!result || !result.success) {
          throw new Error(result?.error || '分片上传失败')
        }
        fileData = result.data
      }

      // 上传成功
      task.fileData = fileData
      task.status = TASK_STATUS.READY
      task.progress = 100
      task.phase = 'done'

      // 清理临时文件
      if (task.tempFilePath && window.api?.cleanupTempFile) {
        window.api.cleanupTempFile({ filePath: task.tempFilePath })
        task.tempFilePath = null
      }

    } catch (e) {
      if (task.status === TASK_STATUS.CANCELLED) return
      task.status = TASK_STATUS.FAILED
      task.error = e.message
      console.error(`[UploadManager] 文件上传失败: ${task.fileName}`, e.message)
    } finally {
      refreshGlobalState()
    }
  }

  /** 小文件整体上传 */
  const _smallFileUpload = async (file, meetingNo) => {
    // 优先用本地路径，让主进程直接读盘，避免把文件内容塞进 IPC（渲染进程零内存占用）
    let filePath = ''
    if (window.api?.getPathForFile) {
      try { filePath = window.api.getPathForFile(file) } catch (_) {}
    }

    const result = await window.api.uploadMeetingFile(
      filePath
        ? { filePath, fileName: file.name, meetingNo }
        : {
            // fallback：直接传 Uint8Array（Electron IPC 对 TypedArray 是内存级拷贝，高效）。
            // 绝不要用 Array.from(buffer)：会把每个字节膨胀成 Number 对象，内存暴涨数倍且序列化卡死 UI
            fileBuffer: new Uint8Array(await file.arrayBuffer()),
            fileName: file.name,
            meetingNo
          }
    )
    if (!result || !result.success) {
      throw new Error(result?.error || '上传失败')
    }
    return result.data
  }

  /** 保存临时文件（仅在拿不到 File 本地路径时使用，如剪贴板粘贴的文件） */
  const _saveToTempFile = async (file) => {
    // 用 arrayBuffer() 一次性读取后，直接以 Uint8Array 通过 IPC 传输。
    // 绝不要用 FileReader.readAsArrayBuffer + Array.from：会让大文件在渲染进程爆内存、IPC 序列化卡死
    const buffer = new Uint8Array(await file.arrayBuffer())
    const saveResult = await window.api.saveTempFile({
      fileBuffer: buffer,
      fileName: file.name
    })
    if (saveResult?.success && saveResult.filePath) {
      return saveResult.filePath
    }
    throw new Error('无法保存临时文件')
  }

  /** 取消上传 */
  const cancelTask = async (task) => {
    task.status = TASK_STATUS.CANCELLED

    // 如果有 uploadId，通知服务端取消
    if (task.uploadId && window.api?.cancelChunkUpload) {
      try {
        await window.api.cancelChunkUpload({ uploadId: task.uploadId })
      } catch (_) {}
    }

    // 清理临时文件
    if (task.tempFilePath && window.api?.cleanupTempFile) {
      window.api.cleanupTempFile({ filePath: task.tempFilePath })
      task.tempFilePath = null
    }

    // 释放预览 URL
    if (task.previewUrl) {
      URL.revokeObjectURL(task.previewUrl)
      task.previewUrl = null
    }

    refreshGlobalState()
  }

  /** 移除任务（从列表中删除） */
  const removeTask = (task) => {
    const idx = tasks.findIndex(t => t.id === task.id)
    if (idx !== -1) {
      // 如果还在上传，先取消
      if (task.status === TASK_STATUS.HASHING ||
          task.status === TASK_STATUS.UPLOADING ||
          task.status === TASK_STATUS.MERGING) {
        cancelTask(task)
      }
      // 释放预览 URL
      if (task.previewUrl) {
        URL.revokeObjectURL(task.previewUrl)
      }
      tasks.splice(idx, 1)
      refreshGlobalState()
    }
  }

  /** 清除已完成/已取消的任务 */
  const clearFinished = () => {
    for (let i = tasks.length - 1; i >= 0; i--) {
      const t = tasks[i]
      if (t.status === TASK_STATUS.SENT ||
          t.status === TASK_STATUS.CANCELLED ||
          t.status === TASK_STATUS.FAILED) {
        if (t.previewUrl) URL.revokeObjectURL(t.previewUrl)
        tasks.splice(i, 1)
      }
    }
    refreshGlobalState()
  }

  /** 获取所有已就绪（上传完成等待发送）的任务，按添加顺序 */
  const getReadyTasks = () => {
    return tasks.filter(t => t.status === TASK_STATUS.READY).sort((a, b) => a.id - b.id)
  }

  /** 标记任务为发送中 */
  const markSending = (task) => {
    task.status = TASK_STATUS.SENDING
    refreshGlobalState()
  }

  /** 标记任务为已发送 */
  const markSent = (task) => {
    task.status = TASK_STATUS.SENT
    refreshGlobalState()
  }

  /** 标记任务发送失败 */
  const markSendFailed = (task) => {
    task.status = TASK_STATUS.FAILED
    task.error = '消息发送失败'
    refreshGlobalState()
  }

  return {
    tasks,
    hasUploading,
    hasReadyToSend,
    TASK_STATUS,
    addTask,
    cancelTask,
    removeTask,
    clearFinished,
    getReadyTasks,
    markSending,
    markSent,
    markSendFailed
  }
}
