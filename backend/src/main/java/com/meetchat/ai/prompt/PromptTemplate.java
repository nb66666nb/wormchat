package com.meetchat.ai.prompt;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词模板引擎
 * 支持 {{variable}} 占位符替换
 *
 * 示例:
 *   PromptTemplate template = new PromptTemplate(
 *       "你是{{role_name}}，{{description}}。请用{{language}}回答。"
 *   );
 *   String result = template.render(Map.of(
 *       "role_name", "会议助手",
 *       "description", "专注于会议效率提升",
 *       "language", "中文"
 *   ));
 *   // -> "你是会议助手，专注于会议效率提升。请用中文回答。"
 */
public class PromptTemplate {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private final String template;

    public PromptTemplate(String template) {
        this.template = template;
    }

    /**
     * 渲染模板，替换所有 {{key}} 为对应值
     * @param variables 变量映射
     * @return 渲染后的字符串
     */
    public String render(Map<String, String> variables) {
        if (template == null) {
            return "";
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = variables != null && variables.containsKey(key) ? variables.get(key) : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 获取原始模板
     */
    public String getTemplate() {
        return template;
    }

    @Override
    public String toString() {
        return template;
    }
}