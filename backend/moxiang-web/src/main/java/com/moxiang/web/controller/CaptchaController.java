package com.moxiang.web.controller;

import com.moxiang.common.api.CommonResult;
import com.moxiang.common.constant.CaptchaConstants;
import com.moxiang.common.utils.WebUtils;
import com.moxiang.service.security.CaptchaChallenge;
import com.moxiang.service.security.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * CAPTCHA endpoints — public, no authentication required.
 *
 * <h3>Flow</h3>
 * <ol>
 *   <li>Client calls {@code GET /api/captcha/challenge?scene=REGISTER} to receive a
 *       challenge whose {@code type} is either {@code INVISIBLE} or {@code SLIDER}.</li>
 *   <li>For {@code INVISIBLE} challenges the frontend calls {@code POST /api/captcha/verify}
 *       immediately (no user interaction needed).</li>
 *   <li>For {@code SLIDER} challenges the user drags a slider; the frontend sends the
 *       resulting position (0–100) to {@code POST /api/captcha/verify}.</li>
 *   <li>On success a short-lived {@code captchaToken} is returned.  The client attaches
 *       it as the {@code X-Captcha-Token} header on the protected API call.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    /**
     * Issues a CAPTCHA challenge.
     *
     * @param scene one of {@link CaptchaConstants#SCENE_REGISTER},
     *              {@link CaptchaConstants#SCENE_POST},
     *              {@link CaptchaConstants#SCENE_COMMENT}
     * @return {@code captchaId}, {@code type} ("INVISIBLE" | "SLIDER"), and
     *         {@code sliderGapPercent} (only when {@code type == "SLIDER"})
     */
    @GetMapping("/challenge")
    public CommonResult<Map<String, Object>> challenge(
            @RequestParam(defaultValue = CaptchaConstants.SCENE_REGISTER) String scene,
            HttpServletRequest request) {

        String clientIp = WebUtils.getClientIp(request);
        Long userId = getCurrentUserId();

        CaptchaChallenge challenge = captchaService.generateChallenge(scene, clientIp, userId);

        Map<String, Object> resp = new HashMap<>();
        resp.put("captchaId", challenge.getCaptchaId());
        resp.put("type", challenge.getType().name());
        if (challenge.getSliderGapPercent() != null) {
            resp.put("sliderGapPercent", challenge.getSliderGapPercent());
        }
        return CommonResult.success(resp);
    }

    /**
     * Verifies the user's answer and issues a single-use CAPTCHA token.
     *
     * @param body {@code captchaId} (required) and {@code answer} (required for SLIDER,
     *             optional / null for INVISIBLE)
     * @return {@code captchaToken} — attach as {@code X-Captcha-Token} header on the
     *         next protected request
     */
    @PostMapping("/verify")
    public CommonResult<Map<String, String>> verify(@RequestBody Map<String, Object> body) {
        String captchaId = (String) body.get("captchaId");
        Object rawAnswer = body.get("answer");
        Integer answer = rawAnswer == null ? null : Integer.valueOf(rawAnswer.toString());

        String token = captchaService.verifyAndIssueToken(captchaId, answer);
        return CommonResult.success(Map.of("captchaToken", token));
    }

    // ---- Helper ----

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof Long) {
            return (Long) auth.getCredentials();
        }
        return null;
    }
}
