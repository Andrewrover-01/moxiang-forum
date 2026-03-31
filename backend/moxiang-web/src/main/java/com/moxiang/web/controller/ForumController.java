package com.moxiang.web.controller;

import com.moxiang.common.api.CommonResult;
import com.moxiang.mbg.entity.Forum;
import com.moxiang.service.forum.ForumService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Forum controller — public listing and retrieval.
 */
@RestController
@RequestMapping("/api/forum")
public class ForumController {

    private final ForumService forumService;

    public ForumController(ForumService forumService) {
        this.forumService = forumService;
    }

    @GetMapping("/list")
    public CommonResult<List<Forum>> listForums() {
        return CommonResult.success(forumService.listAll());
    }

    @GetMapping("/{id}")
    public CommonResult<Forum> getForum(@PathVariable Long id) {
        return CommonResult.success(forumService.getById(id));
    }

    @GetMapping("/page")
    public CommonResult<?> pageForums(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return CommonResult.success(forumService.pageForums(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size)));
    }
}
