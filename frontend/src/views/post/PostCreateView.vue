<template>
  <div class="post-create-page">
    <el-row :gutter="20" justify="center">
      <el-col :xs="24" :sm="24" :md="16" :lg="14">
        <el-card shadow="never" class="create-card">
          <template #header>
            <div class="card-header">
              <el-icon><EditPen /></el-icon>
              <span>发布帖子</span>
            </div>
          </template>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="80px"
            @submit.prevent="handleSubmit"
          >
            <!-- Forum -->
            <el-form-item label="所属板块" prop="forumId">
              <el-select
                v-model="form.forumId"
                placeholder="请选择板块"
                filterable
                :loading="forumsLoading"
                style="width: 100%"
              >
                <el-option
                  v-for="f in forums"
                  :key="f.id"
                  :label="f.name"
                  :value="f.id"
                />
              </el-select>
            </el-form-item>

            <!-- Title -->
            <el-form-item label="标题" prop="title">
              <el-input
                v-model="form.title"
                placeholder="请输入帖子标题（5~100字）"
                :maxlength="100"
                show-word-limit
                clearable
              />
            </el-form-item>

            <!-- Content -->
            <el-form-item label="内容" prop="content">
              <el-input
                v-model="form.content"
                type="textarea"
                placeholder="请输入帖子内容（至少 10 个字符）"
                :rows="12"
                :maxlength="10000"
                show-word-limit
                resize="vertical"
              />
            </el-form-item>

            <!-- Actions -->
            <el-form-item>
              <div class="form-actions">
                <el-button @click="handleCancel">取消</el-button>
                <el-button
                  type="primary"
                  native-type="submit"
                  :loading="submitting"
                  @click="handleSubmit"
                >
                  发布帖子
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'
import * as postApi from '@/api/post'
import * as forumApi from '@/api/forum'
import type { Forum, PostCreateForm } from '@/api/types'

const route = useRoute()
const router = useRouter()

// ── Form state ────────────────────────────────────────────────────────────────

const formRef = ref<FormInstance>()

const form = ref<PostCreateForm>({
  forumId: undefined,
  title: '',
  content: ''
})

const submitting = ref(false)

// ── Forum list ────────────────────────────────────────────────────────────────

const forums = ref<Forum[]>([])
const forumsLoading = ref(false)

async function loadForums() {
  forumsLoading.value = true
  try {
    forums.value = await forumApi.listForums()
  } finally {
    forumsLoading.value = false
  }
}

// ── Validation rules ──────────────────────────────────────────────────────────

const rules: FormRules = {
  forumId: [{ required: true, message: '请选择所属板块', trigger: 'change' }],
  title: [
    { required: true, message: '请输入帖子标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度应在 5 ~ 100 字之间', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入帖子内容', trigger: 'blur' },
    { min: 10, message: '内容不能少于 10 个字符', trigger: 'blur' }
  ]
}

// ── Lifecycle ─────────────────────────────────────────────────────────────────

onMounted(async () => {
  await loadForums()
  // Pre-fill forumId from query param (e.g. from ForumDetailView's "发布帖子" button)
  const forumIdParam = route.query.forumId
  if (forumIdParam) {
    const parsed = Number(forumIdParam)
    if (!isNaN(parsed) && forums.value.some((f) => f.id === parsed)) {
      form.value.forumId = parsed
    }
  }
})

// ── Submit ────────────────────────────────────────────────────────────────────

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (form.value.forumId == null) return

  submitting.value = true
  try {
    const post = await postApi.createPost(form.value.forumId, form.value.title, form.value.content)
    ElMessage.success('帖子发布成功！')
    router.push(`/post/${post.id}`)
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  const forumIdParam = route.query.forumId
  if (forumIdParam) {
    router.push(`/forum/${forumIdParam}`)
  } else {
    router.back()
  }
}
</script>

<style scoped>
.post-create-page {
  padding: 0;
}

.create-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  width: 100%;
}
</style>
