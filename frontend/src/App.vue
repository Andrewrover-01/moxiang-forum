<template>
  <el-config-provider>
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { getUserInfo } from '@/api/user'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()

/** 应用启动时若已有 token，刷新用户信息 */
onMounted(async () => {
  if (userStore.isLoggedIn) {
    try {
      const { data } = await getUserInfo()
      userStore.setUserInfo({
        id: data.data.id,
        username: data.data.username,
        avatar: data.data.avatar,
        role: data.data.role
      })
    } catch {
      // token 已失效，清除登录状态（request 拦截器会处理 401）
    }
  }
})
</script>
