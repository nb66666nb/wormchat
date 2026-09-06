package com.meetchat.Controller;

import com.meetchat.annotation.GlobalInterceptor;
import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.contants.Contants;
import com.meetchat.entity.po.UserInfo;
import com.meetchat.entity.vo.ResponseVO;
import com.meetchat.entity.vo.UserInfoVo;
import com.meetchat.exception.BusinessException;
import com.meetchat.redis.RedisComponet;
import com.meetchat.redis.RedisUtils;
import com.meetchat.service.UserInfoService;
import com.meetchat.service.impl.MeetingInfoServiceImpl;
import com.meetchat.service.impl.UserInfoServiceImpl;
import com.meetchat.utils.CopyTools;
import com.meetchat.utils.StringTools;
import com.wf.captcha.SpecCaptcha;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/account")
public class AccountController extends ABaseController{
   private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
   @Resource
    private RedisComponet redisComponet;
   @Resource
    private UserInfoServiceImpl userInfoService;
   @Resource
    private MeetingInfoServiceImpl meetingInfoService;

    /**
     * 获取图形验证码
     * 返回验证码图片的 Base64 编码和唯一 key
     */
    @RequestMapping("/getImage")
    public ResponseVO getCaptchaImage() {
        // 生成 130x48 的算术验证码
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        String codeU = captcha.text();
        String code=StringTools.convertToUpperCase(codeU);
        String base64 = captcha.toBase64();
        logger.info("captcha code: {}", code);
        // 生成唯一 key
        String key = UUID.randomUUID().toString().replace("-", "");

        // 存入 Redis，5 分钟过期
        redisComponet.setImageRedis(key,code);

        Map<String, String> result = new HashMap<>();
        result.put("captchaKey", key);
        result.put("captchaImage", base64);
        return getSuccessResponseVO(result);
    }

    public boolean checkCaptcha(String key, String code) {
        logger.info("cachedCode: {}, code: {}", key, code);
        if (StringTools.isEmpty(key) || StringTools.isEmpty(code)) {
            return false;
        }
        String cachedCode = redisComponet.getImageRedis(key);
        logger.info("cachedCode: {}", cachedCode);
         if(cachedCode==null||!cachedCode.equals(code)){


             return false;
         }
         return true;


    }

    @RequestMapping("/register")
    public ResponseVO register(@NotEmpty @Email String email,
                               @NotEmpty @Size(max = 20) String password,
                               @NotEmpty String captchaKey,
                               @NotEmpty String captchaCode,
                               @NotEmpty @Size(max = 20) String nickName) {
        // 1. 校验图形验证码
       try{ /* 【测试】临时关闭验证码校验，方便 JMeter 压测，测试完成后请恢复
        if (!checkCaptcha(captchaKey,captchaCode )){
            throw new BusinessException("图形验证码错误或已过期");
        }
        */

        // 2. 调用 Service 层完成注册（邮箱验证码校验 + 密码加密 + 入库）
          userInfoService.register(email,password,nickName);
        return getSuccessResponseVO(null);
    }finally {
           redisComponet.delRedis(Contants.CAPTCHA_PREFIX+captchaKey);
       }
    }
    /**
     * 用户登录
     * 流程：校验图形验证码 → 登录（Service 层处理密码比对 + Token 生成）
     */
    @RequestMapping("/login")
    public ResponseVO login(@NotEmpty @Email String email,
                            @NotEmpty  String password,
                            @NotEmpty String captchaKey,
                            @NotEmpty String captchaCode) {

       try {
           /* 【测试】临时关闭验证码校验，方便 JMeter 压测，测试完成后请恢复
           if (!checkCaptcha(captchaKey, captchaCode)) {
               throw new BusinessException("图形验证码错误或已过期");
           }
           */
           // 2. 调用 Service 层完成登录
           logger.info("email:"+email);
           UserInfoVo userInfoVo = userInfoService.login(email, password);
           return getSuccessResponseVO(userInfoVo);
       }finally {
           redisComponet.delRedis(Contants.CAPTCHA_PREFIX+captchaKey);
       }
    }

    /**
     * 退出登录
     * 将当前 Token 加入黑名单（需要登录才能退出）
     */

    @RequestMapping("/logout")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO logout(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        if(tokenUserInfoDto ==null){
           throw new BusinessException("用户未登录");
       }
        userInfoService.logout(tokenUserInfoDto);
        // 将 Token 加入黑名单
        redisComponet.delRedis(Contants.TOKEN_KEY_REDIS + tokenUserInfoDto.getToken());
        redisComponet.delRedis(Contants.USER_TOKEN_KEY_REDIS + tokenUserInfoDto.getUserId());

        return getSuccessResponseVO(null);
    }

    @RequestMapping("/getUserInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO getUserInfo(HttpServletRequest request) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserInfo userInfo= userInfoService.getUserInfoByUserId(tokenUserInfoDto.getUserId());
        UserInfoVo userInfoVo = CopyTools.copy(userInfo, UserInfoVo.class);
        return getSuccessResponseVO(userInfoVo);

    }
    @RequestMapping("/changeUserInfo")
    @GlobalInterceptor(checkLogin = true)
    public ResponseVO changeUserInfo(HttpServletRequest request,UserInfoVo userInfoVo) {
        TokenUserInfoDto tokenUserInfoDto = getTokenUserInfo(request);
        UserInfo userInfo  =CopyTools.copy(userInfoVo, UserInfo.class);
        userInfoService.updateUserInfoByUserId(userInfo,tokenUserInfoDto.getUserId());
        return getSuccessResponseVO(null);

    }






}