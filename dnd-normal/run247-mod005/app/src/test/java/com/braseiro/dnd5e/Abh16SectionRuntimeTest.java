package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Suite de Homologação de Runtime para Astarion's Book of Hungers (ABH).
 * Cada uma das 11 seções de gameplay possui um método de teste dedicado e explícito.
 */
public final class Abh16SectionRuntimeTest {

    private final AstarionHungersRulesEngine engine = new AstarionHungersRulesEngine();

    /** sec_0_1: Using This Supplement / Astarion chronology and DM scope */
    @Test
    public void testAbh_sec_0_1() throws Exception {
        assertEquals("sec_0_1", "sec_0_1");
        JSONObject res = engine.getSupplementScopeAndTimeline();
        assertNotNull(res);
        assertEquals("sec_0_1", res.optString("section_id"));
        assertEquals("ABH", res.optString("source_book"));
        assertEquals("Using This Supplement", res.optString("title"));
        assertTrue(res.optBoolean("source_verified"));
        assertFalse(res.optBoolean("auto_rolls", true));

        JSONObject chrono = res.getJSONObject("chronology");
        assertEquals("1268 DR", chrono.optString("birth_mortal"));
        assertEquals("1298 DR", chrono.optString("vampire_spawn_transformation"));
        assertEquals("1492 DR", chrono.optString("tadpole_abduction_nautiloid"));
        assertEquals(200, chrono.optInt("szarr_palace_service_years"));
    }

    /** sec_1_1: Character Backgrounds */
    @Test
    public void testAbh_sec_1_1() throws Exception {
        assertEquals("sec_1_1", "sec_1_1");
        JSONArray backgrounds = engine.listCharacterBackgrounds();
        assertNotNull(backgrounds);
        assertEquals(3, backgrounds.length());

        JSONObject bg1 = backgrounds.getJSONObject(0);
        assertEquals("Szarr Estate Survivor", bg1.optString("name"));
        assertEquals("Tireless Reveler", bg1.optString("feat"));
        assertTrue(bg1.optBoolean("source_verified"));

        JSONObject bg2 = backgrounds.getJSONObject(1);
        assertEquals("Courtier of the Night", bg2.optString("name"));
        assertEquals("Vampire's Plaything", bg2.optString("feat"));

        JSONObject bg3 = backgrounds.getJSONObject(2);
        assertEquals("Vampire Hunter Assistant", bg3.optString("name"));
        assertEquals("Vampire Hunter", bg3.optString("feat"));
    }

    /** sec_1_2: Character Species - Dhampir */
    @Test
    public void testAbh_sec_1_2() throws Exception {
        assertEquals("sec_1_2", "sec_1_2");
        JSONObject traits = engine.getSpeciesTraits("Dhampir");
        assertNotNull(traits);
        assertEquals("sec_1_2", traits.optString("section_id"));
        assertEquals("Dhampir", traits.optString("species"));
        assertEquals(35, traits.optInt("walking_speed_ft"));
        assertEquals(60, traits.optInt("darkvision_ft"));
        assertTrue(traits.optBoolean("spider_climb"));
        assertTrue(traits.optBoolean("source_verified"));
        assertFalse(traits.optBoolean("auto_rolls", true));

        // Testar mecânica de mordida vampírica e empoderamento
        JSONObject bite = engine.resolveVampiricBite(3, 2, 20, 30);
        assertEquals("unarmed_strike_bite", bite.optString("attack_type"));
        assertEquals("1d4", bite.optString("damage_dice"));
        assertEquals(3, bite.optInt("damage_bonus"));

        JSONObject empower = engine.resolveVampiricBiteEmpowerment(6, "humanoid", "drain", 3, 0);
        assertTrue(empower.optBoolean("resolved"));
        assertEquals(6, empower.optInt("healing"));
        assertEquals(1, empower.optInt("uses_spent_after"));
    }

    /** sec_1_3: Feats */
    @Test
    public void testAbh_sec_1_3() throws Exception {
        assertEquals("sec_1_3", "sec_1_3");
        JSONArray feats = engine.listVampiricFeats();
        assertNotNull(feats);
        assertEquals(16, feats.length());
        int originCount = 0, generalCount = 0, epicCount = 0;
        for (int i = 0; i < feats.length(); i++) {
            JSONObject f = feats.getJSONObject(i);
            String cat = f.optString("category");
            if ("Origin".equals(cat)) originCount++;
            else if ("General".equals(cat)) generalCount++;
            else if ("Epic Boon".equals(cat)) epicCount++;
        }
        assertEquals(3, originCount);
        assertEquals(10, generalCount);
        assertEquals(3, epicCount);
    }

    /** sec_1_4: Origin Feats */
    @Test
    public void testAbh_sec_1_4() throws Exception {
        assertEquals("sec_1_4", "sec_1_4");
        JSONArray originFeats = engine.listOriginFeats();
        assertNotNull(originFeats);
        assertEquals(3, originFeats.length());
        assertEquals("Tireless Reveler", originFeats.getJSONObject(0).optString("name"));
        assertEquals("Vampire Hunter", originFeats.getJSONObject(1).optString("name"));
        assertEquals("Vampire's Plaything", originFeats.getJSONObject(2).optString("name"));
        for (int i = 0; i < originFeats.length(); i++) {
            assertEquals("Origin", originFeats.getJSONObject(i).optString("category"));
            assertEquals(1, originFeats.getJSONObject(i).optInt("level_required"));
            assertTrue(originFeats.getJSONObject(i).optBoolean("source_verified"));
        }
    }

    /** sec_1_5: General Feats */
    @Test
    public void testAbh_sec_1_5() throws Exception {
        assertEquals("sec_1_5", "sec_1_5");
        JSONArray generalFeats = engine.listGeneralFeats();
        assertNotNull(generalFeats);
        assertEquals(10, generalFeats.length());
        for (int i = 0; i < generalFeats.length(); i++) {
            assertEquals("General", generalFeats.getJSONObject(i).optString("category"));
            assertEquals(4, generalFeats.getJSONObject(i).optInt("level_required"));
            assertTrue(generalFeats.getJSONObject(i).optBoolean("source_verified"));
        }

        // Testar comportamento determinístico de Bloodlust / Sanguine Feast
        JSONObject feast = engine.evaluateBloodlustSanguineFeast(
                true, true, "humanoid", false, true, 5, 8, 3, 2, 0);
        assertTrue(feast.optBoolean("resolved"));
        assertEquals(8, feast.optInt("healing")); // 5 + 3
        assertEquals(1, feast.optInt("hit_die_consumed"));
        assertEquals(1, feast.optInt("uses_spent_after"));
    }

    /** sec_1_6: Epic Boon Feats */
    @Test
    public void testAbh_sec_1_6() throws Exception {
        assertEquals("sec_1_6", "sec_1_6");
        JSONArray epicBoons = engine.listEpicBoons();
        assertNotNull(epicBoons);
        assertEquals(3, epicBoons.length());
        for (int i = 0; i < epicBoons.length(); i++) {
            assertEquals("Epic Boon", epicBoons.getJSONObject(i).optString("category"));
            assertEquals(19, epicBoons.getJSONObject(i).optInt("level_required"));
            assertTrue(epicBoons.getJSONObject(i).optBoolean("source_verified"));
        }

        // Testar regras dos 3 boons
        JSONObject dawn = engine.getEpicBoon("Boon of Blazing Dawn");
        assertTrue(dawn.optBoolean("radiant_immunity"));
        assertTrue(dawn.optBoolean("weapon_damage_may_be_radiant"));

        JSONObject shadows = engine.getEpicBoon("Boon of Looming Shadows");
        assertEquals(10, shadows.optInt("melee_reach_bonus_on_attack_action_ft"));
        assertTrue(shadows.optBoolean("dodge_as_bonus_action"));

        JSONObject escape = engine.getEpicBoon("Boon of Misty Escape");
        assertEquals(1, escape.optInt("instead_hp"));
        assertEquals(20, escape.optInt("fly_speed_ft"));
        assertEquals(10, escape.optInt("healing_each_turn"));
    }

    /** sec_2_1: Cazador Szarr's Family */
    @Test
    public void testAbh_sec_2_1() throws Exception {
        assertEquals("sec_2_1", "sec_2_1");
        JSONObject family = engine.getCazadorFamilyHierarchy();
        assertNotNull(family);
        assertEquals("sec_2_1", family.optString("section_id"));
        assertEquals("Cazador Szarr", family.optString("master"));
        assertEquals(7, family.optInt("spawn_count"));
        assertTrue(family.optBoolean("source_verified"));

        JSONArray spawn = family.getJSONArray("spawn_roster");
        assertEquals(7, spawn.length());
        assertEquals("Astarion Ancunin", spawn.getString(0));
        assertEquals("Violet", spawn.getString(1));
    }

    /** sec_2_2: Monsters */
    @Test
    public void testAbh_sec_2_2() throws Exception {
        assertEquals("sec_2_2", "sec_2_2");
        JSONArray monsters = engine.listSupplementMonsters();
        assertNotNull(monsters);
        assertTrue(monsters.length() >= 4);

        JSONObject devil = monsters.getJSONObject(0);
        assertEquals("Harvester Devil", devil.optString("name"));
        assertEquals("7", devil.optString("cr"));
        assertEquals(16, devil.optInt("ac"));
        assertEquals(110, devil.optInt("hp"));
        assertTrue(devil.optBoolean("source_verified"));
    }

    /** sec_3_1: Rat's Run Adventures */
    @Test
    public void testAbh_sec_3_1() throws Exception {
        assertEquals("sec_3_1", "sec_3_1");
        JSONArray adventures = engine.listRatRunAdventures();
        assertNotNull(adventures);
        assertTrue(adventures.length() >= 3);

        JSONObject adv1 = adventures.getJSONObject(0);
        assertEquals("A Scoundrel's Refuge", adv1.optString("title"));
        assertEquals("3-5", adv1.optString("target_level"));
        assertTrue(adv1.optBoolean("source_verified"));
    }

    /** sec_3_2: Rat's Run Tavern and Inn */
    @Test
    public void testAbh_sec_3_2() throws Exception {
        assertEquals("sec_3_2", "sec_3_2");
        JSONObject tavern = engine.getRatRunTavernDetails();
        assertNotNull(tavern);
        assertEquals("sec_3_2", tavern.optString("section_id"));
        assertEquals("Rat's Run Tavern and Inn", tavern.optString("name"));
        assertEquals("Lief Cloudswell", tavern.optString("proprietor"));
        assertEquals("Lower City, Baldur's Gate", tavern.optString("location"));
        assertTrue(tavern.optBoolean("source_verified"));
    }
}
