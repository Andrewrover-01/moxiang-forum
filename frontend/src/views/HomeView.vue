<template>
  <div class="home-page">
    <el-row :gutter="20">
      <!-- 主内容区：最新帖子 -->
      <el-col :span="17">
        <el-card shadow="never">
          <template #header>
            <span>最新帖子</span>
          </template>
          <el-empty v-if="posts.length === 0" description="暂无帖子" />
          <div v-for="post in posts" :key="post.id" class="post-item">
            <router-link :to="`/post/${post.id}`" class="post-title">{{ post.title }}</router-link>
            <div class="post-meta">
              <span>{{ formatRelativeDate(post.createdAt) }}</span>
              <span>{{ formatCount(post.commentCount) }} 评论</span>
              <span>{{ formatCount(post.viewCount) }} 浏览</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <!-- 侧边栏：热门小说 -->
      <el-col :span="7">
        <el-card shadow="never">
          <template #header><span>热门小说</span></template>
          <el-empty v-if="novels.length === 0" description="暂无小说" />
          <div v-for="novel in novels" :key="novel.id" class="novel-item">
            <router-link :to="`/novel/${novel.id}`">{{ novel.title }}</router-link>
            <el-tag :type="novelStatusType(novel.status)" size="small" class="novel-status">
              {{ novelStatusLabel(novel.status) }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as postApi from '@/api/post'
import * as novelApi from '@/api/novel'
import type { Post, Novel } from '@/api/types'
import { formatRelativeDate, formatCount, novelStatusLabel, novelStatusType } from '@/utils/format'

const posts = ref<Post[]>([])
const novels = ref<Novel[]>([])

onMounted(async () => {
  try {
    const [postPage, novelPage] = await Promise.all([
      postApi.listPosts(undefined, 1, 20),
      novelApi.listNovels(undefined, 1, 10)
    ])
    posts.value = postPage.records
    novels.value = novelPage.records
  } catch {
    // Errors already handled by the http interceptor (ElMessage)
  }
})
</script>

<style scoped>
.home-page { padding: 0; }
.post-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.post-item:last-child { border-bottom: none; }
.post-title { font-size: 15px; font-weight: 500; color: #303133; }
.post-title:hover { color: #409eff; }
.post-meta { margin-top: 4px; font-size: 12px; color: #909399; display: flex; gap: 12px; }
.novel-item { padding: 8px 0; border-bottom: 1px solid #f0f0f0; font-size: 14px; display: flex; align-items: center; justify-content: space-between; }
.novel-item:last-child { border-bottom: none; }
.novel-status { margin-left: 8px; flex-shrink: 0; }
</style>
