package com.meetchat.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;


/**
 * IM即时通讯消息表
 */
public class ChatImMessage implements Serializable {


	/**
	 * 主键ID
	 */
	private Long messageId;

	/**
	 * 所属会话ID
	 */
	private String sessionId;

	/**
	 * 发送人用户ID
	 */
	private String sendUserId;

	/**
	 * 发送者显示名
	 */
	private String sendUserName;

	/**
	 * 发送者头像路径
	 */
	private String sendUserAvatar;

	/**
	 * 接收人用户ID（私聊时填充）
	 */
	private String receiveUserId;

	/**
	 * 消息类型:30文字/32文件/33图片/34视频/35音频/36表情
	 */
	private Integer messageType;

	/**
	 * 消息内容（文字/JSON）
	 */
	private String messageContent;

	/**
	 * 文件名
	 */
	private String fileName;

	/**
	 * 文件大小(字节)
	 */
	private Long fileSize;

	/**
	 * 文件ID
	 */
	private String fileId;

	/**
	 * 文件存储路径
	 */
	private String filePath;

	/**
	 * MIME类型
	 */
	private String contentType;

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

	/**
	 * 送达状态: 0=待送达 1=已送达 2=已读
	 */
	private Integer deliveryStatus;

	/**
	 * 会话内服务端单调递增序号（Redis INCR 生成，用于多端/重连场景顺序校验与空洞检测）
	 */
	private Long sessionSeq;

	/**
	 * 是否已撤回: 0=正常 1=已撤回
	 */
	private Integer recalled;

	/**
	 * 撤回时间戳(ms)
	 */
	private Long recallTime;


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

	public void setSendUserId(String sendUserId){
		this.sendUserId = sendUserId;
	}

	public String getSendUserId(){
		return this.sendUserId;
	}

	public void setSendUserName(String sendUserName){
		this.sendUserName = sendUserName;
	}

	public String getSendUserName(){
		return this.sendUserName;
	}

	public void setSendUserAvatar(String sendUserAvatar){
		this.sendUserAvatar = sendUserAvatar;
	}

	public String getSendUserAvatar(){
		return this.sendUserAvatar;
	}

	public void setReceiveUserId(String receiveUserId){
		this.receiveUserId = receiveUserId;
	}

	public String getReceiveUserId(){
		return this.receiveUserId;
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

	public void setFileName(String fileName){
		this.fileName = fileName;
	}

	public String getFileName(){
		return this.fileName;
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

	public void setFilePath(String filePath){
		this.filePath = filePath;
	}

	public String getFilePath(){
		return this.filePath;
	}

	public void setContentType(String contentType){
		this.contentType = contentType;
	}

	public String getContentType(){
		return this.contentType;
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

	public void setDeliveryStatus(Integer deliveryStatus){
		this.deliveryStatus = deliveryStatus;
	}

	public Integer getDeliveryStatus(){
		return this.deliveryStatus;
	}

	public void setSessionSeq(Long sessionSeq){
		this.sessionSeq = sessionSeq;
	}

	public Long getSessionSeq(){
		return this.sessionSeq;
	}

	public void setRecalled(Integer recalled){
		this.recalled = recalled;
	}

	public Integer getRecalled(){
		return this.recalled;
	}

	public void setRecallTime(Long recallTime){
		this.recallTime = recallTime;
	}

	public Long getRecallTime(){
		return this.recallTime;
	}

	@Override
	public String toString (){
		return "主键ID:"+(messageId == null ? "空" : messageId)+"，所属会话ID:"+(sessionId == null ? "空" : sessionId)+"，发送人用户ID:"+(sendUserId == null ? "空" : sendUserId)+"，发送者显示名:"+(sendUserName == null ? "空" : sendUserName)+"，发送者头像路径:"+(sendUserAvatar == null ? "空" : sendUserAvatar)+"，接收人用户ID（私聊时填充）:"+(receiveUserId == null ? "空" : receiveUserId)+"，消息类型:30文字/32文件/33图片/34视频/35音频/36表情:"+(messageType == null ? "空" : messageType)+"，消息内容（文字/JSON）:"+(messageContent == null ? "空" : messageContent)+"，文件名:"+(fileName == null ? "空" : fileName)+"，文件大小(字节):"+(fileSize == null ? "空" : fileSize)+"，文件ID:"+(fileId == null ? "空" : fileId)+"，文件存储路径:"+(filePath == null ? "空" : filePath)+"，MIME类型:"+(contentType == null ? "空" : contentType)+"，文件类型标识:"+(fileType == null ? "空" : fileType)+"，发送类型:0私聊 1群发:"+(messageSendType == null ? "空" : messageSendType)+"，状态:0发送中 1已发送:"+(status == null ? "空" : status)+"，发送时间戳(ms):"+(sendTime == null ? "空" : sendTime)+"，前端唯一消息ID:"+(messageOnlyId == null ? "空" : messageOnlyId)+"，送达状态:0待送达 1已送达 2已读:"+(deliveryStatus == null ? "空" : deliveryStatus);
	}
}