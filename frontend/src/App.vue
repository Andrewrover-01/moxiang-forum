<template>
  <el-config-provider>
    <AppHeader />
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { getUserInfo } from '@/api/user'
import { useUserStore } from '@/store/user'
import AppHeader from '@/components/AppHeader.vue'

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

<style>
*,
*::before,
*::after {
  box-sizing: border-box;
}

html,
body {
  margin: 0;
  padding: 0;
  background-color: #FAF7F0;
  font-family: "Source Han Serif CN", "SimSun", serif;
  color: #2c1a0e;
  min-height: 100vh;
}

#app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

a {
  color: #8B0000;
}

a:hover {
  color: #a00000;
}
</style>
