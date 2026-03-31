<template>
  <div class="dashboard">
    <!-- ── Stat cards ─────────────────────────────────────────────────── -->
    <el-row :gutter="16" class="stat-row">
      <el-col
        v-for="card in statCards"
        :key="card.label"
        :xs="12"
        :sm="12"
        :md="6"
      >
        <el-card shadow="never" class="stat-card" :style="{ borderTop: `3px solid ${card.color}` }">
          <el-skeleton v-if="statsLoading" :rows="2" animated />
          <template v-else>
            <div class="stat-icon" :style="{ background: card.color + '1a', color: card.color }">
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-body">
              <span class="stat-value">{{ formatCount(card.value) }}</span>
              <span class="stat-label">{{ card.label }}</span>
            </div>
          </template>
        </el-card>
      </el-col>
    </el-row>

    <!-- ── Bottom panels ──────────────────────────────────────────────── -->
    <el-row :gutter="16" class="panel-row">
      <!-- Recent posts -->
      <el-col :xs="24" :md="14">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span class="panel-title">最新帖子</span>
            <el-button link type="primary" @click="router.push('/admin/posts')">查看全部</el-button>
          </template>
          <el-skeleton v-if="postsLoading" :rows="5" animated />
          <el-empty v-else-if="!recentPosts.length" description="暂无帖子" />
          <el-table v-else :data="recentPosts" size="small" style="width:100%">
            <el-table-column label="标题" min-width="160">
              <template #default="{ row }">
                <span class="table-link" @click="router.push(`/post/${row.id}`)">{{ row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column label="浏览" prop="viewCount" width="64" align="right" />
            <el-table-column label="点赞" prop="likeCount" width="64" align="right" />
            <el-table-column label="评论" prop="commentCount" width="64" align="right" />
            <el-table-column label="时间" width="100" align="right">
              <template #default="{ row }">
                <span class="table-date">{{ formatRelativeDate(row.createdAt) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- Recent users -->
      <el-col :xs="24" :md="10">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <span class="panel-title">最新用户</span>
            <el-button link type="primary" @click="router.push('/admin/users')">查看全部</el-button>
          </template>
          <el-skeleton v-if="usersLoading" :rows="5" animated />
          <el-empty v-else-if="!recentUsers.length" description="暂无用户" />
          <el-table v-else :data="recentUsers" size="small" style="width:100%">
            <el-table-column label="用户名" min-width="100">
              <template #default="{ row }">
                <div class="user-cell">
                  <el-avatar :size="24" :src="row.avatar" class="mini-avatar">
                    {{ row.username.charAt(0).toUpperCase() }}
                  </el-avatar>
                  <span>{{ row.username }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="角色" width="80" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="row.role === 'ADMIN' ? 'danger' : 'info'"
                  size="small"
                  effect="plain"
                >
                  {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="注册时间" align="right">
              <template #default="{ row }">
                <span class="table-date">{{ formatRelativeDate(row.createdAt) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Document, Reading, Grid } from '@element-plus/icons-vue'
import { listAdminUsers } from '@/api/admin'
import { listPosts } from '@/api/post'
import { listNovels } from '@/api/novel'
import { pageForums } from '@/api/forum'
import type { Post, UserInfo } from '@/api/types'
import { formatCount, formatRelativeDate } from '@/utils/format'

const router = useRouter()

// ── Stats ─────────────────────────────────────────────────────────────────────
const statsLoading = ref(false)
const totalUsers = ref(0)
const totalPosts = ref(0)
const totalNovels = ref(0)
const totalForums = ref(0)

const statCards = computed(() => [
  { label: '注册用户', value: totalUsers.value,  icon: User,     color: '#409eff' },
  { label: '帖子总数', value: totalPosts.value,  icon: Document, color: '#67c23a' },
  { label: '小说总数', value: totalNovels.value, icon: Reading,  color: '#e6a23c' },
  { label: '板块总数', value: totalForums.value, icon: Grid,     color: '#f56c6c' },
])

async function loadStats() {
  statsLoading.value = true
  try {
    const [users, posts, novels, forums] = await Promise.all([
      listAdminUsers(1, 1),
      listPosts(undefined, 1, 1),
      listNovels(undefined, 1, 1),
      pageForums(1, 1),
    ])
    totalUsers.value  = users.total
    totalPosts.value  = posts.total
    totalNovels.value = novels.total
    totalForums.value = forums.total
  } catch {
    ElMessage.error('加载统计数据失败')
  } finally {
    statsLoading.value = false
  }
}

// ── Recent posts ──────────────────────────────────────────────────────────────
const postsLoading = ref(false)
const recentPosts = ref<Post[]>([])

async function loadRecentPosts() {
  postsLoading.value = true
  try {
    const result = await listPosts(undefined, 1, 5)
    recentPosts.value = result.records
  } catch {
    ElMessage.error('加载最新帖子失败')
  } finally {
    postsLoading.value = false
  }
}

// ── Recent users ──────────────────────────────────────────────────────────────
const usersLoading = ref(false)
const recentUsers = ref<UserInfo[]>([])

async function loadRecentUsers() {
  usersLoading.value = true
  try {
    const result = await listAdminUsers(1, 5)
    recentUsers.value = result.records
  } catch {
    ElMessage.error('加载最新用户失败')
  } finally {
    usersLoading.value = false
  }
}

// ── Lifecycle ─────────────────────────────────────────────────────────────────
onMounted(() => {
  loadStats()
  loadRecentPosts()
  loadRecentUsers()
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ── Stat cards ── */
.stat-row {
  margin-bottom: 0 !important;
}

.stat-card {
  border-radius: 8px;
  min-height: 96px;
  display: flex;
  align-items: center;
}

:deep(.el-card__body) {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

/* ── Panels ── */
.panel-row {
  margin-bottom: 0 !important;
}

.panel-card {
  border-radius: 8px;
  height: 100%;
}

:deep(.el-card__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.table-link {
  cursor: pointer;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: block;
  max-width: 200px;
}
.table-link:hover {
  color: #409eff;
}

.table-date {
  font-size: 12px;
  color: #909399;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mini-avatar {
  background-color: #409eff;
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}
</style>
