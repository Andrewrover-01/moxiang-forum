package com.moxiang.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.common.annotation.RateLimit;
import com.moxiang.common.annotation.RateLimitType;
import com.moxiang.common.api.CommonResult;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.constant.RateLimitConstants;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.common.utils.WebUtils;
import com.moxiang.mbg.entity.Post;
import com.moxiang.service.post.PostService;
import com.moxiang.web.dto.PostCreateDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Post controller — CRUD, search, like, and hot posts.
 */
@RestController
@RequestMapping("/api/post")
public class PostController {

    private final PostService postService;
    private final RedisUtils redisUtils;

    public PostController(PostService postService, RedisUtils redisUtils) {
        this.postService = postService;
        this.redisUtils = redisUtils;
    }

    @PostMapping
    @RateLimit(key = RateLimitConstants.RL_POST_CREATE,
               limit = RateLimitConstants.POST_CREATE_LIMIT,
               period = 3600L,
               limitBy = RateLimitType.USER,
               message = "发帖过于频繁，每小时最多发 " + RateLimitConstants.POST_CREATE_LIMIT + " 篇帖子")
    public CommonResult<Post> createPost(@Valid @RequestBody PostCreateDTO dto) {
        Long userId = getCurrentUserId();
        Post post = postService.createPost(userId, dto.getForumId(), dto.getTitle(),
                dto.getContent(), dto.getTagIds());
        return CommonResult.success(post);
    }

    @GetMapping("/{id}")
    public CommonResult<Post> getPost(@PathVariable Long id, HttpServletRequest request) {
        Post post = postService.getById(id);
        // Deduplicate view-count increments: only count one view per IP per post per window
        String clientIp = WebUtils.getClientIp(request);
        String viewKey = RateLimitConstants.RL_VIEW_COUNT + id + ":" + clientIp;
        if (!redisUtils.hasKey(viewKey)) {
            redisUtils.set(viewKey, "1", RateLimitConstants.VIEW_DEDUP_WINDOW_SECONDS,
                    java.util.concurrent.TimeUnit.SECONDS);
            postService.incrementViewCount(id);
        }
        return CommonResult.success(post);
    }

    @GetMapping("/list")
    public CommonResult<?> listPosts(
            @RequestParam(required = false) Long forumId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return CommonResult.success(postService.pageByForum(new Page<>(current, size), forumId));
    }

    @GetMapping("/user/{userId}")
    public CommonResult<?> listUserPosts(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return CommonResult.success(postService.pageByUser(new Page<>(current, size), userId));
    }

    @GetMapping("/search")
    public CommonResult<?> searchPosts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return CommonResult.success(postService.search(new Page<>(current, size), keyword));
    }

    @GetMapping("/hot")
    public CommonResult<List<Post>> hotPosts(@RequestParam(defaultValue = "10") int limit) {
        return CommonResult.success(postService.getHotPosts(limit));
    }

    @PutMapping("/{id}")
    public CommonResult<Void> updatePost(@PathVariable Long id,
                                         @RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        postService.updatePost(id, userId, body.get("title"), body.get("content"));
        return CommonResult.success();
    }

    @DeleteMapping("/{id}")
    public CommonResult<Void> deletePost(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        postService.deletePost(id, userId);
        return CommonResult.success();
    }

    @PostMapping("/{id}/like")
    @RateLimit(key = RateLimitConstants.RL_POST_LIKE,
               limit = RateLimitConstants.POST_LIKE_LIMIT,
               period = 86400L,
               limitBy = RateLimitType.USER,
               message = "点赞过于频繁，每天最多点赞 " + RateLimitConstants.POST_LIKE_LIMIT + " 次")
    public CommonResult<Map<String, Boolean>> toggleLike(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean liked = postService.toggleLike(id, userId);
        return CommonResult.success(Map.of("liked", liked));
    }

    @GetMapping("/{id}/like/status")
    public CommonResult<Map<String, Boolean>> likeStatus(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return CommonResult.success(Map.of("liked", postService.isLiked(id, userId)));
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
