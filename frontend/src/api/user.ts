import request from '@/utils/request'
import type { ApiResponse, LoginResult, User, PageResult } from '@/types/api'
import { CAPTCHA_TOKEN_HEADER } from '@/utils/captchaHeader'

/** 登录 */
export function login(username: string, password: string) {
  return request.post<ApiResponse<LoginResult>>('/user/login', { username, password })
}

/** 注册 */
export function register(username: string, password: string, email: string, captchaToken?: string) {
  return request.post<ApiResponse<User>>('/user/register', { username, password, email }, {
    headers: captchaToken ? { [CAPTCHA_TOKEN_HEADER]: captchaToken } : {}
  })
}

/** 退出登录 */
export function logout() {
  return request.post<ApiResponse<null>>('/user/logout')
}

/** 获取当前登录用户信息 */
export function getUserInfo() {
  return request.get<ApiResponse<User>>('/user/info')
}

/** 获取指定用户公开信息 */
export function getUserById(id: number) {
  return request.get<ApiResponse<User>>(`/user/${id}`)
}

/** 更新个人资料 */
export function updateProfile(avatar: string, bio: string) {
  return request.put<ApiResponse<null>>('/user/profile', { avatar, bio })
}

/** 修改密码 */
export function changePassword(oldPassword: string, newPassword: string) {
  return request.put<ApiResponse<null>>('/user/password', { oldPassword, newPassword })
}

/** 用户列表（需要登录） */
export function getUserList(current = 1, size = 20, keyword?: string) {
  return request.get<ApiResponse<PageResult<User>>>('/user/list', {
    params: { current, size, keyword }
  })
}
