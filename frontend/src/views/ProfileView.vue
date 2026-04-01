<template>
  <div class="profile-page">
    <el-page-header @back="router.push('/')" title="个人资料" />
    <el-divider />
    <el-card v-if="userStore.userInfo?.username">
      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item label="用户名">
          <el-input :value="userStore.userInfo.username" disabled />
        </el-form-item>
        <el-form-item label="头像 URL">
          <el-input v-model="form.avatar" placeholder="请输入头像 URL" clearable />
        </el-form-item>
        <el-form-item label="个人简介">
          <el-input v-model="form.bio" type="textarea" :rows="4" placeholder="介绍一下自己" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSave">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { updateProfile } from '@/api/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  avatar: '',
  bio: ''
})

async function handleSave() {
  loading.value = true
  try {
    await updateProfile(form.avatar, form.bio)
    ElMessage.success('保存成功')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 640px;
  margin: 24px auto;
  padding: 0 16px;
}
</style>
