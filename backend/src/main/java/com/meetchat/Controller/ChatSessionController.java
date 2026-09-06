package com.meetchat.Controller;

import com.meetchat.annotation.GlobalInterceptor;
import com.meetchat.entity.Dto.CreateRobotDto;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.enums.UserContactTypeEnum;
import com.meetchat.entity.po.ChatSession;
import com.meetchat.entity.query.ChatSessionQuery;
import com.meetchat.exception.BusinessException;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.service.ChatSessionService;
import com.meetchat.utils.StringTools;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/chatSession")
public class ChatSessionController extends ABaseController {
    @Resource
    private ChatSessionService chatSessionService;

    /**
     * 会话列表同步（用户上线时调用）
     * 返回后端该用户的全部会话，解决离线期间的会话变更同步问题
     * （如离线时被邀请入群/被踢出群/群解散等场景）
     * 前端以后端为准 upsert 本地 chat_session 表
     */
    @RequestMapping("/syncSessions")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO syncSessions(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (tokenUserInfoDto == null) {
            throw new BusinessException("token无效");
        }
        ChatSessionQuery query = new ChatSessionQuery();
        query.setUserId(tokenUserInfoDto.getUserId());
        List<ChatSession> sessions = chatSessionService.findListByParam(query);
        return getSuccessResponseVO(sessions);
    }

    /**
     * 创建机器人
     * 流程：1.参数校验 → 2.关联模板填充默认值 → 3.生成机器人ID → 4.添加好友（双向） → 5.创建会话 → 6.初始化用户画像
     * @param dto 创建机器人参数（包含类型、性格调参、陪伴型专属、功能配置等）
     */
    @RequestMapping("/createRobot")
    public ResponseVO createRobot(@RequestBody CreateRobotDto dto, HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if (dto == null) {
            return getBusinessErrorResponseVO(new BusinessException("请求参数不能为空"), null);
        }
        if (StringTools.isEmpty(dto.getBotName())) {
            return getBusinessErrorResponseVO(new BusinessException("机器人名称不能为空"), null);
        }
        if (StringTools.isEmpty(dto.getBotDescription())) {
            return getBusinessErrorResponseVO(new BusinessException("机器人描述不能为空"), null);
        }
        if (StringTools.isEmpty(dto.getCategory())) {
            return getBusinessErrorResponseVO(new BusinessException("机器人类型不能为空"), null);
        }
        // 校验类型合法性
        if (!"FUNCTIONAL".equals(dto.getCategory())
                && !"COMPANION".equals(dto.getCategory())
                && !"HYBRID".equals(dto.getCategory())) {
            return getBusinessErrorResponseVO(new BusinessException("机器人类型不合法，应为FUNCTIONAL/COMPANION/HYBRID"), null);
        }

        // 构建personality字符串
        String botPersonality = dto.buildPersonalityString();

        chatSessionService.createRobot(
                tokenUserInfoDto.getUserId(),
                dto.getBotName(),
                dto.getBotAvatarPath(),
                dto.getBotDescription(),
                dto.getBotSystemPrompt(),
                dto.getBotWelcomeMsg(),
                dto.getCategory(),
                dto.getTemplateId(),
                botPersonality
        );
        return getSuccessResponseVO(null);
    }

}