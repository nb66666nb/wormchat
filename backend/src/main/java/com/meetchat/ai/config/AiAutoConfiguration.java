package com.meetchat.ai.config;

import com.meetchat.ai.mcp.McpTool;
import com.meetchat.ai.mcp.McpToolRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * AI模块自动配置
 * 启动时自动注册所有MCP工具
 */
@Configuration
public class AiAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(AiAutoConfiguration.class);

    @Resource
    private McpToolRegistry mcpToolRegistry;

    @Autowired(required = false)
    private List<McpTool> mcpTools;

    @PostConstruct
    public void init() {
        if (mcpTools != null && !mcpTools.isEmpty()) {
            mcpToolRegistry.registerAll(mcpTools);
            logger.info("AI模块初始化完成，已注册 {} 个MCP工具", mcpTools.size());
        } else {
            logger.info("AI模块初始化完成，无MCP工具注册");
        }
    }
}