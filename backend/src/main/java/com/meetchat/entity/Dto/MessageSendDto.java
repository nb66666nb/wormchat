package com.meetchat.entity.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageSendDto<T> implements Serializable {
    private static final long serialVersionUID = -1045752033171142417L;
    private String sessionId;
    //消息发送类型: 0-私聊 1-群聊 2-机器人（会议/IM 通用）
    private Integer messageSend2Type;
    //消息ID
    private Long messageId;
    //发送人
    private String sendUserId;
    //发送人昵称
    private String sendUserNickName;
    //消息内容
    private T messageContent;
    private String receiveUserId;
    //消息类型
    private Integer messageType;
    //发送时间
    private Long sendTime;

    //消息状态 0:发送中  1:已发送 对于文件是异步上传用状态处理
    private Integer status;

    //消息通道类型: 0-IM消息 1-会议消息（前端据此存入不同数据库表）
    private Integer messageTypeIm;

    //会议ID（用于与当前用户的 currentMeetingId 校验）
    private String meetingId;

    //文件信息
    private Long fileSize;
    private String fileName;
    private String fileId;
    private String filePath;
    private String contentType;
    private Integer fileType;
    private String extendData;
    private String messageOnlyId;

    //会话内服务端单调递增序号（用于客户端顺序校验与空洞检测）
    private Long sessionSeq;

    public Long getSessionSeq() {
        return sessionSeq;
    }

    public void setSessionSeq(Long sessionSeq) {
        this.sessionSeq = sessionSeq;
    }

    public String getMessageOnlyId() {
        return messageOnlyId;
    }

    public void setMessageOnlyId(String messageOnlyId) {
        this.messageOnlyId = messageOnlyId;
    }

    public Integer getMessageSend2Type() {
        return messageSend2Type;
    }

    public void setMessageSend2Type(Integer messageSend2Type) {
        this.messageSend2Type = messageSend2Type;
    }

    public T getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(T messageContent) {
        this.messageContent = messageContent;
    }

    public String getReceiveUserId() {
        return receiveUserId;
    }

    public void setReceiveUserId(String receiveUserId) {
        this.receiveUserId = receiveUserId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }




    public String getSendUserId() {
        return sendUserId;
    }

    public void setSendUserId(String sendUserId) {
        this.sendUserId = sendUserId;
    }




    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public Long getSendTime() {
        return sendTime;
    }

    public void setSendTime(Long sendTime) {
        this.sendTime = sendTime;
    }

    public String getSendUserNickName() {
        return sendUserNickName;
    }

    public void setSendUserNickName(String sendUserNickName) {
        this.sendUserNickName = sendUserNickName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getFileType() {
        return fileType;
    }

    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getExtendData() {
        return extendData;
    }

    public void setExtendData(String extendData) {
        this.extendData = extendData;
    }

    public Integer getMessageTypeIm() {
        return messageTypeIm;
    }

    public void setMessageTypeIm(Integer messageTypeIm) {
        this.messageTypeIm = messageTypeIm;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}