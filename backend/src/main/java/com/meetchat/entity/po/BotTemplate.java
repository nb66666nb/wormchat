package com.meetchat.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.util.Date;
import com.meetchat.entity.enums.DateTimePatternEnum;
import com.meetchat.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * 
 */
public class BotTemplate implements Serializable {


	/**
	 * 模板ID: T+10位数字
	 */
	private String templateId;

	/**
	 * 模板名称
	 */
	private String templateName;

	/**
	 * 分类: FUNCTIONAL/COMPANION/HYBRID
	 */
	private String templateCategory;

	/**
	 * 默认头像
	 */
	private String defaultAvatarPath;

	/**
	 * 默认系统提示词
	 */
	private String defaultSystemPrompt;

	/**
	 * 默认欢迎语
	 */
	private String defaultWelcomeMsg;

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

	/**
	 * emoji频率
	 */
	private String emojiFrequency;

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
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date updateTime;


	public void setTemplateId(String templateId){
		this.templateId = templateId;
	}

	public String getTemplateId(){
		return this.templateId;
	}

	public void setTemplateName(String templateName){
		this.templateName = templateName;
	}

	public String getTemplateName(){
		return this.templateName;
	}

	public void setTemplateCategory(String templateCategory){
		this.templateCategory = templateCategory;
	}

	public String getTemplateCategory(){
		return this.templateCategory;
	}

	public void setDefaultAvatarPath(String defaultAvatarPath){
		this.defaultAvatarPath = defaultAvatarPath;
	}

	public String getDefaultAvatarPath(){
		return this.defaultAvatarPath;
	}

	public void setDefaultSystemPrompt(String defaultSystemPrompt){
		this.defaultSystemPrompt = defaultSystemPrompt;
	}

	public String getDefaultSystemPrompt(){
		return this.defaultSystemPrompt;
	}

	public void setDefaultWelcomeMsg(String defaultWelcomeMsg){
		this.defaultWelcomeMsg = defaultWelcomeMsg;
	}

	public String getDefaultWelcomeMsg(){
		return this.defaultWelcomeMsg;
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

	public void setEmojiFrequency(String emojiFrequency){
		this.emojiFrequency = emojiFrequency;
	}

	public String getEmojiFrequency(){
		return this.emojiFrequency;
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

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	public void setUpdateTime(Date updateTime){
		this.updateTime = updateTime;
	}

	public Date getUpdateTime(){
		return this.updateTime;
	}

	@Override
	public String toString (){
		return "模板ID: T+10位数字:"+(templateId == null ? "空" : templateId)+"，模板名称:"+(templateName == null ? "空" : templateName)+"，分类: FUNCTIONAL/COMPANION/HYBRID:"+(templateCategory == null ? "空" : templateCategory)+"，默认头像:"+(defaultAvatarPath == null ? "空" : defaultAvatarPath)+"，默认系统提示词:"+(defaultSystemPrompt == null ? "空" : defaultSystemPrompt)+"，默认欢迎语:"+(defaultWelcomeMsg == null ? "空" : defaultWelcomeMsg)+"，开放度:"+(openness == null ? "空" : openness)+"，尽责度:"+(conscientiousness == null ? "空" : conscientiousness)+"，外向度:"+(extraversion == null ? "空" : extraversion)+"，亲和度:"+(agreeableness == null ? "空" : agreeableness)+"，幽默度:"+(humor == null ? "空" : humor)+"，共情能力:"+(empathyLevel == null ? "空" : empathyLevel)+"，情感敏感度:"+(emotionalSensitivity == null ? "空" : emotionalSensitivity)+"，主动性:"+(proactivity == null ? "空" : proactivity)+"，自我暴露度:"+(vulnerabilitySharing == null ? "空" : vulnerabilitySharing)+"，回复风格:"+(responseStyle == null ? "空" : responseStyle)+"，emoji频率:"+(emojiFrequency == null ? "空" : emojiFrequency)+"，定时问候:"+(greetingEnabled == null ? "空" : greetingEnabled)+"，主动关心:"+(proactiveCareEnabled == null ? "空" : proactiveCareEnabled)+"，MCP工具列表:"+(enabledTools == null ? "空" : enabledTools)+"，官方模板:"+(isOfficial == null ? "空" : isOfficial)+"，0禁用 1启用:"+(status == null ? "空" : status)+"，sortOrder:"+(sortOrder == null ? "空" : sortOrder)+"，createTime:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，updateTime:"+(updateTime == null ? "空" : DateUtil.format(updateTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}