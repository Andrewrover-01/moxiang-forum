<template>
  <el-container class="admin-wrapper">
    <!-- Left sidebar -->
    <el-aside width="220px" class="admin-aside">
      <!-- Branding -->
      <div class="admin-brand">
        <el-icon :size="22" color="#409eff"><Monitor /></el-icon>
        <span>墨香后台</span>
      </div>

      <!-- Navigation menu -->
      <el-menu
        :default-active="activeMenu"
        router
        class="admin-menu"
        background-color="#001529"
        text-color="#ffffffa8"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/admin">
          <el-icon><DataAnalysis /></el-icon>
          <span>数据统计</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/posts">
          <el-icon><Document /></el-icon>
          <span>帖子管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/forums">
          <el-icon><Grid /></el-icon>
          <span>板块管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/novels">
          <el-icon><Reading /></el-icon>
          <span>小说管理</span>
        </el-menu-item>
      </el-menu>

      <!-- Back to site link -->
      <div class="admin-back">
        <router-link to="/" class="back-link">
          <el-icon><ArrowLeft /></el-icon>返回前台
        </router-link>
      </div>
    </el-aside>

    <!-- Right content area -->
    <el-container class="admin-body">
      <!-- Top bar -->
      <el-header class="admin-header" height="56px">
        <div class="admin-header-inner">
          <span class="page-title">{{ currentTitle }}</span>
          <div class="admin-user">
            <el-avatar :size="28" :src="userStore.userInfo?.avatar" class="mini-avatar">
              {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() }}
            </el-avatar>
            <span class="username">{{ userStore.userInfo?.username }}</span>
          </div>
        </div>
      </el-header>

      <!-- Page content -->
      <el-main class="admin-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Monitor, DataAnalysis, User, Document, Grid, Reading, ArrowLeft } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

/** Active menu index = current route path (normalise trailing slash). */
const activeMenu = computed(() => route.path.replace(/\/$/, '') || '/admin')

/** Page title from route meta, falls back to "管理后台". */
const currentTitle = computed(() => (route.meta.title as string | undefined) ?? '管理后台')
</script>

<style scoped>
.admin-wrapper {
  height: 100vh;
  overflow: hidden;
}

/* ── Sidebar ────────────────────────────────────────────────────────────────── */
.admin-aside {
  background-color: #001529;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px;
  font-size: 16px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
  border-bottom: 1px solid #ffffff14;
  flex-shrink: 0;
}

.admin-menu {
  flex: 1;
  border: none;
  overflow-y: auto;
}

/* Override Element-Plus active item colour inside dark sidebar */
:deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
  color: #fff !important;
}

:deep(.el-menu-item:hover) {
  background-color: #ffffff14 !important;
}

.admin-back {
  flex-shrink: 0;
  border-top: 1px solid #ffffff14;
  padding: 12px 20px;
}

.back-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #ffffffa8;
  font-size: 13px;
  text-decoration: none;
  transition: color 0.15s;
}

.back-link:hover {
  color: #fff;
  text-decoration: none;
}

/* ── Top bar ─────────────────────────────────────────────────────────────────── */
.admin-body {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  flex-shrink: 0;
}

.admin-header-inner {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-avatar {
  background-color: #409eff;
  color: #fff;
  font-weight: 600;
  cursor: default;
}

.username {
  font-size: 13px;
  color: #606266;
}

/* ── Content ─────────────────────────────────────────────────────────────────── */
.admin-main {
  background: #f5f6f7;
  overflow-y: auto;
  padding: 24px !important;
}

/* Page transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.12s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
