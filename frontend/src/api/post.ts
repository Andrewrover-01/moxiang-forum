/**
 * Post API module — wraps all /api/post/* endpoints.
 */
import http from './http'
import type { PageResult, Post } from './types'

/** Create a new post. */
export function createPost(
  forumId: number,
  title: string,
  content: string,
  tagIds?: number[]
): Promise<Post> {
  return http
    .post<{ data: Post }>('/post', { forumId, title, content, tagIds })
    .then((res) => res.data.data)
}

/** Fetch a single post by id (also increments view count server-side). */
export function getPost(id: number | string): Promise<Post> {
  return http.get<{ data: Post }>(`/post/${id}`).then((res) => res.data.data)
}

/** Paginated post list, optionally filtered by forum. */
export function listPosts(
  forumId?: number,
  current = 1,
  size = 20
): Promise<PageResult<Post>> {
  return http
    .get<{ data: PageResult<Post> }>('/post/list', { params: { forumId, current, size } })
    .then((res) => res.data.data)
}

/** Posts authored by a specific user (paginated). */
export function listUserPosts(
  userId: number | string,
  current = 1,
  size = 20
): Promise<PageResult<Post>> {
  return http
    .get<{ data: PageResult<Post> }>(`/post/user/${userId}`, { params: { current, size } })
    .then((res) => res.data.data)
}

/** Full-text search over posts (paginated). */
export function searchPosts(
  keyword: string,
  current = 1,
  size = 20
): Promise<PageResult<Post>> {
  return http
    .get<{ data: PageResult<Post> }>('/post/search', { params: { keyword, current, size } })
    .then((res) => res.data.data)
}

/** Fetch the top N hot posts. */
export function hotPosts(limit = 10): Promise<Post[]> {
  return http
    .get<{ data: Post[] }>('/post/hot', { params: { limit } })
    .then((res) => res.data.data)
}

/** Update a post's title and/or content. */
export function updatePost(id: number | string, title: string, content: string): Promise<void> {
  return http.put(`/post/${id}`, { title, content }).then(() => undefined)
}

/** Delete a post (soft-delete). */
export function deletePost(id: number | string): Promise<void> {
  return http.delete(`/post/${id}`).then(() => undefined)
}

/** Toggle like on a post; returns whether the post is now liked. */
export function togglePostLike(id: number | string): Promise<{ liked: boolean }> {
  return http
    .post<{ data: { liked: boolean } }>(`/post/${id}/like`)
    .then((res) => res.data.data)
}

/** Check whether the current user has liked a post. */
export function getPostLikeStatus(id: number | string): Promise<{ liked: boolean }> {
  return http
    .get<{ data: { liked: boolean } }>(`/post/${id}/like/status`)
    .then((res) => res.data.data)
}
