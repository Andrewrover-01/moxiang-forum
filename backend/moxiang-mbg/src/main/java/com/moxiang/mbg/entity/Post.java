package com.moxiang.mbg.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Forum post entity.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("t_post")
public class Post {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long forumId;

    private Long userId;

    private String title;

    private String content;

    private Long viewCount;

    private Long likeCount;

    private Long commentCount;

    /** 0=normal, 1=pinned */
    private Integer isTop;

    /** 0=normal, 1=featured */
    private Integer isFeatured;

    /** 0=normal, 1=locked (soft deletion tracked via isDeleted field) */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer isDeleted;
}
