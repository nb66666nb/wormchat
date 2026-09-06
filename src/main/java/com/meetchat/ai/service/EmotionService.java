package com.meetchat.ai.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meetchat.ai.llm.LlmClient;
import com.meetchat.ai.llm.LlmRequest;
import com.meetchat.ai.llm.LlmResponse;
import com.meetchat.entity.po.BotEmotionLog;
import com.meetchat.service.BotEmotionLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 情绪分析服务
 * 支持两种分析方式：
 * 1. LLM细粒度情绪分析（推荐，准确率高）
 * 2. 关键词匹配（fallback，LLM不可用时降级）
 *
 * 细粒度情绪类型（12种）：
 * - 正面：happy(开心), excited(兴奋), moved(感动), grateful(感激)
 * - 负面：sad(悲伤), lonely(孤独), anxious(焦虑), angry(愤怒), disappointed(失望)
 * - 中性：neutral(平静), curious(好奇), surprised(惊讶), confused(困惑)
 */
@Service("emotionService")
public class EmotionService {

    private static final Logger logger = LoggerFactory.getLogger(EmotionService.class);

    @Resource
    private BotEmotionLogService botEmotionLogService;

    @Resource
    private LlmClient llmClient;

    /**
     * 分析用户消息的情绪（细粒度，LLM驱动）
     * LLM不可用时自动降级为关键词分析
     *
     * @param userMessage 用户消息
     * @return 情绪分析结果
     */
    public EmotionAnalysisResult analyzeEmotion(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return buildNeutralResult();
        }

        // 优先使用LLM分析
        if (llmClient != null && llmClient.isAvailable()) {
            try {
                EmotionAnalysisResult result = analyzeByLLM(userMessage);
                if (result != null && result.getPrimaryEmotion() != null) {
                    logger.debug("LLM情绪分析完成: emotion={}, intensity={}, confidence={}",
                            result.getPrimaryEmotion(), result.getIntensity(), result.getConfidence());
                    return result;
                }
            } catch (Exception e) {
                logger.warn("LLM情绪分析失败，降级为关键词分析: {}", e.getMessage());
            }
        }

        // fallback: 关键词分析
        return analyzeByKeyword(userMessage);
    }

    /**
     * 记录情绪日志（使用细粒度分析结果）
     */
    public void recordLog(String userId, String botId, String sessionId,
                          String userMessage, Long sourceMessageId) {
        try {
            // 分析情绪
            EmotionAnalysisResult analysis = analyzeEmotion(userMessage);

            BotEmotionLog emotionLog = new BotEmotionLog();
            emotionLog.setUserId(userId);
            emotionLog.setBotId(botId);
            emotionLog.setSessionId(sessionId);
            emotionLog.setSourceMessageId(sourceMessageId);
            emotionLog.setAnalysisMethod(analysis.getConfidence() != null
                    && analysis.getConfidence().compareTo(new BigDecimal("0.7")) > 0
                    ? "llm" : "keyword");

            // 情绪类型（主情绪）
            emotionLog.setEmotionType(analysis.getPrimaryEmotion());
            emotionLog.setEmotionIntensity(analysis.getIntensity());
            emotionLog.setEmotionTrigger(analysis.getTrigger());
            emotionLog.setConfidence(analysis.getConfidence());

            // 时间维度
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            emotionLog.setTimeOfDay(hour < 6 ? "深夜" : hour < 12 ? "上午" : hour < 18 ? "下午" : "晚上");
            emotionLog.setIsWeekend(cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                    || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY ? 1 : 0);

            botEmotionLogService.add(emotionLog);
            logger.debug("情绪日志已记录: userId={}, emotion={}, method={}",
                    userId, analysis.getPrimaryEmotion(), emotionLog.getAnalysisMethod());
        } catch (Exception e) {
            logger.warn("记录情绪日志异常: userId={}", userId, e);
        }
    }

    // ==================== LLM 细粒度情绪分析 ====================

    /**
     * 使用LLM进行细粒度情绪分析
     */
    private EmotionAnalysisResult analyzeByLLM(String userMessage) {
        String systemPrompt = buildEmotionSystemPrompt();

        List<LlmRequest.Message> messages = new ArrayList<>();
        messages.add(new LlmRequest.Message("system", systemPrompt));
        messages.add(new LlmRequest.Message("user", "请分析以下用户消息的情绪：\n\n" + userMessage));

        // 用较低的temperature做分类任务，更稳定
        LlmResponse response = llmClient.chat(messages, 0.1, 300);
        String content = response.getContent();

        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        return parseEmotionResult(content);
    }

    /**
     * 构建情绪分析System Prompt
     */
    private String buildEmotionSystemPrompt() {
        return "你是一个专业的情绪分析专家。请分析用户消息的情绪，输出严格的JSON格式。\n\n" +
                "情绪类型（12种）：\n" +
                "- 正面：happy(开心), excited(兴奋), moved(感动), grateful(感激)\n" +
                "- 负面：sad(悲伤), lonely(孤独), anxious(焦虑), angry(愤怒), disappointed(失望)\n" +
                "- 中性：neutral(平静), curious(好奇), surprised(惊讶), confused(困惑)\n\n" +
                "输出JSON字段说明：\n" +
                "- primary_emotion: 主要情绪类型（必选）\n" +
                "- intensity: 情绪强度，0-1之间的数字（必选）\n" +
                "- secondary_emotion: 次要情绪类型，没有则为空字符串（可选）\n" +
                "- valence: 情绪效价，positive/negative/neutral（必选）\n" +
                "- trigger: 情绪触发点，简短描述，20字以内（必选）\n" +
                "- suggested_response_style: 建议回复风格，empathize共情/comfort安慰/encourage鼓励/listen倾听/normal正常（必选）\n" +
                "- user_openness: 用户开放度，0-1，用户是否愿意深入交流（必选）\n" +
                "- confidence: 分析置信度，0-1（必选）\n\n" +
                "注意：\n" +
                "1. 只输出JSON，不要有任何其他文字或markdown代码块\n" +
                "2. 情绪强度根据语气词、感叹号、程度副词等判断\n" +
                "3. 如果只有简单的\"嗯\"\"哦\"\"好的\"，判断为neutral，强度较低\n" +
                "4. 用户分享个人事情时，开放度较高";
    }

    /**
     * 解析LLM返回的情绪分析结果
     */
    private EmotionAnalysisResult parseEmotionResult(String content) {
        try {
            String jsonStr = extractJson(content);
            JSONObject json = JSON.parseObject(jsonStr);

            EmotionAnalysisResult result = new EmotionAnalysisResult();
            result.setPrimaryEmotion(json.getString("primary_emotion"));
            result.setIntensity(json.getBigDecimal("intensity"));
            result.setSecondaryEmotion(json.getString("secondary_emotion"));
            result.setValence(json.getString("valence"));
            result.setTrigger(json.getString("trigger"));
            result.setSuggestedResponseStyle(json.getString("suggested_response_style"));
            result.setUserOpenness(json.getBigDecimal("user_openness"));
            result.setConfidence(json.getBigDecimal("confidence"));

            // 校验必填字段
            if (result.getPrimaryEmotion() == null || result.getPrimaryEmotion().isEmpty()) {
                result.setPrimaryEmotion("neutral");
            }
            if (result.getIntensity() == null) {
                result.setIntensity(BigDecimal.ZERO);
            }
            if (result.getConfidence() == null) {
                result.setConfidence(new BigDecimal("0.5"));
            }
            if (result.getValence() == null) {
                result.setValence("neutral");
            }

            return result;
        } catch (Exception e) {
            logger.warn("解析情绪分析结果失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从文本中提取JSON
     */
    private String extractJson(String content) {
        if (content == null) return "{}";
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "");
        }
        int start = trimmed.indexOf("{");
        int end = trimmed.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    // ==================== 关键词分析（fallback） ====================

    /**
     * 基于关键词的情绪检测（降级方案）
     */
    private EmotionAnalysisResult analyzeByKeyword(String message) {
        EmotionAnalysisResult result = new EmotionAnalysisResult();

        String emotion = detectEmotionByKeyword(message);
        result.setPrimaryEmotion(emotion);
        result.setIntensity(estimateIntensity(message, emotion));
        result.setConfidence(new BigDecimal("0.6"));
        result.setTrigger(extractTrigger(message, emotion));

        // 效价判断
        switch (emotion) {
            case "happy":
            case "excited":
            case "moved":
            case "grateful":
                result.setValence("positive");
                result.setSuggestedResponseStyle("encourage");
                break;
            case "sad":
            case "lonely":
                result.setValence("negative");
                result.setSuggestedResponseStyle("comfort");
                break;
            case "anxious":
                result.setValence("negative");
                result.setSuggestedResponseStyle("listen");
                break;
            case "angry":
            case "disappointed":
                result.setValence("negative");
                result.setSuggestedResponseStyle("empathize");
                break;
            case "curious":
            case "surprised":
                result.setValence("neutral");
                result.setSuggestedResponseStyle("normal");
                break;
            default:
                result.setValence("neutral");
                result.setSuggestedResponseStyle("normal");
                break;
        }

        result.setUserOpenness(new BigDecimal("0.5"));
        return result;
    }

    /**
     * 基于关键词的情绪检测
     */
    private String detectEmotionByKeyword(String message) {
        if (message == null) return "neutral";
        String lower = message.toLowerCase();

        // 正面情绪
        if (lower.contains("开心") || lower.contains("高兴") || lower.contains("哈哈")
                || lower.contains("太好了") || lower.contains("棒") || lower.contains("谢谢")
                || lower.contains("感谢") || lower.contains("感恩")) {
            return "happy";
        }
        if (lower.contains("激动") || lower.contains("兴奋") || lower.contains("太棒了")
                || lower.contains("超开心")) {
            return "excited";
        }
        if (lower.contains("感动") || lower.contains("暖心") || lower.contains("哭了")
                && (lower.contains("感动") || lower.contains("温暖"))) {
            return "moved";
        }

        // 负面情绪
        if (lower.contains("难过") || lower.contains("伤心")
                || lower.contains("失落") || lower.contains("痛苦")) {
            return "sad";
        }
        if (lower.contains("孤独") || lower.contains("寂寞") || lower.contains("一个人")
                && lower.contains("无聊")) {
            return "lonely";
        }
        if (lower.contains("焦虑") || lower.contains("担心") || lower.contains("害怕")
                || lower.contains("紧张") || lower.contains("压力") || lower.contains("忐忑")) {
            return "anxious";
        }
        if (lower.contains("生气") || lower.contains("烦") || lower.contains("愤怒")
                || lower.contains("讨厌") || lower.contains("气死")) {
            return "angry";
        }
        if (lower.contains("失望") || lower.contains("可惜") || lower.contains("遗憾")) {
            return "disappointed";
        }

        // 中性情绪
        if (lower.contains("好奇") || lower.contains("为什么") || lower.contains("怎么")
                || lower.contains("呢？") || lower.contains("吗？")) {
            return "curious";
        }
        if (lower.contains("惊讶") || lower.contains("没想到") || lower.contains("居然")
                || lower.contains("竟然")) {
            return "surprised";
        }
        if (lower.contains("困惑") || lower.contains("不懂") || lower.contains("不明白")
                || lower.contains("懵")) {
            return "confused";
        }

        return "neutral";
    }

    /**
     * 估算情绪强度
     */
    private BigDecimal estimateIntensity(String message, String emotionType) {
        if ("neutral".equals(emotionType)) {
            return BigDecimal.ZERO;
        }
        BigDecimal intensity = new BigDecimal("0.5");
        if (message.contains("！") || message.contains("!")) {
            intensity = intensity.add(new BigDecimal("0.2"));
        }
        if (message.contains("非常") || message.contains("特别") || message.contains("极其")
                || message.contains("超级") || message.contains("太")) {
            intensity = intensity.add(new BigDecimal("0.2"));
        }
        return intensity.min(BigDecimal.ONE);
    }

    /**
     * 提取情绪触发原因
     */
    private String extractTrigger(String message, String emotionType) {
        if ("neutral".equals(emotionType)) {
            return null;
        }
        return message.length() > 50 ? message.substring(0, 50) + "..." : message;
    }

    /**
     * 构建中性情绪结果（空消息等场景）
     */
    private EmotionAnalysisResult buildNeutralResult() {
        EmotionAnalysisResult result = new EmotionAnalysisResult();
        result.setPrimaryEmotion("neutral");
        result.setIntensity(BigDecimal.ZERO);
        result.setValence("neutral");
        result.setConfidence(new BigDecimal("0.9"));
        result.setSuggestedResponseStyle("normal");
        result.setUserOpenness(new BigDecimal("0.5"));
        return result;
    }
}
