package com.meetchat.entity.query;

import java.util.Date;


/**
 * 会议聊天记录表参数
 */
public class ChatMessageQuery extends BaseParam {


	/**
	 * 主键ID
	 */
	private Long messageId;

	/**
	 * 会议ID
	 */
	private String meetingId;

	private String meetingIdFuzzy;

	/**
	 * 会议号
	 */
	private String meetingNo;

	private String meetingNoFuzzy;

	/**
	 * 消息发送类型
	 */
	private Integer messageSendType;

	/**
	 * 发送人用户ID
	 */
	private String sendUserId;

	private String sendUserIdFuzzy;

	/**
	 * 发送人昵称
	 */
	private String sendUserNickName;

	private String sendUserNickNameFuzzy;

	/**
	 * 接收人用户ID
	 */
	private String receiveUserId;

	private String receiveUserIdFuzzy;

	/**
	 * 消息类型：1-文字 2-文件 3-图片 4-视频
	 */
	private Integer messageType;

	/**
	 * 消息内容
	 */
	private String messageContent;

	private String messageContentFuzzy;

	/**
	 * 文件大小（字节）
	 */
	private Long fileSize;

	/**
	 * 文件名
	 */
	private String fileName;

	private String fileNameFuzzy;

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
	 * 文件MIME类型
	 */
	private String contentType;

	private String contentTypeFuzzy;

	/**
	 * 文件类型标识
	 */
	private Integer fileType;

	/**
	 * 扩展数据JSON
	 */
	private String extendData;

	private String extendDataFuzzy;

	/**
	 * 消息状态：0-发送中 1-已发送
	 */
	private Integer status;

	/**
	 * 发送时间戳（毫秒）
	 */
	private Long sendTime;

	/**
	 * 记录创建时间
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 当前用户ID（用于过滤私聊消息：只返回自己参与的私聊 + 所有群聊）
	 */
	private String currentUserId;


	public void setMessageId(Long messageId){
		this.messageId = messageId;
	}

	public Long getMessageId(){
		return this.messageId;
	}

	public void setMeetingId(String meetingId){
		this.meetingId = meetingId;
	}

	public String getMeetingId(){
		return this.meetingId;
	}

	public void setMeetingIdFuzzy(String meetingIdFuzzy){
		this.meetingIdFuzzy = meetingIdFuzzy;
	}

	public String getMeetingIdFuzzy(){
		return this.meetingIdFuzzy;
	}

	public void setMeetingNo(String meetingNo){
		this.meetingNo = meetingNo;
	}

	public String getMeetingNo(){
		return this.meetingNo;
	}

	public void setMeetingNoFuzzy(String meetingNoFuzzy){
		this.meetingNoFuzzy = meetingNoFuzzy;
	}

	public String getMeetingNoFuzzy(){
		return this.meetingNoFuzzy;
	}

	public void setMessageSendType(Integer messageSendType){
		this.messageSendType = messageSendType;
	}

	public Integer getMessageSendType(){
		return this.messageSendType;
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

	public void setSendUserNickName(String sendUserNickName){
		this.sendUserNickName = sendUserNickName;
	}

	public String getSendUserNickName(){
		return this.sendUserNickName;
	}

	public void setSendUserNickNameFuzzy(String sendUserNickNameFuzzy){
		this.sendUserNickNameFuzzy = sendUserNickNameFuzzy;
	}

	public String getSendUserNickNameFuzzy(){
		return this.sendUserNickNameFuzzy;
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

	public void setFileSize(Long fileSize){
		this.fileSize = fileSize;
	}

	public Long getFileSize(){
		return this.fileSize;
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

	public void setExtendData(String extendData){
		this.extendData = extendData;
	}

	public String getExtendData(){
		return this.extendData;
	}

	public void setExtendDataFuzzy(String extendDataFuzzy){
		this.extendDataFuzzy = extendDataFuzzy;
	}

	public String getExtendDataFuzzy(){
		return this.extendDataFuzzy;
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

	public void setCurrentUserId(String currentUserId){
		this.currentUserId = currentUserId;
	}

	public String getCurrentUserId(){
		return this.currentUserId;
	}

}