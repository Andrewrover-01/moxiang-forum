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
  background-color: var(--mx-bg-page, #FAF7F0);
}

.app-main {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 24px 12px;
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
  font-family: var(--mx-font-serif, "Source Han Serif CN", "SimSun", Georgia, serif);
  background-color: var(--mx-bg-page, #FAF7F0);
  color: var(--mx-text-primary, #333333);
  line-height: 1.6;
}

a {
  color: var(--theme-link, #8B0000);
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

.el-main {
  padding: 0 !important;
}
</style>
