package com.meetchat.entity.contants;

public class Contants {
    /**
     * 验证码 Redis key 前缀
     */
    public static final String CAPTCHA_PREFIX = "captcha:";

    /**
     * 验证码过期时间（分钟）
     */
    public static final int CAPTCHA_EXPIRE_5_Min = 5*60;
    public static final Integer USER_ID_LENGTH_11 = 11;
    public static final Integer TOKEN_LENGTH_20= 20;
    public static final String TOKEN_KEY_REDIS = "token";
    public static final String USER_TOKEN_KEY_REDIS = "token_user";
    //两天
    public static final Integer TOKEN_SAVE_TIME_2_DAYS = 2*24*60*60;
    public static final String CREATE_MEETING_MESSAGE="会议已经创建完成";
    public static final String MEETING_END_MESSAGE="会议已经结束";
    public static final String MEETING_JOIN_SUCCESS="已经成功参加会议";
    public static final String MEETING_LEAVE_SUCCESS="已经离开会议";
    public static final String FILE_HASH="file:hash:";
}