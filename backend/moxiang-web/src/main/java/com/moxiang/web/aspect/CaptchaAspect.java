package com.moxiang.web.aspect;

import com.moxiang.common.annotation.RequireCaptcha;
import com.moxiang.common.constant.CaptchaConstants;
import com.moxiang.common.constant.RateLimitConstants;
import com.moxiang.common.exception.CaptchaException;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.common.utils.WebUtils;
import com.moxiang.service.security.BlacklistService;
import com.moxiang.service.security.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
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

/**
 * AOP aspect that enforces the {@link RequireCaptcha} annotation.
 *
 * <h3>Enforcement policy</h3>
 * <ul>
 *   <li><b>High-risk callers</b> (gray-listed user or IP) — a valid, unconsumed
 *       CAPTCHA token in the {@code X-Captcha-Token} header is <em>strictly
 *       required</em>.  Missing or invalid tokens throw {@link CaptchaException}.</li>
 *   <li><b>Normal callers</b> — token validation is still attempted, but a missing
 *       or invalid token is silently ignored (invisible mode).  This avoids friction
 *       for legitimate users while still providing protection when tokens are present.</li>
 * </ul>
 */
@Aspect
@Component
public class CaptchaAspect {

    private static final Logger log = LoggerFactory.getLogger(CaptchaAspect.class);

    private final CaptchaService captchaService;
    private final BlacklistService blacklistService;
    private final RedisUtils redisUtils;

    public CaptchaAspect(CaptchaService captchaService,
                         BlacklistService blacklistService,
                         RedisUtils redisUtils) {
        this.captchaService = captchaService;
        this.blacklistService = blacklistService;
        this.redisUtils = redisUtils;
    }

    @Around("@annotation(requireCaptcha)")
    public Object around(ProceedingJoinPoint pjp, RequireCaptcha requireCaptcha) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            // Not in a servlet context — allow (e.g. internal calls)
            return pjp.proceed();
        }

        String clientIp = WebUtils.getClientIp(request);
        Long userId = getCurrentUserId();
        boolean highRisk = captchaService.isHighRisk(clientIp, userId);

        String token = request.getHeader(CaptchaConstants.CAPTCHA_TOKEN_HEADER);
        boolean tokenConsumed = captchaService.consumeToken(token);

        if (!tokenConsumed) {
            if (highRisk) {
                log.warn("CAPTCHA required but missing/invalid for high-risk caller: ip={} userId={} scene={}",
                        clientIp, userId, requireCaptcha.scene());
                throw new CaptchaException("请完成人机验证后再提交");
            }
            // Normal user — silently allow but log for monitoring
            log.debug("CAPTCHA token absent/invalid for normal caller (silent pass): ip={} scene={}",
                    clientIp, requireCaptcha.scene());
        }

        return pjp.proceed();
    }

    // ---- Helpers ----

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof Long) {
            return (Long) auth.getCredentials();
        }
        return null;
    }
}
