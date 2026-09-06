package com.meetchat.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 雪花算法 ID 生成器
 *
 * 替代 MySQL 自增主键作为分布式环境下的消息 ID 生成方案，结构（64 bit）：
 *
 *  1bit 符号位(固定0) | 41bit 时间戳(毫秒,可用约69年) | 10bit 机器ID(0-1023) | 12bit 序列号(单机单毫秒4096个)
 *
 * 特点：
 *  - 趋势递增：高位为时间戳，整体按时间有序，可兼作客户端排序键与增量拉取游标
 *  - 分布式：多节点部署时各节点持有不同 workerId，ID 全局不重复
 *  - 时钟回拨处理：检测到回拨时先自旋等待上次时间戳；超过容忍阈值(5ms)则抛异常拒绝发号，
 *    防止回拨期间发出重复 ID（工业级方案可扩展为备用 workerId / 报警降级）
 */
@Component("snowflakeIdGenerator")
public class SnowflakeIdGenerator {

    private static final Logger logger = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    /** 起始纪元：2024-01-01 00:00:00 UTC（单位：毫秒） */
    private static final long EPOCH = 1704067200000L;

    /** workerId 占用位数：10 bit，取值 0-1023 */
    private static final long WORKER_ID_BITS = 10L;

    /** 序列号占用位数：12 bit，单毫秒 4096 个 */
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = -1L ^ (-1L << WORKER_ID_BITS);

    private static final long SEQUENCE_MASK = -1L ^ (-1L << SEQUENCE_BITS);

    /** 时间戳左移位数 = workerId + sequence 位数 */
    private static final long TIMESTAMP_LEFT_SHIFT = WORKER_ID_BITS + SEQUENCE_BITS;

    /** workerId 左移位数 = sequence 位数 */
    private static final long WORKER_ID_LEFT_SHIFT = SEQUENCE_BITS;

    /** 时钟回拨容忍阈值（毫秒）：小于该值自旋等待，超过则抛异常 */
    private static final long MAX_BACKWARD_MS = 5L;

    /** 上次生成 ID 的时间戳 */
    private long lastTimestamp = -1L;

    /** 当前毫秒内的序列号 */
    private long sequence = 0L;

    private final long workerId;

    /**
     * workerId 来源：
     *  1. 优先使用配置 snowflake.worker-id（0-1023，集群部署时必须显式指定且各节点不同）
     *  2. 未配置(-1)时按「主机名+WS端口」哈希散列，单机/学习环境可直接使用
     */
    public SnowflakeIdGenerator(@Value("${snowflake.worker-id:-1}") long configuredWorkerId) {
        long id = configuredWorkerId;
        if (id < 0) {
            String seed = null;
            try {
                seed = java.net.InetAddress.getLocalHost().getHostName() + ":" + System.getProperty("ws.port", "6061");
            } catch (Exception ignore) {
                seed = "default";
            }
            // spread 后取模，落在 [0, 1023]
            id = (seed.hashCode() & 0x7fffffff) % (MAX_WORKER_ID + 1);
        }
        if (id < 0 || id > MAX_WORKER_ID) {
            throw new IllegalArgumentException("snowflake.worker-id 非法，合法范围 0-" + MAX_WORKER_ID);
        }
        this.workerId = id;
        logger.info("SnowflakeIdGenerator 初始化完成：workerId={}", this.workerId);
    }

    /**
     * 生成全局唯一、趋势递增的 64bit ID（线程安全）
     */
    public synchronized long nextId() {
        long currentTimestamp = System.currentTimeMillis();

        // 时钟回拨处理
        if (currentTimestamp < lastTimestamp) {
            long offset = lastTimestamp - currentTimestamp;
            if (offset <= MAX_BACKWARD_MS) {
                // 小幅回拨：自旋等待到上次时间戳
                try {
                    wait(offset + 1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("时钟回拨等待被中断", e);
                }
                currentTimestamp = System.currentTimeMillis();
                if (currentTimestamp < lastTimestamp) {
                    throw new IllegalStateException(String.format(
                            "时钟回拨超过容忍阈值，拒绝发号。last=%d, now=%d", lastTimestamp, currentTimestamp));
                }
            } else {
                throw new IllegalStateException(String.format(
                        "时钟回拨 %d ms，超过容忍阈值 %d ms，拒绝发号", offset, MAX_BACKWARD_MS));
            }
        }

        if (currentTimestamp == lastTimestamp) {
            // 同一毫秒内：序列号递增
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 当前毫秒序列耗尽，自旋到下一毫秒
                currentTimestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 新毫秒：序列号归零（不使用随机起点，保证严格趋势递增）
            sequence = 0L;
        }

        lastTimestamp = currentTimestamp;

        return ((currentTimestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
                | (workerId << WORKER_ID_LEFT_SHIFT)
                | sequence;
    }

    /**
     * 自旋等待到下一毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }

    public long getWorkerId() {
        return workerId;
    }
}
