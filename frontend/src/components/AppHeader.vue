<template>
  <header class="app-header">
    <div class="header-inner">
      <div class="header-left">
        <router-link to="/" class="logo-link">
          <span class="logo-icon">墨</span>
          <span class="logo-title">墨香论坛</span>
        </router-link>
      </div>

      <nav class="header-nav">
        <router-link to="/" class="nav-link" active-class="nav-link--active" exact>首页</router-link>
        <router-link to="/posts" class="nav-link" active-class="nav-link--active">帖子</router-link>
        <router-link to="/forums" class="nav-link" active-class="nav-link--active">版块</router-link>
      </nav>

      <div class="header-right">
        <router-link to="/post/create" class="btn-post">发帖</router-link>
        <template v-if="userStore.isLoggedIn">
          <div class="user-avatar" @click="router.push('/profile')" :title="userStore.userInfo?.username">
            <img v-if="userStore.userInfo?.avatar" :src="userStore.userInfo.avatar" alt="avatar" />
            <span v-else class="avatar-placeholder">{{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() }}</span>
          </div>
        </template>
        <template v-else>
          <router-link to="/login" class="btn-auth">登录</router-link>
          <router-link to="/register" class="btn-auth btn-auth--primary">注册</router-link>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
</script>

<style scoped>
.app-header {
  background: #ffffff;
  border-bottom: 2px solid #8B0000;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* Left: Logo */
.logo-link {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}

.logo-icon {
  width: 36px;
  height: 36px;
  background: #8B0000;
  color: #FAF7F0;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  flex-shrink: 0;
}

.logo-title {
  font-size: 20px;
  font-weight: bold;
  color: #8B0000;
  letter-spacing: 2px;
}

/* Middle: Nav */
.header-nav {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-link {
  color: #4a3728;
  text-decoration: none;
  font-size: 15px;
  letter-spacing: 1px;
  padding-bottom: 2px;
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s;
}

.nav-link:hover,
.nav-link--active {
  color: #8B0000;
  border-bottom-color: #8B0000;
}

/* Right: Actions */
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-post {
  background: #8B0000;
  color: #FAF7F0;
  padding: 6px 18px;
  border-radius: 4px;
  text-decoration: none;
  font-size: 14px;
  letter-spacing: 1px;
  transition: background 0.2s;
}

.btn-post:hover {
  background: #a00000;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 2px solid #8B0000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #FAF7F0;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  font-size: 14px;
  font-weight: bold;
  color: #8B0000;
}

.btn-auth {
  color: #8B0000;
  text-decoration: none;
  font-size: 14px;
  padding: 4px 12px;
  border-radius: 4px;
  border: 1px solid #8B0000;
  transition: background 0.2s, color 0.2s;
}

.btn-auth:hover {
  background: #8B0000;
  color: #FAF7F0;
}

.btn-auth--primary {
  background: #8B0000;
  color: #FAF7F0;
}

.btn-auth--primary:hover {
  background: #a00000;
}
</style>
