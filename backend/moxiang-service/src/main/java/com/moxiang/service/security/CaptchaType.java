package com.moxiang.service.security;

/**
 * Challenge type returned by the server based on the caller's risk level.
 *
 * <ul>
 *   <li>{@link #INVISIBLE} — low-risk caller; the frontend can auto-pass without
 *       showing any puzzle. An empty / null answer is accepted.</li>
 *   <li>{@link #SLIDER} — elevated risk (e.g. gray-listed user or IP);
 *       the user must drag a slider to the correct position.</li>
 * </ul>
 */
public enum CaptchaType {

    /** No user interaction required; token is issued immediately upon verify call. */
    INVISIBLE,

    /** User must drag the slider piece to the gap position. */
    SLIDER
}
