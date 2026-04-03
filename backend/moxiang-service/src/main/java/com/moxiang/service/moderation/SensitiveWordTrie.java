package com.moxiang.service.moderation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-safe DFA (Trie) for multi-pattern sensitive-word detection.
 *
 * <h3>Algorithm</h3>
 * <p>A standard Trie (prefix tree) is built from a collection of patterns.
 * For a text of length <em>n</em> and a total pattern set whose characters
 * sum to <em>P</em>, matching runs in O(n × L_max) worst case, where
 * L_max is the length of the longest pattern — far better than the naïve
 * O(n × m) string-contains approach when there are many keywords.
 *
 * <p>During traversal the scanner also skips "noise" characters (punctuation,
 * spaces) that appear <em>inside</em> a partial match, allowing detection of
 * evasion patterns like "赌 博" or "色，情".
 *
 * <h3>Thread-safety</h3>
 * <p>The internal Trie root is updated via a volatile reference.  {@link #rebuild}
 * constructs a completely new tree then atomically swaps the root, so concurrent
 * readers always see a consistent (possibly slightly stale) tree.  No locking is
 * required for {@link #findFirst} or {@link #replaceAll}.
 */
public final class SensitiveWordTrie {

    // ---- Inner node ----

    private static final class Node {
        final Map<Character, Node> children = new HashMap<>(4);
        boolean isEnd;
        /** The complete keyword that ends at this node (for reporting). */
        String keyword;
    }

    // Volatile so readers always see a fully constructed new root after rebuild.
    private volatile Node root = new Node();

    /**
     * Rebuilds the Trie from {@code words}, atomically replacing the old root.
     * Safe to call at any time; concurrent scanners continue using the old root
     * until this method returns.
     *
     * @param words collection of (already-normalized) sensitive words
     */
    public void rebuild(Collection<String> words) {
        Node newRoot = new Node();
        for (String word : words) {
            if (word == null || word.isBlank()) continue;
            insert(newRoot, word);
        }
        this.root = newRoot;   // atomic volatile write
    }

    /**
     * Returns the first sensitive keyword found in {@code normalizedText}, or
     * {@code null} if the text is clean.
     *
     * @param normalizedText text that has already been processed by
     *                       {@link VariantNormalizer#normalize(String)}
     */
    public String findFirst(String normalizedText) {
        if (normalizedText == null || normalizedText.isEmpty()) return null;
        Node r = root;   // snapshot current root (volatile read)
        int len = normalizedText.length();

        for (int i = 0; i < len; i++) {
            String hit = scanFrom(r, normalizedText, i, len);
            if (hit != null) return hit;
        }
        return null;
    }

    /**
     * Returns {@code text} with every sensitive keyword replaced by {@code '★'}
     * characters of the same length.  The input is first normalised and matched,
     * but replacement is performed on the <em>original</em> (un-normalised) text
     * at the same character positions.
     *
     * <p>Note: because normalisation may shorten the string (by stripping
     * invisible chars) the position mapping is approximate when invisible
     * characters are present.  For the purposes of this filter the goal is
     * best-effort masking, not byte-accurate reconstruction.
     *
     * @param originalText the user-supplied original text
     * @return text with sensitive parts masked, or the original text if clean
     */
    public String replaceAll(String originalText) {
        if (originalText == null || originalText.isBlank()) return originalText;

        String normalized = VariantNormalizer.normalize(originalText);
        Node r = root;
        int len = normalized.length();

        // Build a boolean mask over the *normalized* positions
        boolean[] masked = new boolean[len];
        int i = 0;
        while (i < len) {
            int end = scanFromWithEnd(r, normalized, i, len);
            if (end > i) {
                for (int k = i; k < end; k++) {
                    masked[k] = true;
                }
                i = end;
            } else {
                i++;
            }
        }

        // Apply mask to the *original* text using the normalized positions as a guide.
        // We walk both strings in parallel, skipping invisible chars in original.
        StringBuilder result = new StringBuilder(originalText.length());
        int ni = 0;  // position in normalized
        for (int oi = 0; oi < originalText.length(); oi++) {
            char c = originalText.charAt(oi);
            if (isStrippedByNormalizer(c)) {
                // Invisible chars have no position in normalized — mask them if current
                // normalized position is inside a masked region
                if (ni > 0 && ni <= len && masked[ni - 1]) {
                    result.append('★');
                } else {
                    result.append(c);
                }
            } else if (VariantNormalizer.isSkippable(c)) {
                // Skippable (punctuation/space): mask it when sitting inside a masked region
                if (ni < len && masked[ni]) {
                    result.append('★');
                } else {
                    result.append(c);
                }
                // Skippable chars are kept in normalized, so advance ni
                ni++;
            } else if (ni < len && masked[ni]) {
                result.append('★');
                ni++;
            } else {
                result.append(c);
                if (ni < len) {
                    ni++;
                }
            }
        }
        return result.toString();
    }

    // ---- Private helpers ----

    private static void insert(Node root, String word) {
        Node cur = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            cur = cur.children.computeIfAbsent(c, k -> new Node());
        }
        cur.isEnd = true;
        cur.keyword = word;
    }

    /**
     * Attempts a match starting at position {@code start} in {@code text}.
     * Returns the matched keyword string, or {@code null} if no match.
     */
    private static String scanFrom(Node root, String text, int start, int len) {
        Node cur = root;
        int depth = 0;   // number of Trie chars actually consumed

        for (int i = start; i < len; i++) {
            char c = text.charAt(i);

            // Skip noise chars when we are already inside a partial match
            if (depth > 0 && VariantNormalizer.isSkippable(c)) {
                continue;
            }

            Node next = cur.children.get(c);
            if (next == null) {
                // No match can start here
                return null;
            }
            cur = next;
            depth++;
            if (cur.isEnd) {
                return cur.keyword;
            }
        }
        return null;
    }

    /**
     * Like {@link #scanFrom} but returns the end position (exclusive) of the
     * match in the text, or {@code start} if no match starts there.
     */
    private static int scanFromWithEnd(Node root, String text, int start, int len) {
        Node cur = root;
        int lastMatchEnd = -1;
        int depth = 0;

        for (int i = start; i < len; i++) {
            char c = text.charAt(i);

            if (depth > 0 && VariantNormalizer.isSkippable(c)) {
                continue;
            }

            Node next = cur.children.get(c);
            if (next == null) {
                return lastMatchEnd > 0 ? lastMatchEnd : start;
            }
            cur = next;
            depth++;
            if (cur.isEnd) {
                lastMatchEnd = i + 1;
            }
        }
        return lastMatchEnd > 0 ? lastMatchEnd : start;
    }

    /**
     * Returns {@code true} if the normalizer would strip this character entirely
     * (invisible chars), meaning it has no corresponding position in the normalized string.
     */
    private static boolean isStrippedByNormalizer(char c) {
        // The normalizer only strips invisible chars; for everything else it keeps 1:1 mapping
        return c == '\u200B' || c == '\u200C' || c == '\u200D' || c == '\u200E'
                || c == '\u200F' || c == '\uFEFF' || c == '\u00AD' || c == '\u2060'
                || c == '\u2061' || c == '\u2062' || c == '\u2063' || c == '\u2064'
                || c == '\u034F' || c == '\u115F' || c == '\u1160' || c == '\u17B4'
                || c == '\u17B5' || c == '\uFFA0';
    }
}
