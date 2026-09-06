package com.meetchat.webSocket;


import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.Dto.SignalMessageDto;
import com.meetchat.entity.enums.MessageTypeEnum;
import com.meetchat.entity.enums.MessageTypeImEnum;
import com.meetchat.entity.enums.UserContactTypeEnum;
import com.meetchat.service.ChatImMessageService;
import com.meetchat.utils.JsonUtils;
import com.meetchat.utils.StringTools;
import com.meetchat.webSocket.netty.MessageHandler;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket 信令消息分发器
 *
 * 负责解析客户端上行消息（SignalMessageDto），按 messageType 路由到具体处理逻辑：
 *
 *   HEARTBEAT         → 无需响应（心跳由 IdleStateHandler 维护）
 *   RTC_OFFER         → 中转给目标用户（经集群广播，支持目标用户在其它节点）
 *   RTC_ANSWER        → 中转给目标用户
 *   RTC_ICE_CANDIDATE → 中转给目标用户
 *   RTC_HANGUP        → 中转给目标用户
 *   CHAT_MESSAGE      → 广播到会议室所有成员
 *   IM_UPSTREAM       → IM消息WebSocket上行：服务端落库+分配雪花ID/seq后推送
 *   MESSAGE_ACK       → 消息送达确认
 *
 * 会议室加入/离开/结束的 channel 操作由 MeetingStartService 业务层调用
 * SessionManager.joinGroup / leaveGroup 完成，此处只做消息分发。
 */
@Component
public class WebSocketMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessageHandler.class);

    @Resource
    private SessionManager sessionManager;

    @Resource
    private ChatImMessageService chatImMessageService;

    @Resource
    private MessageHandler messageHandler;

    /**
     * 消息分发入口
     *
     * @param channel 发送该消息的客户端 Channel
     * @param rawJson 收到的原始 JSON 字符串
     */
    public void dispatch(Channel channel, String rawJson) {
        // 1. 解析上行消息
        SignalMessageDto signal;
        try {
            signal = JsonUtils.convertJson2Obj(rawJson, SignalMessageDto.class);
        } catch (Exception e) {
            //logger.warn("消息解析失败，忽略。channel={}，内容：{}", channel.remoteAddress(), rawJson);
            return;
        }

        if (signal == null || signal.getMessageType() == null) {
            logger.warn("消息类型为空，忽略。channel={}", channel.remoteAddress());
            return;
        }

        // 2. 获取发送者 userId
        String sendUserId = sessionManager.getUserId(channel);
        if (sendUserId == null) {
            logger.warn("未认证的连接发送消息，关闭。channel={}", channel.remoteAddress());
            channel.close();
            return;
        }

        MessageTypeEnum typeEnum = MessageTypeEnum.getByType(signal.getMessageType());
        if (typeEnum == null) {
            logger.warn("未知消息类型 {}，忽略。userId={}", signal.getMessageType(), sendUserId);
            return;
        }

        logger.debug("收到消息：userId={}，type={}，meetingNo={}",
                sendUserId, typeEnum.getDesc(), signal.getMeetingNo());

        // 3. 按类型分发
        switch (typeEnum) {
            case HEARTBEAT:
                handleHeartbeat(channel, sendUserId);
                break;
            case RTC_OFFER:
                handleRtcOffer(channel, sendUserId, signal);
                break;
            case RTC_ANSWER:
                handleRtcAnswer(channel, sendUserId, signal);
                break;
            case RTC_ICE_CANDIDATE:
                handleRtcIceCandidate(channel, sendUserId, signal);
                break;
            case RTC_HANGUP:
                handleRtcHangup(channel, sendUserId, signal);
                break;
            case CHAT_MESSAGE:
                handleChatMessage(channel, sendUserId, signal);
                break;
            case IM_UPSTREAM:
                handleImUpstream(channel, sendUserId, signal);
                break;
            case MESSAGE_ACK:
                handleMessageAck(channel, sendUserId, signal);
                break;
            default:
                logger.warn("未处理的消息类型 {}，忽略。userId={}", typeEnum, sendUserId);
        }
    }

    // ==================== 心跳 ====================

    /**
     * 心跳处理：客户端发送 ping，服务端不需要主动 pong（IdleStateHandler 已管理连接活性）
     * 如需 pong，可在此发回一条 HEARTBEAT 响应
     */
    private void handleHeartbeat(Channel channel, String sendUserId) {
        //logger.debug("心跳：userId={}", sendUserId);
        // 可选：回复 pong
        // MessageSendDto<Void> pong = buildServerMsg(MessageTypeEnum.HEARTBEAT, null, null, null);
        // channel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(pong)));
    }

    // ==================== WebRTC 信令中转 ====================

    /**
     * 中转 Offer：将 SDP offer 转发给目标用户
     * 场景：呼叫方 A 向被呼叫方 B 发起 offer
     */
    private void handleRtcOffer(Channel channel, String sendUserId, SignalMessageDto signal) {
        String targetUserId = signal.getTargetUserId();
        if (!checkTarget(targetUserId, sendUserId, "RTC_OFFER")) return;

        MessageSendDto<Map<String, Object>> msg = buildRtcSignal(
                MessageTypeEnum.RTC_OFFER, sendUserId, targetUserId, signal.getMeetingNo());
        Map<String, Object> body = new HashMap<>();
        body.put("sdp", signal.getSdp());
        msg.setMessageContent(body);

        // 经集群广播转发，支持目标用户连接在其它节点
        messageHandler.sendMessage(msg);
        logger.info("RTC Offer 中转：{} → {}", sendUserId, targetUserId);
    }

    /**
     * 中转 Answer：将 SDP answer 转发给 offer 发起方
     */
    private void handleRtcAnswer(Channel channel, String sendUserId, SignalMessageDto signal) {
        String targetUserId = signal.getTargetUserId();
        if (!checkTarget(targetUserId, sendUserId, "RTC_ANSWER")) return;

        MessageSendDto<Map<String, Object>> msg = buildRtcSignal(
                MessageTypeEnum.RTC_ANSWER, sendUserId, targetUserId, signal.getMeetingNo());
        Map<String, Object> body = new HashMap<>();
        body.put("sdp", signal.getSdp());
        msg.setMessageContent(body);

        // 经集群广播转发，支持目标用户连接在其它节点
        messageHandler.sendMessage(msg);
        logger.info("RTC Answer 中转：{} → {}", sendUserId, targetUserId);
    }

    /**
     * 中转 ICE Candidate：将候选地址转发给对端
     */
    private void handleRtcIceCandidate(Channel channel, String sendUserId, SignalMessageDto signal) {
        String targetUserId = signal.getTargetUserId();
        if (!checkTarget(targetUserId, sendUserId, "RTC_ICE_CANDIDATE")) return;

        MessageSendDto<Map<String, Object>> msg = buildRtcSignal(
                MessageTypeEnum.RTC_ICE_CANDIDATE, sendUserId, targetUserId, signal.getMeetingNo());
        Map<String, Object> body = new HashMap<>();
        body.put("candidate", signal.getCandidate());
        msg.setMessageContent(body);

        // 经集群广播转发，支持目标用户连接在其它节点
        messageHandler.sendMessage(msg);
        logger.debug("ICE Candidate 中转：{} → {}", sendUserId, targetUserId);
    }

    /**
     * 中转 Hangup：通知对端挂断
     */
    private void handleRtcHangup(Channel channel, String sendUserId, SignalMessageDto signal) {
        String targetUserId = signal.getTargetUserId();
        if (!checkTarget(targetUserId, sendUserId, "RTC_HANGUP")) return;

        MessageSendDto<Void> msg = buildRtcSignal(
                MessageTypeEnum.RTC_HANGUP, sendUserId, targetUserId, signal.getMeetingNo());
        // 经集群广播转发，支持目标用户连接在其它节点
        messageHandler.sendMessage(msg);
        logger.info("RTC Hangup 中转：{} → {}", sendUserId, targetUserId);
    }

    // ==================== 文字消息 ====================

    /**
     * 会议室文字消息：广播给会议室所有成员（包含发送者）
     */
    private void handleChatMessage(Channel channel, String sendUserId, SignalMessageDto signal) {
        String meetingNo = signal.getMeetingNo();
        if (!checkMeetingNo(meetingNo, sendUserId, "CHAT_MESSAGE")) return;

        MessageSendDto<Map<String, Object>> msg = new MessageSendDto<>();
        msg.setMessageType(MessageTypeEnum.CHAT_MESSAGE.getType());
        msg.setMessageSend2Type(UserContactTypeEnum.GROUP.getType());
        msg.setMessageTypeIm(MessageTypeImEnum.MEETING.getType());
        msg.setSendUserId(sendUserId);
        msg.setSendTime(System.currentTimeMillis());

        Map<String, Object> body = new HashMap<>();
        body.put("content", signal.getContent());
        body.put("meetingNo", meetingNo);
        msg.setMessageContent(body);
        // receiveUserId = 会议号，经集群广播由各节点投递给本地 ChannelGroup
        msg.setReceiveUserId(meetingNo);

        messageHandler.sendMessage(msg);
        logger.debug("文字消息广播：userId={} → 会议室 {}", sendUserId, meetingNo);
    }

    // ==================== IM 消息 WebSocket 上行 ====================

    /**
     * IM 消息 WebSocket 上行通道（替代/补充原 HTTP 发送接口）：
     *
     * 客户端经 WS 发送 IM_UPSTREAM 消息 → 服务端统一处理：
     *   1. 发送者身份取自 Channel 绑定的认证用户（不可伪造）
     *   2. messageOnlyId 幂等查重（网络重发/客户端重试安全）
     *   3. 服务端分配雪花 messageId + 会话内 sessionSeq（顺序性由服务端掌控）
     *   4. 落库后经 afterCommit 推送接收方 + 回声发送方（与 HTTP 路径复用同一套逻辑）
     *
     * 客户端通过 echo 消息中的 messageOnlyId 与本地消息关联，完成发送状态流转
     */
    private void handleImUpstream(Channel channel, String sendUserId, SignalMessageDto signal) {
        String sessionId = signal.getSessionId();
        String receiveUserId = signal.getReceiveUserId();
        String content = signal.getContent();

        if (StringTools.isEmpty(sessionId) || StringTools.isEmpty(receiveUserId)
                || StringTools.isEmpty(content)) {
            logger.warn("IM_UPSTREAM：参数不完整，忽略。userId={}, sessionId={}, receiveUserId={}",
                    sendUserId, sessionId, receiveUserId);
            return;
        }

        // 按接收方ID前缀推断会话类型（U私聊/G群聊/R机器人），服务端强校验
        UserContactTypeEnum contactType = UserContactTypeEnum.getByPrefix(receiveUserId);
        if (contactType == null) {
            logger.warn("IM_UPSTREAM：接收方ID非法，忽略。userId={}, receiveUserId={}", sendUserId, receiveUserId);
            return;
        }

        MessageSendDto<String> dto = new MessageSendDto<>();
        dto.setSessionId(sessionId);
        dto.setSendUserId(sendUserId);
        dto.setReceiveUserId(receiveUserId);
        dto.setMessageSend2Type(contactType.getType());
        dto.setMessageContent(content);
        dto.setMessageType(signal.getImMessageType() == null ? 30 : signal.getImMessageType());
        dto.setMessageOnlyId(signal.getMessageOnlyId());
        dto.setSendTime(System.currentTimeMillis());

        try {
            chatImMessageService.sendImMessage(dto);
            logger.info("IM上行消息已受理: userId={}, sessionId={}, messageOnlyId={}",
                    sendUserId, sessionId, signal.getMessageOnlyId());
        } catch (Exception e) {
            // 上行通道无同步响应，处理失败仅记录（客户端超时后按 messageOnlyId 幂等重试）
            logger.error("IM上行消息处理失败: userId={}, sessionId={}, messageOnlyId={}",
                    sendUserId, sessionId, signal.getMessageOnlyId(), e);
        }
    }

    // ==================== 消息送达确认 ====================

    /**
     * 处理消息送达确认（ACK）
     * 接收方收到IM消息后，通过WebSocket发送ACK，后端标记 delivery_status=1
     *
     * 用 messageOnlyId 作为关联键：雪花 messageId 前端 ACK 回传时会精度丢失，
     * 无法精确匹配后端登记的重推任务，导致重推无法取消。messageOnlyId 是字符串，不受影响。
     */
    private void handleMessageAck(Channel channel, String sendUserId, SignalMessageDto signal) {
        String messageOnlyId = signal.getMessageOnlyId();
        if (StringTools.isEmpty(messageOnlyId)) {
            logger.warn("MESSAGE_ACK: messageOnlyId 为空，忽略。userId={}", sendUserId);
            return;
        }
        chatImMessageService.markDelivered(messageOnlyId);
    }

    // ==================== 工具方法 ====================

    /**
     * 构建 WebRTC 点对点信令消息（发送给单个用户）
     */
    @SuppressWarnings("unchecked")
    private <T> MessageSendDto<T> buildRtcSignal(MessageTypeEnum type,
                                                 String sendUserId,
                                                 String targetUserId,
                                                 String meetingNo) {
        MessageSendDto<T> msg = new MessageSendDto<>();
        msg.setMessageType(type.getType());
        msg.setMessageSend2Type(UserContactTypeEnum.USER.getType());
        msg.setMessageTypeIm(MessageTypeImEnum.MEETING.getType());
        msg.setSendUserId(sendUserId);
        msg.setReceiveUserId(targetUserId);
        msg.setSendTime(System.currentTimeMillis());
        return msg;
    }

    /**
     * 校验目标用户ID是否合法
     */
    private boolean checkTarget(String targetUserId, String sendUserId, String action) {
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            logger.warn("{}：targetUserId 为空，忽略。sendUserId={}", action, sendUserId);
            return false;
        }
        if (targetUserId.equals(sendUserId)) {
            logger.warn("{}：不能给自己发信令，忽略。sendUserId={}", action, sendUserId);
            return false;
        }
        return true;
    }

    /**
     * 校验会议号码是否合法
     */
    private boolean checkMeetingNo(String meetingNo, String sendUserId, String action) {
        if (meetingNo == null || meetingNo.trim().isEmpty()) {
            logger.warn("{}：meetingNo 为空，忽略。sendUserId={}", action, sendUserId);
            return false;
        }
        return true;
    }
}