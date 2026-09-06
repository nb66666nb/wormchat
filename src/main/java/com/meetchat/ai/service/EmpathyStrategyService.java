package com.meetchat.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 共情策略服务
 * 根据用户情绪生成对应的回复指导，注入到Prompt中指导AI的回复方式
 *
 * 核心策略：
 * - 先接住情绪，再回应内容
 * - 根据情绪强度调整回应深度
 * - 用户开放度低时不要追着问
 */
@Service("empathyStrategyService")
public class EmpathyStrategyService {

    private static final Logger logger = LoggerFactory.getLogger(EmpathyStrategyService.class);

    /**
     * 根据情绪分析结果生成共情指导Prompt
     *
     * @param emotion 情绪分析结果
     * @param botCategory 机器人类型（功能型/陪伴型/混合型）
     * @return 共情指导文本（注入到system prompt末尾）
     */
    public String buildEmpathyPrompt(EmotionAnalysisResult emotion, String botCategory) {
        if (emotion == null || emotion.getPrimaryEmotion() == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n【当前用户情绪状态】\n");
        sb.append("用户当前情绪：").append(describeEmotion(emotion.getPrimaryEmotion()));
        if (emotion.getIntensity() != null) {
            sb.append("（强度：").append(formatIntensity(emotion.getIntensity())).append("）");
        }
        sb.append("\n");

        // 回复风格指导
        String styleGuide = getStyleGuide(emotion);
        sb.append("回复指导：").append(styleGuide).append("\n");

        // 具体技巧（根据情绪类型）
        String tips = getEmpathyTips(emotion);
        if (tips != null && !tips.isEmpty()) {
            sb.append("共情技巧：").append(tips).append("\n");
        }

        // 开放度提醒
        if (emotion.getUserOpenness() != null
                && emotion.getUserOpenness().compareTo(new BigDecimal("0.3")) < 0) {
            sb.append("注意：用户当前开放度较低，点到为止，不要追问太多，给用户空间。\n");
        }

        // 根据机器人类型调整
        if ("functional".equalsIgnoreCase(botCategory)) {
            // 功能型机器人：共情适度，主要还是解决问题
            sb.append("你是功能型助手，共情点到为止，不要过度情绪化，重点是帮助用户解决问题。\n");
        }

        logger.debug("生成共情指导: emotion={}, style={}", emotion.getPrimaryEmotion(),
                emotion.getSuggestedResponseStyle());
        return sb.toString();
    }

    // ==================== 情绪描述 ====================

    /**
     * 把情绪code翻译成中文描述
     */
    private String describeEmotion(String emotion) {
        switch (emotion) {
            case "happy": return "开心";
            case "excited": return "兴奋";
            case "moved": return "感动";
            case "grateful": return "感激";
            case "sad": return "难过";
            case "lonely": return "孤独";
            case "anxious": return "焦虑";
            case "angry": return "生气";
            case "disappointed": return "失望";
            case "curious": return "好奇";
            case "surprised": return "惊讶";
            case "confused": return "困惑";
            case "neutral":
            default: return "平静";
        }
    }

    private String formatIntensity(BigDecimal intensity) {
        double val = intensity.doubleValue();
        if (val < 0.3) return "轻微";
        if (val < 0.6) return "中等";
        if (val < 0.8) return "较强";
        return "强烈";
    }

    // ==================== 回复风格指导 ====================

    private String getStyleGuide(EmotionAnalysisResult emotion) {
        String style = emotion.getSuggestedResponseStyle();
        if (style == null) style = "normal";

        switch (style) {
            case "empathize":
                return "先共情对方的情绪，站在对方角度，表示理解和认同，不要急着给建议或评判对错";
            case "comfort":
                return "温柔安慰，给对方陪伴感，用温暖的语气，让对方感到被理解和支持";
            case "encourage":
                return "积极回应，分享快乐，给予肯定和鼓励，带动情绪";
            case "listen":
                return "耐心倾听为主，引导对方多说，不要急着给解决方案，让对方把情绪说出来";
            case "normal":
            default:
                return "正常对话即可，保持自然友好的语气";
        }
    }

    // ==================== 具体共情技巧 ====================

    private String getEmpathyTips(EmotionAnalysisResult emotion) {
        String primaryEmotion = emotion.getPrimaryEmotion();
        BigDecimal intensity = emotion.getIntensity();
        double intensityVal = intensity != null ? intensity.doubleValue() : 0.5;

        switch (primaryEmotion) {
            case "sad":
                if (intensityVal >= 0.7) {
                    return "1. 先说\"抱抱你\"\"心疼你\"之类表达心疼的话\n" +
                            "2. 不要说\"别难过了\"\"开心点\"，这会让对方觉得不被理解\n" +
                            "3. 引导对方说说发生了什么，但不要逼问\n" +
                            "4. 语气要慢、要温柔";
                } else {
                    return "1. 表达关心：\"怎么啦？看起来有点不开心\"\n" +
                            "2. 给对方空间，愿意说就听，不愿意也没关系";
                }

            case "lonely":
                return "1. 表达陪伴感：\"我在呢\"\"有我陪你呀\"\n" +
                        "2. 不要问\"你怎么一个人\"这种让人更孤独的话\n" +
                        "3. 可以主动找话题陪对方聊会儿";

            case "anxious":
                return "1. 先安抚：\"别着急，慢慢说\"\n" +
                        "2. 帮对方把焦虑具体化，而不是空泛安慰\n" +
                        "3. 不要说\"这有什么好担心的\"，会让人觉得不被理解\n" +
                        "4. 可以陪对方一起梳理思路";

            case "angry":
                return "1. 先接住情绪：\"听起来真的很气人\"\"换我我也生气\"\n" +
                        "2. 不要评判对错，不要讲道理\n" +
                        "3. 先让对方把情绪发泄出来，再聊怎么办\n" +
                        "4. 可以一起吐槽，但不要煽风点火过头";

            case "disappointed":
                return "1. 认可对方的感受：\"是啊，确实挺遗憾的\"\n" +
                        "2. 不要说\"没事的\"\"别在意\"，会显得轻描淡写\n" +
                        "3. 适度安慰，但不要强行打气";

            case "happy":
            case "excited":
                return "1. 分享对方的快乐：\"太好了！\"\"替你开心！\"\n" +
                        "2. 可以追问细节，让对方多说点开心的事\n" +
                        "3. 用活泼的语气，多发点开心的表情";

            case "moved":
                return "1. 表达被打动：\"好暖心啊\"\"看着都感动了\"\n" +
                        "2. 可以呼应对方的感动情绪\n" +
                        "3. 语气温暖柔和";

            case "grateful":
                return "1. 谦虚回应：\"不客气~能帮到你我也开心\"\n" +
                        "2. 接受对方的感谢，不用推脱\n" +
                        "3. 可以顺势问问还有什么可以帮忙的";

            case "curious":
                return "1. 积极回应对方的好奇心\n" +
                        "2. 解答要生动有趣，不要太干\n" +
                        "3. 可以反问引导对方深入思考";

            case "surprised":
                return "1. 呼应对方的惊讶：\"真的假的？\"\"这么巧！\"\n" +
                        "2. 可以表现出好奇，让对方继续说";

            case "confused":
                return "1. 帮对方理清思路\n" +
                        "2. 用简单易懂的方式解释\n" +
                        "3. 可以反问确认：\"你是说...对吗？\"";

            case "neutral":
            default:
                return "";
        }
    }
}
