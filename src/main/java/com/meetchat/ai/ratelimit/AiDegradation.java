package com.meetchat.ai.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * AI服务降级策略
 * 当LLM、Embedding、Milvus等依赖不可用时，提供优雅降级
 */
@Component("aiDegradation")
public class AiDegradation {

    private static final Logger logger = LoggerFactory.getLogger(AiDegradation.class);

    @Resource
    private AiRateLimiter rateLimiter;

    /**
     * AI服务统一降级回复
     * @param reason 降级原因
     * @return 降级回复内容
     */
    public String getDegradedReply(String reason) {
        logger.warn("AI服务降级: reason={}", reason);
        return "抱歉，AI服务暂时繁忙，请稍后再试。";
    }

    /**
     * 检查并处理限流
     * @param userId 用户ID
     * @return null-未限流，非null-限流时的回复内容
     */
    public String checkUserMessageRateLimit(String userId) {
        if (!rateLimiter.allowUserMessage(userId)) {
            logger.warn("用户消息被限流: userId={}", userId);
            return "您的消息发送过于频繁，请稍后再试。";
        }
        return null;
    }

    /**
     * 检查Agent并发
     * @param botId 机器人ID
     * @return null-未限流，非null-限流时的回复内容
     */
    public String checkAgentConcurrency(String botId) {
        if (!rateLimiter.allowAgentExecution(botId)) {
            logger.warn("Agent并发被限流: botId={}", botId);
            return "当前机器人正在处理大量请求，请稍后再试。";
        }
        return null;
    }

    /**
     * 检查LLM QPS
     * @return null-未限流，非null-限流时的回复内容
     */
    public String checkLlmQps() {
        if (!rateLimiter.allowLlmCall()) {
            logger.warn("LLM API QPS超限");
            return "AI服务正在高负载运行，请稍后再试。";
        }
        return null;
    }
}
