package com.moxiang.service.security.impl;

import com.moxiang.common.constant.CaptchaConstants;
import com.moxiang.common.constant.RateLimitConstants;
import com.moxiang.common.exception.CaptchaException;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.service.security.BlacklistService;
import com.moxiang.service.security.CaptchaChallenge;
import com.moxiang.service.security.CaptchaService;
import com.moxiang.service.security.CaptchaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis-backed CAPTCHA service.
 *
 * <h3>Challenge storage schema (Redis hash)</h3>
 * Key: {@code captcha:challenge:<captchaId>}
 * <pre>
 *   type    → "INVISIBLE" | "SLIDER"
 *   answer  → expected slider position (0-100) for SLIDER, "" for INVISIBLE
 *   scene   → caller-supplied scene label
 *   ip      → caller's IP (for auditing)
 * </pre>
 *
 * <h3>Token storage schema (Redis string)</h3>
 * Key: {@code captcha:token:<uuid>}  →  value "1"
 */
@Service
public class CaptchaServiceImpl implements CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaServiceImpl.class);

    /**
     * Slider gap is randomly placed between these percentages (inclusive) so the
     * puzzle is always reachable on mobile without extreme drags.
     */
    private static final int SLIDER_MIN = 30;
    private static final int SLIDER_MAX = 70;

    private final RedisUtils redisUtils;
    private final BlacklistService blacklistService;
    private final Random random = new Random();

    public CaptchaServiceImpl(RedisUtils redisUtils, BlacklistService blacklistService) {
        this.redisUtils = redisUtils;
        this.blacklistService = blacklistService;
    }

    // ---- CaptchaService ----

    @Override
    public CaptchaChallenge generateChallenge(String scene, String clientIp, Long userId) {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        CaptchaType type = isHighRisk(clientIp, userId) ? CaptchaType.SLIDER : CaptchaType.INVISIBLE;

        Map<String, String> data = new HashMap<>();
        data.put("type", type.name());
        data.put("scene", scene);
        data.put("ip", clientIp == null ? "" : clientIp);

        Integer sliderGapPercent = null;
        if (type == CaptchaType.SLIDER) {
            sliderGapPercent = SLIDER_MIN + random.nextInt(SLIDER_MAX - SLIDER_MIN + 1);
            data.put("answer", String.valueOf(sliderGapPercent));
        } else {
            data.put("answer", "");
        }

        String key = CaptchaConstants.CHALLENGE_PREFIX + captchaId;
        for (Map.Entry<String, String> entry : data.entrySet()) {
            redisUtils.hashPut(key, entry.getKey(), entry.getValue());
        }
        redisUtils.expire(key, CaptchaConstants.CHALLENGE_TTL_SECONDS, TimeUnit.SECONDS);

        log.debug("CAPTCHA challenge generated: id={} type={} scene={}", captchaId, type, scene);
        return new CaptchaChallenge(captchaId, type, sliderGapPercent);
    }

    @Override
    public String verifyAndIssueToken(String captchaId, Integer answer) {
        String challengeKey = CaptchaConstants.CHALLENGE_PREFIX + captchaId;
        Map<Object, Object> data = redisUtils.hashGetAll(challengeKey);

        if (data == null || data.isEmpty()) {
            throw new CaptchaException("验证码已过期或不存在，请重新获取");
        }

        String typeStr = (String) data.get("type");
        CaptchaType type = CaptchaType.valueOf(typeStr);

        if (type == CaptchaType.SLIDER) {
            String expectedStr = (String) data.get("answer");
            if (expectedStr == null || expectedStr.isBlank()) {
                throw new CaptchaException("验证码数据异常，请重新获取");
            }
            int expected = Integer.parseInt(expectedStr);
            if (answer == null || Math.abs(answer - expected) > CaptchaConstants.SLIDER_TOLERANCE) {
                // Delete the challenge on failure so the user must get a new one
                redisUtils.delete(challengeKey);
                throw new CaptchaException("滑块验证失败，请重试");
            }
        }
        // INVISIBLE: any answer (including null) is accepted

        // Consume the challenge (single-use)
        redisUtils.delete(challengeKey);

        // Issue a short-lived verification token
        String token = UUID.randomUUID().toString().replace("-", "");
        redisUtils.set(CaptchaConstants.TOKEN_PREFIX + token, "1",
                CaptchaConstants.TOKEN_TTL_SECONDS, TimeUnit.SECONDS);

        log.debug("CAPTCHA token issued: type={}", type);
        return token;
    }

    @Override
    public boolean consumeToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String key = CaptchaConstants.TOKEN_PREFIX + token;
        boolean exists = redisUtils.hasKey(key);
        if (exists) {
            redisUtils.delete(key);
        }
        return exists;
    }

    @Override
    public boolean isHighRisk(String clientIp, Long userId) {
        if (userId != null && blacklistService.isUserGraylisted(userId)) {
            return true;
        }
        if (clientIp != null && redisUtils.hasKey(RateLimitConstants.GRAYLIST_IP + clientIp)) {
            return true;
        }
        return false;
    }
}
