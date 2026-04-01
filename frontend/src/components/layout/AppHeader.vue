<template>
  <!-- Top navigation bar -->
  <header class="app-header">
    <div class="header-inner">
      <!-- Logo -->
      <router-link to="/" class="logo">
        <el-icon :size="26" color="#8B0000"><Reading /></el-icon>
        <span class="logo-text">墨香论坛</span>
      </router-link>

      <!-- Center nav links -->
      <nav class="nav-links">
        <router-link to="/" class="nav-item" active-class="active" exact>论坛首页</router-link>
        <router-link to="/forum" class="nav-item" active-class="active">板块分类</router-link>
        <router-link to="/novel" class="nav-item" active-class="active">小说书库</router-link>
      </nav>

      <!-- Right-side actions -->
      <div class="header-actions">
        <!-- Search -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索帖子、小说..."
          :prefix-icon="Search"
          clearable
          size="small"
          class="search-input"
          @keyup.enter="handleSearch"
        />

        <template v-if="userStore.isLoggedIn">
          <!-- Theme toggle -->
          <el-tooltip :content="themeTooltip" placement="bottom">
            <el-button circle size="small" class="icon-btn" @click="cycleTheme">
              <el-icon :size="15">
                <Sunny v-if="themeStore.mode === 'light'" />
                <Coffee v-else-if="themeStore.mode === 'warm'" />
                <Moon v-else />
              </el-icon>
            </el-button>
          </el-tooltip>

          <!-- Post button -->
          <el-button class="post-btn" @click="router.push('/post/create')">
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
                <el-button circle size="small" class="icon-btn" title="通知">
                  <el-icon :size="17"><Bell /></el-icon>
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

          <!-- User avatar dropdown -->
          <el-dropdown @command="handleUserCommand" trigger="click">
            <div class="user-info">
              <el-avatar :size="32" :src="userStore.userInfo?.avatar" class="user-avatar">
                {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.username }}</span>
              <el-icon :size="12" class="arrow-icon"><ArrowDown /></el-icon>
            </div>
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
          <!-- Theme toggle (logged out) -->
          <el-tooltip :content="themeTooltip" placement="bottom">
            <el-button circle size="small" class="icon-btn" @click="cycleTheme">
              <el-icon :size="15">
                <Sunny v-if="themeStore.mode === 'light'" />
                <Coffee v-else-if="themeStore.mode === 'warm'" />
                <Moon v-else />
              </el-icon>
            </el-button>
          </el-tooltip>
          <el-button class="login-btn" @click="router.push('/login')">登录</el-button>
          <el-button class="register-btn" @click="router.push('/register')">免费注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, EditPen, User, Setting, Monitor, SwitchButton,
  Reading, Bell, Sunny, Moon, Coffee, ArrowDown
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useThemeStore, type ThemeMode } from '@/stores/theme'
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
const themeStore = useThemeStore()
const searchKeyword = ref('')

const themeTooltip = computed(() => {
  const map: Record<ThemeMode, string> = {
    light: '切换至浅黄模式',
    warm:  '切换至夜间模式',
    dark:  '切换至日间模式'
  }
  return map[themeStore.mode]
})

const themeOrder: ThemeMode[] = ['light', 'warm', 'dark']
function cycleTheme() {
  const idx = themeOrder.indexOf(themeStore.mode)
  themeStore.setMode(themeOrder[(idx + 1) % themeOrder.length])
}

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
    // silently ignore
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
/* ── Header bar ─────────────────────────────────────────────── */
.app-header {
  background: var(--theme-bg-header, #FAF7F0);
  border-bottom: 1px solid var(--theme-border, #e0d5c5);
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: var(--theme-shadow, 0 2px 8px rgba(0, 0, 0, 0.06));
}

.header-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 10%;
  height: 64px;
}

/* ── Logo ────────────────────────────────────────────────────── */
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
  color: #333333;
  letter-spacing: 2px;
  font-family: var(--mx-font-serif, "SimSun", serif);
}

/* ── Center nav links ────────────────────────────────────────── */
.nav-links {
  display: flex;
  align-items: center;
  gap: 4px;
}

.nav-item {
  padding: 8px 16px;
  text-decoration: none;
  color: var(--theme-text-regular, #666666);
  font-size: 14px;
  font-family: var(--mx-font-serif, "SimSun", serif);
  border-bottom: 2px solid transparent;
  transition: color 0.2s, border-color 0.2s;
  white-space: nowrap;
}

.nav-item:hover,
.nav-item.active {
  color: #8B0000;
  border-bottom-color: #8B0000;
  text-decoration: none;
}

/* ── Right-side actions ──────────────────────────────────────── */
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.search-input {
  min-width: 160px;
  max-width: 240px;
  flex: 1;
}

/* Icon-only circular buttons */
.icon-btn {
  border: 1px solid var(--theme-border, #e0d5c5);
  background: transparent;
  color: var(--theme-text-regular, #666666);
  padding: 0;
}

.icon-btn:hover {
  color: #8B0000;
  border-color: #8B0000;
}

/* Post button — deep crimson with brush icon */
.post-btn {
  background-color: #8B0000;
  border-color: #8B0000;
  color: #ffffff;
  border-radius: var(--mx-radius, 8px);
  font-family: var(--mx-font-serif, "SimSun", serif);
  font-size: 13px;
  padding: 6px 14px;
  height: auto;
}

.post-btn:hover {
  background-color: #a50000;
  border-color: #a50000;
  color: #ffffff;
}

/* Login / Register buttons */
.login-btn {
  border: 1px solid var(--theme-border, #e0d5c5);
  background: transparent;
  color: var(--theme-text-regular, #666666);
  border-radius: var(--mx-radius-sm, 4px);
  font-size: 13px;
}

.login-btn:hover {
  border-color: #8B0000;
  color: #8B0000;
}

.register-btn {
  background-color: #8B0000;
  border-color: #8B0000;
  color: #ffffff;
  border-radius: var(--mx-radius-sm, 4px);
  font-size: 13px;
}

.register-btn:hover {
  background-color: #a50000;
  border-color: #a50000;
  color: #ffffff;
}

/* ── User info section ───────────────────────────────────────── */
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--mx-radius-sm, 4px);
  transition: background 0.2s;
}

.user-info:hover {
  background: var(--theme-bg-fill, #f5f0e8);
}

.user-avatar {
  background-color: #8B0000;
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}

.username {
  font-size: 13px;
  color: var(--theme-text-regular, #666666);
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  color: var(--theme-text-muted, #999999);
}

/* ── Notification badge ──────────────────────────────────────── */
.notif-badge {
  line-height: 1;
}

/* Notification panel */
.notif-panel {
  display: flex;
  flex-direction: column;
}

.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--theme-border-light, #f0e8da);
  margin-bottom: 4px;
}

.notif-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--theme-text-primary, #333333);
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
  border-bottom: 1px solid var(--theme-border-light, #f0e8da);
  cursor: pointer;
  font-size: 13px;
  color: var(--theme-text-regular, #666666);
  transition: background 0.15s;
}

.notif-item:last-child {
  border-bottom: none;
}

.notif-item:hover {
  background: var(--theme-bg-fill, #f5f0e8);
}

.notif-item.unread {
  background: #fdf0f0;
}

.notif-item.unread:hover {
  filter: brightness(0.97);
}

.notif-dot {
  flex-shrink: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #8B0000;
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
  color: var(--theme-text-placeholder, #bbbbbb);
  margin-left: auto;
  white-space: nowrap;
}
</style>

