<template>
  <!-- Top navigation bar -->
  <el-header class="app-header" height="60px">
    <div class="header-inner">
      <!-- Logo -->
      <router-link to="/" class="logo">
        <el-icon :size="28" color="#409eff"><Reading /></el-icon>
        <span class="logo-text">墨香论坛</span>
      </router-link>

      <!-- Search box -->
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

      <!-- Right-side actions -->
      <div class="header-actions">
        <template v-if="userStore.isLoggedIn">
          <el-button type="primary" size="small" @click="router.push('/post/create')">
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
          <el-button @click="router.push('/login')">登录</el-button>
          <el-button type="primary" @click="router.push('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </el-header>

  <!-- Secondary navigation -->
  <div class="sub-nav">
    <div class="sub-nav-inner">
      <router-link to="/" class="sub-nav-item" active-class="active" exact>首页</router-link>
      <router-link to="/forum" class="sub-nav-item" active-class="active">板块</router-link>
      <router-link to="/novel" class="sub-nav-item" active-class="active">小说</router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, EditPen, User, Setting, Monitor, SwitchButton, Reading } from '@element-plus/icons-vue'
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
    await userStore.callLogout()
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
  position: sticky;
  top: 60px;
  z-index: 99;
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
</style>
