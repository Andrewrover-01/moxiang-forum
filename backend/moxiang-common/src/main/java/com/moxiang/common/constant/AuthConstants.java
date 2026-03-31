package com.moxiang.common.constant;

/**
 * Authentication-related constants.
 */
public final class AuthConstants {

    private AuthConstants() {}

    /** HTTP header that carries the JWT token */
    public static final String AUTH_HEADER = "Authorization";

    /** JWT token prefix */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** Redis key prefix for blacklisted/logged-out tokens */
    public static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    /** Redis key prefix for user info cache */
    public static final String USER_CACHE_PREFIX = "user:info:";

    /** Redis key prefix for hot posts cache */
    public static final String HOT_POST_CACHE_KEY = "post:hot:list";

    /** Redis key prefix for post view count */
    public static final String POST_VIEW_COUNT_PREFIX = "post:view:";

    /** Redis key prefix for post like set (stores user IDs who liked a post) */
    public static final String POST_LIKE_PREFIX = "post:like:";

    /** Redis key prefix for novel collect set */
    public static final String NOVEL_COLLECT_PREFIX = "novel:collect:";

    /** JWT claim key for user role */
    public static final String CLAIM_ROLE = "role";

    /** JWT claim key for user ID */
    public static final String CLAIM_USER_ID = "userId";

    /** Default token expiry in seconds (7 days) */
    public static final long TOKEN_EXPIRE_SECONDS = 7 * 24 * 3600L;

    /** User role identifier */
    public static final String ROLE_USER = "USER";

    /** Admin role identifier */
    public static final String ROLE_ADMIN = "ADMIN";
}
