/**
 * Notification API module — wraps all /api/notification/* endpoints.
 */
import http from './http'
import type { PageResult } from './types'

export interface Notification {
  id: number
  userId: number
  senderId: number
  /** e.g. LIKE_POST | COMMENT_POST | REPLY_COMMENT | FOLLOW */
  type: string
  targetId: number
  content: string
  /** 0 = unread, 1 = read */
  isRead: number
  createdAt?: string
}

/** Paginated list of notifications for the current user (newest first). */
export function listNotifications(current = 1, size = 20): Promise<PageResult<Notification>> {
  return http
    .get<{ data: PageResult<Notification> }>('/notification/list', {
      params: { current, size }
    })
    .then((res) => res.data.data)
}

/** Get the number of unread notifications for the current user. */
export function getUnreadCount(): Promise<number> {
  return http
    .get<{ data: number }>('/notification/unread/count')
    .then((res) => res.data.data)
}

/** Mark a single notification as read. */
export function markAsRead(id: number | string): Promise<void> {
  return http.put(`/notification/read/${id}`).then(() => undefined)
}

/** Mark all notifications as read. */
export function markAllAsRead(): Promise<void> {
  return http.put('/notification/read/all').then(() => undefined)
}
