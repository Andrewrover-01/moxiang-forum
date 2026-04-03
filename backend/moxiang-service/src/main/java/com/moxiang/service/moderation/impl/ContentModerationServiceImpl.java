package com.moxiang.service.moderation.impl;

import com.moxiang.common.constant.ContentType;
import com.moxiang.common.constant.ModerationConstants;
import com.moxiang.common.security.SecurityAuditLogger;
import com.moxiang.common.constant.SecurityEventType;
import com.moxiang.common.utils.RedisUtils;
import com.moxiang.mbg.entity.Comment;
import com.moxiang.mbg.entity.Post;
import com.moxiang.mbg.mapper.CommentMapper;
import com.moxiang.mbg.mapper.PostMapper;
import com.moxiang.service.moderation.ContentModerationService;
import com.moxiang.service.moderation.FilterMode;
import com.moxiang.service.moderation.MachineReviewResult;
import com.moxiang.service.moderation.SensitiveWordFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis-backed content moderation service implementation.
 *
 * <h3>State stored in Redis</h3>
 * <ul>
 *   <li>{@code mod:queue:{type}} — ZSet (contentId, score=epochMs) of items awaiting review.</li>
 *   <li>{@code mod:flagged:{type}:{id}} — Hash: authorId, reason, flagTime.</li>
 *   <li>{@code mod:reviewed:{type}:{id}} — Hash: decision, reviewerId, reason, reviewTime.</li>
 *   <li>{@code mod:report:{seq}} — Hash: reporterId, contentType, contentId, reason, status, time.</li>
 *   <li>{@code mod:reports} — List of report IDs (newest first).</li>
 *   <li>{@code mod:report:seq} — Auto-increment report ID counter.</li>
 * </ul>
 */
@Service
public class ContentModerationServiceImpl implements ContentModerationService {

    private static final Logger log = LoggerFactory.getLogger(ContentModerationServiceImpl.class);

    private final SensitiveWordFilter sensitiveWordFilter;
    private final RedisUtils redisUtils;
    private final SecurityAuditLogger auditLogger;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    public ContentModerationServiceImpl(SensitiveWordFilter sensitiveWordFilter,
                                        RedisUtils redisUtils,
                                        SecurityAuditLogger auditLogger,
                                        PostMapper postMapper,
                                        CommentMapper commentMapper) {
        this.sensitiveWordFilter = sensitiveWordFilter;
        this.redisUtils = redisUtils;
        this.auditLogger = auditLogger;
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
    }

    // ---- Machine review ----

    @Override
    public MachineReviewResult machineReview(ContentType type, Long contentId, Long authorId,
                                             String... texts) {
        // Novel chapters use STRICT mode (novel-industry word library active)
        FilterMode mode = (type == ContentType.NOVEL_CHAPTER) ? FilterMode.STRICT : FilterMode.NORMAL;

        for (String text : texts) {
            MachineReviewResult result = sensitiveWordFilter.checkWithMode(text, mode);
            if (!result.isClean()) {
                log.warn("Machine review flagged {} id={} [mode={}]: {}",
                        type, contentId, mode, result.getReason());
                // Hide the content immediately
                hideContent(type, contentId);
                // Submit to manual review queue
                submitForManualReview(type, contentId, authorId, result.getReason());
                auditLogger.log(SecurityEventType.WRITE_POST, "system", authorId,
                        "/moderation/" + type.name().toLowerCase() + "/" + contentId,
                        "FLAGGED", result.getReason());
                return result;
            }
        }
        return MachineReviewResult.pass();
    }

    @Override
    public void submitForManualReview(ContentType type, Long contentId, Long authorId,
                                      String reason) {
        String queueKey = ModerationConstants.MOD_QUEUE_PREFIX + type.name();
        double score = Instant.now().toEpochMilli();
        redisUtils.zsetAdd(queueKey, String.valueOf(contentId), score);

        String flagKey = ModerationConstants.MOD_FLAGGED_PREFIX + type.name() + ":" + contentId;
        redisUtils.hashPut(flagKey, "authorId", String.valueOf(authorId));
        redisUtils.hashPut(flagKey, "reason", reason != null ? reason : "");
        redisUtils.hashPut(flagKey, "flagTime", String.valueOf((long) score));

        log.info("Content submitted for manual review: type={} id={} reason={}", type, contentId, reason);
    }

    // ---- Manual review (admin) ----

    @Override
    public void approve(ContentType type, Long contentId, Long reviewerId, String reason) {
        // Restore content visibility
        restoreContent(type, contentId);

        // Record the decision (also removes from queue)
        saveReviewRecord(type, contentId, reviewerId, ModerationConstants.DECISION_APPROVED,
                ModerationConstants.REVIEW_MANUAL, reason);

        log.info("Content approved: type={} id={} reviewerId={}", type, contentId, reviewerId);
    }

    @Override
    public void reject(ContentType type, Long contentId, Long reviewerId, String reason) {
        // Record the decision (keeps content hidden, removes from queue)
        saveReviewRecord(type, contentId, reviewerId, ModerationConstants.DECISION_REJECTED,
                ModerationConstants.REVIEW_MANUAL, reason);

        log.info("Content rejected: type={} id={} reviewerId={}", type, contentId, reviewerId);
    }

    // ---- Queue listing ----

    @Override
    public List<Map<String, Object>> listPendingQueue(ContentType type, int limit) {
        String queueKey = ModerationConstants.MOD_QUEUE_PREFIX + type.name();
        Set<Object> ids = redisUtils.zsetRangeByScoreDesc(queueKey, 0, Double.MAX_VALUE, 0, limit);
        List<Map<String, Object>> result = new ArrayList<>();
        if (ids == null) return result;

        for (Object idObj : ids) {
            String id = String.valueOf(idObj);
            String flagKey = ModerationConstants.MOD_FLAGGED_PREFIX + type.name() + ":" + id;
            Map<Object, Object> meta = redisUtils.hashGetAll(flagKey);

            Map<String, Object> item = new HashMap<>();
            item.put("contentId", id);
            item.put("contentType", type.name());
            if (meta != null) {
                item.putAll(castMap(meta));
            }
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Long> queueSizes() {
        Map<String, Long> sizes = new HashMap<>();
        for (ContentType type : ContentType.values()) {
            String queueKey = ModerationConstants.MOD_QUEUE_PREFIX + type.name();
            Double score = redisUtils.zsetScore(queueKey, "dummy_sentinel_for_size");
            // Use zsetRangeByScoreDesc to count — use zset size via set-size trick
            Set<Object> all = redisUtils.zsetRangeByScoreDesc(queueKey, 0, Double.MAX_VALUE, 0, Integer.MAX_VALUE);
            sizes.put(type.name(), all == null ? 0L : (long) all.size());
        }
        return sizes;
    }

    // ---- User reports ----

    @Override
    public long submitReport(Long reporterId, ContentType type, Long contentId, String reason) {
        long reportId = redisUtils.increment(ModerationConstants.MOD_REPORT_SEQ, 1L);
        String reportKey = ModerationConstants.MOD_REPORT_PREFIX + reportId;

        redisUtils.hashPut(reportKey, "reportId", String.valueOf(reportId));
        redisUtils.hashPut(reportKey, "reporterId", String.valueOf(reporterId));
        redisUtils.hashPut(reportKey, "contentType", type.name());
        redisUtils.hashPut(reportKey, "contentId", String.valueOf(contentId));
        redisUtils.hashPut(reportKey, "reason", reason != null ? reason : "");
        redisUtils.hashPut(reportKey, "status", "PENDING");
        redisUtils.hashPut(reportKey, "reportTime", String.valueOf(Instant.now().toEpochMilli()));

        // Prepend to the reports list (newest first)
        redisUtils.listRightPush(ModerationConstants.MOD_REPORTS_KEY, String.valueOf(reportId));

        log.info("User report submitted: id={} reporterId={} type={} contentId={}",
                reportId, reporterId, type, contentId);
        return reportId;
    }

    @Override
    public List<Map<String, Object>> listReports(int offset, int limit) {
        List<Object> ids = redisUtils.listRange(ModerationConstants.MOD_REPORTS_KEY,
                offset, (long) offset + limit - 1);
        List<Map<String, Object>> result = new ArrayList<>();
        if (ids == null) return result;

        for (Object idObj : ids) {
            String reportKey = ModerationConstants.MOD_REPORT_PREFIX + idObj;
            Map<Object, Object> data = redisUtils.hashGetAll(reportKey);
            if (data != null && !data.isEmpty()) {
                result.add(castMap(data));
            }
        }
        return result;
    }

    @Override
    public void handleReport(Long reportId, Long reviewerId, String note) {
        String reportKey = ModerationConstants.MOD_REPORT_PREFIX + reportId;
        redisUtils.hashPut(reportKey, "status", "HANDLED");
        redisUtils.hashPut(reportKey, "reviewerId", String.valueOf(reviewerId));
        redisUtils.hashPut(reportKey, "reviewNote", note != null ? note : "");
        redisUtils.hashPut(reportKey, "reviewTime", String.valueOf(Instant.now().toEpochMilli()));
        log.info("Report {} handled by reviewer {}", reportId, reviewerId);
    }

    // ---- Content visibility helpers ----

    /**
     * Hides content by setting its status field to {@code 1} (hidden/locked).
     * Works for Post and Comment; novel chapters don't have a status field so
     * hiding is tracked only in the moderation queue.
     */
    private void hideContent(ContentType type, Long contentId) {
        switch (type) {
            case POST -> postMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Post>()
                            .eq(Post::getId, contentId)
                            .set(Post::getStatus, 1));
            case COMMENT -> commentMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Comment>()
                            .eq(Comment::getId, contentId)
                            .set(Comment::getStatus, 1));
            default -> log.debug("hideContent: no status field for type={}", type);
        }
    }

    /**
     * Restores content visibility by setting its status field back to {@code 0}.
     */
    private void restoreContent(ContentType type, Long contentId) {
        switch (type) {
            case POST -> postMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Post>()
                            .eq(Post::getId, contentId)
                            .set(Post::getStatus, 0));
            case COMMENT -> commentMapper.update(null,
                    new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Comment>()
                            .eq(Comment::getId, contentId)
                            .set(Comment::getStatus, 0));
            default -> log.debug("restoreContent: no status field for type={}", type);
        }
    }

    private void saveReviewRecord(ContentType type, Long contentId, Long reviewerId,
                                  String decision, String reviewType, String reason) {
        String key = ModerationConstants.MOD_REVIEWED_PREFIX + type.name() + ":" + contentId;
        redisUtils.hashPut(key, "decision", decision);
        redisUtils.hashPut(key, "reviewerId", String.valueOf(reviewerId));
        redisUtils.hashPut(key, "reviewType", reviewType);
        redisUtils.hashPut(key, "reason", reason != null ? reason : "");
        redisUtils.hashPut(key, "reviewTime", String.valueOf(Instant.now().toEpochMilli()));

        // Clean up flagged metadata
        redisUtils.delete(ModerationConstants.MOD_FLAGGED_PREFIX + type.name() + ":" + contentId);
        // Remove from pending queue (ZSet)
        String queueKey = ModerationConstants.MOD_QUEUE_PREFIX + type.name();
        redisUtils.zsetRemove(queueKey, String.valueOf(contentId));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Map<Object, Object> source) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<Object, Object> e : source.entrySet()) {
            result.put(String.valueOf(e.getKey()), e.getValue());
        }
        return result;
    }
}
