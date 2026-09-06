/**
 * 聊天会话实体 — 对齐后端 ChatSession.java
 * 新增字段：isTop（置顶）、deleted（软删除）
 */

export const SessionTypeEnum = {
  USER: 0,
  GROUP: 1,
  ROBOT: 2
}

export const BotCategoryEnum = {
  FUNCTIONAL: 'FUNCTIONAL',
  COMPANION: 'COMPANION',
  HYBRID: 'HYBRID'
}

/**
 * 从数据库行转换为驼峰对象
 */
export const rowToEntity = (row) => {
  if (!row) return null
  return {
    sessionId: row.session_id,
    userId: row.user_id,
    targetUserId: row.target_user_id,
    targetNickName: row.target_nick_name,
    sessionType: row.session_type,
    botCategory: row.bot_category,
    templateId: row.template_id,
    botPersonality: row.bot_personality,
    botAvatarPath: row.bot_avatar_path,
    botName: row.bot_name,
    botDescription: row.bot_description,
    botSystemPrompt: row.bot_system_prompt,
    botWelcomeMsg: row.bot_welcome_msg,
    lastMessage: row.last_message,
    lastMessageTime: row.last_message_time,
    isTop: row.is_top,
    unreadCount: row.unread_count || 0,
    deleted: row.deleted
  }
}

/**
 * 从驼峰对象转换为数据库参数
 */
export const entityToParams = (data) => {
  return {
    sessionId: data.sessionId || '',
    userId: data.userId || '',
    targetUserId: data.targetUserId || '',
    targetNickName: data.targetNickName || '',
    sessionType: data.sessionType ?? SessionTypeEnum.USER,
    botCategory: data.botCategory || '',
    templateId: data.templateId || '',
    botPersonality: data.botPersonality || '',
    botAvatarPath: data.botAvatarPath || '',
    botName: data.botName || '',
    botDescription: data.botDescription || '',
    botSystemPrompt: data.botSystemPrompt || '',
    botWelcomeMsg: data.botWelcomeMsg || '',
    lastMessage: data.lastMessage || '',
    lastMessageTime: data.lastMessageTime || 0,
    isTop: data.isTop ?? 0,
    unreadCount: data.unreadCount ?? 0,
    deleted: data.deleted ?? 0
  }
}
