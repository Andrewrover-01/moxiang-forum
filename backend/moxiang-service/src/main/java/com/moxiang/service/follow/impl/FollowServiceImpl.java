package com.moxiang.service.follow.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.mbg.entity.UserFollow;
import com.moxiang.mbg.mapper.UserFollowMapper;
import com.moxiang.service.follow.FollowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Follow service implementation.
 */
@Service
public class FollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements FollowService {

    @Override
    @Transactional
    public boolean follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(ResultCode.CANNOT_FOLLOW_SELF);
        }
        if (isFollowing(followerId, followingId)) {
            return false;
        }
        UserFollow follow = new UserFollow()
                .setFollowerId(followerId)
                .setFollowingId(followingId);
        save(follow);
        return true;
    }

    @Override
    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        remove(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowingId, followingId));
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        return count(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFollowingId, followingId)) > 0;
    }

    @Override
    public long getFollowingCount(Long userId) {
        return count(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId));
    }

    @Override
    public long getFollowerCount(Long userId) {
        return count(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowingId, userId));
    }
}
