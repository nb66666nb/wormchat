package com.meetchat.webSocket;



import com.meetchat.entity.config.AppConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * Netty WebSocket 启动类
 * 以独立线程运行，随 Spring 容器启动/关闭
 * 主从 Reactor 模式：
 *   bossGroup   — 负责接收连接（1个线程足够）
 *   workerGroup — 负责 I/O 读写和业务处理
 */
@Component
public class NettyWebSocketStarter implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(NettyWebSocketStarter.class);

    /** bossGroup 线程数：专门接收连接，1个即可 */
    private static final int BOSS_THREADS = 1;

    /** workerGroup 线程数：0 表示 Netty 默认（CPU核数 * 2） */
    private static final int WORKER_THREADS = 0;

    /** WebSocket 端口，从配置文件读取 */
   @Resource
   private AppConfig appConfig;

    @Resource
    private HandlerInitializer handlerInitializer;

    /** Netty 事件循环组，shutdown 时需要优雅关闭 */
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    /**
     * 启动 Netty 服务器
     * 由 MeetChatApplication 在独立线程中调用
     */
    @Override
    public void run() {
        bossGroup = new NioEventLoopGroup(BOSS_THREADS);
        workerGroup = new NioEventLoopGroup(WORKER_THREADS);

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    // 使用 NIO 非阻塞 I/O
                    .channel(NioServerSocketChannel.class)
                    // 服务端接受连接队列大小
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 保持长连接
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // TCP 无延迟，减少延迟（WebRTC 信令对延迟敏感）
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 绑定 Pipeline 初始化器
                    .childHandler(handlerInitializer);

            // 绑定端口，同步等待成功
            ChannelFuture channelFuture = bootstrap.bind(appConfig.getWsPort()).sync();
            logger.info("===== Netty WebSocket 服务器启动成功，端口：{} =====", appConfig.getWsPort());

            // 阻塞等待服务器 Channel 关闭（正常情况下永久阻塞，直到 stop() 被调用）
            channelFuture.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            logger.warn("Netty 服务器被中断");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Netty 服务器启动失败", e);
        } finally {
            stop();
        }
    }

    /**
     * 优雅关闭 Netty
     * Spring 容器销毁时自动调用（@PreDestroy）
     */
    @PreDestroy
    public void stop() {
        logger.info("Netty WebSocket 服务器正在关闭...");
        if (bossGroup != null && !bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null && !workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully();
        }
        logger.info("Netty WebSocket 服务器已关闭");
    }
}