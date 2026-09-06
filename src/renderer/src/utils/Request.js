/**
 * Request.js - API 请求封装
 *
 * 架构设计：
 *   开发环境 (vite dev server) → axios 直接请求（同源代理）
 *   生产环境 (Electron file://) → IPC 到主进程 → Node.js HTTP 请求
 *     → 完全绕过 Chromium CORS 限制
 *     → 不受 renderer 进程网络栈影响
 */

import { ElLoading } from 'element-plus'
import Message from '../utils/Message'
import router from '@/router'

const contentTypeForm = 'application/x-www-form-urlencoded;charset=UTF-8'
const contentTypeJson = 'application/json'
const responseTypeJson = 'json'
let loading = null

const isDev = import.meta.env.DEV

/**
 * 获取 API 基础 URL
 */
const getBaseUrl = () => {
  if (isDev) return '/api'
  try {
    const cfg = JSON.parse(localStorage.getItem('serverConfig') || '{}')
    if (cfg.host) return `http://${cfg.host}:${cfg.httpPort || 6060}/api`
  } catch (_) {}
  return '/api'
}

/**
 * 构建 FormData
 */
const buildFormData = (params) => {
  const fd = new FormData()
  if (params) {
    for (const key of Object.keys(params)) {
      fd.append(key, params[key] === undefined ? '' : params[key])
    }
  }
  return fd
}

/**
 * 构建请求头
 * @param {boolean} json - 是否使用 JSON 内容类型
 */
const buildHeaders = (json = false) => ({
  'Content-Type': json ? contentTypeJson : contentTypeForm,
  'X-Requested-With': 'XMLHttpRequest',
  token: localStorage.getItem('token') || ''
})

/**
 * 开发环境：直接 axios 请求
 * @param {string} url - 请求路径
 * @param {FormData|Object} body - 请求体（FormData 或 JSON 对象）
 * @param {Object} config - 请求配置
 */
const devRequest = async (url, body, config) => {
  const axios = (await import('axios')).default
  const json = config.json === true
  if (json) {
    // JSON 请求：直接发送原始对象，axios 自动序列化为 JSON
    return axios.post(url, body, {
      baseURL: '/api',
      timeout: 10000,
      withCredentials: true,
      headers: buildHeaders(true),
      responseType: config.responseType || responseTypeJson
    })
  }
  return axios.post(url, body, {
    baseURL: '/api',
    timeout: 10000,
    withCredentials: true,
    headers: buildHeaders(),
    responseType: config.responseType || responseTypeJson,
    onUploadProgress: config.uploadProgressCallback
  })
}

/**
 * 生产环境：通过主进程 IPC 请求（彻底绕过 CORS）
 * @param {string} url - 请求路径
 * @param {FormData|Object} body - 请求体（FormData 或 JSON 对象）
 * @param {Object} config - 请求配置
 */
const prodRequest = async (url, body, config) => {
  const json = config.json === true
  // 生产环境如果 window.api 存在，走 IPC
  if (window.api?.apiRequest) {
    let params
    if (json) {
      // JSON 模式：直接使用原始 params 对象（保持数字/布尔类型）
      params = config.params || {}
    } else {
      // 表单模式：将 FormData 转为普通对象
      params = {}
      for (const [key, val] of body.entries()) {
        params[key] = val
      }
    }
    return window.api.apiRequest({
      url,
      baseURL: getBaseUrl(),
      params,
      json,
      headers: buildHeaders(json),
      responseType: config.responseType || responseTypeJson
    })
  }
  // 如果没有 IPC（刷新页面等特殊情况），fallback 到 axios
  const axios = (await import('axios')).default
  return axios.post(url, body, {
    baseURL: getBaseUrl(),
    timeout: 10000,
    withCredentials: true,
    headers: buildHeaders(json),
    responseType: config.responseType || responseTypeJson,
    onUploadProgress: config.uploadProgressCallback
  })
}

/**
 * 统一处理响应
 */
const handleResponse = (responseData, config) => {
  const { errorCallback, showError = true } = config

  if (!responseData) {
    return null
  }

  if (responseData.code === 200) {
    return responseData
  }

  if (responseData.code === 901) {
    window.electron?.ipcRenderer?.invoke('logout')
    router.push('/')
    return null
  }

  if (errorCallback) {
    errorCallback(responseData)
  }
  if (showError) {
    Message.error(responseData.info || '请求失败')
  }
  return null
}

/**
 * 主请求函数
 * @param {Object} config - 请求配置
 * @param {string} config.url - 请求路径
 * @param {Object} config.params - 请求参数
 * @param {boolean} [config.json=false] - 是否使用 JSON 内容类型（后端 @RequestBody 需要）
 * @param {boolean} [config.showLoading=true] - 是否显示加载动画
 * @param {boolean} [config.showError=true] - 是否自动显示错误提示
 */
const request = async (config) => {
  const { url, params, showLoading = true, showError = true } = config
  const json = config.json === true

  // JSON 模式直接使用原始 params 对象（保留数字/布尔类型）；
  // 表单模式转为 FormData
  const body = json ? (params || {}) : buildFormData(params)

  if (showLoading) {
    loading = ElLoading.service({
      lock: true,
      text: '加载中......',
      background: 'rgba(0, 0, 0, 0.7)'
    })
  }

  try {
    let response
    if (isDev) {
      response = await devRequest(url, body, config)
    } else {
      response = await prodRequest(url, body, config)
    }
    return handleResponse(response.data || response, config)
  } catch (error) {
    if (showError) {
      const msg = error?.msg || error?.message || '网络异常'
      Message.error(msg)
    }
    return null
  } finally {
    if (loading) {
      loading.close()
      loading = null
    }
  }
}

export default request
