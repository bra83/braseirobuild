package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Validação pura da parte da ficha que não pode ser inferida pela UI:
 * histórico explícito de classes, pré-requisitos e posse de componentes.
 * Não persiste nem muta a ficha; o mesmo snapshot sempre produz o mesmo recibo.
 */
public final class CharacterMulticlassRules {
    private CharacterMulticlassRules() {}

    public static JSONObject validate(JSONObject sheet, boolean multiclassEnabled) {
        JSONObject out = new JSONObject();
        try {
            JSONObject s = sheet == null ? new JSONObject() : sheet;
            int characterLevel = s.optInt("level", 0);
            if (characterLevel < 1 || characterLevel > 20) return fail("invalid_character_level");
            JSONArray raw = s.optJSONArray("class_levels");
            if (raw == null) {
                raw = new JSONArray().put(new JSONObject().put("class_name", s.optString("class_name"))
                        .put("level", characterLevel));
            }
            if (raw.length() == 0) return fail("class_levels_required");
            Set<String> seen = new HashSet<String>();
            int total = 0;
            JSONArray classes = new JSONArray();
            for (int i = 0; i < raw.length(); i++) {
                JSONObject entry = raw.optJSONObject(i);
                if (entry == null) return fail("invalid_class_level_entry");
                String cls = canonical(entry.optString("class_name", entry.optString("class", "")));
                int level = entry.optInt("level", 0);
                if (cls.isEmpty() || level < 1 || level > 20) return fail("invalid_class_level");
                if (!seen.add(cls)) return fail("duplicate_class");
                total += level;
                classes.put(new JSONObject().put("class", cls).put("level", level));
            }
            if (total != characterLevel) return fail("class_levels_do_not_sum_to_character_level");
            if (!multiclassEnabled && classes.length() > 1) return fail("multiclass_disabled");
            if (classes.length() > 1) for (int i = 0; i < classes.length(); i++) {
                String prerequisite = prerequisite(classes.getJSONObject(i).optString("class"), s);
                if (!prerequisite.isEmpty()) return fail(prerequisite);
            }
            return out.put("valid", true).put("character_level", characterLevel)
                    .put("class_levels", classes).put("total_class_levels", total)
                    .put("multiclass", classes.length() > 1)
                    .put("inventory_snapshot", inventorySnapshot(s));
        } catch (Exception ignored) { return fail("multiclass_validation_failed"); }
    }

    /** Checks a required component without consuming it or changing the sheet. */
    public static JSONObject requireItem(JSONObject sheet, String requiredName) {
        JSONObject out = new JSONObject();
        try {
            String wanted = norm(requiredName);
            if (wanted.isEmpty()) return fail("item_name_required");
            int quantity = inventoryQuantity(sheet, wanted);
            return out.put("valid", quantity > 0).put("required_item", requiredName)
                    .put("quantity", quantity)
                    .put("reason", quantity > 0 ? "item_available" : "item_missing");
        } catch (Exception ignored) { return fail("item_validation_failed"); }
    }

    private static JSONObject fail(String reason) {
        try { return new JSONObject().put("valid", false).put("reason", reason); }
        catch (Exception ignored) { return new JSONObject(); }
    }

    private static String prerequisite(String cls, JSONObject s) {
        if ("artificer".equals(cls)) return score(s, "int_score") < 13 ? "multiclass_intelligence_13_required" : "";
        if ("barbarian".equals(cls)) return score(s, "str") < 13 ? "multiclass_strength_13_required" : "";
        if ("fighter".equals(cls)) return score(s, "str") < 13 && score(s, "dex") < 13
                ? "multiclass_strength_or_dexterity_13_required" : "";
        if ("paladin".equals(cls)) return score(s, "str") < 13 || score(s, "cha") < 13
                ? "multiclass_strength_and_charisma_13_required" : "";
        if ("ranger".equals(cls)) return score(s, "dex") < 13 || score(s, "wis") < 13
                ? "multiclass_dexterity_and_wisdom_13_required" : "";
        if ("rogue".equals(cls)) return score(s, "dex") < 13 ? "multiclass_dexterity_13_required" : "";
        if ("wizard".equals(cls)) return score(s, "int_score") < 13 ? "multiclass_intelligence_13_required" : "";
        if ("cleric".equals(cls) || "druid".equals(cls) || "monk".equals(cls))
            return score(s, "wis") < 13 ? "multiclass_wisdom_13_required" : "";
        if ("bard".equals(cls) || "sorcerer".equals(cls) || "warlock".equals(cls))
            return score(s, "cha") < 13 ? "multiclass_charisma_13_required" : "";
        return "unsupported_class";
    }

    private static int score(JSONObject s, String key) { return s.optInt(key, 0); }

    private static int inventoryQuantity(JSONObject sheet, String wanted) {
        if (sheet == null) return 0;
        int total = 0;
        JSONArray ledger = sheet.optJSONArray("inventory_ledger");
        if (ledger != null) for (int i = 0; i < ledger.length(); i++) {
            JSONObject item = ledger.optJSONObject(i); if (item == null) continue;
            if (norm(item.optString("name", item.optString("nome", ""))).contains(wanted))
                total += Math.max(0, item.optInt("quantity", 1));
        }
        if (total > 0) return total;
        String equipment = sheet.optString("equipment", sheet.optString("inventory", ""));
        for (String part : equipment.split("[,;\\n]")) if (norm(part).contains(wanted)) total++;
        return total;
    }

    private static String inventorySnapshot(JSONObject s) {
        JSONArray ledger = s.optJSONArray("inventory_ledger");
        return ledger == null ? norm(s.optString("equipment", s.optString("inventory", ""))) : ledger.toString();
    }

    private static String canonical(String raw) {
        String s = norm(raw);
        if (s.startsWith("artificer") || s.startsWith("artifice")) return "artificer";
        if (s.startsWith("barbar")) return "barbarian";
        if (s.startsWith("guerreiro") || s.startsWith("fighter")) return "fighter";
        if (s.startsWith("paladino") || s.startsWith("paladin")) return "paladin";
        if (s.startsWith("patrulheiro") || s.startsWith("ranger") || s.startsWith("guardiao")) return "ranger";
        if (s.startsWith("ladino") || s.startsWith("rogue")) return "rogue";
        if (s.startsWith("mago") || s.startsWith("wizard")) return "wizard";
        if (s.startsWith("clerigo") || s.startsWith("cleric")) return "cleric";
        if (s.startsWith("druida") || s.startsWith("druid")) return "druid";
        if (s.startsWith("monge") || s.startsWith("monk")) return "monk";
        if (s.startsWith("bardo") || s.startsWith("bard")) return "bard";
        if (s.startsWith("feiticeiro") || s.startsWith("sorcerer")) return "sorcerer";
        if (s.startsWith("bruxo") || s.startsWith("warlock")) return "warlock";
        return "";
    }

    private static String norm(String raw) {
        return Normalizer.normalize(raw == null ? "" : raw.toLowerCase(new Locale("pt", "BR")), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", " ").trim();
    }
}
