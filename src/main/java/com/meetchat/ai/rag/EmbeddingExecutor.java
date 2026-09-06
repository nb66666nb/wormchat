package com.meetchat.ai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.*;

/**
 * Embedding异步向量化线程池
 * 用于将文本异步转为向量，避免阻塞文档导入主流程
 */
@Component("embeddingExecutor")
public class EmbeddingExecutor {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddingExecutor.class);

    /** 核心线程数 */
    private static final int CORE_POOL_SIZE = 5;
    /** 最大线程数 */
    private static final int MAX_POOL_SIZE = 10;
    /** 队列容量 */
    private static final int QUEUE_CAPACITY = 200;
    /** 空闲线程存活时间（秒） */
    private static final int KEEP_ALIVE_SECONDS = 60;

    private ThreadPoolExecutor executor;

    @PostConstruct
    public void init() {
        executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "embedding-worker-" + counter++);
                        t.setDaemon(true);
                        return t;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        logger.info("Embedding线程池初始化完成: core={}, max={}, queue={}",
                CORE_POOL_SIZE, MAX_POOL_SIZE, QUEUE_CAPACITY);
    }

    /**
     * 提交异步向量化任务
     */
    public Future<?> submit(Runnable task) {
        if (executor == null || executor.isShutdown()) {
            logger.warn("线程池未初始化或已关闭，任务将同步执行");
            task.run();
            return CompletableFuture.completedFuture(null);
        }
        return executor.submit(task);
    }

    /**
     * 获取当前活跃线程数
     */
    public int getActiveCount() {
        return executor != null ? executor.getActiveCount() : 0;
    }

    /**
     * 获取队列剩余容量
     */
    public int getQueueSize() {
        return executor != null ? executor.getQueue().size() : 0;
    }

    @PreDestroy
    public void shutdown() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            logger.info("Embedding线程池已关闭");
        }
    }
}
