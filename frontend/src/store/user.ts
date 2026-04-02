import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'moxiang_token'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const userInfo = ref<{
    id?: number
    username?: string
    avatar?: string
    role?: string
  }>({})

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value.role === 'ADMIN')

  /** 登录成功后保存 token */
  function setToken(value: string) {
    token.value = value
    localStorage.setItem(TOKEN_KEY, value)
  }

  /** 退出登录时清除 token 和用户信息 */
  function clearToken() {
    token.value = ''
    userInfo.value = {}
    localStorage.removeItem(TOKEN_KEY)
  }

  /** 设置用户信息（登录或刷新后调用） */
  function setUserInfo(info: typeof userInfo.value) {
    userInfo.value = info
  }

  return { token, userInfo, isLoggedIn, isAdmin, setToken, clearToken, setUserInfo }
})
