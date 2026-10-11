package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** Pure, reviewable character-level transition; persistence is left to the caller. */
public final class CharacterLevelUpTransitionEngine {
    private CharacterLevelUpTransitionEngine() {}

    /**
     * Applies one level after XP (or an explicit milestone) is due. A multiclass
     * transition must name the class receiving the level. The returned sheet is
     * always a copy, and pending choices are queued instead of overwritten.
     */
    public static JSONObject apply(JSONObject input, String selectedClass, int hitDie,
                                   int constitutionModifier, boolean requireXp,
                                   boolean allowMulticlass) {
        JSONObject out = new JSONObject();
        try {
            if (input == null) return refused("character_sheet_required", null);
            JSONObject sheet = new JSONObject(input.toString());
            int level = sheet.optInt("level", 0);
            if (level < 1 || level >= 20) return refused("maximum_or_invalid_level", sheet);
            if (requireXp && DndProgressionEngine.levelForXp(sheet.optInt("xp", 0)) <= level)
                return refused("xp_threshold_not_reached", sheet);

            JSONObject validation = CharacterMulticlassRules.validate(sheet, true);
            if (!validation.optBoolean("valid", false))
                return refused("invalid_current_class_levels:" + validation.optString("reason"), sheet);
            JSONArray classes = validation.optJSONArray("class_levels");
            String choice = normalizeClass(selectedClass);
            if (choice.isEmpty()) {
                if (classes == null || classes.length() != 1)
                    return refused("level_up_class_choice_required", sheet);
                choice = classes.optJSONObject(0).optString("class", "");
            }
            int expectedHitDie = hitDieForClass(choice);
            if (expectedHitDie == 0) return refused("unsupported_level_up_class", sheet);
            if (hitDie != expectedHitDie) return refused("class_hit_die_mismatch", sheet);
            int chosenIndex = -1;
            for (int i = 0; classes != null && i < classes.length(); i++) {
                if (sameClass(choice, classes.optJSONObject(i).optString("class", ""))) chosenIndex = i;
            }
            if (chosenIndex < 0 && !allowMulticlass)
                return refused("multiclass_disabled", sheet);

            JSONArray nextClasses = new JSONArray();
            for (int i = 0; classes != null && i < classes.length(); i++) {
                JSONObject entry = classes.optJSONObject(i);
                String className = entry.optString("class", "");
                int classLevel = entry.optInt("level", 0);
                if (i == chosenIndex) classLevel++;
                nextClasses.put(new JSONObject().put("class_name", className).put("level", classLevel));
            }
            if (chosenIndex < 0) nextClasses.put(new JSONObject().put("class_name", choice).put("level", 1));

            int nextLevel = level + 1;
            JSONObject candidate = new JSONObject(sheet.toString());
            candidate.put("level", nextLevel).put("class_levels", nextClasses);
            JSONObject nextValidation = CharacterMulticlassRules.validate(candidate, allowMulticlass);
            if (!nextValidation.optBoolean("valid", false))
                return refused("level_up_invalid:" + nextValidation.optString("reason"), sheet);

            int hpGain = Math.max(1, hitDie / 2 + 1 + constitutionModifier);
            int maxHp = Math.max(1, sheet.optInt("max_hp", 1)) + hpGain;
            int hp = Math.min(maxHp, Math.max(0, sheet.optInt("hp", 0)) + hpGain);
            candidate.put("max_hp", maxHp).put("hp", hp)
                    .put("hit_dice_max", nextLevel)
                    .put("hit_dice_spent", Math.max(0, Math.min(level,
                            sheet.optInt("hit_dice_spent", 0))))
                    .put("level_up_hp_method", "fixed_average")
                    .put("level_up_choices_pending", true)
                    .put("level_up_choice_class", choice);
            JSONArray pending = sheet.optJSONArray("pending_level_up_choices");
            pending = pending == null ? new JSONArray() : new JSONArray(pending.toString());
            pending.put(new JSONObject().put("level", nextLevel).put("class", choice)
                    .put("status", "pending"));
            candidate.put("pending_level_up_choices", pending);
            JSONObject progression = DndProgressionEngine.state(candidate, nextLevel);
            candidate.put("progression", progression);
            return out.put("accepted", true).put("level_up_applied", true)
                    .put("from_level", level).put("to_level", nextLevel)
                    .put("class", choice).put("hp_gain", hpGain)
                    .put("state_after", candidate);
        } catch (Exception e) {
            return refused("level_up_transition_failed", input);
        }
    }

    private static JSONObject refused(String reason, JSONObject state) {
        JSONObject out = new JSONObject();
        try {
            out.put("accepted", false).put("level_up_blocked", reason);
            if (state != null) out.put("state_after", new JSONObject(state.toString()));
        } catch (Exception ignored) {}
        return out;
    }

    private static boolean sameClass(String a, String b) {
        return normalizeClass(a).equals(normalizeClass(b));
    }

    private static int hitDieForClass(String value) {
        switch (normalizeClass(value)) {
            case "barbarian": return 12;
            case "fighter":
            case "paladin":
            case "ranger": return 10;
            case "sorcerer":
            case "wizard": return 6;
            case "artificer": return 8;
            case "bard":
            case "cleric":
            case "druid":
            case "monk":
            case "rogue":
            case "warlock": return 8;
            default: return 0;
        }
    }

    private static String normalizeClass(String value) {
        String text = Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", " ").trim();
        if (text.startsWith("guerreiro") || text.startsWith("fighter")) return "fighter";
        if (text.startsWith("mago") || text.startsWith("wizard")) return "wizard";
        if (text.startsWith("barbaro") || text.startsWith("barbarian")) return "barbarian";
        if (text.startsWith("bardo") || text.startsWith("bard")) return "bard";
        if (text.startsWith("artificer") || text.startsWith("artifice")) return "artificer";
        if (text.startsWith("clerigo") || text.startsWith("cleric")) return "cleric";
        if (text.startsWith("druida") || text.startsWith("druid")) return "druid";
        if (text.startsWith("monge") || text.startsWith("monk")) return "monk";
        if (text.startsWith("paladino") || text.startsWith("paladin")) return "paladin";
        if (text.startsWith("patrulheiro") || text.startsWith("ranger")) return "ranger";
        if (text.startsWith("ladino") || text.startsWith("rogue")) return "rogue";
        if (text.startsWith("feiticeiro") || text.startsWith("sorcerer")) return "sorcerer";
        if (text.startsWith("bruxo") || text.startsWith("warlock")) return "warlock";
        return text;
    }
}
