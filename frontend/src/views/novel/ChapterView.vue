<template>
  <div class="chapter-page">
    <!-- Breadcrumb -->
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: '/novel' }">小说列表</el-breadcrumb-item>
      <el-breadcrumb-item
        v-if="novel"
        :to="{ path: `/novel/${novelId}` }"
      >
        {{ novel.title }}
      </el-breadcrumb-item>
      <el-breadcrumb-item>{{ chapter?.title ?? '阅读章节' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <!-- Loading skeleton -->
    <el-card v-if="loading" shadow="never" class="reader-card">
      <el-skeleton :rows="20" animated />
    </el-card>

    <!-- Not found -->
    <el-card v-else-if="!chapter" shadow="never" class="reader-card">
      <el-result
        icon="warning"
        title="章节不存在"
        sub-title="该章节可能已被删除或地址有误"
      >
        <template #extra>
          <el-button @click="router.push(`/novel/${novelId}`)">返回小说详情</el-button>
        </template>
      </el-result>
    </el-card>

    <!-- Reader -->
    <template v-else>
      <el-card shadow="never" class="reader-card">
        <!-- Chapter header -->
        <div class="chapter-header">
          <h1 class="chapter-title">{{ chapter.title }}</h1>
          <div class="chapter-meta">
            <span class="meta-item">第{{ chapter.chapterNumber }}章</span>
            <span v-if="chapter.wordCount" class="meta-item">
              <el-icon><Document /></el-icon>
              {{ formatCount(chapter.wordCount) }} 字
            </span>
            <span v-if="chapter.createdAt" class="meta-item">
              <el-icon><Calendar /></el-icon>
              {{ formatDateOnly(chapter.createdAt) }}
            </span>
          </div>

          <!-- Owner / admin actions -->
          <div v-if="canEdit" class="chapter-actions">
            <el-button
              size="small"
              :icon="EditPen"
              @click="openEditDialog"
            >
              编辑
            </el-button>
            <el-popconfirm
              title="确定删除本章节？"
              confirm-button-text="删除"
              cancel-button-text="取消"
              @confirm="handleDelete"
            >
              <template #reference>
                <el-button
                  size="small"
                  type="danger"
                  :icon="Delete"
                  :loading="deleteLoading"
                >
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>

        <el-divider />

        <!-- Content -->
        <div class="chapter-content">{{ chapter.content }}</div>

        <el-divider />

        <!-- Prev / Next navigation -->
        <div class="nav-row">
          <el-button
            :disabled="prevChapter == null"
            :icon="ArrowLeft"
            @click="navTo(prevChapter)"
          >
            {{ prevChapter ? `上一章：${prevChapter.title}` : '已是第一章' }}
          </el-button>
          <el-button @click="router.push(`/novel/${novelId}`)">
            <el-icon><List /></el-icon>
            目录
          </el-button>
          <el-button
            :disabled="nextChapter == null"
            @click="navTo(nextChapter)"
          >
            {{ nextChapter ? `下一章：${nextChapter.title}` : '已是最新章' }}
            <el-icon v-if="nextChapter"><ArrowRight /></el-icon>
          </el-button>
        </div>
      </el-card>
    </template>

    <!-- ── Edit dialog ───────────────────────────────────────────────────── -->
    <el-dialog
      v-model="editVisible"
      title="编辑章节"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="80px"
      >
        <el-form-item label="章节标题" prop="title">
          <el-input v-model="editForm.title" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="正文" prop="content">
          <el-input
            v-model="editForm.content"
            type="textarea"
            :rows="18"
            placeholder="在这里书写章节内容…"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveLoading" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Document, Calendar, EditPen, Delete, ArrowLeft, ArrowRight, List } from '@element-plus/icons-vue'
import * as novelApi from '@/api/novel'
import type { Novel, NovelChapter } from '@/api/types'
import { formatCount, formatDateOnly } from '@/utils/format'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// ── Route params ──────────────────────────────────────────────────────────────

const novelId = computed(() => route.params.novelId as string)
const chapterId = computed(() => route.params.chapterId as string)

// ── State ─────────────────────────────────────────────────────────────────────

const novel = ref<Novel | null>(null)
const chapter = ref<NovelChapter | null>(null)
const allChapters = ref<NovelChapter[]>([])
const loading = ref(false)

const deleteLoading = ref(false)

// Edit dialog
const editVisible = ref(false)
const saveLoading = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = ref({ title: '', content: '' })
const editRules: FormRules = {
  title: [{ required: true, message: '请输入章节标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入章节正文', trigger: 'blur' }]
}

// ── Computed ──────────────────────────────────────────────────────────────────

const currentIndex = computed(() =>
  allChapters.value.findIndex((c) => String(c.id) === String(chapterId.value))
)

const prevChapter = computed<NovelChapter | null>(() => {
  const idx = currentIndex.value
  return idx > 0 ? allChapters.value[idx - 1] : null
})

const nextChapter = computed<NovelChapter | null>(() => {
  const idx = currentIndex.value
  return idx >= 0 && idx < allChapters.value.length - 1
    ? allChapters.value[idx + 1]
    : null
})

const canEdit = computed(
  () =>
    userStore.isLoggedIn &&
    novel.value != null &&
    (userStore.userInfo?.id === novel.value.userId || userStore.isAdmin)
)

// ── Data loading ──────────────────────────────────────────────────────────────

async function loadAll() {
  loading.value = true
  try {
    const [chapterData, chaptersData] = await Promise.all([
      novelApi.getChapter(chapterId.value),
      novelApi.listChapters(novelId.value)
    ])
    chapter.value = chapterData
    allChapters.value = chaptersData

    // Load novel info for breadcrumb + edit permission check
    novelApi.getNovel(novelId.value).then((n) => {
      novel.value = n
    }).catch(() => {})
  } catch {
    chapter.value = null
  } finally {
    loading.value = false
  }
}

onMounted(loadAll)

// Reload when navigating between chapters without unmounting the component
watch(chapterId, () => {
  loadAll()
  window.scrollTo({ top: 0, behavior: 'smooth' })
})

// ── Navigation ────────────────────────────────────────────────────────────────

function navTo(target: NovelChapter | null) {
  if (!target) return
  router.push(`/novel/${novelId.value}/chapter/${target.id}`)
}

// ── Edit ─────────────────────────────────────────────────────────────────────

function openEditDialog() {
  if (!chapter.value) return
  editForm.value = {
    title: chapter.value.title,
    content: chapter.value.content
  }
  editVisible.value = true
}

async function handleSave() {
  if (!editFormRef.value || !chapter.value) return
  await editFormRef.value.validate()
  saveLoading.value = true
  try {
    await novelApi.updateChapter(chapter.value.id, editForm.value.title, editForm.value.content)
    // Update local state
    chapter.value = {
      ...chapter.value,
      title: editForm.value.title,
      content: editForm.value.content,
      wordCount: editForm.value.content.replace(/\s/g, '').length
    }
    editVisible.value = false
    ElMessage.success('章节已更新')
  } finally {
    saveLoading.value = false
  }
}

// ── Delete ────────────────────────────────────────────────────────────────────

async function handleDelete() {
  if (!chapter.value) return
  deleteLoading.value = true
  try {
    await novelApi.deleteChapter(chapter.value.id)
    ElMessage.success('章节已删除')
    router.push(`/novel/${novelId.value}`)
  } finally {
    deleteLoading.value = false
  }
}
</script>

<style scoped>
.chapter-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 0;
}

.breadcrumb {
  margin-bottom: 16px;
}

.reader-card {
  border-radius: 8px;
}

/* Chapter header */
.chapter-header {
  text-align: center;
  padding: 8px 0 4px;
}

.chapter-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.5;
  margin-bottom: 10px;
}

.chapter-meta {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 10px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.chapter-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 8px;
}

/* Content */
.chapter-content {
  font-size: 17px;
  line-height: 2;
  color: #2c3e50;
  white-space: pre-wrap;
  word-break: break-word;
  padding: 0 8px;
  max-width: 720px;
  margin: 0 auto;
  /* Indent each paragraph-like block */
  text-indent: 2em;
}

/* Navigation row */
.nav-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 4px 0;
}

.nav-row .el-button {
  flex: 1;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Center "目录" button */
.nav-row .el-button:nth-child(2) {
  flex: 0 0 auto;
  max-width: none;
}
</style>
