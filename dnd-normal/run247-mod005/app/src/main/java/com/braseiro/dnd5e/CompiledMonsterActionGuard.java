package com.braseiro.dnd5e;

/** Small, deterministic guards shared by the durable monster action paths. */
public final class CompiledMonsterActionGuard {
    private CompiledMonsterActionGuard() {}
    private static final java.util.Set<String> CORE_CONDITIONS = new java.util.HashSet<String>(
        java.util.Arrays.asList("cego","enfeiticado","amedrontado","agarrado","incapacitado",
            "invisivel","paralisado","petrificado","envenenado","caido","impedido",
            "aturdido","inconsciente","surdo","exausto"));

    public static String statblockBlock(String persistedId, String requestedId) {
        String stored = persistedId == null ? "" : persistedId.trim();
        String requested = requestedId == null ? "" : requestedId.trim();
        return stored.isEmpty() || !stored.equals(requested) ? "missing_authorized_statblock" : "";
    }

    public static boolean diceRecharge(String specification) {
        String spec = specification == null ? "" : specification.trim();
        return spec.matches("[1-6](-[1-6])?") &&
            (spec.length() == 1 || spec.charAt(0) <= spec.charAt(2));
    }

    public static String actionRechargeBlock(String specification) {
        String spec = specification == null ? "" : specification.trim();
        return spec.isEmpty() || diceRecharge(spec) ? "" : "unsupported_action_activation";
    }

    public static String conditionBlock(String conditions) {
        if (conditions == null || conditions.trim().isEmpty()) return "";
        for (String raw : conditions.split("[;,]")) {
            String condition = raw.trim();
            if (!condition.isEmpty() && !CORE_CONDITIONS.contains(ConditionTokenGuard.canonical(condition)))
                return "unsupported_secondary_effect";
        }
        return "";
    }

    public static String canonicalCondition(String condition) {
        String canonical = ConditionTokenGuard.canonical(condition);
        return CORE_CONDITIONS.contains(canonical) ? canonical : "";
    }

    public static String rechargeBlock(boolean combatRunning, boolean activeTurn,
        int hp, boolean spent, int spentRound, int rolledRound, int currentRound) {
        if (!combatRunning || !activeTurn || hp <= 0 || !spent || currentRound < 1)
            return "recharge_not_due";
        if (spentRound >= currentRound) return "recharge_next_turn_only";
        if (rolledRound == currentRound) return "recharge_already_rolled_this_turn";
        return "";
    }
}
