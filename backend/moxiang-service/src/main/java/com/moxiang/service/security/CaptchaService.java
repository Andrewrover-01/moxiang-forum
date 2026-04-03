package com.moxiang.service.security;

/**
 * Service that generates and validates CAPTCHA challenges.
 *
 * <p>Two-phase flow:
 * <ol>
 *   <li>Client calls {@link #generateChallenge} to receive a challenge.</li>
 *   <li>Client calls {@link #verifyAndIssueToken} with the answer; on success a
 *       short-lived single-use token is returned.</li>
 *   <li>The token is transmitted in the {@code X-Captcha-Token} header on the
 *       protected endpoint.</li>
 *   <li>The protected aspect calls {@link #consumeToken} to validate and
 *       invalidate the token.</li>
 * </ol>
 */
public interface CaptchaService {

    /**
     * Generates a new challenge appropriate for the caller's risk level.
     *
     * @param scene    logical scene identifier (e.g. "REGISTER", "POST")
     * @param clientIp caller's IP address (used for risk assessment)
     * @param userId   authenticated user ID, or {@code null} for anonymous callers
     * @return a challenge object stored in Redis with a short TTL
     */
    CaptchaChallenge generateChallenge(String scene, String clientIp, Long userId);

    /**
     * Validates the user's answer and, if correct, issues a single-use
     * verification token.
     *
     * @param captchaId the ID from the earlier {@link #generateChallenge} call
     * @param answer    the user's response (slider position 0-100, or null for INVISIBLE)
     * @return a short-lived token the client must attach to the protected request
     * @throws com.moxiang.common.exception.CaptchaException if the ID is unknown /
     *         expired, or if the answer is out of tolerance
     */
    String verifyAndIssueToken(String captchaId, Integer answer);

    /**
     * Validates and atomically invalidates a single-use CAPTCHA token.
     *
     * @param token the value from the {@code X-Captcha-Token} header
     * @return {@code true} if the token was valid and has now been consumed,
     *         {@code false} if the token does not exist (already used or expired)
     */
    boolean consumeToken(String token);

    /**
     * Returns {@code true} if the caller is considered high-risk and must solve a
     * visual puzzle (SLIDER), rather than being granted an invisible auto-pass.
     *
     * @param clientIp the caller's IP address
     * @param userId   authenticated user ID, or {@code null}
     */
    boolean isHighRisk(String clientIp, Long userId);
}
