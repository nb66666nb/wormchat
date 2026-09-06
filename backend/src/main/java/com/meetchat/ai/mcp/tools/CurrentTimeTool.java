package com.meetchat.ai.mcp.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meetchat.ai.mcp.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 当前时间MCP工具
 * 允许AI机器人获取当前时间
 */
@Component
public class CurrentTimeTool implements McpTool {

    private static final Logger logger = LoggerFactory.getLogger(CurrentTimeTool.class);

    @Override
    public String getName() {
        return "current_time";
    }

    @Override
    public String getDescription() {
        return "获取当前日期和时间";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"format\":{\"type\":\"string\",\"description\":\"时间格式，默认yyyy-MM-dd HH:mm:ss\"}},\"required\":[]}";
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            String format = "yyyy-MM-dd HH:mm:ss";
            if (argumentsJson != null && !argumentsJson.isEmpty()) {
                JSONObject args = JSON.parseObject(argumentsJson);
                if (args.containsKey("format")) {
                    format = args.getString("format");
                }
            }

            LocalDateTime now = LocalDateTime.now();
            String timeStr = now.format(DateTimeFormatter.ofPattern(format));

            Map<String, Object> result = new HashMap<>();
            result.put("currentTime", timeStr);
            result.put("timestamp", System.currentTimeMillis());

            return JSON.toJSONString(result);
        } catch (Exception e) {
            logger.error("获取时间工具执行异常", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}