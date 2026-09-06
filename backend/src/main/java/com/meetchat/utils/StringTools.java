package com.meetchat.utils;
import com.meetchat.entity.contants.Contants;
import com.meetchat.exception.BusinessException;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

import static java.security.CryptoPrimitive.SECURE_RANDOM;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;


public class StringTools {

    public static void checkParam(Object param) {
        try {
            Field[] fields = param.getClass().getDeclaredFields();
            boolean notEmpty = false;
            for (Field field : fields) {
                String methodName = "get" + StringTools.upperCaseFirstLetter(field.getName());
                Method method = param.getClass().getMethod(methodName);
                Object object = method.invoke(param);
                if (object != null && object instanceof java.lang.String && !StringTools.isEmpty(object.toString())
                        || object != null && !(object instanceof java.lang.String)) {
                    notEmpty = true;
                    break;
                }
            }
            if (!notEmpty) {
                throw new BusinessException("多参数更新，删除，必须有非空条件");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new BusinessException("校验参数是否为空失败");
        }
    }

    public static String upperCaseFirstLetter(String field) {
        if (isEmpty(field)) {
            return field;
        }
        //如果第二个字母是大写，第一个字母不大写
        if (field.length() > 1 && Character.isUpperCase(field.charAt(1))) {
            return field;
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    public static boolean isEmpty(String str) {
        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {
            return true;
        }
        return false;
    }

    public static String encodeByMD5(String originString) {
        return StringTools.isEmpty(originString) ? null : DigestUtils.md5Hex(originString);
    }

    // ==================== 密码加密（BCrypt，替代MD5） ====================

    /**
     * BCrypt 加密器：cost=10（2^10 轮盐化计算，单次约几十毫秒，安全性与性能的平衡点）
     * 线程安全，可静态复用
     */
    private static final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder BCRYPT_ENCODER =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(10);

    /**
     * BCrypt 加密（自带随机盐，同一明文每次密文不同，防彩虹表/撞库比对）
     */
    public static String encodeByBCrypt(String rawPassword) {
        return StringTools.isEmpty(rawPassword) ? null : BCRYPT_ENCODER.encode(rawPassword);
    }

    /**
     * 密码校验：兼容存量 MD5 密码（32位小写hex），BCrypt 密文走 matches()
     *
     * @param rawPassword 用户输入的明文密码
     * @param encodedPassword 数据库存储的密文（MD5 或 BCrypt）
     * @return true=匹配
     */
    public static boolean checkPassword(String rawPassword, String encodedPassword) {
        if (StringTools.isEmpty(rawPassword) || StringTools.isEmpty(encodedPassword)) {
            return false;
        }
        // 存量 MD5 密文（无盐）：直接比对，命中后应由调用方透明升级为 BCrypt
        if (isLegacyMd5Password(encodedPassword)) {
            return encodedPassword.equals(encodeByMD5(rawPassword));
        }
        return BCRYPT_ENCODER.matches(rawPassword, encodedPassword);
    }

    /**
     * 判断是否为历史遗留的 MD5 密文（32位小写十六进制）
     */
    public static boolean isLegacyMd5Password(String encodedPassword) {
        return encodedPassword != null
                && encodedPassword.length() == 32
                && encodedPassword.matches("[0-9a-f]{32}");
    }

    public static final String getRandomNumber(Integer count) {
        return RandomStringUtils.random(count, false, true);
    }

    public static final String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }

    public static final String getUserRandomId() {
        return getRandomNumber(Contants.USER_ID_LENGTH_11);
    }

    public static final String getMeetingRandomId( ) {
        return  "G"+getRandomNumber( Contants.USER_ID_LENGTH_11);
    }

    /**
     * 生成用户与用户之间的会话ID
     * 格式：S + 11位随机数字，共12位
     * 示例：S12345678901
     */
    public static final String getUserSessionId() {
        return "S" + getRandomNumber(Contants.USER_ID_LENGTH_11);
    }

    /**
     * 基于两个 userId 生成确定性的会话ID（与参数顺序无关，共12位）
     * 同一对用户无论参数顺序如何，生成的 sessionId 相同
     * 格式：S + 11位数字
     * 示例：getUserSessionId("U001", "U002") == getUserSessionId("U002", "U001")
     */
    public static final String getUserSessionId(String userId1, String userId2) {
        // 对两个 userId 排序，保证顺序无关
        String min = userId1.compareTo(userId2) < 0 ? userId1 : userId2;
        String max = userId1.compareTo(userId2) < 0 ? userId2 : userId1;
        // MD5 哈希
        String hash = encodeByMD5(min + "_" + max);
        // 从 hash 中提取 11 位数字
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hash.length() && sb.length() < 11; i++) {
            char c = hash.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
            } else {
                // 字母转数字 (a=0, b=1, ...)
                sb.append((c - 'a') % 10);
            }
        }
        // 不足 11 位补 0
        while (sb.length() < 11) {
            sb.append("0");
        }
        return "S" + sb.toString();
    }

    /**
     * 生成用户与群聊之间的会话ID
     * 格式：SG + 10位随机数字，共12位
     * 示例：SG1234567890
     */
    public static final String getGroupSessionId() {
        return "SG" + getRandomNumber(10);
    }

    /**
     * 生成用户与机器人之间的会话ID
     * 格式：SB + 10位随机数字，共12位
     * 示例：SB1234567890
     */
    public static final String getBotSessionId() {
        return "SB" + getRandomNumber(10);
    }

    /**
     * 生成机器人ID
     * 格式：R + 10位随机数字，共11位
     * 示例：R1234567890
     */
    public static final String getBotRandomId() {
        return "R" + getRandomNumber(10);
    }
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_LENGTH = 32; // token长度

    /**
     * 生成安全的随机token
     * @return Base64编码的安全token
     */
    public static String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public static String convertToUpperCase(String input) {
        if (input == null) {
            return null;
        }

        char[] chars = input.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] >= 'a' && chars[i] <= 'z') {
                chars[i] = (char)(chars[i] - 32);
            }
        }
        return new String(chars);
    }




}