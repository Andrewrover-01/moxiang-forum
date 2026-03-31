package com.moxiang.service.user;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.mbg.entity.User;

/**
 * User service interface.
 */
public interface UserService {

    /** Register a new user; returns the saved User with id populated */
    User register(String username, String password, String email);

    /** Validate credentials and return a signed JWT token */
    String login(String username, String password);

    /** Logout: invalidate the token in Redis blacklist */
    void logout(String token);

    User getById(Long id);

    User getByUsername(String username);

    IPage<User> pageUsers(Page<User> page, String keyword);

    void updateProfile(Long userId, String avatar, String bio);

    void changePassword(Long userId, String oldPassword, String newPassword);

    /** Admin: enable/disable user account */
    void updateStatus(Long userId, Integer status);

    void deleteUser(Long userId);
}
