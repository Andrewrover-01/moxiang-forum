package com.moxiang.web.dto;

import com.moxiang.common.annotation.Sanitize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Create novel request DTO.
 */
@Data
public class NovelCreateDTO {

    @NotBlank(message = "小说标题不能为空")
    @Size(max = 100, message = "标题最多100个字符")
    @Sanitize
    private String title;

    @Size(max = 2000, message = "简介最多2000个字符")
    @Sanitize
    private String description;

    private String cover;

    @NotBlank(message = "分类不能为空")
    private String category;
}
