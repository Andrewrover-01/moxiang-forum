/**
 * Admin API module — wraps all /api/admin/* endpoints.
 * All endpoints require the ADMIN role (enforced server-side via Spring Security).
 */
import http from './http'
import type { Forum, PageResult, UserInfo } from './types'

// ── Forum management ──────────────────────────────────────────────────────────

export interface ForumPayload {
  name: string
  description: string
  icon?: string
  sortOrder?: number
}

/** Create a new forum section. */
export function createForum(payload: ForumPayload): Promise<Forum> {
  return http.post<{ data: Forum }>('/admin/forum', payload).then((res) => res.data.data)
}

/** Update an existing forum section. */
export function updateForum(id: number | string, payload: ForumPayload): Promise<void> {
  return http.put(`/admin/forum/${id}`, payload).then(() => undefined)
}

/** Soft-delete a forum section. */
export function deleteForum(id: number | string): Promise<void> {
  return http.delete(`/admin/forum/${id}`).then(() => undefined)
}

/** Enable or disable a forum section (status: 0 = normal, 1 = disabled). */
export function updateForumStatus(id: number | string, status: number): Promise<void> {
  return http.put(`/admin/forum/${id}/status`, { status }).then(() => undefined)
}

// ── Post management ───────────────────────────────────────────────────────────

/** Pin or un-pin a post (isTop: 0 = normal, 1 = pinned). */
export function setPostTop(id: number | string, isTop: number): Promise<void> {
  return http.put(`/admin/post/${id}/top`, { isTop }).then(() => undefined)
}

/** Feature or un-feature a post (isFeatured: 0 = normal, 1 = featured). */
export function setPostFeatured(id: number | string, isFeatured: number): Promise<void> {
  return http.put(`/admin/post/${id}/featured`, { isFeatured }).then(() => undefined)
}

// ── User management ───────────────────────────────────────────────────────────

/** Paginated user list with optional keyword search. */
export function listAdminUsers(
  current = 1,
  size = 20,
  keyword?: string
): Promise<PageResult<UserInfo>> {
  return http
    .get<{ data: PageResult<UserInfo> }>('/admin/user/list', {
      params: { current, size, keyword }
    })
    .then((res) => res.data.data)
}

/** Enable or disable a user account (status: 0 = normal, 1 = disabled). */
export function updateUserStatus(id: number | string, status: number): Promise<void> {
  return http.put(`/admin/user/${id}/status`, { status }).then(() => undefined)
}

/** Soft-delete a user account. */
export function deleteUser(id: number | string): Promise<void> {
  return http.delete(`/admin/user/${id}`).then(() => undefined)
}
