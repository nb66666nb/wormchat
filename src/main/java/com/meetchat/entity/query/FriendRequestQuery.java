package com.meetchat.entity.query;

import java.util.Date;


/**
 * 好友申请表参数
 */
public class FriendRequestQuery extends BaseParam {


	/**
	 * 主键ID
	 */
	private Long id;

	/**
	 * 发起请求的用户ID
	 */
	private String requestUserId;

	private String requestUserIdFuzzy;

	/**
	 * 接收请求的用户ID
	 */
	private String targetUserId;

	private String targetUserIdFuzzy;

	/**
	 * 状态：0=待处理 1=已接受 2=已拒绝 3=已撤销
	 */
	private Integer status;

	/**
	 * 申请备注（如"我是张三"）
	 */
	private String remark;

	private String remarkFuzzy;

	/**
	 * 申请时间
	 */
	private String createTime;

	private String createTimeStart;

	private String createTimeEnd;

	/**
	 * 处理时间
	 */
	private String updateTime;

	private String updateTimeStart;

	private String updateTimeEnd;


	public void setId(Long id){
		this.id = id;
	}

	public Long getId(){
		return this.id;
	}

	public void setRequestUserId(String requestUserId){
		this.requestUserId = requestUserId;
	}

	public String getRequestUserId(){
		return this.requestUserId;
	}

	public void setRequestUserIdFuzzy(String requestUserIdFuzzy){
		this.requestUserIdFuzzy = requestUserIdFuzzy;
	}

	public String getRequestUserIdFuzzy(){
		return this.requestUserIdFuzzy;
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

	public void setStatus(Integer status){
		this.status = status;
	}

	public Integer getStatus(){
		return this.status;
	}

	public void setRemark(String remark){
		this.remark = remark;
	}

	public String getRemark(){
		return this.remark;
	}

	public void setRemarkFuzzy(String remarkFuzzy){
		this.remarkFuzzy = remarkFuzzy;
	}

	public String getRemarkFuzzy(){
		return this.remarkFuzzy;
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

	public void setUpdateTime(String updateTime){
		this.updateTime = updateTime;
	}

	public String getUpdateTime(){
		return this.updateTime;
	}

	public void setUpdateTimeStart(String updateTimeStart){
		this.updateTimeStart = updateTimeStart;
	}

	public String getUpdateTimeStart(){
		return this.updateTimeStart;
	}
	public void setUpdateTimeEnd(String updateTimeEnd){
		this.updateTimeEnd = updateTimeEnd;
	}

	public String getUpdateTimeEnd(){
		return this.updateTimeEnd;
	}

}