package com.moxiang.service.follow;

/**
 * User follow service interface.
 */
public interface FollowService {

    /**
     * Follow a user. Returns true if now following, false if already was following.
     * Throws BusinessException if trying to follow self.
     */
    boolean follow(Long followerId, Long followingId);

    /** Unfollow a user. */
    void unfollow(Long followerId, Long followingId);

    /** Check whether follower is following the target user. */
    boolean isFollowing(Long followerId, Long followingId);

    /** Get the number of users that userId is following. */
    long getFollowingCount(Long userId);

    /** Get the number of followers for userId. */
    long getFollowerCount(Long userId);
}
