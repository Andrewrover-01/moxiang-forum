<template>
  <div class="home-page">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-inner">
        <h1 class="hero-title">墨香论坛</h1>
        <p class="hero-subtitle">以文会友，以墨传情——汇聚四海文人，共话诗书雅趣</p>
        <button class="btn-enter" @click="router.push('/posts')">进入论坛</button>
      </div>
    </section>

    <!-- Stats -->
    <section class="stats-section">
      <div class="stats-inner">
        <div class="stat-card">
          <div class="stat-value">{{ totalPosts !== null ? totalPosts.toLocaleString() : '—' }}</div>
          <div class="stat-label">总帖子数</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-card">
          <div class="stat-value">{{ forumCount !== null ? forumCount : '—' }}</div>
          <div class="stat-label">版块数</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-card">
          <div class="stat-value">{{ memberCount.toLocaleString() }}</div>
          <div class="stat-label">总会员数</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getForumList } from '@/api/forum'
import { getPostList } from '@/api/post'

const router = useRouter()

const totalPosts = ref<number | null>(null)
const forumCount = ref<number | null>(null)
const memberCount = ref<number>(1024)

onMounted(async () => {
  try {
    const [postsRes, forumsRes] = await Promise.allSettled([
      getPostList({ current: 1, size: 1 }),
      getForumList()
    ])

    if (postsRes.status === 'fulfilled') {
      totalPosts.value = postsRes.value.data.data.total
    }
    if (forumsRes.status === 'fulfilled') {
      forumCount.value = forumsRes.value.data.data.length
    }
  } catch {
    // 静默处理，数据显示 —
  }
})
</script>

<style scoped>
.home-page {
  flex: 1;
}

/* Hero */
.hero {
  background: linear-gradient(135deg, #8B0000 0%, #5c0000 100%);
  color: #FAF7F0;
  padding: 80px 24px;
  text-align: center;
}

.hero-inner {
  max-width: 700px;
  margin: 0 auto;
}

.hero-title {
  font-size: clamp(2.5rem, 6vw, 4rem);
  font-weight: bold;
  letter-spacing: 6px;
  margin: 0 0 16px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.hero-subtitle {
  font-size: clamp(1rem, 2.5vw, 1.25rem);
  letter-spacing: 2px;
  opacity: 0.9;
  margin: 0 0 40px;
  line-height: 1.8;
}

.btn-enter {
  display: inline-block;
  background: #FAF7F0;
  color: #8B0000;
  border: none;
  padding: 12px 40px;
  font-size: 16px;
  font-family: inherit;
  letter-spacing: 2px;
  border-radius: 4px;
  cursor: pointer;
  font-weight: bold;
  transition: background 0.2s, transform 0.1s;
}

.btn-enter:hover {
  background: #ffffff;
  transform: translateY(-1px);
}

.btn-enter:active {
  transform: translateY(0);
}

/* Stats */
.stats-section {
  background: #ffffff;
  border-bottom: 1px solid #e8e0d5;
}

.stats-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card {
  flex: 1;
  text-align: center;
  padding: 32px 24px;
}

.stat-value {
  font-size: 2rem;
  font-weight: bold;
  color: #8B0000;
  letter-spacing: 1px;
  line-height: 1.2;
}

.stat-label {
  margin-top: 8px;
  font-size: 14px;
  color: #7a6a5a;
  letter-spacing: 2px;
}

.stat-divider {
  width: 1px;
  height: 48px;
  background: #e8e0d5;
  flex-shrink: 0;
}
</style>
