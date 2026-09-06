package com.meetchat.entity.query;

import java.math.BigDecimal;
import java.util.Date;


/**
 * 参数
 */
public class BotConversationSummaryQuery extends BaseParam {


	/**
	 * 
	 */
	private Long id;

	/**
	 * 
	 */
	private String sessionId;

	private String sessionIdFuzzy;

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
	private String summary;

	private String summaryFuzzy;

	/**
	 * 关键话题
	 */
	private String keyTopics;

	private String keyTopicsFuzzy;

	/**
	 * 用户意图
	 */
	private String userIntents;

	private String userIntentsFuzzy;

	/**
	 * 
	 */
	private String emotionStart;

	private String emotionStartFuzzy;

	/**
	 * 
	 */
	private String emotionEnd;

	private String emotionEndFuzzy;

	/**
	 * 
	 */
	private String emotionTrend;

	private String emotionTrendFuzzy;

	/**
	 * 情绪触发点
	 */
	private String emotionTriggers;

	private String emotionTriggersFuzzy;

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

	private String followUpTopicFuzzy;

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
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;


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

	public void setSessionIdFuzzy(String sessionIdFuzzy){
		this.sessionIdFuzzy = sessionIdFuzzy;
	}

	public String getSessionIdFuzzy(){
		return this.sessionIdFuzzy;
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

	public void setSummary(String summary){
		this.summary = summary;
	}

	public String getSummary(){
		return this.summary;
	}

	public void setSummaryFuzzy(String summaryFuzzy){
		this.summaryFuzzy = summaryFuzzy;
	}

	public String getSummaryFuzzy(){
		return this.summaryFuzzy;
	}

	public void setKeyTopics(String keyTopics){
		this.keyTopics = keyTopics;
	}

	public String getKeyTopics(){
		return this.keyTopics;
	}

	public void setKeyTopicsFuzzy(String keyTopicsFuzzy){
		this.keyTopicsFuzzy = keyTopicsFuzzy;
	}

	public String getKeyTopicsFuzzy(){
		return this.keyTopicsFuzzy;
	}

	public void setUserIntents(String userIntents){
		this.userIntents = userIntents;
	}

	public String getUserIntents(){
		return this.userIntents;
	}

	public void setUserIntentsFuzzy(String userIntentsFuzzy){
		this.userIntentsFuzzy = userIntentsFuzzy;
	}

	public String getUserIntentsFuzzy(){
		return this.userIntentsFuzzy;
	}

	public void setEmotionStart(String emotionStart){
		this.emotionStart = emotionStart;
	}

	public String getEmotionStart(){
		return this.emotionStart;
	}

	public void setEmotionStartFuzzy(String emotionStartFuzzy){
		this.emotionStartFuzzy = emotionStartFuzzy;
	}

	public String getEmotionStartFuzzy(){
		return this.emotionStartFuzzy;
	}

	public void setEmotionEnd(String emotionEnd){
		this.emotionEnd = emotionEnd;
	}

	public String getEmotionEnd(){
		return this.emotionEnd;
	}

	public void setEmotionEndFuzzy(String emotionEndFuzzy){
		this.emotionEndFuzzy = emotionEndFuzzy;
	}

	public String getEmotionEndFuzzy(){
		return this.emotionEndFuzzy;
	}

	public void setEmotionTrend(String emotionTrend){
		this.emotionTrend = emotionTrend;
	}

	public String getEmotionTrend(){
		return this.emotionTrend;
	}

	public void setEmotionTrendFuzzy(String emotionTrendFuzzy){
		this.emotionTrendFuzzy = emotionTrendFuzzy;
	}

	public String getEmotionTrendFuzzy(){
		return this.emotionTrendFuzzy;
	}

	public void setEmotionTriggers(String emotionTriggers){
		this.emotionTriggers = emotionTriggers;
	}

	public String getEmotionTriggers(){
		return this.emotionTriggers;
	}

	public void setEmotionTriggersFuzzy(String emotionTriggersFuzzy){
		this.emotionTriggersFuzzy = emotionTriggersFuzzy;
	}

	public String getEmotionTriggersFuzzy(){
		return this.emotionTriggersFuzzy;
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

	public void setFollowUpTopicFuzzy(String followUpTopicFuzzy){
		this.followUpTopicFuzzy = followUpTopicFuzzy;
	}

	public String getFollowUpTopicFuzzy(){
		return this.followUpTopicFuzzy;
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