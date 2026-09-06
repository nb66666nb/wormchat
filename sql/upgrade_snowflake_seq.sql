-- =====================================================================
-- 升级脚本 V1.1：消息 ID 雪花化 + 服务端会话内序号(session_seq)
-- 说明：
--   1. message_id 保持 BIGINT，改为应用层雪花算法生成（AUTO_INCREMENT 保留作兜底，
--      显式插入雪花 ID 时自增列不再生效）。雪花 ID 趋势递增，可继续作为
--      离线增量拉取的游标（message_id > lastMessageId）。
--   2. session_seq 为「同一会话内服务端单调递增序号」，由 Redis INCR 生成，
--      用于多端/重连场景下的消息顺序校验与空洞检测。
--   3. 历史数据的 session_seq 为 NULL，客户端按 message_id 排序即可，兼容无损。
-- =====================================================================

ALTER TABLE `chat_im_message`
    ADD COLUMN `session_seq` BIGINT NULL DEFAULT NULL COMMENT '会话内单调递增序号(服务端Redis INCR生成,用于顺序校验)' AFTER `message_only_id`;

-- 可选：为增量拉取与幂等查询补索引（若已存在请忽略报错）
ALTER TABLE `chat_im_message`
    ADD INDEX `idx_receive_user_msg_id` (`receive_user_id`, `message_id`) USING BTREE,
    ADD INDEX `idx_session_seq` (`session_id`, `session_seq`) USING BTREE;

-- 说明：MySQL 8.0 以下版本请分别执行上面两条 ADD INDEX
