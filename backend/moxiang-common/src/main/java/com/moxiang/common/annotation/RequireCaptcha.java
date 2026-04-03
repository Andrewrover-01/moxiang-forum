package com.moxiang.common.annotation;

import java.lang.annotation.*;

/**
 * Marks a controller method as requiring a valid CAPTCHA token in the
 * {@code X-Captcha-Token} request header.
 *
 * <p>The enforcement policy is risk-based:
 * <ul>
 *   <li><b>Normal users</b> — if no token is supplied or the token is absent, the
 *       request is <em>silently allowed</em> (invisible verification). The frontend
 *       is expected to obtain an auto-pass token from
 *       {@code GET /api/captcha/challenge?scene=...}.</li>
 *   <li><b>Gray-listed users / suspicious IPs</b> — a valid solved token is
 *       <em>strictly required</em>; missing or invalid tokens yield HTTP 427.</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireCaptcha {

    /**
     * Logical scene name (e.g. {@code "REGISTER"}, {@code "POST"}, {@code "COMMENT"}).
     * Used only for logging; the actual risk decision is made by {@code CaptchaService}.
     */
    String scene() default "DEFAULT";
}
