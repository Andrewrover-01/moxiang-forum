package com.moxiang.service.forum;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.mbg.entity.Forum;

import java.util.List;

/**
 * Forum service interface.
 */
public interface ForumService {

    List<Forum> listAll();

    Forum getById(Long id);

    IPage<Forum> pageForums(Page<Forum> page);

    Forum createForum(String name, String description, String icon, Integer sortOrder);

    void updateForum(Long id, String name, String description, String icon, Integer sortOrder);

    void deleteForum(Long id);

    void updateStatus(Long id, Integer status);
}
