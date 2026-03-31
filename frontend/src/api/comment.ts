/**
 * Comment API module — wraps all /api/comment/* endpoints.
 */
import http from './http'
import type { Comment, PageResult } from './types'

/** Create a new comment (or reply if parentId is provided). */
export function createComment(
  postId: number,
  content: string,
  parentId?: number
): Promise<Comment> {
  return http
    .post<{ data: Comment }>('/comment', { postId, content, parentId })
    .then((res) => res.data.data)
}

/** Paginated top-level comments for a post. */
export function listCommentsByPost(
  postId: number | string,
  current = 1,
  size = 20
): Promise<PageResult<Comment>> {
  return http
    .get<{ data: PageResult<Comment> }>(`/comment/post/${postId}`, {
      params: { current, size }
    })
    .then((res) => res.data.data)
}

/** Paginated replies for a parent comment. */
export function listReplies(
  parentId: number | string,
  current = 1,
  size = 20
): Promise<PageResult<Comment>> {
  return http
    .get<{ data: PageResult<Comment> }>(`/comment/replies/${parentId}`, {
      params: { current, size }
    })
    .then((res) => res.data.data)
}

/** Delete a comment (soft-delete). */
export function deleteComment(id: number | string): Promise<void> {
  return http.delete(`/comment/${id}`).then(() => undefined)
}

/** Toggle like on a comment; returns whether it is now liked. */
export function toggleCommentLike(id: number | string): Promise<{ liked: boolean }> {
  return http
    .post<{ data: { liked: boolean } }>(`/comment/${id}/like`)
    .then((res) => res.data.data)
}
