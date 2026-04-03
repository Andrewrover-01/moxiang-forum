package com.moxiang.web.controller;

import com.moxiang.common.annotation.RateLimit;
import com.moxiang.common.annotation.RateLimitType;
import com.moxiang.common.api.CommonResult;
import com.moxiang.common.api.ResultCode;
import com.moxiang.common.constant.ContentType;
import com.moxiang.common.exception.BusinessException;
import com.moxiang.service.moderation.ContentModerationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User-facing report submission controller.
 *
 * <h3>API</h3>
 * <pre>
 *   POST /api/report  — submit a content report (authenticated users only)
 * </pre>
 *
 * <p>Request body fields:
 * <ul>
 *   <li>{@code contentType} — {@code POST}, {@code COMMENT}, or {@code NOVEL_CHAPTER}</li>
 *   <li>{@code contentId} — ID of the content being reported</li>
 *   <li>{@code reason} — free-text description of the violation</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ContentModerationService moderationService;

    public ReportController(ContentModerationService moderationService) {
        this.moderationService = moderationService;
    }

    /**
     * Submits a user report for the specified content item.
     */
    @PostMapping
    @RateLimit(key = "rl:report:", limit = 10, period = 3600L,
               limitBy = RateLimitType.USER, message = "举报提交过于频繁，请稍后再试")
    public CommonResult<Map<String, Long>> submitReport(@RequestBody Map<String, Object> body) {
        Long userId = getCurrentUserId();

        String contentTypeStr = (String) body.get("contentType");
        Object contentIdObj = body.get("contentId");
        String reason = (String) body.get("reason");

        if (contentTypeStr == null || contentIdObj == null) {
            return CommonResult.validateFailed("contentType和contentId不能为空");
        }
        if (reason == null || reason.isBlank()) {
            return CommonResult.validateFailed("举报原因不能为空");
        }

        ContentType contentType;
        try {
            contentType = ContentType.valueOf(contentTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CommonResult.validateFailed("无效的内容类型: " + contentTypeStr);
        }

        Long contentId;
        try {
            contentId = contentIdObj instanceof Number
                    ? ((Number) contentIdObj).longValue()
                    : Long.parseLong(String.valueOf(contentIdObj));
        } catch (NumberFormatException e) {
            return CommonResult.validateFailed("contentId格式无效");
        }

        long reportId = moderationService.submitReport(userId, contentType, contentId, reason);
        return CommonResult.success(Map.of("reportId", reportId));
    }

    // ---- Helper ----

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getCredentials() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return (Long) auth.getCredentials();
    }
}
