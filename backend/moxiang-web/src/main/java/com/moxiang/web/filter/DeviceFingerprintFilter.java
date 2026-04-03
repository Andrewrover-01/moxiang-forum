package com.moxiang.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moxiang.common.api.CommonResult;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.constant.RateLimitConstants;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.common.utils.WebUtils;
import com.moxiang.service.security.BlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * Security filter that runs early in the filter chain to:
 * <ol>
 *   <li>Block requests from blacklisted IP addresses.</li>
 *   <li>Collect a device fingerprint from request headers.</li>
 *   <li>Block requests carrying a blacklisted device fingerprint.</li>
 *   <li>Enforce per-IP registration rate limits and auto-gray-list abusive IPs.</li>
 * </ol>
 *
 * <p>The fingerprint is derived from:
 * <ul>
 *   <li>{@code X-Device-Fingerprint} header (supplied by the frontend client)</li>
 *   <li>Fallback: SHA-256( User-Agent + Accept-Language + client IP )</li>
 * </ul>
 */
@Component
public class DeviceFingerprintFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprintFilter.class);

    /** Header name that the frontend may use to supply a pre-computed fingerprint. */
    static final String FP_HEADER = "X-Device-Fingerprint";

    /** Registration endpoint path. */
    private static final String REGISTER_PATH = "/api/user/register";

    private final BlacklistService blacklistService;
    private final RedisUtils redisUtils;
    private final ObjectMapper objectMapper;

    public DeviceFingerprintFilter(BlacklistService blacklistService,
                                   RedisUtils redisUtils,
                                   ObjectMapper objectMapper) {
        this.blacklistService = blacklistService;
        this.redisUtils = redisUtils;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = WebUtils.getClientIp(request);

        // 1. Block blacklisted IPs
        if (blacklistService.isIpBlacklisted(clientIp)) {
            log.warn("Blocked request from blacklisted IP: {}", clientIp);
            writeBlocked(response);
            return;
        }

        // 2. Compute / collect device fingerprint
        String fingerprint = computeFingerprint(request, clientIp);

        // 3. Block blacklisted fingerprints
        if (blacklistService.isFingerprintBlacklisted(fingerprint)) {
            log.warn("Blocked request from blacklisted fingerprint: {} (ip={})", fingerprint, clientIp);
            writeBlocked(response);
            return;
        }

        // 4. Attach fingerprint to the request so downstream code can use it
        request.setAttribute(FP_HEADER, fingerprint);

        // 5. Registration rate-limit and multi-account detection
        if (REGISTER_PATH.equals(request.getServletPath())) {
            enforceRegistrationLimit(clientIp, fingerprint);
        }

        filterChain.doFilter(request, response);
    }

    // ---- Helpers ----

    /**
     * Enforces IP-based registration rate limiting.
     * After {@link RateLimitConstants#REGISTER_LIMIT} registrations per hour from
     * the same IP the IP is automatically added to the gray-list.
     */
    private void enforceRegistrationLimit(String clientIp, String fingerprint) {
        String key = RateLimitConstants.RL_REGISTER + clientIp;
        long count = redisUtils.increment(key, 1L);
        if (count == 1L) {
            redisUtils.expire(key, 3600L, TimeUnit.SECONDS);
        }
        if (count > RateLimitConstants.REGISTER_LIMIT) {
            log.warn("High registration rate from IP {} (count={}); auto-gray-listing", clientIp, count);
            // Auto gray-list the IP
            redisUtils.set(RateLimitConstants.GRAYLIST_IP + clientIp, "1",
                    RateLimitConstants.GRAYLIST_IP_TTL_SECONDS, TimeUnit.SECONDS);
        }
        // Record fingerprint → IP association for multi-account analysis
        redisUtils.set(RateLimitConstants.DEVICE_FP_PREFIX + fingerprint + ":ip", clientIp,
                30L * 24 * 3600, TimeUnit.SECONDS);
    }

    /**
     * Returns a hex-encoded SHA-256 hash used as the device fingerprint.
     * Prefers the {@code X-Device-Fingerprint} header supplied by the client;
     * falls back to hashing User-Agent + Accept-Language + IP.
     */
    private String computeFingerprint(HttpServletRequest request, String clientIp) {
        String provided = request.getHeader(FP_HEADER);
        if (provided != null && !provided.isBlank()) {
            return sha256Hex(provided.trim());
        }
        String ua   = nvl(request.getHeader("User-Agent"));
        String lang = nvl(request.getHeader("Accept-Language"));
        return sha256Hex(ua + "|" + lang + "|" + clientIp);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in the JDK
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Writes a JSON 403 response without proceeding through the filter chain. */
    private void writeBlocked(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        CommonResult<Void> body = CommonResult.failed(ResultCode.DEVICE_BLOCKED);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
