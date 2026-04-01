import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import router from '@/router'
import type { ApiResponse } from '@/types/api'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

/**
 * 请求拦截器
 * 若 Pinia store 中存有 token，则自动在请求头中添加 Authorization: Bearer <token>
 */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.set('Authorization', `Bearer ${userStore.token}`)
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 响应拦截器
 * 统一处理业务状态码：code === 200 表示成功，否则弹出错误提示并拒绝 Promise
 */
request.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data
    if (res.code === 200) {
      return response
    }
    // 业务错误：弹出后端返回的错误信息
    ElMessage.error(res.message || '请求失败，请稍后重试')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    // HTTP 层面错误（网络异常、超时、4xx/5xx 等）
    if (error.response) {
      const status = error.response.status as number
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录')
        const userStore = useUserStore()
        userStore.clearToken()
        router.push({ name: 'Login' })
      } else if (status === 403) {
        ElMessage.error('权限不足，无法执行此操作')
      } else if (status === 500) {
        ElMessage.error('服务器内部错误，请稍后重试')
      } else {
        ElMessage.error(error.response.data?.message || `请求错误（${status}）`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接')
    } else {
      ElMessage.error('网络异常，请检查网络连接')
    }
    return Promise.reject(error)
  }
)

export default request
