-- ============================================================
-- 消息撤回功能：给 chat_im_message 表增加撤回标记字段
-- recalled     0=正常 1=已撤回
-- recall_time  撤回时间戳(ms)
-- ============================================================

ALTER TABLE chat_im_message ADD COLUMN recalled TINYINT NOT NULL DEFAULT 0 COMMENT '是否已撤回:0正常 1已撤回';
ALTER TABLE chat_im_message ADD COLUMN recall_time BIGINT NOT NULL DEFAULT 0 COMMENT '撤回时间戳(ms)';

-- 已撤回消息查询索引（按会话过滤已撤回消息时使用）
CREATE INDEX idx_im_session_recalled ON chat_im_message (session_id, recalled);
