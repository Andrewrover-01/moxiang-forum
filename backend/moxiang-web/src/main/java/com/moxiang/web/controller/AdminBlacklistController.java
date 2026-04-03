package com.moxiang.web.controller;

import com.moxiang.common.api.CommonResult;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.service.security.BlacklistService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * Admin endpoints for managing the IP/user/fingerprint blacklist and user gray-list.
 *
 * <p>All endpoints require the {@code ADMIN} role.
 *
 * <p>Request body fields:
 * <ul>
 *   <li>{@code ip} / {@code userId} / {@code fingerprint} — the value to block.</li>
 *   <li>{@code expireSeconds} — TTL in seconds; omit or set to {@code 0} for permanent.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/security")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBlacklistController {

    private final BlacklistService blacklistService;

    public AdminBlacklistController(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    // ---- IP blacklist ----

    @PostMapping("/blacklist/ip")
    public CommonResult<Void> blacklistIp(@RequestBody Map<String, Object> body) {
        String ip = (String) body.get("ip");
        long ttl = toLong(body.get("expireSeconds"));
        blacklistService.blacklistIp(ip, ttl);
        return CommonResult.success();
    }

    @DeleteMapping("/blacklist/ip/{ip}")
    public CommonResult<Void> removeIpFromBlacklist(@PathVariable String ip) {
        blacklistService.removeIpFromBlacklist(ip);
        return CommonResult.success();
    }

    @GetMapping("/blacklist/ips")
    public CommonResult<Set<String>> listBlacklistedIps() {
        return CommonResult.success(blacklistService.listBlacklistedIps());
    }

    // ---- User blacklist ----

    @PostMapping("/blacklist/user")
    public CommonResult<Void> blacklistUser(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        long ttl = toLong(body.get("expireSeconds"));
        blacklistService.blacklistUser(userId, ttl);
        return CommonResult.success();
    }

    @DeleteMapping("/blacklist/user/{userId}")
    public CommonResult<Void> removeUserFromBlacklist(@PathVariable Long userId) {
        blacklistService.removeUserFromBlacklist(userId);
        return CommonResult.success();
    }

    @GetMapping("/blacklist/users")
    public CommonResult<Set<String>> listBlacklistedUsers() {
        return CommonResult.success(blacklistService.listBlacklistedUsers());
    }

    // ---- Fingerprint blacklist ----

    @PostMapping("/blacklist/fingerprint")
    public CommonResult<Void> blacklistFingerprint(@RequestBody Map<String, Object> body) {
        String fp = (String) body.get("fingerprint");
        long ttl = toLong(body.get("expireSeconds"));
        blacklistService.blacklistFingerprint(fp, ttl);
        return CommonResult.success();
    }

    @DeleteMapping("/blacklist/fingerprint/{fingerprint}")
    public CommonResult<Void> removeFingerprintFromBlacklist(@PathVariable String fingerprint) {
        blacklistService.removeFingerprintFromBlacklist(fingerprint);
        return CommonResult.success();
    }

    // ---- User gray-list ----

    @PostMapping("/graylist/user")
    public CommonResult<Void> graylistUser(@RequestBody Map<String, Object> body) {
        Long userId = toLong(body.get("userId"));
        long ttl = toLong(body.get("expireSeconds"));
        blacklistService.graylistUser(userId, ttl);
        return CommonResult.success();
    }

    @DeleteMapping("/graylist/user/{userId}")
    public CommonResult<Void> removeUserFromGraylist(@PathVariable Long userId) {
        blacklistService.removeUserFromGraylist(userId);
        return CommonResult.success();
    }

    @GetMapping("/graylist/users")
    public CommonResult<Set<String>> listGraylistedUsers() {
        return CommonResult.success(blacklistService.listGraylistedUsers());
    }

    // ---- Helpers ----

    private static Long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException("参数格式错误，期望数字类型");
        }
    }
}
