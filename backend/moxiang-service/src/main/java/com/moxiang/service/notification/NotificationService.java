package com.moxiang.service.notification;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.mbg.entity.Notification;

/**
 * Notification service interface.
 */
public interface NotificationService {

    /** Create and persist a new notification. */
    void createNotification(Long userId, Long senderId, String type, Long relatedId, String content);

    /** Get paginated notifications for a user (newest first). */
    IPage<Notification> pageNotifications(Page<Notification> page, Long userId);

    /** Mark all unread notifications as read for the given user. */
    void markAllRead(Long userId);

    /** Mark a single notification as read. */
    void markRead(Long notificationId, Long userId);

    /** Get the unread notification count for the user. */
    long getUnreadCount(Long userId);
}
