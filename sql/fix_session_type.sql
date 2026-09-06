-- =============================================================
-- 修复"用户单聊会话被误标成群聊类型"的数据清理脚本
-- -------------------------------------------------------------
-- 背景：
--   后端 ChatSessionServiceImpl.createSession() 的去重逻辑原先不区分
--   session_type，添加好友时若命中群聊/机器人会话，复用分支不会修正类型，
--   导致用户单聊会话的 session_type 被误标为 1（群聊）。
--   本脚本已从代码层修复（createSession 按会话类型区分查重并修正类型），
--   以下 SQL 用于清理历史已污染的数据。
--
-- 判断依据：
--   群聊会话的 target_user_id = 群ID，群ID 一律以 "G" 开头；
--   用户单聊会话的 target_user_id = 用户ID，为 11 位纯数字；
--   机器人会话的 target_user_id 以 "R" 开头。
--   因此 session_type = 1 但 target_user_id 不以 "G" 开头的记录，
--   必然是"用户单聊被误标成群聊"的脏数据。
-- =============================================================

-- 1) 先预览将要被修正的记录（确认无误再执行第2步）
SELECT session_id, user_id, target_user_id, target_nick_name, session_type
FROM chat_session
WHERE session_type = 1
  AND target_user_id IS NOT NULL
  AND target_user_id NOT LIKE 'G%';

-- 2) 执行修正：将误标成群聊的用户会话改回单聊（session_type = 0）
UPDATE chat_session
SET session_type = 0
WHERE session_type = 1
  AND target_user_id IS NOT NULL
  AND target_user_id NOT LIKE 'G%';

-- 3) 核对修正结果（应无残留 session_type=1 且 target 非 G 开头的记录）
SELECT session_id, user_id, target_user_id, target_nick_name, session_type
FROM chat_session
WHERE session_type = 1
  AND target_user_id IS NOT NULL
  AND target_user_id NOT LIKE 'G%';
