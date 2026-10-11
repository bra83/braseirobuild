package com.braseiro.dnd5e;

/** Deterministic, fail-closed gates for a combatant's turn. No database or AI. */
public final class CombatTurnGuard {
    private CombatTurnGuard() {}
    public static boolean knownResource(String key) {
        return "action".equals(key) || "bonus_action".equals(key) || "reaction".equals(key);
    }
    public static String spendBlock(String key, boolean encounterRunning, boolean activeTurn,
            boolean incapacitated, int hp, boolean dead, boolean reactionUsed,
            boolean actionAvailable, boolean bonusActionAvailable) {
        if (!knownResource(key)) return "unknown_resource";
        if (!encounterRunning) return "encounter_not_running";
        if (incapacitated || dead || hp <= 0) return "incapacitated";
        if ("reaction".equals(key)) return reactionUsed ? "reaction_spent" : "";
        if (!activeTurn) return "not_active_turn";
        if ("action".equals(key) && !actionAvailable) return "action_available_spent";
        if ("bonus_action".equals(key) && !bonusActionAvailable) return "bonus_action_available_spent";
        return "";
    }
    public static String deathSaveBlock(int hp, boolean dead, boolean stable, boolean due, int natural) {
        if (natural < 1 || natural > 20) return "invalid_d20";
        if (hp > 0 || dead || stable) return "not_dying";
        if (!due) return "death_save_not_due";
        return "";
    }
    public static String concentrationBlock(boolean pending, boolean concentrating, int natural) {
        if (natural < 1 || natural > 20) return "invalid_d20";
        if (!pending) return "concentration_save_not_due";
        if (!concentrating) return "not_concentrating";
        return "";
    }
    public static String movementBlock(boolean activeTurn, boolean incapacitated, boolean speedZero,
            int hp, boolean dead, int requested, int remaining) {
        if (requested < 0) return "invalid_distance";
        if (!activeTurn) return "not_active_turn";
        if (incapacitated || dead || hp <= 0) return "incapacitated";
        if (speedZero) return "speed_zero";
        if (requested > remaining) return "insufficient_movement";
        return "";
    }
    public static int safeHealing(int current, int maximum, int amount) {
        int max = Math.max(1, maximum);
        long after = (long)Math.max(0, current) + Math.max(0, amount);
        return (int)Math.min((long)max, after);
    }
    public static boolean canStand(int speed, int remaining, boolean speedZero) {
        return speed > 0 && !speedZero && remaining >= speed / 2 + speed % 2;
    }
}
