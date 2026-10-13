package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class Xmm2025AlphabetIndexTest {
    @Test public void chapterOneLetterSectionsMapCompiledCreaturesWithoutClaimingSemanticProof() {
        JSONArray indices = Xmm2025AlphabetIndex.sections();
        assertEquals(25, indices.length());
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < indices.length(); i++) {
            JSONObject entry = indices.optJSONObject(i);
            assertNotNull(entry);
            assertEquals("XMM", entry.optString("source"));
            assertEquals("sec_1_" + i, entry.optString("section_id"));
            assertEquals(String.valueOf((char)('A'+i)), entry.optString("letter"));
            assertFalse(entry.optBoolean("auto_rolls", true));
            assertFalse(entry.optBoolean("statblock_semantics_certified", true));
            JSONArray items = entry.optJSONArray("names");
            assertNotNull(items);
            for (int k=0;k<items.length();k++) {
                String name = items.optString(k);
                assertTrue(name, name.toUpperCase(java.util.Locale.ROOT).startsWith(entry.optString("letter")));
                assertTrue("Duplicate across letter sections: "+name, seen.add(name));
            }
            assertEquals(items.length(), entry.optInt("entry_count"));
        }
        JSONObject all = Xmm2025AlphabetIndex.coverage();
        assertEquals(25, all.optInt("inventoried_letter_sections"));
        assertEquals(Xmm2025CompiledBestiaryEngine.EXPECTED, all.optInt("compiled_names"));
        assertEquals(seen.size(), all.optInt("mapped_to_A_through_Y"));
        assertFalse(all.optBoolean("complete_edition_comparison", true));
        assertTrue("Z-names exist, but inventory lists only A-Y",
                all.optJSONArray("outside_inventory_letter_range").length() > 0);
        assertEquals("section_not_in_supplied_inventory",
                Xmm2025AlphabetIndex.section("sec_1_25").optString("status"));
    }
}
