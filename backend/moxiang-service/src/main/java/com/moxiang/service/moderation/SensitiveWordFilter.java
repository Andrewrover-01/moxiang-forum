package com.moxiang.service.moderation;

import com.moxiang.common.constant.ModerationConstants;
import com.moxiang.common.utils.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Sensitive-word filter used by the machine review stage.
 *
 * <h3>Keyword sources</h3>
 * <ol>
 *   <li><b>Default keywords</b> — a small hardcoded set loaded on first use.</li>
 *   <li><b>Dynamic keywords</b> — stored in the Redis Set
 *       {@value ModerationConstants#MOD_KEYWORDS_KEY}; managed via the admin API.</li>
 * </ol>
 *
 * <p>Keywords are matched case-insensitively against the input text.
 */
@Component
public class SensitiveWordFilter {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordFilter.class);

    /**
     * Default sensitive keywords.  These are loaded into Redis on the first call
     * to {@link #ensureDefaultsLoaded()} if the Redis set is empty.
     */
    private static final String[] DEFAULT_KEYWORDS = {
        "赌博", "诈骗", "博彩", "黄色", "色情", "裸露", "毒品", "贩毒",
        "枪支", "炸弹", "恐怖", "暴恐", "洗钱", "传销", "非法",
        "儿童色情", "未成年色情", "侮辱", "谩骂", "人身攻击",
        "spam", "advertisement"
    };

    private final RedisUtils redisUtils;

    /** Guard so we only populate defaults once per process start. */
    private volatile boolean defaultsLoaded = false;

    public SensitiveWordFilter(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    /**
     * Scans {@code text} for sensitive keywords.
     *
     * @param text user-supplied text (may be {@code null})
     * @return {@link MachineReviewResult#pass()} if clean,
     *         {@link MachineReviewResult#flagged(String)} if a keyword is matched
     */
    public MachineReviewResult check(String text) {
        if (text == null || text.isBlank()) {
            return MachineReviewResult.pass();
        }
        ensureDefaultsLoaded();
        String lowerText = text.toLowerCase();

        Set<Object> keywords = redisUtils.setMembers(ModerationConstants.MOD_KEYWORDS_KEY);
        if (keywords == null) {
            return MachineReviewResult.pass();
        }
        for (Object kw : keywords) {
            String keyword = String.valueOf(kw).toLowerCase();
            if (lowerText.contains(keyword)) {
                log.info("Sensitive keyword matched: '{}'", kw);
                return MachineReviewResult.flagged(String.valueOf(kw));
            }
        }
        return MachineReviewResult.pass();
    }

    /**
     * Adds a keyword to the active keyword set.
     */
    public void addKeyword(String keyword) {
        redisUtils.setAdd(ModerationConstants.MOD_KEYWORDS_KEY, keyword.toLowerCase().trim());
    }

    /**
     * Removes a keyword from the active keyword set.
     */
    public void removeKeyword(String keyword) {
        redisUtils.setRemove(ModerationConstants.MOD_KEYWORDS_KEY, keyword.toLowerCase().trim());
    }

    /**
     * Returns all currently active keywords.
     */
    public Set<Object> listKeywords() {
        ensureDefaultsLoaded();
        return redisUtils.setMembers(ModerationConstants.MOD_KEYWORDS_KEY);
    }

    // ---- Helpers ----

    private void ensureDefaultsLoaded() {
        if (!defaultsLoaded) {
            synchronized (this) {
                if (!defaultsLoaded) {
                    long existing = redisUtils.setSize(ModerationConstants.MOD_KEYWORDS_KEY);
                    if (existing == 0) {
                        Object[] keywords = new Object[DEFAULT_KEYWORDS.length];
                        for (int i = 0; i < DEFAULT_KEYWORDS.length; i++) {
                            keywords[i] = DEFAULT_KEYWORDS[i];
                        }
                        redisUtils.setAdd(ModerationConstants.MOD_KEYWORDS_KEY, keywords);
                        log.info("Loaded {} default sensitive keywords", DEFAULT_KEYWORDS.length);
                    }
                    defaultsLoaded = true;
                }
            }
        }
    }
}
