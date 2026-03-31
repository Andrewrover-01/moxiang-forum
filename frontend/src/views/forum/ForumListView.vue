<template>
  <div class="forum-list-page">
    <el-card shadow="never" class="page-card">
      <template #header>
        <div class="page-header">
          <span class="page-title">全部板块</span>
          <span class="page-sub">共 {{ forumStore.forums.length }} 个板块</span>
        </div>
      </template>

      <!-- Loading skeleton -->
      <el-row v-if="forumStore.loading" :gutter="16">
        <el-col v-for="n in 8" :key="n" :xs="24" :sm="12" :md="8" :lg="6">
          <el-skeleton :rows="3" animated class="skeleton-card" />
        </el-col>
      </el-row>

      <!-- Forum grid -->
      <template v-else>
        <el-empty v-if="forumStore.forums.length === 0" description="暂无板块" />
        <el-row v-else :gutter="16">
          <el-col
            v-for="forum in forumStore.forums"
            :key="forum.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
            class="forum-col"
          >
            <router-link :to="`/forum/${forum.id}`" class="forum-card-link">
              <el-card shadow="hover" class="forum-card">
                <!-- Icon -->
                <div class="forum-icon-wrap">
                  <el-icon v-if="forum.icon" class="forum-icon">
                    <component :is="forum.icon" />
                  </el-icon>
                  <el-icon v-else class="forum-icon"><ChatDotRound /></el-icon>
                </div>

                <!-- Name & description -->
                <div class="forum-name">{{ forum.name }}</div>
                <p class="forum-desc">{{ truncateText(forum.description, 40) || '暂无简介' }}</p>

                <!-- Footer: post count -->
                <div class="forum-footer">
                  <span class="forum-stat">
                    <el-icon><Document /></el-icon>
                    {{ formatCount(forum.postCount) }} 帖子
                  </span>
                </div>
              </el-card>
            </router-link>
          </el-col>
        </el-row>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { ChatDotRound, Document } from '@element-plus/icons-vue'
import { useForumStore } from '@/stores/forum'
import { formatCount, truncateText } from '@/utils/format'

const forumStore = useForumStore()

onMounted(() => {
  forumStore.fetchForums(true)
})
</script>

<style scoped>
.forum-list-page {
  padding: 0;
}

.page-card {
  border-radius: 8px;
}

.page-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.page-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.page-sub {
  font-size: 13px;
  color: #909399;
}

/* Forum grid */
.forum-col {
  margin-bottom: 16px;
}

.forum-card-link {
  display: block;
  text-decoration: none;
}

.forum-card {
  border-radius: 8px;
  transition: transform 0.15s, box-shadow 0.15s;
  cursor: pointer;
  height: 100%;
}

.forum-card:hover {
  transform: translateY(-2px);
}

.forum-icon-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.forum-icon {
  font-size: 36px;
  color: #409eff;
}

.forum-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  text-align: center;
  margin-bottom: 6px;
}

.forum-desc {
  font-size: 12px;
  color: #909399;
  text-align: center;
  line-height: 1.5;
  min-height: 36px;
  margin-bottom: 12px;
}

.forum-footer {
  display: flex;
  justify-content: center;
  font-size: 12px;
  color: #c0c4cc;
  border-top: 1px solid #f0f0f0;
  padding-top: 10px;
}

.forum-stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Skeleton */
.skeleton-card {
  padding: 16px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 16px;
}
</style>
