<template>
  <div class="home-page">
    <!-- ── Hero Section ────────────────────────────────────────────── -->
    <section class="hero-section">
      <div class="hero-content">
        <h1 class="hero-title">墨香论坛</h1>
        <p class="hero-subtitle">文字为墨，思想为香——小说爱好者的雅集之地</p>
        <div class="hero-actions">
          <router-link to="/forum" class="btn btn-primary">
            <el-icon><Reading /></el-icon>
            进入论坛
          </router-link>
          <router-link to="/register" class="btn btn-secondary">
            免费注册
          </router-link>
        </div>
      </div>
    </section>

    <!-- ── Stats Section ───────────────────────────────────────────── -->
    <section class="stats-section">
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon">
            <el-icon :size="28" color="#8B0000"><UserFilled /></el-icon>
          </div>
          <div class="stat-number">{{ statsDisplay.members }}</div>
          <div class="stat-label">注册会员</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <el-icon :size="28" color="#8B0000"><ChatDotSquare /></el-icon>
          </div>
          <div class="stat-number">{{ statsDisplay.posts }}</div>
          <div class="stat-label">发布帖子</div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">
            <el-icon :size="28" color="#8B0000"><Collection /></el-icon>
          </div>
          <div class="stat-number">{{ statsDisplay.novels }}</div>
          <div class="stat-label">收录小说</div>
        </div>
      </div>
    </section>

    <!-- ── Forum Content ────────────────────────────────────────────── -->
    <div class="home-layout">
      <!-- Main feed -->
      <div class="main-col">
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
      </div>

      <!-- Sidebar -->
      <div class="sidebar-col">
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
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Clock, View, ChatDotRound, Star, Reading, Picture,
  UserFilled, ChatDotSquare, Collection
} from '@element-plus/icons-vue'
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

// ── Stats display ─────────────────────────────────────────────────────────────

const statsDisplay = ref({ members: '—', posts: '—', novels: '—' })

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
    if (page.total != null && page.total > 0) {
      statsDisplay.value.novels = formatCount(page.total)
    }
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
.home-page {
  padding: 0;
}

/* ── Hero Section ────────────────────────────────────────────────── */
.hero-section {
  width: 100%;
  background: linear-gradient(135deg, #FAF7F0 0%, #f5ece0 100%);
  border-bottom: 1px solid #e0d5c5;
  padding: 64px 10% 56px;
  text-align: center;
}

.hero-content {
  max-width: 640px;
  margin: 0 auto;
}

.hero-title {
  font-size: 56px;
  font-weight: 700;
  color: #333333;
  letter-spacing: 8px;
  font-family: var(--mx-font-serif, "Source Han Serif CN", "SimSun", serif);
  line-height: 1.2;
  margin-bottom: 16px;
}

.hero-subtitle {
  font-size: 16px;
  color: #666666;
  letter-spacing: 3px;
  line-height: 1.8;
  margin-bottom: 36px;
  font-family: var(--mx-font-serif, "SimSun", serif);
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 12px 32px;
  border-radius: 8px;
  font-size: 15px;
  font-family: var(--mx-font-serif, "SimSun", serif);
  text-decoration: none;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.btn-primary {
  background-color: #8B0000;
  color: #ffffff;
  border-color: #8B0000;
}

.btn-primary:hover {
  background-color: #a50000;
  border-color: #a50000;
  color: #ffffff;
  text-decoration: none;
}

.btn-secondary {
  background-color: #FAF7F0;
  color: #5c3d1e;
  border-color: #c8b89a;
}

.btn-secondary:hover {
  background-color: #f0ece0;
  border-color: #8B0000;
  color: #8B0000;
  text-decoration: none;
}

/* ── Stats Section ───────────────────────────────────────────────── */
.stats-section {
  padding: 40px 10%;
  background: #FAF7F0;
}

.stats-grid {
  display: flex;
  gap: 24px;
  justify-content: center;
  max-width: 900px;
  margin: 0 auto;
}

.stat-card {
  flex: 1;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  padding: 32px 24px;
  text-align: center;
  border: 1px solid #f0e8da;
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  margin-bottom: 12px;
  display: flex;
  justify-content: center;
}

.stat-number {
  font-size: 32px;
  font-weight: 700;
  color: #333333;
  letter-spacing: 1px;
  font-family: var(--mx-font-serif, "SimSun", serif);
  margin-bottom: 6px;
}

.stat-label {
  font-size: 13px;
  color: #999999;
  letter-spacing: 1px;
}

/* ── Two-column flex layout ──────────────────────────────────────── */
.home-layout {
  display: flex;
  gap: 20px;
  align-items: flex-start;
  padding: 24px 10% 40px;
}

.main-col {
  flex: 1;
  min-width: 0;
}

/* ── Feed card ───────────────────────────────────────────────────── */
.feed-card {
  border-radius: 8px;
  border-color: #e0d5c5;
}

.feed-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.feed-tabs {
  flex: 1;
  margin-bottom: -14px;
}

.more-link {
  font-size: 13px;
  color: #999999;
  white-space: nowrap;
  margin-left: 12px;
}

.more-link:hover {
  color: #8B0000;
}

/* Skeleton */
.skeleton-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0e8da;
}

.skeleton-item:last-child {
  border-bottom: none;
}

/* Post item */
.post-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0e8da;
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
  color: #333333;
  line-height: 1.5;
  margin-bottom: 4px;
  text-decoration: none;
  font-family: var(--mx-font-serif, "SimSun", serif);
}

.post-title:hover {
  color: #8B0000;
  text-decoration: none;
}

.post-excerpt {
  font-size: 13px;
  color: #999999;
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
  color: #bbbbbb;
}

.meta-time,
.meta-stat {
  display: flex;
  align-items: center;
  gap: 3px;
}

/* ── Sidebar ─────────────────────────────────────────────────────── */
.sidebar-col {
  min-width: 260px;
  width: 260px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (max-width: 767px) {
  .sidebar-col {
    display: none;
  }

  .home-layout {
    padding: 16px 4% 32px;
  }

  .hero-section,
  .stats-section {
    padding-left: 4%;
    padding-right: 4%;
  }

  .stats-grid {
    flex-direction: column;
  }

  .hero-title {
    font-size: 36px;
  }
}

.sidebar-card {
  border-radius: 8px;
  border-color: #e0d5c5;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #333333;
  font-family: var(--mx-font-serif, "SimSun", serif);
}

.card-more {
  margin-left: auto;
  font-size: 12px;
  color: #999999;
  font-weight: 400;
  text-decoration: none;
}

.card-more:hover {
  color: #8B0000;
  text-decoration: none;
}

/* Novel items */
.novel-item {
  display: flex;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #f0e8da;
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
  background: #f5f0e8;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c8b89a;
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
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-decoration: none;
  font-family: var(--mx-font-serif, "SimSun", serif);
}

.novel-title:hover {
  color: #8B0000;
  text-decoration: none;
}

.novel-meta {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.novel-stats {
  font-size: 11px;
  color: #bbbbbb;
  display: flex;
  gap: 8px;
}
</style>
