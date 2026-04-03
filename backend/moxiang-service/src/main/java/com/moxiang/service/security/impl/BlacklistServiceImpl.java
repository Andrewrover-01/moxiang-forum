package com.moxiang.service.security.impl;

import com.moxiang.common.constant.RateLimitConstants;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.service.security.BlacklistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Redis-backed implementation of {@link BlacklistService}.
 *
 * <p>Each blacklist/graylist entry is stored as a plain string key in Redis:
 * <ul>
 *   <li>{@code blacklist:ip:<ip>}          → value "1", optional TTL</li>
 *   <li>{@code blacklist:user:<userId>}    → value "1", optional TTL</li>
 *   <li>{@code blacklist:fp:<fp>}          → value "1", optional TTL</li>
 *   <li>{@code graylist:user:<userId>}     → value "1", optional TTL</li>
 *   <li>{@code device:fp:<fp>}             → userId (string)</li>
 * </ul>
 */
@Service
public class BlacklistServiceImpl implements BlacklistService {

    private static final Logger log = LoggerFactory.getLogger(BlacklistServiceImpl.class);

    /** Sentinel value stored for every blacklist / graylist entry. */
    private static final String BLOCKED = "1";

    /** Default TTL (seconds) used when the caller passes 0 (permanent). */
    private static final long PERMANENT_TTL = 365L * 24 * 3600; // 1 year

    /** TTL for device-fingerprint tracking entries (30 days). */
    private static final long FP_TTL_SECONDS = 30L * 24 * 3600;

    private final RedisUtils redisUtils;

    public BlacklistServiceImpl(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    // ---- Blacklist: IP ----

    @Override
    public void blacklistIp(String ip, long expireSeconds) {
        String key = RateLimitConstants.BLACKLIST_IP + ip;
        store(key, expireSeconds);
        log.info("IP blacklisted: {} (ttl={}s)", ip, expireSeconds == 0 ? "permanent" : expireSeconds);
    }

    @Override
    public void removeIpFromBlacklist(String ip) {
        redisUtils.delete(RateLimitConstants.BLACKLIST_IP + ip);
        log.info("IP removed from blacklist: {}", ip);
    }

    @Override
    public boolean isIpBlacklisted(String ip) {
        return redisUtils.hasKey(RateLimitConstants.BLACKLIST_IP + ip);
    }

    @Override
    public Set<String> listBlacklistedIps() {
        return extractSuffixes(redisUtils.keys(RateLimitConstants.BLACKLIST_IP + "*"),
                RateLimitConstants.BLACKLIST_IP);
    }

    // ---- Blacklist: User ----

    @Override
    public void blacklistUser(Long userId, long expireSeconds) {
        String key = RateLimitConstants.BLACKLIST_USER + userId;
        store(key, expireSeconds);
        log.info("User blacklisted: {} (ttl={}s)", userId, expireSeconds == 0 ? "permanent" : expireSeconds);
    }

    @Override
    public void removeUserFromBlacklist(Long userId) {
        redisUtils.delete(RateLimitConstants.BLACKLIST_USER + userId);
        log.info("User removed from blacklist: {}", userId);
    }

    @Override
    public boolean isUserBlacklisted(Long userId) {
        return redisUtils.hasKey(RateLimitConstants.BLACKLIST_USER + userId);
    }

    @Override
    public Set<String> listBlacklistedUsers() {
        return extractSuffixes(redisUtils.keys(RateLimitConstants.BLACKLIST_USER + "*"),
                RateLimitConstants.BLACKLIST_USER);
    }

    // ---- Blacklist: Fingerprint ----

    @Override
    public void blacklistFingerprint(String fingerprint, long expireSeconds) {
        String key = RateLimitConstants.BLACKLIST_FINGERPRINT + fingerprint;
        store(key, expireSeconds);
        log.info("Fingerprint blacklisted: {} (ttl={}s)", fingerprint, expireSeconds == 0 ? "permanent" : expireSeconds);
    }

    @Override
    public void removeFingerprintFromBlacklist(String fingerprint) {
        redisUtils.delete(RateLimitConstants.BLACKLIST_FINGERPRINT + fingerprint);
    }

    @Override
    public boolean isFingerprintBlacklisted(String fingerprint) {
        return redisUtils.hasKey(RateLimitConstants.BLACKLIST_FINGERPRINT + fingerprint);
    }

    // ---- Gray-list: User ----

    @Override
    public void graylistUser(Long userId, long expireSeconds) {
        String key = RateLimitConstants.GRAYLIST_USER + userId;
        store(key, expireSeconds);
        log.info("User gray-listed: {} (ttl={}s)", userId, expireSeconds == 0 ? "permanent" : expireSeconds);
    }

    @Override
    public void removeUserFromGraylist(Long userId) {
        redisUtils.delete(RateLimitConstants.GRAYLIST_USER + userId);
        log.info("User removed from gray-list: {}", userId);
    }

    @Override
    public boolean isUserGraylisted(Long userId) {
        return redisUtils.hasKey(RateLimitConstants.GRAYLIST_USER + userId);
    }

    @Override
    public Set<String> listGraylistedUsers() {
        return extractSuffixes(redisUtils.keys(RateLimitConstants.GRAYLIST_USER + "*"),
                RateLimitConstants.GRAYLIST_USER);
    }

    // ---- Device fingerprint tracking ----

    @Override
    public void recordDeviceFingerprint(String fingerprint, Long userId) {
        String key = RateLimitConstants.DEVICE_FP_PREFIX + fingerprint;
        redisUtils.set(key, String.valueOf(userId), FP_TTL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public Long getLastUserForFingerprint(String fingerprint) {
        Object value = redisUtils.get(RateLimitConstants.DEVICE_FP_PREFIX + fingerprint);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---- Helpers ----

    private void store(String key, long expireSeconds) {
        long ttl = expireSeconds <= 0 ? PERMANENT_TTL : expireSeconds;
        redisUtils.set(key, BLOCKED, ttl, TimeUnit.SECONDS);
    }

    private Set<String> extractSuffixes(Set<String> keys, String prefix) {
        return keys.stream()
                .map(k -> k.startsWith(prefix) ? k.substring(prefix.length()) : k)
                .collect(Collectors.toSet());
    }
}
