package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Source-verified 2024 DMG pp. 350-352: eleven event families, 100 d100 faces,
 * explicit nested dice, choices and non-mutating outputs. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public final class Bastion2024AllEventsRobolectricTest {
    private static JSONObject x(int d100) throws Exception {return new JSONObject().put("d100_roll",d100);}
    private static JSONArray d(int...dice) {JSONArray a=new JSONArray();for(int v:dice)a.put(v);return a;}
    private static JSONObject r(JSONObject in){return Bastion2024RulesEngine.execute("core2024.bastion.event.effects",in);}
    private static void ok(JSONObject j) {assertEquals(j.toString(),"ok",j.optString("status"));assertFalse(j.optBoolean("persisted_to_world",true));assertFalse(j.optBoolean("auto_rolls",true));}
    private static void blocked(JSONObject j,String reason) {assertEquals(j.toString(),"blocked",j.optString("status"));assertEquals(reason,j.optString("reason"));}

    @Test public void allOneHundredD100FacesMapToOfficialElevenBands() throws Exception {
        final int[] upper={50,55,58,63,72,76,79,83,91,98,100};
        final String[] event={"All Is Well","Attack","Criminal Hireling","Extraordinary Opportunity",
            "Friendly Visitors","Guest","Lost Hirelings","Magical Discovery","Refugees","Request for Aid","Treasure"};
        int[] count=new int[11];
        for(int roll=1;roll<=100;roll++){
            JSONObject j=Bastion2024RulesEngine.execute("core2024.bastion.event.resolve",x(roll));
            String found=j.optString("event");
            int index=0;while(roll>upper[index])index++;
            assertEquals("d100="+roll,event[index],found);
            count[index]++;
        }
        assertArrayEquals(new int[]{50,5,3,5,9,4,3,4,8,7,2},count);
        assertEquals("Treasure",r(x(0).put("treasure_d100",100)).optString("event"));
        blocked(r(x(-1)),"invalid_d100_roll");
        blocked(r(x(101)),"invalid_d100_roll");
    }

    @Test public void allIsWellAndAttackDefendersAndFacilityClosure() throws Exception {
        ok(r(x(1).put("detail_d8",8)));
        blocked(r(x(2).put("detail_d8",9)),"invalid_detail_d8");
        JSONObject attack=r(x(55).put("defenders_before",3)
            .put("attack_6d6",d(1,2,1,4,5,6)));
        ok(attack);assertEquals(-2,attack.optInt("defenders_delta"));
        assertEquals(1,attack.optInt("defenders_after"));
        assertFalse(attack.optBoolean("shut_down_facility_next_turn"));
        blocked(r(x(51).put("defenders_before",0).put("attack_6d6",d(2,3,4,5,6,2))),
            "damaged_special_facility_must_be_selected_by_dm");
        JSONObject shut=r(x(51).put("defenders_before",0)
            .put("attack_6d6",d(2,3,4,5,6,2)).put("facility_id","smithy"));
        ok(shut);assertTrue(shut.optBoolean("shut_down_facility_next_turn"));
        assertEquals("smithy",shut.optString("facility_id"));
        blocked(r(x(52).put("defenders_before",2).put("attack_6d6",d(1,2))),
            "attack_six_d6_faces_required");
    }

    @Test public void criminalHirelingBribeOrTemporaryFacilityLoss() throws Exception {
        JSONObject retained=r(x(56).put("retain_hireling",true).put("bribe_d6",4));
        ok(retained);assertEquals(-400,retained.optInt("gold_delta_gp"));
        blocked(r(x(56).put("retain_hireling",true)),"bribe_d6_required");
        JSONObject arrested=r(x(58).put("retain_hireling",false)
            .put("facility_has_no_other_hirelings",true).put("facility_id","laboratory"));
        ok(arrested);assertTrue(arrested.optBoolean("shut_down_facility_next_turn"));
    }

    @Test public void extraordinaryOpportunityMayRequestRerollButNeverAutoRolls() throws Exception {
        JSONObject accept=r(x(59).put("accept_opportunity",true));
        ok(accept);assertEquals(-500,accept.optInt("gold_delta_gp"));
        assertTrue(accept.optBoolean("reroll_bastion_event"));
        assertTrue(accept.optBoolean("reroll_excludes_same_opportunity"));
        JSONObject decline=r(x(63).put("accept_opportunity",false));
        ok(decline);assertEquals(0,decline.optInt("gold_delta_gp"));
        assertFalse(decline.optBoolean("reroll_bastion_event"));
    }

    @Test public void friendlyVisitorsAndAllFourGuestTypes() throws Exception {
        JSONObject visitors=r(x(64).put("payment_d6",6));
        ok(visitors);assertEquals(600,visitors.optInt("gold_delta_gp"));
        assertTrue(visitors.optBoolean("facility_orders_uninterrupted"));
        JSONObject first=r(x(73).put("guest_d4",1));
        ok(first);assertTrue(first.optBoolean("letter_of_recommendation_after_stay"));
        JSONObject second=r(x(74).put("guest_d4",2).put("gift_d8",7));
        ok(second);assertEquals(700,second.optInt("gold_delta_gp"));
        JSONObject third=r(x(75).put("guest_d4",3));
        ok(third);assertEquals(1,third.optInt("defenders_delta"));
        JSONObject fourth=r(x(76).put("guest_d4",4));
        ok(fourth);assertTrue(fourth.optBoolean("friendly_monster_protects_next_bastion_attack"));
        blocked(r(x(74).put("guest_d4",2)),"guest_gift_d8_required");
    }

    @Test public void lostHirelingsAndMagicDiscoveryStrictSourceChoice() throws Exception {
        JSONObject loss=r(x(77).put("facility_id","temple"));
        ok(loss);assertTrue(loss.optBoolean("hirelings_replaced_after_next_turn"));
        blocked(r(x(79)),"affected_facility_id_required");
        JSONObject potion=r(x(80).put("magic_item_type","potion")
            .put("magic_item_id","Potion of Invisibility").put("chosen_item_is_uncommon",true));
        ok(potion);assertEquals("uncommon",potion.optString("rarity"));
        blocked(r(x(81).put("magic_item_type","weapon").put("magic_item_id","Sword")
            .put("chosen_item_is_uncommon",true)),"uncommon_potion_or_scroll_choice_required");
        blocked(r(x(82).put("magic_item_type","scroll").put("magic_item_id","Scroll"))
            ,"uncommon_rarity_must_be_confirmed");
    }

    @Test public void refugeesAndRequestForAidDeterministicOutcomes() throws Exception {
        JSONObject refugee=r(x(84).put("refugees_2d4",d(2,3)).put("payment_d6",5)
            .put("sufficient_basic_facility_space",false));
        ok(refugee);assertEquals(5,refugee.optInt("refugee_count"));
        assertEquals(500,refugee.optInt("gold_delta_gp"));
        assertTrue(refugee.optBoolean("refugees_camp_outside"));
        blocked(r(x(84).put("refugees_2d4",d(1,1)).put("payment_d6",1)),
            "basic_facility_space_choice_required");
        JSONObject success=r(x(92).put("send_defenders",true).put("defenders_before",4)
            .put("aid_defender_d6",d(4,3,3)).put("reward_d6",6));
        ok(success);assertEquals(600,success.optInt("gold_delta_gp"));
        assertEquals(0,success.optInt("defenders_delta"));
        JSONObject fail=r(x(98).put("send_defenders",true).put("defenders_before",2)
            .put("aid_defender_d6",d(1,2)).put("reward_d6",5));
        ok(fail);assertEquals(250,fail.optInt("gold_delta_gp"));
        assertEquals(-1,fail.optInt("defenders_delta"));
        blocked(r(x(92).put("send_defenders",true).put("defenders_before",1)
            .put("aid_defender_d6",d(1,2)).put("reward_d6",3)),
            "one_d6_per_dispatched_defender_required");
    }

    @Test public void treasureBandsRequireSeparateSourceTableRoll() throws Exception {
        int[] faces={1,40,41,63,64,73,74,75,76,90,91,98,99,100};
        String[] table={"art_objects_25_gp","art_objects_25_gp","art_objects_250_gp","art_objects_250_gp",
            "art_objects_750_gp","art_objects_750_gp","art_objects_2500_gp","art_objects_2500_gp",
            "common_magic_items","common_magic_items","uncommon_magic_items","uncommon_magic_items",
            "rare_magic_items","rare_magic_items"};
        for(int i=0;i<faces.length;i++){
            JSONObject j=r(x(99).put("treasure_d100",faces[i]));
            ok(j);assertEquals("treasure="+faces[i],table[i],j.optString("treasure_table"));
            assertTrue(j.optBoolean("treasure_table_roll_required"));
        }
        blocked(r(x(99)),"second_treasure_d100_required");
    }

    @Test public void missingDiceAndInvalidInputsFailClosedNoSilentRoll() throws Exception {
        blocked(r(new JSONObject()),"d100_roll_required");
        blocked(r(x(55)),"defenders_before_required");
        blocked(r(x(56)),"retain_hireling_choice_required");
        blocked(r(x(59)),"accept_opportunity_choice_required");
        blocked(r(x(64)),"payment_d6_required");
        blocked(r(x(73)),"guest_d4_required");
        blocked(r(x(80)),"uncommon_potion_or_scroll_choice_required");
        blocked(r(x(84)),"two_refugee_d4_faces_required");
        blocked(r(x(92)),"send_defenders_choice_required");
        blocked(r(x(100)),"second_treasure_d100_required");
    }

    @Test public void legacyPreviewUnchangedAndExplicitFullResolutionAvailable() throws Exception {
        JSONObject legacy=Bastion2024RulesEngine.execute("core2024.bastion.event.resolve",x(51));
        assertEquals("ok",legacy.optString("status"));
        assertTrue(legacy.optBoolean("resolved_band_only"));
        JSONObject full=Bastion2024RulesEngine.execute("core2024.bastion.event.resolve",
            x(51).put("resolve_full_event",true).put("defenders_before",2)
                .put("attack_6d6",d(2,2,2,2,2,2)));
        ok(full);assertEquals("Attack",full.optString("event"));
        assertEquals(0,full.optInt("defenders_delta"));
    }
}
