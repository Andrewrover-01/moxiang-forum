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

          <!-- Notification bell -->
          <el-popover
            v-model:visible="notifPopoverVisible"
            placement="bottom-end"
            :width="360"
            trigger="click"
            popper-class="notif-popover"
            @show="onNotifOpen"
          >
            <template #reference>
              <el-badge :value="unreadCount || undefined" :max="99" class="notif-badge">
                <el-button circle size="small" class="notif-btn" title="通知">
                  <el-icon :size="18"><Bell /></el-icon>
                </el-button>
              </el-badge>
            </template>

            <!-- Notification panel -->
            <div class="notif-panel">
              <div class="notif-header">
                <span class="notif-title">通知</span>
                <el-button
                  v-if="notifications.length"
                  text
                  size="small"
                  :loading="markingAll"
                  @click="handleMarkAllRead"
                >
                  全部已读
                </el-button>
              </div>

              <el-skeleton v-if="notifLoading" :rows="3" animated />
              <el-empty v-else-if="!notifications.length" description="暂无通知" :image-size="60" />
              <ul v-else class="notif-list">
                <li
                  v-for="n in notifications"
                  :key="n.id"
                  class="notif-item"
                  :class="{ unread: n.isRead === 0 }"
                  @click="handleNotifClick(n)"
                >
                  <span class="notif-dot" v-if="n.isRead === 0" />
                  <span class="notif-content">{{ n.content }}</span>
                  <span class="notif-time">{{ n.createdAt ? formatRelativeDate(n.createdAt) : '' }}</span>
                </li>
              </ul>
            </div>
          </el-popover>

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
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, EditPen, User, Setting, Monitor, SwitchButton, Reading, Bell } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import {
  listNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
  type Notification
} from '@/api/notification'
import { formatRelativeDate } from '@/utils/format'

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

// ── Notifications ─────────────────────────────────────────────────────────────

const unreadCount = ref(0)
const notifications = ref<Notification[]>([])
const notifLoading = ref(false)
const notifPopoverVisible = ref(false)
const markingAll = ref(false)

let pollTimer: ReturnType<typeof setInterval> | null = null

async function fetchUnreadCount() {
  if (!userStore.isLoggedIn) return
  try {
    unreadCount.value = await getUnreadCount()
  } catch {
    // silently ignore — badge just won't update
  }
}

async function onNotifOpen() {
  notifLoading.value = true
  try {
    const page = await listNotifications(1, 20)
    notifications.value = page.records
    unreadCount.value = 0
  } catch {
    // ignore
  } finally {
    notifLoading.value = false
  }
}

async function handleNotifClick(n: Notification) {
  if (n.isRead === 0) {
    try {
      await markAsRead(n.id)
      n.isRead = 1
    } catch {
      // ignore
    }
  }
  notifPopoverVisible.value = false
  if (n.targetId) {
    router.push(`/post/${n.targetId}`)
  }
}

async function handleMarkAllRead() {
  markingAll.value = true
  try {
    await markAllAsRead()
    notifications.value.forEach((n) => (n.isRead = 1))
    unreadCount.value = 0
  } catch {
    ElMessage.error('操作失败')
  } finally {
    markingAll.value = false
  }
}

// Start polling when logged in, stop when not
watch(
  () => userStore.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      fetchUnreadCount()
      pollTimer = setInterval(fetchUnreadCount, 30_000)
    } else {
      if (pollTimer) clearInterval(pollTimer)
      pollTimer = null
      unreadCount.value = 0
      notifications.value = []
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
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

/* Notification bell */
.notif-badge {
  line-height: 1;
}

.notif-btn {
  border: none;
  background: transparent;
  color: #606266;
  padding: 0;
}

.notif-btn:hover {
  color: #409eff;
}

/* Notification panel (inside popover) */
.notif-panel {
  display: flex;
  flex-direction: column;
}

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 4px;
}

.notif-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.notif-list {
  list-style: none;
  margin: 0;
  padding: 0;
  max-height: 320px;
  overflow-y: auto;
}

.notif-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 4px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: background 0.15s;
}

.notif-item:last-child {
  border-bottom: none;
}

.notif-item:hover {
  background: #f5f7fa;
}

.notif-item.unread {
  background: #ecf5ff;
}

.notif-item.unread:hover {
  background: #d9ecff;
}

.notif-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #409eff;
  margin-top: 4px;
}

.notif-content {
  flex: 1;
  word-break: break-word;
  line-height: 1.5;
}

.notif-time {
  flex-shrink: 0;
  font-size: 11px;
  color: #c0c4cc;
  margin-left: auto;
  white-space: nowrap;
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
