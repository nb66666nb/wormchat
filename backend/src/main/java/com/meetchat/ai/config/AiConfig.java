package com.meetchat.ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AI模块配置
 * 在application.properties中配置:
 *   ai.llm.api-key=sk-xxx
 *   ai.llm.base-url=https://api.openai.com/v1
 *   ai.llm.model=gpt-4o-mini
 *   ai.llm.max-tokens=2048
 *   ai.llm.temperature=0.7
 *   ai.llm.timeout-seconds=60
 *   ai.rag.enabled=false
 *   ai.mcp.enabled=true
 *   ai.agent.max-iterations=5
 */
@Component("aiConfig")
public class AiConfig {

    @Value("${ai.llm.api-key:}")
    private String apiKey;

    @Value("${ai.llm.base-url:}")
    private String baseUrl;

    @Value("${ai.llm.model:}")
    private String model;

    @Value("${ai.llm.max-tokens:2048}")
    private Integer maxTokens;

    @Value("${ai.llm.temperature:0.7}")
    private Double temperature;

    @Value("${ai.llm.timeout-seconds:60}")
    private Integer timeoutSeconds;

    @Value("${ai.rag.enabled:false}")
    private Boolean ragEnabled;

    @Value("${ai.mcp.enabled:true}")
    private Boolean mcpEnabled;

    @Value("${ai.agent.max-iterations:5}")
    private Integer agentMaxIterations;

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public String getModel() {
        return model;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public Boolean getRagEnabled() {
        return ragEnabled;
    }

    public Boolean getMcpEnabled() {
        return mcpEnabled;
    }

    public Integer getAgentMaxIterations() {
        return agentMaxIterations;
    }
}