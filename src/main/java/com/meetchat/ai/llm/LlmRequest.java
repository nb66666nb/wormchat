package com.meetchat.ai.llm;

import java.util.List;

/**
 * LLM聊天请求
 * 兼容OpenAI Chat Completions API格式
 */
public class LlmRequest {

    private String model;
    private List<Message> messages;
    private Double temperature;
    private Integer maxTokens;
    private Boolean stream;

    public static class Message {
        private String role;
        private String content;

        public Message() {}

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static LlmRequest create(String model, List<Message> messages, Double temperature, Integer maxTokens) {
        LlmRequest request = new LlmRequest();
        request.setModel(model);
        request.setMessages(messages);
        request.setTemperature(temperature);
        request.setMaxTokens(maxTokens);
        request.setStream(false);
        return request;
    }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Boolean getStream() { return stream; }
    public void setStream(Boolean stream) { this.stream = stream; }
}