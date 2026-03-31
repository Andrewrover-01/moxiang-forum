/**
 * Shared entity interfaces and common response types.
 * All types mirror the Java entity / DTO shapes returned by the backend.
 */

// ── Generic page result (MyBatis-Plus IPage) ─────────────────────────────────

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// ── Entity interfaces ─────────────────────────────────────────────────────────

export interface UserInfo {
  id: number
  username: string
  /** Present for own profile; stripped from public user lookups */
  email?: string
  avatar?: string
  bio?: string
  role: 'USER' | 'ADMIN'
  /** 0 = normal, 1 = disabled */
  status?: number
  createdAt?: string
  updatedAt?: string
}

export interface Forum {
  id: number
  name: string
  description: string
  icon?: string
  sortOrder?: number
  postCount: number
  /** 0 = normal, 1 = disabled */
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface Post {
  id: number
  forumId: number
  userId: number
  title: string
  content: string
  viewCount: number
  likeCount: number
  commentCount: number
  /** 0 = normal, 1 = pinned */
  isTop: number
  /** 0 = normal, 1 = featured */
  isFeatured: number
  /** 0 = normal, 1 = locked */
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface Novel {
  id: number
  userId: number
  title: string
  description?: string
  cover?: string
  category: string
  wordCount?: number
  chapterCount?: number
  viewCount?: number
  collectCount?: number
  /** 0 = serializing, 1 = completed, 2 = suspended */
  status: number
  createdAt?: string
  updatedAt?: string
}

export interface NovelChapter {
  id: number
  novelId: number
  chapterNumber: number
  title: string
  content: string
  wordCount?: number
  createdAt?: string
  updatedAt?: string
}

export interface Comment {
  id: number
  postId: number
  userId: number
  /** null for top-level comments */
  parentId?: number
  content: string
  likeCount: number
  /** 0 = normal, 1 = hidden */
  status: number
  createdAt?: string
  updatedAt?: string
}

// ── Form / request payload types (client-side input shapes) ──────────────────

/** Login form fields. */
export interface LoginForm {
  username: string
  password: string
}

/** Registration form fields (includes confirmPassword for UI validation). */
export interface RegisterForm {
  username: string
  password: string
  confirmPassword: string
  email: string
}

/** Update profile form fields. */
export interface UpdateProfileForm {
  avatar: string
  bio: string
}

/** Change-password form fields. */
export interface ChangePasswordForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

/** Create / edit post form fields. */
export interface PostCreateForm {
  forumId: number | undefined
  title: string
  content: string
  tagIds?: number[]
}

/** Create comment (or reply) form fields. */
export interface CommentCreateForm {
  postId: number
  content: string
  parentId?: number
}

/** Create / edit novel form fields. */
export interface NovelCreateForm {
  title: string
  description: string
  cover: string
  category: string
}

/** Create / edit chapter form fields. */
export interface NovelChapterForm {
  title: string
  content: string
}

/** Common pagination query parameters. */
export interface PaginationParams {
  current?: number
  size?: number
}
