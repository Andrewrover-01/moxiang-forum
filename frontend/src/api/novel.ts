/**
 * Novel API module — wraps all /api/novel/* and chapter endpoints.
 */
import http from './http'
import type { Novel, NovelChapter, PageResult } from './types'

// ── Novel CRUD ────────────────────────────────────────────────────────────────

/** Create a new novel. */
export function createNovel(
  title: string,
  description: string,
  cover: string,
  category: string
): Promise<Novel> {
  return http
    .post<{ data: Novel }>('/novel', { title, description, cover, category })
    .then((res) => res.data.data)
}

/** Fetch a single novel by id (also increments view count server-side). */
export function getNovel(id: number | string): Promise<Novel> {
  return http.get<{ data: Novel }>(`/novel/${id}`).then((res) => res.data.data)
}

/** Paginated novel list, optionally filtered by category. */
export function listNovels(
  category?: string,
  current = 1,
  size = 20
): Promise<PageResult<Novel>> {
  return http
    .get<{ data: PageResult<Novel> }>('/novel/list', { params: { category, current, size } })
    .then((res) => res.data.data)
}

/** Full-text search over novels (paginated). */
export function searchNovels(
  keyword: string,
  current = 1,
  size = 20
): Promise<PageResult<Novel>> {
  return http
    .get<{ data: PageResult<Novel> }>('/novel/search', { params: { keyword, current, size } })
    .then((res) => res.data.data)
}

/** Novels authored by a specific user (paginated). */
export function listUserNovels(
  userId: number | string,
  current = 1,
  size = 20
): Promise<PageResult<Novel>> {
  return http
    .get<{ data: PageResult<Novel> }>(`/novel/user/${userId}`, { params: { current, size } })
    .then((res) => res.data.data)
}

/** Update novel metadata. All fields are optional on the frontend. */
export function updateNovel(
  id: number | string,
  payload: {
    title?: string
    description?: string
    cover?: string
    category?: string
    status?: number
  }
): Promise<void> {
  return http.put(`/novel/${id}`, payload).then(() => undefined)
}

/** Delete a novel (soft-delete). */
export function deleteNovel(id: number | string): Promise<void> {
  return http.delete(`/novel/${id}`).then(() => undefined)
}

/** Toggle collection on a novel; returns whether it is now collected. */
export function toggleCollect(id: number | string): Promise<{ collected: boolean }> {
  return http
    .post<{ data: { collected: boolean } }>(`/novel/${id}/collect`)
    .then((res) => res.data.data)
}

/** Check whether the current user has collected a novel. */
export function getCollectStatus(id: number | string): Promise<{ collected: boolean }> {
  return http
    .get<{ data: { collected: boolean } }>(`/novel/${id}/collect/status`)
    .then((res) => res.data.data)
}

// ── Chapter CRUD ──────────────────────────────────────────────────────────────

/** Add a chapter to a novel. */
export function addChapter(
  novelId: number | string,
  title: string,
  content: string
): Promise<NovelChapter> {
  return http
    .post<{ data: NovelChapter }>(`/novel/${novelId}/chapter`, { title, content })
    .then((res) => res.data.data)
}

/** Fetch all chapters for a novel (table-of-contents list). */
export function listChapters(novelId: number | string): Promise<NovelChapter[]> {
  return http
    .get<{ data: NovelChapter[] }>(`/novel/${novelId}/chapters`)
    .then((res) => res.data.data)
}

/** Fetch a single chapter's full content by chapter id. */
export function getChapter(chapterId: number | string): Promise<NovelChapter> {
  return http
    .get<{ data: NovelChapter }>(`/novel/chapter/${chapterId}`)
    .then((res) => res.data.data)
}

/** Update a chapter's title and/or content. */
export function updateChapter(
  chapterId: number | string,
  title: string,
  content: string
): Promise<void> {
  return http.put(`/novel/chapter/${chapterId}`, { title, content }).then(() => undefined)
}

/** Delete a chapter (soft-delete). */
export function deleteChapter(chapterId: number | string): Promise<void> {
  return http.delete(`/novel/chapter/${chapterId}`).then(() => undefined)
}
