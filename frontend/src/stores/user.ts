import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/api/http'

export interface UserInfo {
  id: number
  username: string
  email: string
  avatar?: string
  bio?: string
  role: 'USER' | 'ADMIN'
  status?: number
  createdAt?: string
}

const TOKEN_KEY = 'mx_token'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const userInfo = ref<UserInfo | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')

  // ── Internal helpers ────────────────────────────────────────────────────────
  function setToken(value: string) {
    token.value = value
    localStorage.setItem(TOKEN_KEY, value)
  }

  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem(TOKEN_KEY)
  }

  // ── API actions ─────────────────────────────────────────────────────────────

  /** Login with username + password; stores JWT and fetches user info. */
  async function login(username: string, password: string): Promise<void> {
    loading.value = true
    try {
      const res = await http.post<{ data: { token: string } }>('/user/login', {
        username,
        password
      })
      setToken(res.data.data.token)
      await fetchUserInfo()
    } finally {
      loading.value = false
    }
  }

  /** Register a new account. Returns the created user. */
  async function register(
    username: string,
    password: string,
    email: string
  ): Promise<UserInfo> {
    loading.value = true
    try {
      const res = await http.post<{ data: UserInfo }>('/user/register', {
        username,
        password,
        email
      })
      return res.data.data
    } finally {
      loading.value = false
    }
  }

  /** Fetch the currently authenticated user's info from the server. */
  async function fetchUserInfo(): Promise<void> {
    if (!token.value) return
    try {
      const res = await http.get<{ data: UserInfo }>('/user/info')
      setUserInfo(res.data.data)
    } catch {
      // Token is invalid / expired — clear local state
      logout()
    }
  }

  /** Update avatar and/or bio for the current user. */
  async function updateProfile(avatar: string, bio: string): Promise<void> {
    await http.put('/user/profile', { avatar, bio })
    if (userInfo.value) {
      userInfo.value.avatar = avatar
      userInfo.value.bio = bio
    }
  }

  /** Change the current user's password. */
  async function changePassword(
    oldPassword: string,
    newPassword: string
  ): Promise<void> {
    await http.put('/user/password', { oldPassword, newPassword })
  }

  /** Notify the backend to blacklist the token, then clear local state. */
  async function callLogout(): Promise<void> {
    if (token.value) {
      try {
        await http.post('/user/logout')
      } catch {
        // Ignore — we still clear local state
      }
    }
    logout()
  }

  return {
    token,
    userInfo,
    loading,
    isLoggedIn,
    isAdmin,
    setToken,
    setUserInfo,
    logout,
    login,
    register,
    fetchUserInfo,
    updateProfile,
    changePassword,
    callLogout
  }
})

