<template>
  <el-dialog
    v-model="visible"
    title="服务器设置"
    width="380px"
    :close-on-click-modal="false"
    top="25vh"
  >
    <div class="server-config-form">
      <div class="form-item">
        <label>服务器地址（IP 或域名）</label>
        <el-input
          v-model="form.host"
          placeholder="例如 192.168.1.100"
          size="small"
          clearable
        />
      </div>
      <div class="form-row">
        <div class="form-item flex-1">
          <label>HTTP 端口 <span class="port-hint">(API 请求)</span></label>
          <el-input v-model="form.httpPort" placeholder="6060" size="small" />
        </div>
        <div class="form-item flex-1">
          <label>WS 端口 <span class="port-hint">(信令)</span></label>
          <el-input v-model="form.wsPort" placeholder="6061" size="small" />
        </div>
      </div>
      <div class="port-warning" v-if="form.httpPort === form.wsPort && form.httpPort">
        ⚠️ HTTP 端口和 WebSocket 端口不能相同！HTTP=6060, WS=6061
      </div>
      <div class="form-item">
        <label>WebSocket 路径</label>
        <el-input v-model="form.wsPath" placeholder="/ws" size="small" />
      </div>
      <div v-if="testResult" class="test-result" :class="testResult.success ? 'success' : 'error'">
        {{ testResult.msg }}
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button size="small" @click="testConnection" :loading="testing">
          {{ testing ? '测试中...' : '测试连接' }}
        </el-button>
        <el-button size="small" type="primary" @click="saveConfig">
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const visible = ref(false)
const testing = ref(false)
const testResult = ref(null)

const form = reactive({
  host: '',
  httpPort: '6060',
  wsPort: '6061',
  wsPath: '/ws'
})

// 从 localStorage 加载已有配置
const loadConfig = () => {
  try {
    const saved = localStorage.getItem('serverConfig')
    if (saved) {
      const cfg = JSON.parse(saved)
      form.host = cfg.host || ''
      form.httpPort = cfg.httpPort || '6060'
      form.wsPort = cfg.wsPort || '6061'
      form.wsPath = cfg.wsPath || '/ws'
    }
  } catch (e) {
    console.warn('加载服务器配置失败:', e)
  }
}

const show = () => {
  loadConfig()
  testResult.value = null
  visible.value = true
}

const saveConfig = () => {
  if (!form.host.trim()) {
    ElMessage.warning('请输入服务器地址')
    return
  }

  if (form.httpPort === form.wsPort) {
    ElMessage.warning('HTTP 端口和 WebSocket 端口不能相同！请检查后重试')
    return
  }

  const config = {
    host: form.host.trim(),
    httpPort: form.httpPort || '6060',
    wsPort: form.wsPort || '6061',
    wsPath: form.wsPath || '/ws'
  }

  localStorage.setItem('serverConfig', JSON.stringify(config))
  ElMessage.success('服务器配置已保存')
  visible.value = false
}

const testConnection = async () => {
  if (!form.host.trim()) {
    ElMessage.warning('请输入服务器地址')
    return
  }

  testing.value = true
  testResult.value = null

  try {
    let result
    // 优先使用主进程测试（打包后避免 CORS 限制）
    if (window.api?.testServerConnection) {
      result = await window.api.testServerConnection({
        host: form.host.trim(),
        httpPort: form.httpPort || '6060'
      })
    } else {
      // 开发环境 fallback：使用 axios 直接请求
      const axios = (await import('axios')).default
      const url = `http://${form.host.trim()}:${form.httpPort || 6060}/api/account/getImage`
      try {
        const resp = await axios.get(url, { timeout: 5000 })
        result = resp.data && resp.data.code === 200
          ? { success: true, msg: '连接成功！服务器正常响应' }
          : { success: false, msg: `服务器响应异常: ${resp.data?.info || '未知'}` }
      } catch (e) {
        result = { success: false, msg: `连接失败: ${e.message}` }
      }
    }
    testResult.value = {
      success: result.success,
      msg: (result.success ? '✅ ' : '❌ ') + result.msg
    }
  } catch (e) {
    testResult.value = { success: false, msg: `❌ 测试异常: ${e.message}` }
  } finally {
    testing.value = false
  }
}

defineExpose({ show })
</script>

<style scoped>
.server-config-form {
  padding: 4px 0;
}

.form-item {
  margin-bottom: 14px;
}

.form-item label {
  display: block;
  font-size: 12px;
  color: #606266;
  margin-bottom: 6px;
  font-weight: 500;
}

.port-hint {
  font-weight: 400;
  color: #909399;
}

.port-warning {
  margin-bottom: 12px;
  padding: 6px 10px;
  font-size: 11px;
  color: #e6a23c;
  background: #fdf6ec;
  border-radius: 6px;
  border: 1px solid #faecd8;
}

.form-row {
  display: flex;
  gap: 12px;
}

.flex-1 {
  flex: 1;
}

.test-result {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  font-size: 12px;
}

.test-result.success {
  background: #f0f9eb;
  color: #67c23a;
  border: 1px solid #e1f3d8;
}

.test-result.error {
  background: #fef0f0;
  color: #f56c6c;
  border: 1px solid #fde2e2;
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
}
</style>
