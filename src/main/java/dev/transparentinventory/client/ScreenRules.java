package dev.transparentinventory.client;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Screen-name rules shared by the config and the spatial screen registry.
 *
 * <p>Screen names are fully qualified class names. A rule may contain {@code *}
 * (any run of characters, including none) or {@code ?} (exactly one character),
 * so a single entry such as {@code "*MerchantScreen"} or {@code
 * "net.minecraft.client.gui.screens.inventory.*"} can cover a family of screens.
 *
 * <p>Plain Java only: unit tests drive this class without any Minecraft type.
 */
public final class ScreenRules {

    /** Longest rule accepted from a config file; keeps matching and logs bounded. */
    public static final int MAX_NAME_LENGTH = 256;

    private ScreenRules() {
    }

    /** True when the rule contains a wildcard and must be matched instead of compared. */
    public static boolean isPattern(String rule) {
        if (rule == null) return false;
        for (int i = 0; i < rule.length(); i++) {
            char c = rule.charAt(i);
            if (c == '*' || c == '?') return true;
        }
        return false;
    }

    /**
     * Glob match with {@code *}/{@code ?}. Linear in the name with at most one
     * backtracking point per star, so a pathological rule cannot blow up.
     */
    public static boolean matches(String rule, String name) {
        if (rule == null || name == null) return false;
        int r = 0, n = 0;
        int star = -1, mark = 0;
        while (n < name.length()) {
            if (r < rule.length() && (rule.charAt(r) == '?' || rule.charAt(r) == name.charAt(n))) {
                r++;
                n++;
            } else if (r < rule.length() && rule.charAt(r) == '*') {
                star = r++;
                mark = n;
            } else if (star >= 0) {
                r = star + 1;
                n = ++mark;
            } else {
                return false;
            }
        }
        while (r < rule.length() && rule.charAt(r) == '*') r++;
        return r == rule.length();
    }

    /** True when any entry matches the name; exact entries and patterns are both accepted. */
    public static boolean matchesAny(Iterable<String> rules, String name) {
        if (rules == null || name == null) return false;
        for (String rule : rules) {
            if (rule == null || rule.isEmpty()) continue;
            if (rule.equals(name) || (isPattern(rule) && matches(rule, name))) return true;
        }
        return false;
    }

    /**
     * The override key that best describes this screen: an exact key wins, then the
     * matching pattern with the most literal (non-wildcard) characters. Ties keep
     * the earliest entry, matching the insertion order of the saved JSON.
     */
    public static String bestKey(Map<String, Float> overrides, String name) {
        if (overrides == null || name == null) return null;
        if (overrides.containsKey(name)) return name;
        String best = null;
        int bestLiteral = -1;
        for (String rule : overrides.keySet()) {
            if (rule == null || !isPattern(rule) || !matches(rule, name)) continue;
            int literal = literalCount(rule);
            if (literal > bestLiteral) {
                bestLiteral = literal;
                best = rule;
            }
        }
        return best;
    }

    /** Effective opacity for a screen, falling back to the global panel opacity. */
    public static float opacity(Map<String, Float> overrides, String name, float fallback) {
        String key = bestKey(overrides, name);
        if (key == null) return fallback;
        Float value = overrides.get(key);
        return value == null ? fallback : value;
    }

    /** True when the screen has an exact override rather than inheriting from a pattern. */
    public static boolean hasExactOverride(Map<String, Float> overrides, String name) {
        return overrides != null && name != null && overrides.containsKey(name);
    }

    /** Number of literal characters in a rule; more literals means a more specific pattern. */
    public static int literalCount(String rule) {
        if (rule == null) return 0;
        int count = 0;
        for (int i = 0; i < rule.length(); i++) {
            char c = rule.charAt(i);
            if (c != '*' && c != '?') count++;
        }
        return count;
    }

    /** Trims a rule and rejects blanks or absurdly long entries. Returns null for invalid input. */
    public static String cleanName(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.length() > MAX_NAME_LENGTH ? null : trimmed;
    }

    /** Trimmed, de-duplicated, blank-free copy of a rule list. */
    public static List<String> cleanList(List<String> values) {
        if (values == null) return new ArrayList<>();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String clean = cleanName(value);
            if (clean != null) unique.add(clean);
        }
        return new ArrayList<>(unique);
    }
}
