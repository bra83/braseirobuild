package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/**
 * Validates EACH of the 503 bundled XMM tactical records at runtime. This is
 * runtime completeness, not independent validation against the 2025 book.
 */
public final class Xmm503StatblockRuntimeContractTest {
    @Test public void all503BundledMonstersHaveIndependentPlayableRuntimeProfiles() {
        JSONObject installed=Xmm2025CompiledBestiaryEngine.install();
        assertEquals(0,installed.optInt("blocked",-1));
        assertEquals(503,installed.optInt("statblocks_installed",-1));
        JSONArray names=Xmm2025CompiledBestiaryEngine.catalog().optJSONArray("names");
        assertNotNull("Missing monster catalog",names);
        assertEquals(503,names.length());
        Set<String> unique=new HashSet<>();
        int actionless=0;
        for(int i=0;i<names.length();i++) {
            String id=names.optString(i,"").trim();
            assertFalse("Missing monster ID at "+i,id.isEmpty());
            assertTrue("Duplicate XMM creature "+id,unique.add(id));
            JSONObject raw=CompiledStatblockEngine.get(id);
            assertNotNull("Missing compiled source "+id,raw);
            assertEquals("Monster Manual (2025)",raw.optString("source_book"));
            assertEquals(id,raw.optString("id"));
            assertTrue(id+" armor class",raw.optInt("armor_class")>0);
            assertTrue(id+" hit points",raw.optInt("hit_points")>0);
            assertFalse(id+" cannot enable hidden dice",raw.optBoolean("auto_rolls",true));
            assertFalse(id+" cannot promise unverified full source text",
                    raw.optBoolean("full_statblock_claim",true));
            JSONObject authority=raw.optJSONObject("mechanical_basis");
            assertNotNull(id+" source authority",authority);
            assertFalse(id+" missing source locator",authority.optString("source_locator").trim().isEmpty());
            JSONObject profile=CompiledStatblockEngine.runtimeProfile(id);
            assertTrue(id+" profile unavailable",profile.optBoolean("found"));
            assertEquals(id,profile.optString("id"));
            assertEquals(raw.optInt("armor_class"),profile.optInt("armor_class"));
            assertEquals(raw.optInt("hit_points"),profile.optInt("hit_points"));
            assertNotNull(id+" movement",profile.optJSONObject("movement"));
            assertNotNull(id+" abilities",profile.optJSONObject("abilities"));
            assertNotNull(id+" actions list",profile.optJSONArray("actions"));
            assertNotNull(id+" specials list",profile.optJSONArray("special_actions"));
            if(raw.optBoolean("source_actionless")) actionless++;
            else assertTrue(id+" must expose actions",profile.optJSONArray("actions").length()>0);
        }
        assertEquals("Exactly 503 distinct monsters must be tested",503,unique.size());
        assertEquals("Catalog count",503,Xmm2025CompiledBestiaryEngine.catalog().optInt("count"));
        assertTrue("Actionless tally must be nonnegative",actionless>=0);
    }
}
