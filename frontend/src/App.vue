<template>
  <el-config-provider :locale="zhCn">
    <el-container class="app-wrapper">
      <AppHeader />

      <!-- Main content -->
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>

      <AppFooter />
    </el-container>
  </el-config-provider>
</template>

<script setup lang="ts">
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
</script>

<style scoped>
.app-wrapper {
  min-height: 100vh;
  background-color: var(--theme-bg-page, #f5f6f7);
}

.app-main {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 20px 12px;
  box-sizing: border-box;
}

/* Page transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<style>
/* Global resets */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC',
    'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  background-color: var(--theme-bg-page, #f5f6f7);
  color: var(--theme-text-primary, #303133);
  line-height: 1.6;
}

a {
  color: var(--theme-link, #409eff);
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

.el-main {
  padding: 0 !important;
}
</style>
