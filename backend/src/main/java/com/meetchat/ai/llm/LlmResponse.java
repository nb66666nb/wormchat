package com.meetchat.ai.llm;

/**
 * LLM聊天响应
 * 兼容OpenAI Chat Completions API格式
 */
public class LlmResponse {

    private String id;
    private String model;
    private Choice[] choices;
    private Usage usage;

    public static class Choice {
        private Integer index;
        private LlmRequest.Message message;
        private String finishReason;

        public Integer getIndex() { return index; }
        public void setIndex(Integer index) { this.index = index; }
        public LlmRequest.Message getMessage() { return message; }
        public void setMessage(LlmRequest.Message message) { this.message = message; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
    }

    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;

        public Integer getPromptTokens() { return promptTokens; }
        public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
        public Integer getCompletionTokens() { return completionTokens; }
        public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
        public Integer getTotalTokens() { return totalTokens; }
        public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    }

    /**
     * 获取回复文本内容
     */
    public String getContent() {
        if (choices != null && choices.length > 0 && choices[0].getMessage() != null) {
            return choices[0].getMessage().getContent();
        }
        return "";
    }

    /**
     * 是否需要调用工具（finish_reason=tool_calls时）
     */
    public boolean isToolCall() {
        return choices != null && choices.length > 0 && "tool_calls".equals(choices[0].getFinishReason());
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Choice[] getChoices() { return choices; }
    public void setChoices(Choice[] choices) { this.choices = choices; }
    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }
}