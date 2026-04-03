<template>
  <div class="register-page">
    <el-card class="register-card">
      <template #header>
        <div class="card-header">
          <span>注册墨香论坛</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="2-20个字符，支持字母、数字、下划线、汉字" clearable />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" clearable />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="6-32位密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="人机验证">
          <CaptchaWidget scene="REGISTER" @verified="onCaptchaVerified" ref="captchaRef" />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            style="width: 100%"
            :loading="loading"
            @click="handleRegister"
          >
            注册
          </el-button>
        </el-form-item>

        <div class="form-footer">
          已有账号？
          <el-link type="primary" @click="router.push('/login')">立即登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { register } from '@/api/user'
import CaptchaWidget from '@/components/CaptchaWidget.vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const captchaRef = ref<InstanceType<typeof CaptchaWidget>>()
const loading = ref(false)
const captchaToken = ref('')

const form = reactive({
  username: '',
  email: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度需在2-20个字符之间', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度需在6-32个字符之间', trigger: 'blur' }
  ]
}

function onCaptchaVerified(token: string) {
  captchaToken.value = token
}

async function handleRegister() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (!captchaToken.value) {
    ElMessage.warning('请先完成人机验证')
    return
  }

  loading.value = true
  try {
    await register(form.username, form.password, form.email, captchaToken.value)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (err) {
    console.error('[RegisterView] Registration failed:', err)
    // Request interceptor already shows the error; refresh CAPTCHA on any failure
    captchaToken.value = ''
    captchaRef.value?.reload()
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f7fa;
}
.register-card {
  width: 420px;
}
.card-header {
  font-size: 18px;
  font-weight: bold;
  text-align: center;
}
.form-footer {
  text-align: center;
  color: #909399;
}
</style>
