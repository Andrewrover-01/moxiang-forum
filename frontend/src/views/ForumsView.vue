<template>
  <div class="forums-page">
    <div class="forums-inner">
      <h2 class="page-title">版块列表</h2>
      <el-skeleton :loading="loading" animated :rows="6">
        <template #default>
          <div v-if="forums.length === 0" class="empty-tip">暂无版块</div>
          <ul v-else class="forum-list">
            <li
              v-for="forum in forums"
              :key="forum.id"
              class="forum-item"
              @click="router.push(`/forum/${forum.id}`)"
            >
              <div class="forum-icon">
                <img
                  v-if="forum.icon && (forum.icon.startsWith('/') || forum.icon.startsWith('http')) && !iconErrors[forum.id]"
                  :src="forum.icon"
                  :alt="forum.name"
                  class="forum-icon-img"
                  @error="iconErrors[forum.id] = true"
                />
                <span v-else>{{ forum.name.charAt(0) }}</span>
              </div>
              <div class="forum-info">
                <div class="forum-name">{{ forum.name }}</div>
                <div class="forum-desc">{{ forum.description }}</div>
              </div>
              <div class="forum-count">
                <span class="count-value">{{ forum.postCount }}</span>
                <span class="count-label">帖子</span>
              </div>
            </li>
          </ul>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getForumList } from '@/api/forum'
import type { Forum } from '@/types/api'

const router = useRouter()
const forums = ref<Forum[]>([])
const loading = ref(false)
const iconErrors = ref<Record<number, boolean>>({})

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await getForumList()
    forums.value = data.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.forums-page {
  flex: 1;
  padding: 32px 24px;
}

.forums-inner {
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

.forum-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.forum-item {
  background: #ffffff;
  border-radius: 6px;
  padding: 16px 20px;
  cursor: pointer;
  border: 1px solid #e8e0d5;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.forum-item:hover {
  border-color: #8B0000;
  box-shadow: 0 2px 8px rgba(139, 0, 0, 0.1);
}

.forum-icon {
  width: 48px;
  height: 48px;
  background: #FAF7F0;
  border: 1px solid #e8e0d5;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #8B0000;
  flex-shrink: 0;
  overflow: hidden;
}

.forum-icon-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.forum-info {
  flex: 1;
  min-width: 0;
}

.forum-name {
  font-size: 16px;
  font-weight: bold;
  color: #2c1a0e;
  margin-bottom: 4px;
}

.forum-desc {
  font-size: 13px;
  color: #7a6a5a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.forum-count {
  text-align: center;
  flex-shrink: 0;
}

.count-value {
  display: block;
  font-size: 1.25rem;
  font-weight: bold;
  color: #8B0000;
}

.count-label {
  font-size: 12px;
  color: #7a6a5a;
}
</style>
