package com.moxiang.mbg.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.moxiang.mbg.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Post mapper.
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {

    @Select("SELECT p.* FROM t_post p WHERE p.is_deleted = 0 AND p.status = 0 ORDER BY p.view_count DESC LIMIT #{limit}")
    List<Post> selectHotPosts(@Param("limit") int limit);

    @Select("SELECT p.* FROM t_post p WHERE p.is_deleted = 0 AND p.forum_id = #{forumId} " +
            "AND (p.title LIKE CONCAT('%', #{keyword}, '%') OR p.content LIKE CONCAT('%', #{keyword}, '%'))")
    List<Post> searchByKeyword(@Param("forumId") Long forumId, @Param("keyword") String keyword);
}
