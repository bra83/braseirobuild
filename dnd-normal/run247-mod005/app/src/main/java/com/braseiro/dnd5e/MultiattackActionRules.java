package com.braseiro.dnd5e;

import java.util.Locale;

/** Validates requested monster attack components against structured statblock plans. */
public final class MultiattackActionRules {
    private MultiattackActionRules() {}

    /**
     * Returns true when a structured declaration authorizes this exact sequence.
     * A declaration with no machine-readable plan returns false so callers can
     * keep it on their existing manual path instead of guessing at prose.
     */
    /** Fail closed: an attack count alone never authorizes an arbitrary sequence. */
    public static String declarationBlock(String[] sequence, int count,
            String fixedAction, String options) {
        if (count < 2 || count > 8) return "invalid_multiattack_count";
        if (sequence != null) {
            if (sequence.length != count) return "invalid_multiattack_sequence_length";
            for (String action : sequence) if (blank(action)) return "invalid_multiattack_sequence";
            return "";
        }
        if (!blank(fixedAction)) return "";
        if (blank(options)) return "multiattack_requires_explicit_components";
        String[] allowed = options.split("\\|", -1);
        for (String action : allowed) if (blank(action)) return "invalid_multiattack_options";
        return "";
    }

    public static boolean matches(String[] requested, String[] sequence,
            int count, String fixedAction, String options) {
        if (requested == null || requested.length != count ||
                !declarationBlock(sequence, count, fixedAction, options).isEmpty()) return false;
        if (sequence != null) {
            for (int i = 0; i < sequence.length; i++)
                if (!same(sequence[i], requested[i])) return false;
            return true;
        }
        if (!blank(fixedAction)) {
            for (String action : requested) if (!same(fixedAction, action)) return false;
            return true;
        }
        String[] allowed = options.split("\\|", -1);
        for (String action : requested) {
            boolean found = false;
            for (String option : allowed) if (same(option, action)) { found = true; break; }
            if (!found) return false;
        }
        return true;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean same(String left, String right) {
        return left != null && right != null &&
            left.trim().toLowerCase(Locale.ROOT).equals(right.trim().toLowerCase(Locale.ROOT));
    }
}
