package com.meetchat.entity;

import java.io.Serializable;

/**
 * AI回复消息DTO - 用于RabbitMQ异步消息传递
 */
public class AiReplyMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private String sessionId;

    /** 用户ID */
    private String userId;

    /** 用户名 */
    private String userName;

    /** 用户消息内容 */
    private String userMessage;

    public AiReplyMessage() {
    }

    public AiReplyMessage(String sessionId, String userId, String userName, String userMessage) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.userName = userName;
        this.userMessage = userMessage;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    @Override
    public String toString() {
        return "AiReplyMessage{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", userMessage='" + userMessage + '\'' +
                '}';
    }
}
