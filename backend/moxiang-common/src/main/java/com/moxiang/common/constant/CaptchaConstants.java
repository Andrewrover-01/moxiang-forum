package com.moxiang.common.constant;

/**
 * Constants for the CAPTCHA / human-machine verification subsystem.
 */
public final class CaptchaConstants {

    private CaptchaConstants() {}

    // ---- Redis key prefixes ----

    /**
     * Challenge data stored while waiting for the user to solve it.
     * Full key: {@code captcha:challenge:<captchaId>}
     * TTL: {@link #CHALLENGE_TTL_SECONDS}.
     */
    public static final String CHALLENGE_PREFIX = "captcha:challenge:";

    /**
     * Issued single-use verification tokens.
     * Full key: {@code captcha:token:<token>}
     * TTL: {@link #TOKEN_TTL_SECONDS}.
     */
    public static final String TOKEN_PREFIX = "captcha:token:";

    // ---- HTTP headers ----

    /** Request header used to transmit a solved CAPTCHA token to a protected endpoint. */
    public static final String CAPTCHA_TOKEN_HEADER = "X-Captcha-Token";

    // ---- TTLs ----

    /** How long (seconds) a challenge is valid before the user must request a new one (5 min). */
    public static final long CHALLENGE_TTL_SECONDS = 300L;

    /**
     * How long (seconds) a verification token stays valid after the user solves the challenge.
     * Short window to prevent token replay (30 s).
     */
    public static final long TOKEN_TTL_SECONDS = 30L;

    // ---- Slider range (% of track width) ----

    /** Minimum gap position for SLIDER challenges (30%). */
    public static final int SLIDER_MIN = 30;

    /** Maximum gap position for SLIDER challenges (70%). */
    public static final int SLIDER_MAX = 70;

    /**
     * Acceptable deviation (in percentage points, 0-100 scale) between the user's
     * slider answer and the expected gap position.
     */
    public static final int SLIDER_TOLERANCE = 5;

    // ---- Scene identifiers ----

    /** CAPTCHA scene: user registration. */
    public static final String SCENE_REGISTER = "REGISTER";

    /** CAPTCHA scene: creating a post. */
    public static final String SCENE_POST = "POST";

    /** CAPTCHA scene: posting a comment / reply. */
    public static final String SCENE_COMMENT = "COMMENT";
}
