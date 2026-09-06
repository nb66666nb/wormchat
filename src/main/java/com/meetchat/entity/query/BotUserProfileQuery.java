package com.meetchat.entity.query;

import java.math.BigDecimal;
import java.util.Date;


/**
 * 参数
 */
public class BotUserProfileQuery extends BaseParam {


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
	private String preferredLanguage;

	private String preferredLanguageFuzzy;

	/**
	 * 
	 */
	private String communicationStyle;

	private String communicationStyleFuzzy;

	/**
	 * 
	 */
	private String preferredResponseLength;

	private String preferredResponseLengthFuzzy;

	/**
	 * 兴趣偏好
	 */
	private String interests;

	private String interestsFuzzy;

	/**
	 * 关键事实
	 */
	private String keyFacts;

	private String keyFactsFuzzy;

	/**
	 * 
	 */
	private String emotionBaseline;

	private String emotionBaselineFuzzy;

	/**
	 * 情绪日志
	 */
	private String emotionJournal;

	private String emotionJournalFuzzy;

	/**
	 * 
	 */
	private BigDecimal stressLevel;

	/**
	 * 
	 */
	private BigDecimal lonelinessLevel;

	/**
	 * 
	 */
	private String supportPreference;

	private String supportPreferenceFuzzy;

	/**
	 * 
	 */
	private Integer relationshipLevel;

	/**
	 * 
	 */
	private BigDecimal relationshipTrust;

	/**
	 * 
	 */
	private Integer interactionCount;

	/**
	 * 
	 */
	private Integer totalChatDuration;

	/**
	 * 
	 */
	private Long lastMoodCheckTime;

	/**
	 * 
	 */
	private Integer consecutiveNegativeDays;

	/**
	 * 
	 */
	private Integer crisisFlag;

	/**
	 * 关怀计划
	 */
	private String carePlan;

	private String carePlanFuzzy;

	/**
	 * 
	 */
	private Long lastInteractionTime;

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

	public void setPreferredLanguage(String preferredLanguage){
		this.preferredLanguage = preferredLanguage;
	}

	public String getPreferredLanguage(){
		return this.preferredLanguage;
	}

	public void setPreferredLanguageFuzzy(String preferredLanguageFuzzy){
		this.preferredLanguageFuzzy = preferredLanguageFuzzy;
	}

	public String getPreferredLanguageFuzzy(){
		return this.preferredLanguageFuzzy;
	}

	public void setCommunicationStyle(String communicationStyle){
		this.communicationStyle = communicationStyle;
	}

	public String getCommunicationStyle(){
		return this.communicationStyle;
	}

	public void setCommunicationStyleFuzzy(String communicationStyleFuzzy){
		this.communicationStyleFuzzy = communicationStyleFuzzy;
	}

	public String getCommunicationStyleFuzzy(){
		return this.communicationStyleFuzzy;
	}

	public void setPreferredResponseLength(String preferredResponseLength){
		this.preferredResponseLength = preferredResponseLength;
	}

	public String getPreferredResponseLength(){
		return this.preferredResponseLength;
	}

	public void setPreferredResponseLengthFuzzy(String preferredResponseLengthFuzzy){
		this.preferredResponseLengthFuzzy = preferredResponseLengthFuzzy;
	}

	public String getPreferredResponseLengthFuzzy(){
		return this.preferredResponseLengthFuzzy;
	}

	public void setInterests(String interests){
		this.interests = interests;
	}

	public String getInterests(){
		return this.interests;
	}

	public void setInterestsFuzzy(String interestsFuzzy){
		this.interestsFuzzy = interestsFuzzy;
	}

	public String getInterestsFuzzy(){
		return this.interestsFuzzy;
	}

	public void setKeyFacts(String keyFacts){
		this.keyFacts = keyFacts;
	}

	public String getKeyFacts(){
		return this.keyFacts;
	}

	public void setKeyFactsFuzzy(String keyFactsFuzzy){
		this.keyFactsFuzzy = keyFactsFuzzy;
	}

	public String getKeyFactsFuzzy(){
		return this.keyFactsFuzzy;
	}

	public void setEmotionBaseline(String emotionBaseline){
		this.emotionBaseline = emotionBaseline;
	}

	public String getEmotionBaseline(){
		return this.emotionBaseline;
	}

	public void setEmotionBaselineFuzzy(String emotionBaselineFuzzy){
		this.emotionBaselineFuzzy = emotionBaselineFuzzy;
	}

	public String getEmotionBaselineFuzzy(){
		return this.emotionBaselineFuzzy;
	}

	public void setEmotionJournal(String emotionJournal){
		this.emotionJournal = emotionJournal;
	}

	public String getEmotionJournal(){
		return this.emotionJournal;
	}

	public void setEmotionJournalFuzzy(String emotionJournalFuzzy){
		this.emotionJournalFuzzy = emotionJournalFuzzy;
	}

	public String getEmotionJournalFuzzy(){
		return this.emotionJournalFuzzy;
	}

	public void setStressLevel(BigDecimal stressLevel){
		this.stressLevel = stressLevel;
	}

	public BigDecimal getStressLevel(){
		return this.stressLevel;
	}

	public void setLonelinessLevel(BigDecimal lonelinessLevel){
		this.lonelinessLevel = lonelinessLevel;
	}

	public BigDecimal getLonelinessLevel(){
		return this.lonelinessLevel;
	}

	public void setSupportPreference(String supportPreference){
		this.supportPreference = supportPreference;
	}

	public String getSupportPreference(){
		return this.supportPreference;
	}

	public void setSupportPreferenceFuzzy(String supportPreferenceFuzzy){
		this.supportPreferenceFuzzy = supportPreferenceFuzzy;
	}

	public String getSupportPreferenceFuzzy(){
		return this.supportPreferenceFuzzy;
	}

	public void setRelationshipLevel(Integer relationshipLevel){
		this.relationshipLevel = relationshipLevel;
	}

	public Integer getRelationshipLevel(){
		return this.relationshipLevel;
	}

	public void setRelationshipTrust(BigDecimal relationshipTrust){
		this.relationshipTrust = relationshipTrust;
	}

	public BigDecimal getRelationshipTrust(){
		return this.relationshipTrust;
	}

	public void setInteractionCount(Integer interactionCount){
		this.interactionCount = interactionCount;
	}

	public Integer getInteractionCount(){
		return this.interactionCount;
	}

	public void setTotalChatDuration(Integer totalChatDuration){
		this.totalChatDuration = totalChatDuration;
	}

	public Integer getTotalChatDuration(){
		return this.totalChatDuration;
	}

	public void setLastMoodCheckTime(Long lastMoodCheckTime){
		this.lastMoodCheckTime = lastMoodCheckTime;
	}

	public Long getLastMoodCheckTime(){
		return this.lastMoodCheckTime;
	}

	public void setConsecutiveNegativeDays(Integer consecutiveNegativeDays){
		this.consecutiveNegativeDays = consecutiveNegativeDays;
	}

	public Integer getConsecutiveNegativeDays(){
		return this.consecutiveNegativeDays;
	}

	public void setCrisisFlag(Integer crisisFlag){
		this.crisisFlag = crisisFlag;
	}

	public Integer getCrisisFlag(){
		return this.crisisFlag;
	}

	public void setCarePlan(String carePlan){
		this.carePlan = carePlan;
	}

	public String getCarePlan(){
		return this.carePlan;
	}

	public void setCarePlanFuzzy(String carePlanFuzzy){
		this.carePlanFuzzy = carePlanFuzzy;
	}

	public String getCarePlanFuzzy(){
		return this.carePlanFuzzy;
	}

	public void setLastInteractionTime(Long lastInteractionTime){
		this.lastInteractionTime = lastInteractionTime;
	}

	public Long getLastInteractionTime(){
		return this.lastInteractionTime;
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