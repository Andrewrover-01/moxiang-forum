<template>
  <div class="app-sidebar">
    <!-- Forum list -->
    <el-card shadow="never" class="sidebar-card">
      <template #header>
        <div class="card-header">
          <el-icon><Grid /></el-icon>
          <span>热门板块</span>
        </div>
      </template>

      <el-skeleton v-if="forumStore.loading" :rows="5" animated />
      <template v-else>
        <el-empty v-if="forumStore.forums.length === 0" description="暂无板块" :image-size="60" />
        <ul v-else class="forum-list">
          <li
            v-for="forum in forumStore.forums.slice(0, 8)"
            :key="forum.id"
            class="forum-item"
          >
            <router-link :to="`/forum/${forum.id}`" class="forum-link">
              <el-icon v-if="forum.icon" class="forum-icon">
                <component :is="forum.icon" />
              </el-icon>
              <el-icon v-else class="forum-icon"><ChatDotRound /></el-icon>
              <span class="forum-name">{{ forum.name }}</span>
              <el-badge
                :value="formatCount(forum.postCount)"
                class="forum-count"
                type="info"
              />
            </router-link>
          </li>
        </ul>
        <router-link v-if="forumStore.forums.length > 8" to="/forum" class="view-all">
          查看全部板块 →
        </router-link>
      </template>
    </el-card>

    <!-- Quick links -->
    <el-card shadow="never" class="sidebar-card">
      <template #header>
        <div class="card-header">
          <el-icon><Link /></el-icon>
          <span>快速入口</span>
        </div>
      </template>
      <ul class="quick-links">
        <li>
          <router-link to="/post/create">
            <el-icon><EditPen /></el-icon>发布帖子
          </router-link>
        </li>
        <li>
          <router-link to="/novel/create">
            <el-icon><Reading /></el-icon>创作小说
          </router-link>
        </li>
        <li>
          <router-link to="/novel">
            <el-icon><Collection /></el-icon>小说书架
          </router-link>
        </li>
      </ul>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { Grid, ChatDotRound, Link, EditPen, Reading, Collection } from '@element-plus/icons-vue'
import { useForumStore } from '@/stores/forum'
import { formatCount } from '@/utils/format'

const forumStore = useForumStore()

onMounted(() => {
  forumStore.fetchForums()
})
</script>

<style scoped>
.app-sidebar {
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

/* Forum list */
.forum-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.forum-item {
  border-bottom: 1px solid #f5f5f5;
}

.forum-item:last-child {
  border-bottom: none;
}

.forum-link {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 4px;
  text-decoration: none;
  color: #606266;
  font-size: 13px;
  transition: color 0.15s;
}

.forum-link:hover {
  color: #409eff;
  text-decoration: none;
}

.forum-icon {
  color: #409eff;
  flex-shrink: 0;
}

.forum-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.forum-count {
  flex-shrink: 0;
}

.view-all {
  display: block;
  margin-top: 10px;
  font-size: 12px;
  color: #409eff;
  text-align: center;
  text-decoration: none;
}

.view-all:hover {
  text-decoration: underline;
}

/* Quick links */
.quick-links {
  list-style: none;
  margin: 0;
  padding: 0;
}

.quick-links li {
  border-bottom: 1px solid #f5f5f5;
}

.quick-links li:last-child {
  border-bottom: none;
}

.quick-links a {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 4px;
  color: #606266;
  font-size: 13px;
  text-decoration: none;
  transition: color 0.15s;
}

.quick-links a:hover {
  color: #409eff;
  text-decoration: none;
}
</style>
