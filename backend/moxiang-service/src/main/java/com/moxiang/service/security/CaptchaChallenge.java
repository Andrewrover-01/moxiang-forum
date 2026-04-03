package com.moxiang.service.security;

/**
 * Represents a CAPTCHA challenge issued to a client.
 */
public class CaptchaChallenge {

    private final String captchaId;
    private final CaptchaType type;

    /**
     * For {@link CaptchaType#SLIDER}: the horizontal gap start position (0–100, percentage).
     * The client should position a CSS-drawn gap at this percentage of the track width.
     * {@code null} for {@link CaptchaType#INVISIBLE}.
     */
    private final Integer sliderGapPercent;

    public CaptchaChallenge(String captchaId, CaptchaType type, Integer sliderGapPercent) {
        this.captchaId = captchaId;
        this.type = type;
        this.sliderGapPercent = sliderGapPercent;
    }

    public String getCaptchaId() { return captchaId; }
    public CaptchaType getType() { return type; }
    public Integer getSliderGapPercent() { return sliderGapPercent; }
}
