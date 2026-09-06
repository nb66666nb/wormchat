<template>
  <div class="login-container">
    <div class="login-bg">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
    <div class="login-box">
      <div class="back-button" @click="goToLogin">
        <el-icon><ArrowLeft /></el-icon>
      </div>
      <div class="login-icon">
        <svg viewBox="0 0 128 128" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="64" cy="64" r="64" fill="url(#gradient)"/>
          <path d="M64 30v68M30 64h68" stroke="#fff" stroke-width="8" stroke-linecap="round"/>
          <defs>
            <linearGradient id="gradient" x1="0" y1="0" x2="128" y2="128">
              <stop offset="0%" stop-color="#667eea"/>
              <stop offset="100%" stop-color="#764ba2"/>
            </linearGradient>
          </defs>
        </svg>
      </div>
      <h1>虫聊</h1>
      <p class="subtitle">用户注册</p>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        class="login-form"
        size="large"
      >
        <el-form-item prop="username">
          <div class="input-wrapper">
            <el-icon class="input-icon"><User /></el-icon>
            <el-input
              v-model="registerForm.username"
              placeholder="请输入用户名"
              class="custom-input"
            />
          </div>
        </el-form-item>
        <el-form-item prop="email">
          <div class="input-wrapper">
            <el-icon class="input-icon"><Message /></el-icon>
            <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱"
              class="custom-input"
            />
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <div class="input-wrapper">
            <el-icon class="input-icon"><Lock /></el-icon>
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="请输入密码"
              class="custom-input"
              show-password
            />
          </div>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <div class="input-wrapper">
            <el-icon class="input-icon"><Lock /></el-icon>
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="请确认密码"
              class="custom-input"
              show-password
              @keyup.enter="handleRegister"
            />
          </div>
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-wrapper">
            <div class="input-wrapper captcha-input">
              <el-icon class="input-icon"><Picture /></el-icon>
              <el-input
                v-model="registerForm.captchaCode"
                placeholder="请输入验证码"
                class="custom-input"
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
            @click="handleRegister"
          >
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span class="footer-text">已有账号？</span>
        <el-button type="primary" link @click="goToLogin">立即登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, getCurrentInstance, onMounted } from 'vue'
const { proxy } = getCurrentInstance();
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { User, Lock, Message, ArrowLeft, Picture } from '@element-plus/icons-vue'

const router = useRouter()
const registerFormRef = ref(null)
const loading = ref(false)
const captchaKey = ref('')
const captchaImage = ref('')

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  captchaCode: ''
})

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

const getImage = async () => {
  const result = await proxy.Request({
    url: proxy.Api.getImage,
    showLoading: false
  })
  if (result) {
    localStorage.setItem('captchaKey', result.data.captchaKey)
    captchaImage.value = result.data.captchaImage
  }
}

const getCaptchaKey = () => {
  return localStorage.getItem('captchaKey') || ''
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      const result = await proxy.Request({
        url: proxy.Api.register,
        params: {
          email: registerForm.email,
          password: registerForm.password,
          captchaKey: getCaptchaKey(),
          captchaCode: registerForm.captchaCode,
          nickName: registerForm.username
        }
      })
      loading.value = false
      if (!result) {
        getImage()
        return
      }
      if (result.code !== 200) {
        ElMessage.error(result.info || '注册失败')
        getImage()
        return
      }
      ElMessage.success('注册成功，请登录')
      router.push('/')
    }
  })
}

const goToLogin = () => {
  router.push('/')
}

onMounted(() => {
  getImage()
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

.back-button {
  position: absolute;
  top: 16px;
  left: 16px;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  border: 1px solid #e8eaed;
  border-radius: 9px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.back-button:hover {
  background: #f1f3f4;
  border-color: #dadce0;
}

.back-button .el-icon {
  color: #5f6368;
  font-size: 17px;
}

.login-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto 14px;
}

.login-icon svg {
  width: 100%;
  height: 100%;
  border-radius: 13px;
  box-shadow: 0 4px 14px rgba(26, 115, 232, 0.25);
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
</style>
