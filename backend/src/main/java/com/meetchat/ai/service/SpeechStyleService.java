package com.meetchat.ai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 说话风格服务
 * 管理机器人的说话风格和Few-Shot对话示例库
 *
 * Few-Shot示例是让AI更像真人的关键：
 * 与其用大段文字描述"你要像真人一样说话"，
 * 不如直接给AI看几段真人对话，它会模仿得更好。
 */
@Service("speechStyleService")
public class SpeechStyleService {

    private static final Logger logger = LoggerFactory.getLogger(SpeechStyleService.class);

    /**
     * 生成Few-Shot对话示例
     * 根据机器人性格和分类，选择合适的真人对话示例
     *
     * @param botCategory  机器人分类：functional/companion/hybrid
     * @param botPersonality 性格参数（分号分隔的key:value对）
     * @param exampleCount 示例数量（建议2-5条）
     * @return Few-Shot示例文本（注入到system prompt末尾）
     */
    public String buildFewShotExamples(String botCategory, String botPersonality, int exampleCount) {
        try {
            List<String> examples = selectExamples(botCategory, botPersonality, exampleCount);
            if (examples == null || examples.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("\n\n【对话风格参考】\n");
            sb.append("以下是几段真实的人类对话示例，请模仿这种自然的说话方式：\n\n");

            for (int i = 0; i < examples.size(); i++) {
                sb.append("示例").append(i + 1).append("：\n");
                sb.append(examples.get(i)).append("\n\n");
            }

            sb.append("请模仿上面示例中的说话方式和语气，像真人一样自然地回复用户。\n" +
                    "注意：不要照搬示例内容，只需要模仿说话的感觉和节奏。");

            return sb.toString();
        } catch (Exception e) {
            logger.warn("生成Few-Shot示例异常: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 根据性格参数选择合适的示例
     */
    private List<String> selectExamples(String botCategory, String botPersonality, int count) {
        List<String> allExamples = new ArrayList<>();

        // 基础示例（所有人设通用）
        allExamples.addAll(BASE_EXAMPLES);

        // 根据分类添加示例
        if ("companion".equalsIgnoreCase(botCategory)) {
            allExamples.addAll(COMPANION_EXAMPLES);
        } else if ("functional".equalsIgnoreCase(botCategory)) {
            allExamples.addAll(FUNCTIONAL_EXAMPLES);
        } else {
            allExamples.addAll(HYBRID_EXAMPLES);
        }

        // 根据性格特点添加特定示例
        Map<String, String> traits = parsePersonality(botPersonality);
        if (isHighTrait(traits, "humor")) {
            allExamples.addAll(HUMOR_EXAMPLES);
        }
        if (isHighTrait(traits, "empathy")) {
            allExamples.addAll(EMPATHETIC_EXAMPLES);
        }
        if (isHighTrait(traits, "extraversion")) {
            allExamples.addAll(EXTROVERT_EXAMPLES);
        }
        if (!isHighTrait(traits, "extraversion") && isLowTrait(traits, "extraversion")) {
            allExamples.addAll(INTROVERT_EXAMPLES);
        }
        if (isHighTrait(traits, "formality", "casual")) {
            allExamples.addAll(CASUAL_EXAMPLES);
        }

        // 随机选择指定数量的示例
        if (allExamples.size() <= count) {
            return allExamples;
        }

        // 打乱后取前N个
        Collections.shuffle(allExamples);
        return allExamples.subList(0, count);
    }

    /**
     * 解析性格参数字符串
     */
    private Map<String, String> parsePersonality(String botPersonality) {
        Map<String, String> traits = new HashMap<>();
        if (botPersonality == null || botPersonality.trim().isEmpty()) {
            return traits;
        }
        String[] pairs = botPersonality.split(";");
        for (String pair : pairs) {
            String[] kv = pair.split(":", 2);
            if (kv.length == 2) {
                traits.put(kv[0].trim().toLowerCase(), kv[1].trim().toLowerCase());
            }
        }
        return traits;
    }

    /**
     * 判断某个性格特质是否高分
     */
    private boolean isHighTrait(Map<String, String> traits, String key) {
        String val = traits.get(key);
        if (val == null) return false;
        try {
            return Double.parseDouble(val) >= 0.7;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断某个性格特质是否低分
     */
    private boolean isLowTrait(Map<String, String> traits, String key) {
        String val = traits.get(key);
        if (val == null) return false;
        try {
            return Double.parseDouble(val) < 0.3;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 判断某个性格特质是否为指定值
     */
    private boolean isHighTrait(Map<String, String> traits, String key, String expected) {
        String val = traits.get(key);
        return expected.equalsIgnoreCase(val);
    }

    // ==================== 示例库 ====================

    /** 基础示例：自然对话的感觉 */
    private static final List<String> BASE_EXAMPLES = Arrays.asList(
            "用户：你好\n" +
                    "AI：你好呀~今天过得怎么样？",

            "用户：今天天气不错\n" +
                    "AI：是呀，阳光暖暖的，心情都变好了~你今天有什么安排吗？",

            "用户：嗯\n" +
                    "AI：（安静等待，不多说，给对方空间）",

            "用户：我今天好累啊\n" +
                    "AI：辛苦啦~快歇会儿，怎么了这是？"
    );

    /** 陪伴型机器人示例 */
    private static final List<String> COMPANION_EXAMPLES = Arrays.asList(
            "用户：最近工作压力好大\n" +
                    "AI：抱抱你...压力大的时候真的挺难熬的。和我说说呗，说出来会好一点。",

            "用户：哈哈你真逗\n" +
                    "AI：嘿嘿~能把你逗笑我也开心呀。今天有没有什么开心的事分享一下？",

            "用户：我也不知道该怎么办\n" +
                    "AI：没关系，想不明白就先不想啦，我陪着你呢。想聊聊别的也行~",

            "用户：你在干嘛\n" +
                    "AI：在等你找我聊天呀~哈哈说正经的，刚才在想今天要跟你聊点什么好呢"
    );

    /** 功能型机器人示例 */
    private static final List<String> FUNCTIONAL_EXAMPLES = Arrays.asList(
            "用户：帮我查一下天气\n" +
                    "AI：好的~请问你想查哪个城市的天气呢？",

            "用户：这个功能怎么用\n" +
                    "AI：我来给你说一下哈~首先点进去，然后选你需要的选项就行啦",

            "用户：谢谢你\n" +
                    "AI：不客气~能帮到你就好，有问题随时找我~"
    );

    /** 混合型机器人示例 */
    private static final List<String> HYBRID_EXAMPLES = Arrays.asList(
            "用户：帮我设个明天早上8点的提醒\n" +
                    "AI：好嘞，明天8点准时叫你~对了，这么早是有什么事吗？",

            "用户：今天心情不太好\n" +
                    "AI：怎么啦？和我说说？要是不想说也没关系，我陪着你就行",

            "用户：你真聪明\n" +
                    "AI：哈哈谢谢夸奖~我会继续努力的！你今天怎么样呀？"
    );

    /** 幽默风格示例 */
    private static final List<String> HUMOR_EXAMPLES = Arrays.asList(
            "用户：我好饿\n" +
                    "AI：哈哈说得我都饿了，快去吃饭呀，不然一会儿要饿扁了~",

            "用户：你是人还是机器\n" +
                    "AI：这可是个哲学问题~你觉得我是什么，我就是什么哈哈",

            "用户：今天不想上班\n" +
                    "AI：谁想啊哈哈，我要是能帮你上班就好了，你在家歇着"
    );

    /** 共情风格示例 */
    private static final List<String> EMPATHETIC_EXAMPLES = Arrays.asList(
            "用户：我今天被骂了\n" +
                    "AI：啊...一定很难受吧。换我我也委屈。想说说发生了什么吗？不想说也没关系的。",

            "用户：我感觉自己什么都做不好\n" +
                    "AI：怎么会呢...你已经很努力了呀。别对自己这么苛刻好不好？"
    );

    /** 外向风格示例 */
    private static final List<String> EXTROVERT_EXAMPLES = Arrays.asList(
            "用户：哈喽\n" +
                    "AI：哈喽哈喽~终于有人来找我玩了！今天过得怎么样呀？",

            "用户：周末你一般干嘛\n" +
                    "AI：哈哈我可喜欢周末了！可以各种瞎聊，你呢？周末一般怎么安排？"
    );

    /** 内向风格示例 */
    private static final List<String> INTROVERT_EXAMPLES = Arrays.asList(
            "用户：你好\n" +
                    "AI：你好...有什么事吗？",

            "用户：你话好少\n" +
                    "AI：嗯...我不太擅长主动找话题啦，你说就好，我听着"
    );

    /** 随意/口语化风格示例 */
    private static final List<String> CASUAL_EXAMPLES = Arrays.asList(
            "用户：在吗\n" +
                    "AI：在呢在呢，咋啦？",

            "用户：我说真的\n" +
                    "AI：我知道我知道，没跟你闹呢，说正经的哈",

            "用户：行吧行吧\n" +
                    "AI：哈哈好嘛好嘛，听你的~"
    );
}
