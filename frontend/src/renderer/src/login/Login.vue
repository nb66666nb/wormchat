<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
    <div class="login-box">
      <div class="login-icon">
        <img src="@/assets/logo.svg" alt="虫聊" />
      </div>
      <h1>虫聊</h1>
      <p class="subtitle">用户登录</p>

      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        size="large"
      >
        <el-form-item prop="email">
          <div class="input-wrapper account-input-wrapper">
            <el-icon class="input-icon"><User /></el-icon>
            <el-input
              v-model="loginForm.email"
              placeholder="请输入邮箱"
              class="custom-input"
              @focus="onEmailFocus"
              @blur="onEmailBlur"
            >
              <!-- 有常用账号时显示下拉入口，可随时点击展开/收起 -->
              <template #suffix>
                <el-icon
                  v-if="savedAccounts.length > 0"
                  class="account-toggle"
                  :class="{ 'is-open': showAccountList }"
                  title="选择常用账号"
                  @mousedown.prevent.stop="toggleAccountList"
                >
                  <ArrowDown />
                </el-icon>
              </template>
            </el-input>
            <!-- 常用账号下拉（仅缓存账号，不缓存密码） -->
            <div
              v-if="showAccountList && savedAccounts.length > 0"
              class="account-dropdown"
            >
              <div class="account-dropdown-header">
                <span>常用账号</span>
                <el-button link size="small" type="danger" @mousedown.prevent.stop="clearAllAccounts">
                  清空
                </el-button>
              </div>
              <div
                v-for="acc in savedAccounts"
                :key="acc.email"
                class="account-item"
                @mousedown.prevent.stop="selectAccount(acc)"
              >
                <ChatAvatar
                  :file-id="acc.avatarFileId"
                  :file-path="acc.avatarFilePath"
                  :user-name="acc.nickName || acc.email"
                  :user-id="acc.userId"
                  :size="28"
                />
                <div class="account-info">
                  <span class="account-name">{{ acc.nickName || acc.email }}</span>
                  <span class="account-email">{{ acc.email }}</span>
                </div>
                <span class="account-time">{{ formatAccountTime(acc.lastLoginTime) }}</span>
                <el-icon class="account-remove" @mousedown.prevent.stop="removeAccount(acc.email)">
                  <Close />
                </el-icon>
              </div>
            </div>
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <div class="input-wrapper">
            <el-icon class="input-icon"><Lock /></el-icon>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              class="custom-input"
              show-password
            />
          </div>
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-wrapper">
            <div class="input-wrapper captcha-input">
              <el-icon class="input-icon"><Picture /></el-icon>
              <el-input
                v-model="loginForm.captchaCode"
                placeholder="请输入验证码"
                class="custom-input"
                @keyup.enter="handleLogin"
              />
            </div>
            <div class="captcha-image" @click="getImage">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
            </div>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span class="footer-text">还没有账号？</span>
        <el-button type="primary" link @click="goToRegister">立即注册</el-button>
      </div>
      <div class="server-config-link" @click="showServerConfig">
        <el-icon style="margin-right:4px"><Setting /></el-icon>服务器设置
      </div>
    </div>
    <ServerConfig ref="serverConfigRef" />
  </div>
</template>

<script setup>
import { ref, reactive, getCurrentInstance, onMounted, onUnmounted } from "vue"
const { proxy } = getCurrentInstance();
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { User, Lock, Picture, Setting, Close, ArrowDown } from '@element-plus/icons-vue'
import UserInfoStore from '@/stores/UserInfoStore'
import ServerConfig from '@/components/ServerConfig.vue'
import ChatAvatar from '@/components/ChatAvatar.vue'

const router = useRouter()
const serverConfigRef = ref(null)

const showServerConfig = () => {
  serverConfigRef.value?.show()
}

const useServerConfig = () => {
  try {
    const cfg = localStorage.getItem('serverConfig')
    if (cfg) return JSON.parse(cfg)
  } catch (_) {}
  return null
}

const loginFormRef = ref(null)
const loading = ref(false)
const captchaKey = ref('')
const captchaImage = ref('')

const loginForm = reactive({
  email: '',
  password: '',
  captchaCode: ''
})

const loginRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

// ==================== 常用账号缓存 ====================
// 仅缓存账号（邮箱/昵称/头像/最后登录时间），不缓存密码：
// 登录本身还需图形验证码，记密码也无法真正一键登录，且明文存密码有安全风险。

const SAVED_ACCOUNTS_KEY = 'savedLoginAccounts'
/** 最多保留的常用账号数量 */
const MAX_SAVED_ACCOUNTS = 5

const savedAccounts = ref([])
const showAccountList = ref(false)

/** 读取本地缓存的常用账号 */
const loadSavedAccounts = () => {
  try {
    const raw = localStorage.getItem(SAVED_ACCOUNTS_KEY)
    const list = raw ? JSON.parse(raw) : []
    savedAccounts.value = Array.isArray(list) ? list : []
  } catch (_) {
    savedAccounts.value = []
  }
}

/** 邮箱框聚焦：有缓存账号则展开下拉 */
const onEmailFocus = () => {
  loadSavedAccounts()
  showAccountList.value = savedAccounts.value.length > 0
}

/** 点击右侧下拉箭头：手动展开/收起常用账号列表 */
const toggleAccountList = () => {
  loadSavedAccounts()
  showAccountList.value = !showAccountList.value
}

/**
 * 点击页面其它区域时收起下拉
 * 用 mousedown 与下拉项的 @mousedown 保持同一时序，避免 click 晚于 blur 导致失效
 */
const onDocumentClick = (e) => {
  if (!showAccountList.value) return
  if (e.target && e.target.closest && e.target.closest('.account-input-wrapper')) return
  showAccountList.value = false
}

/** 邮箱框失焦：延迟关闭，避免点击下拉项时 mousedown 前就被关掉 */
const onEmailBlur = () => {
  setTimeout(() => { showAccountList.value = false }, 200)
}

/** 选中某个常用账号 → 填充邮箱 */
const selectAccount = (acc) => {
  loginForm.email = acc.email
  showAccountList.value = false
}

/** 删除单个常用账号 */
const removeAccount = (email) => {
  savedAccounts.value = savedAccounts.value.filter(a => a.email !== email)
  localStorage.setItem(SAVED_ACCOUNTS_KEY, JSON.stringify(savedAccounts.value))
  if (savedAccounts.value.length === 0) showAccountList.value = false
}

/** 清空全部常用账号 */
const clearAllAccounts = () => {
  savedAccounts.value = []
  localStorage.setItem(SAVED_ACCOUNTS_KEY, '[]')
  showAccountList.value = false
}

/** 最后登录时间：今天显示 HH:mm，否则显示 M-D */
const formatAccountTime = (t) => {
  if (!t) return ''
  const d = new Date(t)
  const pad = (n) => String(n).padStart(2, '0')
  const now = new Date()
  if (d.toDateString() === now.toDateString()) {
    return `${pad(d.getHours())}:${pad(d.getMinutes())}`
  }
  return `${d.getMonth() + 1}-${d.getDate()}`
}

/**
 * 登录成功后写入/更新常用账号缓存
 * 同一邮箱只保留一条，并置到列表最前（按最近登录排序）
 */
const saveAccountToCache = (userInfo) => {
  const email = (loginForm.email || '').trim()
  if (!email) return
  const list = [...savedAccounts.value]
  const idx = list.findIndex(a => a.email === email)
  const record = {
    email,
    nickName: userInfo?.userName || userInfo?.nickName || '',
    userId: userInfo?.userId || '',
    avatarFileId: userInfo?.avatarFileId || userInfo?.fileId || '',
    avatarFilePath: userInfo?.avatarFilePath || userInfo?.filePath || '',
    lastLoginTime: Date.now()
  }
  if (idx !== -1) list.splice(idx, 1)
  list.unshift(record)
  const trimmed = list.slice(0, MAX_SAVED_ACCOUNTS)
  savedAccounts.value = trimmed
  localStorage.setItem(SAVED_ACCOUNTS_KEY, JSON.stringify(trimmed))
}

const getImage = async () => {
  const result = await proxy.Request({
    url: proxy.Api.getImage,
    showLoading: false
  })
  if (!result) {
    ElMessage.error('获取验证码失败')
    return
  }
  if (result) {
    localStorage.setItem('captchaKey', result.data.captchaKey)
    captchaImage.value = result.data.captchaImage
  }
}

const getCaptchaKey = () => {
  return localStorage.getItem('captchaKey') || ''
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      const result = await proxy.Request({
        url: proxy.Api.login,
        params: {
          email: loginForm.email,
          password: loginForm.password,
          captchaKey: getCaptchaKey(),
          captchaCode: loginForm.captchaCode
        }
      })
      loading.value = false
      if (!result) {
        getImage()
        return
      }
      if (result.code !== 200) {
        ElMessage.error(result.info || '登录失败')
        getImage()
        return
      }
      UserInfoStore.setUserInfo(result.data)
      localStorage.setItem('token', result.data.token)
      // 登录成功：写入/更新常用账号缓存（下次可直接点选，免输邮箱）
      saveAccountToCache(result.data)
      ElMessage.success('登录成功')
      // 注入服务器配置到 userInfo（WebSocket 连接 + 主进程存储用）
      const serverCfg = useServerConfig()
      const userInfoWithWs = {
        ...result.data,
        wsHost: serverCfg?.host || 'localhost',
        httpPort: serverCfg?.httpPort || '6060',  // 新增：传给主进程存储
        wsPort: serverCfg?.wsPort || '6061'
      }
      window.api.saveUserInfo(userInfoWithWs)
      setTimeout(() => {
        window.api.changeWindowSize()
        router.push('/main')
      }, 500)
    }
  })
}

const goToRegister = () => {
  router.push('/register')
}

onMounted(() => {
  getImage()
  loadSavedAccounts()
  // 自动回填最近一次登录的账号（仅邮箱，不含密码），打开登录页即可直接输密码
  if (savedAccounts.value.length > 0) {
    loginForm.email = savedAccounts.value[0].email
  }
  document.addEventListener('mousedown', onDocumentClick)
})

onUnmounted(() => {
  document.removeEventListener('mousedown', onDocumentClick)
})
</script>

<style lang="css" scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.login-container {
  width: 100%;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  background: linear-gradient(135deg, #f5f7fa 0%, #e8ecf1 50%, #f0f4f8 100%);
  position: relative;
  overflow: hidden;
  padding: 40px 16px 20px;
}

.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  opacity: 0.15;
}

.circle-1 {
  width: 400px;
  height: 400px;
  background: #1a73e8;
  top: -150px;
  left: -150px;
  animation: float 12s ease-in-out infinite;
}

.circle-2 {
  width: 300px;
  height: 300px;
  background: #34a853;
  bottom: -100px;
  right: -100px;
  animation: float 15s ease-in-out infinite reverse;
}

.circle-3 {
  width: 200px;
  height: 200px;
  background: #ea4335;
  top: 45%;
  left: 55%;
  transform: translate(-50%, -50%);
  animation: pulse 10s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(30px, 20px); }
}

@keyframes pulse {
  0%, 100% { transform: translate(-50%, -50%) scale(1); opacity: 0.1; }
  50% { transform: translate(-50%, -50%) scale(1.3); opacity: 0.18; }
}

.login-box {
  width: 100%;
  max-width: 380px;
  padding: 36px 28px 24px;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e8eaed;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06), 0 8px 32px rgba(0, 0, 0, 0.04);
  position: relative;
  z-index: 10;
  margin: 0 auto;
}

.login-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 14px;
}

.login-icon img {
  width: 100%;
  height: 100%;
  display: block;
}

.login-box h1 {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a1a;
  text-align: center;
  margin-bottom: 2px;
  letter-spacing: -0.3px;
}

.subtitle {
  font-size: 13px;
  color: #5f6368;
  text-align: center;
  margin-bottom: 24px;
  font-weight: 400;
}

.login-form {
  width: 100%;
}

.input-wrapper {
  position: relative;
  width: 100%;
}

/* ==================== 常用账号下拉 ==================== */
.account-input-wrapper {
  /* 下拉需要相对此容器定位 */
  position: relative;
}

/* 邮箱框右侧的「选择常用账号」入口（有缓存账号时才显示） */
.account-toggle {
  cursor: pointer;
  color: #9aa0a6;
  font-size: 15px;
  padding: 4px;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.account-toggle:hover {
  color: #1a73e8;
  background: rgba(26, 115, 232, 0.08);
}

.account-toggle.is-open {
  color: #1a73e8;
  transform: rotate(180deg);
}

.account-dropdown {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  z-index: 100;
  background: #ffffff;
  border: 1px solid #e8eaed;
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  padding: 6px;
  max-height: 250px;
  overflow-y: auto;
  animation: accountDropIn 0.14s ease-out;
}

.account-dropdown::-webkit-scrollbar {
  width: 4px;
}
.account-dropdown::-webkit-scrollbar-thumb {
  background-color: #dadce0;
  border-radius: 2px;
}

.account-dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 3px 8px 6px;
  font-size: 11px;
  color: #9aa0a6;
  border-bottom: 1px solid #f1f3f4;
  margin-bottom: 4px;
}

.account-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
}

.account-item:hover {
  background: #f1f3f4;
}

.account-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.account-name {
  font-size: 13px;
  color: #202124;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-email {
  font-size: 11px;
  color: #9aa0a6;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-time {
  font-size: 10px;
  color: #bdc1c6;
  flex-shrink: 0;
}

.account-remove {
  flex-shrink: 0;
  color: #bdc1c6;
  font-size: 14px;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.15s;
}

.account-remove:hover {
  color: #ea4335;
  background: #fee2e2;
}

@keyframes accountDropIn {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}

.input-icon {
  position: absolute;
  left: 13px;
  top: 50%;
  transform: translateY(-50%);
  color: #9aa0a6;
  z-index: 10;
  font-size: 17px;
}

.custom-input {
  width: 100%;
}

.custom-input :deep(.el-input__wrapper) {
  background: #f8f9fa;
  border: 1px solid #dadce0;
  border-radius: 10px;
  padding: 3px 13px 3px 42px;
  box-shadow: none;
  transition: all 0.2s ease;
}

.custom-input :deep(.el-input__wrapper:hover),
.custom-input :deep(.el-input__wrapper.is-focus) {
  background: #ffffff;
  border-color: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.1);
}

.custom-input :deep(.el-input__inner) {
  color: #202124;
  font-size: 14px;
  height: 42px;
}

.custom-input :deep(.el-input__inner::placeholder) {
  color: #9aa0a6;
}

.custom-input :deep(.el-input__suffix) {
  color: #5f6368;
}

.captcha-wrapper {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-input {
  flex: 1;
}

.captcha-image {
  width: 110px;
  height: 42px;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  background: #f8f9fa;
  border: 1px solid #dadce0;
  transition: all 0.2s ease;
}

.captcha-image:hover {
  border-color: #1a73e8;
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.08);
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.login-button {
  width: 100%;
  height: 46px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 500;
  letter-spacing: 1px;
  background: #1a73e8;
  border: none;
  color: #fff;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 2px 6px rgba(26, 115, 232, 0.25);
}

.login-button:hover {
  background: #1557b0;
  box-shadow: 0 4px 12px rgba(26, 115, 232, 0.35);
  transform: translateY(-1px);
}

.login-button:active {
  transform: translateY(0);
  background: #134c96;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.login-form :deep(.el-form-item__error) {
  color: #d93025;
  font-size: 11px;
  padding-top: 4px;
}

.login-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 18px;
  gap: 4px;
}

.footer-text {
  color: #5f6368;
  font-size: 13px;
}

.login-footer .el-button {
  color: #1a73e8;
  font-size: 13px;
  font-weight: 600;
}

.login-footer .el-button:hover {
  color: #1557b0;
}

.server-config-link {
  text-align: center;
  margin-top: 18px;
  font-size: 12px;
  color: #9aa0a6;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s ease;
}

.server-config-link:hover {
  color: #5f6368;
}
</style>

