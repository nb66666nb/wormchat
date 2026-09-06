package com.meetchat.Controller;
import com.meetchat.annotation.GlobalInterceptor;
import com.meetchat.entity.Dto.MessageSendDto;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.po.MeetingMember;
import com.meetchat.entity.po.UserInfo;
import com.meetchat.entity.vo.MeetingInfoUserVo;
import com.meetchat.entity.vo.MeetingMemberVO;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.service.MeetingInfoService;
import com.meetchat.service.MeetingMemberService;
import com.meetchat.service.impl.UserInfoServiceImpl;
import com.meetchat.utils.StringTools;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@RestController
@RequestMapping("/meeting")
public class MeetingController extends ABaseController{

    @Resource
    private MeetingMemberService meetingMemberService;
    @Resource
    private UserInfoServiceImpl userInfoService;
    @Resource
    private MeetingInfoService meetingInfoService;

    @RequestMapping("/create")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO createMeeting(HttpServletRequest httpRequest, @NotNull Integer joinType, @NotNull Integer meetingNoType, @NotEmpty String meetingNickName, @Size(max = 5) String password) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo(httpRequest);
        String userId = tokenUserInfo.getUserId();
        UserInfo userInfo =userInfoService.getUserInfoByUserId(userId);
        MeetingMember meetingMember = new MeetingMember();
        meetingMember.setCreateUserId(userId);
        meetingMember.setJionType(joinType);
        meetingMember.setMeetingName(meetingNickName);
        meetingMember.setJionPassword(password);
        meetingMember.setMeetingNo(meetingNoType==0 ? userInfo.getMeetingNo() : StringTools.getMeetingRandomId());
        MeetingMember meetingMember1=  meetingMemberService.createMeeting(meetingMember,tokenUserInfo);
        return getSuccessResponseVO(meetingMember1);
    }
    /**
     * 加入会议
     * 需要登录
     */
    @RequestMapping("/join")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO joinMeeting( HttpServletRequest httpRequest,@NotEmpty String meetingNo,String password) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo(httpRequest);
    MeetingMember meetingMember=    meetingInfoService.joinMeeting(meetingNo,password, tokenUserInfo);
        return getSuccessResponseVO(meetingMember);
    }
    /**
     * 离开会议
     * 需要登录
     */
    @RequestMapping("/leave")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO leaveMeeting(String meetingNo, HttpServletRequest httpRequest) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo(httpRequest);
        meetingInfoService.leaveMeeting(meetingNo, tokenUserInfo.getUserId());
        return getSuccessResponseVO(null);
    }
    /**
     * 结束会议（仅主持人可操作）
     * 需要登录
     */
    @RequestMapping("/end")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO endMeeting(String meetingNo, HttpServletRequest httpRequest) {
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo(httpRequest);
        meetingMemberService.endMeeting(meetingNo, tokenUserInfo);
        return getSuccessResponseVO(null);
    }
    /**
     * 根据会议号码查询会议信息
     * 需要登录
     */
    @RequestMapping("/getInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getMeetingInfo(String meetingNo) {
        MeetingMember meeting = meetingMemberService.getMeetingByMeetingNo(meetingNo);
        return getSuccessResponseVO(meeting);
    }

    /**
     * 获取会议成员列表
     * 需要登录
     */
    @RequestMapping("/getMembers")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getMeetingMembers(HttpServletRequest request,String meetingNo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        List<MeetingInfoUserVo> members = meetingInfoService.getMeetingMembers(meetingNo);
        return getSuccessResponseVO(members);
    }

    /**
     * 获取我创建的会议列表（分页）
     * 需要登录
     */
    @RequestMapping("/getMyMeetings")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getMyMeetings(Integer pageNo, Integer pageSize, HttpServletRequest httpRequest) {
        if (pageNo == null || pageNo < 1) pageNo = 1;
        if (pageSize == null || pageSize < 1) pageSize = 15;
        TokenUserInfoDto tokenUserInfo = getTokenUserInfo(httpRequest);
        PaginationResultVO<MeetingMember> result = meetingMemberService.getMyMeetings(
                tokenUserInfo.getUserId(), pageNo, pageSize);
        return getSuccessResponseVO(result);
    }
    @RequestMapping("/kickUser")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO kickUser(HttpServletRequest request, String meetingNo, String kickUserId,Integer staus) {
         TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
         meetingInfoService.kickUser(meetingNo, kickUserId, tokenUserInfoDto,staus);
         return getSuccessResponseVO(null);
    }
    @RequestMapping("/sendMessage")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO sendMessage(HttpServletRequest request, MessageSendDto messageSendDto) {
         TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
         meetingInfoService.sendMessage(messageSendDto, tokenUserInfoDto);
         return getSuccessResponseVO(null);
    }

}