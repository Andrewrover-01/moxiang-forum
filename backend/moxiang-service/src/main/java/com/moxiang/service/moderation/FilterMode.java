package com.moxiang.service.moderation;

/**
 * Filter mode controlling which word libraries are active during a check.
 *
 * <ul>
 *   <li>{@link #NORMAL} — BASE + CUSTOM libraries; used for forum posts and comments.</li>
 *   <li>{@link #STRICT} — BASE + NOVEL + CUSTOM libraries; used for novel chapters
 *       where industry-specific prohibited content requires stricter scrutiny.</li>
 * </ul>
 */
public enum FilterMode {

    /** Base vocabulary + custom admin-added words. */
    NORMAL,

    /** Base vocabulary + novel-industry vocabulary + custom admin-added words. */
    STRICT
}
