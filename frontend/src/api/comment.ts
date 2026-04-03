import request from '@/utils/request'
import type { ApiResponse, PageResult } from '@/types/api'
import { CAPTCHA_TOKEN_HEADER } from '@/utils/captchaHeader'

export interface Comment {
  id: number
  postId: number
  userId: number
  parentId?: number
  content: string
  likeCount: number
  status: number
  createdAt: string
  updatedAt: string
}

/** 获取帖子评论列表（公开） */
export function getCommentsByPost(postId: number, current = 1, size = 20) {
  return request.get<ApiResponse<PageResult<Comment>>>(`/comment/post/${postId}`, {
    params: { current, size }
  })
}

/** 发表评论（需要登录） */
export function createComment(
  data: { postId: number; content: string; parentId?: number },
  captchaToken?: string
) {
  return request.post<ApiResponse<Comment>>('/comment', data, {
    headers: captchaToken ? { [CAPTCHA_TOKEN_HEADER]: captchaToken } : {}
  })
}

/** 删除评论（需要登录） */
export function deleteComment(id: number) {
  return request.delete<ApiResponse<null>>(`/comment/${id}`)
}

/** 点赞评论（需要登录） */
export function likeComment(id: number) {
  return request.post<ApiResponse<null>>(`/comment/${id}/like`)
}
