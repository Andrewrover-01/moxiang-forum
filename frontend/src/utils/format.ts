/**
 * Shared formatting utilities for dates, numbers, and domain labels.
 */

// ── Date / time ───────────────────────────────────────────────────────────────

/**
 * Format an ISO date string as an absolute date-time.
 * Returns "-" for null / invalid values.
 */
export function formatDate(dateStr: string | undefined | null): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(d)
}

/**
 * Format an ISO date string as a date only (no time component).
 * Returns "-" for null / invalid values.
 */
export function formatDateOnly(dateStr: string | undefined | null): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).format(d)
}

/**
 * Return a human-readable relative time string.
 * Examples: "刚刚", "5分钟前", "3小时前", "2天前", "1个月前", "2年前"
 */
export function formatRelativeDate(dateStr: string | undefined | null): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return '-'
  const diff = Date.now() - d.getTime()
  const minutes = Math.floor(diff / 60_000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}小时前`
  const days = Math.floor(hours / 24)
  if (days < 7) return `${days}天前`
  if (days < 30) return `${Math.floor(days / 7)}周前`
  if (days < 365) return `${Math.floor(days / 30)}个月前`
  return `${Math.floor(days / 365)}年前`
}

// ── Number formatting ─────────────────────────────────────────────────────────

/**
 * Format a count/statistic for compact display.
 * - < 10,000 → "999"
 * - ≥ 10,000 → "1.2万"
 * - ≥ 100,000,000 → "1.2亿"
 */
export function formatCount(n: number | undefined | null): string {
  if (n == null || isNaN(n)) return '0'
  if (n < 10_000) return String(n)
  if (n < 100_000_000) return `${(n / 10_000).toFixed(1).replace(/\.0$/, '')}万`
  return `${(n / 100_000_000).toFixed(1).replace(/\.0$/, '')}亿`
}

// ── Text utilities ────────────────────────────────────────────────────────────

/**
 * Truncate text to `maxLen` characters, appending "…" if cut.
 */
export function truncateText(text: string, maxLen: number): string {
  if (!text) return ''
  return text.length > maxLen ? text.slice(0, maxLen) + '…' : text
}

// ── Domain-specific label helpers ─────────────────────────────────────────────

/** Novel status code → human-readable Chinese label. */
const NOVEL_STATUS_LABELS: Record<number, string> = { 0: '连载中', 1: '已完结', 2: '已暂停' }

/** Novel status code → Element-Plus tag type. */
const NOVEL_STATUS_TYPES: Record<number, 'success' | 'warning' | 'info' | 'danger' | ''> = {
  0: 'success',
  1: 'info',
  2: 'warning'
}

/** Post status code → human-readable Chinese label. */
const POST_STATUS_LABELS: Record<number, string> = { 0: '正常', 1: '已锁定' }

/** Map novel status code → human-readable Chinese label. */
export function novelStatusLabel(status: number): string {
  return NOVEL_STATUS_LABELS[status] ?? '未知'
}

/** Map novel status code → Element-Plus tag type. */
export function novelStatusType(
  status: number
): 'success' | 'warning' | 'info' | 'danger' | '' {
  return NOVEL_STATUS_TYPES[status] ?? ''
}

/** Map user status code → human-readable Chinese label. */
export function userStatusLabel(status: number | undefined): string {
  return status === 1 ? '已禁用' : '正常'
}

/** Map user status code → Element-Plus tag type. */
export function userStatusType(status: number | undefined): 'success' | 'danger' {
  return status === 1 ? 'danger' : 'success'
}

/** Map user role string → human-readable Chinese label. */
export function userRoleLabel(role: string | undefined): string {
  return role === 'ADMIN' ? '管理员' : '普通用户'
}

/** Map post status code → human-readable Chinese label. */
export function postStatusLabel(status: number): string {
  return POST_STATUS_LABELS[status] ?? '未知'
}
