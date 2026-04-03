package com.moxiang.service.moderation;

import com.moxiang.common.constant.ModerationConstants;
import com.moxiang.common.utils.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * DFA/Trie-based sensitive-word filter for the content moderation pipeline.
 *
 * <h3>Word libraries</h3>
 * <ol>
 *   <li><b>BASE</b> — hardcoded general-purpose keywords (illegal activity, fraud, extremism,
 *       political risk, etc.); always active.</li>
 *   <li><b>NOVEL</b> — hardcoded novel/fiction-industry specific keywords (adult content
 *       terms relevant to online fiction platforms); active in {@link FilterMode#STRICT} only.</li>
 *   <li><b>CUSTOM</b> — admin-managed keywords stored in two Redis Sets:
 *       {@value ModerationConstants#MOD_KEYWORDS_KEY} (general) and
 *       {@value ModerationConstants#MOD_KEYWORDS_NOVEL_KEY} (novel-specific).
 *       Custom keywords in the novel set are also only active in STRICT mode.</li>
 * </ol>
 *
 * <h3>Algorithm</h3>
 * <p>Two {@link SensitiveWordTrie} instances are maintained:
 * <ul>
 *   <li>{@code normalTrie} — BASE + CUSTOM-general</li>
 *   <li>{@code strictTrie} — BASE + NOVEL + CUSTOM-general + CUSTOM-novel</li>
 * </ul>
 * Tries are rebuilt atomically whenever the keyword corpus changes.
 *
 * <h3>Variant / homophone detection</h3>
 * <p>Before scanning, {@link VariantNormalizer#normalize(String)} is applied to the input.
 * During Trie traversal, noise characters (punctuation, spaces) between matched characters
 * are skipped, enabling detection of patterns like "赌 博" or "色。情".
 */
@Component
public class SensitiveWordFilter {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordFilter.class);

    // ---- BASE word library (general — always active) ----
    private static final String[] BASE_WORDS = {
        // Illegal activity & fraud
        "赌博", "诈骗", "博彩", "洗钱", "传销", "非法集资", "贩毒", "毒品",
        "冰毒", "大麻", "海洛因", "可卡因", "摇头丸", "枪支", "弹药", "炸药",
        "炸弹", "爆炸物", "恐怖袭击", "恐怖主义", "暴恐",
        // Adult / obscene (general)
        "黄色", "色情", "裸露", "淫秽", "猥亵", "卖淫", "嫖娼", "援交",
        "儿童色情", "未成年色情", "loli色情",
        // Insult / harassment
        "侮辱", "谩骂", "人身攻击", "仇恨言论",
        // Spam
        "spam", "advertisement", "广告刷屏",
        // Self-harm
        "自杀方法", "割腕方法",
    };

    // ---- NOVEL word library (fiction-platform specific — STRICT mode only) ----
    private static final String[] NOVEL_WORDS = {
        // Explicit sexual content keywords common in online fiction
        "性爱", "做爱", "性交", "强奸", "轮奸", "群交", "肛交", "口交",
        "自慰", "手淫", "阴茎", "阴道", "阴蒂", "乳头", "射精", "高潮",
        "插入", "抽插", "下体", "私处", "胸部裸露", "全裸", "半裸写真",
        // Underage content in fiction
        "萝莉文", "幼女文", "未成年情色", "初中生性",
        // Violence + graphic content specific to fiction
        "血腥虐杀", "变态杀人", "连环杀手", "人体实验",
    };

    private final RedisUtils redisUtils;

    /** Normal-mode Trie: BASE + CUSTOM-general. */
    private volatile SensitiveWordTrie normalTrie;
    /** Strict-mode Trie: BASE + NOVEL + CUSTOM-general + CUSTOM-novel. */
    private volatile SensitiveWordTrie strictTrie;

    /** Guard for one-time initialisation of default keywords. */
    private volatile boolean initialized = false;

    public SensitiveWordFilter(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    // ============================  Public API  ============================

    /**
     * Checks {@code text} using the {@link FilterMode#NORMAL} filter.
     *
     * @param text user-supplied text (may be {@code null})
     * @return {@link MachineReviewResult#pass()} if clean, otherwise flagged
     */
    public MachineReviewResult check(String text) {
        return checkWithMode(text, FilterMode.NORMAL);
    }

    /**
     * Checks {@code text} using the specified {@link FilterMode}.
     *
     * @param text user-supplied text (may be {@code null})
     * @param mode filter mode selecting which libraries to apply
     * @return {@link MachineReviewResult#pass()} if clean, otherwise flagged with the
     *         first matched keyword
     */
    public MachineReviewResult checkWithMode(String text, FilterMode mode) {
        if (text == null || text.isBlank()) {
            return MachineReviewResult.pass();
        }
        ensureInitialized();

        String normalized = VariantNormalizer.normalize(text);
        SensitiveWordTrie trie = mode == FilterMode.STRICT ? getStrictTrie() : getNormalTrie();
        String hit = trie.findFirst(normalized);

        if (hit != null) {
            log.info("Sensitive keyword matched [mode={}]: '{}'", mode, hit);
            return MachineReviewResult.flagged(hit);
        }
        return MachineReviewResult.pass();
    }

    /**
     * Returns {@code text} with every sensitive keyword replaced by {@code '★'}
     * characters.  Uses {@link FilterMode#NORMAL}.
     *
     * @param text original user text
     * @return masked text, or original if no keyword was found
     */
    public String replace(String text) {
        return replaceWithMode(text, FilterMode.NORMAL);
    }

    /**
     * Returns {@code text} with every sensitive keyword (for the given mode) replaced by
     * {@code '★'} characters.
     *
     * @param text original user text
     * @param mode filter mode
     * @return masked text, or original if no keyword was found
     */
    public String replaceWithMode(String text, FilterMode mode) {
        if (text == null || text.isBlank()) return text;
        ensureInitialized();
        SensitiveWordTrie trie = mode == FilterMode.STRICT ? getStrictTrie() : getNormalTrie();
        return trie.replaceAll(text);
    }

    // ---- Keyword management ----

    /**
     * Adds a custom keyword to the general (CUSTOM) library and rebuilds both tries.
     */
    public void addKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        String normalized = VariantNormalizer.normalize(keyword.trim());
        redisUtils.setAdd(ModerationConstants.MOD_KEYWORDS_KEY, normalized);
        rebuildTries();
        log.info("Custom keyword added: '{}'", keyword.trim());
    }

    /**
     * Removes a keyword from the general (CUSTOM) library and rebuilds both tries.
     */
    public void removeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        String normalized = VariantNormalizer.normalize(keyword.trim());
        redisUtils.setRemove(ModerationConstants.MOD_KEYWORDS_KEY, normalized);
        // Also try the original form for backward-compatibility
        redisUtils.setRemove(ModerationConstants.MOD_KEYWORDS_KEY, keyword.toLowerCase().trim());
        rebuildTries();
        log.info("Custom keyword removed: '{}'", keyword.trim());
    }

    /**
     * Adds a novel-specific keyword to the NOVEL custom library and rebuilds tries.
     */
    public void addNovelKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        String normalized = VariantNormalizer.normalize(keyword.trim());
        redisUtils.setAdd(ModerationConstants.MOD_KEYWORDS_NOVEL_KEY, normalized);
        rebuildTries();
        log.info("Novel keyword added: '{}'", keyword.trim());
    }

    /**
     * Removes a novel-specific keyword from the NOVEL custom library and rebuilds tries.
     */
    public void removeNovelKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return;
        String normalized = VariantNormalizer.normalize(keyword.trim());
        redisUtils.setRemove(ModerationConstants.MOD_KEYWORDS_NOVEL_KEY, normalized);
        redisUtils.setRemove(ModerationConstants.MOD_KEYWORDS_NOVEL_KEY,
                keyword.toLowerCase().trim());
        rebuildTries();
        log.info("Novel keyword removed: '{}'", keyword.trim());
    }

    /**
     * Returns all active custom keywords in the general library.
     */
    public Set<Object> listKeywords() {
        ensureInitialized();
        return redisUtils.setMembers(ModerationConstants.MOD_KEYWORDS_KEY);
    }

    /**
     * Returns all active custom keywords in the novel library.
     */
    public Set<Object> listNovelKeywords() {
        ensureInitialized();
        return redisUtils.setMembers(ModerationConstants.MOD_KEYWORDS_NOVEL_KEY);
    }

    /**
     * Forces an immediate Trie rebuild from the current Redis state.
     * Useful after bulk keyword imports.
     */
    public void refreshTries() {
        ensureInitialized();
        rebuildTries();
        log.info("Tries rebuilt on demand");
    }

    // ============================  Private helpers  ============================

    /** Returns the lazy-initialized normal-mode Trie. */
    private SensitiveWordTrie getNormalTrie() {
        if (normalTrie == null) {
            synchronized (this) {
                if (normalTrie == null) rebuildTries();
            }
        }
        return normalTrie;
    }

    /** Returns the lazy-initialized strict-mode Trie. */
    private SensitiveWordTrie getStrictTrie() {
        if (strictTrie == null) {
            synchronized (this) {
                if (strictTrie == null) rebuildTries();
            }
        }
        return strictTrie;
    }

    /**
     * Rebuilds both tries from the current corpus.  Called on startup and on every
     * keyword mutation.  Thread-safe: new Trie instances are built first, then
     * atomically swapped via volatile writes.
     */
    private synchronized void rebuildTries() {
        List<String> customGeneral = redisSetToList(
                redisUtils.setMembers(ModerationConstants.MOD_KEYWORDS_KEY));
        List<String> customNovel = redisSetToList(
                redisUtils.setMembers(ModerationConstants.MOD_KEYWORDS_NOVEL_KEY));

        // Normal Trie: BASE + CUSTOM-general
        List<String> normalWords = new ArrayList<>(BASE_WORDS.length + customGeneral.size());
        for (String w : BASE_WORDS) {
            normalWords.add(VariantNormalizer.normalize(w));
        }
        normalWords.addAll(customGeneral);

        SensitiveWordTrie newNormal = new SensitiveWordTrie();
        newNormal.rebuild(normalWords);

        // Strict Trie: BASE + NOVEL + CUSTOM-general + CUSTOM-novel
        List<String> strictWords = new ArrayList<>(
                normalWords.size() + NOVEL_WORDS.length + customNovel.size());
        strictWords.addAll(normalWords);
        for (String w : NOVEL_WORDS) {
            strictWords.add(VariantNormalizer.normalize(w));
        }
        strictWords.addAll(customNovel);

        SensitiveWordTrie newStrict = new SensitiveWordTrie();
        newStrict.rebuild(strictWords);

        // Atomic volatile writes
        this.normalTrie = newNormal;
        this.strictTrie = newStrict;

        log.debug("Tries rebuilt: normalWords={} strictWords={}",
                normalWords.size(), strictWords.size());
    }

    /**
     * Ensures default keyword state is set up in Redis (idempotent — only runs once
     * per process start if Redis sets are empty).
     */
    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    // No defaults to push — all base/novel words are hardcoded in Trie
                    // Rebuild tries to incorporate any existing Redis custom words
                    rebuildTries();
                    initialized = true;
                    log.info("SensitiveWordFilter initialized: base={} novel={}",
                            BASE_WORDS.length, NOVEL_WORDS.length);
                }
            }
        }
    }

    private static List<String> redisSetToList(Set<Object> set) {
        if (set == null) return new ArrayList<>();
        List<String> list = new ArrayList<>(set.size());
        for (Object o : set) {
            if (o != null) list.add(String.valueOf(o));
        }
        return list;
    }
}

