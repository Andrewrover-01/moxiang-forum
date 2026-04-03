package com.moxiang.web.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.moxiang.common.annotation.RateLimit;
import com.moxiang.common.annotation.RateLimitType;
import com.moxiang.common.api.CommonResult;
import com.moxiang.common.constant.RateLimitConstants;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.common.api.ResultCode;
import com.moxiang.mbg.entity.User;
import com.moxiang.service.user.UserService;
import com.moxiang.web.dto.LoginRequest;
import com.moxiang.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User controller — registration, login, profile management.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @RateLimit(key = RateLimitConstants.RL_REGISTER,
               limit = RateLimitConstants.REGISTER_LIMIT,
               period = 3600L,
               limitBy = RateLimitType.IP,
               message = "注册请求过于频繁，请稍后再试")
    public CommonResult<User> register(@Valid @RequestBody RegisterRequest req) {
        User user = userService.register(req.getUsername(), req.getPassword(), req.getEmail());
        user.setPassword(null); // Never return password
        return CommonResult.success(user);
    }

    @PostMapping("/login")
    public CommonResult<Map<String, String>> login(@Valid @RequestBody LoginRequest req) {
        String token = userService.login(req.getUsername(), req.getPassword());
        return CommonResult.success(Map.of("token", token));
    }

    @PostMapping("/logout")
    public CommonResult<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userService.logout(token);
        }
        return CommonResult.success();
    }

    @GetMapping("/info")
    public CommonResult<User> getUserInfo() {
        Long userId = getCurrentUserId();
        User user = userService.getById(userId);
        user.setPassword(null);
        return CommonResult.success(user);
    }

    @GetMapping("/{id}")
    public CommonResult<User> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND);
        }
        user.setPassword(null);
        user.setEmail(null);
        return CommonResult.success(user);
    }

    @PutMapping("/profile")
    public CommonResult<Void> updateProfile(@RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        userService.updateProfile(userId, body.get("avatar"), body.get("bio"));
        return CommonResult.success();
    }

    @PutMapping("/password")
    public CommonResult<Void> changePassword(@RequestBody Map<String, String> body) {
        Long userId = getCurrentUserId();
        userService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
        return CommonResult.success();
    }

    @GetMapping("/list")
    public CommonResult<?> listUsers(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword) {
        return CommonResult.success(userService.pageUsers(new Page<>(current, size), keyword));
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
