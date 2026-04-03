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

        <!-- Comment section -->
        <div class="comment-section">
          <h3 class="comment-title">评论</h3>

          <!-- Comment form (only for logged-in users) -->
          <el-card v-if="userStore.isLoggedIn" class="comment-form-card">
            <el-input
              v-model="commentContent"
              type="textarea"
              :rows="3"
              placeholder="写下你的评论..."
              :maxlength="500"
              show-word-limit
            />
            <div class="comment-form-footer">
              <CaptchaWidget
                scene="COMMENT"
                @verified="onCaptchaVerified"
                ref="captchaRef"
                class="comment-captcha"
              />
              <el-button
                type="primary"
                size="small"
                :loading="submittingComment"
                @click="handleSubmitComment"
              >
                发表评论
              </el-button>
            </div>
          </el-card>
          <el-alert
            v-else
            type="info"
            title="登录后才能发表评论"
            :closable="false"
            style="margin-bottom: 12px"
          />

          <!-- Comment list -->
          <el-skeleton :loading="commentsLoading" animated :rows="4">
            <template #default>
              <div v-if="comments.length === 0" class="no-comments">
                暂无评论，快来抢沙发吧～
              </div>
              <div v-for="c in comments" :key="c.id" class="comment-item">
                <div class="comment-meta">
                  <span class="comment-user">用户 {{ c.userId }}</span>
                  <span class="comment-time">{{ c.createdAt }}</span>
                </div>
                <div class="comment-body">{{ c.content }}</div>
              </div>
            </template>
          </el-skeleton>
        </div>
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
import { getCommentsByPost, createComment, type Comment } from '@/api/comment'
import CaptchaWidget from '@/components/CaptchaWidget.vue'
import type { Post } from '@/types/api'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const post = ref<Post>()
const loading = ref(false)
const postId = Number(route.params.id)

// Comments
const comments = ref<Comment[]>([])
const commentsLoading = ref(false)
const commentContent = ref('')
const submittingComment = ref(false)
const captchaToken = ref('')
const captchaRef = ref<InstanceType<typeof CaptchaWidget>>()

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await getPostById(postId)
    post.value = data.data
  } finally {
    loading.value = false
  }
  loadComments()
})

async function loadComments() {
  commentsLoading.value = true
  try {
    const { data } = await getCommentsByPost(postId)
    comments.value = data.data.records
  } finally {
    commentsLoading.value = false
  }
}

async function handleLike() {
  await likePost(postId)
  ElMessage.success('点赞成功')
  if (post.value) post.value.likeCount++
}

function onCaptchaVerified(token: string) {
  captchaToken.value = token
}

async function handleSubmitComment() {
  if (!commentContent.value.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }
  if (!captchaToken.value) {
    ElMessage.warning('请先完成人机验证')
    return
  }
  submittingComment.value = true
  try {
    await createComment({ postId, content: commentContent.value }, captchaToken.value)
    ElMessage.success('评论成功')
    commentContent.value = ''
    captchaToken.value = ''
    captchaRef.value?.reload()
    await loadComments()
  } catch {
    captchaToken.value = ''
    captchaRef.value?.reload()
  } finally {
    submittingComment.value = false
  }
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

/* ---- Comments ---- */
.comment-section {
  margin-top: 24px;
}
.comment-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
  color: #303133;
}
.comment-form-card {
  margin-bottom: 16px;
}
.comment-form-footer {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-top: 10px;
  flex-wrap: wrap;
}
.comment-captcha {
  flex: 1;
  min-width: 240px;
}
.no-comments {
  text-align: center;
  color: #909399;
  padding: 24px 0;
}
.comment-item {
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}
.comment-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 4px;
}
.comment-user {
  font-weight: 600;
  color: #303133;
  font-size: 13px;
}
.comment-time {
  color: #909399;
  font-size: 12px;
}
.comment-body {
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
