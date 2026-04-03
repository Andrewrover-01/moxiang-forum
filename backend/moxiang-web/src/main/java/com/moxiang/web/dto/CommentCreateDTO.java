package com.moxiang.web.dto;

import com.moxiang.common.annotation.Sanitize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Create comment request DTO.
 */
@Data
public class CommentCreateDTO {

    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    /** Optional: ID of the comment being replied to */
    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 5000, message = "评论最多5000个字符")
    @Sanitize
    private String content;
}
