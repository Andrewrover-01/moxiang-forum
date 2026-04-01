package com.moxiang.service.favorite.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moxiang.common.constant.AuthConstants;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.mbg.entity.Post;
import com.moxiang.mbg.entity.UserFavoritePost;
import com.moxiang.mbg.mapper.PostMapper;
import com.moxiang.mbg.mapper.UserFavoritePostMapper;
import com.moxiang.service.favorite.FavoriteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Post favorite service implementation.
 */
@Service
public class FavoriteServiceImpl extends ServiceImpl<UserFavoritePostMapper, UserFavoritePost> implements FavoriteService {

    private final PostMapper postMapper;
    private final RedisUtils redisUtils;

    public FavoriteServiceImpl(PostMapper postMapper, RedisUtils redisUtils) {
        this.postMapper = postMapper;
        this.redisUtils = redisUtils;
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long postId, Long userId) {
        String key = AuthConstants.POST_FAVORITE_PREFIX + postId;
        String member = String.valueOf(userId);
        if (redisUtils.setIsMember(key, member)) {
            // Un-favorite
            redisUtils.setRemove(key, member);
            remove(new LambdaQueryWrapper<UserFavoritePost>()
                    .eq(UserFavoritePost::getUserId, userId)
                    .eq(UserFavoritePost::getPostId, postId));
            return false;
        } else {
            // Favorite
            redisUtils.setAdd(key, member);
            redisUtils.expire(key, 30, TimeUnit.DAYS);
            UserFavoritePost record = new UserFavoritePost()
                    .setUserId(userId)
                    .setPostId(postId);
            save(record);
            return true;
        }
    }

    @Override
    public boolean isFavorited(Long postId, Long userId) {
        String key = AuthConstants.POST_FAVORITE_PREFIX + postId;
        return redisUtils.setIsMember(key, String.valueOf(userId));
    }

    @Override
    public IPage<Post> pageFavoritePosts(Page<Post> page, Long userId) {
        // Fetch favorite post IDs for the user (ordered by creation time desc)
        List<UserFavoritePost> favorites = list(
                new LambdaQueryWrapper<UserFavoritePost>()
                        .eq(UserFavoritePost::getUserId, userId)
                        .orderByDesc(UserFavoritePost::getCreatedAt)
        );
        if (favorites.isEmpty()) {
            return page;
        }
        List<Long> postIds = favorites.stream()
                .map(UserFavoritePost::getPostId)
                .toList();
        return postMapper.selectPage(page,
                new LambdaQueryWrapper<Post>()
                        .in(Post::getId, postIds)
                        .eq(Post::getStatus, 0));
    }
}
