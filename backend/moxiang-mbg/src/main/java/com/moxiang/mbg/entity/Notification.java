package com.moxiang.mbg.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * In-app notification entity.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("t_notification")
public class Notification {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** The user who receives this notification */
    private Long userId;

    /** The user who triggered this notification (null for system notifications) */
    private Long senderId;

    /**
     * Notification type:
     * LIKE_POST     — someone liked your post
     * COMMENT_POST  — someone commented on your post
     * REPLY_COMMENT — someone replied to your comment
     * FOLLOW        — someone followed you
     */
    private String type;

    /** Related resource ID (post ID, comment ID, etc.) */
    private Long relatedId;

    /** Human-readable notification text */
    private String content;

    /** 0=unread, 1=read */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
