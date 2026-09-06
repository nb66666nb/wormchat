package com.meetchat.entity.query;



/**
 * IM即时通讯消息表参数
 */
public class ChatImMessageQuery extends BaseParam {


	/**
	 * 主键ID
	 */
	private Long messageId;

	/**
	 * 所属会话ID
	 */
	private String sessionId;

	private String sessionIdFuzzy;

	/**
	 * 发送人用户ID
	 */
	private String sendUserId;

	private String sendUserIdFuzzy;

	/**
	 * 发送者显示名
	 */
	private String sendUserName;

	private String sendUserNameFuzzy;

	/**
	 * 发送者头像路径
	 */
	private String sendUserAvatar;

	private String sendUserAvatarFuzzy;

	/**
	 * 接收人用户ID（私聊时填充）
	 */
	private String receiveUserId;

	private String receiveUserIdFuzzy;

	/**
	 * 消息类型:30文字/32文件/33图片/34视频/35音频/36表情
	 */
	private Integer messageType;

	/**
	 * 消息内容（文字/JSON）
	 */
	private String messageContent;

	private String messageContentFuzzy;

	/**
	 * 文件名
	 */
	private String fileName;

	private String fileNameFuzzy;

	/**
	 * 文件大小(字节)
	 */
	private Long fileSize;

	/**
	 * 文件ID
	 */
	private String fileId;

	private String fileIdFuzzy;

	/**
	 * 文件存储路径
	 */
	private String filePath;

	private String filePathFuzzy;

	/**
	 * MIME类型
	 */
	private String contentType;

	private String contentTypeFuzzy;

	/**
	 * 文件类型标识
	 */
	private Integer fileType;

	/**
	 * 发送类型:0私聊 1群发
	 */
	private Integer messageSendType;

	/**
	 * 状态:0发送中 1已发送
	 */
	private Integer status;

	/**
	 * 发送时间戳(ms)
	 */
	private Long sendTime;

	/**
	 * 前端生成的唯一消息ID（用于幂等去重）
	 */
	private String messageOnlyId;

	private String messageOnlyIdFuzzy;

	/**
	 * 送达状态: 0=待送达 1=已送达 2=已读
	 */
	private Integer deliveryStatus;


	public void setMessageId(Long messageId){
		this.messageId = messageId;
	}

	public Long getMessageId(){
		return this.messageId;
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

	public void setSendUserId(String sendUserId){
		this.sendUserId = sendUserId;
	}

	public String getSendUserId(){
		return this.sendUserId;
	}

	public void setSendUserIdFuzzy(String sendUserIdFuzzy){
		this.sendUserIdFuzzy = sendUserIdFuzzy;
	}

	public String getSendUserIdFuzzy(){
		return this.sendUserIdFuzzy;
	}

	public void setSendUserName(String sendUserName){
		this.sendUserName = sendUserName;
	}

	public String getSendUserName(){
		return this.sendUserName;
	}

	public void setSendUserNameFuzzy(String sendUserNameFuzzy){
		this.sendUserNameFuzzy = sendUserNameFuzzy;
	}

	public String getSendUserNameFuzzy(){
		return this.sendUserNameFuzzy;
	}

	public void setSendUserAvatar(String sendUserAvatar){
		this.sendUserAvatar = sendUserAvatar;
	}

	public String getSendUserAvatar(){
		return this.sendUserAvatar;
	}

	public void setSendUserAvatarFuzzy(String sendUserAvatarFuzzy){
		this.sendUserAvatarFuzzy = sendUserAvatarFuzzy;
	}

	public String getSendUserAvatarFuzzy(){
		return this.sendUserAvatarFuzzy;
	}

	public void setReceiveUserId(String receiveUserId){
		this.receiveUserId = receiveUserId;
	}

	public String getReceiveUserId(){
		return this.receiveUserId;
	}

	public void setReceiveUserIdFuzzy(String receiveUserIdFuzzy){
		this.receiveUserIdFuzzy = receiveUserIdFuzzy;
	}

	public String getReceiveUserIdFuzzy(){
		return this.receiveUserIdFuzzy;
	}

	public void setMessageType(Integer messageType){
		this.messageType = messageType;
	}

	public Integer getMessageType(){
		return this.messageType;
	}

	public void setMessageContent(String messageContent){
		this.messageContent = messageContent;
	}

	public String getMessageContent(){
		return this.messageContent;
	}

	public void setMessageContentFuzzy(String messageContentFuzzy){
		this.messageContentFuzzy = messageContentFuzzy;
	}

	public String getMessageContentFuzzy(){
		return this.messageContentFuzzy;
	}

	public void setFileName(String fileName){
		this.fileName = fileName;
	}

	public String getFileName(){
		return this.fileName;
	}

	public void setFileNameFuzzy(String fileNameFuzzy){
		this.fileNameFuzzy = fileNameFuzzy;
	}

	public String getFileNameFuzzy(){
		return this.fileNameFuzzy;
	}

	public void setFileSize(Long fileSize){
		this.fileSize = fileSize;
	}

	public Long getFileSize(){
		return this.fileSize;
	}

	public void setFileId(String fileId){
		this.fileId = fileId;
	}

	public String getFileId(){
		return this.fileId;
	}

	public void setFileIdFuzzy(String fileIdFuzzy){
		this.fileIdFuzzy = fileIdFuzzy;
	}

	public String getFileIdFuzzy(){
		return this.fileIdFuzzy;
	}

	public void setFilePath(String filePath){
		this.filePath = filePath;
	}

	public String getFilePath(){
		return this.filePath;
	}

	public void setFilePathFuzzy(String filePathFuzzy){
		this.filePathFuzzy = filePathFuzzy;
	}

	public String getFilePathFuzzy(){
		return this.filePathFuzzy;
	}

	public void setContentType(String contentType){
		this.contentType = contentType;
	}

	public String getContentType(){
		return this.contentType;
	}

	public void setContentTypeFuzzy(String contentTypeFuzzy){
		this.contentTypeFuzzy = contentTypeFuzzy;
	}

	public String getContentTypeFuzzy(){
		return this.contentTypeFuzzy;
	}

	public void setFileType(Integer fileType){
		this.fileType = fileType;
	}

	public Integer getFileType(){
		return this.fileType;
	}

	public void setMessageSendType(Integer messageSendType){
		this.messageSendType = messageSendType;
	}

	public Integer getMessageSendType(){
		return this.messageSendType;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setSendTime(Long sendTime){
		this.sendTime = sendTime;
	}

	public Long getSendTime(){
		return this.sendTime;
	}

	public void setMessageOnlyId(String messageOnlyId){
		this.messageOnlyId = messageOnlyId;
	}

	public String getMessageOnlyId(){
		return this.messageOnlyId;
	}

	public void setMessageOnlyIdFuzzy(String messageOnlyIdFuzzy){
		this.messageOnlyIdFuzzy = messageOnlyIdFuzzy;
	}

	public String getMessageOnlyIdFuzzy(){
		return this.messageOnlyIdFuzzy;
	}

	public void setDeliveryStatus(Integer deliveryStatus){
		this.deliveryStatus = deliveryStatus;
	}

	public Integer getDeliveryStatus(){
		return this.deliveryStatus;
	}

}