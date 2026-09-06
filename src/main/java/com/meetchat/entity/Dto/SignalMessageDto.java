package com.meetchat.entity.Dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;


@JsonIgnoreProperties(ignoreUnknown = true)
public class SignalMessageDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型，对应 MessageTypeEnum.type
     */
    private Integer messageType;

    /**
     * 会议号码（meetingNo），标识所在会议室
     */
    private String meetingNo;

    /**
     * 目标用户ID（WebRTC 点对点信令中转时使用）
     * offer/answer/ice-candidate/hangup 需要填写
     */
    private String targetUserId;

    /**
     * SDP 内容（offer 和 answer 时携带）
     */
    private String sdp;

    /**
     * ICE Candidate JSON 字符串（ice-candidate 时携带）
     * 客户端可直接传 RTCIceCandidate 的序列化字符串
     */
    private String candidate;

    /**
     * 文字消息内容（chat 时携带）
     */
    private String content;

    /**
     * 消息ID（MESSAGE_ACK 时携带，标识已送达的消息）
     */
    private Long messageId;

    // ==================== IM 上行消息（IM_UPSTREAM=50 时使用） ====================

    /**
     * IM上行消息：所属会话ID
     */
    private String sessionId;

    /**
     * IM上行消息：接收方用户ID（U/G/R 前缀，服务端据此推断私聊/群聊/机器人）
     */
    private String receiveUserId;

    /**
     * IM上行消息：前端生成的唯一消息ID（幂等去重键，服务端查重防重复入库）
     */
    private String messageOnlyId;

    /**
     * IM上行消息：内容类型（30文字/32文件/33图片/34视频/35音频/36表情，默认30文字）
     */
    private Integer imMessageType;

    // ==================== Getter / Setter ====================

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getReceiveUserId() {
        return receiveUserId;
    }

    public void setReceiveUserId(String receiveUserId) {
        this.receiveUserId = receiveUserId;
    }

    public String getMessageOnlyId() {
        return messageOnlyId;
    }

    public void setMessageOnlyId(String messageOnlyId) {
        this.messageOnlyId = messageOnlyId;
    }

    public Integer getImMessageType() {
        return imMessageType;
    }

    public void setImMessageType(Integer imMessageType) {
        this.imMessageType = imMessageType;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public String getMeetingNo() {
        return meetingNo;
    }

    public void setMeetingNo(String meetingNo) {
        this.meetingNo = meetingNo;
    }

    public String getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(String targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    public String getCandidate() {
        return candidate;
    }

    public void setCandidate(String candidate) {
        this.candidate = candidate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }
}