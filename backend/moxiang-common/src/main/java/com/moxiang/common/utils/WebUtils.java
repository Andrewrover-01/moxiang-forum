package com.moxiang.common.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility methods related to HTTP requests.
 */
public final class WebUtils {

    private WebUtils() {}

    /**
     * Extracts the real client IP address, honouring common reverse-proxy headers.
     *
     * <p>Header priority: {@code X-Forwarded-For} → {@code X-Real-IP} →
     * {@code Proxy-Client-IP} → {@code request.getRemoteAddr()}.
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isBlank(ip)) ip = request.getHeader("X-Real-IP");
        if (isBlank(ip)) ip = request.getHeader("Proxy-Client-IP");
        if (isBlank(ip)) ip = request.getRemoteAddr();
        // X-Forwarded-For may be a comma-separated list; take the first entry
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip == null ? "unknown" : ip;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank() || "unknown".equalsIgnoreCase(s);
    }
}
