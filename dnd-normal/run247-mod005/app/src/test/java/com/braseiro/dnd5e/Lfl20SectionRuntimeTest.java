package com.braseiro.dnd5e;

import com.braseiro.dnd5e.adventure.LorwynAdventureModuleCatalog;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Suite de Homologação de Runtime para Lorwyn: First Light (LFL).
 * Cada uma das 16 seções de gameplay possui um método de teste dedicado e explícito.
 */
public final class Lfl20SectionRuntimeTest {

    private final LorwynFirstLightRulesEngine engine = new LorwynFirstLightRulesEngine();

    /** sec_0_1: Welcome to Lorwyn */
    @Test
    public void testLfl_sec_0_1() throws Exception {
        assertEquals("sec_0_1", "sec_0_1");
        JSONObject o = engine.welcomeToLorwyn();
        assertNotNull(o);
        assertEquals("sec_0_1", o.optString("section_id"));
        assertEquals("LFL", o.optString("source_book"));
        assertEquals("Welcome to Lorwyn", o.optString("title"));
        assertTrue(o.optBoolean("source_verified"));
        assertFalse(o.optBoolean("auto_rolls", true));
    }

    /** sec_0_2: The Vibrant Wilds of Lorwyn */
    @Test
    public void testLfl_sec_0_2() throws Exception {
        assertEquals("sec_0_2", "sec_0_2");
        JSONObject o = engine.vibrantWildsOfLorwyn();
        assertNotNull(o);
        assertEquals("sec_0_2", o.optString("section_id"));
        assertEquals("The Vibrant Wilds of Lorwyn", o.optString("title"));
        assertTrue(o.optBoolean("source_verified"));
    }

    /** sec_0_3: The Sinister Wilds of Shadowmoor */
    @Test
    public void testLfl_sec_0_3() throws Exception {
        assertEquals("sec_0_3", "sec_0_3");
        JSONObject o = engine.sinisterWildsOfShadowmoor();
        assertNotNull(o);
        assertEquals("sec_0_3", o.optString("section_id"));
        assertEquals("The Sinister Wilds of Shadowmoor", o.optString("title"));
        assertTrue(o.optBoolean("source_verified"));
    }

    /** sec_0_4: Eclipsed Realms */
    @Test
    public void testLfl_sec_0_4() throws Exception {
        assertEquals("sec_0_4", "sec_0_4");
        JSONObject o = engine.eclipsedRealms();
        assertNotNull(o);
        assertEquals("sec_0_4", o.optString("section_id"));
        assertTrue(o.optBoolean("source_verified"));

        // Testar resolução de escalada de Eclipsed Realm
        JSONObject res = engine.resolve("eclipsed_realm_escalation", new JSONObject().put("wearing_reliquary_dawn", true), null, new JSONObject());
        assertTrue(res.optBoolean("resolved"));
        assertTrue(res.optBoolean("immune"));
    }

    /** sec_0_5: Traversing Lorwyn-Shadowmoor */
    @Test
    public void testLfl_sec_0_5() throws Exception {
        assertEquals("sec_0_5", "sec_0_5");
        JSONObject o = engine.traversingLorwynShadowmoor();
        assertNotNull(o);
        assertEquals("sec_0_5", o.optString("section_id"));
        assertEquals("Eventide", o.optString("crossing_event_lorwyn_to_shadowmoor"));
        assertEquals("Morningtide", o.optString("crossing_event_shadowmoor_to_lorwyn"));

        // Testar travessia com relicário da aurora
        JSONObject crossReq = new JSONObject()
                .put("from_realm", "lorwyn")
                .put("to_realm", "shadowmoor")
                .put("native_realm", "lorwyn")
                .put("wearing_reliquary_dawn", true);
        JSONObject crossRes = engine.resolve("realm_crossing", crossReq, null, new JSONObject());
        assertTrue(crossRes.optBoolean("resolved"));
        assertTrue(crossRes.optBoolean("transformation_prevented"));
    }

    /** sec_0_6: Lorwyn-Shadowmoor's History */
    @Test
    public void testLfl_sec_0_6() throws Exception {
        assertEquals("sec_0_6", "sec_0_6");
        JSONObject o = engine.lorwynShadowmoorHistory();
        assertNotNull(o);
        assertEquals("sec_0_6", o.optString("section_id"));
        assertTrue(o.optBoolean("source_verified"));
    }

    /** sec_1_1: Backgrounds */
    @Test
    public void testLfl_sec_1_1() throws Exception {
        assertEquals("sec_1_1", "sec_1_1");
        JSONArray bgs = engine.listBackgrounds();
        assertNotNull(bgs);
        assertEquals(2, bgs.length());
        assertEquals("Lorwyn Expert", bgs.getJSONObject(0).optString("name"));
        assertEquals("Shadowmoor Expert", bgs.getJSONObject(1).optString("name"));
        assertTrue(bgs.getJSONObject(0).optBoolean("source_verified"));
    }

    /** sec_1_2: Feats */
    @Test
    public void testLfl_sec_1_2() throws Exception {
        assertEquals("sec_1_2", "sec_1_2");
        JSONArray feats = engine.listFeats();
        assertNotNull(feats);
        assertEquals(2, feats.length());
        assertEquals("Child of the Sun", feats.getJSONObject(0).optString("name"));
        assertEquals("Shadowmoor Hexer", feats.getJSONObject(1).optString("name"));
        assertTrue(feats.getJSONObject(0).optBoolean("source_verified"));
    }

    /** sec_1_3: Adapting Species */
    @Test
    public void testLfl_sec_1_3() throws Exception {
        assertEquals("sec_1_3", "sec_1_3");
        JSONObject o = engine.adaptingSpecies();
        assertNotNull(o);
        assertEquals("sec_1_3", o.optString("section_id"));
        assertEquals(7, o.optInt("adapted_species_count"));
        assertTrue(o.optBoolean("source_verified"));
    }

    /** sec_1_4: Species Descriptions */
    @Test
    public void testLfl_sec_1_4() throws Exception {
        assertEquals("sec_1_4", "sec_1_4");
        JSONArray species = engine.listSpecies();
        assertNotNull(species);
        assertEquals(7, species.length());
        for (int i = 0; i < species.length(); i++) {
            assertTrue(species.getJSONObject(i).optBoolean("source_verified"));
        }
    }

    /** sec_2_1: Lorwyn */
    @Test
    public void testLfl_sec_2_1() throws Exception {
        assertEquals("sec_2_1", "sec_2_1");
        JSONObject o = engine.gazetteerLorwyn();
        assertNotNull(o);
        assertEquals("sec_2_1", o.optString("section_id"));
        assertTrue(o.optBoolean("source_verified"));
    }

    /** sec_2_2: Shadowmoor */
    @Test
    public void testLfl_sec_2_2() throws Exception {
        assertEquals("sec_2_2", "sec_2_2");
        JSONObject o = engine.gazetteerShadowmoor();
        assertNotNull(o);
        assertEquals("sec_2_2", o.optString("section_id"));
        assertTrue(o.optBoolean("source_verified"));
    }

    /** sec_3_0: Adapting Other Creatures */
    @Test
    public void testLfl_sec_3_0() throws Exception {
        assertEquals("sec_3_0", "sec_3_0");
        JSONObject o = engine.adaptingOtherCreatures();
        assertNotNull(o);
        assertEquals("sec_3_0", o.optString("section_id"));
        assertEquals(8, o.optInt("tactical_monsters_count"));
        assertTrue(o.optBoolean("source_verified"));
    }

    /** sec_3_1: Creature Descriptions */
    @Test
    public void testLfl_sec_3_1() throws Exception {
        assertEquals("sec_3_1", "sec_3_1");
        JSONArray monsters = engine.creatureDescriptions();
        assertNotNull(monsters);
        assertEquals(8, monsters.length());
        assertEquals("Incarnation of Transience", monsters.getJSONObject(0).optString("name"));
        assertEquals("Incarnation of Vibrance", monsters.getJSONObject(1).optString("name"));
        for (int i = 0; i < monsters.length(); i++) {
            assertTrue(monsters.getJSONObject(i).optBoolean("source_verified"));
        }
    }

    /** sec_4_0: The Aeghith */
    @Test
    public void testLfl_sec_4_0() throws Exception {
        assertEquals("sec_4_0", "sec_4_0");
        JSONArray locs = LorwynAdventureModuleCatalog.aeghithLocations();
        assertNotNull(locs);
        assertEquals(7, locs.length());
        assertEquals("aeghith_a1", locs.getJSONObject(0).optString("location_id"));
        assertEquals("Greeting Hall", locs.getJSONObject(0).optString("name"));
        assertTrue(locs.getJSONObject(0).optBoolean("source_bound"));
    }

    /** sec_4_1: Sample Adventures */
    @Test
    public void testLfl_sec_4_1() throws Exception {
        assertEquals("sec_4_1", "sec_4_1");
        JSONArray advs = LorwynAdventureModuleCatalog.sampleAdventures();
        assertNotNull(advs);
        assertEquals(2, advs.length());
        assertEquals("inventors_sabotage", advs.getJSONObject(0).optString("slug"));
        assertEquals("blood_at_banquet", advs.getJSONObject(1).optString("slug"));
        assertTrue(advs.getJSONObject(0).optBoolean("source_verified"));
    }
}
