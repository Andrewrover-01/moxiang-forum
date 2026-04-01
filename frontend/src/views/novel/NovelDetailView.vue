<template>
  <div class="novel-detail-page">
    <!-- Breadcrumb -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/novel' }">小说列表</el-breadcrumb-item>
      <el-breadcrumb-item>{{ novel?.title ?? '小说详情' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-row :gutter="20">
      <!-- ── Main content ───────────────────────────────────────────────── -->
      <el-col :xs="24" :sm="24" :md="17">

        <!-- Novel header card -->
        <el-card shadow="never" class="header-card">
          <el-skeleton v-if="novelLoading" :rows="5" animated />

          <el-result
            v-else-if="!novel"
            icon="warning"
            title="小说不存在"
            sub-title="该小说可能已被删除或地址有误"
          >
            <template #extra>
              <el-button @click="router.push('/novel')">返回小说列表</el-button>
            </template>
          </el-result>

          <div v-else class="novel-header">
            <!-- Cover -->
            <div class="cover-section">
              <el-image
                v-if="novel.cover"
                :src="novel.cover"
                fit="cover"
                class="cover-img"
                lazy
              >
                <template #error>
                  <div class="cover-placeholder"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
              <div v-else class="cover-placeholder cover-img">
                <el-icon><Picture /></el-icon>
              </div>
            </div>

            <!-- Info -->
            <div class="info-section">
              <h1 class="novel-title">{{ novel.title }}</h1>

              <!-- Badges row -->
              <div class="badges-row">
                <el-tag type="primary" effect="plain">{{ novel.category }}</el-tag>
                <el-tag :type="novelStatusType(novel.status)" effect="plain">
                  {{ novelStatusLabel(novel.status) }}
                </el-tag>
              </div>

              <!-- Author & dates -->
              <div class="meta-row">
                <span class="meta-item">
                  <el-icon><User /></el-icon>
                  <router-link v-if="author" :to="`/user/${author.id}`" class="author-link">
                    {{ author.username }}
                  </router-link>
                  <span v-else>加载中…</span>
                </span>
                <span class="meta-item">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDateOnly(novel.createdAt) }}
                </span>
                <span v-if="novel.updatedAt" class="meta-item">
                  <el-icon><RefreshRight /></el-icon>
                  更新于 {{ formatRelativeDate(novel.updatedAt) }}
                </span>
              </div>

              <!-- Stats -->
              <div class="stats-row">
                <span class="stat-item">
                  <el-icon><Document /></el-icon>
                  {{ formatCount(novel.wordCount) }} 字
                </span>
                <span class="stat-item">
                  <el-icon><List /></el-icon>
                  {{ novel.chapterCount ?? 0 }} 章
                </span>
                <span class="stat-item">
                  <el-icon><View /></el-icon>
                  {{ formatCount(novel.viewCount) }} 浏览
                </span>
                <span class="stat-item">
                  <el-icon><Star /></el-icon>
                  {{ formatCount(novel.collectCount) }} 收藏
                </span>
              </div>

              <!-- Description -->
              <p class="novel-desc">{{ novel.description || '暂无简介' }}</p>

              <!-- Actions -->
              <div class="actions-row">
                <!-- Start reading (first chapter) -->
                <el-button
                  v-if="chapters.length > 0"
                  type="primary"
                  :icon="Reading"
                  @click="readFirstChapter"
                >
                  开始阅读
                </el-button>

                <!-- Collect toggle -->
                <el-button
                  v-if="userStore.isLoggedIn"
                  :type="collected ? 'warning' : 'default'"
                  :icon="collected ? StarFilled : Star"
                  :loading="collectLoading"
                  round
                  @click="handleCollect"
                >
                  {{ collected ? '已收藏' : '加入收藏' }}
                </el-button>

                <!-- Add chapter (owner) -->
                <el-button
                  v-if="canEdit"
                  :icon="EditPen"
                  @click="router.push({ name: 'novel-create', query: { editId: novel.id } })"
                >
                  编辑小说
                </el-button>

                <!-- Delete (owner / admin) -->
                <el-popconfirm
                  v-if="canDelete"
                  title="确定要删除这部小说吗？所有章节也将一并删除。"
                  confirm-button-text="删除"
                  cancel-button-text="取消"
                  @confirm="handleDelete"
                >
                  <template #reference>
                    <el-button type="danger" :icon="Delete" :loading="deleteLoading" round>
                      删除
                    </el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </div>
        </el-card>

        <!-- Chapter list card -->
        <el-card v-if="novel" shadow="never" class="chapters-card">
          <template #header>
            <div class="chapters-header">
              <span class="chapters-title">目录</span>
              <span class="chapters-count">共 {{ chapters.length }} 章</span>
            </div>
          </template>

          <el-skeleton v-if="chaptersLoading" :rows="5" animated />
          <template v-else>
            <el-empty v-if="chapters.length === 0" description="暂无章节，敬请期待！" />
            <div
              v-for="chapter in chapters"
              :key="chapter.id"
              class="chapter-item"
              @click="router.push(`/novel/${novel.id}/chapter/${chapter.id}`)"
            >
              <span class="chapter-number">第{{ chapter.chapterNumber }}章</span>
              <span class="chapter-title">{{ chapter.title }}</span>
              <span class="chapter-meta">
                <span v-if="chapter.wordCount" class="chapter-words">
                  {{ formatCount(chapter.wordCount) }}字
                </span>
                <span class="chapter-time">{{ formatRelativeDate(chapter.createdAt) }}</span>
              </span>
            </div>
          </template>
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Picture, User, Calendar, RefreshRight, Document, List, View, Star,
  StarFilled, Reading, EditPen, Delete
} from '@element-plus/icons-vue'
import * as novelApi from '@/api/novel'
import * as userApi from '@/api/user'
import type { Novel, NovelChapter, UserInfo } from '@/api/types'
import { formatCount, formatDateOnly, formatRelativeDate, novelStatusLabel, novelStatusType } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import AppSidebar from '@/components/layout/AppSidebar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ── State ─────────────────────────────────────────────────────────────────────

const novel = ref<Novel | null>(null)
const novelLoading = ref(false)
const author = ref<UserInfo | null>(null)

const chapters = ref<NovelChapter[]>([])
const chaptersLoading = ref(false)

const collected = ref(false)
const collectLoading = ref(false)
const deleteLoading = ref(false)

const canEdit = computed(
  () =>
    userStore.isLoggedIn &&
    novel.value != null &&
    (userStore.userInfo?.id === novel.value.userId || userStore.isAdmin)
)
const canDelete = computed(() => canEdit.value)

// ── Data loading ──────────────────────────────────────────────────────────────

async function loadNovel() {
  novelLoading.value = true
  try {
    novel.value = await novelApi.getNovel(route.params.id as string)
    const [authorData, collectData] = await Promise.allSettled([
      userApi.getUser(novel.value.userId),
      userStore.isLoggedIn
        ? novelApi.getCollectStatus(novel.value.id)
        : Promise.resolve({ collected: false })
    ])
    if (authorData.status === 'fulfilled') author.value = authorData.value
    if (collectData.status === 'fulfilled') collected.value = collectData.value.collected
  } catch {
    novel.value = null
  } finally {
    novelLoading.value = false
  }
}

async function loadChapters() {
  if (!novel.value) return
  chaptersLoading.value = true
  try {
    chapters.value = await novelApi.listChapters(novel.value.id)
  } catch {
    // handled globally
  } finally {
    chaptersLoading.value = false
  }
}

onMounted(async () => {
  await loadNovel()
  await loadChapters()
})

// ── Actions ───────────────────────────────────────────────────────────────────

async function handleCollect() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!novel.value) return
  collectLoading.value = true
  try {
    const result = await novelApi.toggleCollect(novel.value.id)
    collected.value = result.collected
    novel.value.collectCount = (novel.value.collectCount ?? 0) + (result.collected ? 1 : -1)
    ElMessage.success(result.collected ? '已加入收藏' : '已取消收藏')
  } finally {
    collectLoading.value = false
  }
}

async function handleDelete() {
  if (!novel.value) return
  deleteLoading.value = true
  try {
    await novelApi.deleteNovel(novel.value.id)
    ElMessage.success('小说已删除')
    router.push('/novel')
  } finally {
    deleteLoading.value = false
  }
}

function readFirstChapter() {
  if (!novel.value || chapters.value.length === 0) return
  const first = chapters.value[0]
  router.push(`/novel/${novel.value.id}/chapter/${first.id}`)
}
</script>

<style scoped>
.novel-detail-page {
  padding: 0;
}

.breadcrumb {
  margin-bottom: 16px;
}

/* Header card */
.header-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.novel-header {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* Cover */
.cover-section {
  flex-shrink: 0;
}

.cover-img {
  width: 140px;
  height: 187px;
  border-radius: 6px;
  overflow: hidden;
  display: block;
}

.cover-placeholder {
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 40px;
}

/* Info */
.info-section {
  flex: 1;
  min-width: 0;
}

.novel-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.4;
  margin-bottom: 10px;
}

.badges-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.author-link {
  color: #606266;
  text-decoration: none;
  font-weight: 500;
}

.author-link:hover {
  color: #409eff;
}

.stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.novel-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  margin-bottom: 16px;
  white-space: pre-wrap;
  word-break: break-word;
}

.actions-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

/* Chapters card */
.chapters-card {
  border-radius: 8px;
}

.chapters-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.chapters-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chapters-count {
  font-size: 13px;
  color: #909399;
}

/* Chapter items */
.chapter-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 0;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.1s;
  font-size: 14px;
}

.chapter-item:last-child {
  border-bottom: none;
}

.chapter-item:hover {
  color: #409eff;
}

.chapter-number {
  flex-shrink: 0;
  font-size: 12px;
  color: #c0c4cc;
  min-width: 52px;
}

.chapter-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
}

.chapter-item:hover .chapter-title {
  color: #409eff;
}

.chapter-meta {
  flex-shrink: 0;
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #c0c4cc;
}

/* Mobile: hide cover on very small screens */
@media (max-width: 480px) {
  .novel-header {
    flex-direction: column;
  }

  .cover-img {
    width: 100%;
    height: 240px;
  }
}
</style>
