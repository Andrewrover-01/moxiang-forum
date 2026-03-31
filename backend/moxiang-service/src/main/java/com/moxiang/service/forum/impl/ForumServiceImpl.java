package com.moxiang.service.forum.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.mbg.entity.Forum;
import com.moxiang.mbg.mapper.ForumMapper;
import com.moxiang.service.forum.ForumService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Forum service implementation.
 */
@Service
public class ForumServiceImpl extends ServiceImpl<ForumMapper, Forum> implements ForumService {

    @Override
    public List<Forum> listAll() {
        return list(new LambdaQueryWrapper<Forum>()
                .eq(Forum::getStatus, 0)
                .orderByAsc(Forum::getSortOrder));
    }

    @Override
    public Forum getById(Long id) {
        Forum forum = super.getById(id);
        if (forum == null) {
            throw new BusinessException(ResultCode.FORUM_NOT_FOUND);
        }
        return forum;
    }

    @Override
    public IPage<Forum> pageForums(Page<Forum> page) {
        return page(page, new LambdaQueryWrapper<Forum>().orderByAsc(Forum::getSortOrder));
    }

    @Override
    public Forum createForum(String name, String description, String icon, Integer sortOrder) {
        Forum forum = new Forum()
                .setName(name)
                .setDescription(description)
                .setIcon(icon)
                .setSortOrder(sortOrder)
                .setPostCount(0L)
                .setStatus(0);
        save(forum);
        return forum;
    }

    @Override
    public void updateForum(Long id, String name, String description, String icon, Integer sortOrder) {
        Forum forum = getById(id);
        forum.setName(name)
             .setDescription(description)
             .setIcon(icon)
             .setSortOrder(sortOrder);
        updateById(forum);
    }

    @Override
    public void deleteForum(Long id) {
        getById(id); // throws if not found
        removeById(id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Forum forum = getById(id);
        forum.setStatus(status);
        updateById(forum);
    }
}
