<template>
  <Teleport to="body">
    <Transition name="group-panel">
      <div v-if="visible" class="group-panel-overlay" @click.self="handleClose">
        <div class="group-panel">
          <!-- 头部 -->
          <div class="panel-header">
            <h3>群聊设置</h3>
            <button class="close-btn" @click="handleClose">
              <el-icon :size="18"><Close /></el-icon>
            </button>
          </div>

          <!-- 内容区 -->
          <div class="panel-body" v-loading="loading">
            <!-- 群信息区 -->
            <div class="section">
              <div class="section-title">群信息</div>
              <div class="info-row">
                <span class="info-label">群名称</span>
                <el-input
                  v-if="canEditGroupInfo"
                  v-model="editForm.groupName"
                  size="small"
                  maxlength="30"
                  show-word-limit
                />
                <span v-else class="info-value">{{ groupInfo?.groupName || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">群公告</span>
                <el-input
                  v-if="canEditGroupInfo"
                  v-model="editForm.announcement"
                  type="textarea"
                  :rows="2"
                  size="small"
                  maxlength="500"
                  show-word-limit
                />
                <span v-else class="info-value">{{ groupInfo?.announcement || '暂无公告' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">成员数</span>
                <span class="info-value">{{ members.length }}人</span>
              </div>
              <div v-if="canEditGroupInfo" class="info-row">
                <el-button type="primary" size="small" @click="handleSaveSettings" :loading="saving">保存设置</el-button>
              </div>
            </div>

            <!-- 权限设置区 -->
            <div class="section" v-if="canEditGroupInfo">
              <div class="section-title">权限设置</div>
              <div class="permission-row">
                <span>普通成员可邀请好友入群</span>
                <el-switch
                  v-model="editForm.invitePermission"
                  :active-value="0"
                  :inactive-value="1"
                  @change="handleSaveInvitePermission"
                />
              </div>
            </div>

            <!-- 成员管理区 -->
            <div class="section">
              <div class="section-title">
                成员管理
                <el-button v-if="canInvite" link size="small" @click="showInviteDialog = true">
                  <el-icon><Plus /></el-icon>
                  邀请成员
                </el-button>
              </div>
              <div class="member-list">
                <div v-for="member in members" :key="member.userId" class="member-item">
                  <ChatAvatar
                    :user-id="member.userId"
                    :user-name="member.nicknameInGroup || member.userId"
                    :size="36"
                  />
                  <div class="member-info">
                    <span class="member-name">{{ member.nicknameInGroup || member.userId }}</span>
                    <div class="member-tags">
                      <span v-if="member.role === 'OWNER'" class="role-tag owner">群主</span>
                      <span v-else-if="member.role === 'ADMIN'" class="role-tag admin">管理员</span>
                    </div>
                  </div>
                  <!-- 操作按钮 -->
                  <div class="member-actions" v-if="canManage && member.userId !== currentUserId && member.role !== 'OWNER'">
                    <el-button
                      v-if="myRole === 'OWNER' && member.role === 'MEMBER'"
                      link
                      size="small"
                      type="primary"
                      @click="handleSetAdmin(member)"
                    >设为管理员</el-button>
                    <el-button
                      v-if="myRole === 'OWNER' && member.role === 'ADMIN'"
                      link
                      size="small"
                      type="warning"
                      @click="handleUnsetAdmin(member)"
                    >取消管理员</el-button>
                    <el-button
                      v-if="member.role === 'MEMBER'"
                      link
                      size="small"
                      type="danger"
                      @click="handleKick(member)"
                    >踢出</el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 危险操作区 -->
            <div class="section danger-section">
              <el-button v-if="myRole === 'OWNER'" type="danger" plain @click="handleDissolve">
                解散群聊
              </el-button>
              <el-button v-if="myRole !== 'OWNER'" type="warning" plain @click="handleLeave">
                退出群聊
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 邀请成员对话框 -->
    <el-dialog v-model="showInviteDialog" title="邀请好友入群" width="400px" append-to-body>
      <div v-if="invitableFriends.length === 0" class="empty-invite">
        <span>没有可邀请的好友</span>
      </div>
      <div v-else class="invite-list">
        <div
          v-for="friend in invitableFriends"
          :key="friend.friendId"
          :class="['invite-item', { selected: inviteSelected.includes(friend.friendId) }]"
          @click="toggleInvite(friend.friendId)"
        >
          <ChatAvatar
            :user-id="friend.friendId"
            :user-name="friend.nickName || friend.friendId"
            :size="32"
          />
          <span class="invite-name">{{ friend.nickName || friend.friendId }}</span>
          <el-icon v-if="inviteSelected.includes(friend.friendId)" class="invite-check" :size="16"><Check /></el-icon>
        </div>
      </div>
      <template #footer>
        <el-button @click="showInviteDialog = false">取消</el-button>
        <el-button type="primary" @click="handleInvite" :disabled="inviteSelected.length === 0">邀请</el-button>
      </template>
    </el-dialog>
  </Teleport>
</template>

<script setup>
import { ref, computed, reactive, watch, onMounted } from 'vue'
import { Close, Plus, Check } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatAvatar from '@/components/ChatAvatar.vue'
import Request from '@/utils/Request'
import Api from '@/utils/Api'

const props = defineProps({
  groupId: { type: String, required: true },
  sessionId: { type: String, default: '' },
  currentUserId: { type: String, required: true }
})

const emit = defineEmits(['close', 'group-changed'])

const visible = ref(true)
const loading = ref(false)
const saving = ref(false)
const groupInfo = ref(null)
const members = ref([])
const myRole = ref(null)
const showInviteDialog = ref(false)
const inviteSelected = ref([])
const friendList = ref([])

const editForm = reactive({
  groupName: '',
  announcement: '',
  invitePermission: 0
})

/** 当前用户是否可编辑群设置（群主或管理员） */
const canEditGroupInfo = computed(() => {
  return myRole.value === 'OWNER' || myRole.value === 'ADMIN'
})

/** 当前用户是否可管理成员（群主或管理员） */
const canManage = computed(() => {
  return myRole.value === 'OWNER' || myRole.value === 'ADMIN'
})

/** 当前用户是否可邀请成员 */
const canInvite = computed(() => {
  if (!canManage.value && groupInfo.value?.invitePermission === 1) return false
  return true
})

/** 可邀请的好友列表（排除已在群中的） */
const invitableFriends = computed(() => {
  const memberIds = new Set(members.value.map(m => m.userId))
  return friendList.value.filter(f => !memberIds.has(f.friendId))
})

/** 加载群信息 */
const loadGroupData = async () => {
  if (!props.groupId) return
  loading.value = true
  try {
    // 从后端获取完整数据
    const [infoRes, membersRes] = await Promise.all([
      Request({ url: Api.getGroupInfo, params: { groupId: props.groupId }, json: true, showLoading: false, showError: false }),
      Request({ url: Api.getGroupMembers, params: { groupId: props.groupId }, json: true, showLoading: false, showError: false })
    ])

    if (infoRes?.code === 200 && infoRes.data) {
      groupInfo.value = infoRes.data
      editForm.groupName = infoRes.data.groupName || ''
      editForm.announcement = infoRes.data.announcement || ''
      editForm.invitePermission = infoRes.data.invitePermission ?? 0
      // 同步到本地 SQLite
      await window.api.dbGroupInfoSave(infoRes.data)
    }

    if (membersRes?.code === 200 && membersRes.data) {
      members.value = membersRes.data
      // 同步到本地 SQLite
      await window.api.dbGroupMembersBatchSave(membersRes.data)
    }

    // 获取当前用户角色
    myRole.value = await window.api.dbGroupMyRole(props.groupId, props.currentUserId)
  } catch (e) {
    console.error('[GroupSettingsPanel] 加载群数据失败:', e.message)
  } finally {
    loading.value = false
  }
}

/** 保存群设置 */
const handleSaveSettings = async () => {
  saving.value = true
  try {
    const res = await Request({
      url: Api.updateGroupSettings,
      params: {
        groupId: props.groupId,
        operatorUserId: props.currentUserId,
        groupName: editForm.groupName,
        announcement: editForm.announcement
      },
      json: true,
      showError: false
    })
    if (res?.code === 200) {
      ElMessage.success('群设置已更新')
      emit('group-changed')
      await loadGroupData()
    } else {
      ElMessage.error(res?.info || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  } finally {
    saving.value = false
  }
}

/** 保存邀请权限 */
const handleSaveInvitePermission = async (val) => {
  try {
    const res = await Request({
      url: Api.updateGroupSettings,
      params: {
        groupId: props.groupId,
        operatorUserId: props.currentUserId,
        invitePermission: val
      },
      json: true,
      showError: false
    })
    if (res?.code === 200) {
      ElMessage.success(val === 0 ? '已开启普通成员邀请' : '已限制仅管理员可邀请')
      emit('group-changed')
    } else {
      ElMessage.error(res?.info || '设置失败')
      editForm.invitePermission = val === 0 ? 1 : 0
    }
  } catch (e) {
    ElMessage.error('设置失败: ' + e.message)
  }
}

/** 设置管理员 */
const handleSetAdmin = async (member) => {
  try {
    await ElMessageBox.confirm(`确认将 "${member.nicknameInGroup || member.userId}" 设为管理员？`, '提示', { type: 'info' })
    const res = await Request({
      url: Api.setAdmin,
      params: {
        groupId: props.groupId,
        operatorUserId: props.currentUserId,
        targetUserId: member.userId,
        isAdmin: true
      },
      json: true,
      showError: false
    })
    if (res?.code === 200) {
      ElMessage.success('已设为管理员')
      await window.api.dbGroupMemberUpdateRole(props.groupId, member.userId, 'ADMIN')
      await loadGroupData()
      emit('group-changed')
    } else {
      ElMessage.error(res?.info || '操作失败')
    }
  } catch (_) { /* 取消 */ }
}

/** 取消管理员 */
const handleUnsetAdmin = async (member) => {
  try {
    await ElMessageBox.confirm(`确认取消 "${member.nicknameInGroup || member.userId}" 的管理员身份？`, '提示', { type: 'warning' })
    const res = await Request({
      url: Api.setAdmin,
      params: {
        groupId: props.groupId,
        operatorUserId: props.currentUserId,
        targetUserId: member.userId,
        isAdmin: false
      },
      json: true,
      showError: false
    })
    if (res?.code === 200) {
      ElMessage.success('已取消管理员')
      await window.api.dbGroupMemberUpdateRole(props.groupId, member.userId, 'MEMBER')
      await loadGroupData()
      emit('group-changed')
    } else {
      ElMessage.error(res?.info || '操作失败')
    }
  } catch (_) { /* 取消 */ }
}

/** 踢出成员 */
const handleKick = async (member) => {
  try {
    await ElMessageBox.confirm(`确认将 "${member.nicknameInGroup || member.userId}" 踢出群聊？`, '警告', { type: 'warning' })
    const res = await Request({
      url: Api.kickMember,
      params: {
        groupId: props.groupId,
        operatorUserId: props.currentUserId,
        targetUserId: member.userId
      },
      json: true,
      showError: false
    })
    if (res?.code === 200) {
      ElMessage.success('已踢出')
      await window.api.dbGroupMemberKick(props.groupId, member.userId)
      await loadGroupData()
      emit('group-changed')
    } else {
      ElMessage.error(res?.info || '操作失败')
    }
  } catch (_) { /* 取消 */ }
}

/** 解散群聊 */
const handleDissolve = async () => {
  try {
    await ElMessageBox.confirm('解散群聊后不可恢复，所有成员将无法继续发送消息。确认解散？', '危险操作', {
      type: 'error',
      confirmButtonText: '确认解散',
      cancelButtonText: '取消'
    })
    const res = await Request({
      url: Api.dissolveGroup,
      params: {
        groupId: props.groupId,
        operatorUserId: props.currentUserId
      },
      json: true,
      showError: false
    })
    if (res?.code === 200) {
      ElMessage.success('群聊已解散')
      await window.api.dbGroupDissolve(props.groupId)
      emit('group-changed')
      handleClose()
    } else {
      ElMessage.error(res?.info || '解散失败')
    }
  } catch (_) { /* 取消 */ }
}

/** 退出群聊 */
const handleLeave = async () => {
  try {
    await ElMessageBox.confirm('确认退出该群聊？', '提示', { type: 'warning' })
    const res = await Request({
      url: Api.leaveGroup,
      params: {
        groupId: props.groupId,
        userId: props.currentUserId
      },
      json: true,
      showError: false
    })
    if (res?.code === 200) {
      ElMessage.success('已退出群聊')
      await window.api.dbGroupMemberRemove(props.groupId, props.currentUserId)
      handleClose()
    } else {
      ElMessage.error(res?.info || '退出失败')
    }
  } catch (_) { /* 取消 */ }
}

/** 加载好友列表（邀请用，仅非机器人联系人） */
const loadFriendList = async () => {
  try {
    const res = await Request({
      url: Api.getNonBotFriends,
      json: true,
      showLoading: false,
      showError: false
    })
    if (res?.code === 200) {
      friendList.value = res.data || []
    }
  } catch (e) {
    console.error('[GroupSettingsPanel] 加载好友列表失败:', e.message)
  }
}

/** 切换邀请选中 */
const toggleInvite = (friendId) => {
  const idx = inviteSelected.value.indexOf(friendId)
  if (idx === -1) inviteSelected.value.push(friendId)
  else inviteSelected.value.splice(idx, 1)
}

/** 邀请成员 */
const handleInvite = async () => {
  if (inviteSelected.value.length === 0) return
  try {
    const res = await Request({
      url: Api.inviteMember,
      params: {
        groupId: props.groupId,
        memberIds: inviteSelected.value
      },
      json: true,
      showError: false
    })
    if (res?.code === 200) {
      ElMessage.success('邀请成功')
      showInviteDialog.value = false
      inviteSelected.value = []
      await loadGroupData()
      emit('group-changed')
    } else {
      ElMessage.error(res?.info || '邀请失败')
    }
  } catch (e) {
    ElMessage.error('邀请失败: ' + e.message)
  }
}

const handleClose = () => {
  visible.value = false
  emit('close')
}

watch(showInviteDialog, (val) => {
  if (val) loadFriendList()
})

onMounted(() => {
  loadGroupData()
})
</script>

<style scoped>
.group-panel-overlay {
  position: fixed;
  top: 0;
  right: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  z-index: 2000;
  display: flex;
  justify-content: flex-end;
}

.group-panel {
  width: 400px;
  height: 100%;
  background: #ffffff;
  box-shadow: -4px 0 16px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e8eaed;
  flex-shrink: 0;
}

.panel-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #202124;
  margin: 0;
}

.close-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #5f6368;
  padding: 4px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}

.close-btn:hover {
  background: #f1f3f4;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 0;
}

.section {
  padding: 16px 20px;
  border-bottom: 1px solid #f1f3f4;
}

.section-title {
  font-size: 13px;
  font-weight: 600;
  color: #5f6368;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.info-label {
  font-size: 13px;
  color: #5f6368;
  min-width: 60px;
  flex-shrink: 0;
  padding-top: 4px;
}

.info-value {
  font-size: 13px;
  color: #202124;
  flex: 1;
  word-break: break-all;
}

.permission-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #202124;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.15s;
}

.member-item:hover {
  background: #f8f9fa;
}

.member-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.member-name {
  font-size: 13px;
  color: #202124;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.member-tags {
  display: flex;
  gap: 4px;
}

.role-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 3px;
  font-weight: 500;
}

.role-tag.owner {
  color: #1a73e8;
  background: rgba(26, 115, 232, 0.1);
}

.role-tag.admin {
  color: #e8710a;
  background: rgba(232, 113, 10, 0.1);
}

.member-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.danger-section {
  display: flex;
  gap: 8px;
  padding: 20px;
  border-bottom: none;
}

/* 邀请对话框 */
.empty-invite {
  padding: 24px;
  text-align: center;
  color: #9aa0a6;
  font-size: 13px;
}

.invite-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 300px;
  overflow-y: auto;
}

.invite-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.15s;
}

.invite-item:hover {
  background: #f1f3f4;
}

.invite-item.selected {
  background: rgba(26, 115, 232, 0.06);
  border-color: rgba(26, 115, 232, 0.2);
}

.invite-name {
  flex: 1;
  font-size: 13px;
  color: #202124;
}

.invite-check {
  color: #1a73e8;
}

/* 过渡动画 */
.group-panel-enter-active,
.group-panel-leave-active {
  transition: opacity 0.25s ease;
}

.group-panel-enter-active .group-panel,
.group-panel-leave-active .group-panel {
  transition: transform 0.25s ease;
}

.group-panel-enter-from,
.group-panel-leave-to {
  opacity: 0;
}

.group-panel-enter-from .group-panel,
.group-panel-leave-to .group-panel {
  transform: translateX(100%);
}
</style>
