<template>
  <el-config-provider :locale="zhCn">
    <el-container class="app-wrapper">
      <!-- 顶部导航 -->
      <el-header class="app-header" height="60px">
        <div class="header-inner">
          <!-- Logo -->
          <router-link to="/" class="logo">
            <el-icon :size="28" color="#409eff"><Reading /></el-icon>
            <span class="logo-text">墨香论坛</span>
          </router-link>

          <!-- 搜索框 -->
          <div class="search-bar">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索帖子、小说..."
              :prefix-icon="Search"
              clearable
              @keyup.enter="handleSearch"
              style="width: 340px"
            />
          </div>

          <!-- 右侧操作区 -->
          <div class="header-actions">
            <template v-if="userStore.isLoggedIn">
              <el-button type="primary" size="small" @click="$router.push('/post/create')">
                <el-icon><EditPen /></el-icon>发帖
              </el-button>
              <el-dropdown @command="handleUserCommand" trigger="click">
                <el-avatar
                  :size="36"
                  :src="userStore.userInfo?.avatar"
                  class="user-avatar"
                >
                  {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() }}
                </el-avatar>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="profile">
                      <el-icon><User /></el-icon>个人主页
                    </el-dropdown-item>
                    <el-dropdown-item command="settings">
                      <el-icon><Setting /></el-icon>账号设置
                    </el-dropdown-item>
                    <el-dropdown-item v-if="userStore.isAdmin" command="admin" divided>
                      <el-icon><Monitor /></el-icon>管理后台
                    </el-dropdown-item>
                    <el-dropdown-item command="logout" divided>
                      <el-icon><SwitchButton /></el-icon>退出登录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <template v-else>
              <el-button @click="$router.push('/login')">登录</el-button>
              <el-button type="primary" @click="$router.push('/register')">注册</el-button>
            </template>
          </div>
        </div>
      </el-header>

      <!-- 二级导航 -->
      <div class="sub-nav">
        <div class="sub-nav-inner">
          <router-link to="/" class="sub-nav-item" active-class="active" exact>首页</router-link>
          <router-link to="/forum" class="sub-nav-item" active-class="active">板块</router-link>
          <router-link to="/novel" class="sub-nav-item" active-class="active">小说</router-link>
        </div>
      </div>

      <!-- 主体内容 -->
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>

      <!-- 底部 -->
      <el-footer class="app-footer" height="60px">
        <div class="footer-inner">
          <span>© 2024 墨香论坛 · 小说爱好者的家园</span>
          <span class="footer-links">
            <a href="#">关于我们</a>
            <a href="#">帮助中心</a>
            <a href="#">意见反馈</a>
          </span>
        </div>
      </el-footer>
    </el-container>
  </el-config-provider>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, EditPen, User, Setting, Monitor, SwitchButton, Reading } from '@element-plus/icons-vue'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const searchKeyword = ref('')

function handleSearch() {
  const kw = searchKeyword.value.trim()
  if (!kw) return
  router.push({ path: '/post', query: { keyword: kw } })
}

async function handleUserCommand(command: string) {
  if (command === 'logout') {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    userStore.callLogout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'profile') {
    router.push(`/user/${userStore.userInfo?.id}`)
  } else if (command === 'settings') {
    router.push('/settings')
  } else if (command === 'admin') {
    router.push('/admin')
  }
}
</script>

<style scoped>
.app-wrapper {
  min-height: 100vh;
  background-color: #f5f6f7;
}

.app-header {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex-shrink: 0;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  letter-spacing: 1px;
}

.search-bar {
  flex: 1;
  display: flex;
  justify-content: center;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.user-avatar {
  cursor: pointer;
  background-color: #409eff;
  color: #fff;
  font-weight: 600;
}

.sub-nav {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.sub-nav-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  gap: 4px;
  padding: 0 8px;
}

.sub-nav-item {
  padding: 10px 16px;
  text-decoration: none;
  color: #606266;
  font-size: 14px;
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s;
}

.sub-nav-item:hover,
.sub-nav-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
}

.app-main {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 20px 12px;
  box-sizing: border-box;
}

.app-footer {
  background: #fff;
  border-top: 1px solid #e4e7ed;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #909399;
  font-size: 13px;
}

.footer-links {
  display: flex;
  gap: 16px;
}

.footer-links a {
  color: #909399;
  text-decoration: none;
}

.footer-links a:hover {
  color: #409eff;
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
  background-color: #f5f6f7;
  color: #303133;
  line-height: 1.6;
}

a {
  color: #409eff;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

.el-main {
  padding: 0 !important;
}
</style>
