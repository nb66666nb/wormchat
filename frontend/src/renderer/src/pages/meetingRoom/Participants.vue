<template>
  <div class="participants-panel">
    <div class="panel-header">
      <el-icon><User /></el-icon>
      <span>参会者</span>
      <span class="participant-count">({{ participantCount }})</span>
      <el-button
        v-if="isCreator"
        type="primary"
        size="small"
        class="manage-btn"
        @click="showManageDialog"
      >
        <el-icon><Setting /></el-icon>
        管理
      </el-button>
    </div>
    <div class="panel-body">
      <div
        v-for="(participant, index) in participants"
        :key="index"
        class="participant-item"
        :class="{ 'offline': participant.status === 1 || participant.status === 3 }"
      >
        <ChatAvatar
          :user-id="participant.userId || participant.id || ''"
          :file-id="participant.avatarFileId || participant.fileId || ''"
          :file-path="participant.avatarFilePath || participant.filePath || participant.avatarUrl || ''"
          :user-name="participant.nickName || participant.nickname || participant.userName || '用户'"
          :size="32"
        />
        <div class="participant-info">
          <div class="participant-name">
            {{ participant.nickName || participant.nickname || `用户${index + 1}` }}
            <span v-if="participant.memberType === 0" class="role-badge host">主持</span>
            <span v-else-if="participant.status === 2" class="status-dot online"></span>
          </div>
          <div class="participant-role">
            {{ getMemberTypeText(participant.memberType) }}
            <span v-if="participant.status === 3" class="status-text blacked">(已拉黑)</span>
            <span v-else-if="participant.status === 1" class="status-text offline">(已退出)</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 管理人员弹框 -->
    <el-dialog
      v-model="manageDialogVisible"
      title="管理人员"
      width="450px"
      :close-on-click-modal="false"
    >
      <div class="manage-list">
        <div
          v-for="(member, index) in manageableMembers"
          :key="index"
          class="manage-item"
        >
          <div class="member-info">
            <ChatAvatar
              :user-id="member.userId || member.id || ''"
              :file-id="member.avatarFileId || member.fileId || ''"
              :file-path="member.avatarFilePath || member.filePath || member.avatarUrl || ''"
              :user-name="member.nickName || member.nickname || member.userName || '用户'"
              :size="36"
            />
            <div class="member-detail">
              <div class="member-name">
                {{ member.nickName || member.nickname || `用户${index + 1}` }}
                <span v-if="member.memberType === 0" class="role-badge host">主持</span>
              </div>
              <div class="member-status">
                {{ getMemberTypeText(member.memberType) }}
                <span v-if="member.status === 3" class="status-text blacked">(已拉黑)</span>
                <span v-else-if="member.status === 1" class="status-text offline">(已退出)</span>
                <span v-else class="status-text online">(在线)</span>
              </div>
            </div>
          </div>
          <div class="member-actions">
            <el-button
              type="warning"
              size="small"
              :disabled="member.status === 1 || member.status === 3"
              @click="handleKick(member)"
            >
              踢出
            </el-button>
            <el-button
              type="danger"
              size="small"
              :disabled="member.status === 3"
              @click="handleBlack(member)"
            >
              {{ member.status === 3 ? '已拉黑' : '拉黑' }}
            </el-button>
          </div>
        </div>
        <el-empty v-if="manageableMembers.length === 0" description="暂无其他参会者" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { User, Setting } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCurrentInstance } from 'vue'
import UserInfoStore from '../../stores/UserInfoStore'
import ChatAvatar from '@/components/ChatAvatar.vue'

const { proxy } = getCurrentInstance()

const props = defineProps({
  participants: {
    type: Array,
    default: () => []
  },
  participantCount: {
    type: Number,
    default: 1
  },
  isCreator: {
    type: Boolean,
    default: false
  },
  meetingNo: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['refresh'])

const manageDialogVisible = ref(false)

const manageableMembers = computed(() => {
  const currentUserId = UserInfoStore.getUserId()
  return props.participants.filter(p => p.userId !== currentUserId)
})

const getMemberTypeText = (memberType) => {
  return memberType === 0 ? '主持人' : '参会者'
}

const showManageDialog = () => {
  manageDialogVisible.value = true
}

const handleKick = async (member) => {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${member.nickName || member.nickname || '该用户'} 踢出通话吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const result = await proxy.Request({
      url: proxy.Api.kickUser,
      params: {
        meetingNo: props.meetingNo,
        kickUserId: member.userId,
        staus: 1
      }
    })

    if (result && result.code === 200) {
      ElMessage.success('已踢出该成员')
      emit('refresh')
    } else {
      ElMessage.error(result?.info || '踢出失败')
    }
  } catch (error) {
    // 用户取消
  }
}

const handleBlack = async (member) => {
  try {
    await ElMessageBox.confirm(
      `确定要将 ${member.nickName || member.nickname || '该用户'} 拉黑吗？拉黑后该用户将无法再次加入此通话。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const result = await proxy.Request({
      url: proxy.Api.kickUser,
      params: {
        meetingNo: props.meetingNo,
        kickUserId: member.userId,
        staus:3
      }
    })

    if (result && result.code === 200) {
      ElMessage.success('已拉黑该成员')
      emit('refresh')
    } else {
      ElMessage.error(result?.info || '拉黑失败')
    }
  } catch (error) {
    // 用户取消
  }
}
</script>

<style lang="css" scoped>
.participants-panel {
  width: 220px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 200px);
}

.panel-header {
  padding: 14px 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.panel-header .el-icon {
  color: #409eff;
}

.participant-count {
  color: #909399;
  font-weight: 400;
}

.manage-btn {
  margin-left: auto;
  padding: 4px 8px;
  font-size: 12px;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.participant-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.participant-item:hover {
  background: #f5f7fa;
}

.participant-info {
  flex: 1;
  min-width: 0;
}

.participant-name {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.participant-role {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.participant-item.offline {
  opacity: 0.6;
}

.participant-item.offline .el-avatar {
  background: #e4e7ed;
}

.role-badge {
  display: inline-block;
  padding: 1px 6px;
  font-size: 10px;
  border-radius: 4px;
  margin-left: 4px;
  vertical-align: middle;
}

.role-badge.host {
  background: #fef0f0;
  color: #f56c6c;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-left: 4px;
  vertical-align: middle;
}

.status-dot.online {
  background: #67c23a;
}

.status-text {
  margin-left: 4px;
}

.status-text.blacked {
  color: #f56c6c;
}

.status-text.offline {
  color: #909399;
}

.status-text.online {
  color: #67c23a;
}

/* 管理弹框样式 */
.manage-list {
  max-height: 400px;
  overflow-y: auto;
}

.manage-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.manage-item:last-child {
  border-bottom: none;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.member-detail {
  flex: 1;
  min-width: 0;
}

.member-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.member-status {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.member-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
</style>
