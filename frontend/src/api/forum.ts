/**
 * Forum API module — wraps all /api/forum/* endpoints.
 */
import http from './http'
import type { Forum, PageResult } from './types'

/** Fetch the full (non-paginated) forum list. */
export function listForums(): Promise<Forum[]> {
  return http.get<{ data: Forum[] }>('/forum/list').then((res) => res.data.data)
}

/** Fetch a single forum by id. */
export function getForum(id: number | string): Promise<Forum> {
  return http.get<{ data: Forum }>(`/forum/${id}`).then((res) => res.data.data)
}

/** Paginated forum list. */
export function pageForums(current = 1, size = 20): Promise<PageResult<Forum>> {
  return http
    .get<{ data: PageResult<Forum> }>('/forum/page', { params: { current, size } })
    .then((res) => res.data.data)
}
