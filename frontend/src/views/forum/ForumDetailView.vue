<template>
  <div class="forum-detail-page">
    <!-- Breadcrumb -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/forum' }">全部板块</el-breadcrumb-item>
      <el-breadcrumb-item>{{ forum?.name ?? '板块详情' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-row :gutter="20">
      <!-- ── Main content ───────────────────────────────────────────────── -->
      <el-col :xs="24" :sm="24" :md="17">

        <!-- Forum header card -->
        <el-card shadow="never" class="forum-header-card">
          <el-skeleton v-if="forumLoading" :rows="2" animated />
          <div v-else-if="forum" class="forum-header">
            <div class="forum-header-icon">
              <el-icon v-if="forum.icon" class="header-icon">
                <component :is="forum.icon" />
              </el-icon>
              <el-icon v-else class="header-icon"><ChatDotRound /></el-icon>
            </div>
            <div class="forum-header-info">
              <h1 class="forum-title">{{ forum.name }}</h1>
              <p class="forum-desc">{{ forum.description || '暂无简介' }}</p>
              <div class="forum-meta">
                <span class="meta-stat">
                  <el-icon><Document /></el-icon>
                  {{ formatCount(forum.postCount) }} 帖子
                </span>
                <span v-if="forum.createdAt" class="meta-stat">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDateOnly(forum.createdAt) }} 创建
                </span>
              </div>
            </div>
            <div class="forum-header-actions">
              <el-button
                v-if="userStore.isLoggedIn"
                type="primary"
                :icon="EditPen"
                @click="router.push({ name: 'post-create', query: { forumId: forum.id } })"
              >
                发布帖子
              </el-button>
            </div>
          </div>
          <el-result
            v-else
            icon="warning"
            title="板块不存在"
            sub-title="该板块可能已被删除或地址有误"
          >
            <template #extra>
              <el-button @click="router.push('/forum')">返回板块列表</el-button>
            </template>
          </el-result>
        </el-card>

        <!-- Posts card -->
        <el-card shadow="never" class="posts-card" v-if="forum">
          <template #header>
            <div class="posts-header">
              <span class="posts-title">帖子列表</span>
              <span class="posts-total">共 {{ total }} 篇</span>
            </div>
          </template>

          <!-- Loading skeleton -->
          <template v-if="postsLoading">
            <div v-for="n in 6" :key="n" class="skeleton-item">
              <el-skeleton :rows="2" animated />
            </div>
          </template>

          <template v-else>
            <el-empty v-if="posts.length === 0" description="暂无帖子，快来发第一篇！" />
            <div
              v-for="post in posts"
              :key="post.id"
              class="post-item"
            >
              <!-- Badges -->
              <div class="post-badges">
                <el-tag v-if="post.isTop === 1" type="danger" size="small" effect="plain">置顶</el-tag>
                <el-tag v-if="post.isFeatured === 1" type="warning" size="small" effect="plain">精华</el-tag>
                <el-tag v-if="post.status === 1" type="info" size="small" effect="plain">已锁定</el-tag>
              </div>

              <!-- Title -->
              <router-link :to="`/post/${post.id}`" class="post-title">
                {{ post.title }}
              </router-link>

              <!-- Excerpt -->
              <p class="post-excerpt">{{ truncateText(stripMarkdown(post.content), 80) }}</p>

              <!-- Meta -->
              <div class="post-meta">
                <span class="meta-time">
                  <el-icon><Clock /></el-icon>
                  {{ formatRelativeDate(post.createdAt) }}
                </span>
                <span class="meta-stat">
                  <el-icon><View /></el-icon>
                  {{ formatCount(post.viewCount) }}
                </span>
                <span class="meta-stat">
                  <el-icon><ChatDotSquare /></el-icon>
                  {{ formatCount(post.commentCount) }}
                </span>
                <span class="meta-stat">
                  <el-icon><Star /></el-icon>
                  {{ formatCount(post.likeCount) }}
                </span>
              </div>
            </div>
          </template>

          <!-- Pagination -->
          <div v-if="total > pageSize" class="pagination-wrap">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="loadPosts"
            />
          </div>
        </el-card>
      </el-col>

      <!-- ── Sidebar ──────────────────────────────────────────────────── -->
      <el-col :xs="0" :sm="0" :md="7">
        <AppSidebar />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Document, Calendar, Clock, View, ChatDotSquare, Star, EditPen } from '@element-plus/icons-vue'
import * as forumApi from '@/api/forum'
import * as postApi from '@/api/post'
import type { Forum, Post } from '@/api/types'
import { formatCount, formatRelativeDate, formatDateOnly, truncateText } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import AppSidebar from '@/components/layout/AppSidebar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ── State ─────────────────────────────────────────────────────────────────────

const forum = ref<Forum | null>(null)
const forumLoading = ref(false)

const posts = ref<Post[]>([])
const postsLoading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

// ── Data loading ──────────────────────────────────────────────────────────────

async function loadForum() {
  forumLoading.value = true
  try {
    forum.value = await forumApi.getForum(route.params.id as string)
  } catch {
    forum.value = null
  } finally {
    forumLoading.value = false
  }
}

async function loadPosts() {
  if (!forum.value) return
  postsLoading.value = true
  try {
    const page = await postApi.listPosts(forum.value.id, currentPage.value, pageSize)
    posts.value = page.records
    total.value = page.total
  } catch {
    // handled globally
  } finally {
    postsLoading.value = false
  }
}

onMounted(async () => {
  await loadForum()
  await loadPosts()
})

// ── Utility ───────────────────────────────────────────────────────────────────

function stripMarkdown(text: string): string {
  return text
    .replace(/#{1,6}\s+/g, '')
    .replace(/\*{1,3}(.+?)\*{1,3}/g, '$1')
    .replace(/`{1,3}[^`]*`{1,3}/g, '')
    .replace(/!\[.*?\]\(.*?\)/g, '')
    .replace(/\[(.+?)\]\(.*?\)/g, '$1')
    .replace(/\n+/g, ' ')
    .trim()
}
</script>

<style scoped>
.forum-detail-page {
  padding: 0;
}

.breadcrumb {
  margin-bottom: 16px;
}

/* Forum header */
.forum-header-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.forum-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.forum-header-icon {
  flex-shrink: 0;
}

.header-icon {
  font-size: 48px;
  color: #409eff;
}

.forum-header-info {
  flex: 1;
  min-width: 0;
}

.forum-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.forum-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 8px;
}

.forum-meta {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}

.forum-header-actions {
  flex-shrink: 0;
}

/* Posts card */
.posts-card {
  border-radius: 8px;
}

.posts-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.posts-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.posts-total {
  font-size: 13px;
  color: #909399;
}

/* Skeleton */
.skeleton-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.skeleton-item:last-child {
  border-bottom: none;
}

/* Post items */
.post-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.post-item:last-child {
  border-bottom: none;
}

.post-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 6px;
}

.post-title {
  display: block;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  line-height: 1.5;
  margin-bottom: 4px;
  text-decoration: none;
}

.post-title:hover {
  color: #409eff;
}

.post-excerpt {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  font-size: 12px;
  color: #c0c4cc;
}

.meta-time,
.meta-stat {
  display: flex;
  align-items: center;
  gap: 3px;
}

/* Pagination */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 20px 0 4px;
}
</style>
