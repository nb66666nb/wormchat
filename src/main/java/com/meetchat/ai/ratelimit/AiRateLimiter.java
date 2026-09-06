package com.meetchat.ai.ratelimit;


import com.meetchat.redis.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流器 - 基于Redis的滑动窗口实现
 * 提供多维度的限流能力：
 * 1. 用户消息频率限制
 * 2. Agent并发执行限制
 * 3. LLM API全局QPS限制
 * 4. 用户画像更新频率限制
 */
@Component("aiRateLimiter")
public class AiRateLimiter {

    private static final Logger logger = LoggerFactory.getLogger(AiRateLimiter.class);

    @Resource
    private RedisUtils redisUtils;

    /** 用户消息频率限制（条/分钟） */
    @Value("${ratelimit.user.message-per-minute:20}")
    private int userMessagePerMinute;

    /** Agent并发执行限制（同一机器人同时处理的请求数） */
    @Value("${ratelimit.agent.concurrent:10}")
    private int agentConcurrent;

    /** LLM API全局QPS限制 */
    @Value("${ratelimit.llm.qps:50}")
    private int llmQps;

    /** 用户画像更新冷却时间（秒） */
    @Value("${ratelimit.profile.cooldown-seconds:60}")
    private int profileCooldownSeconds;

    /** LLM QPS计数器（本地内存，作为Redis的快速路径） */
    private final AtomicLong llmQpsCounter = new AtomicLong(0);
    private volatile long llmQpsWindowStart = System.currentTimeMillis();

    /**
     * 检查用户消息频率
     * @param userId 用户ID
     * @return true-允许，false-被限流
     */
    public boolean allowUserMessage(String userId) {
        if (userId == null) return true;
        String key = "rl:user:msg:" + userId;
        return checkRateLimit(key, userMessagePerMinute, 60);
    }

    /**
     * 检查Agent并发执行数
     * @param botId 机器人ID
     * @return true-允许，false-被限流
     */
    public boolean allowAgentExecution(String botId) {
        if (botId == null) return true;
        String key = "rl:agent:concurrent:" + botId;
        return checkConcurrent(key, agentConcurrent);
    }

    /**
     * 检查LLM API全局QPS
     * @return true-允许，false-被限流
     */
    public boolean allowLlmCall() {
        long now = System.currentTimeMillis();
        // 每秒重置计数
        if (now - llmQpsWindowStart >= 1000) {
            llmQpsWindowStart = now;
            llmQpsCounter.set(0);
        }
        long current = llmQpsCounter.incrementAndGet();
        return current <= llmQps;
    }

    /**
     * 检查用户画像更新冷却
     * @param userId 用户ID
     * @param botId 机器人ID
     * @return true-允许更新，false-冷却中
     */
    public boolean allowProfileUpdate(String userId, String botId) {
        if (userId == null || botId == null) return true;
        String key = "rl:profile:cooldown:" + userId + ":" + botId;
        return redisUtils.setnx(key, "1", profileCooldownSeconds);
    }

    /**
     * 释放Agent并发计数
     * @param botId 机器人ID
     */
    public void releaseAgentExecution(String botId) {
        if (botId == null) return;
        String key = "rl:agent:concurrent:" + botId;
        try {
            redisUtils.decr(key);
        } catch (Exception e) {
            logger.warn("释放Agent并发计数异常: botId={}", botId, e);
        }
    }

    /**
     * 基于Redis的滑动窗口限流
     */
    private boolean checkRateLimit(String key, int maxCount, int windowSeconds) {
        try {
            Long current = redisUtils.incr(key);
            if (current != null && current == 1L) {
                redisUtils.expire(key, windowSeconds);
            }
            return current == null || current <= maxCount;
        } catch (Exception e) {
            logger.warn("限流检查异常，默认放行: key={}", key, e);
            return true;
        }
    }

    /**
     * 并发数限制
     */
    private boolean checkConcurrent(String key, int maxConcurrent) {
        try {
            Long current = redisUtils.incr(key);
            if (current != null && current == 1L) {
                redisUtils.expire(key, 60);
            }
            if (current != null && current > maxConcurrent) {
                // 超限立即递减
                redisUtils.decr(key);
                return false;
            }
            return true;
        } catch (Exception e) {
            logger.warn("并发限流检查异常，默认放行: key={}", key, e);
            return true;
        }
    }
}
