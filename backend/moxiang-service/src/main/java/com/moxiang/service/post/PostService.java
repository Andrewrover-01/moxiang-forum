package com.moxiang.service.post;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.mbg.entity.Post;

import java.util.List;

/**
 * Post service interface.
 */
public interface PostService {

    Post createPost(Long userId, Long forumId, String title, String content, List<Long> tagIds);

    Post getById(Long id);

    IPage<Post> pageByForum(Page<Post> page, Long forumId);

    IPage<Post> pageByUser(Page<Post> page, Long userId);

    IPage<Post> search(Page<Post> page, String keyword);

    void updatePost(Long postId, Long userId, String title, String content);

    void deletePost(Long postId, Long userId);

    /** Increment view count (Redis buffered) */
    void incrementViewCount(Long postId);

    /** Toggle like; returns true if liked, false if unliked */
    boolean toggleLike(Long postId, Long userId);

    boolean isLiked(Long postId, Long userId);

    /** Admin: pin/unpin */
    void setTop(Long postId, Integer isTop);

    /** Admin: feature/unfeature */
    void setFeatured(Long postId, Integer isFeatured);

    List<Post> getHotPosts(int limit);
}
