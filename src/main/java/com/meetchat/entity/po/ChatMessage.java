package com.meetchat.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.meetchat.entity.enums.DateTimePatternEnum;
import com.meetchat.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * 会议聊天记录表
 */
public class ChatMessage implements Serializable {


	/**
	 * 主键ID
	 */
	private Long messageId;

	/**
	 * 会议ID
	 */
	private String meetingId;

	/**
	 * 会议号
	 */
	private String meetingNo;

	/**
	 * 消息发送类型
	 */
	private Integer messageSendType;

	/**
	 * 发送人用户ID
	 */
	private String sendUserId;

	/**
	 * 发送人昵称
	 */
	private String sendUserNickName;

	/**
	 * 接收人用户ID
	 */
	private String receiveUserId;

	/**
	 * 消息类型：1-文字 2-文件 3-图片 4-视频
	 */
	private Integer messageType;

	/**
	 * 消息内容
	 */
	private String messageContent;

	/**
	 * 文件大小（字节）
	 */
	private Long fileSize;

	/**
	 * 文件名
	 */
	private String fileName;

	/**
	 * 文件ID
	 */
	private String fileId;

	/**
	 * 文件存储路径
	 */
	private String filePath;

	/**
	 * 文件MIME类型
	 */
	private String contentType;

	/**
	 * 文件类型标识
	 */
	private Integer fileType;

	/**
	 * 扩展数据JSON
	 */
	private String extendData;

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
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;


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

	public void setMeetingNo(String meetingNo){
		this.meetingNo = meetingNo;
	}

	public String getMeetingNo(){
		return this.meetingNo;
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

	public void setSendUserNickName(String sendUserNickName){
		this.sendUserNickName = sendUserNickName;
	}

	public String getSendUserNickName(){
		return this.sendUserNickName;
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

	public void setExtendData(String extendData){
		this.extendData = extendData;
	}

	public String getExtendData(){
		return this.extendData;
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

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	@Override
	public String toString (){
		return "主键ID:"+(messageId == null ? "空" : messageId)+"，会议ID:"+(meetingId == null ? "空" : meetingId)+"，会议号:"+(meetingNo == null ? "空" : meetingNo)+"，消息发送类型:"+(messageSendType == null ? "空" : messageSendType)+"，发送人用户ID:"+(sendUserId == null ? "空" : sendUserId)+"，发送人昵称:"+(sendUserNickName == null ? "空" : sendUserNickName)+"，接收人用户ID:"+(receiveUserId == null ? "空" : receiveUserId)+"，消息类型：1-文字 2-文件 3-图片 4-视频:"+(messageType == null ? "空" : messageType)+"，消息内容:"+(messageContent == null ? "空" : messageContent)+"，文件大小（字节）:"+(fileSize == null ? "空" : fileSize)+"，文件名:"+(fileName == null ? "空" : fileName)+"，文件ID:"+(fileId == null ? "空" : fileId)+"，文件存储路径:"+(filePath == null ? "空" : filePath)+"，文件MIME类型:"+(contentType == null ? "空" : contentType)+"，文件类型标识:"+(fileType == null ? "空" : fileType)+"，扩展数据JSON:"+(extendData == null ? "空" : extendData)+"，消息状态：0-发送中 1-已发送:"+(status == null ? "空" : status)+"，发送时间戳（毫秒）:"+(sendTime == null ? "空" : sendTime)+"，记录创建时间:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}