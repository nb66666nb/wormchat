package com.meetchat.webSocket;

import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.enums.MessageTypeEnum;
import com.meetchat.entity.enums.UserContactTypeEnum;
import com.meetchat.entity.po.GroupMember;
import com.meetchat.exception.BusinessException;
import com.meetchat.mappers.GroupMemberMapper;
import com.meetchat.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.meetchat.entity.enums.UserContactTypeEnum.GROUP;
import static com.meetchat.entity.enums.UserContactTypeEnum.USER;

/**
 * WebSocket 会话管理器
 *
 * 映射关系说明：
 *   - Channel ←→ userId：通过 Channel 自身的 AttributeKey（每个 Channel 的 Key 名称唯一 = channel.id()）
 *   - userId  → Channel：通过 USER_CONTEXT_MAP 全局索引（单点推送）
 *   - groupId → ChannelGroup：通过 GROUP_CONTEXT_MAP（会议室群发）
 
 */
@Component
public class SessionManager {

    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);

    /**
     * 群成员表 Mapper：用户上线时查询其加入的所有群
     */
    @Resource
    private GroupMemberMapper<GroupMember, Object> groupMemberMapper;

    /**
     * 用户上下文：userId → Channel
     * 用于通过 userId 反向查找 Channel（给指定用户发消息）
     */
    public static final ConcurrentMap<String, Channel> USER_CONTEXT_MAP = new ConcurrentHashMap<>();

    /**
     * 会议室上下文：groupId → ChannelGroup
     * 用于向会议室内所有成员广播消息
     */
    public static final ConcurrentMap<String, ChannelGroup> GROUP_CONTEXT_MAP = new ConcurrentHashMap<>();

    /**
     * 获取或创建当前 Channel 专属的唯一 AttributeKey
     * 每个 Channel 的 AttributeKey 名称使用 channel.id()，保证全局唯一
     *
     * @param channel Netty Channel
     * @return 该 Channel 专属的 AttributeKey
     */
    private static AttributeKey<String> getOrCreateAttrKey(Channel channel) {
        String channelId = channel.id().toString();
        if (AttributeKey.exists(channelId)) {
            return AttributeKey.valueOf(channelId);
        } else {
            return AttributeKey.newInstance(channelId);
        }
    }

    /**
     * 绑定 Channel 与用户
     * WebSocket 握手认证成功后调用
     *   1. 用该 Channel 唯一的 AttributeKey 将 userId 绑定到 Channel 本身
     *   2. 将 userId → Channel 写入全局索引
     *   3. 将 Channel 加入该用户所有群聊的 ChannelGroup（群消息广播依赖此注册）
     *
     * @param channel Netty Channel
     * @param userId  用户ID
     */
    public void bind(Channel channel, String userId) {
        // 同一用户已有连接，先踢掉旧连接（同一时间只允许一个设备在线）
        Channel oldChannel = USER_CONTEXT_MAP.get(userId);
        if (oldChannel != null && oldChannel.isActive()) {
            logger.info("用户 {} 已有连接，踢掉旧连接：{}", userId, oldChannel.remoteAddress());
            oldChannel.close();
        }

        // 用该 Channel 专属的唯一 AttributeKey 绑定 userId 到 Channel 本身
        AttributeKey<String> attrKey = getOrCreateAttrKey(channel);
        channel.attr(attrKey).set(userId);

        // 维护 userId → Channel 的全局索引（用于按 userId 查找 Channel）
        USER_CONTEXT_MAP.put(userId, channel);

        // 自动加入该用户所有群聊的 ChannelGroup（群消息 sendToGroup 广播的前提）
        joinUserGroups(channel, userId);

        logger.info("用户上线：userId={}, channelId={}, channel={}",
                userId, channel.id(), channel.remoteAddress());
    }

    /**
     * 将用户 Channel 加入其所有正常状态群聊的 ChannelGroup
     * 查询/加入失败不影响上线流程（离线拉取兜底）
     *
     * @param channel 用户 Channel
     * @param userId  用户ID
     */
    private void joinUserGroups(Channel channel, String userId) {
        try {
            List<String> groupIds = groupMemberMapper.selectActiveGroupIdsByUserId(userId);
            if (groupIds == null || groupIds.isEmpty()) {
                return;
            }
            for (String groupId : groupIds) {
                // ChannelGroup.add 幂等：已包含该 Channel 时返回 false，不抛异常
                ChannelGroup group = GROUP_CONTEXT_MAP.computeIfAbsent(groupId,
                        k -> new DefaultChannelGroup("group-" + k, GlobalEventExecutor.INSTANCE));
                group.add(channel);
            }
            logger.info("用户 {} 上线，已自动加入 {} 个群聊的ChannelGroup", userId, groupIds.size());
        } catch (Exception e) {
            logger.error("用户上线自动加入群组失败: userId={}", userId, e);
        }
    }

    /**
     * 解绑 Channel 与用户
     * 连接断开时调用
     *
     * @param channel Netty Channel
     */
    public void unbind(Channel channel) {
        // 从 Channel 的唯一 AttributeKey 中取出 userId
        AttributeKey<String> attrKey = getOrCreateAttrKey(channel);
        String userId = channel.attr(attrKey).getAndSet(null);
        if (userId != null) {
            // 从全局索引中移除（只有匹配当前 Channel 时才移除，防止误删新连接）
            USER_CONTEXT_MAP.remove(userId, channel);
            logger.info("用户下线：userId={}, channelId={}, channel={}",
                    userId, channel.id(), channel.remoteAddress());
        }
    }

    /**
     * 根据 Channel 获取用户ID
     * 直接从 Channel 的 AttributeKey 中读取（每个 Channel 的 Key 名称唯一）
     *
     * @param channel Netty Channel
     * @return 用户ID，未绑定返回 null
     */
    public String getUserId(Channel channel) {
        return channel.attr(getOrCreateAttrKey(channel)).get();
    }

    /**
     * 根据用户ID获取其 Channel
     *
     * @param userId 用户ID
     * @return Channel，用户不在线返回 null
     */
    public Channel getChannel(String userId) {
        return USER_CONTEXT_MAP.get(userId);
    }

    /**
     * 判断用户是否在线
     *
     * @param userId 用户ID
     * @return true=在线
     */
    public boolean isOnline(String userId) {
        Channel channel = USER_CONTEXT_MAP.get(userId);
        return channel != null && channel.isActive();
    }

    /**
     * 获取当前在线用户数量
     */
    public int getOnlineCount() {
        return USER_CONTEXT_MAP.size();
    }

    /**
     * 获取当前所有在线用户的ID集合
     */
    public java.util.Set<String> getOnlineUserIds() {
        return USER_CONTEXT_MAP.keySet();
    }

    // ==================== 会议室相关方法 ====================

    /**
     * 加入会议室：将用户的 Channel 添加到对应 groupId 的 ChannelGroup
     *
     * @param userId  用户ID
     * @param groupId 会议室ID（meetingNo）
     */
    public void joinGroup(String userId, String groupId) {
        Channel channel = USER_CONTEXT_MAP.get(userId);
        if (channel == null || !channel.isActive()) {
            logger.warn("加入会议室失败：用户不在线，userId={}", userId);
            return;
        }

        ChannelGroup group = GROUP_CONTEXT_MAP.computeIfAbsent(groupId,
                k -> new DefaultChannelGroup("group-" + k, GlobalEventExecutor.INSTANCE));
        if(group.contains(channel)) {
            throw new BusinessException("用户已经在会议室中");
        }
        group.add(channel);
        logger.info("用户 {} 加入会议室 {}", userId, groupId);
    }


    /**
     * 创建会议室：为指定 groupId 初始化一个空的 ChannelGroup
     * 会议创建时调用，确保 GROUP_CONTEXT_MAP 中存在该会议室的记录。
     * 若已存在同名会议室则直接返回（幂等操作，不会重复创建）。
     *
     * @param groupId 会议室ID（meetingNo）
     */
    public void createGroup(String groupId) {
        GROUP_CONTEXT_MAP.computeIfAbsent(groupId,
                k -> new DefaultChannelGroup("group-" + k, GlobalEventExecutor.INSTANCE));
        logger.info("会议室已创建（或已存在）：groupId={}", groupId);
    }
    /**
     * 离开会议室：将用户的 Channel 从对应 groupId 的 ChannelGroup 移除
     *
     * @param userId  用户ID
     * @param groupId 会议室ID
     */
    public void leaveGroup(String userId, String groupId) {
        ChannelGroup group = GROUP_CONTEXT_MAP.get(groupId);
        if (group == null) return;
        Channel channel = USER_CONTEXT_MAP.get(userId);
        if (channel != null && channel.isActive()) {
            group.remove(channel);
        }
    }

    public void sendMessage(MessageSendDto message) {
        String userOrGroupId = message.getReceiveUserId();
      if(UserContactTypeEnum.GROUP.getType().equals(message.getMessageSend2Type())){
          sendToGroup(userOrGroupId, message);
      }else{
          sendToUser(userOrGroupId, message);
      }
    }

    /**
     * 向指定会议室的所有成员发送消息（广播）
     *
     * @param groupId     会议室ID
     * @param messageDto  消息载体（会自动序列化为 JSON）
     */
    public void sendToGroup(String groupId, MessageSendDto messageDto) {
        ChannelGroup group = GROUP_CONTEXT_MAP.get(groupId);
        if (group == null ) {
            logger.warn("发送失败：会议室 {} 不存在或为空", groupId);
            return;
        }
        group.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageDto)));
        if(messageDto.getMessageType().equals(MessageTypeEnum.MEETING_END.getType())){
            dissolveGroup(groupId);
        }
    }
    /**
     * 给指定用户发送单点消息
     *
     * @param userId      用户ID
     * @param messageDto  消息载体
     */
    public void sendToUser(String userId, MessageSendDto messageDto) {

        Channel channel = USER_CONTEXT_MAP.get(userId);
        if (channel == null || !channel.isActive()) {
            logger.warn("发送消息失败：用户 {} 不在线", userId);
            return;
        }
        channel.writeAndFlush(new TextWebSocketFrame(JsonUtils.convertObj2Json(messageDto)));
    }

    /**
     * 获取指定会议室的成员数量
     *
     * @param groupId 会议室ID
     * @return 成员数，会议室不存在返回 0
     */
    public int getGroupMemberCount(String groupId) {
        ChannelGroup group = GROUP_CONTEXT_MAP.get(groupId);
        return group == null ? 0 : group.size();
    }


    /**
     * 解散会议室：清空 ChannelGroup 并从 GROUP_CONTEXT_MAP 移除
     * 会议结束时调用。注意：只移除 ChannelGroup，不关闭 Channel（用户仍保持 WebSocket 连接）
     *
     * @param groupId 会议室ID（meetingNo）
     */
    public void dissolveGroup(String groupId) {
        ChannelGroup group = GROUP_CONTEXT_MAP.remove(groupId);
        if (group != null) {
            group.clear();
            logger.info("会议室 {} 已解散，ChannelGroup 已清空", groupId);
        }
    }
    /**
     * 移除并关闭指定用户的所有连接
     *
     * @param userId 用户ID
     */
    public void kickUser(String userId) {
        Channel channel = USER_CONTEXT_MAP.remove(userId);
        if (channel != null) {
            channel.attr(getOrCreateAttrKey(channel)).set(null);
            channel.close();
            logger.info("踢出用户：userId={}", userId);
        }
    }
}