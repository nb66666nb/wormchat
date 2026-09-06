# WormChat (虫聊) - 前端

基于 **Electron + Vue 3** 的即时通讯与音视频会议桌面客户端。

> 后端仓库：[../backend](../backend)（Spring Boot + Netty WebSocket）

## 功能特性

- 💬 **IM 即时聊天**：单聊 / 群聊（置顶、免打扰、未读数、消息撤回）、AI 机器人对话
- 🤖 **AI 机器人**：功能型 / 陪伴型 / 混合型，支持自定义人设、头像与欢迎语
- 📹 **音视频会议**：WebRTC 多人会议、屏幕共享、会中聊天、参会人管理
- 👥 **通讯录**：好友搜索、申请与管理
- 📁 **文件传输**：大文件分片上传、断点续传
- 🛠 **管理后台**：用户 / 会议 / 群聊 / 日志管理

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Electron 33 + Vue 3 + Vite |
| UI | Element Plus |
| 状态管理 | Pinia |
| 本地存储 | better-sqlite3（消息与会话本地缓存） |
| 音视频 | WebRTC |
| 构建 | electron-vite + electron-builder |

## 项目结构

```
├── src/main          # Electron 主进程
│   ├── db/           # SQLite 本地缓存（mapper / service / entity）
│   ├── wsClient.js   # WebSocket 客户端与消息分发
│   └── ipc.js        # 主进程 IPC 接口
├── src/preload       # 预加载脚本
└── src/renderer      # 渲染进程（Vue 3）
    └── src/pages/    # chat（聊天）/ meeting（会议）/ contacts（通讯录）/ admin（后台）
```

## 快速开始

### 环境要求

- Node.js >= 18
- 后端服务已启动（见后端仓库 README）

### 安装与运行

```bash
npm install
npm run dev
```

> 首次启动后请在登录页「服务器配置」中填入后端地址。

### 打包

```bash
npm run build:win     # Windows
npm run build:mac     # macOS
npm run build:linux   # Linux
```

> 注意：`resources/ffmpeg/` 下的 ffmpeg 可执行文件不随仓库分发，如需音视频相关能力请自行放置。

## Recommended IDE Setup

- [VSCode](https://code.visualstudio.com/) + [ESLint](https://marketplace.visualstudio.com/items?itemName=dbaeumer.vscode-eslint) + [Prettier](https://marketplace.visualstudio.com/items?itemName=esbenp.prettier-vscode) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar)
