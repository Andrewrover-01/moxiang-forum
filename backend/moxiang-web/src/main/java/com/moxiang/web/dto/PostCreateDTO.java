package com.moxiang.web.dto;

import com.moxiang.common.annotation.Sanitize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Create post request DTO.
 */
@Data
public class PostCreateDTO {

    @NotNull(message = "所属版块不能为空")
    private Long forumId;

    @NotBlank(message = "帖子标题不能为空")
    @Size(max = 100, message = "标题最多100个字符")
    @Sanitize
    private String title;

    @NotBlank(message = "帖子内容不能为空")
    @Size(max = 50000, message = "内容最多50000个字符")
    @Sanitize
    private String content;

    /** Optional tag IDs to attach */
    private List<Long> tagIds;
}
