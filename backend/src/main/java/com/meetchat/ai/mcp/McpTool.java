package com.meetchat.ai.mcp;

/**
 * MCP工具定义接口
 * Model Context Protocol - 工具抽象
 * 每个MCP工具需要实现此接口并注册到McpToolRegistry
 */
public interface McpTool {

    /**
     * 工具唯一名称
     */
    String getName();

    /**
     * 工具描述（LLM根据此描述决定是否调用）
     */
    String getDescription();

    /**
     * 工具参数描述（JSON Schema格式）
     * 示例: {"type":"object","properties":{"query":{"type":"string","description":"搜索关键词"}},"required":["query"]}
     */
    String getParameterSchema();

    /**
     * 执行工具
     * @param argumentsJson 参数JSON字符串
     * @return 执行结果
     */
    String execute(String argumentsJson);
}