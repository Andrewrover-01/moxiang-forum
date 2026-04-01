package com.moxiang.mbg.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * User favorite post entity.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("t_user_favorite_post")
public class UserFavoritePost {

    private Long userId;

    private Long postId;

    private LocalDateTime createdAt;
}
