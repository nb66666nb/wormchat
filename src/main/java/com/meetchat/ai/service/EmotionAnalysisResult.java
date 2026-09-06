package com.meetchat.ai.service;

import java.math.BigDecimal;

/**
 * 细粒度情绪分析结果
 * 由LLM分析生成，包含主情绪、次情绪、强度、触发点、建议回复风格等
 */
public class EmotionAnalysisResult {

    /** 主要情绪类型 */
    private String primaryEmotion;

    /** 情绪强度 0-1 */
    private BigDecimal intensity;

    /** 次要情绪类型（可空） */
    private String secondaryEmotion;

    /** 情绪触发点（简短描述） */
    private String trigger;

    /** 建议回复风格：empathize/comfort/encourage/listen/normal */
    private String suggestedResponseStyle;

    /** 分析置信度 0-1 */
    private BigDecimal confidence;

    /** 情绪效价：positive/negative/neutral */
    private String valence;

    /** 用户开放度 0-1（从对话中感知用户是否愿意深入聊） */
    private BigDecimal userOpenness;

    public String getPrimaryEmotion() {
        return primaryEmotion;
    }

    public void setPrimaryEmotion(String primaryEmotion) {
        this.primaryEmotion = primaryEmotion;
    }

    public BigDecimal getIntensity() {
        return intensity;
    }

    public void setIntensity(BigDecimal intensity) {
        this.intensity = intensity;
    }

    public String getSecondaryEmotion() {
        return secondaryEmotion;
    }

    public void setSecondaryEmotion(String secondaryEmotion) {
        this.secondaryEmotion = secondaryEmotion;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getSuggestedResponseStyle() {
        return suggestedResponseStyle;
    }

    public void setSuggestedResponseStyle(String suggestedResponseStyle) {
        this.suggestedResponseStyle = suggestedResponseStyle;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getValence() {
        return valence;
    }

    public void setValence(String valence) {
        this.valence = valence;
    }

    public BigDecimal getUserOpenness() {
        return userOpenness;
    }

    public void setUserOpenness(BigDecimal userOpenness) {
        this.userOpenness = userOpenness;
    }

    @Override
    public String toString() {
        return "EmotionAnalysisResult{" +
                "primaryEmotion='" + primaryEmotion + '\'' +
                ", intensity=" + intensity +
                ", secondaryEmotion='" + secondaryEmotion + '\'' +
                ", valence='" + valence + '\'' +
                ", suggestedStyle='" + suggestedResponseStyle + '\'' +
                ", confidence=" + confidence +
                '}';
    }
}
