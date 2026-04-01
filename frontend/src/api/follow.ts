/**
 * Follow API module — wraps all /api/follow/* endpoints.
 */
import http from './http'

export interface FollowStats {
  followerCount: number
  followingCount: number
}

/** Toggle follow/unfollow a user; returns whether the current user is now following. */
export function toggleFollow(userId: number | string): Promise<{ following: boolean }> {
  return http
    .post<{ data: { following: boolean } }>(`/follow/${userId}`)
    .then((res) => res.data.data)
}

/** Explicitly unfollow a user. */
export function unfollowUser(userId: number | string): Promise<void> {
  return http.delete(`/follow/${userId}`).then(() => undefined)
}

/** Check whether the current user is following a given user. */
export function isFollowing(userId: number | string): Promise<boolean> {
  return http
    .get<{ data: boolean }>(`/follow/${userId}/status`)
    .then((res) => res.data.data)
}

/** Get follower / following counts for a user (public, no auth required). */
export function getFollowStats(userId: number | string): Promise<FollowStats> {
  return http
    .get<{ data: FollowStats }>(`/follow/stats/${userId}`)
    .then((res) => res.data.data)
}
