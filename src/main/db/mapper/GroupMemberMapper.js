/**
 * GroupMember 数据访问层
 */
import { getDb } from '../Database'
import { rowToEntity, entityToParams } from '../entity/GroupMember'

export const getByGroupAndUser = (groupId, userId) => {
  const db = getDb()
  const row = db.prepare('SELECT * FROM group_member WHERE group_id = ? AND user_id = ?').get(groupId, userId)
  return rowToEntity(row)
}

export const listByGroupId = (groupId) => {
  const db = getDb()
  const rows = db.prepare('SELECT * FROM group_member WHERE group_id = ? AND status = 0 ORDER BY role DESC, join_time ASC').all(groupId)
  return rows.map(rowToEntity)
}

export const insert = (data) => {
  const db = getDb()
  const p = entityToParams(data)
  db.prepare(`
    INSERT OR REPLACE INTO group_member (
      group_id, user_id, role, nickname_in_group, join_time, status
    ) VALUES (
      $groupId, $userId, $role, $nicknameInGroup, $joinTime, $status
    )
  `).run(p)
}

export const batchInsert = (list) => {
  const db = getDb()
  const stmt = db.prepare(`
    INSERT OR REPLACE INTO group_member (
      group_id, user_id, role, nickname_in_group, join_time, status
    ) VALUES (
      $groupId, $userId, $role, $nicknameInGroup, $joinTime, $status
    )
  `)
  const insertMany = db.transaction((items) => {
    for (const item of items) {
      stmt.run(entityToParams(item))
    }
  })
  insertMany(list)
}

export const updateRole = (groupId, userId, role) => {
  const db = getDb()
  db.prepare('UPDATE group_member SET role = ? WHERE group_id = ? AND user_id = ?').run(role, groupId, userId)
}

export const updateStatus = (groupId, userId, status) => {
  const db = getDb()
  db.prepare('UPDATE group_member SET status = ? WHERE group_id = ? AND user_id = ?').run(status, groupId, userId)
}

export const deleteByGroupId = (groupId) => {
  const db = getDb()
  db.prepare('DELETE FROM group_member WHERE group_id = ?').run(groupId)
}
