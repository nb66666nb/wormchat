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
public class UserInfo implements Serializable {


	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 昵称
	 */
	private String nickName;

	/**
	 * 密码
	 */
	private String password;

	/**
	 * 邮箱
	 */
	private String email;

	/**
	 * 性别  0：女 1：男 2：保密
	 */
	private Integer sex;

	/**
	 * 状态
	 */
	private Integer status;

	/**
	 * 上次登陆时间
	 */
	private Long lastLoginTime;

	/**
	 * 上次退出登陆时间
	 */
	private Long lastOffTime;

	/**
	 * 会议ID
	 */
	private String meetingNo;

	/**
	 * 创建时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;


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

	public void setPassword(String password){
		this.password = password;
	}

	public String getPassword(){
		return this.password;
	}

	public void setEmail(String email){
		this.email = email;
	}

	public String getEmail(){
		return this.email;
	}

	public void setSex(Integer sex){
		this.sex = sex;
	}

	public Integer getSex(){
		return this.sex;
	}

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setLastLoginTime(Long lastLoginTime){
		this.lastLoginTime = lastLoginTime;
	}

	public Long getLastLoginTime(){
		return this.lastLoginTime;
	}

	public void setLastOffTime(Long lastOffTime){
		this.lastOffTime = lastOffTime;
	}

	public Long getLastOffTime(){
		return this.lastOffTime;
	}

	public void setMeetingNo(String meetingNo){
		this.meetingNo = meetingNo;
	}

	public String getMeetingNo(){
		return this.meetingNo;
	}

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	@Override
	public String toString (){
		return "用户ID:"+(userId == null ? "空" : userId)+"，昵称:"+(nickName == null ? "空" : nickName)+"，密码:"+(password == null ? "空" : password)+"，邮箱:"+(email == null ? "空" : email)+"，性别  0：女 1：男 2：保密:"+(sex == null ? "空" : sex)+"，状态:"+(status == null ? "空" : status)+"，上次登陆时间:"+(lastLoginTime == null ? "空" : lastLoginTime)+"，上次退出登陆时间:"+(lastOffTime == null ? "空" : lastOffTime)+"，会议ID:"+(meetingNo == null ? "空" : meetingNo)+"，创建时间:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}