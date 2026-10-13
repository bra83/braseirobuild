package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Source: user's Astarion's Book of Hungers PDF, Chapter 1, Bloodlust.
 * Tests exact rule behaviors without copying copyrighted prose:
 * Powerful Recovery floor of 3; Sanguine Feast only once per turn against a
 * Bloodied non-Construct non-Undead hit by an attack roll, expends Hit Die,
 * heals roll + CON, limits uses to proficiency bonus per Long Rest.
 */
public final class AstarionBloodlustPdfRuleTest {
    @Test public void powerfulRecoveryAdjustsNaturalOneAndTwoToThree() {
        AstarionHungersRulesEngine e=new AstarionHungersRulesEngine();
        for(int roll=1;roll<=10;roll++) {
            JSONObject r=e.evaluateBloodlustRecovery(roll,10);
            assertTrue(r.optBoolean("resolved"));
            assertEquals(Math.max(3,roll),r.optInt("adjusted_hit_die"));
            assertFalse(r.optBoolean("auto_rolls",true));
        }
        assertFalse(e.evaluateBloodlustRecovery(0,10).optBoolean("resolved"));
        assertFalse(e.evaluateBloodlustRecovery(13,12).optBoolean("resolved"));
        assertFalse(e.evaluateBloodlustRecovery(1,13).optBoolean("resolved"));
    }

    @Test public void sanguineFeastRequiresActualAttackHitBloodiedEligibleTargetAndResources() {
        AstarionHungersRulesEngine e=new AstarionHungersRulesEngine();
        JSONObject hit=e.evaluateBloodlustSanguineFeast(
                true,true,"humanoid",false,true,7,8,3,3,1);
        assertTrue(hit.optBoolean("resolved"));
        assertEquals(10,hit.optInt("healing"));
        assertEquals(1,hit.optInt("hit_die_consumed"));
        assertEquals(2,hit.optInt("uses_spent_after"));
        assertEquals(3,hit.optInt("uses_max"));
        assertTrue(hit.optBoolean("requires_manual_application",false));
        assertFalse(hit.optBoolean("auto_rolls",true));
        assertFalse(hit.optBoolean("auto_mutation_allowed",true));

        assertFalse(e.evaluateBloodlustSanguineFeast(
                false,true,"humanoid",false,true,7,8,3,3,1).optBoolean("resolved"));
        assertFalse(e.evaluateBloodlustSanguineFeast(
                true,false,"humanoid",false,true,7,8,3,3,1).optBoolean("resolved"));
        assertFalse(e.evaluateBloodlustSanguineFeast(
                true,true,"undead",false,true,7,8,3,3,1).optBoolean("resolved"));
        assertFalse(e.evaluateBloodlustSanguineFeast(
                true,true,"construct",false,true,7,8,3,3,1).optBoolean("resolved"));
        assertFalse(e.evaluateBloodlustSanguineFeast(
                true,true,"humanoid",true,true,7,8,3,3,1).optBoolean("resolved"));
        assertFalse(e.evaluateBloodlustSanguineFeast(
                true,true,"humanoid",false,false,7,8,3,3,1).optBoolean("resolved"));
        assertFalse(e.evaluateBloodlustSanguineFeast(
                true,true,"humanoid",false,true,7,8,3,3,3).optBoolean("resolved"));
        assertFalse(e.evaluateBloodlustSanguineFeast(
                true,true,"",false,true,7,8,3,3,1).optBoolean("resolved"));
        assertFalse(e.evaluateBloodlustSanguineFeast(
                true,true,"humanoid",false,true,0,8,3,3,1).optBoolean("resolved"));
    }

    @Test public void gatewayResolvesBloodlustWithoutHiddenStateMutation() throws Exception {
        OfficialBookRuntimeGateway gateway=new OfficialBookRuntimeGateway(null);
        JSONObject recovery=gateway.execute("abh","bloodlust_powerful_recovery",
                new JSONObject().put("rolled_hit_die",2).put("hit_die_sides",8));
        assertTrue(recovery.optBoolean("executed"));
        assertEquals(3,recovery.getJSONObject("result").optInt("adjusted_hit_die"));
        assertFalse(recovery.optBoolean("persisted",true));

        JSONObject request=new JSONObject()
                .put("hit_with_attack_roll",true)
                .put("target_bloodied",true)
                .put("target_creature_type","fiend")
                .put("already_used_this_turn",false)
                .put("hit_die_available",true)
                .put("rolled_hit_die",6)
                .put("hit_die_sides",6)
                .put("constitution_modifier",1)
                .put("proficiency_bonus",2)
                .put("uses_spent",0);
        JSONObject feast=gateway.execute("abh","bloodlust_sanguine_feast",request);
        assertTrue(feast.optBoolean("executed"));
        assertEquals("manual_application_required",feast.optString("status"));
        assertEquals(7,feast.getJSONObject("result").optInt("healing"));
        assertFalse(feast.optBoolean("persisted",true));
    }
}
