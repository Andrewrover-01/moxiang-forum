<template>
  <div class="home-container">
    <el-container>
      <el-header class="site-header">
        <div class="header-left">
          <span class="site-title">墨香论坛</span>
        </div>
        <div class="header-right">
          <template v-if="userStore.isLoggedIn">
            <span class="username">{{ userStore.userInfo?.username }}</span>
            <el-button text @click="handleLogout">退出登录</el-button>
          </template>
          <template v-else>
            <el-button @click="router.push('/login')">登录</el-button>
            <el-button type="primary" @click="router.push('/register')">注册</el-button>
          </template>
        </div>
      </el-header>

      <el-main>
        <el-row :gutter="20">
          <el-col :span="16">
            <el-card class="forum-list-card">
              <template #header>
                <span>版块列表</span>
              </template>
              <el-skeleton :loading="loading" animated :rows="4">
                <template #default>
                  <el-table :data="forums" style="width: 100%">
                    <el-table-column prop="name" label="版块名称" />
                    <el-table-column prop="description" label="简介" />
                    <el-table-column prop="postCount" label="帖子数" width="100" />
                    <el-table-column label="操作" width="100">
                      <template #default="{ row }">
                        <el-button text type="primary" @click="router.push(`/forum/${row.id}`)">
                          进入
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
              </el-skeleton>
            </el-card>
          </el-col>
        </el-row>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getForumList } from '@/api/forum'
import { logout } from '@/api/user'
import type { Forum } from '@/types/api'

const router = useRouter()
const userStore = useUserStore()
const forums = ref<Forum[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await getForumList()
    forums.value = data.data
  } finally {
    loading.value = false
  }
})

async function handleLogout() {
  try {
    await logout()
  } catch {
    // 无论后端是否响应，本地都清除登录状态
  }
  userStore.clearToken()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.site-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
}
.site-title {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}
.username {
  margin-right: 12px;
  color: #606266;
}
.home-container {
  min-height: 100vh;
}
</style>
