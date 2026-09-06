# 基于 Java、Netty、SpringBoot 的 IM 聊天（虫聊）系统软件

**WormChat（虫聊）** 是一个全栈即时通讯与音视频会议系统，包含 Electron 桌面客户端与服务端，支持单聊 / 群聊 / AI 机器人对话与多人视频会议。

![Arch](https://img.shields.io/badge/Backend-Java%2011%20%2B%20Netty%20%2B%20SpringBoot-orange) ![Front](https://img.shields.io/badge/Frontend-Electron%20%2B%20Vue3%20%2B%20Vite-blue) ![DB](https://img.shields.io/badge/Storage-MySQL%20%2B%20Redis%20%2B%20SQLite%20%2B%20Milvus-green)

## 系统架构

```
┌─────────────────────────────────────────────────┐
│            WormChat 桌面客户端（frontend）          │
│   Electron + Vue 3 + WebRTC + SQLite 本地缓存     │
└────────────────┬───────────────┬────────────────┘
                 │ HTTP (REST)   │ WebSocket (Netty 6061)
                 ▼               ▼
┌─────────────────────────────────────────────────┐
│              服务端（backend）                     │
│  Spring Boot 6060 /api  │  Netty WebSocket 6061   │
│  ┌──────────┬──────────┬──────────┬───────────┐  │
│  │ IM 消息服务 │ 群聊管理  │ 会议服务  │ AI 能力    │  │
│  └──────────┴──────────┴──────────┴───────────┘  │
│       │            │           │          │       │
│  ┌────▼───┐  ┌─────▼────┐ ┌───▼────┐ ┌──▼─────┐  │
│  │ MySQL  │  │  Redis   │ │RabbitMQ│ │ Milvus │  │
│  │(消息分表)│  │(会话/缓存)│ │(集群广播)│ │(RAG向量)│  │
│  └────────┘  └──────────┘ └────────┘ └────────┘  │
└─────────────────────────────────────────────────┘
```

## 功能特性

### 💬 IM 即时通讯
- 单聊 / 群聊：消息撤回、置顶、未读数、会话软删除
- 离线消息拉取、ACK 确认与超时重推（指数退避）
- 雪花算法分布式 ID、消息表按用户分表，支撑高并发
- 客户端 SQLite 本地缓存（消息、会话、群信息镜像同步）

### 👥 群聊系统
- 创建 / 解散 / 退出 / 踢人 / 管理员设置
- 邀请权限控制（仅管理员可邀 / 所有人可邀）
- 成员加入 / 退出事件全群 WebSocket 广播

### 🤖 AI 机器人
- 三类机器人：功能型（会议助手）/ 陪伴型（情感陪伴）/ 混合型
- LLM 对话：OpenAI 兼容协议，默认对接阿里云 DashScope（qwen 系列）
- RAG 知识库：Milvus 向量检索 + MySQL 关键词检索混合
- MCP 工具调用：时间查询、会议查询等
- 情感分析、用户画像、共情策略、对话摘要
- 限流与降级：用户级频率限制、Agent 并发控制、LLM 全局 QPS

### 📹 音视频会议
- WebRTC 多人音视频、屏幕共享
- 会议创建 / 加入 / 结束、参会人状态管理、会中聊天
- 会议密码、会议记录

### 📁 文件传输
- 大文件分片上传、秒传、断点续传
- 单文件最大 5GB、并发上传任务控制

### 🛠 管理后台
- 用户 / 会议 / 群聊管理、操作日志
- 管理员权限（邮箱白名单）

## 技术栈

| 层 | 技术 |
|----|------|
| 客户端 | Electron 33、Vue 3、Vite、Element Plus、Pinia、better-sqlite3、WebRTC |
| 服务端框架 | Java、Spring Boot、MyBatis |
| 通信 | Netty WebSocket（心跳保活、集群广播、重试投递）、REST |
| 存储 | MySQL（消息分表）、Redis（Redisson 分布式锁）、SQLite（客户端缓存） |
| 消息队列 | RabbitMQ（集群广播 / AI 异步回复） |
| AI | DashScope LLM、Milvus 向量库、RAG 混合检索、MCP |
| 构建 | Maven、electron-vite、electron-builder |

## 仓库结构

```
wormchat/
├── frontend/    # Electron + Vue 3 桌面客户端
│   ├── src/main/          # 主进程（SQLite、WebSocket 客户端、IPC）
│   └── src/renderer/      # 渲染进程（聊天 / 会议 / 通讯录 / 管理后台）
└── backend/     # Spring Boot + Netty 服务端
    ├── src/main/java/com/meetchat/
    │   ├── webSocket/     # Netty WebSocket、会话管理、消息投递重试
    │   ├── ai/            # LLM、RAG、MCP、情感分析、限流降级
    │   ├── service/       # 业务逻辑
    │   └── mappers/       # MyBatis Mapper
    ├── sql/               # 建表脚本与压测数据生成
    └── docker-compose.yml # Milvus 一键部署
```

## 快速开始

### 1. 启动后端

```bash
cd backend

# 配置（填写 MySQL/Redis/RabbitMQ 连接与 LLM API Key）
cp src/main/resources/application.properties.example src/main/resources/application.properties

# 初始化数据库（按需执行 sql/ 下脚本）
mvn spring-boot:run        # HTTP 6060 /api，WebSocket 6061
```

### 2. 启动客户端

```bash
cd frontend
npm install
npm run dev                # 首次启动在登录页配置服务器地址
```

详细说明见各子目录 README：
- [frontend/README.md](frontend/README.md)
- [backend/README.md](backend/README.md)

## 性能与压测

- 内置 JMeter 压测方案：千级账号自动生成（`backend/sql/insert_loadtest.py`）、IM 发消息脚本
- Tomcat 线程池 / HikariCP 连接池 / Netty 心跳等参数均已按压测调优
- 消息表分表设计支持水平扩展

## 说明

- `frontend/resources/ffmpeg/` 可执行文件不入库，需自行放置
- 真实配置文件 `application.properties` 不入库，请使用 example 模板
