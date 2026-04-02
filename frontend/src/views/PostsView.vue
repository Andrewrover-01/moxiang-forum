<template>
  <div class="posts-page">
    <div class="posts-inner">
      <h2 class="page-title">全部帖子</h2>
      <el-skeleton :loading="loading" animated :rows="6">
        <template #default>
          <div v-if="posts.length === 0" class="empty-tip">暂无帖子</div>
          <ul v-else class="post-list">
            <li v-for="post in posts" :key="post.id" class="post-item" @click="router.push(`/post/${post.id}`)">
              <div class="post-title">{{ post.title }}</div>
              <div class="post-meta">
                <span>{{ post.createdAt }}</span>
                <span>浏览 {{ post.viewCount }}</span>
                <span>评论 {{ post.commentCount }}</span>
              </div>
            </li>
          </ul>

          <div class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="prev, pager, next"
              background
              @current-change="fetchPosts"
            />
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPostList } from '@/api/post'
import type { Post } from '@/types/api'

const router = useRouter()
const posts = ref<Post[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

async function fetchPosts(page = 1) {
  loading.value = true
  try {
    const { data } = await getPostList({ current: page, size: pageSize.value })
    posts.value = data.data.records
    total.value = data.data.total
    currentPage.value = page
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchPosts())
</script>

<style scoped>
.posts-page {
  flex: 1;
  padding: 32px 24px;
}

.posts-inner {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 1.5rem;
  color: #8B0000;
  letter-spacing: 2px;
  margin: 0 0 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid #8B0000;
}

.empty-tip {
  text-align: center;
  color: #999;
  padding: 48px 0;
}

.post-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-item {
  background: #ffffff;
  border-radius: 6px;
  padding: 16px 20px;
  cursor: pointer;
  border: 1px solid #e8e0d5;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.post-item:hover {
  border-color: #8B0000;
  box-shadow: 0 2px 8px rgba(139, 0, 0, 0.1);
}

.post-title {
  font-size: 16px;
  color: #2c1a0e;
  margin-bottom: 8px;
}

.post-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #7a6a5a;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: center;
}
</style>
