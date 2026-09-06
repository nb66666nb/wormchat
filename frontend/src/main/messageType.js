export const MessageType = {
  CREATE_MEETING: 1,
  MEETING_KICK: 2,
  MEETING_JOIN: 10,
  MEETING_LEAVE: 11,
  MEETING_END: 12,
  // WebRTC 信令
  RTC_OFFER: 20,
  RTC_ANSWER: 21,
  RTC_ICE_CANDIDATE: 22,
  RTC_HANGUP: 23,
  CHAT_MESSAGE: 30,
  // 文件消息
  FILE_MESSAGE: 32,
  IMAGE_MESSAGE: 33,
  VIDEO_MESSAGE: 34,
  AUDIO_MESSAGE: 35,
  // 会话通知（后端推送会话创建/更新）
  SESSION_MESSAGE: 36,
  // 消息送达确认（C→S，接收方确认收到IM消息）
  MESSAGE_ACK: 99,
  // ========== 群聊系统事件 ==========
  GROUP_MEMBER_JOIN: 40,       // 成员加入群聊
  GROUP_MEMBER_LEAVE: 41,      // 成员主动退出群聊
  GROUP_MEMBER_KICKED: 42,     // 成员被踢出群聊
  GROUP_ROLE_CHANGE: 43,       // 成员角色变更（设为管理员/取消管理员）
  GROUP_DISSOLVED: 44,         // 群聊已解散
  GROUP_SETTINGS_UPDATE: 45,    // 群聊设置更新
  FORCE_OFFLINE: 47,            // 强制下线（管理员→指定用户）
  MESSAGE_RECALL: 48            // 消息撤回通知（S→C，后端推送撤回指令）
}

export const MessageTypeText = {
  [MessageType.CREATE_MEETING]: '创建通话',
  [MessageType.MEETING_KICK]: '踢出通话',
  [MessageType.MEETING_JOIN]: '加入通话',
  [MessageType.MEETING_LEAVE]: '离开通话',
  [MessageType.MEETING_END]: '通话结束',
  [MessageType.RTC_OFFER]: 'WebRTC Offer',
  [MessageType.RTC_ANSWER]: 'WebRTC Answer',
  [MessageType.RTC_ICE_CANDIDATE]: 'ICE Candidate',
  [MessageType.RTC_HANGUP]: '挂断',
  [MessageType.CHAT_MESSAGE]: '文字消息',
  [MessageType.FILE_MESSAGE]: '文件消息',
  [MessageType.IMAGE_MESSAGE]: '图片消息',
  [MessageType.VIDEO_MESSAGE]: '视频消息',
  [MessageType.AUDIO_MESSAGE]: '音频消息',
  [MessageType.SESSION_MESSAGE]: '会话通知',
  [MessageType.MESSAGE_ACK]: '消息送达确认',
  [MessageType.GROUP_MEMBER_JOIN]: '成员加入',
  [MessageType.GROUP_MEMBER_LEAVE]: '成员退出',
  [MessageType.GROUP_MEMBER_KICKED]: '成员被踢出',
  [MessageType.GROUP_ROLE_CHANGE]: '角色变更',
  [MessageType.GROUP_DISSOLVED]: '群聊解散',
  [MessageType.GROUP_SETTINGS_UPDATE]: '群设置更新',
  [MessageType.FORCE_OFFLINE]: '强制下线',
  [MessageType.MESSAGE_RECALL]: '消息撤回'
}

/** 文件消息类型集合 */
export const FILE_MESSAGE_TYPES = [
  MessageType.FILE_MESSAGE,
  MessageType.IMAGE_MESSAGE,
  MessageType.VIDEO_MESSAGE,
  MessageType.AUDIO_MESSAGE
]

export const isSystemMessage = (type) => {
  return type !== MessageType.CHAT_MESSAGE
    && type !== MessageType.FILE_MESSAGE
    && type !== MessageType.IMAGE_MESSAGE
    && type !== MessageType.VIDEO_MESSAGE
    && type !== MessageType.AUDIO_MESSAGE
    && (type < 20 || type >= 36)
}

/** 群聊角色枚举 */
export const GroupRoleEnum = {
  OWNER: 'OWNER',       // 群主
  ADMIN: 'ADMIN',       // 管理员
  MEMBER: 'MEMBER'      // 普通成员
}

/** 群聊状态枚举 */
export const GroupStatusEnum = {
  ACTIVE: 0,           // 正常
  DISSOLVED: 1         // 已解散
}

export const isChatMessage = (type) => {
  return type === MessageType.CHAT_MESSAGE
}

export const isWebRtcSignal = (type) => {
  return type >= 20 && type <= 23
}

/** 消息通道类型：区分IM消息、会议消息、会话系统事件，前端据此走不同逻辑 */
export const MessageTypeIm = {
  IM: 0,         // IM即时通讯消息 → chat_im_message表
  MEETING: 1,    // 会议消息 → chat_message表
  SESSION: 2     // 会话系统事件 → 只操作 chat_session 表
}

export const MessageTypeImText = {
  [MessageTypeIm.IM]: 'IM消息',
  [MessageTypeIm.MEETING]: '通话消息',
  [MessageTypeIm.SESSION]: '会话系统事件'
}
