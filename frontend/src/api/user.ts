/**
 * User API module — wraps all /api/user/* endpoints.
 */
import http from './http'
import type { PageResult, UserInfo } from './types'

/** Login and receive a JWT token. */
export function login(username: string, password: string): Promise<{ token: string }> {
  return http
    .post<{ data: { token: string } }>('/user/login', { username, password })
    .then((res) => res.data.data)
}

/** Register a new user account. */
export function register(
  username: string,
  password: string,
  email: string
): Promise<UserInfo> {
  return http
    .post<{ data: UserInfo }>('/user/register', { username, password, email })
    .then((res) => res.data.data)
}

/** Notify the backend to blacklist the current token. */
export function logout(): Promise<void> {
  return http.post('/user/logout').then(() => undefined)
}

/** Fetch the currently authenticated user's full profile. */
export function getUserInfo(): Promise<UserInfo> {
  return http.get<{ data: UserInfo }>('/user/info').then((res) => res.data.data)
}

/** Fetch a public user profile by id (email is stripped server-side). */
export function getUser(id: number | string): Promise<UserInfo> {
  return http.get<{ data: UserInfo }>(`/user/${id}`).then((res) => res.data.data)
}

/** Update avatar and/or bio for the current user. */
export function updateProfile(avatar: string, bio: string): Promise<void> {
  return http.put('/user/profile', { avatar, bio }).then(() => undefined)
}

/** Change the current user's password. */
export function changePassword(oldPassword: string, newPassword: string): Promise<void> {
  return http.put('/user/password', { oldPassword, newPassword }).then(() => undefined)
}

/** Paginated user list (admin use). */
export function listUsers(
  current = 1,
  size = 20,
  keyword?: string
): Promise<PageResult<UserInfo>> {
  return http
    .get<{ data: PageResult<UserInfo> }>('/user/list', { params: { current, size, keyword } })
    .then((res) => res.data.data)
}
