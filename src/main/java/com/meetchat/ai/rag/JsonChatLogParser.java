package com.meetchat.ai.rag;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 聊天记录 JSON 解析器
 * 将微信/QQ 导出的聊天记录 JSON 转为可检索的文本
 *
 * 支持格式:
 * [
 *   {"sender":"张三","content":"明天开会吗？","time":"2024-01-15 14:30:00"},
 *   {"sender":"李四","content":"开，下午3点","time":"2024-01-15 14:31:00"}
 * ]
 */
public class JsonChatLogParser {

    private static final Logger logger = LoggerFactory.getLogger(JsonChatLogParser.class);

    /**
     * 判断是否为聊天记录格式的 JSON
     */
    public boolean isChatLog(String jsonText) {
        try {
            JSONArray arr = JSON.parseArray(jsonText);
            if (arr == null || arr.isEmpty()) return false;
            JSONObject first = arr.getJSONObject(0);
            // 包含 sender/content 字段视为聊天记录
            return first.containsKey("sender") && first.containsKey("content");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析聊天记录 JSON，拼成可读文本
     */
    public String parse(String jsonText) {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            return "";
        }
        try {
            JSONArray arr = JSON.parseArray(jsonText);
            if (arr == null || arr.isEmpty()) {
                return jsonText;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject msg = arr.getJSONObject(i);
                String time = msg.getString("time");
                String sender = msg.getString("sender");
                String content = msg.getString("content");

                if (sender == null || content == null) continue;

                if (time != null && !time.isEmpty()) {
                    sb.append("[").append(time).append("] ");
                }
                sb.append(sender).append(": ").append(content).append("\n");
            }
            logger.info("聊天记录解析: {} 条消息", arr.size());
            return sb.toString();
        } catch (Exception e) {
            logger.warn("JSON 解析失败，按纯文本处理", e);
            return jsonText;
        }
    }
}