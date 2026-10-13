package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Individual PDF-derived behavioral oracles for 12 Astarion's Book of Hungers
 * chapter 1 features (Drive file 1rDyGkKe_btRHAR1P4xULh5enYEUM5Qgs).
 * No automatic mutations, resource spending or dice rolling is permitted.
 */
public final class AstarionPdf12FeatsRulesTest {
    private final AstarionPdfFeatRulesEngine engine=new AstarionPdfFeatRulesEngine();
    private static JSONObject input(Object... pairs) throws Exception {
        JSONObject o=new JSONObject();
        for(int i=0;i<pairs.length;i+=2)o.put((String)pairs[i],pairs[i+1]);
        return o;
    }
    private JSONObject valid(String id,JSONObject args) {
        JSONObject result=engine.evaluate(id,args);
        assertTrue(id+": "+result, result.optBoolean("resolved",false));
        assertEquals("ABH",result.optString("source_book"));
        assertEquals("ch_1",result.optString("source_section"));
        assertEquals(AstarionPdfFeatRulesEngine.SOURCE_DRIVE_ID,result.optString("source_drive_id"));
        assertFalse(id+" may not roll",result.optBoolean("auto_rolls",true));
        assertFalse(id+" may not mutate",result.optBoolean("auto_mutation_allowed",true));
        assertFalse(id+" may not persist",result.optBoolean("persisted",true));
        assertTrue(id+" must be preview only",result.optBoolean("requires_manual_application",false));
        return result;
    }
    private void invalid(String id,JSONObject args) {
        assertFalse(id+" should block invalid input",engine.evaluate(id,args).optBoolean("resolved",false));
    }

    @Test public void tirelessRevelerRequiresAllyDistanceSpendAndMissingInspiration() throws Exception {
        JSONObject x=input("proficiency_bonus",3,"uses_spent",2,"ally_visible",true,
            "ally_spent_heroic_inspiration",true,"self_has_heroic_inspiration",false,"distance_ft",60);
        JSONObject r=valid("tireless_reveler",x);
        assertTrue(r.optBoolean("grant_heroic_inspiration"));
        assertEquals(3,r.optInt("uses_spent_after"));
        invalid("tireless_reveler",input("proficiency_bonus",3,"uses_spent",3,
            "ally_visible",true,"ally_spent_heroic_inspiration",true,
            "self_has_heroic_inspiration",false,"distance_ft",60));
        x.put("distance_ft",61);
        invalid("tireless_reveler",x);
    }

    @Test public void vampireHunterVitalityWardRequiresExactPbExplicitDice() throws Exception {
        JSONObject x=input("reaction_available",true,"used_since_rest",false,
                "incoming_necrotic_damage",7,"proficiency_bonus",2,
                "d6_rolls",new JSONArray().put(3).put(6));
        JSONObject r=valid("vampire_hunter_vitality_ward",x);
        assertEquals(9,r.optInt("mitigation_roll_total"));
        assertEquals(7,r.optInt("damage_prevented"));
        assertEquals(0,r.optInt("damage_after"));
        assertTrue(r.optBoolean("reaction_consumed"));
        x.put("d6_rolls",new JSONArray().put(7).put(2));
        invalid("vampire_hunter_vitality_ward",x);
        x.put("d6_rolls",new JSONArray().put(3));
        invalid("vampire_hunter_vitality_ward",x);
    }

    @Test public void vampiresPlaythingTimelyRetreatConsumesBonusActionAndPbUse() throws Exception {
        JSONObject x=input("bonus_action_available",true,"choice","disengage",
                "proficiency_bonus",2,"uses_spent",1);
        JSONObject r=valid("vampires_plaything_timely_retreat",x);
        assertEquals("disengage",r.optString("granted_action"));
        assertEquals(2,r.optInt("uses_spent_after"));
        x.put("choice","attack");
        invalid("vampires_plaything_timely_retreat",x);
        x.put("choice","dash").put("uses_spent",2);
        invalid("vampires_plaything_timely_retreat",x);
    }

    @Test public void vampiresPlaythingDecantingOnlyAfterLongRestWithVial() throws Exception {
        JSONObject x=input("just_finished_long_rest",true,"empty_vial_or_flask",true,
                "choice","antitoxin");
        JSONObject r=valid("vampires_plaything_decanting",x);
        assertEquals("antitoxin",r.optString("item_to_create"));
        assertEquals("end_of_next_long_rest",r.optString("expiry"));
        x.put("empty_vial_or_flask",false);
        invalid("vampires_plaything_decanting",x);
    }

    @Test public void bomberFarLobberAllowsVisibleFortyFootVialDuringAttack() throws Exception {
        JSONObject x=input("taking_attack_action",true,"target_visible",true,
                "projectile","vial","distance_ft",40);
        assertEquals(40,valid("bomber_far_lobber",x).optInt("range_ft"));
        x.put("distance_ft",41);
        invalid("bomber_far_lobber",x);
        x.put("distance_ft",30).put("projectile","sword");
        invalid("bomber_far_lobber",x);
    }

    @Test public void cloyingMistsExtinguishesOnlyNonmagicalFlamesAndSlowsEnemies() throws Exception {
        JSONObject r=valid("cloying_mists_grasping_mist",input("fog_cloud_cast",true));
        assertTrue(r.optBoolean("extinguish_nonmagical_flames_in_sphere"));
        assertEquals(5,r.optInt("non_allied_creature_speed_penalty_ft"));
        assertFalse(r.optBoolean("affects_self_or_allies",true));
        invalid("cloying_mists_grasping_mist",new JSONObject());
    }

    @Test public void deliciousPainGrantsBpsResistanceByReactionUntilNextTurn() throws Exception {
        JSONObject x=input("reaction_available",true,"trigger_confirmed",true,
                "used_since_rest",false,"incoming_damage_type","piercing");
        JSONObject r=valid("delicious_pain_toughened_flesh",x);
        assertEquals(3,r.getJSONArray("resistance_types").length());
        assertEquals("until_start_of_next_turn",r.optString("duration"));
        x.put("incoming_damage_type","necrotic");
        invalid("delicious_pain_toughened_flesh",x);
    }

    @Test public void lightBringerSunHealingRequiresSunlightActualHitDieAndBonusAction() throws Exception {
        JSONObject x=input("bonus_action_available",true,"within_sunlight",true,
                "hit_die_available",true,"used_since_rest",false,
                "hit_die_sides",8,"rolled_hit_die",6);
        JSONObject r=valid("light_bringer_suns_healing",x);
        assertEquals(6,r.optInt("healing"));
        assertEquals(1,r.optInt("hit_die_consumed"));
        x.put("within_sunlight",false);
        invalid("light_bringer_suns_healing",x);
        x.put("within_sunlight",true).put("rolled_hit_die",9);
        invalid("light_bringer_suns_healing",x);
    }

    @Test public void loveBitesRequiresImmediateDamageFromMeleeOrUnarmed() throws Exception {
        JSONObject x=input("bonus_action_available",true,
                "damaged_target_immediately_prior",true,"used_since_rest",false,
                "attack_type","unarmed_strike");
        JSONObject r=valid("love_bites_endearing_pain",x);
        assertEquals("charmed",r.optString("condition"));
        assertEquals("start_of_next_turn_or_damage_from_you_or_allies",r.optString("ends_on"));
        x.put("attack_type","ranged_weapon");
        invalid("love_bites_endearing_pain",x);
    }

    @Test public void putrefyPoisonsSingleDamagedTargetUntilNextTurn() throws Exception {
        JSONObject x=input("necrotic_damage_roll",true,"target_took_damage",true,
                "used_since_rest",false);
        JSONObject r=valid("putrefy_necrosis",x);
        assertEquals("poisoned",r.optString("condition"));
        assertEquals(1,r.optInt("target_count"));
        x.put("used_since_rest",true);
        invalid("putrefy_necrosis",x);
    }

    @Test public void rebukePronesHugeOrSmallerOnlyOnRadiantDamage() throws Exception {
        JSONObject x=input("radiant_damage_roll",true,"target_took_damage",true,
                "target_size","huge","used_since_rest",false);
        assertEquals("prone",valid("rebuke_radiant_strike",x).optString("condition"));
        x.put("target_size","gargantuan");
        invalid("rebuke_radiant_strike",x);
        x.put("target_size","medium").put("target_took_damage",false);
        invalid("rebuke_radiant_strike",x);
    }

    @Test public void treacherousAllureGrantsAdvantageOnlyAgainstCharmedTarget() throws Exception {
        JSONObject x=input("target_charmed",true,"making_attack_roll",true);
        assertTrue(valid("treacherous_allure_inevitable_betrayal",x)
                .optBoolean("attack_roll_advantage"));
        x.put("target_charmed",false);
        invalid("treacherous_allure_inevitable_betrayal",x);
    }

    @Test public void gatewayPreservesFailClosedNoMutationContract() throws Exception {
        OfficialBookRuntimeGateway gateway=new OfficialBookRuntimeGateway(null);
        JSONObject ok=gateway.execute("abh","feat_feature",input("feature_id","bomber_far_lobber",
            "taking_attack_action",true,"target_visible",true,"projectile","flask",
            "distance_ft",30));
        assertTrue(ok.optBoolean("executed"));
        assertFalse(ok.optBoolean("persisted",true));
        assertEquals("manual_application_required",ok.optString("status"));
        JSONObject bad=gateway.execute("abh","feat_feature",
                input("feature_id","unknown_feature","trigger_confirmed",true));
        assertFalse(bad.optBoolean("executed"));
        assertEquals("blocked",bad.optString("status"));
    }
}
