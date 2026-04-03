package com.moxiang.common.annotation;

import java.lang.annotation.*;

/**
 * Marks a controller method for Redis-backed rate limiting.
 *
 * <p>The aspect increments a counter in Redis for every invocation; when the
 * counter exceeds {@link #limit()} within the {@link #period()} window a
 * {@link com.moxiang.common.exception.RateLimitException} is thrown.
 *
 * <p>Users on the gray-list automatically receive a stricter limit
 * ({@code limit / GRAYLIST_DIVISOR}).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * Redis key prefix that distinguishes this rate-limit bucket
     * (e.g. {@code "rl:post:create:"}).
     */
    String key();

    /** Maximum allowed invocations within the {@link #period()} window. */
    int limit() default 10;

    /** Sliding-window length in seconds. */
    long period() default 3600L;

    /** Dimension used to identify the caller ({@code USER} or {@code IP}). */
    RateLimitType limitBy() default RateLimitType.USER;

    /** Message returned to the caller when the limit is exceeded. */
    String message() default "操作过于频繁，请稍后再试";
}
