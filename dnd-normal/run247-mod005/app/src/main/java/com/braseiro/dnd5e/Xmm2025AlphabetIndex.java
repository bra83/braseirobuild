package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/**
 * 25 Monster Manual 2025 chapter-1 letter sections, from the 483-row inventory.
 * An alphabetic index is not proof that all creature features execute.
 */
public final class Xmm2025AlphabetIndex {
    private Xmm2025AlphabetIndex() {}

    /** A-Y in ch_1/sec_1_0...24; Z currently has no section in supplied matrix. */
    public static JSONArray sections() {
        JSONObject catalog = Xmm2025CompiledBestiaryEngine.catalog();
        JSONArray names = catalog.optJSONArray("names");
        JSONArray results = new JSONArray();
        for (int i = 0; i < 25; i++) {
            String letter = String.valueOf((char)('A' + i));
            JSONArray creatures = new JSONArray();
            if (names != null) for (int j = 0; j < names.length(); j++) {
                String name = names.optString(j, "");
                if (name.trim().toUpperCase(Locale.ROOT).startsWith(letter))
                    creatures.put(name);
            }
            try {
                results.put(new JSONObject()
                    .put("source", "XMM").put("chapter_id", "ch_1")
                    .put("section_id", "sec_1_" + i)
                    .put("letter", letter).put("names", creatures)
                    .put("entry_count", creatures.length())
                    .put("auto_rolls", false)
                    .put("statblock_semantics_certified", false));
            } catch (Exception e) {
                throw new IllegalStateException("XMM alphabetical index failure", e);
            }
        }
        return results;
    }

    public static JSONObject section(String id) {
        JSONArray list = sections();
        for (int i=0; i<list.length(); i++) {
            JSONObject row = list.optJSONObject(i);
            if (row != null && row.optString("section_id").equals(id)) return row;
        }
        JSONObject notFound = new JSONObject();
        try {
            notFound.put("source", "XMM").put("section_id", id == null ? "" : id)
                .put("resolved", false).put("auto_rolls", false)
                .put("status", "section_not_in_supplied_inventory");
        } catch (Exception e) { throw new IllegalStateException(e); }
        return notFound;
    }

    public static JSONObject coverage() {
        JSONObject original = Xmm2025CompiledBestiaryEngine.catalog();
        JSONArray source = original.optJSONArray("names");
        JSONArray sections = sections();
        int mapped = 0;
        JSONArray outOfIndex = new JSONArray();
        if (source != null) for (int j=0;j<source.length();j++) {
            String name = source.optString(j, "");
            if (name.trim().isEmpty()) continue;
            char first = Character.toUpperCase(name.trim().charAt(0));
            if (first >= 'A' && first <= 'Y') mapped++;
            else outOfIndex.put(name);
        }
        try {
            return new JSONObject().put("source", "XMM")
                .put("inventoried_letter_sections", sections.length())
                .put("compiled_names", source == null ? 0 : source.length())
                .put("mapped_to_A_through_Y", mapped)
                .put("outside_inventory_letter_range", outOfIndex)
                .put("complete_edition_comparison", false)
                .put("auto_rolls", false);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
