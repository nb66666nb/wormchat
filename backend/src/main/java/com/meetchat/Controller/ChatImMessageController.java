package com.meetchat.Controller;


import com.meetchat.annotation.GlobalInterceptor;

import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.po.ChatImMessage;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.exception.BusinessException;
import com.meetchat.service.ChatImMessageService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chatImMessage")
public class ChatImMessageController extends ABaseController {

    @Resource
    private ChatImMessageService chatImMessageService;

    @RequestMapping("/sendImMessage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO sendImMessage(HttpServletRequest request, MessageSendDto<String> messageSendDto) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if(tokenUserInfoDto==null){
            throw  new BusinessException("token无效");

        }
        if(!tokenUserInfoDto.getUserId().equals(messageSendDto.getSendUserId())){
            throw  new BusinessException("参数有误，请联系后端管理员");
        }
        if(tokenUserInfoDto.getUserId().equals(messageSendDto.getReceiveUserId())){
            throw  new BusinessException("不能给自己发送消息");
        }
        Long messageId = chatImMessageService.sendImMessage(messageSendDto);
        Map<String, Object> data = new HashMap<>();
        data.put("messageId", messageId);
        data.put("messageOnlyId", messageSendDto.getMessageOnlyId());
        return getSuccessResponseVO(data);
    }

    /**
     * 拉取离线消息（增量同步）
     * 前端传 lastMessageId=0 或 null 时拉取最新分页
     * 前端传 lastMessageId=N 时拉取 messageId > N 的所有消息（增量）
     */
    @RequestMapping("/pullOffline")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO pullOffline(HttpServletRequest request, Long lastMessageId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            throw new BusinessException("token无效");
        }
        if (lastMessageId == null) {
            lastMessageId = 0L;
        }
        List<ChatImMessage> messages = chatImMessageService.pullOfflineMessages(
                tokenUserInfoDto.getUserId(), lastMessageId);
        return getSuccessResponseVO(messages);
    }

    /**
     * 撤回消息
     * 校验：仅发送者本人可撤回、发送时间在 2 分钟内（service 层强制）
     * 处理：标记 recalled=1，事务提交后经 WebSocket 推送 MESSAGE_RECALL(48) 通知接收方 + 回声发送方
     *
     * 用 messageOnlyId（前端生成的 UUID 字符串）作为关联键，而非 messageId：
     * 雪花 messageId 超出 JS 安全整数范围，前端 JSON 解析会精度丢失，
     * 传回后端无法精确匹配。messageOnlyId 是字符串，不受精度影响。
     */
    @RequestMapping("/recallMessage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO recallMessage(HttpServletRequest request, String messageOnlyId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            throw new BusinessException("token无效");
        }
        if (messageOnlyId == null || messageOnlyId.trim().isEmpty()) {
            throw new BusinessException("messageOnlyId不能为空");
        }
        // operatorUserId 取自 token，service 层再校验是否为消息发送者本人
        chatImMessageService.recallMessage(messageOnlyId, tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);
    }

    /**
     * 拉取群聊离线消息（增量同步）
     * 群消息单条存储（receive_user_id = groupId），按用户所在的所有群查询 messageId > lastMessageId 的消息
     * 前端游标：本地群消息中排除自己发送后的最大 backend_message_id
     */
    @RequestMapping("/pullOfflineGroupMessages")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO pullOfflineGroupMessages(HttpServletRequest request, Long lastMessageId) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            throw new BusinessException("token无效");
        }
        if (lastMessageId == null) {
            lastMessageId = 0L;
        }
        List<ChatImMessage> messages = chatImMessageService.pullOfflineGroupMessages(
                tokenUserInfoDto.getUserId(), lastMessageId);
        return getSuccessResponseVO(messages);
    }
}