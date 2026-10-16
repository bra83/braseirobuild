package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class Bastion2024SpecialFacilitiesRobolectricTest {
    private Context context;
    private File dbFile;
    private AppDb db;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        dbFile = context.getDatabasePath("v1_sf_" + shortId + ".db");
        if (dbFile.exists()) dbFile.delete();
        db = new AppDb(context, dbFile.getAbsolutePath());
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
            db = null;
        }
        if (dbFile != null && dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    public void testCatalogContainsAll29FacilitiesWithCorrectLevelDistribution() throws Exception {
        JSONObject res = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_catalog", null);
        assertEquals("ok", res.optString("status"));
        assertEquals(29, res.optInt("total_in_catalog"));
        JSONArray facilities = res.getJSONArray("facilities");
        assertEquals(29, facilities.length());

        int countLv5 = 0, countLv9 = 0, countLv13 = 0, countLv17 = 0;
        Set<String> uniqueIds = new HashSet<>();

        for (int i = 0; i < facilities.length(); i++) {
            JSONObject fac = facilities.getJSONObject(i);
            String id = fac.getString("id");
            assertTrue("ID duplicado no catálogo: " + id, uniqueIds.add(id));
            assertFalse(fac.getString("name_en").isEmpty());
            assertFalse(fac.getString("name_pt").isEmpty());
            assertTrue(fac.getJSONArray("allowed_orders").length() > 0);

            int lvl = fac.getInt("level_req");
            if (lvl == 5) countLv5++;
            else if (lvl == 9) countLv9++;
            else if (lvl == 13) countLv13++;
            else if (lvl == 17) countLv17++;
            else fail("Nível inesperado de instalação: " + lvl);
        }

        assertEquals("Instalações nível 5", 9, countLv5);
        assertEquals("Instalações nível 9", 10, countLv9);
        assertEquals("Instalações nível 13", 6, countLv13);
        assertEquals("Instalações nível 17", 4, countLv17);
    }

    @Test
    public void testCatalogFiltersByLevelAndOrder() throws Exception {
        // Filter by level 5
        JSONObject queryLv5 = new JSONObject().put("character_level", 5);
        JSONObject resLv5 = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_catalog", queryLv5);
        assertEquals(9, resLv5.getJSONArray("facilities").length());

        // Filter by level 9
        JSONObject queryLv9 = new JSONObject().put("character_level", 9);
        JSONObject resLv9 = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_catalog", queryLv9);
        assertEquals(19, resLv9.getJSONArray("facilities").length());

        // Filter by order Recruit
        JSONObject queryRecruit = new JSONObject().put("order", "Recruit");
        JSONObject resRecruit = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_catalog", queryRecruit);
        // Barracks and Guildhall
        assertEquals(2, resRecruit.getJSONArray("facilities").length());
    }

    @Test
    public void testLookupByEnAndPtNamesAndAliases() {
        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("arcane_study"));
        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("Arcane Study"));
        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("Estudo Arcano"));
        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("gabinete_arcano"));

        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("barracks"));
        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("barrack"));
        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("Quartel"));

        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("demiplane"));
        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("semiplano"));
        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("semi_plano"));

        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("sanctum"));
        assertNotNull(Bastion2024SpecialFacilitiesEngine.getFacility("sanctum_sanctorum"));

        assertNull(Bastion2024SpecialFacilitiesEngine.getFacility("non_existent_facility_xyz"));
    }

    @Test
    public void testPrerequisiteValidations() throws Exception {
        // Hero level < 5
        JSONObject valLv4 = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 4).put("facility_id", "barracks"));
        assertEquals("blocked", valLv4.optString("status"));
        assertEquals("hero_level_five_required", valLv4.optString("reason"));

        // Level insufficient for facility
        JSONObject valLv5ForLv9 = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 5).put("facility_id", "gaming_hall"));
        assertEquals("blocked", valLv5ForLv9.optString("status"));
        assertEquals("insufficient_level_for_facility", valLv5ForLv9.optString("reason"));

        // Arcane Study requires arcane spellcasting
        JSONObject valArcaneNo = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 5).put("facility_id", "arcane_study")
                        .put("character_class", "Fighter"));
        assertEquals("blocked", valArcaneNo.optString("status"));
        assertEquals("arcane_spellcasting_focus_required", valArcaneNo.optString("reason"));

        JSONObject valArcaneYes = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 5).put("facility_id", "arcane_study")
                        .put("character_class", "Wizard"));
        assertEquals("ok", valArcaneYes.optString("status"));

        // Sanctuary requires divine/primal spellcasting
        JSONObject valSancNo = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 5).put("facility_id", "sanctuary")
                        .put("character_class", "Rogue"));
        assertEquals("blocked", valSancNo.optString("status"));
        assertEquals("divine_or_primal_focus_required", valSancNo.optString("reason"));

        JSONObject valSancYes = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 5).put("facility_id", "sanctuary")
                        .put("character_class", "Cleric"));
        assertEquals("ok", valSancYes.optString("status"));

        // Archive requires existing Library
        JSONObject valArchNo = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 13).put("facility_id", "archive")
                        .put("existing_facilities", new JSONArray().put("barracks").put("smithy")));
        assertEquals("blocked", valArchNo.optString("status"));
        assertEquals("prerequisite_library_required", valArchNo.optString("reason"));

        JSONObject valArchYes = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 13).put("facility_id", "archive")
                        .put("existing_facilities", new JSONArray().put("library").put("smithy")));
        assertEquals("ok", valArchYes.optString("status"));

        // Menagerie requires existing Stable
        JSONObject valMenNo = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 13).put("facility_id", "menagerie")
                        .put("existing_facilities", new JSONArray().put("barracks")));
        assertEquals("blocked", valMenNo.optString("status"));
        assertEquals("prerequisite_stable_required", valMenNo.optString("reason"));

        JSONObject valMenYes = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 13).put("facility_id", "menagerie")
                        .put("existing_facilities", new JSONArray().put("stable")));
        assertEquals("ok", valMenYes.optString("status"));

        // Guildhall requires Expertise
        JSONObject valGuildNo = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 17).put("facility_id", "guildhall")
                        .put("character_class", "Fighter"));
        assertEquals("blocked", valGuildNo.optString("status"));
        assertEquals("expertise_feature_required", valGuildNo.optString("reason"));

        JSONObject valGuildYes = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 17).put("facility_id", "guildhall")
                        .put("character_class", "Rogue"));
        assertEquals("ok", valGuildYes.optString("status"));

        // War Room requires Fighting Style or Unarmored Defense
        JSONObject valWarNo = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 17).put("facility_id", "war_room")
                        .put("character_class", "Wizard"));
        assertEquals("blocked", valWarNo.optString("status"));
        assertEquals("fighting_style_or_unarmored_defense_required", valWarNo.optString("reason"));

        JSONObject valWarYes = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 17).put("facility_id", "war_room")
                        .put("character_class", "Barbarian"));
        assertEquals("ok", valWarYes.optString("status"));

        // Capacity limit: Level 5 allows 2 facilities
        JSONObject valCap = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 5).put("facility_id", "garden")
                        .put("existing_facilities", new JSONArray().put("barracks").put("smithy")));
        assertEquals("blocked", valCap.optString("status"));
        assertEquals("bastion_special_facility_capacity_reached", valCap.optString("reason"));

        // Duplicate facility
        JSONObject valDup = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_validate",
                new JSONObject().put("character_level", 9).put("facility_id", "barracks")
                        .put("existing_facilities", new JSONArray().put("barracks")));
        assertEquals("blocked", valDup.optString("status"));
        assertEquals("duplicate_special_facility_not_allowed", valDup.optString("reason"));
    }

    @Test
    public void testOrderResolutionDeterministicOutputs() throws Exception {
        // Recruit on Barracks (+4 defenders)
        JSONObject recBarracks = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_order_resolve",
                new JSONObject().put("facility_id", "barracks").put("order", "Recruit"));
        assertEquals("ok", recBarracks.optString("status"));
        assertEquals(4, recBarracks.optInt("defenders_delta"));
        assertTrue(recBarracks.optBoolean("requires_dm_confirmation"));
        assertFalse(recBarracks.optBoolean("auto_rolls"));

        // Recruit on Guildhall (+8 defenders)
        JSONObject recGuild = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_order_resolve",
                new JSONObject().put("facility_id", "guildhall").put("order", "Recruit"));
        assertEquals("ok", recGuild.optString("status"));
        assertEquals(8, recGuild.optInt("defenders_delta"));

        // Unsupported order: Recruit on Library
        JSONObject recLib = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_order_resolve",
                new JSONObject().put("facility_id", "library").put("order", "Recruit"));
        assertEquals("blocked", recLib.optString("status"));
        assertEquals("order_not_supported_by_facility", recLib.optString("reason"));

        // Harvest on Garden
        JSONObject harvGarden = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_order_resolve",
                new JSONObject().put("facility_id", "garden").put("order", "Harvest"));
        assertEquals("ok", harvGarden.optString("status"));
        assertEquals("Harvest", harvGarden.optString("canonical_order"));

        // Trade on Gaming Hall
        JSONObject tradeGaming = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_order_resolve",
                new JSONObject().put("facility_id", "gaming_hall").put("order", "Trade"));
        assertEquals("ok", tradeGaming.optString("status"));
        assertTrue(tradeGaming.optInt("gold_delta_gp") > 0);

        // Empower on Sanctuary
        JSONObject empSanc = Bastion2024RulesEngine.execute("core2024.bastion.special_facility_order_resolve",
                new JSONObject().put("facility_id", "sanctuary").put("order", "Empower"));
        assertEquals("ok", empSanc.optString("status"));
        assertEquals("Empower", empSanc.optString("canonical_order"));
    }

    @Test
    public void testAtomicDbIntegrationWithReplayToken() throws Exception {
        db.setActiveMapProfile("dnd");
        db.saveCampaign("DMG Bastion V1", DndRulesEngine.PROFILE_2024,
                "Sandbox", true, true, false, "");
        long heroId = db.addCharacter("Valen", "Humano", "Guerreiro", 5, 44, 44, 17,
                16, 13, 15, 10, 12, 8, true, "Soldado");
        assertTrue(heroId > 0);

        // Configure Bastion with explicit DM confirmation
        JSONArray initialFacilities = new JSONArray().put("smithy");
        JSONObject conf = db.configureBastion2024(heroId, 500, 2, initialFacilities, true);
        assertEquals("ok", conf.optJSONObject("bastion_state").optString("status"));
        assertTrue(conf.optBoolean("bastion_configured"));

        // Add barracks facility
        JSONObject addFac = db.addBastion2024SpecialFacility(heroId, "barracks", true);
        assertTrue(addFac.optBoolean("bastion_facility_added"));
        JSONObject stateAfterAdd = db.getBastion2024State(heroId);
        assertTrue(stateAfterAdd.optJSONObject("facilities").has("barracks"));

        // Issue Recruit order on Barracks
        String token = "ui-bastion-order-" + UUID.randomUUID().toString();
        JSONObject orderRes = db.issueBastion2024SpecialFacilityOrder(heroId, "barracks", "Recruit", null, true, token);
        assertTrue(orderRes.optBoolean("bastion_order_committed"));

        // Check that defenders increased from 2 to 6 (2 + 4 = 6)
        JSONObject stateAfterOrder = db.getBastion2024State(heroId);
        assertEquals(6, stateAfterOrder.optInt("defenders"));

        // Replay of same token is blocked
        JSONObject replay = db.issueBastion2024SpecialFacilityOrder(heroId, "barracks", "Recruit", null, true, token);
        assertEquals("already_resolved_or_replayed", replay.optString("bastion_blocked"));

        // Advance turn
        String turnToken = "ui-bastion-turn-" + UUID.randomUUID().toString();
        JSONObject turnRes = db.advanceBastion2024Turn(heroId, true, turnToken);
        assertTrue(turnRes.optBoolean("bastion_turn_committed"));
        assertEquals(1, db.getBastion2024State(heroId).optInt("turn_number"));
    }
}
