/**
 * 群成员实体 — 对齐后端 GroupMember
 */

export const rowToEntity = (row) => {
  if (!row) return null
  return {
    id: row.id,
    groupId: row.group_id,
    userId: row.user_id,
    role: row.role,
    nicknameInGroup: row.nickname_in_group,
    joinTime: row.join_time,
    status: row.status
  }
}

export const entityToParams = (data) => {
  return {
    id: data.id || null,
    groupId: data.groupId || '',
    userId: data.userId || '',
    role: data.role || 'MEMBER',
    nicknameInGroup: data.nicknameInGroup || '',
    joinTime: data.joinTime || 0,
    status: data.status ?? 0
  }
}
