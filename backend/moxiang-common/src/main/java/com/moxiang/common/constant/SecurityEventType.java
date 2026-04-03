package com.moxiang.common.constant;

/**
 * Taxonomy of security events written to the centralized audit log.
 *
 * <p>Events are grouped by category:
 * <ul>
 *   <li><b>REQ_*</b>   — request lifecycle (start / complete / blocked)</li>
 *   <li><b>AUTH_*</b>  — authentication events (login, logout, token issues)</li>
 *   <li><b>ACCT_*</b>  — account events (register)</li>
 *   <li><b>WRITE_*</b> — content-write events (post, comment, novel)</li>
 *   <li><b>RL_*</b>    — rate-limit events</li>
 *   <li><b>CAP_*</b>   — CAPTCHA events</li>
 *   <li><b>BL_*</b>    — blacklist/graylist events</li>
 *   <li><b>SANITIZE_*</b> — input sanitization events</li>
 *   <li><b>PLUGIN_*</b> — security-plugin events</li>
 * </ul>
 */
public enum SecurityEventType {

    // ---- Request lifecycle ----
    REQ_START,
    REQ_COMPLETE,
    REQ_BLOCKED,

    // ---- Authentication ----
    AUTH_LOGIN_OK,
    AUTH_LOGIN_FAIL,
    AUTH_LOGOUT,
    AUTH_TOKEN_INVALID,
    AUTH_TOKEN_EXPIRED,
    AUTH_ACCESS_DENIED,

    // ---- Account ----
    ACCT_REGISTER,

    // ---- Content writes ----
    WRITE_POST,
    WRITE_COMMENT,
    WRITE_NOVEL,

    // ---- Rate limiting ----
    RL_EXCEEDED,

    // ---- CAPTCHA ----
    CAP_FAIL,
    CAP_PASS,

    // ---- Blacklist / graylist ----
    BL_IP_BLOCKED,
    BL_USER_BLOCKED,
    BL_FP_BLOCKED,
    BL_IP_GRAYLISTED,

    // ---- Input sanitization ----
    SANITIZE_CLEANED,

    // ---- Plugin ----
    PLUGIN_BLOCKED
}
