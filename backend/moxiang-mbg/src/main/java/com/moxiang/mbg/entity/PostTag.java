package com.moxiang.mbg.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * Many-to-many mapping between posts and tags.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("t_post_tag")
public class PostTag {

    private Long postId;

    private Long tagId;
}
