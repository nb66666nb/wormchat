package com.meetchat.webSocket;



import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 心跳处理器
 * 配合 IdleStateHandler 工作：
 *   当连接读空闲超过 60 秒（客户端没有发来任何消息），
 *   触发 IdleStateEvent.READER_IDLE，本类将主动关闭该连接。
 *
 * @Sharable 表示此 Handler 无状态，可被多个 Channel 共享（Spring 单例注入安全）
 */
@Component
@ChannelHandler.Sharable
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(HeartBeatHandler.class);

    /**
     * 捕获 IdleStateEvent：读空闲超时 → 关闭连接
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;

            if (event.state() == IdleState.READER_IDLE) {
                // 读空闲：客户端长时间没有发消息（包括心跳 ping），判定为掉线
                logger.info("心跳超时，关闭连接：{}", ctx.channel().remoteAddress());
                ctx.channel().close();
            }
        } else {
            // 其他事件继续向下传递
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 不在此处处理异常，交由 Pipeline 后续的 WebSocketHandler 统一处理
     * 这样避免异常被提前拦截，WebSocketHandler 可以在关闭前清理 SessionManager
     */
}