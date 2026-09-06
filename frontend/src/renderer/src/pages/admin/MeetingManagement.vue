<template>
  <div class="meeting-management">
    <div class="page-header">
      <h2>📅 通话管理</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        创建通话
      </el-button>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索通话（通话号、主题）"
        clearable
        @keyup.enter="searchMeetings"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="statusFilter" placeholder="通话状态" clearable style="width: 150px">
        <el-option label="全部" :value="''" />
        <el-option label="进行中" :value="1" />
        <el-option label="已结束" :value="2" />
        <el-option label="已取消" :value="3" />
      </el-select>
      <el-button type="primary" @click="searchMeetings">搜索</el-button>
    </div>

    <div class="table-container">
      <el-table :data="meetings" stripe style="width: 100%">
        <el-table-column prop="meetingId" label="通话号" width="140" />
        <el-table-column prop="meetingTitle" label="通话主题" width="200" />
        <el-table-column prop="hostName" label="主持人" width="120" />
        <el-table-column prop="participantCount" label="参会人数" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="duration" label="时长" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" @click="viewDetail(row)">详情</el-button>
            <el-button size="small" type="warning" @click="editMeeting(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteMeeting(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 创建/编辑通话对话框 -->
    <el-dialog
      v-model="meetingDialogVisible"
      :title="isEdit ? '编辑通话' : '创建通话'"
      width="600px"
    >
      <el-form :model="meetingForm" label-width="100px">
        <el-form-item label="通话主题">
          <el-input v-model="meetingForm.meetingTitle" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="meetingForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="预计时长">
          <el-input-number v-model="meetingForm.duration" :min="30" :max="720" />
          <span style="margin-left: 8px">分钟</span>
        </el-form-item>
        <el-form-item label="通话密码">
          <el-input v-model="meetingForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="录制权限">
          <el-select v-model="meetingForm.recordPermission">
            <el-option label="仅主持人" :value="1" />
            <el-option label="全体成员" :value="2" />
            <el-option label="禁止录制" :value="3" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="meetingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMeeting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 通话详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="通话详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="通话号">{{ currentMeeting?.meetingId }}</el-descriptions-item>
        <el-descriptions-item label="通话主题">{{ currentMeeting?.meetingTitle }}</el-descriptions-item>
        <el-descriptions-item label="主持人">{{ currentMeeting?.hostName }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ currentMeeting?.startTime }}</el-descriptions-item>
        <el-descriptions-item label="参会人数">{{ currentMeeting?.participantCount }}</el-descriptions-item>
        <el-descriptions-item label="通话状态">
          <el-tag :type="getStatusType(currentMeeting?.status)">
            {{ getStatusText(currentMeeting?.status) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { Plus, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { proxy } = getCurrentInstance()

const meetings = ref([
  {
    meetingId: 'M001',
    meetingTitle: '产品需求评审会',
    hostName: '张三',
    participantCount: 8,
    startTime: '2026-05-10 10:00:00',
    duration: 90,
    status: 1
  },
  {
    meetingId: 'M002',
    meetingTitle: '周例会',
    hostName: '李四',
    participantCount: 15,
    startTime: '2026-05-11 14:00:00',
    duration: 60,
    status: 2
  }
])
const searchQuery = ref('')
const statusFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(2)

const meetingDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const isEdit = ref(false)
const currentMeeting = ref(null)
const meetingForm = reactive({
  meetingId: '',
  meetingTitle: '',
  startTime: '',
  duration: 60,
  password: '',
  recordPermission: 1
})

const getStatusText = (status) => {
  const map = { 1: '进行中', 2: '已结束', 3: '已取消' }
  return map[status] || '未知'
}

const getStatusType = (status) => {
  const map = { 1: 'success', 2: 'info', 3: 'danger' }
  return map[status] || 'info'
}

const loadMeetings = () => {
  proxy.$message.info('加载通话列表')
}

const searchMeetings = () => {
  proxy.$message.info(`搜索: ${searchQuery.value}`)
}

const showCreateDialog = () => {
  isEdit.value = false
  Object.assign(meetingForm, {
    meetingId: '',
    meetingTitle: '',
    startTime: '',
    duration: 60,
    password: '',
    recordPermission: 1
  })
  meetingDialogVisible.value = true
}

const editMeeting = (meeting) => {
  isEdit.value = true
  Object.assign(meetingForm, meeting)
  meetingDialogVisible.value = true
}

const saveMeeting = () => {
  proxy.$message.success(isEdit.value ? '通话已更新' : '通话已创建')
  meetingDialogVisible.value = false
}

const viewDetail = (meeting) => {
  currentMeeting.value = meeting
  detailDialogVisible.value = true
}

const deleteMeeting = (meeting) => {
  ElMessageBox.confirm(`确定要删除通话 ${meeting.meetingTitle}？`, '提示', {
    type: 'danger'
  }).then(() => {
    const index = meetings.value.findIndex(m => m.meetingId === meeting.meetingId)
    if (index > -1) {
      meetings.value.splice(index, 1)
      total.value--
    }
    ElMessage.success('通话已删除')
  }).catch(() => {})
}

const handlePageChange = () => {
  loadMeetings()
}

onMounted(() => {
  loadMeetings()
})
</script>

<style lang="scss" scoped>
.meeting-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

  h2 {
    margin: 0;
    font-size: 20px;
    color: #303133;
  }
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;

  .el-input {
    flex: 1;
  }
}

.table-container {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>