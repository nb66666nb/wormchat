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
public class BotUserProfile implements Serializable {


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
	private String preferredLanguage;

	/**
	 * 
	 */
	private String communicationStyle;

	/**
	 * 
	 */
	private String preferredResponseLength;

	/**
	 * 兴趣偏好
	 */
	private String interests;

	/**
	 * 关键事实
	 */
	private String keyFacts;

	/**
	 * 
	 */
	private String emotionBaseline;

	/**
	 * 情绪日志
	 */
	private String emotionJournal;

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

	/**
	 * 
	 */
	private Long lastInteractionTime;

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

	public void setPreferredLanguage(String preferredLanguage){
		this.preferredLanguage = preferredLanguage;
	}

	public String getPreferredLanguage(){
		return this.preferredLanguage;
	}

	public void setCommunicationStyle(String communicationStyle){
		this.communicationStyle = communicationStyle;
	}

	public String getCommunicationStyle(){
		return this.communicationStyle;
	}

	public void setPreferredResponseLength(String preferredResponseLength){
		this.preferredResponseLength = preferredResponseLength;
	}

	public String getPreferredResponseLength(){
		return this.preferredResponseLength;
	}

	public void setInterests(String interests){
		this.interests = interests;
	}

	public String getInterests(){
		return this.interests;
	}

	public void setKeyFacts(String keyFacts){
		this.keyFacts = keyFacts;
	}

	public String getKeyFacts(){
		return this.keyFacts;
	}

	public void setEmotionBaseline(String emotionBaseline){
		this.emotionBaseline = emotionBaseline;
	}

	public String getEmotionBaseline(){
		return this.emotionBaseline;
	}

	public void setEmotionJournal(String emotionJournal){
		this.emotionJournal = emotionJournal;
	}

	public String getEmotionJournal(){
		return this.emotionJournal;
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

	public void setLastInteractionTime(Long lastInteractionTime){
		this.lastInteractionTime = lastInteractionTime;
	}

	public Long getLastInteractionTime(){
		return this.lastInteractionTime;
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
		return "id:"+(id == null ? "空" : id)+"，userId:"+(userId == null ? "空" : userId)+"，botId:"+(botId == null ? "空" : botId)+"，preferredLanguage:"+(preferredLanguage == null ? "空" : preferredLanguage)+"，communicationStyle:"+(communicationStyle == null ? "空" : communicationStyle)+"，preferredResponseLength:"+(preferredResponseLength == null ? "空" : preferredResponseLength)+"，兴趣偏好:"+(interests == null ? "空" : interests)+"，关键事实:"+(keyFacts == null ? "空" : keyFacts)+"，emotionBaseline:"+(emotionBaseline == null ? "空" : emotionBaseline)+"，情绪日志:"+(emotionJournal == null ? "空" : emotionJournal)+"，stressLevel:"+(stressLevel == null ? "空" : stressLevel)+"，lonelinessLevel:"+(lonelinessLevel == null ? "空" : lonelinessLevel)+"，supportPreference:"+(supportPreference == null ? "空" : supportPreference)+"，relationshipLevel:"+(relationshipLevel == null ? "空" : relationshipLevel)+"，relationshipTrust:"+(relationshipTrust == null ? "空" : relationshipTrust)+"，interactionCount:"+(interactionCount == null ? "空" : interactionCount)+"，totalChatDuration:"+(totalChatDuration == null ? "空" : totalChatDuration)+"，lastMoodCheckTime:"+(lastMoodCheckTime == null ? "空" : lastMoodCheckTime)+"，consecutiveNegativeDays:"+(consecutiveNegativeDays == null ? "空" : consecutiveNegativeDays)+"，crisisFlag:"+(crisisFlag == null ? "空" : crisisFlag)+"，关怀计划:"+(carePlan == null ? "空" : carePlan)+"，lastInteractionTime:"+(lastInteractionTime == null ? "空" : lastInteractionTime)+"，createTime:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，updateTime:"+(updateTime == null ? "空" : DateUtil.format(updateTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}