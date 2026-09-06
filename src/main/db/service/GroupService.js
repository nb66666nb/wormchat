/**
 * GroupService — 群聊业务逻辑层
 * 提供群信息、群成员的本地 SQLite 读写操作
 */
import * as groupInfoMapper from '../mapper/GroupInfoMapper'
import * as groupMemberMapper from '../mapper/GroupMemberMapper'
import { GroupStatusEnum } from '../../messageType'

// ==================== 群信息 ====================

export const getGroupInfo = (groupId) => {
  return groupInfoMapper.getById(groupId)
}

export const saveGroupInfo = (data) => {
  groupInfoMapper.insert(data)
  return groupInfoMapper.getById(data.groupId)
}

export const updateGroupInfo = (groupId, changes) => {
  groupInfoMapper.updateById(groupId, changes)
  return groupInfoMapper.getById(groupId)
}

export const dissolveGroup = (groupId) => {
  groupInfoMapper.updateStatus(groupId, GroupStatusEnum.DISSOLVED)
}

export const isGroupDissolved = (groupId) => {
  const info = groupInfoMapper.getById(groupId)
  return info ? info.status === GroupStatusEnum.DISSOLVED : false
}

// ==================== 群成员 ====================

export const getGroupMembers = (groupId) => {
  return groupMemberMapper.listByGroupId(groupId)
}

export const getMember = (groupId, userId) => {
  return groupMemberMapper.getByGroupAndUser(groupId, userId)
}

export const saveMember = (data) => {
  groupMemberMapper.insert(data)
  return groupMemberMapper.getByGroupAndUser(data.groupId, data.userId)
}

export const batchSaveMembers = (list) => {
  groupMemberMapper.batchInsert(list)
}

export const updateMemberRole = (groupId, userId, role) => {
  groupMemberMapper.updateRole(groupId, userId, role)
}

export const removeMember = (groupId, userId) => {
  groupMemberMapper.updateStatus(groupId, userId, 1)
}

export const kickMember = (groupId, userId) => {
  groupMemberMapper.updateStatus(groupId, userId, 2)
}

/**
 * 获取当前用户在群中的角色
 * @returns {string|null} OWNER / ADMIN / MEMBER / null(非群成员)
 */
export const getMyRole = (groupId, userId) => {
  const member = groupMemberMapper.getByGroupAndUser(groupId, userId)
  return member ? member.role : null
}

/**
 * 批量保存群信息+群成员（WebSocket 推送时使用）
 * @param {Object} groupData - 群信息
 * @param {Array} members - 成员列表
 */
export const syncGroupData = (groupData, members) => {
  if (groupData) {
    groupInfoMapper.insert(groupData)
  }
  if (members && members.length > 0) {
    groupMemberMapper.batchInsert(members)
  }
}
