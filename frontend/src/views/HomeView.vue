<template>
  <div class="home-page">
    <el-row :gutter="20">
      <!-- ── Main feed ─────────────────────────────────────────────────── -->
      <el-col :xs="24" :sm="24" :md="17">
        <el-card shadow="never" class="feed-card">
          <template #header>
            <div class="feed-header">
              <el-tabs v-model="activeTab" @tab-change="onTabChange" class="feed-tabs">
                <el-tab-pane label="最新" name="latest" />
                <el-tab-pane label="最热" name="hot" />
                <el-tab-pane label="精华" name="featured" />
              </el-tabs>
              <router-link to="/post" class="more-link">更多 →</router-link>
            </div>
          </template>

          <!-- Loading skeleton -->
          <template v-if="postsLoading">
            <div v-for="n in 6" :key="n" class="skeleton-item">
              <el-skeleton :rows="2" animated />
            </div>
          </template>

          <!-- Post list -->
          <template v-else>
            <el-empty v-if="posts.length === 0" description="暂无帖子" />
            <div
              v-for="post in posts"
              :key="post.id"
              class="post-item"
            >
              <!-- Badges row -->
              <div class="post-badges">
                <el-tag v-if="post.isTop === 1" type="danger" size="small" effect="plain">置顶</el-tag>
                <el-tag v-if="post.isFeatured === 1" type="warning" size="small" effect="plain">精华</el-tag>
                <el-tag
                  v-if="forumMap[post.forumId]"
                  type="info"
                  size="small"
                  effect="plain"
                  class="forum-tag"
                  @click.prevent="$router.push(`/forum/${post.forumId}`)"
                >
                  {{ forumMap[post.forumId] }}
                </el-tag>
              </div>

              <!-- Title -->
              <router-link :to="`/post/${post.id}`" class="post-title">
                {{ post.title }}
              </router-link>

              <!-- Excerpt -->
              <p class="post-excerpt">{{ truncateText(stripMarkdown(post.content), 80) }}</p>

              <!-- Meta row -->
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
                  <el-icon><ChatDotRound /></el-icon>
                  {{ formatCount(post.commentCount) }}
                </span>
                <span class="meta-stat">
                  <el-icon><Star /></el-icon>
                  {{ formatCount(post.likeCount) }}
                </span>
              </div>
            </div>
          </template>
        </el-card>
      </el-col>

      <!-- ── Sidebar ──────────────────────────────────────────────────── -->
      <el-col :xs="0" :sm="0" :md="7" class="sidebar-col">
        <!-- Hot novels card -->
        <el-card shadow="never" class="sidebar-card">
          <template #header>
            <div class="card-header">
              <el-icon><Reading /></el-icon>
              <span>热门小说</span>
              <router-link to="/novel" class="card-more">更多</router-link>
            </div>
          </template>

          <el-skeleton v-if="novelsLoading" :rows="6" animated />
          <template v-else>
            <el-empty v-if="novels.length === 0" description="暂无小说" :image-size="60" />
            <div
              v-for="novel in novels"
              :key="novel.id"
              class="novel-item"
            >
              <!-- Cover thumbnail -->
              <router-link :to="`/novel/${novel.id}`" class="novel-cover-wrap">
                <el-image
                  v-if="novel.cover"
                  :src="novel.cover"
                  fit="cover"
                  class="novel-cover"
                  lazy
                >
                  <template #error>
                    <div class="cover-placeholder"><el-icon><Picture /></el-icon></div>
                  </template>
                </el-image>
                <div v-else class="cover-placeholder"><el-icon><Picture /></el-icon></div>
              </router-link>

              <!-- Info -->
              <div class="novel-info">
                <router-link :to="`/novel/${novel.id}`" class="novel-title">
                  {{ novel.title }}
                </router-link>
                <div class="novel-meta">
                  <el-tag size="small" type="primary" effect="plain">{{ novel.category }}</el-tag>
                  <el-tag :type="novelStatusType(novel.status)" size="small" effect="plain">
                    {{ novelStatusLabel(novel.status) }}
                  </el-tag>
                </div>
                <div class="novel-stats">
                  <span>{{ formatCount(novel.wordCount) }}字</span>
                  <span>{{ novel.chapterCount ?? 0 }}章</span>
                </div>
              </div>
            </div>
          </template>
        </el-card>

        <!-- Forum + quick links (AppSidebar) -->
        <AppSidebar />
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Clock, View, ChatDotRound, Star, Reading, Picture } from '@element-plus/icons-vue'
import * as postApi from '@/api/post'
import * as novelApi from '@/api/novel'
import type { Post, Novel } from '@/api/types'
import { formatRelativeDate, formatCount, truncateText, novelStatusLabel, novelStatusType } from '@/utils/format'
import { useForumStore } from '@/stores/forum'
import AppSidebar from '@/components/layout/AppSidebar.vue'

const router = useRouter()
const forumStore = useForumStore()

// ── State ─────────────────────────────────────────────────────────────────────

const activeTab = ref<'latest' | 'hot' | 'featured'>('latest')
const posts = ref<Post[]>([])
const novels = ref<Novel[]>([])
const postsLoading = ref(false)
const novelsLoading = ref(false)

/** Forum id → name map for quick badge lookup. */
const forumMap = computed<Record<number, string>>(() => {
  const map: Record<number, string> = {}
  for (const f of forumStore.forums) map[f.id] = f.name
  return map
})

// ── Data loading ──────────────────────────────────────────────────────────────

async function loadPosts() {
  postsLoading.value = true
  try {
    if (activeTab.value === 'hot') {
      posts.value = await postApi.hotPosts(20)
    } else {
      const page = await postApi.listPosts(undefined, 1, 20)
      if (activeTab.value === 'featured') {
        posts.value = page.records.filter((p) => p.isFeatured === 1)
      } else {
        posts.value = page.records
      }
    }
  } catch {
    // ElMessage shown by http interceptor
  } finally {
    postsLoading.value = false
  }
}

async function loadNovels() {
  novelsLoading.value = true
  try {
    const page = await novelApi.listNovels(undefined, 1, 6)
    novels.value = page.records
  } catch {
    // handled globally
  } finally {
    novelsLoading.value = false
  }
}

function onTabChange() {
  loadPosts()
}

onMounted(() => {
  forumStore.fetchForums()
  loadPosts()
  loadNovels()
})

// ── Utility ───────────────────────────────────────────────────────────────────

/** Strip common markdown markers for a plain-text excerpt. */
function stripMarkdown(text: string): string {
  return text
    .replace(/#{1,6}\s+/g, '')      // headings
    .replace(/\*{1,3}(.+?)\*{1,3}/g, '$1') // bold / italic
    .replace(/`{1,3}[^`]*`{1,3}/g, '') // code
    .replace(/!\[.*?\]\(.*?\)/g, '') // images
    .replace(/\[(.+?)\]\(.*?\)/g, '$1') // links
    .replace(/\n+/g, ' ')
    .trim()
}
</script>

<style scoped>
.home-page {
  padding: 0;
}

/* ── Feed card ───────────────────────────────────────────────────────────── */
.feed-card {
  border-radius: 8px;
}

.feed-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.feed-tabs {
  flex: 1;
  margin-bottom: -14px; /* pull tabs flush with card border */
}

.more-link {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
  margin-left: 12px;
}

.more-link:hover {
  color: #409eff;
}

/* Skeleton */
.skeleton-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.skeleton-item:last-child {
  border-bottom: none;
}

/* Post item */
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

.forum-tag {
  cursor: pointer;
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

/* ── Sidebar ─────────────────────────────────────────────────────────────── */
.sidebar-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.card-more {
  margin-left: auto;
  font-size: 12px;
  color: #909399;
  font-weight: 400;
}

.card-more:hover {
  color: #409eff;
}

/* Novel items */
.novel-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.novel-item:last-child {
  border-bottom: none;
}

.novel-cover-wrap {
  flex-shrink: 0;
  width: 52px;
  height: 70px;
  border-radius: 4px;
  overflow: hidden;
  display: block;
}

.novel-cover {
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  width: 52px;
  height: 70px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 20px;
  border-radius: 4px;
}

.novel-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.novel-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-decoration: none;
}

.novel-title:hover {
  color: #409eff;
}

.novel-meta {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.novel-stats {
  font-size: 11px;
  color: #c0c4cc;
  display: flex;
  gap: 8px;
}
</style>
