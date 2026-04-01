/**
 * Favorite API module — wraps all /api/favorite/* endpoints.
 */
import http from './http'
import type { Post, PageResult } from './types'

/** Toggle favorite (bookmark) status for a post; returns whether it is now favorited. */
export function toggleFavorite(postId: number | string): Promise<{ favorited: boolean }> {
  return http
    .post<{ data: { favorited: boolean } }>(`/favorite/post/${postId}`)
    .then((res) => res.data.data)
}

/** Check whether the current user has favorited a post. */
export function isFavorited(postId: number | string): Promise<boolean> {
  return http
    .get<{ data: boolean }>(`/favorite/post/${postId}/status`)
    .then((res) => res.data.data)
}

/** Paginated list of posts favorited by the current user. */
export function listFavoritePosts(current = 1, size = 20): Promise<PageResult<Post>> {
  return http
    .get<{ data: PageResult<Post> }>('/favorite/posts', { params: { current, size } })
    .then((res) => res.data.data)
}
