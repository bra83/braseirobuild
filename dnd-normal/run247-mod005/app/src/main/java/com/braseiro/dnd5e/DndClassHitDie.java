package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;

/** Single deterministic hit-die mapping for D&D 5e character and multiclass UI.
 * Source aliases are intentionally accepted in English and Brazilian Portuguese.
 * Unknown/homebrew classes retain the baseline d8 fallback (do not change saves).
 */
public final class DndClassHitDie {
    private DndClassHitDie() {}

    public static int forClass(String className) {
        String c = normalize(className);
        if (c.contains("barbar")) return 12;
        if (c.contains("cacador de sangue") || c.contains("blood hunter")) return 10;
        if (c.contains("atirador")) return 8;
        if (c.contains("guerreiro") || c.contains("fighter") || c.contains("paladino")
                || c.contains("paladin") || c.contains("patrulheiro")
                || c.contains("ranger") || c.contains("guardiao")) return 10;
        if (c.equals("necromante") || c.contains("necromancer")) return 6;
        if (c.contains("mago") || c.contains("wizard") || c.contains("feiticeiro")
                || c.contains("sorcerer")) return 6;
        return 8;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", " ").trim();
    }
}
