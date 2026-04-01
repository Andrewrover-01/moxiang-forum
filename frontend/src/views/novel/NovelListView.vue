<template>
  <div class="novel-list-page">
    <el-row :gutter="20">
      <!-- ── Main content ───────────────────────────────────────────────── -->
      <el-col :xs="24" :sm="24" :md="17">
        <el-card shadow="never" class="list-card">
          <template #header>
            <div class="page-header">
              <span class="page-title">小说书架</span>
              <span class="page-sub">共 {{ total }} 部</span>
              <div class="header-actions">
                <router-link v-if="userStore.isLoggedIn" to="/novel/create">
                  <el-button type="primary" size="small" :icon="EditPen">创作小说</el-button>
                </router-link>
              </div>
            </div>
          </template>

          <!-- Filter bar -->
          <div class="filter-bar">
            <!-- Search -->
            <el-input
              v-model="keyword"
              placeholder="搜索小说标题…"
              :prefix-icon="Search"
              clearable
              class="search-input"
              @keyup.enter="onSearch"
              @clear="onSearch"
            />

            <!-- Category filter -->
            <el-select
              v-model="selectedCategory"
              placeholder="全部分类"
              clearable
              class="category-select"
              @change="onCategoryChange"
            >
              <el-option
                v-for="c in CATEGORIES"
                :key="c"
                :label="c"
                :value="c"
              />
            </el-select>

            <!-- Status filter -->
            <el-select
              v-model="selectedStatus"
              placeholder="全部状态"
              clearable
              class="status-select"
              @change="onStatusChange"
            >
              <el-option label="连载中" :value="0" />
              <el-option label="已完结" :value="1" />
              <el-option label="已暂停" :value="2" />
            </el-select>
          </div>

          <!-- Loading skeleton -->
          <el-row v-if="loading" :gutter="16">
            <el-col v-for="n in 8" :key="n" :xs="12" :sm="8" :md="6" class="novel-col">
              <el-skeleton animated class="novel-skeleton" />
            </el-col>
          </el-row>

          <!-- Novel grid -->
          <template v-else>
            <el-empty v-if="novels.length === 0" description="暂无小说" />
            <el-row v-else :gutter="16">
              <el-col
                v-for="novel in novels"
                :key="novel.id"
                :xs="12"
                :sm="8"
                :md="6"
                class="novel-col"
              >
                <router-link :to="`/novel/${novel.id}`" class="novel-card-link">
                  <el-card shadow="hover" class="novel-card">
                    <!-- Cover -->
                    <div class="cover-wrap">
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
                      <div v-else class="cover-placeholder">
                        <el-icon><Picture /></el-icon>
                      </div>
                      <!-- Status overlay -->
                      <el-tag
                        :type="novelStatusType(novel.status)"
                        size="small"
                        effect="dark"
                        class="status-overlay"
                      >
                        {{ novelStatusLabel(novel.status) }}
                      </el-tag>
                    </div>

                    <!-- Info -->
                    <div class="novel-info">
                      <div class="novel-title" :title="novel.title">{{ novel.title }}</div>
                      <div class="novel-meta">
                        <el-tag type="primary" size="small" effect="plain">{{ novel.category }}</el-tag>
                      </div>
                      <div class="novel-stats">
                        <span>{{ formatCount(novel.wordCount) }}字</span>
                        <span>{{ novel.chapterCount ?? 0 }}章</span>
                        <span>
                          <el-icon><View /></el-icon>
                          {{ formatCount(novel.viewCount) }}
                        </span>
                      </div>
                    </div>
                  </el-card>
                </router-link>
              </el-col>
            </el-row>
          </template>

          <!-- Pagination -->
          <div v-if="total > pageSize" class="pagination-wrap">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="loadNovels"
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
import { EditPen, Search, Picture, View } from '@element-plus/icons-vue'
import * as novelApi from '@/api/novel'
import type { Novel } from '@/api/types'
import { formatCount, novelStatusLabel, novelStatusType } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import AppSidebar from '@/components/layout/AppSidebar.vue'

const userStore = useUserStore()

// ── Constants ─────────────────────────────────────────────────────────────────

const CATEGORIES = ['玄幻', '仙侠', '武侠', '都市', '现实', '军事', '历史', '游戏', '体育', '科幻', '悬疑', '轻小说']

// ── State ─────────────────────────────────────────────────────────────────────

const novels = ref<Novel[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = 20
const total = ref(0)

const keyword = ref('')
const selectedCategory = ref<string | undefined>(undefined)
const selectedStatus = ref<number | undefined>(undefined)

// ── Data loading ──────────────────────────────────────────────────────────────

async function loadNovels() {
  loading.value = true
  try {
    let page
    if (keyword.value.trim()) {
      page = await novelApi.searchNovels(keyword.value.trim(), currentPage.value, pageSize)
    } else {
      page = await novelApi.listNovels(selectedCategory.value, currentPage.value, pageSize)
    }
    // Client-side status filter (API may not support it directly)
    if (selectedStatus.value != null) {
      const filtered = page.records.filter((n) => n.status === selectedStatus.value)
      novels.value = filtered
      // Reflect the filtered count so pagination is not misleading
      total.value = filtered.length
    } else {
      novels.value = page.records
      total.value = page.total
    }
  } catch {
    // handled globally
  } finally {
    loading.value = false
  }
}

function onSearch() {
  currentPage.value = 1
  loadNovels()
}

function onCategoryChange() {
  currentPage.value = 1
  loadNovels()
}

function onStatusChange() {
  currentPage.value = 1
  loadNovels()
}

onMounted(() => {
  loadNovels()
})
</script>

<style scoped>
.novel-list-page {
  padding: 0;
}

.list-card {
  border-radius: 8px;
}

/* Header */
.page-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.page-sub {
  font-size: 13px;
  color: #909399;
  flex: 1;
}

.header-actions {
  margin-left: auto;
}

/* Filter bar */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
  min-width: 160px;
}

.category-select,
.status-select {
  width: 130px;
}

/* Novel grid */
.novel-col {
  margin-bottom: 16px;
}

.novel-card-link {
  display: block;
  text-decoration: none;
}

.novel-card {
  border-radius: 8px;
  transition: transform 0.15s;
  cursor: pointer;
  padding: 0;
}

.novel-card:hover {
  transform: translateY(-2px);
}

/* Override el-card padding */
.novel-card :deep(.el-card__body) {
  padding: 0;
}

/* Cover */
.cover-wrap {
  position: relative;
  width: 100%;
  padding-top: 133%; /* 3:4 aspect ratio */
  overflow: hidden;
  border-radius: 8px 8px 0 0;
}

.cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.cover-placeholder {
  position: absolute;
  inset: 0;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
  font-size: 32px;
}

.status-overlay {
  position: absolute;
  bottom: 6px;
  left: 6px;
}

/* Info */
.novel-info {
  padding: 8px 10px 10px;
}

.novel-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 5px;
}

.novel-meta {
  margin-bottom: 5px;
}

.novel-stats {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: #c0c4cc;
  align-items: center;
}

.novel-stats .el-icon {
  font-size: 11px;
}

/* Skeleton */
.novel-skeleton {
  height: 260px;
  border-radius: 8px;
  background: #f5f7fa;
}

/* Pagination */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 20px 0 4px;
}
</style>
