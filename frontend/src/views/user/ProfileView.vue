<template>
  <div class="profile-page">
    <!-- ── Profile card ──────────────────────────────────────────────────── -->
    <el-card shadow="never" class="profile-card">
      <el-skeleton v-if="loading" :rows="3" animated />

      <el-result
        v-else-if="!profile"
        icon="warning"
        title="用户不存在"
        sub-title="该用户可能已注销或地址有误"
      >
        <template #extra>
          <el-button @click="router.back()">返回上一页</el-button>
        </template>
      </el-result>

      <div v-else class="profile-main">
        <div class="profile-left">
          <el-avatar :size="80" :src="profile.avatar" class="profile-avatar">
            {{ profile.username.charAt(0).toUpperCase() }}
          </el-avatar>
        </div>
        <div class="profile-right">
          <div class="profile-name-row">
            <span class="profile-username">{{ profile.username }}</span>
            <el-tag
              :type="profile.role === 'ADMIN' ? 'danger' : 'info'"
              size="small"
              effect="plain"
              class="role-tag"
            >
              {{ profile.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
            <el-button
              v-if="isSelf"
              type="primary"
              size="small"
              plain
              @click="router.push('/settings')"
            >
              编辑资料
            </el-button>
            <el-button
              v-else-if="userStore.isLoggedIn"
              :type="followingUser ? 'default' : 'primary'"
              size="small"
              :loading="followLoading"
              @click="handleToggleFollow"
            >
              {{ followingUser ? '已关注' : '关注' }}
            </el-button>
          </div>
          <p class="profile-bio">{{ profile.bio || '这个人很懒，什么都没留下…' }}</p>
          <div class="profile-stats">
            <span class="profile-stat">
              <strong>{{ followStats.followerCount }}</strong> 粉丝
            </span>
            <span class="profile-stat">
              <strong>{{ followStats.followingCount }}</strong> 关注
            </span>
          </div>
          <p class="profile-joined">注册于 {{ formatDateOnly(profile.createdAt) }}</p>
        </div>
      </div>
    </el-card>

    <!-- ── Content tabs ──────────────────────────────────────────────────── -->
    <el-card v-if="profile" shadow="never" class="content-card">
      <el-tabs v-model="activeTab">
        <!-- Posts tab -->
        <el-tab-pane label="他的帖子" name="posts">
          <el-skeleton v-if="postsLoading" :rows="4" animated />
          <el-empty v-else-if="!posts.length" description="暂无帖子" />
          <template v-else>
            <div
              v-for="post in posts"
              :key="post.id"
              class="post-item"
              @click="router.push(`/post/${post.id}`)"
            >
              <div class="post-item-title">{{ post.title }}</div>
              <div class="post-item-meta">
                <span class="meta-stat">
                  <el-icon><View /></el-icon>{{ post.viewCount }}
                </span>
                <span class="meta-stat">
                  <el-icon><Thumb /></el-icon>{{ post.likeCount }}
                </span>
                <span class="meta-stat">
                  <el-icon><ChatDotRound /></el-icon>{{ post.commentCount }}
                </span>
                <span class="meta-date">{{ formatRelativeDate(post.createdAt) }}</span>
              </div>
            </div>
            <!-- Pagination -->
            <el-pagination
              v-if="postsTotal > postsPageSize"
              class="pagination"
              background
              layout="prev, pager, next"
              :total="postsTotal"
              :page-size="postsPageSize"
              :current-page="postsPage"
              @current-change="onPostsPageChange"
            />
          </template>
        </el-tab-pane>

        <!-- Novels tab -->
        <el-tab-pane label="他的小说" name="novels">
          <el-skeleton v-if="novelsLoading" :rows="4" animated />
          <el-empty v-else-if="!novels.length" description="暂无小说" />
          <template v-else>
            <div
              v-for="novel in novels"
              :key="novel.id"
              class="novel-item"
              @click="router.push(`/novel/${novel.id}`)"
            >
              <el-image
                v-if="novel.cover"
                :src="novel.cover"
                class="novel-cover"
                fit="cover"
              />
              <div v-else class="novel-cover novel-cover-placeholder">
                <el-icon :size="28"><Reading /></el-icon>
              </div>
              <div class="novel-info">
                <div class="novel-title">{{ novel.title }}</div>
                <div class="novel-meta">
                  <el-tag size="small" effect="plain">{{ novel.category }}</el-tag>
                  <el-tag :type="novelStatusType(novel.status)" size="small" effect="plain">
                    {{ novelStatusLabel(novel.status) }}
                  </el-tag>
                  <span class="meta-text">{{ formatCount(novel.wordCount) }}字</span>
                  <span class="meta-text">{{ formatRelativeDate(novel.createdAt) }}</span>
                </div>
                <p class="novel-desc">{{ novel.description || '暂无简介' }}</p>
              </div>
            </div>
            <!-- Pagination -->
            <el-pagination
              v-if="novelsTotal > novelsPageSize"
              class="pagination"
              background
              layout="prev, pager, next"
              :total="novelsTotal"
              :page-size="novelsPageSize"
              :current-page="novelsPage"
              @current-change="onNovelsPageChange"
            />
          </template>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { View, ChatDotRound, Reading } from '@element-plus/icons-vue'
import { getUser } from '@/api/user'
import { listUserPosts } from '@/api/post'
import { listUserNovels } from '@/api/novel'
import { toggleFollow, isFollowing, getFollowStats } from '@/api/follow'
import type { UserInfo, Post, Novel } from '@/api/types'
import { useUserStore } from '@/stores/user'
import {
  formatDateOnly,
  formatRelativeDate,
  formatCount,
  novelStatusLabel,
  novelStatusType
} from '@/utils/format'

// Thumb icon alias — Element Plus uses "Pointer" for a thumbs-up shape
import { Pointer as Thumb } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ── Profile ──────────────────────────────────────────────────────────────────
const loading = ref(false)
const profile = ref<UserInfo | null>(null)

const isSelf = computed(
  () => !!userStore.userInfo && userStore.userInfo.id === profile.value?.id
)

// ── Follow ────────────────────────────────────────────────────────────────────
const followingUser = ref(false)
const followLoading = ref(false)
const followStats = ref({ followerCount: 0, followingCount: 0 })

async function loadFollowInfo(userId: number) {
  const [stats, following] = await Promise.all([
    getFollowStats(userId),
    userStore.isLoggedIn ? isFollowing(userId) : Promise.resolve(false)
  ])
  followStats.value = stats
  followingUser.value = following
}

async function handleToggleFollow() {
  if (!profile.value) return
  followLoading.value = true
  try {
    const result = await toggleFollow(profile.value.id)
    followingUser.value = result.following
    followStats.value.followerCount += result.following ? 1 : -1
  } catch {
    ElMessage.error('操作失败，请稍后再试')
  } finally {
    followLoading.value = false
  }
}

async function loadProfile(id: string | string[]) {
  loading.value = true
  profile.value = null
  followStats.value = { followerCount: 0, followingCount: 0 }
  followingUser.value = false
  try {
    profile.value = await getUser(id as string)
  } catch {
    ElMessage.error('加载用户信息失败')
  } finally {
    loading.value = false
  }
}

// ── Posts tab ────────────────────────────────────────────────────────────────
const postsLoading = ref(false)
const posts = ref<Post[]>([])
const postsTotal = ref(0)
const postsPage = ref(1)
const postsPageSize = 10

async function loadPosts() {
  if (!profile.value) return
  postsLoading.value = true
  try {
    const result = await listUserPosts(profile.value.id, postsPage.value, postsPageSize)
    posts.value = result.records
    postsTotal.value = result.total
  } catch {
    ElMessage.error('加载帖子失败')
  } finally {
    postsLoading.value = false
  }
}

function onPostsPageChange(page: number) {
  postsPage.value = page
  loadPosts()
}

// ── Novels tab ───────────────────────────────────────────────────────────────
const novelsLoading = ref(false)
const novels = ref<Novel[]>([])
const novelsTotal = ref(0)
const novelsPage = ref(1)
const novelsPageSize = 10

async function loadNovels() {
  if (!profile.value) return
  novelsLoading.value = true
  try {
    const result = await listUserNovels(profile.value.id, novelsPage.value, novelsPageSize)
    novels.value = result.records
    novelsTotal.value = result.total
  } catch {
    ElMessage.error('加载小说失败')
  } finally {
    novelsLoading.value = false
  }
}

function onNovelsPageChange(page: number) {
  novelsPage.value = page
  loadNovels()
}

// ── Tabs ─────────────────────────────────────────────────────────────────────
const activeTab = ref('posts')

watch(activeTab, (tab) => {
  if (tab === 'posts' && !posts.value.length) loadPosts()
  if (tab === 'novels' && !novels.value.length) loadNovels()
})

// ── Lifecycle ────────────────────────────────────────────────────────────────
async function init() {
  await loadProfile(route.params.id)
  if (profile.value) {
    await Promise.all([loadPosts(), loadFollowInfo(profile.value.id)])
  }
}

onMounted(init)

// Re-run when navigating between different user profiles
watch(() => route.params.id, (newId, oldId) => {
  if (newId && newId !== oldId) {
    postsPage.value = 1
    novelsPage.value = 1
    posts.value = []
    novels.value = []
    activeTab.value = 'posts'
    init()
  }
})
</script>

<style scoped>
.profile-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 16px;
}

.profile-card {
  margin-bottom: 16px;
  border-radius: 10px;
}

.content-card {
  border-radius: 10px;
}

/* ── Profile header ── */
.profile-main {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.profile-avatar {
  flex-shrink: 0;
  font-size: 28px;
}

.profile-right {
  flex: 1;
  min-width: 0;
}

.profile-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.profile-username {
  font-size: 20px;
  font-weight: 700;
  color: #1a1a2e;
}

.role-tag {
  flex-shrink: 0;
}

.profile-bio {
  color: #606266;
  font-size: 14px;
  margin: 0 0 6px;
}

.profile-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 6px;
  font-size: 13px;
  color: #606266;
}

.profile-stat strong {
  color: #303133;
  font-weight: 600;
}

.profile-joined {
  color: #909399;
  font-size: 12px;
  margin: 0;
}

/* ── Post list ── */
.post-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}
.post-item:last-child {
  border-bottom: none;
}
.post-item:hover .post-item-title {
  color: #409eff;
}

.post-item-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-item-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: #909399;
}

.meta-stat {
  display: flex;
  align-items: center;
  gap: 3px;
}

.meta-date {
  margin-left: auto;
}

/* ── Novel list ── */
.novel-item {
  display: flex;
  gap: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.15s;
}
.novel-item:last-child {
  border-bottom: none;
}
.novel-item:hover .novel-title {
  color: #409eff;
}

.novel-cover {
  width: 64px;
  height: 88px;
  border-radius: 4px;
  flex-shrink: 0;
  object-fit: cover;
}

.novel-cover-placeholder {
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

.novel-info {
  flex: 1;
  min-width: 0;
}

.novel-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.novel-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.meta-text {
  font-size: 12px;
  color: #909399;
}

.novel-desc {
  font-size: 13px;
  color: #606266;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ── Pagination ── */
.pagination {
  margin-top: 16px;
  justify-content: center;
}
</style>
