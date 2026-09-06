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
public class BotEmotionLog implements Serializable {


	/**
	 * 
	 */
	private Long id;

	/**
	 * 
	 */
	private String userId;

	/**
	 * 
	 */
	private String botId;

	/**
	 * 
	 */
	private String sessionId;

	/**
	 * 情绪类型
	 */
	private String emotionType;

	/**
	 * 强度
	 */
	private BigDecimal emotionIntensity;

	/**
	 * 触发原因
	 */
	private String emotionTrigger;

	/**
	 * 
	 */
	private Long sourceMessageId;

	/**
	 * 
	 */
	private String analysisMethod;

	/**
	 * 
	 */
	private BigDecimal confidence;

	/**
	 * 
	 */
	private String timeOfDay;

	/**
	 * 
	 */
	private Integer isWeekend;

	/**
	 * 
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;


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

	public void setBotId(String botId){
		this.botId = botId;
	}

	public String getBotId(){
		return this.botId;
	}

	public void setSessionId(String sessionId){
		this.sessionId = sessionId;
	}

	public String getSessionId(){
		return this.sessionId;
	}

	public void setEmotionType(String emotionType){
		this.emotionType = emotionType;
	}

	public String getEmotionType(){
		return this.emotionType;
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

	public void setIsWeekend(Integer isWeekend){
		this.isWeekend = isWeekend;
	}

	public Integer getIsWeekend(){
		return this.isWeekend;
	}

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	@Override
	public String toString (){
		return "id:"+(id == null ? "空" : id)+"，userId:"+(userId == null ? "空" : userId)+"，botId:"+(botId == null ? "空" : botId)+"，sessionId:"+(sessionId == null ? "空" : sessionId)+"，情绪类型:"+(emotionType == null ? "空" : emotionType)+"，强度:"+(emotionIntensity == null ? "空" : emotionIntensity)+"，触发原因:"+(emotionTrigger == null ? "空" : emotionTrigger)+"，sourceMessageId:"+(sourceMessageId == null ? "空" : sourceMessageId)+"，analysisMethod:"+(analysisMethod == null ? "空" : analysisMethod)+"，confidence:"+(confidence == null ? "空" : confidence)+"，timeOfDay:"+(timeOfDay == null ? "空" : timeOfDay)+"，isWeekend:"+(isWeekend == null ? "空" : isWeekend)+"，createTime:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}