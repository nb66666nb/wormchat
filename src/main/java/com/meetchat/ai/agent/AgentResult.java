package com.meetchat.ai.agent;

import java.util.List;
import java.util.Map;

/**
 * Agent执行结果
 */
public class AgentResult {

    private String content;
    private boolean toolCalled;
    private String toolName;
    private String toolResult;
    private int iterations;
    private Map<String, Object> metadata;

    public static AgentResult of(String content) {
        AgentResult result = new AgentResult();
        result.setContent(content);
        result.setToolCalled(false);
        return result;
    }

    public static AgentResult toolCall(String toolName, String toolResult, String content) {
        AgentResult result = new AgentResult();
        result.setContent(content);
        result.setToolCalled(true);
        result.setToolName(toolName);
        result.setToolResult(toolResult);
        return result;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isToolCalled() { return toolCalled; }
    public void setToolCalled(boolean toolCalled) { this.toolCalled = toolCalled; }
    public String getToolName() { return toolName; }
    public void setToolName(String toolName) { this.toolName = toolName; }
    public String getToolResult() { return toolResult; }
    public void setToolResult(String toolResult) { this.toolResult = toolResult; }
    public int getIterations() { return iterations; }
    public void setIterations(int iterations) { this.iterations = iterations; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}