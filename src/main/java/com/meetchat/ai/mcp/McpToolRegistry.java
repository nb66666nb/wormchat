package com.meetchat.ai.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP工具注册中心
 * 管理所有可用的MCP工具，提供注册、查询和执行能力
 */
@Component("mcpToolRegistry")
public class McpToolRegistry {

    private static final Logger logger = LoggerFactory.getLogger(McpToolRegistry.class);

    private final Map<String, McpTool> toolMap = new ConcurrentHashMap<>();

    /**
     * 注册工具
     */
    public void register(McpTool tool) {
        if (tool != null && tool.getName() != null) {
            toolMap.put(tool.getName(), tool);
            logger.info("注册MCP工具: {}", tool.getName());
        }
    }

    /**
     * 批量注册工具
     */
    public void registerAll(List<McpTool> tools) {
        if (tools != null) {
            tools.forEach(this::register);
        }
    }

    /**
     * 获取工具
     */
    public McpTool getTool(String name) {
        return toolMap.get(name);
    }

    /**
     * 执行工具
     * @param name 工具名称
     * @param argumentsJson 参数JSON
     * @return 执行结果
     */
    public String executeTool(String name, String argumentsJson) {
        McpTool tool = toolMap.get(name);
        if (tool == null) {
            logger.warn("MCP工具不存在: {}", name);
            return "错误：工具 " + name + " 不存在";
        }
        try {
            logger.info("执行MCP工具: name={}, args={}", name, argumentsJson);
            String result = tool.execute(argumentsJson);
            logger.info("MCP工具执行完成: name={}, resultLength={}", name, result != null ? result.length() : 0);
            return result;
        } catch (Exception e) {
            logger.error("MCP工具执行异常: name={}", name, e);
            return "工具执行异常: " + e.getMessage();
        }
    }

    /**
     * 获取所有已注册工具
     */
    public List<McpTool> getAllTools() {
        return new ArrayList<>(toolMap.values());
    }

    /**
     * 生成所有工具的描述文本（供LLM理解可用工具）
     */
    public String generateToolsDescription() {
        StringBuilder sb = new StringBuilder();
        for (McpTool tool : toolMap.values()) {
            sb.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
            sb.append("  参数: ").append(tool.getParameterSchema()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 检查是否有可用工具
     */
    public boolean hasTools() {
        return !toolMap.isEmpty();
    }
}