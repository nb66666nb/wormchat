package com.meetchat.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.meetchat.entity.enums.DateTimePatternEnum;
import com.meetchat.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * 好友关系表
 */
public class UserFriend implements Serializable {


	/**
	 * 主键ID
	 */
	private Long id;

	/**
	 * 用户ID
	 */
	private String userId;

	/**
	 * 好友的用户ID
	 */
	private String friendUserId;

	/**
	 * 用户给好友设置的备注名
	 */
	private String remarkName;

	/**
	 * 成为好友的时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;


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

	public void setFriendUserId(String friendUserId){
		this.friendUserId = friendUserId;
	}

	public String getFriendUserId(){
		return this.friendUserId;
	}

	public void setRemarkName(String remarkName){
		this.remarkName = remarkName;
	}

	public String getRemarkName(){
		return this.remarkName;
	}

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	@Override
	public String toString (){
		return "主键ID:"+(id == null ? "空" : id)+"，用户ID:"+(userId == null ? "空" : userId)+"，好友的用户ID:"+(friendUserId == null ? "空" : friendUserId)+"，用户给好友设置的备注名:"+(remarkName == null ? "空" : remarkName)+"，成为好友的时间:"+(createTime == null ? "空" : DateUtil.format(createTime, DateTimePatternEnum.YYYY_MM_DD_HH_MM_SS.getPattern()));
	}
}