package com.moxiang.service.moderation;

/**
 * Immutable result of a machine text-review pass.
 *
 * <p>A result is either <em>clean</em> (no violations detected) or
 * <em>flagged</em> (at least one sensitive keyword was matched).
 */
public final class MachineReviewResult {

    private final boolean clean;
    /** The first matched keyword; {@code null} when {@link #clean} is {@code true}. */
    private final String matchedKeyword;
    private final String reason;

    private MachineReviewResult(boolean clean, String matchedKeyword, String reason) {
        this.clean = clean;
        this.matchedKeyword = matchedKeyword;
        this.reason = reason;
    }

    public static MachineReviewResult pass() {
        return new MachineReviewResult(true, null, null);
    }

    public static MachineReviewResult flagged(String matchedKeyword) {
        return new MachineReviewResult(false, matchedKeyword,
                "文本包含违规关键词: " + matchedKeyword);
    }

    public boolean isClean() {
        return clean;
    }

    public String getMatchedKeyword() {
        return matchedKeyword;
    }

    public String getReason() {
        return reason;
    }
}
