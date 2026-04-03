package com.moxiang.common.security;

import com.moxiang.common.constant.SecurityEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Centralized security audit logger.
 *
 * <p>All security-relevant events (authentication, authorization failures,
 * rate-limit hits, CAPTCHA outcomes, input sanitization, blacklist enforcement,
 * and plugin decisions) should be routed through this service so that they appear
 * in a single, structured audit trail.
 *
 * <h3>Log format</h3>
 * Each line is written to the dedicated {@code SECURITY_AUDIT} logger:
 * <pre>
 *   [AUDIT] event=&lt;type&gt; ip=&lt;ip&gt; userId=&lt;id|-&gt; path=&lt;path&gt; result=&lt;result&gt; detail=&lt;detail&gt;
 * </pre>
 * Configure a separate appender in {@code logback-spring.xml} to route the
 * {@code SECURITY_AUDIT} logger to its own file for compliance or SIEM integration.
 */
@Component
public class SecurityAuditLogger {

    /** Dedicated logger name — configure a separate appender in logback-spring.xml. */
    private static final Logger AUDIT = LoggerFactory.getLogger("SECURITY_AUDIT");

    // ---- Generic entry point ----

    /**
     * Writes a structured audit log entry.
     *
     * @param type   event type
     * @param ip     client IP address (never null; use "unknown" if unavailable)
     * @param userId authenticated user ID, or {@code null} for anonymous
     * @param path   request path
     * @param result a short outcome label, e.g. "OK", "BLOCKED", "FAIL"
     * @param detail additional context (key=value pairs or free text)
     */
    public void log(SecurityEventType type, String ip, Long userId, String path,
                    String result, String detail) {
        AUDIT.info("[AUDIT] event={} ip={} userId={} path={} result={} detail={}",
                type, ip, userId != null ? userId : "-", path, result,
                detail != null ? detail : "-");
    }

    // ---- Convenience overloads ----

    /**
     * Logs a request-blocked event.
     */
    public void logBlocked(SecurityEventType type, String ip, Long userId, String path,
                           String reason) {
        log(type, ip, userId, path, "BLOCKED", reason);
    }

    /**
     * Logs a successful login.
     */
    public void logLoginOk(String ip, Long userId, String username) {
        log(SecurityEventType.AUTH_LOGIN_OK, ip, userId, "/api/user/login",
                "OK", "username=" + username);
    }

    /**
     * Logs a failed login attempt.
     */
    public void logLoginFail(String ip, String username, String reason) {
        log(SecurityEventType.AUTH_LOGIN_FAIL, ip, null, "/api/user/login",
                "FAIL", "username=" + username + " reason=" + reason);
    }

    /**
     * Logs a new user registration.
     */
    public void logRegister(String ip, Long userId, String username) {
        log(SecurityEventType.ACCT_REGISTER, ip, userId, "/api/user/register",
                "OK", "username=" + username);
    }

    /**
     * Logs a rate-limit exceeded event.
     */
    public void logRateLimited(String ip, Long userId, String path, String limitKey) {
        log(SecurityEventType.RL_EXCEEDED, ip, userId, path,
                "RATE_LIMITED", "limitKey=" + limitKey);
    }

    /**
     * Logs a CAPTCHA failure.
     */
    public void logCaptchaFail(String ip, Long userId, String path, String reason) {
        log(SecurityEventType.CAP_FAIL, ip, userId, path, "CAPTCHA_FAIL", reason);
    }

    /**
     * Logs that input sanitization removed XSS content from a field.
     */
    public void logSanitized(String ip, Long userId, String path, String fieldName) {
        log(SecurityEventType.SANITIZE_CLEANED, ip, userId, path,
                "CLEANED", "field=" + fieldName);
    }

    /**
     * Logs an access-denied (403) event.
     */
    public void logAccessDenied(String ip, Long userId, String path) {
        log(SecurityEventType.AUTH_ACCESS_DENIED, ip, userId, path, "DENIED", "-");
    }

    /**
     * Logs an authentication failure (invalid/expired token).
     */
    public void logTokenInvalid(String ip, String path, String reason) {
        log(SecurityEventType.AUTH_TOKEN_INVALID, ip, null, path, "INVALID_TOKEN", reason);
    }
}
