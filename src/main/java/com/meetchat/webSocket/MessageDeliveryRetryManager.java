package com.meetchat.webSocket;

import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.webSocket.netty.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 消息送达重试管理器（ACK 超时重推 + 指数退避）
 *
 * 保障链路闭环：
 *   服务端推送 → 接收方回 MESSAGE_ACK → ack() 取消重试
 *   未收到 ACK → 首次延迟 initialDelay 后重推，之后按 指数退避（3s/6s/12s）重推
 *   超过最大重试次数后放弃 → 依赖客户端登录后的离线增量拉取兜底（推拉结合）
 *
 * 幂等性：重推使用同一个 messageId，客户端按 messageId/messageOnlyId 去重，重复推送无副作用。
 * 仅对私聊消息生效（群消息为广播，无逐人 ACK；机器人消息无 ACK）。
 */
@Component
public class MessageDeliveryRetryManager {

    private static final Logger logger = LoggerFactory.getLogger(MessageDeliveryRetryManager.class);

    @Resource
    private MessageHandler messageHandler;

    /** 首次重试延迟（毫秒），默认 3 秒 */
    @Value("${ws.retry.initial-delay-ms:3000}")
    private long initialDelayMs;

    /** 最大重推次数（不含首次推送），默认 3 次 */
    @Value("${ws.retry.max-attempts:3}")
    private int maxAttempts;

    /** 待 ACK 消息上限：防止 ACK 大面积丢失时任务无限堆积 */
    private static final int MAX_PENDING = 10000;

    /** messageOnlyId -> 待执行的下次重推任务（收到 ACK 时取消）
     *  用 messageOnlyId 而非 messageId 作 key：雪花 messageId 超出 JS 安全整数范围，
     *  前端 ACK 回传时会精度丢失，无法精确匹配。messageOnlyId 是字符串 UUID，不受影响。 */
    private final ConcurrentMap<String, ScheduledFuture<?>> pendingTasks = new ConcurrentHashMap<>();

    /** 重推线程池：2 个守护线程即可（重推是轻量网络写，且量级被 MAX_PENDING 封顶） */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new ThreadFactory() {
        private final AtomicInteger index = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "ws-delivery-retry-" + index.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * 登记一条待确认消息：事务提交并完成首次推送后调用
     *
     * @param pushDto 发给接收方的推送 DTO（重推时原样重发），内部携带 messageOnlyId
     */
    public void track(MessageSendDto<?> pushDto) {
        if (pushDto == null || pushDto.getMessageOnlyId() == null || pushDto.getMessageOnlyId().isEmpty()) {
            return;
        }
        String messageOnlyId = pushDto.getMessageOnlyId();
        if (pendingTasks.size() >= MAX_PENDING) {
            logger.warn("待ACK消息数达到上限 {}，放弃登记重试，依赖离线拉取兜底: messageOnlyId={}", MAX_PENDING, messageOnlyId);
            return;
        }
        scheduleRetry(messageOnlyId, pushDto, 1, initialDelayMs);
        logger.debug("登记ACK超时重推: messageOnlyId={}, messageId={}, attempt={}, delay={}ms",
                messageOnlyId, pushDto.getMessageId(), 1, initialDelayMs);
    }

    /**
     * 收到 MESSAGE_ACK 后取消重推
     */
    public void ack(String messageOnlyId) {
        if (messageOnlyId == null) {
            return;
        }
        ScheduledFuture<?> future = pendingTasks.remove(messageOnlyId);
        if (future != null) {
            future.cancel(false);
            logger.debug("收到ACK，取消重推: messageOnlyId={}", messageOnlyId);
        }
    }

    /**
     * 调度一次重推；执行时仍未 ACK 则经集群广播重推，并按指数退避调度下一次
     *
     * @param attempt 第几次重推（1 = 首次重推）
     * @param delayMs 距当前时间的延迟
     */
    private void scheduleRetry(final String messageOnlyId, final MessageSendDto<?> pushDto, final int attempt, final long delayMs) {
        ScheduledFuture<?> future = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    // 到点执行：若期间已 ACK，任务已被取消，不会走到这里
                    logger.info("消息未确认，第 {} 次重推: messageOnlyId={}, messageId={}", attempt, messageOnlyId, pushDto.getMessageId());
                    messageHandler.sendMessage(pushDto);
                } catch (Exception e) {
                    logger.error("消息重推异常: messageOnlyId={}", messageOnlyId, e);
                } finally {
                    // 无论成败，先移除本次任务再决定是否继续退避
                    pendingTasks.remove(messageOnlyId);
                    if (attempt < maxAttempts) {
                        // 指数退避：delay * 2
                        scheduleRetry(messageOnlyId, pushDto, attempt + 1, delayMs * 2);
                    } else {
                        logger.warn("消息重推达上限({}次)，停止重推，等待客户端离线拉取兜底: messageOnlyId={}",
                                maxAttempts, messageOnlyId);
                    }
                }
            }
        }, delayMs, TimeUnit.MILLISECONDS);
        pendingTasks.put(messageOnlyId, future);
    }

    /**
     * 优雅关闭
     */
    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
        for (Map.Entry<String, ScheduledFuture<?>> entry : pendingTasks.entrySet()) {
            entry.getValue().cancel(false);
        }
        pendingTasks.clear();
    }

    /**
     * 当前待 ACK 重推的消息数（监控用）
     */
    public int getPendingCount() {
        return pendingTasks.size();
    }
}
