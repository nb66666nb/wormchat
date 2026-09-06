package com.meetchat.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.meetchat.entity.enums.DateTimePatternEnum;
import com.meetchat.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * 
 */
public class MeetingInfo implements Serializable {


	/**
	 * 
	 */
	private String meetingId;

	/**
	 * 
	 */
	private String userId;

	/**
	 * 
	 */
	private String nickName;

	/**
	 * 
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date lastJoinTime;

	/**
	 * 
	 */
	private Integer status;

	/**
	 * 
	 */
	private Integer memberType;

	/**
	 * 
	 */
	private Integer meetingStatus;


	public void setMeetingId(String meetingId){
		this.meetingId = meetingId;
	}

	public String getMeetingId(){
		return this.meetingId;
	}

	public void setUserId(String userId){
		this.userId = userId;
	}

	public String getUserId(){
		return this.userId;
	}

	public void setNickName(String nickName){
		this.nickName = nickName;
	}

	public String getNickName(){
		return this.nickName;
	}

	public void setLastJoinTime(Date lastJoinTime){
		this.lastJoinTime = lastJoinTime;
	}

	public Date getLastJoinTime(){
		return this.lastJoinTime;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setMemberType(Integer memberType){
		this.memberType = memberType;
	}

	public Integer getMemberType(){
		return this.memberType;
	}

	public void setMeetingStatus(Integer meetingStatus){
		this.meetingStatus = meetingStatus;
	}

	public Integer getMeetingStatus(){
		return this.meetingStatus;
	}

	@Override
	public String toString (){
		return "meetingId:"+(meetingId == null ? "空" : meetingId)+"，userId:"+(userId == null ? "空" : userId)+"，nickName:"+(nickName == null ? "空" : nickName)+"，lastJoinTime:"+(lastJoinTime == null ? "空" : DateUtil.format(lastJoinTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()))+"，status:"+(status == null ? "空" : status)+"，memberType:"+(memberType == null ? "空" : memberType)+"，meetingStatus:"+(meetingStatus == null ? "空" : meetingStatus);
	}
}