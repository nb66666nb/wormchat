package com.meetchat.entity.po;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import com.meetchat.entity.enums.DateTimePatternEnum;
import com.meetchat.utils.DateUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;


/**
 * 好友申请表
 */
public class FriendRequest implements Serializable {


	/**
	 * 主键ID
	 */
	private Long id;

	/**
	 * 发起请求的用户ID
	 */
	private String requestUserId;

	/**
	 * 接收请求的用户ID
	 */
	private String targetUserId;

	/**
	 * 状态：0=待处理 1=已接受 2=已拒绝 3=已撤销
	 */
	private Integer status;

	/**
	 * 申请备注（如"我是张三"）
	 */
	private String remark;

	/**
	 * 申请时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 处理时间
	 */
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date updateTime;


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

	public void setTargetUserId(String targetUserId){
		this.targetUserId = targetUserId;
	}

	public String getTargetUserId(){
		return this.targetUserId;
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

	public void setCreateTime(Date createTime){
		this.createTime = createTime;
	}

	public Date getCreateTime(){
		return this.createTime;
	}

	public void setUpdateTime(Date updateTime){
		this.updateTime = updateTime;
	}

	public Date getUpdateTime(){
		return this.updateTime;
	}


}