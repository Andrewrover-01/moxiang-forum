package com.moxiang.mbg.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Post tag label entity.
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName("t_tag")
public class Tag {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String color;

    private Long useCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
