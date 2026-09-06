/**
 * file.js — 文件管理主进程模块
 *
 * 职责：
 *   - 文件上传到后端（Node.js HTTP / axios）
 *   - 本地缓存管理（meetchat-files = 远程下载缓存）
 *   - 本地文件存储（meetchat-front-LocalFile = 按日期分层）
 *   - 接收方文件处理：下载 + 缩略图（sharp/ffmpeg） + 本地存储
 *   - checkFile：检查本地是否有缓存
 *   - downloadFile：从后端下载文件到本地
 */

import fs from 'fs'
import path from 'path'
import http from 'http'
import crypto from 'crypto'
import { app, BrowserWindow } from 'electron'
import { getData, getUserData } from './store'

// ==================== 路径常量 ====================

/** 远程下载缓存目录（旧逻辑保留） */
const getLocalRoot = () => {
  return path.join(app.getPath('userData'), 'meetchat-files')
}

/** 本地文件主存储（发送方存原件，接收方下载后存） */
const LOCAL_FILE_ROOT = () => {
  const root = path.join(app.getPath('userData'), 'meetchat-front-LocalFile')
  if (!fs.existsSync(root)) fs.mkdirSync(root, { recursive: true })
  return root
}

/** 按日期分目录 */
const getDateDir = () => {
  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '/')
  const dir = path.join(LOCAL_FILE_ROOT(), dateStr)
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
  return dir
}

// ==================== 工具函数 ====================

const getTypeDir = (fileType) => {
  const map = { 'image': 'images', 'video': 'videos', 'audio': 'audios', 'file': 'files' }
  const sub = map[fileType] || 'files'
  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '/')
  const dir = path.join(getLocalRoot(), sub, dateStr)
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
  return dir
}

const getFileCategory = (fileType) => {
  const map = { 1: 'images', 2: 'videos', 3: 'audios' }
  return map[fileType] || 'files'
}

const getServerConfig = () => {
  try {
    const raw = getData('serverConfig')
    if (raw) return JSON.parse(raw)
  } catch (_) {}
  return { host: 'localhost', httpPort: '6060' }
}

// ==================== 头像缓存（以 userId 为键，不同于文件消息的 fileId）====================

const AVATAR_DIR = () => {
  const dir = path.join(app.getPath('userData'), 'meetchat-avatars')
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
  return dir
}

const AVATAR_INDEX = () => path.join(AVATAR_DIR(), 'index.json')

const loadAvatarIndex = () => {
  try {
    const idxPath = AVATAR_INDEX()
    if (fs.existsSync(idxPath)) return JSON.parse(fs.readFileSync(idxPath, 'utf-8'))
  } catch (_) {}
  return {}
}

const saveAvatarIndex = (index) => {
  try { fs.writeFileSync(AVATAR_INDEX(), JSON.stringify(index, null, 2)) } catch (_) {}
}

// 机器人/非用户头像缓存（按 fileId 分目录，命名 bot_<fileId>.<ext>）
//   与 userId 索引的 AVATAR_DIR 隔离，避免和用户自身头像互相覆盖
const BOT_AVATAR_DIR = () => {
  const dir = path.join(app.getPath('userData'), 'meetchat-bot-avatars')
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
  return dir
}

const getBotAvatarCachePath = (fileId, ext) => {
  return path.join(BOT_AVATAR_DIR(), `bot_${fileId}${ext || '.png'}`)
}

export const uploadAvatar = ({ userId, fileBuffer, fileName, token, scope }) => {
  return new Promise((resolve, reject) => {
    const config = getServerConfig()
    const boundary = '----MeetChatAvatar' + Date.now()

    let actualBuffer
    if (fileBuffer && Array.isArray(fileBuffer) && fileBuffer.length > 0) {
      actualBuffer = Buffer.from(fileBuffer)
    } else if (fileName && fs.existsSync(fileName)) {
      actualBuffer = fs.readFileSync(fileName)
    } else {
      reject(new Error('缺少头像文件数据'))
      return
    }

    const ext = path.extname(fileName || '') || '.png'
    const isBot = scope === 'bot'
    const saveName = isBot ? `bot_upload_${Date.now()}${ext}` : `avatar_${userId}${ext}`
    const savePath = isBot
      ? path.join(BOT_AVATAR_DIR(), saveName)
      : path.join(AVATAR_DIR(), saveName)

    const doUpload = (uploadPath, formFields) => {
      const parts = formFields.map(f =>
        Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="${f.name}"\r\n\r\n${f.value}\r\n`)
      )
      parts.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\nContent-Type: application/octet-stream\r\n\r\n`))
      parts.push(actualBuffer)
      parts.push(Buffer.from(`\r\n--${boundary}--\r\n`))
      const body = Buffer.concat(parts)

      const options = {
        hostname: config.host, port: config.httpPort || 6060,
        path: uploadPath, method: 'POST',
        headers: {
          'Content-Type': `multipart/form-data; boundary=${boundary}`,
          'Content-Length': body.length,
          'X-Requested-With': 'XMLHttpRequest',
          'token': token || ''
        }
      }

      const req = http.request(options, (res) => {
        let data = ''
        res.on('data', chunk => data += chunk)
        res.on('end', () => {
          try {
            const json = JSON.parse(data)
            if (json.code === 200) {
              const fileData = json.data || {}
              fs.writeFileSync(savePath, actualBuffer)
              if (isBot) {
                // 机器人头像缓存：写一个 bot_<fileId>.<ext> 的别名，下载时按 fileId 直查
                const aliased = getBotAvatarCachePath(fileData.fileId, ext)
                try { fs.copyFileSync(savePath, aliased) } catch (_) {}
                // 异步缩小缓存副本（不阻塞上传响应）
                resizeAvatar(aliased).catch(() => {})
              } else {
                const index = loadAvatarIndex()
                index[userId] = { ext, fileId: fileData.fileId || '', filePath: fileData.filePath || '', savedAt: Date.now() }
                saveAvatarIndex(index)
              }
              resolve({ fileId: fileData.fileId || '', filePath: fileData.filePath || '', localPath: savePath, ext })
            } else {
              reject(new Error(json.info || '头像上传失败'))
            }
          } catch (e) { reject(new Error('响应解析失败: ' + e.message)) }
        })
      })
      req.on('error', (e) => reject(e))
      req.write(body)
      req.end()
    }

    doUpload('/api/avatar/upload', [{ name: 'userId', value: userId || '' }])
  })
}

export const checkAvatar = ({ userId, fileId, filePath, scope }) => {
  // 机器人头像：按 fileId 直查 BOT_AVATAR_DIR
  if (scope === 'bot' && fileId) {
    const ext = path.extname(filePath || '') || '.png'
    const candidate = getBotAvatarCachePath(fileId, ext)
    if (fs.existsSync(candidate)) return { exists: true, localPath: candidate }
    return { exists: false, localPath: null }
  }

  const index = loadAvatarIndex()
  const entry = index[userId]
  if (entry) {
    const saveName = `avatar_${userId}${entry.ext || '.png'}`
    const localPath = path.join(AVATAR_DIR(), saveName)
    if (fs.existsSync(localPath)) return { exists: true, localPath }
  }
  if (fileId || filePath) {
    const ext = path.extname(filePath || '') || '.png'
    const candidate = path.join(AVATAR_DIR(), `avatar_${userId}${ext}`)
    if (fs.existsSync(candidate)) return { exists: true, localPath: candidate }
  }
  return { exists: false, localPath: null }
}

export const downloadAvatar = ({ userId, fileId, filePath, fileType, scope }) => {
  return new Promise((resolve, reject) => {
    // 机器人头像：按 fileId 直查 + 下载
    if (scope === 'bot') {
      if (!fileId && !filePath) {
        reject(new Error('缺少机器人头像下载参数 (fileId/filePath)'))
        return
      }
      const existing = checkAvatar({ userId, fileId, filePath, scope: 'bot' })
      if (existing.exists) { resolve(existing.localPath); return }

      const config = getServerConfig()
      const ext = path.extname(filePath || '') || '.png'
      const savePath = getBotAvatarCachePath(fileId, ext)
      const targetFileId = fileId || `bot_unknown`

      const options = {
        hostname: config.host, port: config.httpPort || 6060,
        // 复用 /api/file/download（FileController 已支持任意相对路径），传入完整 filePath
        path: `/api/file/download/${targetFileId}?filePath=${encodeURIComponent(filePath || '')}`,
        method: 'GET', headers: { 'token': getUserData('token') || getData('token') || '' }
      }

      const req = http.request(options, (res) => {
        if (res.statusCode !== 200) { reject(new Error(`机器人头像下载失败 HTTP ${res.statusCode}`)); return }
        const ws = fs.createWriteStream(savePath)
        res.pipe(ws)
        ws.on('finish', async () => {
          try {
            // 缩小到 256x256，显示更清晰
            await resizeAvatar(savePath)
          } catch (_) {}
          resolve(savePath)
        })
        ws.on('error', reject)
      })
      req.on('error', reject)
      req.end()
      return
    }

    const existing = checkAvatar({ userId })
    if (existing.exists) { resolve(existing.localPath); return }

    if (!fileId && !filePath) {
      reject(new Error('缺少头像下载参数 (fileId/filePath)'))
      return
    }

    const config = getServerConfig()
    const ext = path.extname(filePath || '') || '.png'
    const savePath = path.join(AVATAR_DIR(), `avatar_${userId}${ext}`)
    const targetFileId = fileId || `avatar_${userId}`

    const options = {
      hostname: config.host, port: config.httpPort || 6060,
      path: `/api/file/download/${targetFileId}?filePath=${encodeURIComponent(filePath || '')}`,
      method: 'GET', headers: { 'token': getUserData('token') || getData('token') || '' }
    }

    const req = http.request(options, (res) => {
      if (res.statusCode !== 200) { reject(new Error(`头像下载失败 HTTP ${res.statusCode}`)); return }
      const ws = fs.createWriteStream(savePath)
      res.pipe(ws)
      ws.on('finish', () => {
        const index = loadAvatarIndex()
        index[userId] = { ext, fileId: fileId || '', filePath: filePath || '', savedAt: Date.now() }
        saveAvatarIndex(index)
        resolve(savePath)
      })
      ws.on('error', reject)
    })
    req.on('error', reject)
    req.end()
  })
}

// ==================== 头像缩略图（sharp, 256x256 适配 HiDPI）====================

/**
 * 将头像文件缩小到 256x256，保持比例，不允许放大
 * @param {string} inputPath  原始文件路径
 * @returns {Promise<string|null>}  缩略图路径（覆盖原文件则返回原路径）, null=失败
 */
const resizeAvatar = async (inputPath) => {
  try {
    const sharp = (await import('sharp')).default
    const metadata = await sharp(inputPath).metadata()
    // 如果原图已经 ≤256x256，跳过
    if (metadata.width <= 256 && metadata.height <= 256) {
      return inputPath
    }
    const tmpPath = inputPath + '.thumb.tmp'
    await sharp(inputPath)
      .resize(256, 256, { fit: 'cover', withoutEnlargement: true })
      .toFile(tmpPath)
    fs.copyFileSync(tmpPath, inputPath)
    fs.unlinkSync(tmpPath)
    console.log('  [sharp] 头像缩至 256x256:', inputPath)
    return inputPath
  } catch (e) {
    console.warn('  [sharp] 头像缩略失败:', e.message, inputPath)
    return inputPath // 兜底：用原图
  }
}

// ==================== 图片缩略图（sharp，文件消息用）====================

const generateImageThumb = async (inputPath, outputName, outputDir) => {
  const thumbPath = path.join(outputDir, `thumb_${outputName}.jpg`)
  try {
    const sharp = (await import('sharp')).default
    await sharp(inputPath)
      .resize(320, 240, { fit: 'cover', withoutEnlargement: true })
      .jpeg({ quality: 80 })
      .toFile(thumbPath)
    console.log('  [sharp] 图片缩略图已生成:', thumbPath)
    return thumbPath
  } catch (e) {
    console.warn('  [sharp] 缩略图生成失败:', e.message)
    return null
  }
}

// ==================== 视频缩略图（fluent-ffmpeg）====================

/** 获取 ffmpeg 二进制路径 */
const getFfmpegPath = () => {
  const isPackaged = app.isPackaged
  if (isPackaged) {
    return path.join(process.resourcesPath, 'ffmpeg', 'ffmpeg.exe')
  }
  // 开发模式：项目根目录 resources/ffmpeg/
  return path.join(__dirname, '..', '..', 'resources', 'ffmpeg', 'ffmpeg.exe')
}

const generateVideoThumb = async (inputPath, outputName, outputDir) => {
  const ffmpeg = (await import('fluent-ffmpeg')).default
  ffmpeg.setFfmpegPath(getFfmpegPath())
  const thumbPath = path.join(outputDir, `thumb_${outputName}.jpg`)
  try {
    await new Promise((resolve, reject) => {
      ffmpeg(inputPath)
        .screenshots({ timestamps: ['5%'], filename: `thumb_${outputName}.jpg`, folder: outputDir, size: '320x240' })
        .on('end', resolve)
        .on('error', reject)
    })
    console.log('  [ffmpeg] 视频缩略图已生成:', thumbPath)
    return thumbPath
  } catch (e) {
    console.warn('  [ffmpeg] 缩略图生成失败:', e.message)
    return null
  }
}

/** 统一缩略图入口 */
export const generateThumbnail = async (localPath, messageType) => {
  const dir = path.dirname(localPath)
  const baseName = path.basename(localPath, path.extname(localPath))

  if (messageType === 33) return generateImageThumb(localPath, baseName, dir)   // IMAGE
  if (messageType === 34) return generateVideoThumb(localPath, baseName, dir)   // VIDEO
  return null
}

/** 查找已存在的缩略图 */
export const findThumb = (localPath) => {
  if (!localPath) return null
  const dir = path.dirname(localPath)
  const baseName = path.basename(localPath, path.extname(localPath))
  const thumbPath = path.join(dir, `thumb_${baseName}.jpg`)
  return fs.existsSync(thumbPath) ? thumbPath : null
}

// ==================== 接收方核心：下载 + 存储 + 缩略图 ====================

export const processFileMessage = async (message) => {
  const { fileId, filePath, messageType, fileType } = message
  console.log('  [processFileMessage] 开始处理文件消息, fileId:', fileId)

  // 1. 先检查本地是否已有（发送方自己已存过，或之前下载过）
  const existing = getLocalFilePath({ fileId, filePath, fileType })
  if (existing) {
    console.log('  [processFileMessage] 本地已有缓存，直接返回:', existing)
    return { localPath: existing, thumbPath: findThumb(existing) }
  }

  // 2. 下载文件
  const config = getServerConfig()
  const dir = getDateDir()
  const ext = path.extname(filePath || '')
  const localName = (fileId || `file_${Date.now()}`) + ext
  const savePath = path.join(dir, localName)

  console.log('  [processFileMessage] 下载文件...')
  await downloadToPath(fileId, filePath, savePath, config)

  // 3. 生成缩略图
  let thumbPath = null
  if (messageType === 33 || messageType === 34) {
    console.log('  [processFileMessage] 生成缩略图...')
    thumbPath = await generateThumbnail(savePath, messageType)
  }

  console.log('  [processFileMessage] 完成, localPath:', savePath, 'thumbPath:', thumbPath)
  return { localPath: savePath, thumbPath }
}

/** 流式下载文件到指定路径 */
const downloadToPath = (fileId, filePath, savePath, config) => {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: config.host,
      port: config.httpPort || 6060,
      path: `/api/file/download/${fileId}?filePath=${encodeURIComponent(filePath)}`,
      method: 'GET',
      headers: { 'token': getUserData('token') || getData('token') || '' }
    }
    const req = http.request(options, (res) => {
      if (res.statusCode !== 200) {
        reject(new Error(`下载失败 HTTP ${res.statusCode}`))
        return
      }
      const ws = fs.createWriteStream(savePath)
      res.pipe(ws)
      ws.on('finish', () => resolve())
      ws.on('error', reject)
    })
    req.on('error', reject)
    req.end()
  })
}

// ==================== 上传文件到后端 ====================

export const uploadFile = ({ filePath, fileBuffer, fileName, meetingNo, token }) => {
  return new Promise((resolve, reject) => {
    const config = getServerConfig()
    const boundary = '----MeetChat' + Date.now()

    // 支持多种来源：fileBuffer（Uint8Array/ArrayBuffer/旧版数组，来自渲染进程）或 filePath（本地路径）
    // 注意：渲染进程应传 TypedArray/ArrayBuffer，不要再传 Array.from 的普通数组（每个字节会膨胀成对象，内存暴涨且 IPC 卡死）
    let actualBuffer
    const hasBufferData = fileBuffer && (fileBuffer.byteLength || (Array.isArray(fileBuffer) && fileBuffer.length > 0))
    if (hasBufferData) {
      actualBuffer = Buffer.from(fileBuffer)
    } else if (filePath && fs.existsSync(filePath)) {
      actualBuffer = fs.readFileSync(filePath)
    } else {
      reject(new Error(`缺少文件数据 (fileBuffer.byteLength=${fileBuffer?.byteLength ?? 'null'}, filePath=${filePath ?? 'null'})`))
      return
    }

    let body = Buffer.concat([
      Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="meetingNo"\r\n\r\n${meetingNo}\r\n`),
      Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="${fileName}"\r\nContent-Type: application/octet-stream\r\n\r\n`),
      actualBuffer,
      Buffer.from(`\r\n--${boundary}--\r\n`)
    ])

    const options = {
      hostname: config.host, port: config.httpPort || 6060,
      path: '/api/file/upload', method: 'POST',
      headers: {
        'Content-Type': `multipart/form-data; boundary=${boundary}`,
        'Content-Length': body.length,
        'X-Requested-With': 'XMLHttpRequest',
        'token': token || ''
      }
    }

    console.log('  [uploadFile] 发送请求 →',
      `http://${options.hostname}:${options.port}${options.path}`,
      `| Content-Type: ${options.headers['Content-Type']}`,
      `| Content-Length: ${options.headers['Content-Length']}`,
      `| token: ${token ? token.substring(0, 8) + '...' : '(空)'}`)
    console.log('  [uploadFile] meetingNo:', meetingNo,
      '| fileName:', fileName,
      '| fileSize:', actualBuffer.length, 'bytes')

    const req = http.request(options, (res) => {
      let data = ''
      res.on('data', chunk => data += chunk)
      res.on('end', () => {
        console.log('  [uploadFile] 响应状态:', res.statusCode,
          '| 响应体长度:', data.length, '字节')
        console.log('  [uploadFile] 响应头:', JSON.stringify(res.headers))
        console.log('  [uploadFile] 响应体:', data)
        try {
          const json = JSON.parse(data)
          if (json.code === 200) {
            console.log('  [uploadFile] ✅ 上传成功, data:', JSON.stringify(json.data))
            resolve(json.data)
          } else {
            console.log('  [uploadFile] ❌ 上传失败, code:', json.code, 'info:', json.info)
            reject(new Error(json.info || '上传失败'))
          }
        } catch (e) {
          console.log('  [uploadFile] ❌ 响应解析失败:', e.message, '| 原始响应:', data)
          reject(new Error('解析响应失败'))
        }
      })
    })
    req.on('error', (e) => {
      console.error('  [uploadFile] ❌ 请求错误:', e.message)
      reject(e)
    })
    req.write(body)
    req.end()
  })
}

// ==================== 从后端下载文件到本地缓存 ====================

export const downloadFile  = ({ fileId, filePath, fileType }) => {
  return new Promise((resolve, reject) => {
    const config = getServerConfig()
    const dir = getTypeDir(getFileCategory(fileType))
    const ext = path.extname(filePath || '')
    const localPath = path.join(dir, (fileId || 'file') + ext)
    if (fs.existsSync(localPath)) {
      console.log('  [file.js] 文件已缓存:', localPath)
      resolve(localPath)
      return
    }
    console.log('  [file.js] 下载文件:', filePath)
    const options = {
      hostname: config.host, port: config.httpPort || 6060,
      path: `/api/file/download/${fileId}?filePath=${encodeURIComponent(filePath)}`,
      method: 'GET', headers: { 'token': getUserData('token') || getData('token') || '' }
    }
    const req = http.request(options, (res) => {
      if (res.statusCode !== 200) {
        reject(new Error(`下载失败 HTTP ${res.statusCode}`))
        return
      }
      const ws = fs.createWriteStream(localPath)
      res.pipe(ws)
      ws.on('finish', () => { console.log('  [file.js] 下载完成:', localPath); resolve(localPath) })
      ws.on('error', reject)
    })
    req.on('error', reject)
    req.end()
  })
}

// ==================== 检查本地是否有文件 ====================

/**
 * 检查文件是否存在于本地存储
 * @param {Object} params - { fileId, filePath, fileType, messageType }
 * @returns {Object} { exists: boolean, localPath: string|null, thumbPath: string|null }
 */
export const checkFile = ({ fileId, filePath, fileType, messageType }) => {
  const localPath = getLocalFilePath({ fileId, filePath, fileType })
  if (localPath && fs.existsSync(localPath)) {
    const thumb = findThumb(localPath)
    return { exists: true, localPath, thumbPath: thumb }
  }
  return { exists: false, localPath: null, thumbPath: null }
}

// ==================== 获取本地文件路径 ====================

export const getLocalFilePath = ({ fileId, filePath, fileType }) => {
  // 先查 LocalFile 新目录（递归搜索日期子目录）
  const searchInLocalFile = () => {
    try {
      const root = LOCAL_FILE_ROOT()
      if (!fs.existsSync(root)) return null
      const walk = (dir) => {
        try {
          const entries = fs.readdirSync(dir, { withFileTypes: true })
          for (const e of entries) {
            const full = path.join(dir, e.name)
            if (e.isDirectory()) {
              const result = walk(full)
              if (result) return result
            } else if (e.name.startsWith(fileId)) {
              return full
            }
          }
        } catch (_) {}
        return null
      }
      return walk(root)
    } catch (_) { return null }
  }

  const localResult = searchInLocalFile()
  if (localResult) return localResult

  // 再查旧缓存目录
  const ext = path.extname(filePath || '')
  const dir = getTypeDir(getFileCategory(fileType))
  const exactPath = path.join(dir, (fileId || 'file') + ext)
  if (fs.existsSync(exactPath)) return exactPath

  if (fileId) {
    try {
      const files = fs.readdirSync(dir)
      const match = files.find(f => f.startsWith(fileId))
      if (match) return path.join(dir, match)
    } catch (_) {}
  }
  return null
}

// ==================== 本地消息存储 ====================

const MESSAGES_FILE = 'file-messages.json'

export const saveFileMessage = (message) => {
  const filePath = path.join(getLocalRoot(), MESSAGES_FILE)
  if (!fs.existsSync(getLocalRoot())) fs.mkdirSync(getLocalRoot(), { recursive: true })
  let messages = []
  try {
    if (fs.existsSync(filePath)) messages = JSON.parse(fs.readFileSync(filePath, 'utf-8'))
  } catch (_) {}
  const exists = messages.find(m =>
    m.messageId === message.messageId || (m.fileId === message.fileId && m.sendTime === message.sendTime)
  )
  if (!exists) {
    messages.push({ ...message, cachedAt: Date.now() })
    if (messages.length > 500) messages = messages.slice(-500)
    fs.writeFileSync(filePath, JSON.stringify(messages, null, 2))
  }
}

// ==================== 分片上传（主进程） ====================

/** 已取消的上传任务集合（用于中断正在进行的分片上传） */
const cancelledUploads = new Set()

/** 默认分片大小 5MB */
const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024

/** 发送进度到渲染进程 */
const sendProgress = (uploadId, data) => {
  try {
    const win = BrowserWindow.getFocusedWindow() || BrowserWindow.getAllWindows()[0]
    if (win && !win.isDestroyed()) {
      win.webContents.send('chunk-upload-progress', { uploadId, ...data })
    }
  } catch (_) {}
}

/** 计算 SHA-256（流式，不占内存） */
export const calculateFileHash = (filePath) => {
  return new Promise((resolve, reject) => {
    const hash = crypto.createHash('sha256')
    const stream = fs.createReadStream(filePath)
    stream.on('data', chunk => hash.update(chunk))
    stream.on('end', () => resolve(hash.digest('hex')))
    stream.on('error', reject)
  })
}

/** 通用 HTTP POST 请求
 * @param {URL} urlObj - 请求 URL
 * @param {string|Object} body - 请求体（字符串当作 form-urlencoded，对象当作 JSON）
 * @param {string} token - 认证 token
 */
const httpPost = (urlObj, body, token) => {
  return new Promise((resolve, reject) => {
    const isString = typeof body === 'string'
    const postData = isString ? body : JSON.stringify(body)
    const options = {
      hostname: urlObj.hostname,
      port: urlObj.port,
      path: urlObj.pathname + (urlObj.search || ''),
      method: 'POST',
      headers: {
        'Content-Type': isString ? 'application/x-www-form-urlencoded' : 'application/json',
        'Content-Length': Buffer.byteLength(postData),
        'X-Requested-With': 'XMLHttpRequest',
        token: token || ''
      }
    }
    const req = http.request(options, (res) => {
      let data = ''
      res.on('data', chunk => data += chunk)
      res.on('end', () => {
        try { resolve(JSON.parse(data)) } catch (e) { reject(new Error('响应解析失败: ' + e.message)) }
      })
    })
    req.on('error', reject)
    req.write(postData)
    req.end()
  })
}

/** 通用 HTTP DELETE 请求 */
const httpDelete = (urlObj, token) => {
  return new Promise((resolve, reject) => {
    const options = {
      hostname: urlObj.hostname,
      port: urlObj.port,
      path: urlObj.pathname + (urlObj.search || ''),
      method: 'DELETE',
      headers: { 'X-Requested-With': 'XMLHttpRequest', token: token || '' }
    }
    const req = http.request(options, (res) => {
      let data = ''
      res.on('data', chunk => data += chunk)
      res.on('end', () => {
        try { resolve(JSON.parse(data)) } catch (e) { reject(new Error('响应解析失败: ' + e.message)) }
      })
    })
    req.on('error', reject)
    req.end()
  })
}

/** 上传单个分片（multipart/form-data，流式切片） */
const uploadChunkHttp = (config, { uploadId, chunkIndex, chunkHash, filePath, start, end, token }) => {
  return new Promise((resolve, reject) => {
    // 流式读取文件片段，避免全量加载
    const chunkStream = fs.createReadStream(filePath, { start, end: end - 1 })
    const chunks = []
    chunkStream.on('data', chunk => chunks.push(chunk))
    chunkStream.on('end', () => {
      const chunkBuffer = Buffer.concat(chunks)
      const boundary = '----Chunk' + Date.now() + Math.random()

      const parts = [
        Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="uploadId"\r\n\r\n${uploadId}\r\n`),
        Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="chunkIndex"\r\n\r\n${chunkIndex}\r\n`)
      ]
      if (chunkHash) {
        parts.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="chunkHash"\r\n\r\n${chunkHash}\r\n`))
      }
      parts.push(Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="chunk_${chunkIndex}"\r\nContent-Type: application/octet-stream\r\n\r\n`))
      parts.push(chunkBuffer)
      parts.push(Buffer.from(`\r\n--${boundary}--\r\n`))
      const body = Buffer.concat(parts)

      const options = {
        hostname: config.host,
        port: config.httpPort || 6060,
        path: '/api/file/upload/chunk',
        method: 'POST',
        headers: {
          'Content-Type': `multipart/form-data; boundary=${boundary}`,
          'Content-Length': body.length,
          'X-Requested-With': 'XMLHttpRequest',
          token: token || ''
        }
      }

      const req = http.request(options, (res) => {
        let data = ''
        res.on('data', chunk => data += chunk)
        res.on('end', () => {
          try {
            const json = JSON.parse(data)
            resolve(json)
          } catch (e) { reject(new Error('分片响应解析失败: ' + e.message)) }
        })
      })
      req.on('error', reject)
      req.write(body)
      req.end()
    })
    chunkStream.on('error', reject)
  })
}

/** 计算分片 MD5 */
const calculateChunkMd5 = (filePath, start, end) => {
  return new Promise((resolve, reject) => {
    const hash = crypto.createHash('md5')
    const stream = fs.createReadStream(filePath, { start, end: end - 1 })
    stream.on('data', chunk => hash.update(chunk))
    stream.on('end', () => resolve(hash.digest('hex')))
    stream.on('error', reject)
  })
}

/**
 * 分片上传完整流程（主进程）
 * @param {Object} params
 * @param {string} params.filePath - 本地文件路径
 * @param {string} params.fileName - 文件名
 * @param {string} params.meetingNo - 会议号
 * @param {number} [params.chunkSize] - 分片大小（默认 5MB）
 * @param {number} [params.concurrency=3] - 并发数
 * @returns {Promise<Object>} 上传结果
 */
export const chunkUpload = async ({ filePath, fileName, meetingNo, chunkSize, concurrency = 3 }) => {
  const config = getServerConfig()
  const token = getUserData('token') || getData('token') || ''
  const baseUrl = `http://${config.host}:${config.httpPort || 6060}`

  if (!filePath || !fs.existsSync(filePath)) {
    throw new Error('文件不存在: ' + filePath)
  }

  const fileSize = fs.statSync(filePath).size
  const actualChunkSize = chunkSize || DEFAULT_CHUNK_SIZE
  const totalChunks = Math.ceil(fileSize / actualChunkSize)

  console.log(`[chunkUpload] 开始分片上传: ${fileName}, size=${fileSize}, chunks=${totalChunks}, chunkSize=${actualChunkSize}`)

  // 1. 计算文件 SHA-256
  sendProgress('calculating', { phase: 'hashing', progress: 0, fileName })
  const fileHash = await calculateFileHash(filePath)
  console.log(`[chunkUpload] SHA-256: ${fileHash}`)
  sendProgress('calculating', { phase: 'hashing', progress: 100, fileHash, fileName })

  // 2. 预检
  const checkUrl = new URL(baseUrl + '/api/file/upload/check')
  const checkBody = `fileHash=${encodeURIComponent(fileHash)}&fileName=${encodeURIComponent(fileName)}&fileSize=${fileSize}&chunkSize=${actualChunkSize}&meetingNo=${encodeURIComponent(meetingNo || '')}`
  const checkResult = await httpPost(checkUrl, checkBody, token)

  if (!checkResult || checkResult.code !== 200) {
    throw new Error(checkResult?.info || '预检请求失败')
  }

  const checkData = checkResult.data
  const status = checkData.status
  const uploadId = checkData.uploadId

  // 立即发送 uploadId + fileHash 关联，让渲染进程能匹配到对应 task
  sendProgress(uploadId, { phase: 'uploading', progress: 0, fileHash, fileName, uploadId })

  // 2a. 秒传
  if (status === 'FAST') {
    console.log('[chunkUpload] 秒传命中')
    sendProgress(uploadId || 'fast', { phase: 'done', progress: 100, fastPass: true, data: checkData, fileHash })
    return checkData
  }

  // 2b. 断点续传或全新上传
  const uploadedChunks = new Set((checkData.uploadedChunks || []).map(Number))
  console.log(`[chunkUpload] 状态=${status}, uploadId=${uploadId}, 已上传=${uploadedChunks.size}/${totalChunks}`)

  // 3. 上传分片（并发控制）
  const pendingChunks = []
  for (let i = 0; i < totalChunks; i++) {
    if (!uploadedChunks.has(i)) {
      pendingChunks.push(i)
    }
  }

  let completedCount = uploadedChunks.size

  const uploadOneChunk = async (chunkIndex) => {
    // 检查是否已取消
    if (cancelledUploads.has(uploadId)) {
      throw new Error('上传已取消')
    }

    const start = chunkIndex * actualChunkSize
    const end = Math.min(start + actualChunkSize, fileSize)

    // 计算分片 MD5
    const chunkHash = await calculateChunkMd5(filePath, start, end)

    const result = await uploadChunkHttp(config, {
      uploadId,
      chunkIndex,
      chunkHash,
      filePath,
      start,
      end,
      token
    })

    if (!result || result.code !== 200) {
      throw new Error(result?.info || `分片 ${chunkIndex} 上传失败`)
    }

    completedCount++
    const progress = Math.round((completedCount / totalChunks) * 100)
    sendProgress(uploadId, {
      phase: 'uploading',
      progress,
      chunkIndex,
      completedCount,
      totalChunks,
      idempotent: result.data?.idempotent || false
    })
  }

  // 并发池
  const pool = []
  for (const chunkIndex of pendingChunks) {
    const p = uploadOneChunk(chunkIndex).then(() => null).catch(e => e)
    pool.push(p)
    if (pool.length >= concurrency) {
      const results = await Promise.all(pool.splice(0))
      const err = results.find(r => r instanceof Error)
      if (err) {
        // 上传出错，尝试取消
        try {
          const cancelUrl = new URL(baseUrl + `/api/file/upload/${uploadId}`)
          await httpDelete(cancelUrl, token)
        } catch (_) {}
        throw err
      }
    }
  }
  // 等待剩余
  if (pool.length > 0) {
    const results = await Promise.all(pool)
    const err = results.find(r => r instanceof Error)
    if (err) {
      try {
        const cancelUrl = new URL(baseUrl + `/api/file/upload/${uploadId}`)
        await httpDelete(cancelUrl, token)
      } catch (_) {}
      throw err
    }
  }

  // 4. 合并
  sendProgress(uploadId, { phase: 'merging', progress: 0 })
  const mergeUrl = new URL(baseUrl + '/api/file/upload/merge')
  const mergeBody = `uploadId=${encodeURIComponent(uploadId)}&fileHash=${encodeURIComponent(fileHash)}&fileName=${encodeURIComponent(fileName)}&totalChunks=${totalChunks}&meetingNo=${encodeURIComponent(meetingNo || '')}`
  const mergeResult = await httpPost(mergeUrl, mergeBody, token)

  if (!mergeResult || mergeResult.code !== 200) {
    throw new Error(mergeResult?.info || '合并失败')
  }

  console.log('[chunkUpload] 合并成功:', JSON.stringify(mergeResult.data))
  sendProgress(uploadId, { phase: 'done', progress: 100, data: mergeResult.data })
  return mergeResult.data
}

/** 取消分片上传 */
export const cancelChunkUpload = async ({ uploadId }) => {
  // 标记为已取消，让正在进行的 chunkUpload 检查并中断
  if (uploadId) cancelledUploads.add(uploadId)

  const config = getServerConfig()
  const token = getUserData('token') || getData('token') || ''
  const baseUrl = `http://${config.host}:${config.httpPort || 6060}`
  const cancelUrl = new URL(baseUrl + `/api/file/upload/${uploadId}`)
  const result = await httpDelete(cancelUrl, token)

  // 延迟清理取消标记（避免内存泄漏）
  setTimeout(() => cancelledUploads.delete(uploadId), 60000)
  return result
}

/** 保存临时文件（渲染进程大文件 → 主进程本地路径 → 分片上传流式读取） */
export const saveTempFile = ({ fileBuffer, fileName }) => {
  const tmpDir = path.join(app.getPath('userData'), 'meetchat-upload-tmp')
  if (!fs.existsSync(tmpDir)) fs.mkdirSync(tmpDir, { recursive: true })
  const tmpName = `upload_${Date.now()}_${fileName}`
  const tmpPath = path.join(tmpDir, tmpName)
  const buffer = Buffer.from(fileBuffer)
  fs.writeFileSync(tmpPath, buffer)
  console.log(`[saveTempFile] 临时文件已保存: ${tmpPath} (${buffer.length} bytes)`)
  return tmpPath
}

/** 清理临时上传文件 */
export const cleanupTempFile = (filePath) => {
  try {
    if (filePath && fs.existsSync(filePath)) {
      fs.unlinkSync(filePath)
      console.log(`[cleanupTempFile] 已清理: ${filePath}`)
    }
  } catch (_) {}
}
