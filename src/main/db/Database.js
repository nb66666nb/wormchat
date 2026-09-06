/**
 * Database.js — SQLite 数据库定义（主进程）
 * 使用 better-sqlite3，数据库文件存储在 meetchat-localFiles 目录下
 */
import Database from 'better-sqlite3'
import { app } from 'electron'
import path from 'path'
import fs from 'fs'

let db = null

/** 数据库文件目录 */
const DB_DIR = () => {
  const dir = path.join(app.getPath('userData'), 'meetchat-localFiles')
  if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true })
  return dir
}

/** 数据库文件路径 */
const DB_PATH = () => path.join(DB_DIR(), 'meetchat-chat.db')

/**
 * 初始化数据库
 * 先检查本地是否已存在数据库文件，已存在则直接打开使用，否则创建并建表
 * @returns {boolean}
 */
export const initDatabase = () => {
  if (db) return true

  const dbPath = DB_PATH()
  const dbExists = fs.existsSync(dbPath)

  try {
    db = new Database(dbPath)
    // 开启WAL模式，提升并发读写性能
    db.pragma('journal_mode = WAL')
    // 外键约束
    db.pragma('foreign_keys = ON')

    if (dbExists) {
      console.log('[DB] 本地数据库已存在，直接打开:', dbPath)
      runMigrations()
    } else {
      createTables()
      console.log('[DB] 本地数据库不存在，已创建并建表:', dbPath)
    }

    return true
  } catch (e) {
    console.error('[DB] SQLite 数据库初始化失败:', e)
    return false
  }
}

/**
 * 数据库迁移：为已有表添加新字段
 */
const runMigrations = () => {
  const migrations = [
    {
      table: 'chat_im_message',
      column: 'message_only_id',
      type: 'TEXT DEFAULT \'\'',
      sql: 'ALTER TABLE chat_im_message ADD COLUMN message_only_id TEXT DEFAULT \'\''
    },
    {
      table: 'chat_im_message',
      column: 'backend_message_id',
      type: 'INTEGER DEFAULT 0',
      sql: 'ALTER TABLE chat_im_message ADD COLUMN backend_message_id INTEGER DEFAULT 0'
    },
    {
      table: 'chat_session',
      column: 'unread_count',
      type: 'INTEGER DEFAULT 0',
      sql: 'ALTER TABLE chat_session ADD COLUMN unread_count INTEGER DEFAULT 0'
    },
    {
      table: 'chat_im_message',
      column: 'recalled',
      type: 'INTEGER DEFAULT 0',
      sql: 'ALTER TABLE chat_im_message ADD COLUMN recalled INTEGER DEFAULT 0'
    },
    {
      table: 'chat_im_message',
      column: 'recall_time',
      type: 'INTEGER DEFAULT 0',
      sql: 'ALTER TABLE chat_im_message ADD COLUMN recall_time INTEGER DEFAULT 0'
    }
  ]

  for (const m of migrations) {
    try {
      const cols = db.prepare(`PRAGMA table_info(${m.table})`).all()
      const exists = cols.some(c => c.name === m.column)
      if (!exists) {
        db.exec(m.sql)
        console.log(`[DB] 迁移: ${m.table} + ${m.column}`)
      }
    } catch (e) {
      console.error(`[DB] 迁移失败 ${m.table}.${m.column}:`, e.message)
    }
  }

  // 补索引
  try {
    db.exec('CREATE INDEX IF NOT EXISTS idx_im_message_only_id ON chat_im_message (message_only_id)')
  } catch (_) {}
  try {
    db.exec('CREATE UNIQUE INDEX IF NOT EXISTS idx_im_backend_msg_id ON chat_im_message (backend_message_id)')
  } catch (_) {}

  // 会话表索引迁移：支持多用户存储同一会话（群聊场景）
  try {
    const hasSessionUserIdUnique = db.prepare(
      "SELECT COUNT(*) as cnt FROM sqlite_master WHERE type='index' AND name='uk_session_session_user'"
    ).get()
    if (!hasSessionUserIdUnique.cnt) {
      db.exec('DROP INDEX IF EXISTS uk_session_session_id')
      db.exec('CREATE UNIQUE INDEX IF NOT EXISTS uk_session_session_user ON chat_session (session_id, user_id)')
      console.log('[DB] 迁移: chat_session 索引 uk_session_session_id → uk_session_session_user')
    }
  } catch (e) {
    console.error('[DB] 会话表索引迁移失败:', e.message)
  }

  // 群聊表迁移：检测 group_info 表是否存在，不存在则建表
  try {
    const tables = db.prepare("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('group_info','group_member')").all()
    const tableNames = tables.map(t => t.name)
    if (!tableNames.includes('group_info')) {
      db.exec(`
        CREATE TABLE group_info (
          group_id          TEXT PRIMARY KEY,
          group_name        TEXT DEFAULT '',
          group_avatar      TEXT DEFAULT '',
          owner_user_id     TEXT DEFAULT '',
          invite_permission INTEGER DEFAULT 0,
          announcement      TEXT DEFAULT '',
          status            INTEGER DEFAULT 0,
          create_time       INTEGER DEFAULT 0,
          update_time       INTEGER DEFAULT 0
        );
        CREATE INDEX idx_group_info_owner ON group_info (owner_user_id);
        CREATE INDEX idx_group_info_status ON group_info (status);
      `)
      console.log('[DB] 迁移: group_info 表已创建')
    }
  } catch (e) {
    console.error('[DB] group_info 表迁移失败:', e.message)
  }
  try {
    const tables = db.prepare("SELECT name FROM sqlite_master WHERE type='table' AND name='group_member'").all()
    if (tables.length === 0) {
      db.exec(`
        CREATE TABLE group_member (
          id                INTEGER PRIMARY KEY AUTOINCREMENT,
          group_id          TEXT NOT NULL,
          user_id           TEXT NOT NULL,
          role              TEXT DEFAULT 'MEMBER',
          nickname_in_group TEXT DEFAULT '',
          join_time         INTEGER DEFAULT 0,
          status            INTEGER DEFAULT 0
        );
        CREATE UNIQUE INDEX uk_group_user ON group_member (group_id, user_id);
        CREATE INDEX idx_group_member_group ON group_member (group_id);
        CREATE INDEX idx_group_member_user ON group_member (user_id);
        CREATE INDEX idx_group_member_status ON group_member (group_id, status);
      `)
      console.log('[DB] 迁移: group_member 表已创建')
    }
  } catch (e) {
    console.error('[DB] group_member 表迁移失败:', e.message)
  }

  // chat_session 表结构迁移：将 session_id 从主键改为普通字段，
  // 新增 id 自增主键，使双向会话能共用同一个 session_id
  try {
    const cols = db.prepare('PRAGMA table_info(chat_session)').all()
    const hasId = cols.some(c => c.name === 'id')
    if (!hasId) {
      console.log('[DB] 迁移: chat_session 重建表结构（session_id 不再是主键）')
      db.exec(`
        CREATE TABLE chat_session_new (
          id                INTEGER PRIMARY KEY AUTOINCREMENT,
          session_id        TEXT    NOT NULL,
          user_id           TEXT    NOT NULL,
          target_user_id    TEXT    DEFAULT '',
          target_nick_name  TEXT    DEFAULT '',
          session_type      INTEGER DEFAULT 1,
          bot_category      TEXT    DEFAULT '',
          template_id       TEXT    DEFAULT '',
          bot_personality   TEXT    DEFAULT '',
          bot_avatar_path   TEXT    DEFAULT '',
          bot_name          TEXT    DEFAULT '',
          bot_description   TEXT    DEFAULT '',
          bot_system_prompt TEXT    DEFAULT '',
          bot_welcome_msg   TEXT    DEFAULT '',
          last_message      TEXT    DEFAULT '',
          last_message_time INTEGER DEFAULT 0,
          is_top            INTEGER DEFAULT 0,
          unread_count      INTEGER DEFAULT 0,
          deleted           INTEGER DEFAULT 0
        );
        INSERT INTO chat_session_new (session_id, user_id, target_user_id, target_nick_name,
          session_type, bot_category, template_id, bot_personality, bot_avatar_path, bot_name,
          bot_description, bot_system_prompt, bot_welcome_msg, last_message, last_message_time,
          is_top, unread_count, deleted)
        SELECT session_id, user_id, target_user_id, target_nick_name,
          session_type, bot_category, template_id, bot_personality, bot_avatar_path, bot_name,
          bot_description, bot_system_prompt, bot_welcome_msg, last_message, last_message_time,
          is_top, unread_count, deleted FROM chat_session;
        DROP TABLE chat_session;
        ALTER TABLE chat_session_new RENAME TO chat_session;
        CREATE INDEX idx_session_session_id ON chat_session (session_id);
        CREATE INDEX idx_session_user_id ON chat_session (user_id);
        CREATE INDEX idx_session_target_user_id ON chat_session (target_user_id);
        CREATE INDEX idx_session_user_type ON chat_session (user_id, session_type);
        CREATE INDEX idx_session_user_time ON chat_session (user_id, last_message_time);
        CREATE INDEX idx_session_user_deleted ON chat_session (user_id, deleted);
        CREATE INDEX idx_session_user_top_time ON chat_session (user_id, is_top, last_message_time);
        -- 唯一约束：同一用户对同一会话只有一条记录（支持多用户存储同一群聊会话）
        CREATE UNIQUE INDEX IF NOT EXISTS uk_session_session_user ON chat_session (session_id, user_id);
      `)
      console.log('[DB] 迁移完成: chat_session 表结构已更新')
    }
  } catch (e) {
    console.error('[DB] chat_session 表结构迁移失败:', e.message)
  }
}

/**
 * 建表
 */
const createTables = () => {
  db.exec(`
    CREATE TABLE IF NOT EXISTS chat_session (
      id                INTEGER PRIMARY KEY AUTOINCREMENT,
      session_id        TEXT    NOT NULL,
      user_id           TEXT    NOT NULL,
      target_user_id    TEXT    DEFAULT '',
      target_nick_name  TEXT    DEFAULT '',
      session_type      INTEGER DEFAULT 1,
      bot_category      TEXT    DEFAULT '',
      template_id       TEXT    DEFAULT '',
      bot_personality   TEXT    DEFAULT '',
      bot_avatar_path   TEXT    DEFAULT '',
      bot_name          TEXT    DEFAULT '',
      bot_description   TEXT    DEFAULT '',
      bot_system_prompt TEXT    DEFAULT '',
      bot_welcome_msg   TEXT    DEFAULT '',
      last_message      TEXT    DEFAULT '',
      last_message_time INTEGER DEFAULT 0,
      is_top            INTEGER DEFAULT 0,
      unread_count      INTEGER DEFAULT 0,
      deleted           INTEGER DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_session_session_id
      ON chat_session (session_id);
    -- 唯一约束：同一用户对同一会话只有一条记录（支持多用户存储同一群聊会话）
    CREATE UNIQUE INDEX IF NOT EXISTS uk_session_session_user
      ON chat_session (session_id, user_id);
    -- 唯一约束：同一用户对同一目标的会话只能有一条
    CREATE UNIQUE INDEX IF NOT EXISTS uk_session_user_target
      ON chat_session (user_id, target_user_id);
    CREATE INDEX IF NOT EXISTS idx_session_user_id
      ON chat_session (user_id);
    CREATE INDEX IF NOT EXISTS idx_session_target_user_id
      ON chat_session (target_user_id);
    CREATE INDEX IF NOT EXISTS idx_session_user_type
      ON chat_session (user_id, session_type);
    CREATE INDEX IF NOT EXISTS idx_session_user_time
      ON chat_session (user_id, last_message_time);
    CREATE INDEX IF NOT EXISTS idx_session_user_deleted
      ON chat_session (user_id, deleted);
    CREATE INDEX IF NOT EXISTS idx_session_user_top_time
      ON chat_session (user_id, is_top, last_message_time);

    CREATE TABLE IF NOT EXISTS chat_im_message (
      message_id          INTEGER PRIMARY KEY AUTOINCREMENT,
      backend_message_id  INTEGER DEFAULT 0,
      session_id          TEXT    NOT NULL,
      send_user_id        TEXT    NOT NULL,
      send_user_name      TEXT    DEFAULT '',
      send_user_avatar    TEXT    DEFAULT '',
      receive_user_id     TEXT    DEFAULT '',
      message_type        INTEGER DEFAULT 30,
      message_content     TEXT    DEFAULT '',
      file_name           TEXT    DEFAULT '',
      file_size           INTEGER DEFAULT 0,
      file_id             TEXT    DEFAULT '',
      file_path           TEXT    DEFAULT '',
      content_type        TEXT    DEFAULT '',
      file_type           INTEGER DEFAULT 0,
      message_send_type   INTEGER DEFAULT 0,
      message_only_id     TEXT    DEFAULT '',
      status              INTEGER DEFAULT 0,
      send_time           INTEGER DEFAULT 0,
      recalled            INTEGER DEFAULT 0,
      recall_time         INTEGER DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_im_session_time
      ON chat_im_message (session_id, send_time);
    CREATE INDEX IF NOT EXISTS idx_im_session_type
      ON chat_im_message (session_id, message_type);
    CREATE INDEX IF NOT EXISTS idx_im_session_status
      ON chat_im_message (session_id, status);
    CREATE INDEX IF NOT EXISTS idx_im_send_user_time
      ON chat_im_message (send_user_id, send_time);
    CREATE INDEX IF NOT EXISTS idx_im_message_only_id
      ON chat_im_message (message_only_id);
    CREATE UNIQUE INDEX IF NOT EXISTS idx_im_backend_msg_id
      ON chat_im_message (backend_message_id);

    CREATE TABLE IF NOT EXISTS chat_message (
      message_id        INTEGER PRIMARY KEY,
      meeting_id        TEXT    DEFAULT '',
      meeting_no        TEXT    NOT NULL,
      message_send_type INTEGER DEFAULT 0,
      send_user_id      TEXT    NOT NULL,
      send_user_nick_name TEXT  DEFAULT '',
      receive_user_id   TEXT    DEFAULT '',
      message_type      INTEGER DEFAULT 1,
      message_content   TEXT    DEFAULT '',
      file_size         INTEGER DEFAULT 0,
      file_name         TEXT    DEFAULT '',
      file_id           TEXT    DEFAULT '',
      file_path         TEXT    DEFAULT '',
      content_type      TEXT    DEFAULT '',
      file_type         INTEGER DEFAULT 0,
      extend_data       TEXT    DEFAULT '',
      status            INTEGER DEFAULT 0,
      send_time         INTEGER DEFAULT 0,
      create_time       INTEGER DEFAULT 0
    );

    CREATE INDEX IF NOT EXISTS idx_msg_meeting_time
      ON chat_message (meeting_no, send_time);
    CREATE INDEX IF NOT EXISTS idx_msg_meeting_type
      ON chat_message (meeting_no, message_type);
    CREATE INDEX IF NOT EXISTS idx_msg_send_user_time
      ON chat_message (send_user_id, send_time);

    CREATE TABLE IF NOT EXISTS group_info (
      group_id          TEXT PRIMARY KEY,
      group_name        TEXT DEFAULT '',
      group_avatar      TEXT DEFAULT '',
      owner_user_id     TEXT DEFAULT '',
      invite_permission INTEGER DEFAULT 0,
      announcement      TEXT DEFAULT '',
      status            INTEGER DEFAULT 0,
      create_time       INTEGER DEFAULT 0,
      update_time       INTEGER DEFAULT 0
    );
    CREATE INDEX IF NOT EXISTS idx_group_info_owner ON group_info (owner_user_id);
    CREATE INDEX IF NOT EXISTS idx_group_info_status ON group_info (status);

    CREATE TABLE IF NOT EXISTS group_member (
      id                INTEGER PRIMARY KEY AUTOINCREMENT,
      group_id          TEXT NOT NULL,
      user_id           TEXT NOT NULL,
      role              TEXT DEFAULT 'MEMBER',
      nickname_in_group TEXT DEFAULT '',
      join_time         INTEGER DEFAULT 0,
      status            INTEGER DEFAULT 0
    );
    CREATE UNIQUE INDEX IF NOT EXISTS uk_group_user ON group_member (group_id, user_id);
    CREATE INDEX IF NOT EXISTS idx_group_member_group ON group_member (group_id);
    CREATE INDEX IF NOT EXISTS idx_group_member_user ON group_member (user_id);
    CREATE INDEX IF NOT EXISTS idx_group_member_status ON group_member (group_id, status);
  `)

  // chat_session 去重迁移：清理重复会话记录，保留每组 (user_id, target_user_id) 中 id 最小的一条
  try {
    const duplicateCount = db.prepare(`
      SELECT COUNT(*) as cnt FROM (
        SELECT user_id, target_user_id, COUNT(*) as c
        FROM chat_session
        WHERE deleted = 0
        GROUP BY user_id, target_user_id
        HAVING c > 1
      )
    `).get().cnt
    if (duplicateCount > 0) {
      console.log(`[DB] 检测到 ${duplicateCount} 组重复会话，开始去重...`)
      db.prepare(`
        DELETE FROM chat_session
        WHERE id NOT IN (
          SELECT MIN(id) FROM chat_session
          WHERE deleted = 0
          GROUP BY user_id, target_user_id
        )
        AND deleted = 0
      `).run()
      console.log('[DB] 会话去重完成')
    }
  } catch (e) {
    console.error('[DB] 会话去重迁移失败:', e.message)
  }
}

/**
 * 获取数据库实例
 */
export const getDb = () => {
  if (!db) {
    initDatabase()
  }
  return db
}

/**
 * 关闭数据库连接
 */
export const closeDatabase = () => {
  if (db) {
    db.close()
    db = null
    console.log('[DB] SQLite 数据库已关闭')
  }
}
