import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as forumApi from '@/api/forum'
import type { Forum, PageResult } from '@/api/types'

// Re-export for backward compatibility with any components importing from this store
export type { Forum }

// ── Store ─────────────────────────────────────────────────────────────────────

export const useForumStore = defineStore('forum', () => {
  /** Full list of forums (for nav / sidebar usage). */
  const forums = ref<Forum[]>([])

  /** Currently viewed forum detail. */
  const currentForum = ref<Forum | null>(null)

  /** Paginated forum list (for admin / browse pages). */
  const page = ref<PageResult<Forum>>({
    records: [],
    total: 0,
    size: 20,
    current: 1,
    pages: 0
  })

  /** Whether a network request is in flight. */
  const loading = ref(false)

  /** Last error message, if any. */
  const error = ref<string | null>(null)

  // ── Actions ─────────────────────────────────────────────────────────────────

  /** Fetch the complete (non-paginated) forum list.  Cached after first call. */
  async function fetchForums(force = false): Promise<void> {
    if (forums.value.length > 0 && !force) return
    loading.value = true
    error.value = null
    try {
      forums.value = await forumApi.listForums()
    } catch (e: any) {
      error.value = e?.message ?? '加载板块失败'
    } finally {
      loading.value = false
    }
  }

  /** Fetch a single forum by id. */
  async function fetchForum(id: number | string): Promise<void> {
    loading.value = true
    error.value = null
    try {
      currentForum.value = await forumApi.getForum(id)
    } catch (e: any) {
      error.value = e?.message ?? '加载板块详情失败'
      currentForum.value = null
    } finally {
      loading.value = false
    }
  }

  /** Fetch paginated forums (used by admin panel / explore page). */
  async function fetchForumPage(current = 1, size = 20): Promise<void> {
    loading.value = true
    error.value = null
    try {
      page.value = await forumApi.pageForums(current, size)
    } catch (e: any) {
      error.value = e?.message ?? '加载板块列表失败'
    } finally {
      loading.value = false
    }
  }

  /** Clear the currently viewed forum. */
  function clearCurrentForum() {
    currentForum.value = null
  }

  return {
    forums,
    currentForum,
    page,
    loading,
    error,
    fetchForums,
    fetchForum,
    fetchForumPage,
    clearCurrentForum
  }
})
