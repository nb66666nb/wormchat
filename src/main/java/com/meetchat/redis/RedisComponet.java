package com.meetchat.redis;


import com.meetchat.entity.Dto.TokenUserInfoDto;
import com.meetchat.entity.contants.Contants;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class RedisComponet {
    @Resource
    private RedisUtils redisUtils;
    public void saveTokenUserInfo(TokenUserInfoDto tokenUserInfoDto){
        redisUtils.setex(Contants.TOKEN_KEY_REDIS + tokenUserInfoDto.getToken(),tokenUserInfoDto, Contants.TOKEN_SAVE_TIME_2_DAYS);
        redisUtils.setex(Contants.USER_TOKEN_KEY_REDIS + tokenUserInfoDto.getUserId(),tokenUserInfoDto.getToken(), Contants.TOKEN_SAVE_TIME_2_DAYS);
    }
    public TokenUserInfoDto getTokenUserInfoByUserId(String userId){
        String token = (String) redisUtils.get(Contants.USER_TOKEN_KEY_REDIS+userId);
        return getTokenUserInfoByToken(token);
    }
    public TokenUserInfoDto getTokenUserInfoByToken(String token){
       TokenUserInfoDto tokenUserInfoDto =(TokenUserInfoDto) redisUtils.get(Contants.TOKEN_KEY_REDIS+token);
         return tokenUserInfoDto;
    }
    public void setImageRedis(String key,String redisValue){
        redisUtils.setex(Contants.CAPTCHA_PREFIX+key,redisValue, Contants.CAPTCHA_EXPIRE_5_Min);

    }
    public String getImageRedis(String redisKey) {
        return (String) redisUtils.get(Contants.CAPTCHA_PREFIX+redisKey);
    }
    public void delRedis(String redisKey){
        redisUtils.delete(redisKey);
    }

}