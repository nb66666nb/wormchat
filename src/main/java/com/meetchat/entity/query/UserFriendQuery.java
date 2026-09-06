package com.meetchat.entity.query;

import java.util.Date;


/**
 * 好友关系表参数
 */
public class UserFriendQuery extends BaseParam {


	/**
	 * 主键ID
	 */
	private Long id;

	/**
	 * 用户ID
	 */
	private String userId;

	private String userIdFuzzy;

	/**
	 * 好友的用户ID
	 */
	private String friendUserId;

	private String friendUserIdFuzzy;

	/**
	 * 用户给好友设置的备注名
	 */
	private String remarkName;

	private String remarkNameFuzzy;

	/**
	 * 成为好友的时间
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;


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

	public void setUserIdFuzzy(String userIdFuzzy){
		this.userIdFuzzy = userIdFuzzy;
	}

	public String getUserIdFuzzy(){
		return this.userIdFuzzy;
	}

	public void setFriendUserId(String friendUserId){
		this.friendUserId = friendUserId;
	}

	public String getFriendUserId(){
		return this.friendUserId;
	}

	public void setFriendUserIdFuzzy(String friendUserIdFuzzy){
		this.friendUserIdFuzzy = friendUserIdFuzzy;
	}

	public String getFriendUserIdFuzzy(){
		return this.friendUserIdFuzzy;
	}

	public void setRemarkName(String remarkName){
		this.remarkName = remarkName;
	}

	public String getRemarkName(){
		return this.remarkName;
	}

	public void setRemarkNameFuzzy(String remarkNameFuzzy){
		this.remarkNameFuzzy = remarkNameFuzzy;
	}

	public String getRemarkNameFuzzy(){
		return this.remarkNameFuzzy;
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

}