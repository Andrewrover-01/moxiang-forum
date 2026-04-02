import request from '@/utils/request'
import type { ApiResponse, Post, PageResult } from '@/types/api'

/** 帖子列表（公开，支持按版块过滤） */
export function getPostList(params: { forumId?: number; current?: number; size?: number } = {}) {
  return request.get<ApiResponse<PageResult<Post>>>('/post/list', { params })
}

/** 获取帖子详情 */
export function getPostById(id: number) {
  return request.get<ApiResponse<Post>>(`/post/${id}`)
}

/** 发布帖子（需要登录） */
export function createPost(data: { forumId: number; title: string; content: string }) {
  return request.post<ApiResponse<Post>>('/post', data)
}

/** 更新帖子（需要登录） */
export function updatePost(id: number, data: { title?: string; content?: string }) {
  return request.put<ApiResponse<Post>>(`/post/${id}`, data)
}

/** 删除帖子（需要登录） */
export function deletePost(id: number) {
  return request.delete<ApiResponse<null>>(`/post/${id}`)
}

/** 点赞帖子（需要登录） */
export function likePost(id: number) {
  return request.post<ApiResponse<null>>(`/post/${id}/like`)
}
