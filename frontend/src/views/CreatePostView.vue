<template>
  <div class="create-post-page">
    <el-page-header @back="router.go(-1)" title="发布帖子" />
    <el-divider />
    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="版块" prop="forumId">
          <el-select v-model="form.forumId" placeholder="请选择版块" style="width: 100%">
            <el-option
              v-for="f in forums"
              :key="f.id"
              :label="f.name"
              :value="f.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入帖子标题" clearable />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="请输入帖子内容"
          />
        </el-form-item>
        <el-form-item label="人机验证">
          <CaptchaWidget scene="POST" @verified="onCaptchaVerified" ref="captchaRef" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">发布</el-button>
          <el-button @click="router.go(-1)">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getForumList } from '@/api/forum'
import { createPost } from '@/api/post'
import type { Forum } from '@/types/api'
import CaptchaWidget from '@/components/CaptchaWidget.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const captchaRef = ref<InstanceType<typeof CaptchaWidget>>()
const loading = ref(false)
const forums = ref<Forum[]>([])
const captchaToken = ref('')

const form = reactive({
  forumId: undefined as number | undefined,
  title: '',
  content: ''
})

const rules: FormRules = {
  forumId: [{ required: true, message: '请选择版块', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

onMounted(async () => {
  const { data } = await getForumList()
  forums.value = data.data
})

function onCaptchaVerified(token: string) {
  captchaToken.value = token
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (!captchaToken.value) {
    ElMessage.warning('请先完成人机验证')
    return
  }

  loading.value = true
  try {
    const { data } = await createPost({
      forumId: form.forumId!,
      title: form.title,
      content: form.content
    }, captchaToken.value)
    ElMessage.success('发布成功')
    router.push(`/post/${data.data.id}`)
  } catch (err) {
    console.error('[CreatePostView] Post submission failed:', err)
    captchaToken.value = ''
    captchaRef.value?.reload()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.create-post-page {
  max-width: 960px;
  margin: 24px auto;
  padding: 0 16px;
}
</style>
