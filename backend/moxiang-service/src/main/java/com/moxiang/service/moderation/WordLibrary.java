package com.moxiang.service.moderation;

/**
 * Classification of a sensitive word by its originating library.
 *
 * <p>Words from multiple libraries are combined into a single Trie for each
 * {@link FilterMode}, but the library tag is preserved for admin reporting.
 */
public enum WordLibrary {

    /** General sensitive words: illegal activity, political, extremism, fraud, etc. */
    BASE,

    /** Novel/fiction-industry specific words: adult content specific to fiction platforms. */
    NOVEL,

    /** Custom words added by administrators at runtime (stored in Redis). */
    CUSTOM
}
