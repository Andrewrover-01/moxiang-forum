<template>
  <div class="post-page">
    <el-page-header @back="router.go(-1)" :title="post?.title || '帖子详情'" />
    <el-divider />
    <el-skeleton :loading="loading" animated :rows="8">
      <template #default>
        <el-card v-if="post">
          <div class="post-meta">
            <span>发布时间：{{ post.createdAt }}</span>
            <span style="margin-left: 16px">浏览：{{ post.viewCount }}</span>
            <span style="margin-left: 16px">点赞：{{ post.likeCount }}</span>
          </div>
          <el-divider />
          <div class="post-content">{{ post.content }}</div>
          <div v-if="userStore.isLoggedIn" style="margin-top: 16px">
            <el-button type="primary" @click="handleLike">点赞</el-button>
          </div>
        </el-card>
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getPostById, likePost } from '@/api/post'
import type { Post } from '@/types/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const post = ref<Post>()
const loading = ref(false)
const postId = Number(route.params.id)

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await getPostById(postId)
    post.value = data.data
  } finally {
    loading.value = false
  }
})

async function handleLike() {
  await likePost(postId)
  ElMessage.success('点赞成功')
  if (post.value) post.value.likeCount++
}
</script>

<style scoped>
.post-page {
  max-width: 960px;
  margin: 24px auto;
  padding: 0 16px;
}
.post-meta {
  color: #909399;
  font-size: 14px;
}
.post-content {
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>
