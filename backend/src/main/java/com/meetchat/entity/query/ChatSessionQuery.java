package com.meetchat.entity.query;



/**
 * 聊天会话表参数
 */
public class ChatSessionQuery extends BaseParam {


	/**
	 * 会话ID（主键）
	 */
	private String sessionId;

	private String sessionIdFuzzy;

	/**
	 * 会话所属用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 对方用户ID（单聊时填充）
	 */
	private String targetUserId;

	private String targetUserIdFuzzy;

	/**
	 * 对方昵称
	 */
	private String targetNickName;

	private String targetNickNameFuzzy;

	/**
	 * 会话类型：1-单聊 2-群聊
	 */
	private Integer sessionType;

	/**
	 * 机器人分类: FUNCTIONAL/COMPANION/HYBRID
	 */
	private String botCategory;

	private String botCategoryFuzzy;

	/**
	 * 关联模板ID
	 */
	private String templateId;

	private String templateIdFuzzy;

	/**
	 * 个性化性格参数
	 */
	private String botPersonality;

	private String botPersonalityFuzzy;

	/**
	 * 机器人头像路径
	 */
	private String botAvatarPath;

	private String botAvatarPathFuzzy;

	/**
	 * 机器人角色名称
	 */
	private String botName;

	private String botNameFuzzy;

	/**
	 * 机器人角色描述
	 */
	private String botDescription;

	private String botDescriptionFuzzy;

	/**
	 * 系统提示词
	 */
	private String botSystemPrompt;

	private String botSystemPromptFuzzy;

	/**
	 * 欢迎语
	 */
	private String botWelcomeMsg;

	private String botWelcomeMsgFuzzy;

	/**
	 * 最后一条消息摘要
	 */
	private String lastMessage;

	private String lastMessageFuzzy;

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

	public void setTargetUserId(String targetUserId){
		this.targetUserId = targetUserId;
	}

	public String getTargetUserId(){
		return this.targetUserId;
	}

	public void setTargetUserIdFuzzy(String targetUserIdFuzzy){
		this.targetUserIdFuzzy = targetUserIdFuzzy;
	}

	public String getTargetUserIdFuzzy(){
		return this.targetUserIdFuzzy;
	}

	public void setTargetNickName(String targetNickName){
		this.targetNickName = targetNickName;
	}

	public String getTargetNickName(){
		return this.targetNickName;
	}

	public void setTargetNickNameFuzzy(String targetNickNameFuzzy){
		this.targetNickNameFuzzy = targetNickNameFuzzy;
	}

	public String getTargetNickNameFuzzy(){
		return this.targetNickNameFuzzy;
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

	public void setBotCategoryFuzzy(String botCategoryFuzzy){
		this.botCategoryFuzzy = botCategoryFuzzy;
	}

	public String getBotCategoryFuzzy(){
		return this.botCategoryFuzzy;
	}

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

	public void setBotPersonality(String botPersonality){
		this.botPersonality = botPersonality;
	}

	public String getBotPersonality(){
		return this.botPersonality;
	}

	public void setBotPersonalityFuzzy(String botPersonalityFuzzy){
		this.botPersonalityFuzzy = botPersonalityFuzzy;
	}

	public String getBotPersonalityFuzzy(){
		return this.botPersonalityFuzzy;
	}

	public void setBotAvatarPath(String botAvatarPath){
		this.botAvatarPath = botAvatarPath;
	}

	public String getBotAvatarPath(){
		return this.botAvatarPath;
	}

	public void setBotAvatarPathFuzzy(String botAvatarPathFuzzy){
		this.botAvatarPathFuzzy = botAvatarPathFuzzy;
	}

	public String getBotAvatarPathFuzzy(){
		return this.botAvatarPathFuzzy;
	}

	public void setBotName(String botName){
		this.botName = botName;
	}

	public String getBotName(){
		return this.botName;
	}

	public void setBotNameFuzzy(String botNameFuzzy){
		this.botNameFuzzy = botNameFuzzy;
	}

	public String getBotNameFuzzy(){
		return this.botNameFuzzy;
	}

	public void setBotDescription(String botDescription){
		this.botDescription = botDescription;
	}

	public String getBotDescription(){
		return this.botDescription;
	}

	public void setBotDescriptionFuzzy(String botDescriptionFuzzy){
		this.botDescriptionFuzzy = botDescriptionFuzzy;
	}

	public String getBotDescriptionFuzzy(){
		return this.botDescriptionFuzzy;
	}

	public void setBotSystemPrompt(String botSystemPrompt){
		this.botSystemPrompt = botSystemPrompt;
	}

	public String getBotSystemPrompt(){
		return this.botSystemPrompt;
	}

	public void setBotSystemPromptFuzzy(String botSystemPromptFuzzy){
		this.botSystemPromptFuzzy = botSystemPromptFuzzy;
	}

	public String getBotSystemPromptFuzzy(){
		return this.botSystemPromptFuzzy;
	}

	public void setBotWelcomeMsg(String botWelcomeMsg){
		this.botWelcomeMsg = botWelcomeMsg;
	}

	public String getBotWelcomeMsg(){
		return this.botWelcomeMsg;
	}

	public void setBotWelcomeMsgFuzzy(String botWelcomeMsgFuzzy){
		this.botWelcomeMsgFuzzy = botWelcomeMsgFuzzy;
	}

	public String getBotWelcomeMsgFuzzy(){
		return this.botWelcomeMsgFuzzy;
	}

	public void setLastMessage(String lastMessage){
		this.lastMessage = lastMessage;
	}

	public String getLastMessage(){
		return this.lastMessage;
	}

	public void setLastMessageFuzzy(String lastMessageFuzzy){
		this.lastMessageFuzzy = lastMessageFuzzy;
	}

	public String getLastMessageFuzzy(){
		return this.lastMessageFuzzy;
	}

	public void setLastMessageTime(Long lastMessageTime){
		this.lastMessageTime = lastMessageTime;
	}

	public Long getLastMessageTime(){
		return this.lastMessageTime;
	}

}