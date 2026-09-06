package com.meetchat.webSocket.netty;

import com.alibaba.fastjson.JSON;
import com.meetchat.config.RabbitMqConfig;
import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.webSocket.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * WebSocket消息集群广播处理器
 *
 * 使用RabbitMQ Fanout Exchange实现跨服务节点广播：
 * - 发送消息时，publish到ws.broadcast.exchange
 * - 每个服务节点监听自己的独立队列（exclusive + autoDelete，节点下线自动清理）
 * - 收到广播后，由本地SessionManager判断目标用户是否在本节点，在则推送
 *
 * 单机部署可将 ws.broadcast.enabled 置为 false，跳过MQ直推本地，降低延迟。
 */
@Component("messageHandler")
public class MessageHandler<T> {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    /** 集群广播开关：false时直接本地推送（单机模式省一次MQ往返） */
    @Value("${ws.broadcast.enabled:true}")
    private boolean broadcastEnabled;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private SessionManager sessionManager;

    /**
     * 发送WebSocket消息：优先集群广播（各节点尝试本地投递），失败或关闭广播时降级本地直推
     * @param sendDto 待发送消息DTO
     */
    public void sendMessage(MessageSendDto sendDto) {
        if (!broadcastEnabled) {
            sessionManager.sendMessage(sendDto);
            return;
        }
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.WS_BROADCAST_EXCHANGE,
                    "", // Fanout Exchange不需要routingKey
                    sendDto
            );
            logger.debug("WebSocket广播消息已发送到RabbitMQ: {}", JSON.toJSONString(sendDto));
        } catch (Exception e) {
            logger.error("WebSocket广播消息发送失败，降级本地推送", e);
            // 降级：直接尝试本地推送
            sessionManager.sendMessage(sendDto);
        }
    }

    /**
     * 监听本节点的WebSocket广播队列
     * 收到消息后尝试本地SessionManager推送
     * @param sendDto 广播消息DTO
     */
    @RabbitListener(queues = "#{wsBroadcastQueue.name}", containerFactory = "rabbitListenerContainerFactory")
    public void onBroadcastMessage(MessageSendDto sendDto) {
        if (sendDto == null) {
            return;
        }
        logger.info("收到RabbitMQ广播消息: {}", JSON.toJSONString(sendDto));
        sessionManager.sendMessage(sendDto);
    }
}
