package com.moxiang.service.post.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.constant.AuthConstants;
import com.moxiang.common.constant.ContentType;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.mbg.entity.Post;
import com.moxiang.mbg.entity.PostTag;
import com.moxiang.mbg.mapper.PostMapper;
import com.moxiang.mbg.mapper.PostTagMapper;
import com.moxiang.service.moderation.ContentModerationService;
import com.moxiang.service.notification.NotificationService;
import com.moxiang.service.post.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Post service implementation.
 */
@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements PostService {

    private static final int HOT_POSTS_CACHE_TTL_MINUTES = 10;

    private final RedisUtils redisUtils;
    private final PostTagMapper postTagMapper;
    private final NotificationService notificationService;
    private final ContentModerationService moderationService;

    public PostServiceImpl(RedisUtils redisUtils, PostTagMapper postTagMapper,
                           NotificationService notificationService,
                           ContentModerationService moderationService) {
        this.redisUtils = redisUtils;
        this.postTagMapper = postTagMapper;
        this.notificationService = notificationService;
        this.moderationService = moderationService;
    }

    @Override
    @Transactional
    public Post createPost(Long userId, Long forumId, String title, String content, List<Long> tagIds) {
        Post post = new Post()
                .setUserId(userId)
                .setForumId(forumId)
                .setTitle(title)
                .setContent(content)
                .setViewCount(0L)
                .setLikeCount(0L)
                .setCommentCount(0L)
                .setIsTop(0)
                .setIsFeatured(0)
                .setStatus(0);
        save(post);

        // Bind tags
        if (!CollectionUtils.isEmpty(tagIds)) {
            bindTags(post.getId(), tagIds);
        }

        // Machine review — flags and hides post if sensitive content detected
        moderationService.machineReview(ContentType.POST, post.getId(), userId, title, content);

        // Invalidate hot posts cache
        redisUtils.delete(AuthConstants.HOT_POST_CACHE_KEY);
        return post;
    }

    @Override
    public Post getById(Long id) {
        Post post = super.getById(id);
        if (post == null || post.getIsDeleted() == 1) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        return post;
    }

    @Override
    public IPage<Post> pageByForum(Page<Post> page, Long forumId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(forumId != null, Post::getForumId, forumId)
                .eq(Post::getStatus, 0)
                .orderByDesc(Post::getIsTop)
                .orderByDesc(Post::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public IPage<Post> pageByUser(Page<Post> page, Long userId) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getStatus, 0)
                .orderByDesc(Post::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public IPage<Post> search(Page<Post> page, String keyword) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 0)
                .and(StringUtils.hasText(keyword),
                        w -> w.like(Post::getTitle, keyword).or().like(Post::getContent, keyword))
                .orderByDesc(Post::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    @Transactional
    public void updatePost(Long postId, Long userId, String title, String content) {
        Post post = requirePost(postId);
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        post.setTitle(title).setContent(content);
        updateById(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = requirePost(postId);
        if (!post.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        removeById(postId);
        redisUtils.delete(AuthConstants.HOT_POST_CACHE_KEY);
    }

    @Override
    public void incrementViewCount(Long postId) {
        // Buffer in Redis; a scheduled job can periodically flush to DB
        String key = AuthConstants.POST_VIEW_COUNT_PREFIX + postId;
        redisUtils.increment(key, 1L);
        // Also update DB directly for simplicity
        baseMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Post>()
                        .eq(Post::getId, postId)
                        .setSql("view_count = view_count + 1"));
    }

    @Override
    public boolean toggleLike(Long postId, Long userId) {
        String key = AuthConstants.POST_LIKE_PREFIX + postId;
        String member = String.valueOf(userId);
        if (redisUtils.setIsMember(key, member)) {
            // Unlike
            redisUtils.setRemove(key, member);
            baseMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Post>()
                            .eq(Post::getId, postId)
                            .setSql("like_count = GREATEST(like_count - 1, 0)"));
            return false;
        } else {
            // Like
            redisUtils.setAdd(key, member);
            redisUtils.expire(key, 30, TimeUnit.DAYS);
            baseMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Post>()
                            .eq(Post::getId, postId)
                            .setSql("like_count = like_count + 1"));
            // Notify post author
            Post post = super.getById(postId);
            if (post != null) {
                notificationService.createNotification(
                        post.getUserId(), userId, "LIKE_POST", postId, "有人点赞了你的帖子");
            }
            return true;
        }
    }

    @Override
    public boolean isLiked(Long postId, Long userId) {
        String key = AuthConstants.POST_LIKE_PREFIX + postId;
        return redisUtils.setIsMember(key, String.valueOf(userId));
    }

    @Override
    public void setTop(Long postId, Integer isTop) {
        Post post = requirePost(postId);
        post.setIsTop(isTop);
        updateById(post);
    }

    @Override
    public void setFeatured(Long postId, Integer isFeatured) {
        Post post = requirePost(postId);
        post.setIsFeatured(isFeatured);
        updateById(post);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Post> getHotPosts(int limit) {
        Object cached = redisUtils.get(AuthConstants.HOT_POST_CACHE_KEY);
        if (cached instanceof List) {
            return (List<Post>) cached;
        }
        List<Post> hotPosts = baseMapper.selectHotPosts(limit);
        redisUtils.set(AuthConstants.HOT_POST_CACHE_KEY, hotPosts, HOT_POSTS_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return hotPosts;
    }

    // ---- Helpers ----

    private Post requirePost(Long postId) {
        Post post = super.getById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.POST_NOT_FOUND);
        }
        return post;
    }

    private void bindTags(Long postId, List<Long> tagIds) {
        for (Long tagId : tagIds) {
            PostTag pt = new PostTag().setPostId(postId).setTagId(tagId);
            postTagMapper.insert(pt);
        }
    }
}
