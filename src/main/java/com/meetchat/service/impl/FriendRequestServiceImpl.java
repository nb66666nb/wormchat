package com.meetchat.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import com.meetchat.entity.enums.ContactRequestStatusEnum;
import com.meetchat.entity.po.UserFriend;
import com.meetchat.entity.po.UserInfo;
import com.meetchat.entity.query.UserFriendQuery;
import com.meetchat.entity.query.UserInfoQuery;
import com.meetchat.entity.vo.UserInfoVo;
import com.meetchat.exception.BusinessException;
import com.meetchat.mappers.UserFriendMapper;
import com.meetchat.mappers.UserInfoMapper;
import com.meetchat.utils.CopyTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meetchat.entity.enums.PageSize;
import com.meetchat.entity.query.FriendRequestQuery;
import com.meetchat.entity.po.FriendRequest;
import com.meetchat.entity.vo.PaginationResultVO;
import com.meetchat.entity.query.SimplePage;
import com.meetchat.mappers.FriendRequestMapper;
import com.meetchat.service.ChatSessionService;
import com.meetchat.service.FriendRequestService;
import com.meetchat.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 好友申请表 业务接口实现
 */
@Service("friendRequestService")
public class FriendRequestServiceImpl implements FriendRequestService {

	@Resource
	private FriendRequestMapper<FriendRequest, FriendRequestQuery> friendRequestMapper;
	@Resource
	private UserFriendMapper<UserFriend, UserFriendQuery> userFriendMapper;
    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery>userInfoMapper;
	@Resource
	private ChatSessionService chatSessionService;

	/**
	 * 根据条件查询列表
	 */
	@Override
	public List<FriendRequest> findListByParam(FriendRequestQuery param) {
		return this.friendRequestMapper.selectList(param);
	}

	/**
	 * 根据条件查询列表
	 */
	@Override
	public Integer findCountByParam(FriendRequestQuery param) {
		return this.friendRequestMapper.selectCount(param);
	}

	/**
	 * 分页查询方法
	 */
	@Override
	public PaginationResultVO<FriendRequest> findListByPage(FriendRequestQuery param) {
		int count = this.findCountByParam(param);
		int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

		SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
		param.setSimplePage(page);
		List<FriendRequest> list = this.findListByParam(param);
		PaginationResultVO<FriendRequest> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
		return result;
	}

	/**
	 * 新增
	 */
	@Override
	public Integer add(FriendRequest bean) {
		return this.friendRequestMapper.insert(bean);
	}

	/**
	 * 批量新增
	 */
	@Override
	public Integer addBatch(List<FriendRequest> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.friendRequestMapper.insertBatch(listBean);
	}

	/**
	 * 批量新增或者修改
	 */
	@Override
	public Integer addOrUpdateBatch(List<FriendRequest> listBean) {
		if (listBean == null || listBean.isEmpty()) {
			return 0;
		}
		return this.friendRequestMapper.insertOrUpdateBatch(listBean);
	}

	/**
	 * 多条件更新
	 */
	@Override
	public Integer updateByParam(FriendRequest bean, FriendRequestQuery param) {
		StringTools.checkParam(param);
		return this.friendRequestMapper.updateByParam(bean, param);
	}

	/**
	 * 多条件删除
	 */
	@Override
	public Integer deleteByParam(FriendRequestQuery param) {
		StringTools.checkParam(param);
		return this.friendRequestMapper.deleteByParam(param);
	}

	/**
	 * 根据Id获取对象
	 */
	@Override
	public FriendRequest getFriendRequestById(Long id) {
		return this.friendRequestMapper.selectById(id);
	}

	/**
	 * 根据Id修改
	 */
	@Override
	public Integer updateFriendRequestById(FriendRequest bean, Long id) {
		return this.friendRequestMapper.updateById(bean, id);
	}

	/**
	 * 根据Id删除
	 */
	@Override
	public Integer deleteFriendRequestById(Long id) {
		return this.friendRequestMapper.deleteById(id);
	}

	/**
	 * 根据RequestUserIdAndTargetUserId获取对象
	 */
	@Override
	public FriendRequest getFriendRequestByRequestUserIdAndTargetUserId(String requestUserId, String targetUserId) {
		return this.friendRequestMapper.selectByRequestUserIdAndTargetUserId(requestUserId, targetUserId);
	}

	/**
	 * 根据RequestUserIdAndTargetUserId修改
	 */
	@Override
	public Integer updateFriendRequestByRequestUserIdAndTargetUserId(FriendRequest bean, String requestUserId, String targetUserId) {
		return this.friendRequestMapper.updateByRequestUserIdAndTargetUserId(bean, requestUserId, targetUserId);
	}

	/**
	 * 根据RequestUserIdAndTargetUserId删除
	 */
	@Override
	public Integer deleteFriendRequestByRequestUserIdAndTargetUserId(String requestUserId, String targetUserId) {
		return this.friendRequestMapper.deleteByRequestUserIdAndTargetUserId(requestUserId, targetUserId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void addFriend(String userId, String contactId, String remark) {
		if (userId == null || contactId == null) {
			throw new BusinessException("参数不能为空");
		}
		UserFriend userFriend = userFriendMapper.selectByUserIdAndFriendUserId(userId, contactId);
		if (userFriend != null) {
			throw new BusinessException("已经添加过好友");
		}
		FriendRequest friendRequest = friendRequestMapper.selectByRequestUserIdAndTargetUserId(userId, contactId);
		if (friendRequest != null) {
			if (friendRequest.getStatus().equals(ContactRequestStatusEnum.ACCEPT.getStatus())) {
				throw new BusinessException("已经添加过好友");
			}
			if (friendRequest.getStatus().equals(ContactRequestStatusEnum.TO_DEAL.getStatus())) {
				throw new BusinessException("已经发送过好友请求,对方还未同意，请耐心等待"+friendRequest.getStatus());
			}

			if (friendRequest.getStatus().equals(ContactRequestStatusEnum.REFUSE.getStatus()) || friendRequest.getStatus().equals(ContactRequestStatusEnum.SEND_BACK.getStatus())) {
				friendRequest.setCreateTime(new Date());
				friendRequest.setStatus(ContactRequestStatusEnum.TO_DEAL.getStatus());
				friendRequest.setRemark(remark);
				friendRequestMapper.updateByRequestUserIdAndTargetUserId(friendRequest, userId, contactId);
			}

		} else {
			FriendRequest friendRequest1 = new FriendRequest();
			friendRequest1.setRequestUserId(userId);
			friendRequest1.setTargetUserId(contactId);
			friendRequest1.setStatus(ContactRequestStatusEnum.TO_DEAL.getStatus());
			friendRequest1.setCreateTime(new Date());
			friendRequest1.setRemark(remark);
			friendRequestMapper.insert(friendRequest1);
		}


	}

	@Override
	public UserInfoVo search(String userId, String contactId) {
		if(userId ==null||contactId==null){
			throw new BusinessException("参数不能为空");
        }
		if(userId.equals(contactId)){
			throw new BusinessException("不能添加自己为好友");
		}
		UserInfo userInfo =userInfoMapper.selectByUserId(contactId);
		UserInfoVo userInfoVo = CopyTools.copy(userInfo, UserInfoVo.class);
		return userInfoVo;


	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void manageRequest(String userId, String contactId, Integer status) {
		ContactRequestStatusEnum contactRequestStatusEnum = ContactRequestStatusEnum.getByStatus(status);
		if(contactId ==null|| contactRequestStatusEnum==null){
			throw new BusinessException("参数不能为空");
        }
        FriendRequest friendRequest = friendRequestMapper.selectByRequestUserIdAndTargetUserId(contactId,userId);
		if(friendRequest==null){
			throw new BusinessException("没有好友请求，或者已经过期");
		}
		if(friendRequest.getStatus().equals(ContactRequestStatusEnum.ACCEPT.getStatus())){
			throw new BusinessException("已经添加过好友");
		}

		if(friendRequest.getStatus().equals(ContactRequestStatusEnum.SEND_BACK.getStatus())){
			throw new BusinessException("已经撤回");
		}
		friendRequest.setStatus(contactRequestStatusEnum.getStatus());
		friendRequest.setUpdateTime(new Date());
		friendRequestMapper.updateByRequestUserIdAndTargetUserId(friendRequest, contactId, userId);
		if(status.equals(ContactRequestStatusEnum.ACCEPT.getStatus())){
			UserFriend userFriend = new UserFriend();
            userFriend.setUserId(userId);
            userFriend.setFriendUserId(contactId);
            userFriend.setCreateTime(new Date());
            userFriendMapper.insert(userFriend);
            UserFriend userFriend1 = new UserFriend();
            userFriend1.setUserId(contactId);
            userFriend1.setFriendUserId(userId);
            userFriend1.setCreateTime(new Date());
            userFriendMapper.insert(userFriend1);

            // 创建双方会话（ChatSession）
            UserInfo userA = userInfoMapper.selectByUserId(userId);
            UserInfo userB = userInfoMapper.selectByUserId(contactId);
            String nickA = (userA != null && userA.getNickName() != null) ? userA.getNickName() : userId;
            String nickB = (userB != null && userB.getNickName() != null) ? userB.getNickName() : contactId;
            chatSessionService.createFriendSessions(userId, nickA, contactId, nickB);
		}


	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void manageMyRequest(String userId, String contactId, Integer status) {
		ContactRequestStatusEnum statusEnum = ContactRequestStatusEnum.getByStatus(status);
		if(contactId ==null|| statusEnum !=ContactRequestStatusEnum.SEND_BACK){
			throw new BusinessException("参数报错");
		}
		FriendRequest friendRequest = friendRequestMapper.selectByRequestUserIdAndTargetUserId(userId, contactId);
		if(friendRequest==null){
			throw new BusinessException("没有好友请求，或者已经过期");
		}
		if(friendRequest.getStatus().equals(ContactRequestStatusEnum.ACCEPT.getStatus())){
			throw new BusinessException("已经添加过好友,不可撤回");
		}
		if(friendRequest.getStatus().equals(ContactRequestStatusEnum.REFUSE.getStatus())){
			throw new BusinessException("已经拒绝，不可撤回");
		}
		FriendRequest friendRequest1 = new FriendRequest();
		friendRequest1.setUpdateTime(new Date());
		friendRequest1.setStatus(statusEnum.getStatus());

		friendRequestMapper.updateByRequestUserIdAndTargetUserId(friendRequest1, userId, contactId);


	}

}