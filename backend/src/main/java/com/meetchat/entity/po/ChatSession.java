package com.meetchat.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;


/**
 * 聊天会话表
 */
public class ChatSession implements Serializable {


	/**
	 * 会话ID（主键）
	 */
	private String sessionId;

	/**
	 * 会话所属用户ID
	 */
	private String userId;

	/**
	 * 对方用户ID（单聊时填充）
	 */
	private String targetUserId;

	/**
	 * 对方昵称
	 */
	private String targetNickName;

	/**
	 * 会话类型：1-单聊 2-群聊
	 */
	private Integer sessionType;

	/**
	 * 机器人分类: FUNCTIONAL/COMPANION/HYBRID
	 */
	private String botCategory;

	/**
	 * 关联模板ID
	 */
	private String templateId;

	/**
	 * 个性化性格参数(覆盖模板默认值)
	 */
	private String botPersonality;

	/**
	 * 机器人头像路径
	 */
	private String botAvatarPath;

	/**
	 * 机器人角色名称
	 */
	private String botName;

	/**
	 * 机器人角色描述
	 */
	private String botDescription;

	/**
	 * 系统提示词
	 */
	private String botSystemPrompt;

	/**
	 * 欢迎语
	 */
	private String botWelcomeMsg;

	/**
	 * 最后一条消息摘要
	 */
	private String lastMessage;

	/**
	 * 最后消息时间戳(ms)
	 */
	private Long lastMessageTime;


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

	public void setTargetUserId(String targetUserId){
		this.targetUserId = targetUserId;
	}

	public String getTargetUserId(){
		return this.targetUserId;
	}

	public void setTargetNickName(String targetNickName){
		this.targetNickName = targetNickName;
	}

	public String getTargetNickName(){
		return this.targetNickName;
	}

	public void setSessionType(Integer sessionType){
		this.sessionType = sessionType;
	}

	public Integer getSessionType(){
		return this.sessionType;
	}

	public void setBotCategory(String botCategory){
		this.botCategory = botCategory;
	}

	public String getBotCategory(){
		return this.botCategory;
	}

	public void setTemplateId(String templateId){
		this.templateId = templateId;
	}

	public String getTemplateId(){
		return this.templateId;
	}

	public void setBotPersonality(String botPersonality){
		this.botPersonality = botPersonality;
	}

	public String getBotPersonality(){
		return this.botPersonality;
	}

	public void setBotAvatarPath(String botAvatarPath){
		this.botAvatarPath = botAvatarPath;
	}

	public String getBotAvatarPath(){
		return this.botAvatarPath;
	}

	public void setBotName(String botName){
		this.botName = botName;
	}

	public String getBotName(){
		return this.botName;
	}

	public void setBotDescription(String botDescription){
		this.botDescription = botDescription;
	}

	public String getBotDescription(){
		return this.botDescription;
	}

	public void setBotSystemPrompt(String botSystemPrompt){
		this.botSystemPrompt = botSystemPrompt;
	}

	public String getBotSystemPrompt(){
		return this.botSystemPrompt;
	}

	public void setBotWelcomeMsg(String botWelcomeMsg){
		this.botWelcomeMsg = botWelcomeMsg;
	}

	public String getBotWelcomeMsg(){
		return this.botWelcomeMsg;
	}

	public void setLastMessage(String lastMessage){
		this.lastMessage = lastMessage;
	}

	public String getLastMessage(){
		return this.lastMessage;
	}

	public void setLastMessageTime(Long lastMessageTime){
		this.lastMessageTime = lastMessageTime;
	}

	public Long getLastMessageTime(){
		return this.lastMessageTime;
	}

	@Override
	public String toString (){
		return "会话ID（主键）:"+(sessionId == null ? "空" : sessionId)+"，会话所属用户ID:"+(userId == null ? "空" : userId)+"，对方用户ID（单聊时填充）:"+(targetUserId == null ? "空" : targetUserId)+"，对方昵称:"+(targetNickName == null ? "空" : targetNickName)+"，会话类型:"+(sessionType == null ? "空" : sessionType)+"，机器人分类:"+(botCategory == null ? "空" : botCategory)+"，模板ID:"+(templateId == null ? "空" : templateId)+"，个性化性格:"+(botPersonality == null ? "空" : botPersonality)+"，机器人头像路径:"+(botAvatarPath == null ? "空" : botAvatarPath)+"，机器人角色名称:"+(botName == null ? "空" : botName)+"，机器人角色描述:"+(botDescription == null ? "空" : botDescription)+"，系统提示词:"+(botSystemPrompt == null ? "空" : botSystemPrompt)+"，欢迎语:"+(botWelcomeMsg == null ? "空" : botWelcomeMsg)+"，最后一条消息摘要:"+(lastMessage == null ? "空" : lastMessage)+"，最后消息时间戳(ms):"+(lastMessageTime == null ? "空" : lastMessageTime);
	}
}