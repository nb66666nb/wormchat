package com.meetchat.webSocket;



import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * Netty Channel Pipeline 初始化器
 * 每个新连接建立时，都会调用 initChannel() 来装配处理器链：
 *
 *  HTTP 编解码      → HttpServerCodec
 *  大文件支持       → ChunkedWriteHandler
 *  HTTP 报文聚合    → HttpObjectAggregator（WebSocket 握手必须）
 *  WS 压缩          → WebSocketServerCompressionHandler
 *  心跳检测         → IdleStateHandler + HeartBeatHandler
 *  WS 协议升级      → WebSocketServerProtocolHandler
 *  业务处理         → WebSocketHandler
 */
@Component
public class HandlerInitializer extends ChannelInitializer<SocketChannel> {

    /**
     * WebSocket 握手路径
     */
    private static final String WS_PATH = "/ws";

    /**
     * HTTP 报文最大聚合大小：64 KB（信令消息不会太大）
     */
    private static final int MAX_CONTENT_LENGTH = 64 * 1024;

    /**
     * 心跳超时时间（秒）
     * 读空闲超过此时间则判定客户端掉线
     */
    private static final int READER_IDLE_SECONDS = 60;

    @Resource
    private HeartBeatHandler heartBeatHandler;

    @Resource
    private WebSocketHandler webSocketHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();

        // ① HTTP 编解码器（WebSocket 基于 HTTP 握手升级）
        pipeline.addLast("httpCodec", new HttpServerCodec());

        // ② 支持大数据流写出（文件传输等场景）
        pipeline.addLast("chunkedWrite", new ChunkedWriteHandler());

        // ③ HTTP 报文聚合器：将多个 HttpContent 合并为一个 FullHttpRequest
        //    WebSocket 握手 upgrade 请求必须是完整的 HTTP 报文
        pipeline.addLast("httpAggregator", new HttpObjectAggregator(MAX_CONTENT_LENGTH));

        // ④ WebSocket 压缩扩展（permessage-deflate），减少带宽
        pipeline.addLast("wsCompressor", new WebSocketServerCompressionHandler());

        // ⑤ 心跳空闲检测
        //    readerIdleTime=60s：60秒内未收到任何消息则触发 READER_IDLE 事件
        //    writerIdleTime=0：不检测写空闲
        //    allIdleTime=0：不检测读写双空闲
        pipeline.addLast("idleState",
                new IdleStateHandler(READER_IDLE_SECONDS, 0, 0, TimeUnit.SECONDS));

        // ⑥ 心跳业务处理器（处理 IdleStateEvent）
        pipeline.addLast("heartBeat", heartBeatHandler);

        // ⑦ WebSocket 协议处理器：完成 HTTP→WebSocket 升级握手
        //    握手成功后，后续消息以 WebSocketFrame 形式传递
        pipeline.addLast("wsProtocol",
                new WebSocketServerProtocolHandler(WS_PATH, null, true,
                        MAX_CONTENT_LENGTH, false, true));

        // ⑧ WebSocket 业务处理器（信令消息处理）
        pipeline.addLast("wsHandler", webSocketHandler);
    }
}