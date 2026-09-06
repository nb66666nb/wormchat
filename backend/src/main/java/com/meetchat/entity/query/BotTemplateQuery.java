package com.meetchat.entity.query;

import java.math.BigDecimal;
import java.util.Date;


/**
 * 参数
 */
public class BotTemplateQuery extends BaseParam {


	/**
	 * 模板ID: T+10位数字
	 */
	private String templateId;

	private String templateIdFuzzy;

	/**
	 * 模板名称
	 */
	private String templateName;

	private String templateNameFuzzy;

	/**
	 * 分类: FUNCTIONAL/COMPANION/HYBRID
	 */
	private String templateCategory;

	private String templateCategoryFuzzy;

	/**
	 * 默认头像
	 */
	private String defaultAvatarPath;

	private String defaultAvatarPathFuzzy;

	/**
	 * 默认系统提示词
	 */
	private String defaultSystemPrompt;

	private String defaultSystemPromptFuzzy;

	/**
	 * 默认欢迎语
	 */
	private String defaultWelcomeMsg;

	private String defaultWelcomeMsgFuzzy;

	/**
	 * 开放度
	 */
	private BigDecimal openness;

	/**
	 * 尽责度
	 */
	private BigDecimal conscientiousness;

	/**
	 * 外向度
	 */
	private BigDecimal extraversion;

	/**
	 * 亲和度
	 */
	private BigDecimal agreeableness;

	/**
	 * 幽默度
	 */
	private BigDecimal humor;

	/**
	 * 共情能力
	 */
	private BigDecimal empathyLevel;

	/**
	 * 情感敏感度
	 */
	private BigDecimal emotionalSensitivity;

	/**
	 * 主动性
	 */
	private BigDecimal proactivity;

	/**
	 * 自我暴露度
	 */
	private BigDecimal vulnerabilitySharing;

	/**
	 * 回复风格
	 */
	private String responseStyle;

	private String responseStyleFuzzy;

	/**
	 * emoji频率
	 */
	private String emojiFrequency;

	private String emojiFrequencyFuzzy;

	/**
	 * 定时问候
	 */
	private Integer greetingEnabled;

	/**
	 * 主动关心
	 */
	private Integer proactiveCareEnabled;

	/**
	 * MCP工具列表
	 */
	private String enabledTools;

	private String enabledToolsFuzzy;

	/**
	 * 官方模板
	 */
	private Integer isOfficial;

	/**
	 * 0禁用 1启用
	 */
	private Integer status;

	/**
	 * 
	 */
	private Integer sortOrder;

	/**
	 * 
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 
	 */
	private String updateTime;

	private String updateTimeStart;

	private String updateTimeEnd;


	public void setTemplateId(String templateId){
		this.templateId = templateId;
	}

	public String getTemplateId(){
		return this.templateId;
	}

	public void setTemplateIdFuzzy(String templateIdFuzzy){
		this.templateIdFuzzy = templateIdFuzzy;
	}

	public String getTemplateIdFuzzy(){
		return this.templateIdFuzzy;
	}

	public void setTemplateName(String templateName){
		this.templateName = templateName;
	}

	public String getTemplateName(){
		return this.templateName;
	}

	public void setTemplateNameFuzzy(String templateNameFuzzy){
		this.templateNameFuzzy = templateNameFuzzy;
	}

	public String getTemplateNameFuzzy(){
		return this.templateNameFuzzy;
	}

	public void setTemplateCategory(String templateCategory){
		this.templateCategory = templateCategory;
	}

	public String getTemplateCategory(){
		return this.templateCategory;
	}

	public void setTemplateCategoryFuzzy(String templateCategoryFuzzy){
		this.templateCategoryFuzzy = templateCategoryFuzzy;
	}

	public String getTemplateCategoryFuzzy(){
		return this.templateCategoryFuzzy;
	}

	public void setDefaultAvatarPath(String defaultAvatarPath){
		this.defaultAvatarPath = defaultAvatarPath;
	}

	public String getDefaultAvatarPath(){
		return this.defaultAvatarPath;
	}

	public void setDefaultAvatarPathFuzzy(String defaultAvatarPathFuzzy){
		this.defaultAvatarPathFuzzy = defaultAvatarPathFuzzy;
	}

	public String getDefaultAvatarPathFuzzy(){
		return this.defaultAvatarPathFuzzy;
	}

	public void setDefaultSystemPrompt(String defaultSystemPrompt){
		this.defaultSystemPrompt = defaultSystemPrompt;
	}

	public String getDefaultSystemPrompt(){
		return this.defaultSystemPrompt;
	}

	public void setDefaultSystemPromptFuzzy(String defaultSystemPromptFuzzy){
		this.defaultSystemPromptFuzzy = defaultSystemPromptFuzzy;
	}

	public String getDefaultSystemPromptFuzzy(){
		return this.defaultSystemPromptFuzzy;
	}

	public void setDefaultWelcomeMsg(String defaultWelcomeMsg){
		this.defaultWelcomeMsg = defaultWelcomeMsg;
	}

	public String getDefaultWelcomeMsg(){
		return this.defaultWelcomeMsg;
	}

	public void setDefaultWelcomeMsgFuzzy(String defaultWelcomeMsgFuzzy){
		this.defaultWelcomeMsgFuzzy = defaultWelcomeMsgFuzzy;
	}

	public String getDefaultWelcomeMsgFuzzy(){
		return this.defaultWelcomeMsgFuzzy;
	}

	public void setOpenness(BigDecimal openness){
		this.openness = openness;
	}

	public BigDecimal getOpenness(){
		return this.openness;
	}

	public void setConscientiousness(BigDecimal conscientiousness){
		this.conscientiousness = conscientiousness;
	}

	public BigDecimal getConscientiousness(){
		return this.conscientiousness;
	}

	public void setExtraversion(BigDecimal extraversion){
		this.extraversion = extraversion;
	}

	public BigDecimal getExtraversion(){
		return this.extraversion;
	}

	public void setAgreeableness(BigDecimal agreeableness){
		this.agreeableness = agreeableness;
	}

	public BigDecimal getAgreeableness(){
		return this.agreeableness;
	}

	public void setHumor(BigDecimal humor){
		this.humor = humor;
	}

	public BigDecimal getHumor(){
		return this.humor;
	}

	public void setEmpathyLevel(BigDecimal empathyLevel){
		this.empathyLevel = empathyLevel;
	}

	public BigDecimal getEmpathyLevel(){
		return this.empathyLevel;
	}

	public void setEmotionalSensitivity(BigDecimal emotionalSensitivity){
		this.emotionalSensitivity = emotionalSensitivity;
	}

	public BigDecimal getEmotionalSensitivity(){
		return this.emotionalSensitivity;
	}

	public void setProactivity(BigDecimal proactivity){
		this.proactivity = proactivity;
	}

	public BigDecimal getProactivity(){
		return this.proactivity;
	}

	public void setVulnerabilitySharing(BigDecimal vulnerabilitySharing){
		this.vulnerabilitySharing = vulnerabilitySharing;
	}

	public BigDecimal getVulnerabilitySharing(){
		return this.vulnerabilitySharing;
	}

	public void setResponseStyle(String responseStyle){
		this.responseStyle = responseStyle;
	}

	public String getResponseStyle(){
		return this.responseStyle;
	}

	public void setResponseStyleFuzzy(String responseStyleFuzzy){
		this.responseStyleFuzzy = responseStyleFuzzy;
	}

	public String getResponseStyleFuzzy(){
		return this.responseStyleFuzzy;
	}

	public void setEmojiFrequency(String emojiFrequency){
		this.emojiFrequency = emojiFrequency;
	}

	public String getEmojiFrequency(){
		return this.emojiFrequency;
	}

	public void setEmojiFrequencyFuzzy(String emojiFrequencyFuzzy){
		this.emojiFrequencyFuzzy = emojiFrequencyFuzzy;
	}

	public String getEmojiFrequencyFuzzy(){
		return this.emojiFrequencyFuzzy;
	}

	public void setGreetingEnabled(Integer greetingEnabled){
		this.greetingEnabled = greetingEnabled;
	}

	public Integer getGreetingEnabled(){
		return this.greetingEnabled;
	}

	public void setProactiveCareEnabled(Integer proactiveCareEnabled){
		this.proactiveCareEnabled = proactiveCareEnabled;
	}

	public Integer getProactiveCareEnabled(){
		return this.proactiveCareEnabled;
	}

	public void setEnabledTools(String enabledTools){
		this.enabledTools = enabledTools;
	}

	public String getEnabledTools(){
		return this.enabledTools;
	}

	public void setEnabledToolsFuzzy(String enabledToolsFuzzy){
		this.enabledToolsFuzzy = enabledToolsFuzzy;
	}

	public String getEnabledToolsFuzzy(){
		return this.enabledToolsFuzzy;
	}

	public void setIsOfficial(Integer isOfficial){
		this.isOfficial = isOfficial;
	}

	public Integer getIsOfficial(){
		return this.isOfficial;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setSortOrder(Integer sortOrder){
		this.sortOrder = sortOrder;
	}

	public Integer getSortOrder(){
		return this.sortOrder;
	}

	public void setCreateTime(String createTime){
		this.createTime = createTime;
	}

	public String getCreateTime(){
		return this.createTime;
	}

	public void setCreateTimeStart(String createTimeStart){
		this.createTimeStart = createTimeStart;
	}

	public String getCreateTimeStart(){
		return this.createTimeStart;
	}
	public void setCreateTimeEnd(String createTimeEnd){
		this.createTimeEnd = createTimeEnd;
	}

	public String getCreateTimeEnd(){
		return this.createTimeEnd;
	}

	public void setUpdateTime(String updateTime){
		this.updateTime = updateTime;
	}

	public String getUpdateTime(){
		return this.updateTime;
	}

	public void setUpdateTimeStart(String updateTimeStart){
		this.updateTimeStart = updateTimeStart;
	}

	public String getUpdateTimeStart(){
		return this.updateTimeStart;
	}
	public void setUpdateTimeEnd(String updateTimeEnd){
		this.updateTimeEnd = updateTimeEnd;
	}

	public String getUpdateTimeEnd(){
		return this.updateTimeEnd;
	}

}