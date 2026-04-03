package com.moxiang.common.constant;

/**
 * Redis key prefixes and default threshold values for the anti-fraud rate-limit system.
 *
 * <p>Key naming convention: {@code rl:<action>:<userId|ip>}
 */
public final class RateLimitConstants {

    private RateLimitConstants() {}

    // ---- Rate-limit Redis key prefixes ----

    /** Per-user post-creation counter (window: 1 hour). */
    public static final String RL_POST_CREATE = "rl:post:create:";

    /** Per-user comment-creation counter (window: 1 hour). */
    public static final String RL_COMMENT_CREATE = "rl:comment:create:";

    /** Per-user post-like counter (window: 24 hours). */
    public static final String RL_POST_LIKE = "rl:post:like:";

    /** Per-user comment-like counter (window: 24 hours). */
    public static final String RL_COMMENT_LIKE = "rl:comment:like:";

    /**
     * Per-IP per-post view-count deduplication key.
     * Full key: {@code rl:view:<postId>:<ip>}
     */
    public static final String RL_VIEW_COUNT = "rl:view:";

    /** Per-IP registration counter (window: 1 hour). */
    public static final String RL_REGISTER = "rl:register:";

    // ---- Blacklist / graylist Redis key prefixes ----

    /** Blocked IP addresses: {@code blacklist:ip:<ip>} → value "1", optional TTL. */
    public static final String BLACKLIST_IP = "blacklist:ip:";

    /** Blocked user IDs: {@code blacklist:user:<userId>} → value "1", optional TTL. */
    public static final String BLACKLIST_USER = "blacklist:user:";

    /** Blocked device fingerprints: {@code blacklist:fp:<fp>} → value "1", optional TTL. */
    public static final String BLACKLIST_FINGERPRINT = "blacklist:fp:";

    /** Gray-listed user IDs (restricted write access): {@code graylist:user:<userId>}. */
    public static final String GRAYLIST_USER = "graylist:user:";

    /** Gray-listed IP addresses (restricted write access): {@code graylist:ip:<ip>}. */
    public static final String GRAYLIST_IP = "graylist:ip:";

    // ---- Device fingerprint ----

    /** Maps fingerprint hash → userId (last seen): {@code device:fp:<fp>}. */
    public static final String DEVICE_FP_PREFIX = "device:fp:";

    // ---- Default rate-limit thresholds ----

    /** Max posts per user per hour. */
    public static final int POST_CREATE_LIMIT = 10;

    /** Max comments per user per hour. */
    public static final int COMMENT_CREATE_LIMIT = 30;

    /** Max post likes per user per 24 hours. */
    public static final int POST_LIKE_LIMIT = 100;

    /** Max comment likes per user per 24 hours. */
    public static final int COMMENT_LIKE_LIMIT = 100;

    /** Max view-count increments per IP per post within the dedup window (10 min). */
    public static final int VIEW_COUNT_LIMIT = 1;

    /** Max registrations per IP per hour before auto-graylist. */
    public static final int REGISTER_LIMIT = 5;

    /**
     * View-count deduplication window in seconds (10 minutes).
     * Prevents repeated increments from the same IP within this window.
     */
    public static final long VIEW_DEDUP_WINDOW_SECONDS = 600L;

    /** How long (seconds) the graylist IP entry persists after auto-listing (24 hours). */
    public static final long GRAYLIST_IP_TTL_SECONDS = 24L * 3600;

    /**
     * Divisor applied to the normal limit when the caller is on the graylist.
     * Effective limit = normalLimit / GRAYLIST_DIVISOR.
     */
    public static final int GRAYLIST_DIVISOR = 5;
}
