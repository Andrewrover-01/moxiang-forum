package com.moxiang.service.notification.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.constant.AuthConstants;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.mbg.entity.Notification;
import com.moxiang.mbg.mapper.NotificationMapper;
import com.moxiang.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notification service implementation.
 */
@Service
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements NotificationService {

    private final RedisUtils redisUtils;

    public NotificationServiceImpl(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    @Override
    @Transactional
    public void createNotification(Long userId, Long senderId, String type, Long relatedId, String content) {
        // Do not notify yourself
        if (userId.equals(senderId)) {
            return;
        }
        Notification notification = new Notification()
                .setUserId(userId)
                .setSenderId(senderId)
                .setType(type)
                .setRelatedId(relatedId)
                .setContent(content)
                .setIsRead(0);
        save(notification);
        // Increment unread counter in Redis
        redisUtils.increment(AuthConstants.NOTIFICATION_UNREAD_PREFIX + userId, 1L);
    }

    @Override
    public IPage<Notification> pageNotifications(Page<Notification> page, Long userId) {
        return page(page, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt));
    }

    @Override
    @Transactional
    public void markAllRead(Long userId) {
        update(new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
        redisUtils.delete(AuthConstants.NOTIFICATION_UNREAD_PREFIX + userId);
    }

    @Override
    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = getById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOTIFICATION_NOT_FOUND);
        }
        if (notification.getIsRead() == 0) {
            notification.setIsRead(1);
            updateById(notification);
            // Decrement the unread counter (floor at 0)
            String key = AuthConstants.NOTIFICATION_UNREAD_PREFIX + userId;
            Object val = redisUtils.get(key);
            if (val instanceof Number n && n.longValue() > 0) {
                redisUtils.increment(key, -1L);
            }
        }
    }

    @Override
    public long getUnreadCount(Long userId) {
        Object cached = redisUtils.get(AuthConstants.NOTIFICATION_UNREAD_PREFIX + userId);
        if (cached instanceof Number n) {
            return n.longValue();
        }
        long count = count(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        if (count > 0) {
            redisUtils.set(AuthConstants.NOTIFICATION_UNREAD_PREFIX + userId, count);
        }
        return count;
    }
}
