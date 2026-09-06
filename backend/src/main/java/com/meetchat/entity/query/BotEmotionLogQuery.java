package com.meetchat.entity.query;

import java.math.BigDecimal;
import java.util.Date;


/**
 * 参数
 */
public class BotEmotionLogQuery extends BaseParam {


	/**
	 * 
	 */
	private Long id;

	/**
	 * 
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 
	 */
	private String botId;

	private String botIdFuzzy;

	/**
	 * 
	 */
	private String sessionId;

	private String sessionIdFuzzy;

	/**
	 * 情绪类型
	 */
	private String emotionType;

	private String emotionTypeFuzzy;

	/**
	 * 强度
	 */
	private BigDecimal emotionIntensity;

	/**
	 * 触发原因
	 */
	private String emotionTrigger;

	private String emotionTriggerFuzzy;

	/**
	 * 
	 */
	private Long sourceMessageId;

	/**
	 * 
	 */
	private String analysisMethod;

	private String analysisMethodFuzzy;

	/**
	 * 
	 */
	private BigDecimal confidence;

	/**
	 * 
	 */
	private String timeOfDay;

	private String timeOfDayFuzzy;

	/**
	 * 
	 */
	private Integer isWeekend;

	/**
	 * 
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;


	public void setId(Long id){
		this.id = id;
	}

	public Long getId(){
		return this.id;
	}

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
	}

	public void setUserIdFuzzy(String userIdFuzzy){
		this.userIdFuzzy = userIdFuzzy;
	}

	public String getUserIdFuzzy(){
		return this.userIdFuzzy;
	}

	public void setBotId(String botId){
		this.botId = botId;
	}

	public String getBotId(){
		return this.botId;
	}

	public void setBotIdFuzzy(String botIdFuzzy){
		this.botIdFuzzy = botIdFuzzy;
	}

	public String getBotIdFuzzy(){
		return this.botIdFuzzy;
	}

	public void setSessionId(String sessionId){
		this.sessionId = sessionId;
	}

	public String getSessionId(){
		return this.sessionId;
	}

	public void setSessionIdFuzzy(String sessionIdFuzzy){
		this.sessionIdFuzzy = sessionIdFuzzy;
	}

	public String getSessionIdFuzzy(){
		return this.sessionIdFuzzy;
	}

	public void setEmotionType(String emotionType){
		this.emotionType = emotionType;
	}

	public String getEmotionType(){
		return this.emotionType;
	}

	public void setEmotionTypeFuzzy(String emotionTypeFuzzy){
		this.emotionTypeFuzzy = emotionTypeFuzzy;
	}

	public String getEmotionTypeFuzzy(){
		return this.emotionTypeFuzzy;
	}

	public void setEmotionIntensity(BigDecimal emotionIntensity){
		this.emotionIntensity = emotionIntensity;
	}

	public BigDecimal getEmotionIntensity(){
		return this.emotionIntensity;
	}

	public void setEmotionTrigger(String emotionTrigger){
		this.emotionTrigger = emotionTrigger;
	}

	public String getEmotionTrigger(){
		return this.emotionTrigger;
	}

	public void setEmotionTriggerFuzzy(String emotionTriggerFuzzy){
		this.emotionTriggerFuzzy = emotionTriggerFuzzy;
	}

	public String getEmotionTriggerFuzzy(){
		return this.emotionTriggerFuzzy;
	}

	public void setSourceMessageId(Long sourceMessageId){
		this.sourceMessageId = sourceMessageId;
	}

	public Long getSourceMessageId(){
		return this.sourceMessageId;
	}

	public void setAnalysisMethod(String analysisMethod){
		this.analysisMethod = analysisMethod;
	}

	public String getAnalysisMethod(){
		return this.analysisMethod;
	}

	public void setAnalysisMethodFuzzy(String analysisMethodFuzzy){
		this.analysisMethodFuzzy = analysisMethodFuzzy;
	}

	public String getAnalysisMethodFuzzy(){
		return this.analysisMethodFuzzy;
	}

	public void setConfidence(BigDecimal confidence){
		this.confidence = confidence;
	}

	public BigDecimal getConfidence(){
		return this.confidence;
	}

	public void setTimeOfDay(String timeOfDay){
		this.timeOfDay = timeOfDay;
	}

	public String getTimeOfDay(){
		return this.timeOfDay;
	}

	public void setTimeOfDayFuzzy(String timeOfDayFuzzy){
		this.timeOfDayFuzzy = timeOfDayFuzzy;
	}

	public String getTimeOfDayFuzzy(){
		return this.timeOfDayFuzzy;
	}

	public void setIsWeekend(Integer isWeekend){
		this.isWeekend = isWeekend;
	}

	public Integer getIsWeekend(){
		return this.isWeekend;
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

}