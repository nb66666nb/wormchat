/**
 * 群聊信息实体 — 对齐后端 GroupInfo
 */

export const rowToEntity = (row) => {
  if (!row) return null
  return {
    groupId: row.group_id,
    groupName: row.group_name,
    groupAvatar: row.group_avatar,
    ownerUserId: row.owner_user_id,
    invitePermission: row.invite_permission,
    announcement: row.announcement,
    status: row.status,
    createTime: row.create_time,
    updateTime: row.update_time
  }
}

export const entityToParams = (data) => {
  return {
    groupId: data.groupId || '',
    groupName: data.groupName || '',
    groupAvatar: data.groupAvatar || '',
    ownerUserId: data.ownerUserId || '',
    invitePermission: data.invitePermission ?? 0,
    announcement: data.announcement || '',
    status: data.status ?? 0,
    createTime: data.createTime || 0,
    updateTime: data.updateTime || 0
  }
}
