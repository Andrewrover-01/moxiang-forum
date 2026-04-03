package com.moxiang.common.exception;

/**
 * Thrown when a rate-limit threshold is exceeded for a particular action.
 */
public class RateLimitException extends RuntimeException {

    public RateLimitException(String message) {
        super(message);
    }
}
