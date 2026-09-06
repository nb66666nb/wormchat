package com.meetchat.ai.llm;

import java.util.List;

/**
 * LLM客户端接口
 * 支持任意OpenAI兼容的API提供商
 */
public interface LlmClient {

    /**
     * 发送聊天请求
     * @param messages 消息列表（system/user/assistant/tool）
     * @return LLM响应
     */
    LlmResponse chat(List<LlmRequest.Message> messages);

    /**
     * 发送聊天请求（带自定义参数）
     * @param messages 消息列表
     * @param temperature 温度
     * @param maxTokens 最大token数
     * @return LLM响应
     */
    LlmResponse chat(List<LlmRequest.Message> messages, Double temperature, Integer maxTokens);

    /**
     * 检查LLM服务是否可用
     */
    boolean isAvailable();
}