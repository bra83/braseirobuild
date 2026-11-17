package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/** Season 01 is opt-in content, never a parallel or permissive rules engine. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public final class FaerunSeasonRuntimeRobolectricTest {
    private JSONObject pack() throws Exception {
        return FaerunSeasonRuntime.loadAsset(RuntimeEnvironment.getApplication());
    }

    @Test public void seasonAssetHasEightCanonicalAnchorsAndExpectedContent() throws Exception {
        JSONObject pack = pack();
        FaerunSeasonRuntime.validate(pack);
        assertEquals("braseiro.faerun.season/1", pack.getString("schema"));
        assertEquals("dnd5e-2024", pack.getString("rules_profile"));
        assertEquals("open_sandbox", pack.getString("adventure_mode"));
        assertEquals(8, pack.getJSONArray("places").length());
        assertEquals(5, pack.getJSONArray("factions").length());
        assertEquals(6, pack.getJSONArray("arcs").length());
        assertEquals(10, pack.getJSONArray("adventures").length());
        assertEquals(12, pack.getJSONArray("encounters").length());
        assertEquals(10, pack.getJSONArray("iconic_locations").length());
        assertEquals(4, pack.getJSONArray("clock_templates").length());
        JSONObject entry = pack.getJSONObject("start_hex_requirement");
        assertEquals("Yartar", entry.getString("city"));
        assertEquals("DM28", entry.getString("hex"));
        assertTrue(entry.getBoolean("must_match_installed_atlas"));
        Set<String> ids = new HashSet<>();
        JSONArray places = pack.getJSONArray("places");
        for (int i = 0; i < places.length(); i++) {
            JSONObject place = places.getJSONObject(i);
            assertTrue(ids.add(place.getString("id")));
            assertTrue(place.getBoolean("canonical_place"));
            assertEquals("exact_catalog_match_required",
                place.getString("atlas_resolution"));
            if (!"yartar".equals(place.getString("id")))
                assertTrue("No guessed hex for " + place, place.isNull("atlas_hex"));
        }
    }

    @Test public void onlyFaerunAfterStrixhavenIsEligible() {
        assertTrue(FaerunSeasonRuntime.eligible("dnd", false));
        assertFalse(FaerunSeasonRuntime.eligible("dnd", true));
        assertFalse(FaerunSeasonRuntime.eligible("mystara", false));
        assertFalse(FaerunSeasonRuntime.eligible("strixhaven", false));
        assertFalse(FaerunSeasonRuntime.eligible("fl", false));
        assertFalse(FaerunSeasonRuntime.eligible(null, false));
    }

    @Test public void leadsAreOptInAndNeverPreAcceptAQuest() throws Exception {
        JSONObject pack = pack();
        JSONObject quiet = FaerunSeasonRuntime.previewForPlace(
            pack, "yartar", "Eu observo os telhados.", 11L);
        assertEquals("on_demand_only", quiet.getString("status"));
        assertFalse(quiet.has("leads_director_only"));

        JSONObject asked = FaerunSeasonRuntime.previewForPlace(
            pack, "yartar", "Quais trabalhos estão disponíveis?", 11L);
        assertEquals("optional_candidates", asked.getString("status"));
        assertTrue(asked.getBoolean("no_state_mutation"));
        JSONArray candidates = asked.getJSONArray("leads_director_only");
        assertEquals(2, candidates.length());
        for (int i=0; i<candidates.length(); i++) {
            JSONObject candidate = candidates.getJSONObject(i);
            assertEquals("yartar", candidate.getString("canonical_anchor"));
            assertTrue(candidate.getBoolean("offer_not_accepted"));
            assertTrue(candidate.getBoolean("not_yet_discovered"));
            assertFalse(candidate.has("mechanical_result"));
            assertFalse(candidate.has("xp"));
            assertFalse(candidate.has("reward"));
            assertFalse(candidate.has("secrets"));
        }
    }

    @Test public void unknownOrUnrelatedLocationNeverSpawnsAdventure() throws Exception {
        JSONObject pack = pack();
        JSONObject unknown = FaerunSeasonRuntime.previewForPlace(
            pack, "made_up_place", "trabalho", 9L);
        assertEquals("place_not_in_season", unknown.getString("status"));
        JSONObject withoutLead = FaerunSeasonRuntime.previewForPlace(
            pack, "neverwinter", "trabalho", 9L);
        assertEquals("no_local_lead", withoutLead.getString("status"));
    }

    @Test public void d6GeneratorIsRepeatableAndPreviewOnly() throws Exception {
        JSONObject pack = pack();
        JSONObject first = FaerunSeasonRuntime.generate(pack, 20261115L);
        JSONObject again = FaerunSeasonRuntime.generate(pack, 20261115L);
        assertEquals(first.toString(), again.toString());
        assertEquals("premise_preview_only", first.getString("type"));
        assertTrue(first.getBoolean("no_gameplay_commit"));
        assertEquals(6, first.getJSONArray("dice_receipts").length());
        assertEquals(6, first.getJSONObject("choices").length());
        for (int i=0; i<6; i++) {
            JSONObject receipt = first.getJSONArray("dice_receipts").getJSONObject(i);
            assertTrue(receipt.getInt("subcategory_d6") >= 1);
            assertTrue(receipt.getInt("subcategory_d6") <= 6);
            assertTrue(receipt.getInt("entry_d6") >= 1);
            assertTrue(receipt.getInt("entry_d6") <= 6);
        }
        assertFalse(first.has("quest_accepted"));
        assertFalse(first.has("skill_check_outcome"));
    }

    @Test public void packCannotCrossEditionOrAcceptDuplicateIds() throws Exception {
        JSONObject wrong = new JSONObject(pack().toString());
        wrong.put("rules_profile", "dnd5e-2014");
        try { FaerunSeasonRuntime.validate(wrong); fail("Edition firewall"); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("firewall")); }

        JSONObject duplicate = new JSONObject(pack().toString());
        JSONArray adventures = duplicate.getJSONArray("adventures");
        adventures.getJSONObject(1).put("id", adventures.getJSONObject(0).getString("id"));
        try { FaerunSeasonRuntime.validate(duplicate); fail("Duplicate ID firewall"); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("Duplicate")); }
    }

    @Test public void cachedCatalogCannotBeModifiedByCallers() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        JSONObject mutable = FaerunSeasonRuntime.loadAsset(context);
        mutable.put("title", "TAMPERED");
        assertEquals("Temporada 01 — As Contas da Estrada Longa",
                FaerunSeasonRuntime.loadAsset(context).getString("title"));
    }

    @Test public void everyAdventureRemainsOptionalAndRequiresRulesHost() throws Exception {
        JSONArray a = pack().getJSONArray("adventures");
        for (int i=0; i<a.length(); i++) {
            JSONObject entry = a.getJSONObject(i);
            assertTrue(entry.getBoolean("no_forced_outcome"));
            assertEquals("host_dnd2024", entry.getString("mechanical_authority"));
            assertTrue(entry.getJSONArray("level_range").getInt(0) >= 1);
            assertFalse(entry.getString("hook").isEmpty());
            assertFalse(entry.getString("complication").isEmpty());
            assertFalse(entry.getString("if_ignored").isEmpty());
        }
    }
}
