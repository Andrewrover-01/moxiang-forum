<template>
  <div class="post-detail-page">
    <!-- Breadcrumb -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item v-if="forum" :to="{ path: `/forum/${forum.id}` }">
        {{ forum.name }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>{{ post?.title ?? '帖子详情' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-row :gutter="20">
      <!-- ── Main content ───────────────────────────────────────────────── -->
      <el-col :xs="24" :sm="24" :md="17">

        <!-- Post card -->
        <el-card shadow="never" class="post-card">
          <el-skeleton v-if="postLoading" :rows="6" animated />

          <el-result
            v-else-if="!post"
            icon="warning"
            title="帖子不存在"
            sub-title="该帖子可能已被删除或地址有误"
          >
            <template #extra>
              <el-button @click="router.back()">返回上一页</el-button>
            </template>
          </el-result>

          <template v-else>
            <!-- Post header -->
            <div class="post-header">
              <!-- Badges -->
              <div class="post-badges">
                <el-tag v-if="post.isTop === 1" type="danger" size="small" effect="plain">置顶</el-tag>
                <el-tag v-if="post.isFeatured === 1" type="warning" size="small" effect="plain">精华</el-tag>
                <el-tag v-if="post.status === 1" type="info" size="small" effect="plain">已锁定</el-tag>
                <el-tag
                  v-if="forum"
                  type="info"
                  size="small"
                  effect="plain"
                  class="forum-badge"
                  @click="router.push(`/forum/${forum.id}`)"
                >
                  {{ forum.name }}
                </el-tag>
              </div>

              <h1 class="post-title">{{ post.title }}</h1>

              <!-- Author + meta row -->
              <div class="post-meta">
                <span class="meta-author">
                  <el-avatar
                    :size="24"
                    :src="author?.avatar"
                    :alt="author?.username"
                    class="author-avatar"
                  >
                    {{ author?.username?.[0]?.toUpperCase() }}
                  </el-avatar>
                  <router-link
                    v-if="author"
                    :to="`/user/${author.id}`"
                    class="author-name"
                  >{{ author.username }}</router-link>
                  <span v-else class="author-name">加载中…</span>
                </span>
                <span class="meta-divider">·</span>
                <span class="meta-time">
                  <el-icon><Clock /></el-icon>
                  {{ formatDate(post.createdAt) }}
                </span>
                <span class="meta-divider">·</span>
                <span class="meta-stat">
                  <el-icon><View /></el-icon>
                  {{ formatCount(post.viewCount) }} 浏览
                </span>
                <span class="meta-stat">
                  <el-icon><ChatDotRound /></el-icon>
                  {{ formatCount(post.commentCount) }} 评论
                </span>
              </div>
            </div>

            <el-divider />

            <!-- Post content -->
            <div class="post-content">{{ post.content }}</div>

            <el-divider />

            <!-- Like / Favorite / Delete actions -->
            <div class="post-actions">
              <el-button
                :type="liked ? 'primary' : 'default'"
                :icon="liked ? StarFilled : Star"
                :loading="likeLoading"
                round
                @click="handleLike"
              >
                {{ liked ? '已点赞' : '点赞' }}
                <span v-if="post.likeCount > 0" class="like-count"> {{ formatCount(post.likeCount) }}</span>
              </el-button>

              <el-button
                v-if="userStore.isLoggedIn"
                :type="favorited ? 'warning' : 'default'"
                :icon="CollectionTag"
                :loading="favoriteLoading"
                round
                @click="handleFavorite"
              >
                {{ favorited ? '已收藏' : '收藏' }}
              </el-button>

              <el-button
                v-if="canEdit"
                :icon="EditPen"
                round
                @click="router.push({ name: 'post-create', query: { editId: post.id } })"
              >
                编辑
              </el-button>

              <el-popconfirm
                v-if="canDelete"
                title="确定要删除这篇帖子吗？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                @confirm="handleDelete"
              >
                <template #reference>
                  <el-button type="danger" :icon="Delete" round :loading="deleteLoading">
                    删除
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-card>

        <!-- ── Comments section ─────────────────────────────────────────── -->
        <el-card v-if="post" shadow="never" class="comments-card">
          <template #header>
            <div class="comments-header">
              <span class="comments-title">评论</span>
              <span class="comments-count">{{ commentTotal }} 条</span>
            </div>
          </template>

          <!-- Write comment box -->
          <div v-if="userStore.isLoggedIn && post.status !== 1" class="comment-box">
            <el-avatar :size="32" :src="userStore.userInfo?.avatar" class="comment-avatar">
              {{ userStore.userInfo?.username?.[0]?.toUpperCase() }}
            </el-avatar>
            <div class="comment-input-wrap">
              <el-input
                v-model="commentText"
                type="textarea"
                :rows="3"
                placeholder="写下你的评论…"
                :maxlength="500"
                show-word-limit
                resize="none"
              />
              <div class="comment-submit-row">
                <el-button
                  type="primary"
                  size="small"
                  :loading="submitCommentLoading"
                  :disabled="!commentText.trim()"
                  @click="submitComment()"
                >
                  发表评论
                </el-button>
              </div>
            </div>
          </div>

          <el-divider v-if="userStore.isLoggedIn && post.status !== 1" />

          <!-- Comment list -->
          <el-skeleton v-if="commentsLoading" :rows="4" animated />
          <template v-else>
            <el-empty v-if="comments.length === 0" description="暂无评论，快来说几句吧！" />

            <div
              v-for="comment in comments"
              :key="comment.id"
              class="comment-item"
            >
              <!-- Main comment -->
              <div class="comment-main">
                <el-avatar :size="32" :src="commentAuthors[comment.userId]?.avatar" class="comment-avatar">
                  {{ commentAuthors[comment.userId]?.username?.[0]?.toUpperCase() }}
                </el-avatar>
                <div class="comment-body">
                  <div class="comment-meta">
                    <router-link
                      :to="`/user/${comment.userId}`"
                      class="comment-author"
                    >
                      {{ commentAuthors[comment.userId]?.username ?? `用户${comment.userId}` }}
                    </router-link>
                    <span class="comment-time">{{ formatRelativeDate(comment.createdAt) }}</span>
                  </div>
                  <p class="comment-text">{{ comment.content }}</p>
                  <div class="comment-actions">
                    <el-button
                      text
                      size="small"
                      :type="commentLiked[comment.id] ? 'primary' : 'default'"
                      :icon="commentLiked[comment.id] ? StarFilled : Star"
                      @click="handleCommentLike(comment)"
                    >
                      {{ comment.likeCount > 0 ? formatCount(comment.likeCount) : '' }} 点赞
                    </el-button>

                    <el-button
                      v-if="userStore.isLoggedIn && post.status !== 1"
                      text
                      size="small"
                      :icon="ChatDotRound"
                      @click="toggleReplyBox(comment.id)"
                    >
                      回复
                    </el-button>

                    <el-popconfirm
                      v-if="canDeleteComment(comment)"
                      title="确定删除该评论？"
                      confirm-button-text="删除"
                      cancel-button-text="取消"
                      @confirm="handleDeleteComment(comment)"
                    >
                      <template #reference>
                        <el-button text size="small" type="danger" :icon="Delete">删除</el-button>
                      </template>
                    </el-popconfirm>
                  </div>

                  <!-- Reply box -->
                  <div v-if="replyingTo === comment.id" class="reply-box">
                    <el-input
                      v-model="replyText"
                      type="textarea"
                      :rows="2"
                      :placeholder="`回复 ${commentAuthors[comment.userId]?.username ?? ''}…`"
                      :maxlength="500"
                      show-word-limit
                      resize="none"
                    />
                    <div class="reply-submit-row">
                      <el-button size="small" @click="replyingTo = null">取消</el-button>
                      <el-button
                        type="primary"
                        size="small"
                        :loading="submitCommentLoading"
                        :disabled="!replyText.trim()"
                        @click="submitComment(comment.id)"
                      >
                        发表回复
                      </el-button>
                    </div>
                  </div>

                  <!-- Replies -->
                  <div v-if="replies[comment.id]?.length" class="replies-list">
                    <div
                      v-for="reply in replies[comment.id]"
                      :key="reply.id"
                      class="reply-item"
                    >
                      <el-avatar :size="24" :src="commentAuthors[reply.userId]?.avatar" class="reply-avatar">
                        {{ commentAuthors[reply.userId]?.username?.[0]?.toUpperCase() }}
                      </el-avatar>
                      <div class="reply-body">
                        <span class="comment-author">
                          {{ commentAuthors[reply.userId]?.username ?? `用户${reply.userId}` }}
                        </span>
                        <span class="comment-time">{{ formatRelativeDate(reply.createdAt) }}</span>
                        <p class="comment-text">{{ reply.content }}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- Comment pagination -->
          <div v-if="commentTotal > commentPageSize" class="pagination-wrap">
            <el-pagination
              v-model:current-page="commentPage"
              :page-size="commentPageSize"
              :total="commentTotal"
              layout="prev, pager, next"
              background
              @current-change="loadComments"
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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Clock, View, ChatDotRound, Star, StarFilled, Delete, EditPen, CollectionTag
} from '@element-plus/icons-vue'
import * as postApi from '@/api/post'
import * as commentApi from '@/api/comment'
import * as forumApi from '@/api/forum'
import * as userApi from '@/api/user'
import { toggleFavorite, isFavorited } from '@/api/favorite'
import type { Post, Comment, Forum, UserInfo } from '@/api/types'
import { formatCount, formatDate, formatRelativeDate } from '@/utils/format'
import { useUserStore } from '@/stores/user'
import AppSidebar from '@/components/layout/AppSidebar.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ── Post state ────────────────────────────────────────────────────────────────

const post = ref<Post | null>(null)
const postLoading = ref(false)
const forum = ref<Forum | null>(null)
const author = ref<UserInfo | null>(null)

const liked = ref(false)
const likeLoading = ref(false)
const deleteLoading = ref(false)

const favorited = ref(false)
const favoriteLoading = ref(false)

const canEdit = computed(
  () =>
    userStore.isLoggedIn &&
    post.value != null &&
    (userStore.userInfo?.id === post.value.userId || userStore.isAdmin)
)
const canDelete = computed(() => canEdit.value)

// ── Comment state ─────────────────────────────────────────────────────────────

const comments = ref<Comment[]>([])
const commentsLoading = ref(false)
const commentPage = ref(1)
const commentPageSize = 20
const commentTotal = ref(0)

/** userId → UserInfo cache */
const commentAuthors = ref<Record<number, UserInfo>>({})

/** commentId → liked status */
const commentLiked = ref<Record<number, boolean>>({})

/** commentId → replies array */
const replies = ref<Record<number, Comment[]>>({})

const replyingTo = ref<number | null>(null)
const replyText = ref('')
const commentText = ref('')
const submitCommentLoading = ref(false)

// ── Data loading ──────────────────────────────────────────────────────────────

async function loadPost() {
  postLoading.value = true
  try {
    post.value = await postApi.getPost(route.params.id as string)
    // Load related data in parallel
    const [forumData, authorData, likeData, favData] = await Promise.allSettled([
      forumApi.getForum(post.value.forumId),
      userApi.getUser(post.value.userId),
      userStore.isLoggedIn
        ? postApi.getPostLikeStatus(post.value.id)
        : Promise.resolve({ liked: false }),
      userStore.isLoggedIn
        ? isFavorited(post.value.id)
        : Promise.resolve(false)
    ])
    if (forumData.status === 'fulfilled') forum.value = forumData.value
    if (authorData.status === 'fulfilled') author.value = authorData.value
    if (likeData.status === 'fulfilled') liked.value = likeData.value.liked
    if (favData.status === 'fulfilled') favorited.value = favData.value
  } catch {
    post.value = null
  } finally {
    postLoading.value = false
  }
}

async function loadComments() {
  if (!post.value) return
  commentsLoading.value = true
  try {
    const page = await commentApi.listCommentsByPost(post.value.id, commentPage.value, commentPageSize)
    comments.value = page.records
    commentTotal.value = page.total

    // Pre-fetch author info for new comment authors
    const unknownIds = [...new Set(page.records.map((c) => c.userId))].filter(
      (id) => !commentAuthors.value[id]
    )
    await Promise.allSettled(
      unknownIds.map(async (id) => {
        try {
          const user = await userApi.getUser(id)
          commentAuthors.value[id] = user
        } catch {
          // ignore
        }
      })
    )

    // Load first-level replies for each comment
    await Promise.allSettled(
      page.records.map(async (c) => {
        try {
          const r = await commentApi.listReplies(c.id, 1, 5)
          if (r.records.length) {
            replies.value[c.id] = r.records
            // fetch reply author info
            const replyAuthorIds = [...new Set(r.records.map((rc) => rc.userId))].filter(
              (id) => !commentAuthors.value[id]
            )
            await Promise.allSettled(
              replyAuthorIds.map(async (id) => {
                try {
                  const u = await userApi.getUser(id)
                  commentAuthors.value[id] = u
                } catch {
                  // ignore
                }
              })
            )
          }
        } catch {
          // ignore
        }
      })
    )
  } catch {
    // handled globally
  } finally {
    commentsLoading.value = false
  }
}

onMounted(async () => {
  await loadPost()
  await loadComments()
})

// ── Actions ───────────────────────────────────────────────────────────────────

async function handleLike() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!post.value) return
  likeLoading.value = true
  try {
    const result = await postApi.togglePostLike(post.value.id)
    liked.value = result.liked
    post.value.likeCount += result.liked ? 1 : -1
  } finally {
    likeLoading.value = false
  }
}

async function handleFavorite() {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!post.value) return
  favoriteLoading.value = true
  try {
    const result = await toggleFavorite(post.value.id)
    favorited.value = result.favorited
    ElMessage.success(result.favorited ? '已收藏' : '已取消收藏')
  } catch {
    ElMessage.error('操作失败，请稍后再试')
  } finally {
    favoriteLoading.value = false
  }
}

async function handleDelete() {
  if (!post.value) return
  deleteLoading.value = true
  try {
    await postApi.deletePost(post.value.id)
    ElMessage.success('帖子已删除')
    if (forum.value) {
      router.push(`/forum/${forum.value.id}`)
    } else {
      router.push('/')
    }
  } finally {
    deleteLoading.value = false
  }
}

async function submitComment(parentId?: number) {
  if (!post.value) return
  const text = parentId ? replyText.value.trim() : commentText.value.trim()
  if (!text) return
  submitCommentLoading.value = true
  try {
    await commentApi.createComment(post.value.id, text, parentId)
    ElMessage.success('评论成功')
    if (parentId) {
      replyText.value = ''
      replyingTo.value = null
    } else {
      commentText.value = ''
    }
    await loadComments()
    if (post.value) post.value.commentCount++
  } finally {
    submitCommentLoading.value = false
  }
}

async function handleCommentLike(comment: Comment) {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  const result = await commentApi.toggleCommentLike(comment.id)
  commentLiked.value[comment.id] = result.liked
  comment.likeCount += result.liked ? 1 : -1
}

async function handleDeleteComment(comment: Comment) {
  await commentApi.deleteComment(comment.id)
  ElMessage.success('评论已删除')
  await loadComments()
  if (post.value && post.value.commentCount > 0) post.value.commentCount--
}

function canDeleteComment(comment: Comment): boolean {
  if (!userStore.isLoggedIn) return false
  return userStore.isAdmin || userStore.userInfo?.id === comment.userId
}

function toggleReplyBox(commentId: number) {
  if (replyingTo.value === commentId) {
    replyingTo.value = null
    replyText.value = ''
  } else {
    replyingTo.value = commentId
    replyText.value = ''
  }
}
</script>

<style scoped>
.post-detail-page {
  padding: 0;
}

.breadcrumb {
  margin-bottom: 16px;
}

/* Post card */
.post-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.post-header {
  margin-bottom: 4px;
}

.post-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.forum-badge {
  cursor: pointer;
}

.post-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.4;
  margin-bottom: 12px;
}

.post-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 13px;
  color: #909399;
}

.meta-author {
  display: flex;
  align-items: center;
  gap: 6px;
}

.author-avatar {
  flex-shrink: 0;
}

.author-name {
  color: #606266;
  text-decoration: none;
  font-weight: 500;
}

.author-name:hover {
  color: #409eff;
}

.meta-divider {
  color: #dcdfe6;
}

.meta-time,
.meta-stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Post content */
.post-content {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 4px 0;
}

/* Post actions */
.post-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.like-count {
  margin-left: 2px;
}

/* Comments card */
.comments-card {
  border-radius: 8px;
}

.comments-header {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.comments-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.comments-count {
  font-size: 13px;
  color: #909399;
}

/* Comment write box */
.comment-box {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 4px;
}

.comment-avatar {
  flex-shrink: 0;
  margin-top: 2px;
}

.comment-input-wrap {
  flex: 1;
  min-width: 0;
}

.comment-submit-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

/* Comment items */
.comment-item {
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.comment-item:last-child {
  border-bottom: none;
}

.comment-main {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-author {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
  text-decoration: none;
}

.comment-author:hover {
  color: #409eff;
}

.comment-time {
  font-size: 12px;
  color: #c0c4cc;
}

.comment-text {
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  margin-bottom: 6px;
  white-space: pre-wrap;
  word-break: break-word;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Reply box */
.reply-box {
  margin-top: 10px;
}

.reply-submit-row {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

/* Replies list */
.replies-list {
  margin-top: 12px;
  background: #fafafa;
  border-radius: 6px;
  padding: 4px 12px;
}

.reply-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.reply-item:last-child {
  border-bottom: none;
}

.reply-avatar {
  flex-shrink: 0;
  margin-top: 2px;
}

.reply-body {
  flex: 1;
  min-width: 0;
  font-size: 13px;
}

/* Pagination */
.pagination-wrap {
  display: flex;
  justify-content: center;
  padding: 20px 0 4px;
}
</style>
