package com.moxiang.mbg.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * User follow relationship entity.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("t_user_follow")
public class UserFollow {

    /** The user who follows */
    private Long followerId;

    /** The user being followed */
    private Long followingId;

    private LocalDateTime createdAt;
}
