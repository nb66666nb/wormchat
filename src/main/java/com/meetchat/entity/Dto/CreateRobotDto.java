package com.meetchat.entity.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建机器人DTO
 * 对应前端机器人创建面板的所有参数
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateRobotDto implements Serializable {
    private static final long serialVersionUID = 1L;

    // ===== 基础配置 =====

    /**
     * 机器人类型: FUNCTIONAL/COMPANION/HYBRID
     */
    private String category;

    /**
     * 机器人名称（必填）
     */
    private String botName;

    /**
     * 机器人头像路径
     */
    private String botAvatarPath;

    /**
     * 机器人描述（必填）
     */
    private String botDescription;

    /**
     * 系统提示词（可选）
     */
    private String botSystemPrompt;

    /**
     * 欢迎语
     */
    private String botWelcomeMsg;

    /**
     * 关联模板ID（可选）
     */
    private String templateId;

    // ===== 性格调参 =====

    /**
     * 开放度 0-1
     */
    private BigDecimal openness;

    /**
     * 外向度 0-1
     */
    private BigDecimal extraversion;

    /**
     * 亲和度 0-1
     */
    private BigDecimal agreeableness;

    /**
     * 幽默度 0-1
     */
    private BigDecimal humor;

    // ===== 陪伴型专属 =====

    /**
     * 共情能力 0-1
     */
    private BigDecimal empathyLevel;

    /**
     * 情感敏感度 0-1
     */
    private BigDecimal emotionalSensitivity;

    /**
     * 主动性 0-1
     */
    private BigDecimal proactivity;

    /**
     * 启用定时问候
     */
    private Boolean greetingEnabled;

    /**
     * 启用主动关心
     */
    private Boolean proactiveCareEnabled;

    /**
     * 启用话题跟进
     */
    private Boolean followUpEnabled;

    // ===== 功能配置 =====

    /**
     * 会议查询工具
     */
    private Boolean toolMeetingQuery;

    /**
     * 日程管理工具
     */
    private Boolean toolScheduleManage;

    /**
     * 数据分析工具
     */
    private Boolean toolDataAnalysis;

    // ===== Getter & Setter =====

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBotName() {
        return botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getBotAvatarPath() {
        return botAvatarPath;
    }

    public void setBotAvatarPath(String botAvatarPath) {
        this.botAvatarPath = botAvatarPath;
    }

    public String getBotDescription() {
        return botDescription;
    }

    public void setBotDescription(String botDescription) {
        this.botDescription = botDescription;
    }

    public String getBotSystemPrompt() {
        return botSystemPrompt;
    }

    public void setBotSystemPrompt(String botSystemPrompt) {
        this.botSystemPrompt = botSystemPrompt;
    }

    public String getBotWelcomeMsg() {
        return botWelcomeMsg;
    }

    public void setBotWelcomeMsg(String botWelcomeMsg) {
        this.botWelcomeMsg = botWelcomeMsg;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public BigDecimal getOpenness() {
        return openness;
    }

    public void setOpenness(BigDecimal openness) {
        this.openness = openness;
    }

    public BigDecimal getExtraversion() {
        return extraversion;
    }

    public void setExtraversion(BigDecimal extraversion) {
        this.extraversion = extraversion;
    }

    public BigDecimal getAgreeableness() {
        return agreeableness;
    }

    public void setAgreeableness(BigDecimal agreeableness) {
        this.agreeableness = agreeableness;
    }

    public BigDecimal getHumor() {
        return humor;
    }

    public void setHumor(BigDecimal humor) {
        this.humor = humor;
    }

    public BigDecimal getEmpathyLevel() {
        return empathyLevel;
    }

    public void setEmpathyLevel(BigDecimal empathyLevel) {
        this.empathyLevel = empathyLevel;
    }

    public BigDecimal getEmotionalSensitivity() {
        return emotionalSensitivity;
    }

    public void setEmotionalSensitivity(BigDecimal emotionalSensitivity) {
        this.emotionalSensitivity = emotionalSensitivity;
    }

    public BigDecimal getProactivity() {
        return proactivity;
    }

    public void setProactivity(BigDecimal proactivity) {
        this.proactivity = proactivity;
    }

    public Boolean getGreetingEnabled() {
        return greetingEnabled;
    }

    public void setGreetingEnabled(Boolean greetingEnabled) {
        this.greetingEnabled = greetingEnabled;
    }

    public Boolean getProactiveCareEnabled() {
        return proactiveCareEnabled;
    }

    public void setProactiveCareEnabled(Boolean proactiveCareEnabled) {
        this.proactiveCareEnabled = proactiveCareEnabled;
    }

    public Boolean getFollowUpEnabled() {
        return followUpEnabled;
    }

    public void setFollowUpEnabled(Boolean followUpEnabled) {
        this.followUpEnabled = followUpEnabled;
    }

    public Boolean getToolMeetingQuery() {
        return toolMeetingQuery;
    }

    public void setToolMeetingQuery(Boolean toolMeetingQuery) {
        this.toolMeetingQuery = toolMeetingQuery;
    }

    public Boolean getToolScheduleManage() {
        return toolScheduleManage;
    }

    public void setToolScheduleManage(Boolean toolScheduleManage) {
        this.toolScheduleManage = toolScheduleManage;
    }

    public Boolean getToolDataAnalysis() {
        return toolDataAnalysis;
    }

    public void setToolDataAnalysis(Boolean toolDataAnalysis) {
        this.toolDataAnalysis = toolDataAnalysis;
    }

    /**
     * 构建personality字符串（存入chat_session.bot_personality）
     * 格式: openness:0.5;extraversion:0.5;agreeableness:0.5;humor:0.5;empathy:0.8;...
     */
    public String buildPersonalityString() {
        StringBuilder sb = new StringBuilder();
        if (openness != null) sb.append("openness:").append(openness).append(";");
        if (extraversion != null) sb.append("extraversion:").append(extraversion).append(";");
        if (agreeableness != null) sb.append("agreeableness:").append(agreeableness).append(";");
        if (humor != null) sb.append("humor:").append(humor).append(";");

        if ("COMPANION".equals(category) || "HYBRID".equals(category)) {
            if (empathyLevel != null) sb.append("empathy:").append(empathyLevel).append(";");
            if (emotionalSensitivity != null) sb.append("sensitivity:").append(emotionalSensitivity).append(";");
            if (proactivity != null) sb.append("proactivity:").append(proactivity).append(";");
            if (greetingEnabled != null) sb.append("greeting:").append(greetingEnabled).append(";");
            if (proactiveCareEnabled != null) sb.append("proactiveCare:").append(proactiveCareEnabled).append(";");
            if (followUpEnabled != null) sb.append("followUp:").append(followUpEnabled).append(";");
        }

        if ("FUNCTIONAL".equals(category) || "HYBRID".equals(category)) {
            StringBuilder tools = new StringBuilder();
            if (Boolean.TRUE.equals(toolMeetingQuery)) tools.append("meetingQuery,");
            if (Boolean.TRUE.equals(toolScheduleManage)) tools.append("scheduleManage,");
            if (Boolean.TRUE.equals(toolDataAnalysis)) tools.append("dataAnalysis,");
            if (tools.length() > 0) {
                tools.setLength(tools.length() - 1); // 去掉末尾逗号
                sb.append("tools:").append(tools).append(";");
            }
        }

        // 去掉末尾分号
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ';') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }
}