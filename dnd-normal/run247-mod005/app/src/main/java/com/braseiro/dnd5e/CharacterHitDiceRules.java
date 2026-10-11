package com.braseiro.dnd5e;

/** Limites mecânicos do gasto de Dados de Vida durante um descanso curto. */
public final class CharacterHitDiceRules {
    private CharacterHitDiceRules() {}

    /** Retorna o motivo do bloqueio; string vazia significa gasto permitido. */
    public static String spendBlock(int totalDice, int spentDice, int count,
                                    int dieSides, int constitutionModifier, int rolledTotal) {
        if (totalDice < 1 || spentDice < 0 || spentDice > totalDice) return "hit_dice_state_invalid";
        if (count < 1) return "hit_dice_count_invalid";
        // A cura mínima é aplicada por dado; o recibo atual contém uma rolagem por vez.
        if (count > 1) return "hit_dice_rolls_individual_required";
        if (count > totalDice - spentDice) return "hit_dice_insufficient";
        if (dieSides < 1) return "hit_die_invalid";
        long minimum = (long) count * (1 + constitutionModifier);
        long maximum = (long) count * (dieSides + constitutionModifier);
        if (rolledTotal < minimum || rolledTotal > maximum) return "hit_dice_roll_out_of_range";
        return "";
    }

    public static int healing(int rolledTotal, int count, boolean revised2024) {
        return Math.max(revised2024 ? count : 0, rolledTotal);
    }

    public static int recoveredOnLongRest(int totalDice, int spentDice, boolean revised2024) {
        int total = Math.max(1, Math.min(20, totalDice));
        int spent = Math.max(0, Math.min(total, spentDice));
        return revised2024 ? spent : Math.min(spent, Math.max(1, total / 2));
    }
}
