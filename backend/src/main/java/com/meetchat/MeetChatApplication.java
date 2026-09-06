package com.meetchat;

import com.meetchat.webSocket.NettyWebSocketStarter;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.meetchat"})
@MapperScan("com.meetchat.mappers")
@EnableScheduling
public class MeetChatApplication {
    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(MeetChatApplication.class, args);

        // 从 Spring 容器中取 NettyWebSocketStarter，以独立线程启动
        // 使用 Spring 托管的 Bean，确保其内部依赖（@Resource）可以正常注入
        NettyWebSocketStarter nettyStarter = context.getBean(NettyWebSocketStarter.class);
        Thread nettyThread = new Thread(nettyStarter, "netty-websocket-thread");
        nettyThread.setDaemon(false); // 非守护线程，JVM 不会在 Spring 关闭时强制终止它
        nettyThread.start();
    }
}