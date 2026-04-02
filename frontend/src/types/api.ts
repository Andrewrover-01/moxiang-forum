/** API 统一响应结构 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** MyBatis-Plus 分页响应 */
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 论坛版块 */
export interface Forum {
  id: number
  name: string
  description: string
  icon: string
  sortOrder: number
  postCount: number
  status: number
  createdAt: string
  updatedAt: string
}

/** 帖子 */
export interface Post {
  id: number
  forumId: number
  userId: number
  title: string
  content: string
  viewCount: number
  likeCount: number
  commentCount: number
  isTop: number
  isFeatured: number
  status: number
  createdAt: string
  updatedAt: string
}

/** 用户 */
export interface User {
  id: number
  username: string
  email?: string
  avatar?: string
  bio?: string
  role: string
  status: number
  createdAt: string
  updatedAt: string
}

/** 登录响应 */
export interface LoginResult {
  token: string
}
