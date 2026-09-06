package com.meetchat.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置 - AI回复异步消息队列
 *
 * 架构：
 * - Topic Exchange: ai.reply.exchange（支持灵活路由）
 * - 持久化队列: ai.reply.queue（消息不丢失）
 * - 死信队列: ai.reply.dead.queue（处理失败消息）
 * - 消息TTL: 1小时（防止消息堆积）
 * - 最大长度: 10000（防止内存爆炸）
 */
@Configuration
public class RabbitMqConfig {

    // ==================== 常量定义 ====================
    public static final String AI_REPLY_EXCHANGE = "ai.reply.exchange";
    public static final String AI_REPLY_QUEUE = "ai.reply.queue";
    public static final String AI_REPLY_ROUTING_KEY = "ai.reply.routing";

    public static final String AI_REPLY_DEAD_EXCHANGE = "ai.reply.dead.exchange";
    public static final String AI_REPLY_DEAD_QUEUE = "ai.reply.dead.queue";
    public static final String AI_REPLY_DEAD_ROUTING_KEY = "ai.reply.dead.routing";

    // ==================== 队列参数 ====================
    /** 消息TTL（毫秒）：1小时 */
    private static final long MESSAGE_TTL_MS = 60 * 60 * 1000L;
    /** 队列最大长度 */
    private static final int QUEUE_MAX_LENGTH = 10000;
    /** 死信队列消息TTL（毫秒）：7天 */
    private static final long DEAD_MESSAGE_TTL_MS = 7 * 24 * 60 * 60 * 1000L;

    // ==================== Exchange & Queue ====================

    /**
     * Topic Exchange - 支持灵活的路由规则
     */
    @Bean("aiReplyExchange")
    public Exchange aiReplyExchange() {
        return ExchangeBuilder.topicExchange(AI_REPLY_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * AI回复队列 - 持久化 + TTL + 最大长度限制
     */
    @Bean("aiReplyQueue")
    public Queue aiReplyQueue() {
        return QueueBuilder.durable(AI_REPLY_QUEUE)
                .withArgument("x-dead-letter-exchange", AI_REPLY_DEAD_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", AI_REPLY_DEAD_ROUTING_KEY)
                .withArgument("x-message-ttl", MESSAGE_TTL_MS)
                .withArgument("x-max-length", QUEUE_MAX_LENGTH)
                .withArgument("x-overflow", "reject-publish")
                .build();
    }

    /**
     * 绑定Queue到Exchange
     */
    @Bean
    public Binding aiReplyBinding(@Qualifier("aiReplyQueue") Queue queue,
                                   @Qualifier("aiReplyExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(AI_REPLY_ROUTING_KEY).noargs();
    }

    // ==================== 死信队列 ====================

    /**
     * 死信Exchange
     */
    @Bean("aiReplyDeadExchange")
    public Exchange aiReplyDeadExchange() {
        return ExchangeBuilder.directExchange(AI_REPLY_DEAD_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * 死信Queue - 保留7天便于人工排查
     */
    @Bean("aiReplyDeadQueue")
    public Queue aiReplyDeadQueue() {
        return QueueBuilder.durable(AI_REPLY_DEAD_QUEUE)
                .withArgument("x-message-ttl", DEAD_MESSAGE_TTL_MS)
                .build();
    }

    /**
     * 死信绑定
     */
    @Bean
    public Binding aiReplyDeadBinding(@Qualifier("aiReplyDeadQueue") Queue queue,
                                       @Qualifier("aiReplyDeadExchange") Exchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(AI_REPLY_DEAD_ROUTING_KEY).noargs();
    }

    // ==================== WebSocket集群广播配置 ====================
    /** WebSocket广播Exchange */
    public static final String WS_BROADCAST_EXCHANGE = "ws.broadcast.exchange";
    /** WebSocket广播队列前缀（每个节点一个独立队列） */
    public static final String WS_BROADCAST_QUEUE_PREFIX = "ws.broadcast.queue.";

    /**
     * Fanout Exchange - WebSocket消息集群广播
     */
    @Bean("wsBroadcastExchange")
    public FanoutExchange wsBroadcastExchange() {
        return ExchangeBuilder.fanoutExchange(WS_BROADCAST_EXCHANGE)
                .durable(true)
                .build();
    }

    /**
     * WebSocket广播队列 - 每个服务实例独立
     * autoDelete: 消费者断开自动删除队列
     * exclusive: 仅当前连接可访问
     */
    @Bean("wsBroadcastQueue")
    public Queue wsBroadcastQueue() {
        String queueName = WS_BROADCAST_QUEUE_PREFIX + java.util.UUID.randomUUID();
        return QueueBuilder.nonDurable(queueName)
                .autoDelete()
                .exclusive()
                .withArgument("x-message-ttl", 5 * 60 * 1000L) // 5分钟TTL
                .withArgument("x-max-length", 1000)
                .build();
    }

    /**
     * WebSocket广播绑定 - Fanout无需RoutingKey
     */
    @Bean
    public Binding wsBroadcastBinding(@Qualifier("wsBroadcastQueue") Queue queue,
                                       @Qualifier("wsBroadcastExchange") FanoutExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange);
    }

    // ==================== 消息转换器 & Template ====================

    /**
     * JSON消息转换器 - 支持POJO自动序列化
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 自定义RabbitTemplate - 使用JSON序列化
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        template.setMandatory(true);
        return template;
    }
}
