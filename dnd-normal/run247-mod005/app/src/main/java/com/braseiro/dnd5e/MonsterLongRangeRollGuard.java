package com.braseiro.dnd5e;

/** G05: explicit two-d20 disadvantage at compiled-monster long range.
 * Never silently fabricate a second die or grant long-range attacks a normal roll.
 * Caller still enforces geometry and other advantage/disadvantage sources.
 */
public final class MonsterLongRangeRollGuard {
    private MonsterLongRangeRollGuard() {}
    public static String block(int first, int second, boolean longRange) {
        if (first < 1 || first > 20) return "invalid_first_d20";
        if (longRange) return second < 1 || second > 20 ? "long_range_requires_two_d20" : "";
        return second == 0 ? "" : "unexpected_second_d20";
    }
    public static int effective(int first, int second, boolean longRange) {
        String error = block(first, second, longRange);
        if (!error.isEmpty()) throw new IllegalArgumentException(error);
        return longRange ? Math.min(first, second) : first;
    }
}
