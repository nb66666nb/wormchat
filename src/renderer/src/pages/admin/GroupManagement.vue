<template>
  <div class="group-mgmt">
    <!-- 搜索栏 -->
    <div class="search-card">
      <el-input
        v-model="query.groupNameFuzzy"
        placeholder="搜索群名称"
        clearable
        style="width: 200px"
        @keyup.enter="search"
      />
      <el-input
        v-model="query.groupId"
        placeholder="群ID"
        clearable
        style="width: 150px"
        @keyup.enter="search"
      />
      <el-input
        v-model="query.ownerUserId"
        placeholder="群主ID"
        clearable
        style="width: 150px"
        @keyup.enter="search"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px">
        <el-option label="正常" :value="0" />
        <el-option label="已封禁" :value="2" />
        <el-option label="已解散" :value="1" />
      </el-select>
      <div class="search-actions">
        <el-button type="primary" @click="search">
          <el-icon><Search /></el-icon>查询
        </el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div class="table-head">
        <span class="table-title">群聊列表</span>
        <span class="table-count">共 {{ totalCount }} 个群</span>
      </div>
      <el-table :data="groups" v-loading="loading" stripe>
        <el-table-column prop="groupName" label="群名称" min-width="150" />
        <el-table-column prop="groupId" label="群ID" width="130" />
        <el-table-column label="群主" min-width="140">
          <template #default="{ row }">
            <div class="owner-cell">
              <span>{{ row.ownerNickName }}</span>
              <span class="owner-id">{{ row.ownerUserId }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="成员数" width="90" align="center">
          <template #default="{ row }">
            <span class="num-badge">{{ row.memberCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="在线成员" width="100" align="center">
          <template #default="{ row }">
            <span class="num-badge online">{{ row.onlineMemberCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatTs(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 0"
              size="small" type="danger" link
              @click="banGroup(row)"
            >封禁</el-button>
            <el-button
              v-else-if="row.status === 2"
              size="small" type="success" link
              @click="unbanGroup(row)"
            >解封</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无群聊" :image-size="80" />
        </template>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNo"
          v-model:page-size="query.pageSize"
          :total="totalCount"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="load"
          @size-change="onSizeChange"
        />
      </div>
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" size="46%" :title="`群聊详情 - ${currentGroup.groupName || ''}`">
      <div v-loading="detailLoading" class="detail">
        <div class="detail-stats">
          <div class="ds-item">
            <div class="ds-value">{{ detail.memberCount }}</div>
            <div class="ds-label">成员总数</div>
          </div>
          <div class="ds-item">
            <div class="ds-value online">{{ detail.onlineMemberCount }}</div>
            <div class="ds-label">在线成员</div>
          </div>
          <div class="ds-item">
            <div class="ds-value" :class="statusTextClass(currentGroup.status)">{{ statusText(currentGroup.status) }}</div>
            <div class="ds-label">群状态</div>
          </div>
        </div>

        <div class="detail-info">
          <div class="info-row"><span class="info-key">群ID</span><span class="info-val">{{ currentGroup.groupId }}</span></div>
          <div class="info-row"><span class="info-key">群主</span><span class="info-val">{{ currentGroup.ownerNickName }} ({{ currentGroup.ownerUserId }})</span></div>
          <div class="info-row"><span class="info-key">公告</span><span class="info-val">{{ currentGroup.announcement || '无' }}</span></div>
          <div class="info-row"><span class="info-key">创建时间</span><span class="info-val">{{ formatTs(currentGroup.createTime) }}</span></div>
        </div>

        <div class="detail-members">
          <div class="dm-title">成员列表（{{ (detail.members || []).length }}）</div>
          <el-table :data="detail.members" size="small" max-height="380">
            <el-table-column prop="nickName" label="昵称" min-width="100" />
            <el-table-column prop="userId" label="用户ID" width="120" />
            <el-table-column label="角色" width="80">
              <template #default="{ row }">
                <el-tag size="small" :type="roleType(row.role)">{{ roleText(row.role) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="在线" width="70" align="center">
              <template #default="{ row }">
                <span class="dot" :class="row.online ? 'on' : 'off'"></span>
              </template>
            </el-table-column>
            <el-table-column label="加入时间" width="150">
              <template #default="{ row }">{{ formatTs(row.joinTime) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { proxy } = getCurrentInstance()

const groups = ref([])
const totalCount = ref(0)
const loading = ref(false)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  groupNameFuzzy: '',
  groupId: '',
  ownerUserId: '',
  status: null
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const currentGroup = ref({})
const detail = ref({})

const buildParams = () => {
  const p = { pageNo: query.pageNo, pageSize: query.pageSize, orderBy: 'create_time desc' }
  if (query.groupNameFuzzy) p.groupNameFuzzy = query.groupNameFuzzy
  if (query.groupId) p.groupId = query.groupId
  if (query.ownerUserId) p.ownerUserId = query.ownerUserId
  if (query.status !== null && query.status !== '') p.status = query.status
  return p
}

const load = async () => {
  loading.value = true
  const r = await proxy.Request({ url: proxy.Api.adminGroupList, params: buildParams(), showLoading: false })
  loading.value = false
  if (r && r.code === 200) {
    groups.value = r.data.list || []
    totalCount.value = r.data.totalCount || 0
  }
}

const search = () => { query.pageNo = 1; load() }
const reset = () => {
  query.groupNameFuzzy = ''
  query.groupId = ''
  query.ownerUserId = ''
  query.status = null
  search()
}
const onSizeChange = () => { query.pageNo = 1; load() }

const openDetail = async (row) => {
  currentGroup.value = { ...row }
  detailVisible.value = true
  detail.value = {}
  detailLoading.value = true
  const r = await proxy.Request({
    url: proxy.Api.adminGroupDetail,
    params: { groupId: row.groupId },
    showLoading: false
  })
  detailLoading.value = false
  if (r && r.code === 200) {
    detail.value = r.data || {}
    currentGroup.value = r.data.groupInfo || currentGroup.value
  }
}

const banGroup = (row) => {
  ElMessageBox.confirm(`确定封禁群「${row.groupName}」吗？封禁后群内无法收发消息。`, '封禁群聊', { type: 'error' })
    .then(async () => {
      const r = await proxy.Request({
        url: proxy.Api.adminManageGroup,
        params: { groupId: row.groupId, status: 2 }
      })
      if (r) { ElMessage.success('群聊已封禁'); load() }
    }).catch(() => {})
}

const unbanGroup = (row) => {
  ElMessageBox.confirm(`确定解封群「${row.groupName}」吗？`, '解封群聊', { type: 'success' })
    .then(async () => {
      const r = await proxy.Request({
        url: proxy.Api.adminManageGroup,
        params: { groupId: row.groupId, status: 0 }
      })
      if (r) { ElMessage.success('群聊已解封'); load() }
    }).catch(() => {})
}

const statusText = (s) => (s === 0 ? '正常' : s === 1 ? '已解散' : s === 2 ? '已封禁' : '未知')
const statusTextClass = (s) => (s === 0 ? 'ok' : 'ban')
const statusType = (s) => (s === 0 ? 'success' : s === 1 ? 'info' : 'danger')
const roleText = (r) => (r === 'OWNER' ? '群主' : r === 'ADMIN' ? '管理员' : '成员')
const roleType = (r) => (r === 'OWNER' ? 'danger' : r === 'ADMIN' ? 'warning' : 'info')
const formatTs = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

onMounted(load)
</script>

<style lang="css" scoped>
.group-mgmt { display: flex; flex-direction: column; gap: 14px; }

.search-card {
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  padding: 16px 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.search-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.table-card {
  background: #fff;
  border: 1px solid #e8eaed;
  border-radius: 12px;
  padding: 16px 18px;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.table-title { font-size: 15px; font-weight: 600; color: #202124; }
.table-count { font-size: 13px; color: #9aa0a6; }

.owner-cell { display: flex; flex-direction: column; line-height: 1.2; }
.owner-id { font-size: 11px; color: #9aa0a6; }

.num-badge {
  display: inline-block;
  min-width: 28px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f1f3f4;
  color: #5f6368;
  font-size: 12px;
  font-weight: 600;
}
.num-badge.online { background: rgba(52, 168, 83, 0.1); color: #34a853; }

.pager { margin-top: 14px; display: flex; justify-content: flex-end; }

.detail { padding: 0 4px; }

.detail-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}

.ds-item {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 16px;
  text-align: center;
}

.ds-value { font-size: 22px; font-weight: 700; color: #202124; }
.ds-value.online { color: #34a853; }
.ds-value.ok { color: #34a853; }
.ds-value.ban { color: #ea4335; }

.ds-label { font-size: 12px; color: #5f6368; margin-top: 4px; }

.detail-info {
  background: #f8f9fa;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 18px;
}

.info-row { display: flex; padding: 5px 0; font-size: 13px; }
.info-key { width: 70px; color: #5f6368; flex-shrink: 0; }
.info-val { color: #202124; word-break: break-all; }

.dm-title { font-size: 14px; font-weight: 600; color: #202124; margin-bottom: 10px; }

.dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; }
.dot.on { background: #34a853; }
.dot.off { background: #dadce0; }
</style>
