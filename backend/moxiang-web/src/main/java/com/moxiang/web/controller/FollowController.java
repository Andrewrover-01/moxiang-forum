package com.moxiang.web.controller;

import com.moxiang.common.api.CommonResult;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.service.follow.FollowService;
import com.moxiang.service.notification.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Follow controller — follow/unfollow users, query follow status and counts.
 */
@RestController
@RequestMapping("/api/follow")
public class FollowController {

    private final FollowService followService;
    private final NotificationService notificationService;

    public FollowController(FollowService followService, NotificationService notificationService) {
        this.followService = followService;
        this.notificationService = notificationService;
    }

    /** Toggle follow: follow if not following, unfollow if already following. */
    @PostMapping("/{userId}")
    public CommonResult<Map<String, Boolean>> toggleFollow(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        if (followService.isFollowing(currentUserId, userId)) {
            followService.unfollow(currentUserId, userId);
            return CommonResult.success(Map.of("following", false));
        } else {
            followService.follow(currentUserId, userId);
            notificationService.createNotification(
                    userId, currentUserId, "FOLLOW", null, "有人关注了你");
            return CommonResult.success(Map.of("following", true));
        }
    }

    /** Unfollow a user explicitly. */
    @DeleteMapping("/{userId}")
    public CommonResult<Void> unfollow(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        followService.unfollow(currentUserId, userId);
        return CommonResult.success();
    }

    /** Check whether the current user is following the target user. */
    @GetMapping("/{userId}/status")
    public CommonResult<Map<String, Boolean>> followStatus(@PathVariable Long userId) {
        Long currentUserId = getCurrentUserId();
        return CommonResult.success(Map.of("following", followService.isFollowing(currentUserId, userId)));
    }

    /** Get follower count and following count for any user. */
    @GetMapping("/stats/{userId}")
    public CommonResult<Map<String, Long>> followStats(@PathVariable Long userId) {
        long followers = followService.getFollowerCount(userId);
        long following = followService.getFollowingCount(userId);
        return CommonResult.success(Map.of("followers", followers, "following", following));
    }

    // ---- Helpers ----

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return (Long) auth.getCredentials();
    }
}
