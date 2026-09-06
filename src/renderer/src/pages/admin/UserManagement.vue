<template>
  <div class="user-mgmt">
    <!-- 搜索栏 -->
    <div class="search-card">
      <el-input
        v-model="query.nickNameFuzzy"
        placeholder="搜索昵称"
        clearable
        style="width: 160px"
        @keyup.enter="search"
      />
      <el-input
        v-model="query.emailFuzzy"
        placeholder="搜索邮箱"
        clearable
        style="width: 200px"
        @keyup.enter="search"
      />
      <el-input
        v-model="query.userIdFuzzy"
        placeholder="用户ID"
        clearable
        style="width: 140px"
        @keyup.enter="search"
      />
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 110px">
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-select v-model="query.sex" placeholder="性别" clearable style="width: 100px">
        <el-option label="女" :value="0" />
        <el-option label="男" :value="1" />
        <el-option label="保密" :value="2" />
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
        <span class="table-title">用户列表</span>
        <span class="table-count">共 {{ totalCount }} 人</span>
      </div>
      <el-table :data="users" v-loading="loading" stripe size="default">
        <el-table-column prop="userId" label="用户ID" width="120" />
        <el-table-column prop="nickName" label="昵称" min-width="110" />
        <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
        <el-table-column label="性别" width="70">
          <template #default="{ row }">
            {{ sexText(row.sex) }}
          </template>
        </el-table-column>
        <el-table-column label="在线" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.online ? 'success' : 'info'" effect="light">
              <span class="online-tag-dot" :class="row.online ? 'on' : 'off'"></span>
              {{ row.online ? '在线' : '离线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后登录" width="160">
          <template #default="{ row }">{{ formatTs(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">{{ formatTs(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button
                v-if="row.online"
                size="small" type="warning" link
                @click="kick(row)"
              >下线</el-button>
              <el-button size="small" type="danger" link @click="ban(row)">封禁</el-button>
            </template>
            <template v-else>
              <el-button size="small" type="success" link @click="unban(row)">解封</el-button>
            </template>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户" :image-size="80" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCurrentInstance } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const { proxy } = getCurrentInstance()

const users = ref([])
const totalCount = ref(0)
const loading = ref(false)

const query = reactive({
  pageNo: 1,
  pageSize: 10,
  userIdFuzzy: '',
  nickNameFuzzy: '',
  emailFuzzy: '',
  sex: null,
  status: null
})

const buildParams = () => {
  const p = { pageNo: query.pageNo, pageSize: query.pageSize }
  if (query.userIdFuzzy) p.userIdFuzzy = query.userIdFuzzy
  if (query.nickNameFuzzy) p.nickNameFuzzy = query.nickNameFuzzy
  if (query.emailFuzzy) p.emailFuzzy = query.emailFuzzy
  if (query.sex !== null && query.sex !== '') p.sex = query.sex
  if (query.status !== null && query.status !== '') p.status = query.status
  return p
}

const load = async () => {
  loading.value = true
  const r = await proxy.Request({
    url: proxy.Api.adminUserList,
    params: buildParams(),
    showLoading: false
  })
  loading.value = false
  if (r && r.code === 200) {
    users.value = r.data.list || []
    totalCount.value = r.data.totalCount || 0
  }
}

const search = () => { query.pageNo = 1; load() }
const reset = () => {
  query.userIdFuzzy = ''
  query.nickNameFuzzy = ''
  query.emailFuzzy = ''
  query.sex = null
  query.status = null
  search()
}
const onSizeChange = () => { query.pageNo = 1; load() }

const kick = (row) => {
  ElMessageBox.confirm(`确定强制「${row.nickName}」下线吗？`, '强制下线', { type: 'warning' })
    .then(async () => {
      const r = await proxy.Request({ url: proxy.Api.adminKickUser, params: { userId: row.userId } })
      if (r) { ElMessage.success('已强制下线'); load() }
    }).catch(() => {})
}

const ban = (row) => {
  ElMessageBox.confirm(`确定封禁「${row.nickName}」吗？封禁后该账号无法登录。`, '封禁账号', { type: 'error' })
    .then(async () => {
      const r = await proxy.Request({
        url: proxy.Api.adminManageUser,
        params: { userId: row.userId, status: 0 }
      })
      if (r) { ElMessage.success('已封禁'); load() }
    }).catch(() => {})
}

const unban = (row) => {
  ElMessageBox.confirm(`确定解封「${row.nickName}」吗？`, '解封账号', { type: 'success' })
    .then(async () => {
      const r = await proxy.Request({
        url: proxy.Api.adminManageUser,
        params: { userId: row.userId, status: 1 }
      })
      if (r) { ElMessage.success('已解封'); load() }
    }).catch(() => {})
}

const sexText = (s) => (s === 0 ? '女' : s === 1 ? '男' : '保密')
const formatTs = (t) => {
  if (!t) return '-'
  const d = new Date(t)
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

onMounted(load)
</script>

<style lang="css" scoped>
.user-mgmt { display: flex; flex-direction: column; gap: 14px; }

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

.search-card .search-actions {
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

.online-tag-dot {
  display: inline-block;
  width: 6px; height: 6px; border-radius: 50%;
  margin-right: 4px; vertical-align: middle;
}
.online-tag-dot.on { background: #34a853; }
.online-tag-dot.off { background: #dadce0; }

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
