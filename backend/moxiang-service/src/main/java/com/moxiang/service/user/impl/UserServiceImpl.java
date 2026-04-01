package com.moxiang.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.constant.AuthConstants;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.common.utils.JwtUtils;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.mbg.entity.User;
import com.moxiang.mbg.mapper.UserMapper;
import com.moxiang.service.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * User service implementation.
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(JwtUtils jwtUtils, RedisUtils redisUtils, PasswordEncoder passwordEncoder) {
        this.jwtUtils = jwtUtils;
        this.redisUtils = redisUtils;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User register(String username, String password, String email) {
        if (getByUsername(username) != null) {
            throw new BusinessException(ResultCode.USER_ALREADY_EXISTS);
        }
        if (getByEmail(email) != null) {
            throw new BusinessException(ResultCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User()
                .setUsername(username)
                .setPassword(passwordEncoder.encode(password))
                .setEmail(email)
                .setRole(AuthConstants.ROLE_USER)
                .setStatus(0);

        save(user);
        return user;
    }

    @Override
    public String login(String username, String password) {
        User user = getByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole());

        // Cache user info in Redis
        String cacheKey = AuthConstants.USER_CACHE_PREFIX + user.getId();
        redisUtils.set(cacheKey, user, AuthConstants.TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        return token;
    }

    @Override
    public void logout(String token) {
        long ttl = jwtUtils.getRemainingExpiry(token);
        if (ttl > 0) {
            String blacklistKey = AuthConstants.TOKEN_BLACKLIST_PREFIX + token;
            redisUtils.set(blacklistKey, "1", ttl, TimeUnit.SECONDS);
        }
    }

    @Override
    public User getById(Long id) {
        String cacheKey = AuthConstants.USER_CACHE_PREFIX + id;
        Object cached = redisUtils.get(cacheKey);
        if (cached instanceof User user) {
            return user;
        }
        User user = super.getById(id);
        if (user != null) {
            redisUtils.set(cacheKey, user, AuthConstants.TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);
        }
        return user;
    }

    @Override
    public User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    private User getByEmail(String email) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    }

    @Override
    public IPage<User> pageUsers(Page<User> page, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                   .or()
                   .like(User::getEmail, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return page(page, wrapper);
    }

    @Override
    public void updateProfile(Long userId, String avatar, String bio) {
        User user = requireUser(userId);
        user.setAvatar(avatar).setBio(bio);
        updateById(user);
        evictUserCache(userId);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException(ResultCode.PASSWORD_ERROR);
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
        evictUserCache(userId);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        User user = requireUser(userId);
        user.setStatus(status);
        updateById(user);
        evictUserCache(userId);
    }

    @Override
    public void deleteUser(Long userId) {
        requireUser(userId);
        removeById(userId);
        evictUserCache(userId);
    }

    // ---- Helpers ----

    private User requireUser(Long userId) {
        User user = super.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    private void evictUserCache(Long userId) {
        redisUtils.delete(AuthConstants.USER_CACHE_PREFIX + userId);
    }
}
