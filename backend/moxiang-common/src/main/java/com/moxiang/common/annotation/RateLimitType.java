package com.moxiang.common.annotation;

/**
 * Specifies the dimension used to identify the rate-limit subject.
 */
public enum RateLimitType {

    /** Rate-limit per authenticated user (falls back to IP if unauthenticated). */
    USER,

    /** Rate-limit per client IP address. */
    IP
}
