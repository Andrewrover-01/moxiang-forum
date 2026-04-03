package com.moxiang.web.controller;

import com.moxiang.common.api.CommonResult;
import com.moxiang.common.constant.ContentType;
import com.moxiang.service.moderation.ContentModerationService;
import com.moxiang.service.moderation.SensitiveWordFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin content moderation controller.
 *
 * <p>All endpoints require the {@code ADMIN} role.
 *
 * <h3>API overview</h3>
 * <pre>
 *   GET  /api/admin/moderation/queue/{type}         — list pending review queue
 *   GET  /api/admin/moderation/queue/sizes          — all queue sizes
 *   POST /api/admin/moderation/{type}/{id}/approve  — approve flagged content
 *   POST /api/admin/moderation/{type}/{id}/reject   — reject flagged content
 *   GET  /api/admin/moderation/reports              — list user reports
 *   POST /api/admin/moderation/report/{id}/handle   — mark report as handled
 *   GET  /api/admin/moderation/keywords             — list active sensitive keywords
 *   POST /api/admin/moderation/keywords             — add a keyword
 *   DELETE /api/admin/moderation/keywords/{keyword} — remove a keyword
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/moderation")
@PreAuthorize("hasRole('ADMIN')")
public class AdminModerationController {

    private final ContentModerationService moderationService;
    private final SensitiveWordFilter sensitiveWordFilter;

    public AdminModerationController(ContentModerationService moderationService,
                                     SensitiveWordFilter sensitiveWordFilter) {
        this.moderationService = moderationService;
        this.sensitiveWordFilter = sensitiveWordFilter;
    }

    // ---- Review queue ----

    /**
     * Lists content items pending manual review for the specified content type.
     *
     * @param type  one of {@code POST}, {@code COMMENT}, {@code NOVEL_CHAPTER}
     * @param limit max items to return (default 50)
     */
    @GetMapping("/queue/{type}")
    public CommonResult<List<Map<String, Object>>> listQueue(
            @PathVariable String type,
            @RequestParam(defaultValue = "50") int limit) {
        ContentType contentType = parseContentType(type);
        return CommonResult.success(moderationService.listPendingQueue(contentType, limit));
    }

    /**
     * Returns the number of items in each pending review queue.
     */
    @GetMapping("/queue/sizes")
    public CommonResult<Map<String, Long>> queueSizes() {
        return CommonResult.success(moderationService.queueSizes());
    }

    // ---- Approve / Reject ----

    /**
     * Approves a flagged content item — restores its visibility.
     */
    @PostMapping("/{type}/{id}/approve")
    public CommonResult<Void> approve(@PathVariable String type,
                                      @PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> body) {
        ContentType contentType = parseContentType(type);
        Long reviewerId = getCurrentUserId();
        String reason = body != null ? body.get("reason") : null;
        moderationService.approve(contentType, id, reviewerId, reason);
        return CommonResult.success();
    }

    /**
     * Rejects a flagged content item — keeps it hidden.
     */
    @PostMapping("/{type}/{id}/reject")
    public CommonResult<Void> reject(@PathVariable String type,
                                     @PathVariable Long id,
                                     @RequestBody(required = false) Map<String, String> body) {
        ContentType contentType = parseContentType(type);
        Long reviewerId = getCurrentUserId();
        String reason = body != null ? body.get("reason") : null;
        moderationService.reject(contentType, id, reviewerId, reason);
        return CommonResult.success();
    }

    // ---- User reports ----

    /**
     * Lists user-submitted reports.
     */
    @GetMapping("/reports")
    public CommonResult<List<Map<String, Object>>> listReports(
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "20") int limit) {
        return CommonResult.success(moderationService.listReports(offset, limit));
    }

    /**
     * Marks a user report as handled.
     */
    @PostMapping("/report/{id}/handle")
    public CommonResult<Void> handleReport(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, String> body) {
        Long reviewerId = getCurrentUserId();
        String note = body != null ? body.get("note") : null;
        moderationService.handleReport(id, reviewerId, note);
        return CommonResult.success();
    }

    // ---- Sensitive keywords ----

    /**
     * Returns the list of all active sensitive keywords.
     */
    @GetMapping("/keywords")
    public CommonResult<Set<Object>> listKeywords() {
        return CommonResult.success(sensitiveWordFilter.listKeywords());
    }

    /**
     * Adds a sensitive keyword.
     * Request body: {@code { "keyword": "..." }}
     */
    @PostMapping("/keywords")
    public CommonResult<Void> addKeyword(@RequestBody Map<String, String> body) {
        String keyword = body.get("keyword");
        if (keyword == null || keyword.isBlank()) {
            return CommonResult.validateFailed("keyword不能为空");
        }
        sensitiveWordFilter.addKeyword(keyword.trim());
        return CommonResult.success();
    }

    /**
     * Removes a sensitive keyword.
     */
    @DeleteMapping("/keywords/{keyword}")
    public CommonResult<Void> removeKeyword(@PathVariable String keyword) {
        sensitiveWordFilter.removeKeyword(keyword);
        return CommonResult.success();
    }

    // ---- Helpers ----

    private ContentType parseContentType(String type) {
        try {
            return ContentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.moxiang.common.exception.BusinessException(
                    com.moxiang.common.api.ResultCode.VALIDATE_FAILED,
                    "无效的内容类型: " + type + "，有效值: POST, COMMENT, NOVEL_CHAPTER");
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof Long) {
            return (Long) auth.getCredentials();
        }
        return -1L;
    }
}
