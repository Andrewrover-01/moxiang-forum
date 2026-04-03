package com.moxiang.common.exception;

/**
 * Thrown when a CAPTCHA token is missing, invalid, or expired.
 */
public class CaptchaException extends RuntimeException {

    public CaptchaException(String message) {
        super(message);
    }
}
