package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class Dmg2024RulesRobolectricTest {

    @Test
    public void testDescriptorDeclaresAllProcedures() {
        JSONObject desc = Dmg2024RulesEngine.descriptor();
        assertEquals("dmg2024_runtime", desc.optString("id"));
        assertEquals("Dungeon Master's Guide (2024)", desc.optString("book"));
        assertFalse(desc.optBoolean("auto_rolls", true));
        JSONArray procs = desc.optJSONArray("procedures");
        assertNotNull(procs);
        assertTrue(procs.length() >= 10);
    }

    @Test
    public void testMagicItemCatalogAndFilters() throws Exception {
        JSONObject all = Dmg2024RulesEngine.execute("dmg2024.magic_item.catalog", null);
        assertEquals("ok", all.optString("status"));
        assertTrue(all.optInt("total_in_catalog") >= 15);

        // Filter by Rare
        JSONObject rare = Dmg2024RulesEngine.execute("dmg2024.magic_item.catalog",
                new JSONObject().put("rarity", "Rare"));
        assertEquals("ok", rare.optString("status"));
        JSONArray rareItems = rare.getJSONArray("items");
        assertTrue(rareItems.length() > 0);
        for (int i = 0; i < rareItems.length(); i++) {
            assertEquals("Rare", rareItems.getJSONObject(i).optString("rarity"));
        }

        // Filter by Potion
        JSONObject potions = Dmg2024RulesEngine.execute("dmg2024.magic_item.catalog",
                new JSONObject().put("category", "Potion"));
        JSONArray potItems = potions.getJSONArray("items");
        assertTrue(potItems.length() >= 4);
    }

    @Test
    public void testAttunementLifecycle() throws Exception {
        // Item that doesn't need attunement
        JSONObject noAttune = Dmg2024RulesEngine.execute("dmg2024.magic_item.attune",
                new JSONObject().put("item_id", "bag_of_holding"));
        assertEquals("blocked", noAttune.optString("status"));
        assertEquals("item_does_not_require_attunement", noAttune.optString("reason"));

        // Cloak of Protection needs short rest
        JSONObject noRest = Dmg2024RulesEngine.execute("dmg2024.magic_item.attune",
                new JSONObject().put("item_id", "cloak_of_protection")
                        .put("short_rest_completed", false));
        assertEquals("blocked", noRest.optString("status"));
        assertEquals("short_rest_required_to_attune", noRest.optString("reason"));

        // Successful attunement with short rest
        JSONObject attuneOk = Dmg2024RulesEngine.execute("dmg2024.magic_item.attune",
                new JSONObject().put("item_id", "cloak_of_protection")
                        .put("short_rest_completed", true)
                        .put("current_attuned_items", new JSONArray()));
        assertEquals("ok", attuneOk.optString("status"));
        assertTrue(attuneOk.optBoolean("attuned"));
        assertEquals(1, attuneOk.optInt("slots_used"));

        // Attunement capacity exceeded (max 3)
        JSONArray fullSlots = new JSONArray().put("item_1").put("item_2").put("item_3");
        JSONObject fullBlocked = Dmg2024RulesEngine.execute("dmg2024.magic_item.attune",
                new JSONObject().put("item_id", "ring_of_protection")
                        .put("short_rest_completed", true)
                        .put("current_attuned_items", fullSlots));
        assertEquals("blocked", fullBlocked.optString("status"));
        assertEquals("attunement_capacity_exceeded", fullBlocked.optString("reason"));

        // Unattune
        JSONObject unattune = Dmg2024RulesEngine.execute("dmg2024.magic_item.unattune",
                new JSONObject().put("item_id", "cloak_of_protection"));
        assertEquals("ok", unattune.optString("status"));
        assertTrue(unattune.optBoolean("unattuned"));
    }

    @Test
    public void testMagicItemValueRanges() throws Exception {
        JSONObject commonConsumable = Dmg2024RulesEngine.execute("dmg2024.magic_item.value",
                new JSONObject().put("rarity", "Common").put("consumable", true));
        assertEquals("ok", commonConsumable.optString("status"));
        assertEquals("25 - 50", commonConsumable.optString("price_range_gp"));

        JSONObject uncommonPermanent = Dmg2024RulesEngine.execute("dmg2024.magic_item.value",
                new JSONObject().put("rarity", "Uncommon").put("consumable", false));
        assertEquals("ok", uncommonPermanent.optString("status"));
        assertEquals("101 - 500", uncommonPermanent.optString("price_range_gp"));

        JSONObject rarePermanent = Dmg2024RulesEngine.execute("dmg2024.magic_item.value",
                new JSONObject().put("rarity", "Rare").put("consumable", false));
        assertEquals("ok", rarePermanent.optString("status"));
        assertEquals("501 - 5000", rarePermanent.optString("price_range_gp"));
    }

    @Test
    public void testEnvironmentalHazards() throws Exception {
        // Extreme Cold with gear -> auto pass
        JSONObject coldProtected = Dmg2024RulesEngine.execute("dmg2024.hazard.resolve",
                new JSONObject().put("hazard", "extreme_cold").put("has_cold_weather_gear", true));
        assertEquals("ok", coldProtected.optString("status"));
        assertFalse(coldProtected.optBoolean("save_required"));
        assertEquals(0, coldProtected.optInt("exhaustion_gained"));

        // Extreme Cold without gear -> DC 10 Con save
        JSONObject coldFail = Dmg2024RulesEngine.execute("dmg2024.hazard.resolve",
                new JSONObject().put("hazard", "extreme_cold").put("save_success", false));
        assertEquals("ok", coldFail.optString("status"));
        assertTrue(coldFail.optBoolean("save_required"));
        assertEquals(1, coldFail.optInt("exhaustion_gained"));

        // Extreme Heat with medium/heavy armor
        JSONObject heatArmor = Dmg2024RulesEngine.execute("dmg2024.hazard.resolve",
                new JSONObject().put("hazard", "extreme_heat")
                        .put("wearing_medium_or_heavy_armor", true)
                        .put("hours_exposed", 2));
        assertEquals(11, heatArmor.optInt("save_dc")); // 10 + (2-1) = 11

        // Heavy precipitation & Strong wind
        JSONObject precip = Dmg2024RulesEngine.execute("dmg2024.hazard.resolve",
                new JSONObject().put("hazard", "heavy_precipitation"));
        assertTrue(precip.optBoolean("perception_disadvantage"));
        assertTrue(precip.optBoolean("open_flames_extinguished"));

        JSONObject wind = Dmg2024RulesEngine.execute("dmg2024.hazard.resolve",
                new JSONObject().put("hazard", "strong_wind"));
        assertTrue(wind.optBoolean("ranged_weapon_attack_disadvantage"));
        assertTrue(wind.optBoolean("flying_creatures_must_land"));
    }

    @Test
    public void testTrapsResolution() throws Exception {
        // Setback trap
        JSONObject setback = Dmg2024RulesEngine.execute("dmg2024.trap.resolve",
                new JSONObject().put("severity", "setback").put("damage_roll", 8).put("save_success", true));
        assertEquals("ok", setback.optString("status"));
        assertEquals(11, setback.optInt("save_dc"));
        assertEquals("1d10", setback.optString("damage_dice"));
        assertEquals(4, setback.optInt("applied_damage")); // 8 / 2 = 4 on successful save

        // Deadly trap
        JSONObject deadly = Dmg2024RulesEngine.execute("dmg2024.trap.resolve",
                new JSONObject().put("severity", "deadly").put("damage_roll", 50).put("save_success", false));
        assertEquals("ok", deadly.optString("status"));
        assertEquals(18, deadly.optInt("save_dc"));
        assertEquals("10d10", deadly.optString("damage_dice"));
        assertEquals(50, deadly.optInt("applied_damage"));
    }

    @Test
    public void testFallingAndSuffocation() throws Exception {
        // Falling 50 feet
        JSONObject fall50 = Dmg2024RulesEngine.execute("dmg2024.falling.resolve",
                new JSONObject().put("distance_feet", 50));
        assertEquals("ok", fall50.optString("status"));
        assertEquals("5d6", fall50.optString("damage_dice"));
        assertEquals("Prone", fall50.optString("condition_applied"));

        // Falling 300 feet (capped at 20d6)
        JSONObject fall300 = Dmg2024RulesEngine.execute("dmg2024.falling.resolve",
                new JSONObject().put("distance_feet", 300));
        assertEquals("20d6", fall300.optString("damage_dice"));

        // Suffocation: Con mod +2
        JSONObject suff = Dmg2024RulesEngine.execute("dmg2024.suffocation.resolve",
                new JSONObject().put("constitution_modifier", 2));
        assertEquals("ok", suff.optString("status"));
        assertEquals(3, suff.optInt("breath_held_minutes")); // 1 + 2 = 3
        assertEquals(2, suff.optInt("suffocation_survival_rounds")); // 2 rounds before dropping to 0 HP
    }

    @Test
    public void testTreasureTables() throws Exception {
        // Individual CR 3, roll 85 -> 3d6 GP
        JSONObject indv = Dmg2024RulesEngine.execute("dmg2024.treasure.individual",
                new JSONObject().put("cr", 3).put("d100_roll", 85));
        assertEquals("ok", indv.optString("status"));
        assertEquals("3d6 GP", indv.optString("coins_formula"));

        // Hoard CR 6, roll 50
        JSONObject hoard = Dmg2024RulesEngine.execute("dmg2024.treasure.hoard",
                new JSONObject().put("cr", 6).put("d100_roll", 50));
        assertEquals("ok", hoard.optString("status"));
        assertTrue(hoard.has("magic_items_formula"));
    }
}
