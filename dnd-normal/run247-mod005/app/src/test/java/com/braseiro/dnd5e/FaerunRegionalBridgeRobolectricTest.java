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

/** Source-bounded catalog coverage is not equivalent to semantic rule approval. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class FaerunRegionalBridgeRobolectricTest {
    @Test public void all64ChapterNineSectionsHaveUniqueIdsAndCorrectKinds() throws Exception {
        JSONArray all = FaerunRegionalBestiaryBridge.sections();
        assertEquals(64, all.length());
        Set<String> keys = new HashSet<>();
        int creature = 0, family = 0, editorial = 0;
        for (int i=0; i<all.length(); i++) {
            JSONObject row = all.getJSONObject(i);
            String id = row.getString("section_id");
            assertTrue(id, keys.add(id));
            assertEquals("sec_9_"+i, id);
            assertEquals("FRAIF", row.getString("source"));
            assertEquals("ch_9", row.getString("chapter_id"));
            assertFalse(row.getString("title").trim().isEmpty());
            assertTrue(row.getBoolean("requires_individual_semantic_evidence"));
            JSONObject lookup = FaerunRegionalBestiaryBridge.section(id);
            assertEquals(id, lookup.getString("section_id"));
            assertFalse(lookup.optBoolean("auto_rolls", true));
            if ("creature".equals(row.getString("kind"))) {
                creature++;
                // An unverified statblock must not invent attack, HP or damage.
                if (!lookup.optBoolean("resolved", false)) {
                    assertEquals("regional_statblock_not_verified",
                            lookup.getString("status"));
                    assertFalse(lookup.has("statblock"));
                    assertTrue(lookup.getBoolean("requires_manual_adjudication"));
                } else {
                    assertTrue(lookup.optJSONObject("statblock")
                            .getJSONObject("completeness").getBoolean("tactical"));
                }
            } else if ("family".equals(row.getString("kind"))) {
                family++;
                assertFalse(lookup.getBoolean("resolved"));
                assertEquals("editorial_heading_not_statblock", lookup.getString("status"));
            } else {
                editorial++;
                assertEquals("editorial_heading_not_statblock", lookup.getString("status"));
                assertFalse(lookup.getBoolean("resolved"));
            }
        }
        assertEquals(39, creature);
        assertEquals(23, family);
        assertEquals(2, editorial);
        JSONObject cover = FaerunRegionalBestiaryBridge.coverage();
        assertEquals(64, cover.getInt("sections"));
        assertEquals(39, cover.getInt("creature_entries"));
        assertEquals(23, cover.getInt("families"));
        assertEquals(2, cover.getInt("editorial_only"));
        assertEquals(39, cover.getInt("tactical_ready")+cover.getInt("tactical_pending"));
        assertFalse(cover.getBoolean("all_semantics_certified"));
    }

    @Test public void sevenNamedRelicsAreVisibleWithoutFabricatedEffects() throws Exception {
        AdventuresInFaerunRulesEngine engine = new AdventuresInFaerunRulesEngine();
        JSONArray relics = engine.listFaerunRelics();
        assertEquals(7, relics.length());
        for (int i=0; i<7; i++) {
            JSONObject relic = relics.getJSONObject(i);
            String id = "sec_8_"+(i+1);
            assertEquals(id, relic.getString("section_id"));
            assertEquals("FRAIF", relic.getString("source"));
            assertFalse(relic.getBoolean("rules_verified"));
            assertFalse(relic.getBoolean("auto_rolls"));
            JSONObject unresolved = engine.resolveFaerunRelic(id);
            assertFalse(unresolved.getBoolean("resolved"));
            assertTrue(unresolved.getBoolean("requires_manual_adjudication"));
            assertFalse("Unsupported item must not grant powers", unresolved.has("effects"));
        }
        assertEquals("relic_not_in_source_index",
                engine.resolveFaerunRelic("sec_8_0").getString("status"));
        assertEquals(64, engine.listRegionalBestiarySections().length());
        assertEquals("editorial_heading_not_statblock",
                engine.regionalStatblockForSection("sec_9_1").getString("status"));
    }

    @Test public void unrecognizedSectionDoesNotExposeForeignBestiary() throws Exception {
        JSONObject unknown = FaerunRegionalBestiaryBridge.section("sec_99_1");
        assertFalse(unknown.getBoolean("resolved"));
        assertEquals("section_not_in_source_index", unknown.getString("status"));
        assertFalse(unknown.has("statblock"));
        assertFalse(unknown.getBoolean("auto_rolls"));
    }
}
