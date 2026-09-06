package com.meetchat.ai.mcp.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.meetchat.ai.mcp.McpTool;
import com.meetchat.service.MeetingInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 会议查询MCP工具
 * 允许AI机器人查询用户的会议信息
 */
@Component
public class MeetingQueryTool implements McpTool {

    private static final Logger logger = LoggerFactory.getLogger(MeetingQueryTool.class);

    @Resource
    private MeetingInfoService meetingInfoService;

    @Override
    public String getName() {
        return "meeting_query";
    }

    @Override
    public String getDescription() {
        return "查询用户的会议信息，包括即将开始的会议、历史会议等";
    }

    @Override
    public String getParameterSchema() {
        return "{\"type\":\"object\",\"properties\":{\"userId\":{\"type\":\"string\",\"description\":\"用户ID\"},\"status\":{\"type\":\"string\",\"description\":\"会议状态：upcoming-即将开始, ongoing-进行中, ended-已结束\"}},\"required\":[\"userId\"]}";
    }

    @Override
    public String execute(String argumentsJson) {
        try {
            JSONObject args = JSON.parseObject(argumentsJson);
            String userId = args.getString("userId");
            String status = args.getString("status");

            if (userId == null || userId.isEmpty()) {
                return "错误：userId参数不能为空";
            }

            // 返回查询结果摘要
            Map<String, Object> result = new HashMap<>();
            result.put("tool", getName());
            result.put("userId", userId);
            result.put("status", status != null ? status : "all");
            result.put("message", "会议查询功能已就绪，等待对接具体查询逻辑");

            return JSON.toJSONString(result);
        } catch (Exception e) {
            logger.error("会议查询工具执行异常", e);
            return "查询失败: " + e.getMessage();
        }
    }
}