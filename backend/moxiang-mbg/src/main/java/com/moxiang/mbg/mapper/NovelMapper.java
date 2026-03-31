package com.moxiang.mbg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxiang.mbg.entity.Novel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Novel mapper.
 */
@Mapper
public interface NovelMapper extends BaseMapper<Novel> {

    @Select("SELECT * FROM t_novel WHERE is_deleted = 0 " +
            "AND (title LIKE CONCAT('%', #{keyword}, '%') OR description LIKE CONCAT('%', #{keyword}, '%'))")
    List<Novel> searchByKeyword(@Param("keyword") String keyword);
}
