/**
 * 数据库统一入口
 */
export { initDatabase, closeDatabase, getDb } from './Database'

export * as chatSessionService from './service/ChatSessionService'
export * as chatImMessageService from './service/ChatImMessageService'
export * as chatMessageService from './service/ChatMessageService'
export * as groupService from './service/GroupService'

export { SessionTypeEnum, BotCategoryEnum } from './entity/ChatSession'
export { ImMessageTypeEnum, ImMessageSendTypeEnum, ImMessageStatusEnum } from './entity/ChatImMessage'
export { MeetingMessageTypeEnum, MeetingMessageStatusEnum } from './entity/ChatMessage'
