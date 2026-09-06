# WormChat (虫聊) - 后端

基于 **Spring Boot + Netty** 的即时通讯与音视频会议服务端。

> 前端仓库：[../frontend](../frontend)（Electron + Vue 3 桌面客户端）

## 功能特性

- 💬 **IM 消息**：单聊 / 群聊 / 机器人消息，离线消息拉取、ACK 超时重推、消息撤回
- 👥 **群聊管理**：创建 / 解散 / 踢人 / 管理员设置 / 邀请权限控制
- 🤖 **AI 能力**：
  - LLM 对话（OpenAI 兼容协议，默认对接阿里云 DashScope）
  - RAG 知识库：Milvus 向量检索 + MySQL 关键词混合检索
  - MCP 工具调用（时间查询、会议查询等）
  - 情感分析、用户画像、对话摘要、共情策略
- 📹 **会议服务**：会议创建 / 加入 / 结束，参会人状态管理
- 📁 **文件服务**：分片上传、秒传、断点续传
- 🛠 **管理接口**：用户 / 会议 / 群聊管理，操作日志

## 技术栈

| 类别 | 技术 |
|------|------|
| 框架 | Spring Boot 2.x + MyBatis |
| WebSocket | Netty（心跳、集群广播、重试投递） |
| 缓存 | Redis（Redisson） |
| 消息队列 | RabbitMQ（集群广播 / AI 异步回复） |
| 数据库 | MySQL（消息表分表） |
| 向量检索 | Milvus |
| ID 生成 | 雪花算法 |

## 快速开始

### 环境要求

- JDK 8+ / Maven
- MySQL 8、Redis、RabbitMQ
- （可选）Milvus —— 启用 RAG 向量检索时需要，见下方 docker-compose

### 配置

```bash
# 复制配置模板并填写本地环境
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

需要配置的关键项：

- `spring.datasource.*` —— MySQL 连接
- `spring.redis.*` —— Redis 连接
- `spring.rabbitmq.*` —— RabbitMQ 连接
- `ai.llm.api-key` —— LLM API Key（默认对接 DashScope）

### 初始化数据库

```bash
# 创建库后按顺序执行 sql/ 目录下的脚本
mysql -u root -p easymetting < sql/group_chat.sql
```

### 启动 Milvus（可选，RAG 向量检索）

```bash
docker-compose up -d
```

### 运行

```bash
mvn spring-boot:run
```

默认端口：HTTP `6060`（context-path `/api`），WebSocket `6061`。

## 项目结构

```
├── src/main/java/com/meetchat/
│   ├── Controller/     # REST 接口
│   ├── webSocket/      # Netty WebSocket、会话管理、消息投递重试
│   ├── ai/             # LLM 客户端、RAG、MCP、情感分析、限流降级
│   ├── service/        # 业务逻辑
│   ├── mappers/        # MyBatis Mapper
│   └── redis/          # Redis 工具
├── src/main/resources/
│   └── application.properties.example   # 配置模板
├── sql/                # 建表与数据脚本（含压测账号生成）
└── docker-compose.yml  # Milvus 一键部署
```

## 说明

- `sql/loadtest_*.sql`、`insert_loadtest.py` 为压测账号生成脚本（账号密码统一为 `test123456`）
- 消息表按用户分表（`BaseMapperTableSplit`），高并发场景可水平扩展
