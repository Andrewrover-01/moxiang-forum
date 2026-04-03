package com.moxiang.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.common.annotation.RateLimit;
import com.moxiang.common.annotation.RateLimitType;
import com.moxiang.common.api.CommonResult;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.constant.RateLimitConstants;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.mbg.entity.Comment;
import com.moxiang.service.comment.CommentService;
import com.moxiang.web.dto.CommentCreateDTO;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Comment controller — CRUD, replies, likes.
 */
@RestController
@RequestMapping("/api/comment")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @RateLimit(key = RateLimitConstants.RL_COMMENT_CREATE,
               limit = RateLimitConstants.COMMENT_CREATE_LIMIT,
               period = 3600L,
               limitBy = RateLimitType.USER,
               message = "评论过于频繁，每小时最多评论30次")
    public CommonResult<Comment> createComment(@Valid @RequestBody CommentCreateDTO dto) {
        Long userId = getCurrentUserId();
        Comment comment = commentService.createComment(
                dto.getPostId(), userId, dto.getParentId(), dto.getContent());
        return CommonResult.success(comment);
    }

    @GetMapping("/post/{postId}")
    public CommonResult<?> listByPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return CommonResult.success(commentService.pageByPost(new Page<>(current, size), postId));
    }

    @GetMapping("/replies/{parentId}")
    public CommonResult<?> listReplies(
            @PathVariable Long parentId,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return CommonResult.success(commentService.pageReplies(new Page<>(current, size), parentId));
    }

    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteComment(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        commentService.deleteComment(id, userId);
        return CommonResult.success();
    }

    @PostMapping("/{id}/like")
    @RateLimit(key = RateLimitConstants.RL_COMMENT_LIKE,
               limit = RateLimitConstants.COMMENT_LIKE_LIMIT,
               period = 86400L,
               limitBy = RateLimitType.USER,
               message = "点赞过于频繁，每天最多点赞100次")
    public CommonResult<Map<String, Boolean>> toggleLike(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        boolean liked = commentService.toggleLike(id, userId);
        return CommonResult.success(Map.of("liked", liked));
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
