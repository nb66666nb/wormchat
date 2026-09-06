-- RAG 知识库 v2 迁移脚本：新增 bot_id、session_id、chunk_index 字段
-- 执行方式: 直接在 MySQL 客户端运行

ALTER TABLE rag_knowledge
  ADD COLUMN IF NOT EXISTS bot_id      VARCHAR(32)  DEFAULT NULL COMMENT '关联机器人ID',
  ADD COLUMN IF NOT EXISTS session_id  VARCHAR(32)  DEFAULT NULL COMMENT '关联会话ID（聊天记录导入）',
  ADD COLUMN IF NOT EXISTS chunk_index INT          DEFAULT 0    COMMENT '分块序号（长文档拆分）',
  ADD INDEX IF NOT EXISTS idx_bot_id (bot_id);

-- 如果表尚未创建，请执行 rag_knowledge.sql 后再执行本脚本
