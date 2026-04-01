<template>
  <div class="forum-page">
    <el-page-header @back="router.push('/')" :title="forum?.name || '版块详情'">
      <template #extra>
        <el-button
          v-if="userStore.isLoggedIn"
          type="primary"
          @click="router.push('/post/create')"
        >
          发布帖子
        </el-button>
      </template>
    </el-page-header>

    <el-divider />

    <el-skeleton :loading="loading" animated :rows="6">
      <template #default>
        <el-table :data="posts" style="width: 100%">
          <el-table-column prop="title" label="标题">
            <template #default="{ row }">
              <el-link type="primary" @click="router.push(`/post/${row.id}`)">
                <el-tag v-if="row.isTop" size="small" type="danger" style="margin-right: 4px">
                  置顶
                </el-tag>
                <el-tag v-if="row.isFeatured" size="small" type="warning" style="margin-right: 4px">
                  精华
                </el-tag>
                {{ row.title }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column prop="viewCount" label="浏览" width="80" />
          <el-table-column prop="commentCount" label="回复" width="80" />
          <el-table-column prop="createdAt" label="发布时间" width="180" />
        </el-table>

        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          style="margin-top: 16px; justify-content: flex-end"
          @current-change="loadPosts"
        />
      </template>
    </el-skeleton>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getForumById } from '@/api/forum'
import { getPostList } from '@/api/post'
import type { Forum, Post } from '@/types/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const forum = ref<Forum>()
const posts = ref<Post[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)

const forumId = Number(route.params.id)

onMounted(async () => {
  loading.value = true
  try {
    const [forumRes, postRes] = await Promise.all([
      getForumById(forumId),
      getPostList({ forumId, current: 1, size: pageSize.value })
    ])
    forum.value = forumRes.data.data
    posts.value = postRes.data.data.records
    total.value = Number(postRes.data.data.total)
  } finally {
    loading.value = false
  }
})

async function loadPosts(page: number) {
  const { data } = await getPostList({ forumId, current: page, size: pageSize.value })
  posts.value = data.data.records
  total.value = Number(data.data.total)
}
</script>

<style scoped>
.forum-page {
  max-width: 960px;
  margin: 24px auto;
  padding: 0 16px;
}
</style>
