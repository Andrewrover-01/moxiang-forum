package com.moxiang.service.favorite;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.mbg.entity.Post;

/**
 * Post favorite (bookmark) service interface.
 */
public interface FavoriteService {

    /**
     * Toggle favorite on a post.
     * Returns true if the post is now favorited, false if it was un-favorited.
     */
    boolean toggleFavorite(Long postId, Long userId);

    /** Check whether the user has favorited the given post. */
    boolean isFavorited(Long postId, Long userId);

    /** Get a paginated list of posts favorited by the user. */
    IPage<Post> pageFavoritePosts(Page<Post> page, Long userId);
}
