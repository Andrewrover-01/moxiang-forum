import request from '@/utils/request'
import type { ApiResponse, Forum } from '@/types/api'

/** 获取全部版块列表（公开） */
export function getForumList() {
  return request.get<ApiResponse<Forum[]>>('/forum/list')
}

/** 获取单个版块详情（公开） */
export function getForumById(id: number) {
  return request.get<ApiResponse<Forum>>(`/forum/${id}`)
}
