package com.moxiang.service.security;

import java.util.Set;

/**
 * Manages IP/user/fingerprint blacklists and gray-lists stored in Redis.
 *
 * <p><b>Blacklist</b> — requests from a blacklisted IP, user, or fingerprint are
 * rejected outright (HTTP 403).<br>
 * <b>Gray-list</b> — write operations for gray-listed users or IPs succeed but
 * are subject to much stricter rate limits.
 */
public interface BlacklistService {

    // ---- Blacklist ----

    /**
     * Adds an IP address to the blacklist.
     *
     * @param ip            IP address to block
     * @param expireSeconds TTL in seconds; {@code 0} means permanent
     */
    void blacklistIp(String ip, long expireSeconds);

    /**
     * Adds a user ID to the blacklist.
     *
     * @param userId        target user
     * @param expireSeconds TTL in seconds; {@code 0} means permanent
     */
    void blacklistUser(Long userId, long expireSeconds);

    /**
     * Adds a device fingerprint to the blacklist.
     *
     * @param fingerprint   hashed fingerprint string
     * @param expireSeconds TTL in seconds; {@code 0} means permanent
     */
    void blacklistFingerprint(String fingerprint, long expireSeconds);

    void removeIpFromBlacklist(String ip);

    void removeUserFromBlacklist(Long userId);

    void removeFingerprintFromBlacklist(String fingerprint);

    boolean isIpBlacklisted(String ip);

    boolean isUserBlacklisted(Long userId);

    boolean isFingerprintBlacklisted(String fingerprint);

    /** Returns all currently blacklisted IP keys (admin view). */
    Set<String> listBlacklistedIps();

    /** Returns all currently blacklisted user keys (admin view). */
    Set<String> listBlacklistedUsers();

    // ---- Gray-list ----

    /**
     * Adds a user to the gray-list (restricted rate limits).
     *
     * @param userId        target user
     * @param expireSeconds TTL in seconds; {@code 0} means permanent
     */
    void graylistUser(Long userId, long expireSeconds);

    void removeUserFromGraylist(Long userId);

    boolean isUserGraylisted(Long userId);

    /** Returns all currently gray-listed user keys (admin view). */
    Set<String> listGraylistedUsers();

    // ---- Device fingerprint tracking ----

    /**
     * Records the most recent user associated with a device fingerprint.
     * Used by the risk-control logic to detect multi-account behaviour.
     *
     * @param fingerprint hashed fingerprint
     * @param userId      authenticated or newly registered user
     */
    void recordDeviceFingerprint(String fingerprint, Long userId);

    /**
     * Returns the userId last associated with the given fingerprint, or
     * {@code null} if not seen before.
     */
    Long getLastUserForFingerprint(String fingerprint);
}
