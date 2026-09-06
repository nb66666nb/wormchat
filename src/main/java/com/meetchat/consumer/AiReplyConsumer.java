package com.meetchat.consumer;

import com.meetchat.entity.AiReplyMessage;
import com.meetchat.ai.service.AiChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * AI回复消息消费者 - 从RabbitMQ消费AI回复任务
 * 异步处理机器人对话，调用AiChatService生成AI回复
 */
@Component
public class AiReplyConsumer {

    private static final Logger logger = LoggerFactory.getLogger(AiReplyConsumer.class);

    @Resource
    private AiChatService aiChatService;

    /**
     * 监听AI回复队列
     * @param message RabbitMQ消息体（AiReplyMessage对象，由Jackson2JsonMessageConverter自动反序列化）
     */
    @RabbitListener(queues = "ai.reply.queue")
    public void handleAiReply(AiReplyMessage message) {
        try {
            logger.info("收到AI回复消息: {}", message);

            if (message == null || message.getSessionId() == null) {
                logger.warn("AI回复消息格式错误: {}", message);
                return;
            }

            // 调用AI服务处理机器人对话
            Long messageId = aiChatService.handleBotChat(
                    message.getSessionId(),
                    message.getUserId(),
                    message.getUserName(),
                    message.getUserMessage()
            );

            if (messageId != null) {
                logger.info("AI回复处理成功: sessionId={}, messageId={}",
                        message.getSessionId(), messageId);
            } else {
                logger.warn("AI回复处理失败: sessionId={}", message.getSessionId());
            }

        } catch (Exception e) {
            logger.error("AI回复消息处理异常: {}", message, e);
            // 异常会自动进入死信队列
            throw e;
        }
    }
}
