package com.moxiang.service.comment.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.constant.AuthConstants;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.mbg.entity.Comment;
import com.moxiang.mbg.entity.Post;
import com.moxiang.mbg.mapper.CommentMapper;
import com.moxiang.mbg.mapper.PostMapper;
import com.moxiang.service.comment.CommentService;
import com.moxiang.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Comment service implementation.
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    private final PostMapper postMapper;
    private final RedisUtils redisUtils;
    private final NotificationService notificationService;

    public CommentServiceImpl(PostMapper postMapper, RedisUtils redisUtils,
                              NotificationService notificationService) {
        this.postMapper = postMapper;
        this.redisUtils = redisUtils;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Comment createComment(Long postId, Long userId, Long parentId, String content) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }

        Comment comment = new Comment()
                .setPostId(postId)
                .setUserId(userId)
                .setParentId(parentId)
                .setContent(content)
                .setLikeCount(0L)
                .setStatus(0);
        save(comment);

        // Increment post comment count
        postMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Post>()
                        .eq(Post::getId, postId)
                        .setSql("comment_count = comment_count + 1"));

        // Send notification to the appropriate recipient
        if (parentId == null) {
            // Top-level comment: notify the post author
            notificationService.createNotification(
                    post.getUserId(), userId, "COMMENT_POST", postId, "有人评论了你的帖子");
        } else {
            // Reply: notify the parent comment's author
            Comment parent = super.getById(parentId);
            if (parent != null) {
                notificationService.createNotification(
                        parent.getUserId(), userId, "REPLY_COMMENT", comment.getId(), "有人回复了你的评论");
            }
        }

        return comment;
    }

    @Override
    public IPage<Comment> pageByPost(Page<Comment> page, Long postId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .isNull(Comment::getParentId)
                .eq(Comment::getStatus, 0)
                .orderByAsc(Comment::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public IPage<Comment> pageReplies(Page<Comment> page, Long parentId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getParentId, parentId)
                .eq(Comment::getStatus, 0)
                .orderByAsc(Comment::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = requireComment(commentId);
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        removeById(commentId);

        // Decrement post comment count
        postMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Post>()
                        .eq(Post::getId, comment.getPostId())
                        .setSql("comment_count = GREATEST(comment_count - 1, 0)"));
    }

    @Override
    public boolean toggleLike(Long commentId, Long userId) {
        String key = "comment:like:" + commentId;
        String member = String.valueOf(userId);
        if (redisUtils.setIsMember(key, member)) {
            redisUtils.setRemove(key, member);
            baseMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Comment>()
                            .eq(Comment::getId, commentId)
                            .setSql("like_count = GREATEST(like_count - 1, 0)"));
            return false;
        } else {
            redisUtils.setAdd(key, member);
            redisUtils.expire(key, 30, TimeUnit.DAYS);
            baseMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Comment>()
                            .eq(Comment::getId, commentId)
                            .setSql("like_count = like_count + 1"));
            return true;
        }
    }

    @Override
    public void updateStatus(Long commentId, Integer status) {
        Comment comment = requireComment(commentId);
        comment.setStatus(status);
        updateById(comment);
    }

    // ---- Helper ----

    private Comment requireComment(Long commentId) {
        Comment comment = super.getById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }
}
