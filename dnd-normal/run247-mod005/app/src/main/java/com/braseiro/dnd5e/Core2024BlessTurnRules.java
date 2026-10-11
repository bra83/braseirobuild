package com.braseiro.dnd5e;

/** Deterministic turn-boundary decision for a persistent PHB 2024 Bless effect.
 * No database access, dice rolls or inferred concentration are permitted here.
 * The AppDb caller must commit both the character sheet and combatant in the
 * same SQLite transaction as advancing the encounter clock.
 */
public final class Core2024BlessTurnRules {
    public enum Decision { KEEP, END_CONCENTRATION, EXPIRE }
    private Core2024BlessTurnRules() {}

    public static Decision reconcile(int castRound, int currentRound,
                                     String currentConcentration, boolean incapacitated) {
        if (castRound < 1 || currentRound < castRound || currentRound > 1000000)
            throw new IllegalArgumentException("invalid_bless_turn_timeline");
        if (incapacitated || !"Bênção".equals(currentConcentration))
            return Decision.END_CONCENTRATION;
        return SpellConcentrationDurationRules.expired("2024", castRound, 10, currentRound)
            ? Decision.EXPIRE : Decision.KEEP;
    }
}
