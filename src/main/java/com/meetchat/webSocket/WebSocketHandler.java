package com.meetchat.webSocket;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.contants.Contants;
import com.meetchat.entity.enums.UserStatusEnum;
import com.meetchat.entity.po.UserInfo;
import com.meetchat.exception.BusinessException;
import com.meetchat.mappers.UserInfoMapper;
import com.meetchat.redis.RedisComponet;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 业务处理器
 *
 * 处理流程：
 *   1. 客户端通过 ws://host:6061/websocket?token=xxx 发起 WebSocket 连接
 *   2. userEventTriggered 收到 HandshakeComplete 事件
 *   3. 从 requestUri 中解析 token 参数，校验有效性
 *   4. 校验通过 → 绑定 Channel 与用户（SessionManager）
 *   5. 校验失败 → 关闭连接
 *
 * 后续在此处实现 WebRTC 信令转发（offer / answer / ice-candidate）
 *
 * @Sharable 无状态，Spring 单例，安全共享
 */
@Component
@ChannelHandler.Sharable
public class WebSocketHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /** 断开连接后延迟清理 Redis 会话的秒数（容忍网络抖动导致的短暂重连） */
    private static final long TOKEN_CLEAR_DELAY_SECONDS = 10L;

    /** 用于延迟清理任务的独立线程池 */
    private static final ScheduledExecutorService LOGIN_GUARD_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ws-login-guard");
                t.setDaemon(true);
                return t;
            });

    @Resource
    private SessionManager sessionManager;

   @Resource
   private RedisComponet redisComponet;
   @Resource
   private WebSocketMessageHandler webSocketMessageHandler;
   @Resource
   private UserInfoMapper<UserInfo, Object> userInfoMapper;

    /**
     * 连接建立完成（TCP 连接，非 WebSocket 握手）
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("新连接接入：{}", ctx.channel().remoteAddress());
    }

    /**
     * 连接断开（客户端主动关闭 / 心跳超时 / 认证失败关闭）
     * 清理 SessionManager 中的映射关系
     *
     * 处理"用户直接关闭窗口（不走 logout）"的场景：
     * 断线后不立即删除 Redis token，而是延迟一段时间再清理，以避免网络抖动导致的
     * 短暂断开被误判为用户已离开。延迟期间若用户重新建立连接则取消清理。
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String userId = sessionManager.getUserId(ctx.channel());
        sessionManager.unbind(ctx.channel());
        if (userId != null) {
            scheduleClearRedisToken(userId);
        }
    }

    /**
     * 延迟清理 Redis 中的 token。
     * 由于 Netty 的线程模型，这里通过独立线程池执行延迟任务。
     * 延迟结束后再次确认该用户确实不在线，才删除 Redis token，
     * 从而允许用户重新登录（解决"关窗口后 token 残留导致无法登录"的问题）。
     */
    private void scheduleClearRedisToken(String userId) {
        LOGIN_GUARD_EXECUTOR.schedule(() -> {
            try {
                // 若该用户已重新建立连接（仍在线上），则取消清理
                if (sessionManager.isOnline(userId)) {
                    return;
                }
                TokenUserInfoDto tokenUserInfo = redisComponet.getTokenUserInfoByUserId(userId);
                if (tokenUserInfo != null && tokenUserInfo.getToken() != null) {
                    redisComponet.delRedis(Contants.TOKEN_KEY_REDIS + tokenUserInfo.getToken());
                    redisComponet.delRedis(Contants.USER_TOKEN_KEY_REDIS + userId);
                    logger.info("用户 {} 断开连接后未重连，已清理其 Redis 会话，允许重新登录", userId);
                }
            } catch (Exception e) {
                logger.error("延迟清理用户 Redis 会话失败：userId={}", userId, e);
            }
        }, TOKEN_CLEAR_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 收到 WebSocket 文本帧消息
     * @param frame TextWebSocketFrame 文本帧
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame frame) throws Exception {
        String message = frame.text();
       // logger.debug("收到消息：来自 userId={}，内容：{}",
               // sessionManager.getUserId(ctx.channel()),message);
        // 分发 WebRTC 信令消息（offer/answer/ice-candidate/hangup/chat 等）
        webSocketMessageHandler.dispatch(ctx.channel(), message);
    }

    /**
     * 捕获 WebSocket 握手完成事件
     * WebSocketServerProtocolHandler 握手成功后会触发 HandshakeComplete 事件
     * 在此完成 Token 认证
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            WebSocketServerProtocolHandler.HandshakeComplete handshake =
                    (WebSocketServerProtocolHandler.HandshakeComplete) evt;

            String uriString = handshake.requestUri();
            logger.info("WebSocket 握手成功：{}，请求URI：{}", ctx.channel().remoteAddress(), uriString);

            // 从 URI 中解析 token 参数
            String token = parseTokenFromUri(uriString);
            if (token == null ) {
                // 认证失败，关闭连接
                logger.warn("WebSocket 认证失败，关闭连接：{}", ctx.channel().remoteAddress());
                ctx.channel().close();
                return;
            }

            authenticate(ctx, token);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 从请求 URI 中解析 token 参数
     * 例如：ws://host:6061/websocket?token=eyJhbGciOi...
     *
     * @param uriString 请求 URI 字符串
     * @return token 值，不存在返回 null
     */
    private String parseTokenFromUri(String uriString) {
        try {
            URI uri = new URI(uriString);
            String token = uri.getQuery();  // 可能直接拿到 token 值，也可能拿到整个 query string
            if (token != null && !token.contains("=")) {
                // URI 格式为 ?tokenValue（无参数名，直接是 token 值）
                return token;
            }
            // 标准 query 参数格式：token=xxx&other=yyy
            if (token != null) {
                String[] pairs = token.split("&");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2 && "token".equalsIgnoreCase(kv[0])) {
                        return kv[1];
                    }
                }
            }
            return null;
        } catch (URISyntaxException e) {
            logger.error("解析 WebSocket URI 失败：{}", uriString, e);
            return null;
        }
    }

    /**
     * 校验 Token 并绑定 Channel 与用户
     *
     * @param ctx   ChannelHandlerContext
     * @param token JWT Token
     * @return true=认证成功，false=认证失败
     */
    private boolean authenticate(ChannelHandlerContext ctx, String token) {
        // 1. Token 为空
        if (token == null || token.trim().isEmpty()) {
            logger.warn("Token 为空");
            return false;
        }

        // 2. Token 格式校验 + 过期检查


        // 3. 检查 Token 是否在黑名单中（退出登录后 Token 失效）


        // 4. 解析用户信息
        TokenUserInfoDto tokenUserInfoDto = redisComponet.getTokenUserInfoByToken(token);
        if(tokenUserInfoDto ==null){
            throw new BusinessException("建立ws连接时，获取tokenUserINO解析失败");
        }
        String userId =tokenUserInfoDto.getUserId();

        if (userId == null) {
            logger.warn("Token 中解析不出用户ID");
            return false;
        }

        // 4.1 校验用户状态（被管理员禁用的账号禁止建立 WebSocket 连接）
        UserInfo userInfo = userInfoMapper.selectByUserId(userId);
        if (userInfo == null || UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            logger.warn("用户不存在或已被禁用，拒绝WebSocket连接：{}", userId);
            ctx.channel().close();
            return false;
        }

        // 5. 将 Channel 与用户绑定
        sessionManager.bind(ctx.channel(), userId);
        logger.info("WebSocket 认证成功：userId={},  channel={}",
                userId,  ctx.channel().remoteAddress());

        return true;
    }

    /**
     * 业务异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("WebSocket 处理异常：{}，原因：{}",
                ctx.channel().remoteAddress(), cause.getMessage());
        ctx.channel().close();
    }
}