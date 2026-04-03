package com.moxiang.service.moderation;

import com.moxiang.common.constant.ContentType;

import java.util.List;
import java.util.Map;

/**
 * Content moderation service — coordinates machine review and manual review workflows.
 *
 * <h3>Workflow overview</h3>
 * <pre>
 *   User submits content
 *         │
 *         ▼
 *   machineReview() ──clean──▶ content visible immediately
 *         │
 *       flagged
 *         │
 *         ▼
 *   submitForManualReview()  ─────▶ added to admin review queue
 *         │                         content hidden (status set to 1)
 *         ▼
 *   Admin calls approve() or reject()
 *         │
 *   approve → content restored (status set to 0), ModerationRecord saved
 *   reject  → content stays hidden, ModerationRecord saved
 * </pre>
 */
public interface ContentModerationService {

    /**
     * Runs machine (keyword) review on the provided text fields.
     * If any text segment contains a sensitive keyword the content is
     * automatically submitted for manual review.
     *
     * @param type      content type
     * @param contentId primary key of the content item
     * @param authorId  user ID of the content author
     * @param texts     one or more text strings to scan (title + body, etc.)
     * @return the machine review result
     */
    MachineReviewResult machineReview(ContentType type, Long contentId, Long authorId,
                                      String... texts);

    /**
     * Explicitly places a content item into the manual-review queue.
     * (Also called internally after a machine-review flag.)
     */
    void submitForManualReview(ContentType type, Long contentId, Long authorId, String reason);

    /**
     * Admin approves a flagged content item — it becomes visible again.
     *
     * @param type       content type
     * @param contentId  content item ID
     * @param reviewerId admin user ID
     * @param reason     optional review note
     */
    void approve(ContentType type, Long contentId, Long reviewerId, String reason);

    /**
     * Admin rejects a flagged content item — it stays hidden.
     *
     * @param type       content type
     * @param contentId  content item ID
     * @param reviewerId admin user ID
     * @param reason     rejection reason (shown to admin; stored for audit)
     */
    void reject(ContentType type, Long contentId, Long reviewerId, String reason);

    /**
     * Returns up to {@code limit} items pending manual review for the given content type.
     * Each map contains: contentId, authorId, reason, flagTime.
     */
    List<Map<String, Object>> listPendingQueue(ContentType type, int limit);

    /**
     * Returns all pending queues sizes: { "POST": n, "COMMENT": n, "NOVEL_CHAPTER": n }.
     */
    Map<String, Long> queueSizes();

    // ---- User reports ----

    /**
     * Submits a user report for a content item.
     *
     * @param reporterId  user submitting the report
     * @param type        content type
     * @param contentId   reported content ID
     * @param reason      free-text reason supplied by the reporter
     * @return the new report ID
     */
    long submitReport(Long reporterId, ContentType type, Long contentId, String reason);

    /**
     * Returns recent user reports.
     *
     * @param offset starting offset (0-based)
     * @param limit  maximum number of reports to return
     */
    List<Map<String, Object>> listReports(int offset, int limit);

    /**
     * Marks a report as handled.
     *
     * @param reportId   the report ID
     * @param reviewerId admin user ID
     * @param note       optional resolution note
     */
    void handleReport(Long reportId, Long reviewerId, String note);
}
