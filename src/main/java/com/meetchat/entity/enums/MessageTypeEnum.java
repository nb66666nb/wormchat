package com.meetchat.entity.enums;

public enum MessageTypeEnum {

    // ==================== 心跳 ====================
    /** 客户端心跳 ping（C→S） */
    HEARTBEAT(0, "心跳"),

    CREATE_MEETING(1,"创建会议"),
    MEETING_KICK(2,"踢出会议"),

    // ==================== 会议室事件（S→C 通知） ====================
    /** 有人加入会议室（S→C 广播） */
    MEETING_JOIN(10, "加入会议室"),

    /** 有人离开会议室（S→C 广播） */
    MEETING_LEAVE(11, "离开会议室"),

    /** 会议被结束（S→C 广播） */
    MEETING_END(12, "会议结束"),


    // ==================== WebRTC 信令（C↔C，服务器中转） ====================
    /** SDP Offer（呼叫方发起） */
    RTC_OFFER(20, "WebRTC Offer"),

    /** SDP Answer（被呼叫方响应） */
    RTC_ANSWER(21, "WebRTC Answer"),

    /** ICE Candidate 候选地址 */
    RTC_ICE_CANDIDATE(22, "ICE Candidate"),

    /** 挂断 / 取消呼叫 */
    RTC_HANGUP(23, "挂断"),

    // ==================== 文字消息 ====================
    /** 会议室内文字消息（C→S→Group 广播） */
    CHAT_MESSAGE(30, "文字消息"),

    // ==================== 文件消息 ====================

    /** 文件上传完成 / 文件消息 */
    FILE_MESSAGE(32, "文件消息"),
    /** 图片消息 */
    IMAGE_MESSAGE(33, "图片消息"),
    /** 视频消息 */
    VIDEO_MESSAGE(34, "视频消息"),
    /** 音频消息 */
    AUDIO_MESSAGE(35, "音频消息"),
    /**会话消息*/
    SESSION_MESSAGE(36,"会话创建成功或发送消息更新的系统消息"),

    SESSION_LIST(37,"用户登录车成功后后端返回会话数据"),
    MESSAGE_LIST(38,"用户登录车成功后后端返回消息数据"),

    // ==================== 群聊系统事件 ====================
    /** 成员加入群聊 */
    GROUP_MEMBER_JOIN(40, "成员加入群聊"),
    /** 成员主动退出群聊 */
    GROUP_MEMBER_LEAVE(41, "成员退出群聊"),
    /** 成员被踢出群聊 */
    GROUP_MEMBER_KICKED(42, "成员被踢出群聊"),
    /** 成员角色变更（设为管理员/取消管理员） */
    GROUP_ROLE_CHANGE(43, "角色变更"),
    /** 群聊已解散 */
    GROUP_DISSOLVED(44, "群聊解散"),
    /** 群聊设置更新 */
    GROUP_SETTINGS_UPDATE(45, "群设置更新"),
    /** 群聊被管理员封禁（S→C 广播） */
    GROUP_BANNED(46, "群聊被封禁"),
    /** 强制下线（管理员→指定用户单点通知，前端收到后应登出） */
    FORCE_OFFLINE(47, "强制下线"),

    /** 消息撤回通知（S→C，发送方撤回消息后服务端推送，接收方据此把对应消息标记为已撤回） */
    MESSAGE_RECALL(48, "消息撤回"),

    /** IM上行消息（C→S，客户端经WebSocket上行发送IM消息，服务端落库+分配雪花ID/seq后推送） */
    IM_UPSTREAM(50, "IM上行消息"),

    /** 消息送达确认（C→S，接收方确认收到IM消息） */
    MESSAGE_ACK(99, "消息送达确认");

    private final int type;
    private final String desc;

    MessageTypeEnum(int type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public int getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static MessageTypeEnum getByType(Integer type) {
        if (type == null) return null;
        for (MessageTypeEnum e : values()) {
            if (e.type == type) return e;
        }
        return null;
    }
}