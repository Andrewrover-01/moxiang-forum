package com.moxiang.common.constant;

/**
 * Redis key prefixes and constants for the content moderation system.
 *
 * <h3>Key naming scheme</h3>
 * <pre>
 *   mod:queue:{contentType}           — ZSet: pending manual-review items (score = epoch ms)
 *   mod:flagged:{contentType}:{id}    — Hash: machine-flag metadata
 *   mod:reviewed:{contentType}:{id}   — Hash: review decision record
 *   mod:keywords                      — Set:  active sensitive keywords
 *   mod:report:{id}                   — Hash: individual report record
 *   mod:reports                       — List: ordered report IDs (newest first)
 *   mod:report:seq                    — String: auto-increment report ID counter
 * </pre>
 */
public final class ModerationConstants {

    private ModerationConstants() {}

    // ---- Queue ----

    /** ZSet of content IDs pending manual review, per content type. */
    public static final String MOD_QUEUE_PREFIX = "mod:queue:";

    // ---- Flagged metadata ----

    /** Hash storing machine-flag details for a specific content item. */
    public static final String MOD_FLAGGED_PREFIX = "mod:flagged:";

    /** Hash storing the admin review decision for a specific content item. */
    public static final String MOD_REVIEWED_PREFIX = "mod:reviewed:";

    // ---- Keywords ----

    /** Redis Set containing all active sensitive keyword entries. */
    public static final String MOD_KEYWORDS_KEY = "mod:keywords";

    // ---- Reports ----

    /** Hash for an individual user report record. */
    public static final String MOD_REPORT_PREFIX = "mod:report:";

    /** List of report IDs (newest first). */
    public static final String MOD_REPORTS_KEY = "mod:reports";

    /** Auto-increment counter for report IDs. */
    public static final String MOD_REPORT_SEQ = "mod:report:seq";

    // ---- Decisions ----

    public static final String DECISION_APPROVED = "APPROVED";
    public static final String DECISION_REJECTED = "REJECTED";
    public static final String DECISION_FLAGGED  = "FLAGGED";

    // ---- Review types ----

    public static final String REVIEW_MACHINE = "MACHINE";
    public static final String REVIEW_MANUAL  = "MANUAL";
}
