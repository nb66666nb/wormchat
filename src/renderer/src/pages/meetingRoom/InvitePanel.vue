<template>
  <el-dialog
    v-model="dialogVisible"
    title="邀请参会者"
    width="400px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="invite-content">
    </div>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

const dialogVisible = ref(false)

const show = () => {
  dialogVisible.value = true
}

const handleClose = () => {
  dialogVisible.value = false
}

defineExpose({ show })
</script>

<style lang="css" scoped>
.invite-content {
  min-height: 200px;
  padding: 8px 0;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
