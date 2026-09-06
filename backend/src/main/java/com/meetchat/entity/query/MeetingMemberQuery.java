package com.meetchat.entity.query;

import java.util.Date;


/**
 * 参数
 */
public class MeetingMemberQuery extends BaseParam {


	/**
	 * 
	 */
	private String meetingId;

	private String meetingIdFuzzy;

	/**
	 * 
	 */
	private String meetingNo;

	private String meetingNoFuzzy;

	/**
	 * 
	 */
	private String meetingName;

	private String meetingNameFuzzy;

	/**
	 * 
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 
	 */
	private String createUserId;

	private String createUserIdFuzzy;

	/**
	 * 
	 */
	private Integer jionType;

	/**
	 * 
	 */
	private String jionPassword;

	private String jionPasswordFuzzy;

	/**
	 * 
	 */
	private String startTime;

	private String startTimeStart;

	private String startTimeEnd;

	/**
	 * 
	 */
	private String endTime;

	private String endTimeStart;

	private String endTimeEnd;

	/**
	 * 
	 */
	private Integer status;


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

	public void setMeetingName(String meetingName){
		this.meetingName = meetingName;
	}

	public String getMeetingName(){
		return this.meetingName;
	}

	public void setMeetingNameFuzzy(String meetingNameFuzzy){
		this.meetingNameFuzzy = meetingNameFuzzy;
	}

	public String getMeetingNameFuzzy(){
		return this.meetingNameFuzzy;
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

	public void setCreateUserId(String createUserId){
		this.createUserId = createUserId;
	}

	public String getCreateUserId(){
		return this.createUserId;
	}

	public void setCreateUserIdFuzzy(String createUserIdFuzzy){
		this.createUserIdFuzzy = createUserIdFuzzy;
	}

	public String getCreateUserIdFuzzy(){
		return this.createUserIdFuzzy;
	}

	public void setJionType(Integer jionType){
		this.jionType = jionType;
	}

	public Integer getJionType(){
		return this.jionType;
	}

	public void setJionPassword(String jionPassword){
		this.jionPassword = jionPassword;
	}

	public String getJionPassword(){
		return this.jionPassword;
	}

	public void setJionPasswordFuzzy(String jionPasswordFuzzy){
		this.jionPasswordFuzzy = jionPasswordFuzzy;
	}

	public String getJionPasswordFuzzy(){
		return this.jionPasswordFuzzy;
	}

	public void setStartTime(String startTime){
		this.startTime = startTime;
	}

	public String getStartTime(){
		return this.startTime;
	}

	public void setStartTimeStart(String startTimeStart){
		this.startTimeStart = startTimeStart;
	}

	public String getStartTimeStart(){
		return this.startTimeStart;
	}
	public void setStartTimeEnd(String startTimeEnd){
		this.startTimeEnd = startTimeEnd;
	}

	public String getStartTimeEnd(){
		return this.startTimeEnd;
	}

	public void setEndTime(String endTime){
		this.endTime = endTime;
	}

	public String getEndTime(){
		return this.endTime;
	}

	public void setEndTimeStart(String endTimeStart){
		this.endTimeStart = endTimeStart;
	}

	public String getEndTimeStart(){
		return this.endTimeStart;
	}
	public void setEndTimeEnd(String endTimeEnd){
		this.endTimeEnd = endTimeEnd;
	}

	public String getEndTimeEnd(){
		return this.endTimeEnd;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

}