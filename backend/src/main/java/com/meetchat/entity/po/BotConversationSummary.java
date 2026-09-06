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
public class BotConversationSummary implements Serializable {


	/**
	 * 
	 */
	private Long id;

	/**
	 * 
	 */
	private String sessionId;

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
	private String summary;

	/**
	 * 关键话题
	 */
	private String keyTopics;

	/**
	 * 用户意图
	 */
	private String userIntents;

	/**
	 * 
	 */
	private String emotionStart;

	/**
	 * 
	 */
	private String emotionEnd;

	/**
	 * 
	 */
	private String emotionTrend;

	/**
	 * 情绪触发点
	 */
	private String emotionTriggers;

	/**
	 * 
	 */
	private BigDecimal userOpenness;

	/**
	 * 
	 */
	private BigDecimal botEmpathyScore;

	/**
	 * 
	 */
	private Integer followUpNeeded;

	/**
	 * 
	 */
	private String followUpTopic;

	/**
	 * 
	 */
	private Long startTime;

	/**
	 * 
	 */
	private Long endTime;

	/**
	 * 
	 */
	private Integer messageCount;

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

	public void setSessionId(String sessionId){
		this.sessionId = sessionId;
	}

	public String getSessionId(){
		return this.sessionId;
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

	public void setSummary(String summary){
		this.summary = summary;
	}

	public String getSummary(){
		return this.summary;
	}

	public void setKeyTopics(String keyTopics){
		this.keyTopics = keyTopics;
	}

	public String getKeyTopics(){
		return this.keyTopics;
	}

	public void setUserIntents(String userIntents){
		this.userIntents = userIntents;
	}

	public String getUserIntents(){
		return this.userIntents;
	}

	public void setEmotionStart(String emotionStart){
		this.emotionStart = emotionStart;
	}

	public String getEmotionStart(){
		return this.emotionStart;
	}

	public void setEmotionEnd(String emotionEnd){
		this.emotionEnd = emotionEnd;
	}

	public String getEmotionEnd(){
		return this.emotionEnd;
	}

	public void setEmotionTrend(String emotionTrend){
		this.emotionTrend = emotionTrend;
	}

	public String getEmotionTrend(){
		return this.emotionTrend;
	}

	public void setEmotionTriggers(String emotionTriggers){
		this.emotionTriggers = emotionTriggers;
	}

	public String getEmotionTriggers(){
		return this.emotionTriggers;
	}

	public void setUserOpenness(BigDecimal userOpenness){
		this.userOpenness = userOpenness;
	}

	public BigDecimal getUserOpenness(){
		return this.userOpenness;
	}

	public void setBotEmpathyScore(BigDecimal botEmpathyScore){
		this.botEmpathyScore = botEmpathyScore;
	}

	public BigDecimal getBotEmpathyScore(){
		return this.botEmpathyScore;
	}

	public void setFollowUpNeeded(Integer followUpNeeded){
		this.followUpNeeded = followUpNeeded;
	}

	public Integer getFollowUpNeeded(){
		return this.followUpNeeded;
	}

	public void setFollowUpTopic(String followUpTopic){
		this.followUpTopic = followUpTopic;
	}

	public String getFollowUpTopic(){
		return this.followUpTopic;
	}

	public void setStartTime(Long startTime){
		this.startTime = startTime;
	}

	public Long getStartTime(){
		return this.startTime;
	}

	public void setEndTime(Long endTime){
		this.endTime = endTime;
	}

	public Long getEndTime(){
		return this.endTime;
	}

	public void setMessageCount(Integer messageCount){
		this.messageCount = messageCount;
	}

	public Integer getMessageCount(){
		return this.messageCount;
	}

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	@Override
	public String toString (){
		return "id:"+(id == null ? "空" : id)+"，sessionId:"+(sessionId == null ? "空" : sessionId)+"，userId:"+(userId == null ? "空" : userId)+"，botId:"+(botId == null ? "空" : botId)+"，summary:"+(summary == null ? "空" : summary)+"，关键话题:"+(keyTopics == null ? "空" : keyTopics)+"，用户意图:"+(userIntents == null ? "空" : userIntents)+"，emotionStart:"+(emotionStart == null ? "空" : emotionStart)+"，emotionEnd:"+(emotionEnd == null ? "空" : emotionEnd)+"，emotionTrend:"+(emotionTrend == null ? "空" : emotionTrend)+"，情绪触发点:"+(emotionTriggers == null ? "空" : emotionTriggers)+"，userOpenness:"+(userOpenness == null ? "空" : userOpenness)+"，botEmpathyScore:"+(botEmpathyScore == null ? "空" : botEmpathyScore)+"，followUpNeeded:"+(followUpNeeded == null ? "空" : followUpNeeded)+"，followUpTopic:"+(followUpTopic == null ? "空" : followUpTopic)+"，startTime:"+(startTime == null ? "空" : startTime)+"，endTime:"+(endTime == null ? "空" : endTime)+"，messageCount:"+(messageCount == null ? "空" : messageCount)+"，createTime:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}