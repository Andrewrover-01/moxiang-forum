package com.moxiang.service.moderation;

/**
 * Text normalizer that improves detection of variant and homophone evasion.
 *
 * <h3>Transformations applied (in order)</h3>
 * <ol>
 *   <li>Strip invisible/zero-width control characters that are used to split words.</li>
 *   <li>Map full-width ASCII characters (ＡＢＣ…, ０１２…, ！＂＃…) to their
 *       half-width equivalents.</li>
 *   <li>Apply a character substitution table for common visual/phonetic replacements
 *       (e.g., {@code 0 → o}, {@code 1 → i}, {@code 3 → e}, {@code ① → 1},
 *       {@code ¥ → y}, {@code $ → s}, etc.).</li>
 *   <li>Convert to lowercase so all Trie lookups are case-insensitive.</li>
 * </ol>
 *
 * <p>The returned string is used <em>only</em> for matching; original user text is
 * preserved for storage and display.
 */
public final class VariantNormalizer {

    private VariantNormalizer() {}

    // ---- Invisible/zero-width characters to strip ----
    private static final String INVISIBLE_CHARS =
            "\u200B\u200C\u200D\u200E\u200F\uFEFF\u00AD\u2060\u2061\u2062\u2063\u2064"
            + "\u034F\u115F\u1160\u17B4\u17B5\uFFA0";

    // ---- Character substitution map for common evasion patterns ----
    // This is an approximation covering the most common techniques; it does NOT
    // attempt exhaustive unicode look-alike coverage.
    private static final char[] SUB_FROM = {
        // ASCII digits used as letter stand-ins
        '0', '1', '3', '4', '5',
        // circled/enclosed digits (①–⑨; ⑩ is not a single char mapping)
        '①', '②', '③', '④', '⑤', '⑥', '⑦', '⑧', '⑨',
        // common currency / symbol evasions
        '$', '¥', '@',
        // common punctuation used as letter separators — NOT stripped (handled by Trie skip),
        // but we do map a few that look like letters
        'ℓ', 'ρ', 'α', 'β', 'γ', 'δ', 'ε', 'ζ', 'η', 'θ',
        // Greek/Latin lookalikes
        'Α', 'Β', 'Ε', 'Ζ', 'Η', 'Ι', 'Κ', 'Μ', 'Ν', 'Ο', 'Ρ', 'Τ', 'Υ', 'Χ',
    };
    private static final char[] SUB_TO = {
        'o', 'i', 'e', 'a', 's',
        '1', '2', '3', '4', '5', '6', '7', '8', '9',
        's', 'y', 'a',
        'l', 'p', 'a', 'b', 'g', 'd', 'e', 'z', 'n', 'o',
        'a', 'b', 'e', 'z', 'h', 'i', 'k', 'm', 'n', 'o', 'p', 't', 'u', 'x',
    };

    static {
        if (SUB_FROM.length != SUB_TO.length) {
            throw new ExceptionInInitializerError(
                    "VariantNormalizer: SUB_FROM and SUB_TO lengths differ");
        }
    }

    /**
     * Normalizes {@code text} for sensitive-word matching.
     *
     * @param text raw user input (may be {@code null})
     * @return normalized string safe for Trie scanning; never {@code null}
     */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // 1. Skip invisible/zero-width chars
            if (isInvisible(c)) {
                continue;
            }

            // 2. Full-width ASCII → half-width  (U+FF01–U+FF5E → U+0021–U+007E)
            if (c >= '\uFF01' && c <= '\uFF5E') {
                c = (char) (c - 0xFEE0);
            }
            // Full-width space → regular space
            if (c == '\u3000') {
                c = ' ';
            }

            // 3. Apply substitution table
            c = applySubstitution(c);

            // 4. Lowercase
            c = Character.toLowerCase(c);

            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Returns {@code true} if {@code c} is a noise character that can be safely
     * skipped during Trie traversal <em>when already inside a partial match</em>.
     *
     * <p>This enables detection of insertions like "赌 博" and "色。情" which match
     * "赌博" and "色情" respectively.
     *
     * <p>We do <em>not</em> skip printable ASCII letters, digits, or CJK characters —
     * only separators and punctuation.
     */
    public static boolean isSkippable(char c) {
        if (Character.isLetterOrDigit(c)) {
            return false;
        }
        // CJK Unified Ideographs
        if (c >= '\u4E00' && c <= '\u9FFF') {
            return false;
        }
        // CJK Extension A / B ranges
        if (c >= '\u3400' && c <= '\u4DBF') {
            return false;
        }
        // Allow various punctuation, space, and control chars to be skipped
        return true;
    }

    // ---- Private helpers ----

    private static boolean isInvisible(char c) {
        return INVISIBLE_CHARS.indexOf(c) >= 0;
    }

    private static char applySubstitution(char c) {
        for (int i = 0; i < SUB_FROM.length; i++) {
            if (c == SUB_FROM[i]) {
                return SUB_TO[i];
            }
        }
        return c;
    }
}
