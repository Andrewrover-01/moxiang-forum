package com.moxiang.common.utils;

import java.util.regex.Pattern;

/**
 * Utility class for stripping potentially dangerous HTML / script content from
 * user-supplied text (XSS prevention).
 *
 * <p>The cleaner is intentionally conservative: it removes HTML tags, JavaScript
 * event handlers, and common injection vectors while preserving plain text, so
 * forum content remains readable.
 *
 * <p><b>Note:</b> This is a lightweight defence layer applied <em>before</em>
 * persistence.  Output encoding at render time (handled by the frontend) is
 * still the primary XSS defence.
 */
public final class XssUtils {

    private XssUtils() {}

    // ---- Pre-compiled patterns ----

    /** Matches HTML/XML open or close tags: <tag ...> or </tag> */
    private static final Pattern HTML_TAGS =
            Pattern.compile("<[^>]*>", Pattern.DOTALL);

    /** Matches inline event handlers: onXxx=... */
    private static final Pattern EVENT_HANDLERS =
            Pattern.compile("\\bon\\w+\\s*=", Pattern.CASE_INSENSITIVE);

    /** Matches javascript: URI scheme */
    private static final Pattern JS_URI =
            Pattern.compile("javascript\\s*:", Pattern.CASE_INSENSITIVE);

    /** Matches vbscript: URI scheme */
    private static final Pattern VBS_URI =
            Pattern.compile("vbscript\\s*:", Pattern.CASE_INSENSITIVE);

    /** Matches expression(...) CSS injection */
    private static final Pattern CSS_EXPRESSION =
            Pattern.compile("expression\\s*\\(", Pattern.CASE_INSENSITIVE);

    /**
     * Returns a sanitized copy of the input string with HTML tags and common XSS
     * vectors removed.  Returns {@code null} if the input is {@code null}.
     *
     * @param input raw user-supplied string
     * @return sanitized string
     */
    public static String clean(String input) {
        if (input == null) {
            return null;
        }
        String result = input;
        result = HTML_TAGS.matcher(result).replaceAll("");
        result = EVENT_HANDLERS.matcher(result).replaceAll("");
        result = JS_URI.matcher(result).replaceAll("");
        result = VBS_URI.matcher(result).replaceAll("");
        result = CSS_EXPRESSION.matcher(result).replaceAll("");
        return result;
    }

    /**
     * Returns {@code true} if the input contains HTML tags or common XSS vectors
     * (i.e., if {@link #clean(String)} would change the value).
     */
    public static boolean isDirty(String input) {
        if (input == null) {
            return false;
        }
        return !input.equals(clean(input));
    }
}
