package com.moxiang.service.comment;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.mbg.entity.Comment;

/**
 * Comment service interface.
 */
public interface CommentService {

    Comment createComment(Long postId, Long userId, Long parentId, String content);

    IPage<Comment> pageByPost(Page<Comment> page, Long postId);

    IPage<Comment> pageReplies(Page<Comment> page, Long parentId);

    void deleteComment(Long commentId, Long userId);

    boolean toggleLike(Long commentId, Long userId);

    /** Admin: hide/show comment */
    void updateStatus(Long commentId, Integer status);
}
