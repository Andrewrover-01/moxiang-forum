package com.moxiang.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.common.api.CommonResult;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.mbg.entity.Notification;
import com.moxiang.service.notification.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Notification controller — list, mark read, unread count.
 */
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Get paginated notifications for the current user. */
    @GetMapping("/list")
    public CommonResult<?> listNotifications(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        Long userId = getCurrentUserId();
        return CommonResult.success(
                notificationService.pageNotifications(new Page<>(current, size), userId));
    }

    /** Get the unread notification count for the current user. */
    @GetMapping("/unread/count")
    public CommonResult<Map<String, Long>> unreadCount() {
        Long userId = getCurrentUserId();
        return CommonResult.success(Map.of("count", notificationService.getUnreadCount(userId)));
    }

    /** Mark all notifications as read. */
    @PutMapping("/read/all")
    public CommonResult<Void> markAllRead() {
        Long userId = getCurrentUserId();
        notificationService.markAllRead(userId);
        return CommonResult.success();
    }

    /** Mark a single notification as read. */
    @PutMapping("/read/{id}")
    public CommonResult<Void> markRead(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        notificationService.markRead(id, userId);
        return CommonResult.success();
    }

    // ---- Helpers ----

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return (Long) auth.getCredentials();
    }
}
