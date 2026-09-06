/**
 * GroupInfo 数据访问层
 */
import { getDb } from '../Database'
import { rowToEntity, entityToParams } from '../entity/GroupInfo'

export const getById = (groupId) => {
  const db = getDb()
  const row = db.prepare('SELECT * FROM group_info WHERE group_id = ?').get(groupId)
  return rowToEntity(row)
}

export const insert = (data) => {
  const db = getDb()
  const p = entityToParams(data)
  db.prepare(`
    INSERT OR REPLACE INTO group_info (
      group_id, group_name, group_avatar, owner_user_id,
      invite_permission, announcement, status, create_time, update_time
    ) VALUES (
      $groupId, $groupName, $groupAvatar, $ownerUserId,
      $invitePermission, $announcement, $status, $createTime, $updateTime
    )
  `).run(p)
}

export const updateById = (groupId, changes) => {
  const db = getDb()
  const sets = []
  const params = {}
  for (const [key, value] of Object.entries(changes)) {
    const col = key.replace(/[A-Z]/g, (m) => '_' + m.toLowerCase())
    sets.push(`${col} = $${key}`)
    params[key] = value
  }
  if (sets.length === 0) return
  params.groupId = groupId
  db.prepare(`UPDATE group_info SET ${sets.join(', ')} WHERE group_id = $groupId`).run(params)
}

export const updateStatus = (groupId, status) => {
  const db = getDb()
  db.prepare('UPDATE group_info SET status = ?, update_time = ? WHERE group_id = ?')
    .run(status, Date.now(), groupId)
}
