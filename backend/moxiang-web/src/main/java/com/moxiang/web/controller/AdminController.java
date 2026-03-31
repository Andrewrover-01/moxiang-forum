package com.moxiang.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.common.api.CommonResult;
import com.moxiang.mbg.entity.Forum;
import com.moxiang.service.forum.ForumService;
import com.moxiang.service.post.PostService;
import com.moxiang.service.user.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin controller — privileged management endpoints (ADMIN role required).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ForumService forumService;
    private final PostService postService;
    private final UserService userService;

    public AdminController(ForumService forumService,
                           PostService postService,
                           UserService userService) {
        this.forumService = forumService;
        this.postService = postService;
        this.userService = userService;
    }

    // ---- Forum management ----

    @PostMapping("/forum")
    public CommonResult<Forum> createForum(@RequestBody Map<String, Object> body) {
        Forum forum = forumService.createForum(
                (String) body.get("name"),
                (String) body.get("description"),
                (String) body.get("icon"),
                body.get("sortOrder") != null ? (Integer) body.get("sortOrder") : 0);
        return CommonResult.success(forum);
    }

    @PutMapping("/forum/{id}")
    public CommonResult<Void> updateForum(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        forumService.updateForum(id,
                (String) body.get("name"),
                (String) body.get("description"),
                (String) body.get("icon"),
                body.get("sortOrder") != null ? (Integer) body.get("sortOrder") : 0);
        return CommonResult.success();
    }

    @DeleteMapping("/forum/{id}")
    public CommonResult<Void> deleteForum(@PathVariable Long id) {
        forumService.deleteForum(id);
        return CommonResult.success();
    }

    @PutMapping("/forum/{id}/status")
    public CommonResult<Void> updateForumStatus(@PathVariable Long id,
                                                 @RequestBody Map<String, Integer> body) {
        forumService.updateStatus(id, body.get("status"));
        return CommonResult.success();
    }

    // ---- Post management ----

    @PutMapping("/post/{id}/top")
    public CommonResult<Void> setPostTop(@PathVariable Long id,
                                          @RequestBody Map<String, Integer> body) {
        postService.setTop(id, body.get("isTop"));
        return CommonResult.success();
    }

    @PutMapping("/post/{id}/featured")
    public CommonResult<Void> setPostFeatured(@PathVariable Long id,
                                               @RequestBody Map<String, Integer> body) {
        postService.setFeatured(id, body.get("isFeatured"));
        return CommonResult.success();
    }

    // ---- User management ----

    @GetMapping("/user/list")
    public CommonResult<?> listUsers(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        return CommonResult.success(userService.pageUsers(new Page<>(current, size), keyword));
    }

    @PutMapping("/user/{id}/status")
    public CommonResult<Void> updateUserStatus(@PathVariable Long id,
                                                @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return CommonResult.success();
    }

    @DeleteMapping("/user/{id}")
    public CommonResult<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return CommonResult.success();
    }
}
