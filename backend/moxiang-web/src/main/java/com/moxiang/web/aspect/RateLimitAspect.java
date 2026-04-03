package com.moxiang.web.aspect;

import com.moxiang.common.annotation.RateLimit;
import com.moxiang.common.annotation.RateLimitType;
import com.moxiang.common.constant.RateLimitConstants;
import com.moxiang.common.exception.RateLimitException;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.common.utils.WebUtils;
import com.moxiang.service.security.BlacklistService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * AOP aspect that enforces {@link RateLimit} annotations on controller methods.
 *
 * <p>Algorithm (fixed-window counter via Redis INCR):
 * <ol>
 *   <li>Determine the subject key (user-id or client IP).</li>
 *   <li>Build the full Redis key: {@code annotation.key() + subject}.</li>
 *   <li>Increment the counter; on first increment set the window TTL.</li>
 *   <li>If the counter exceeds the limit (adjusted for gray-list), throw
 *       {@link RateLimitException}.</li>
 * </ol>
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    private final RedisUtils redisUtils;
    private final BlacklistService blacklistService;

    public RateLimitAspect(RedisUtils redisUtils, BlacklistService blacklistService) {
        this.redisUtils = redisUtils;
        this.blacklistService = blacklistService;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {

        String subject = resolveSubject(rateLimit.limitBy());
        String redisKey = rateLimit.key() + subject;
        int effectiveLimit = resolveEffectiveLimit(rateLimit);

        long count = redisUtils.increment(redisKey, 1L);
        if (count == 1L) {
            // First hit in this window — set the TTL
            redisUtils.expire(redisKey, rateLimit.period(), TimeUnit.SECONDS);
        }

        if (count > effectiveLimit) {
            log.warn("Rate limit exceeded: key={} count={} limit={}", redisKey, count, effectiveLimit);
            throw new RateLimitException(rateLimit.message());
        }

        return pjp.proceed();
    }

    // ---- Helpers ----

    /**
     * Determines the effective rate-limit threshold, applying a stricter cap for
     * gray-listed users.
     */
    private int resolveEffectiveLimit(RateLimit rateLimit) {
        if (rateLimit.limitBy() == RateLimitType.USER) {
            Long userId = getCurrentUserId();
            if (userId != null && blacklistService.isUserGraylisted(userId)) {
                int graylisted = Math.max(1, rateLimit.limit() / RateLimitConstants.GRAYLIST_DIVISOR);
                log.debug("Gray-listed user {}: reduced limit {} → {}", userId, rateLimit.limit(), graylisted);
                return graylisted;
            }
        }
        return rateLimit.limit();
    }

    /**
     * Resolves the rate-limit subject (user-id string or IP address).
     * Falls back to the client IP when no authenticated user is present.
     */
    private String resolveSubject(RateLimitType type) {
        if (type == RateLimitType.USER) {
            Long userId = getCurrentUserId();
            if (userId != null) {
                return String.valueOf(userId);
            }
        }
        return getClientIp();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof Long) {
            return (Long) auth.getCredentials();
        }
        return null;
    }

    private String getClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        return WebUtils.getClientIp(attrs.getRequest());
    }
}
