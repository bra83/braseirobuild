package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Source-indexed regional bestiary section gateway for FRAIF chapter 9.
 * 64 distinct table-of-contents sections != 64 published creature statblocks:
 * section 0 is a caption, 1 is an index, 2-24 are family headings,
 * and 25-63 are 39 statblock entries.
 *
 * No invented AC/HP/dice/traits. Gameplay is authorized only if an exact
 * regional statblock from the same supplement is already compiled.
 */
public final class FaerunRegionalBestiaryBridge {
    private static final String SOURCE_CODE = "FRAIF";
    private FaerunRegionalBestiaryBridge() {}

    private static final String[][] SECTIONS = {
        {"sec_9_0", "Xanathar—along with Sylgar, Ott Steeltoes, and the mind flayer Nihiloor—are only some of the many dangers adventurers encounter in the Forgotten Realms.", "caption"},
        {"sec_9_1", "Monsters by Region", "index"},
        {"sec_9_2", "Aranea", "family"},
        {"sec_9_3", "Aranea", "family"},
        {"sec_9_4", "Beast of Malar", "family"},
        {"sec_9_5", "Biha Babir", "family"},
        {"sec_9_6", "Cultists", "family"},
        {"sec_9_7", "Dead Three Scion", "family"},
        {"sec_9_8", "Deep Dragons", "family"},
        {"sec_9_9", "Drow of Lolth", "family"},
        {"sec_9_10", "Karas Chembryl", "family"},
        {"sec_9_11", "Manshoon", "family"},
        {"sec_9_12", "Nimblewrights", "family"},
        {"sec_9_13", "Phaerimm", "family"},
        {"sec_9_14", "Polar Serpent", "family"},
        {"sec_9_15", "Queen Forfallen", "family"},
        {"sec_9_16", "Rusted", "family"},
        {"sec_9_17", "Sammaster", "family"},
        {"sec_9_18", "Spirit Dragons", "family"},
        {"sec_9_19", "Spore of Moander", "family"},
        {"sec_9_20", "Swanmay", "family"},
        {"sec_9_21", "Valindra Shadowmantle", "family"},
        {"sec_9_22", "Werewyvern", "family"},
        {"sec_9_23", "Zhentilar", "family"},
        {"sec_9_24", "Zlan", "family"},
        {"sec_9_25", "Beast of Malar", "creature"},
        {"sec_9_26", "Biha Babir", "creature"},
        {"sec_9_27", "Cultist of Bane", "creature"},
        {"sec_9_28", "Cultist of Bhaal", "creature"},
        {"sec_9_29", "Cultist of Myrkul", "creature"},
        {"sec_9_30", "Dead Three Scion", "creature"},
        {"sec_9_31", "Deep Dragon Wyrmling", "creature"},
        {"sec_9_32", "Young Deep Dragon", "creature"},
        {"sec_9_33", "Adult Deep Dragon", "creature"},
        {"sec_9_34", "Ancient Deep Dragon", "creature"},
        {"sec_9_35", "Drow of Lolth", "creature"},
        {"sec_9_36", "Drow Elite Warrior of Lolth", "creature"},
        {"sec_9_37", "Drow Mage of Lolth", "creature"},
        {"sec_9_38", "Drow Priestess of Lolth", "creature"},
        {"sec_9_39", "Karas Chembryl", "creature"},
        {"sec_9_40", "Manshoon", "creature"},
        {"sec_9_41", "Nimblewright Guard", "creature"},
        {"sec_9_42", "Nimblewright Hulk", "creature"},
        {"sec_9_43", "Nimblewright Steed", "creature"},
        {"sec_9_44", "Phaerimm Hatchling", "creature"},
        {"sec_9_45", "Phaerimm Agent", "creature"},
        {"sec_9_46", "Polar Serpent", "creature"},
        {"sec_9_47", "Queen Forfallen", "creature"},
        {"sec_9_48", "Rusted Behemoth", "creature"},
        {"sec_9_49", "Rusted Berserker", "creature"},
        {"sec_9_50", "Rusted Wyrm", "creature"},
        {"sec_9_51", "Sammaster (Dracolich Form)", "creature"},
        {"sec_9_52", "Sammaster (Lich Form)", "creature"},
        {"sec_9_53", "Spirit Dragon Wyrmling", "creature"},
        {"sec_9_54", "Young Spirit Dragon", "creature"},
        {"sec_9_55", "Adult Spirit Dragon", "creature"},
        {"sec_9_56", "Ancient Spirit Dragon", "creature"},
        {"sec_9_57", "Spore of Moander", "creature"},
        {"sec_9_58", "Swanmay", "creature"},
        {"sec_9_59", "Valindra Shadowmantle", "creature"},
        {"sec_9_60", "Werewyvern", "creature"},
        {"sec_9_61", "Zhentilar Soldier", "creature"},
        {"sec_9_62", "Zhentilar Paladin of Bane", "creature"},
        {"sec_9_63", "Zlan", "creature"},
    };

    public static JSONArray sections() {
        JSONArray out = new JSONArray();
        for (String[] row : SECTIONS) {
            JSONObject entry = new JSONObject();
            try {
                entry.put("source", SOURCE_CODE)
                    .put("chapter_id", "ch_9")
                    .put("section_id", row[0])
                    .put("title", row[1])
                    .put("kind", row[2])
                    .put("requires_individual_semantic_evidence", true);
            } catch (Exception e) {
                throw new IllegalStateException("FRAIF section index malformed", e);
            }
            out.put(entry);
        }
        return out;
    }

    public static JSONObject section(String sectionId) {
        for (String[] row : SECTIONS) {
            if (!row[0].equals(sectionId)) continue;
            JSONObject out = new JSONObject();
            try {
                out.put("source", SOURCE_CODE)
                    .put("chapter_id", "ch_9")
                    .put("section_id", row[0])
                    .put("title", row[1])
                    .put("kind", row[2])
                    .put("auto_rolls", false);
                if (!"creature".equals(row[2]))
                    return out.put("resolved", false)
                        .put("status", "editorial_heading_not_statblock");
                JSONObject statblock = CompiledStatblockEngine.get(row[1]);
                String origin = statblock.optString("source_book", "");
                String locator = statblock.optJSONObject("mechanical_basis") == null
                        ? "" : statblock.optJSONObject("mechanical_basis")
                                .optString("source_locator", "");
                // Never confuse XMM's coincidentally named monster with
                // this supplement's regional entry.
                boolean validSource = "FRAIF".equalsIgnoreCase(origin)
                        || origin.toLowerCase(java.util.Locale.ROOT)
                                .contains("adventures in faer");
                boolean validLocator = "FRAIF".equalsIgnoreCase(origin)
                        || locator.contains("FRAIF:")
                        || locator.toLowerCase(java.util.Locale.ROOT)
                                .contains("book-fraif");
                JSONObject completeness = CompiledStatblockEngine.completeness(statblock);
                boolean canUse = statblock.length() > 0 && validSource && validLocator
                        && completeness.optBoolean("tactical", false);
                out.put("resolved", canUse)
                    .put("status", canUse ? "source_bound_tactical_statblock_ready"
                            : "regional_statblock_not_verified")
                    .put("source_verified", canUse)
                    .put("requires_manual_adjudication", !canUse);
                if (canUse)
                    out.put("statblock", CompiledStatblockEngine.runtimeProfile(row[1]));
                return out;
            } catch (Exception e) {
                throw new IllegalStateException("Unable to query regional entry " + sectionId, e);
            }
        }
        JSONObject unknown = new JSONObject();
        try {
            unknown.put("source", SOURCE_CODE).put("section_id",
                    sectionId == null ? "" : sectionId)
                .put("resolved", false).put("auto_rolls", false)
                .put("status", "section_not_in_source_index");
        } catch (Exception ignored) {}
        return unknown;
    }

    public static JSONObject coverage() {
        JSONObject out = new JSONObject();
        int creatures = 0, ready = 0, families = 0, metadata = 0;
        JSONArray blocked = new JSONArray();
        for (String[] row : SECTIONS) {
            if ("creature".equals(row[2])) {
                creatures++;
                JSONObject result = section(row[0]);
                if (result.optBoolean("resolved", false)) ready++;
                else blocked.put(row[0]);
            } else if ("family".equals(row[2])) families++;
            else metadata++;
        }
        try {
            return out.put("source", SOURCE_CODE).put("sections", SECTIONS.length)
                .put("creature_entries", creatures).put("families", families)
                .put("editorial_only", metadata).put("tactical_ready", ready)
                .put("tactical_pending", creatures - ready)
                .put("blocked_sections", blocked).put("auto_rolls", false)
                .put("all_semantics_certified", false);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
