import './assets/main.css'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import Api from './utils/Api'
import Request from './utils/Request'

// 确保服务器配置存在默认值，避免组件读取 serverConfig 时 host 为 undefined
// 导致图片/文件 URL 拼成 http://undefined:6060/... 触发 CSP 拦截
if (!localStorage.getItem('serverConfig')) {
  localStorage.setItem('serverConfig', JSON.stringify({
    host: 'localhost', httpPort: '6060', wsPort: '6061', wsPath: '/ws'
  }))
}

const app = createApp(App)
app.use(ElementPlus)
app.use(router)
app.config.globalProperties.Api = Api
app.config.globalProperties.Request = Request
app.mount('#app')
