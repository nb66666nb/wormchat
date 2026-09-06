package com.meetchat.Controller;

import com.meetchat.annotation.GlobalInterceptor;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.query.ChatMessageQuery;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.service.ChatMessageService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/chatMessage")
public class ChatMessageController extends ABaseController {

    @Resource
    private ChatMessageService chatMessageService;

    /**
     * 根据会议号查询聊天记录（分页）
     * SQL层过滤：群聊消息全部返回，私聊消息只返回当前用户参与的
     */
    @RequestMapping("/getChatMessages")
    @GlobalInterceptor
    public ResponseVO getChatMessages(HttpServletRequest request,
                                      String meetingNo,
                                      Integer pageNo,
                                      Integer pageSize) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo(request);
        if (tokenUserInfo == null) {
            return getServerErrorResponseVO(null);
        }

        if (pageNo == null || pageNo < 1) pageNo = 1;
        if (pageSize == null || pageSize < 1) pageSize = 15;

        ChatMessageQuery query = new ChatMessageQuery();
        query.setMeetingNo(meetingNo);
        query.setCurrentUserId(tokenUserInfo.getUserId());
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);

        PaginationResultVO result = chatMessageService.findListByPage(query);
        return getSuccessResponseVO(result);
    }
}