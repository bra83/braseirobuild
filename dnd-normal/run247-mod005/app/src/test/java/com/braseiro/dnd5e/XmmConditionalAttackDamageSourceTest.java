package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Source-verified alternatives must select one branch or block before damage. */
public final class XmmConditionalAttackDamageSourceTest {
    private static final Object[][] BLOODIED={
        {"Blood Hawk","Beak","1d4 + 2","1d8 + 2","piercing",false},
        {"Quaggoth","Claw","1d6 + 3","3d6 + 3","slashing",false},
        {"Swarm of Bats","Bites","2d4","1d4","piercing",false},
        {"Swarm of Crawling Claws","Swarm of Grasping Hands","4d8 + 2","2d8 + 2","necrotic",false},
        {"Swarm of Dretches","Rend","3d6 + 2","3d4 + 2","slashing",false},
        {"Swarm of Insects","Bites","2d4 + 1","1d4 + 1","poison",false},
        {"Swarm of Larvae","Bites","2d6 + 2","2d4 + 2","necrotic",false},
        {"Swarm of Lemures","Vile Slime","2d8 + 2","2d6 + 2","poison",false},
        {"Swarm of Piranhas","Bites","2d4 + 3","1d4 + 3","piercing",true},
        {"Swarm of Rats","Bites","2d4","1d4","piercing",false},
        {"Swarm of Ravens","Beaks","1d6 + 2","1d4","piercing",false},
        {"Swarm of Stirges","Swarm of Proboscises","2d10 + 3","1d10 + 3","piercing",true},
        {"Swarm of Venomous Snakes","Bites","1d8 + 4 + 3d6","1d4 + 4 + 3d6","piercing + poison",false}
    };

    @BeforeClass public static void install(){
        JSONObject result=Xmm2025CompiledBestiaryEngine.install();
        assertEquals(503,result.optInt("statblocks_installed"));
        assertEquals(0,result.optInt("blocked"));
    }

    @Test public void bloodiedBranchesMatchSourceAndChooseOnlyOneDamageExpression()throws Exception{
        for(Object[] row:BLOODIED){
            String id=(String)row[0],action=(String)row[1],normal=(String)row[2],reduced=(String)row[3];
            JSONObject profile=CompiledStatblockEngine.runtimeProfile(id);
            JSONObject raw=CompiledStatblockEngine.get(id);
            JSONObject declaration=findAction(profile,action);
            assertNotNull(id+" action",declaration);
            assertEquals(id+" source full damage",normal,declaration.optString("damage"));
            assertEquals(id+" source Bloodied damage",reduced,declaration.optString("damage_bloodied"));
            assertEquals(id+" source damage type",row[4],declaration.optString("damage_type"));
            assertEquals(id+" other manual source effect",(Boolean)row[5],declaration.optBoolean("manual_rider_resolution_required"));
            if((Boolean)row[5]){
                JSONObject blocked=StatblockCombatRuntimeEngine.attack(id,action,20,1,
                    Math.max(1,raw.optInt("hit_points")/2),raw.optInt("hit_points"));
                assertTrue(id+" must fail closed: "+blocked,blocked.optBoolean("blocked"));
                assertEquals("manual_source_action_resolution_required",blocked.optString("reason"));
            }else{
                int maximum=raw.optInt("hit_points"),bloodiedHp=maximum/2;
                JSONObject low=StatblockCombatRuntimeEngine.attack(id,action,20,1,bloodiedHp,maximum);
                assertTrue(id+" bloodied attack: "+low,low.optBoolean("resolved"));
                assertTrue(id+" reduced branch",low.optBoolean("bloodied_damage_selected"));
                assertEquals(id+" reduced expression",reduced,low.optString("damage_expression"));
                JSONObject high=StatblockCombatRuntimeEngine.attack(id,action,20,1,bloodiedHp+1,maximum);
                assertTrue(id+" healthy attack: "+high,high.optBoolean("resolved"));
                assertFalse(id+" full branch",high.optBoolean("bloodied_damage_selected"));
                assertEquals(id+" full expression",normal,high.optString("damage_expression"));
            }
        }
    }

    @Test public void unmodeledMoveAdvantageAndGrappleAlternativesFailClosed(){
        JSONObject perytonGore=findAction(CompiledStatblockEngine.runtimeProfile("Peryton"),"Gore");
        assertNotNull(perytonGore);
        assertEquals("1d8 + 3",perytonGore.optString("damage"));
        assertEquals("2d8",perytonGore.optString("extra_damage"));
        assertEquals("moved_30ft_straight_toward_target_immediately_before_hit",
            perytonGore.optString("extra_damage_condition"));
        assertFalse("source branch is additive, not an alternate base expression",
            perytonGore.has("damage_alternative_expression"));
        assertTrue("movement history is not an executable combat input yet",
            perytonGore.optBoolean("manual_rider_resolution_required"));
        JSONObject mimicBite=findAction(CompiledStatblockEngine.runtimeProfile("Mimic"),"Bite");
        assertNotNull(mimicBite);
        assertEquals("1d8 + 3 + 1d8",mimicBite.optString("damage"));
        assertEquals("piercing + acid",mimicBite.optString("damage_type"));
        assertFalse("grapple is a prerequisite, not a failed-save rider",mimicBite.has("condition_on_fail"));
        assertTrue("alternate damage and advantage still require target/source grappling context",
            mimicBite.optBoolean("manual_rider_resolution_required"));
        String[][] manual={
            {"Aarakocra Skirmisher","Talons"},{"Chimera","Bite"},
            {"Giant Seahorse","Ram"},{"Goat","Ram"},{"Peryton","Gore"},
            {"Mimic","Bite"}
        };
        for(String[] row:manual){
            JSONObject action=findAction(CompiledStatblockEngine.runtimeProfile(row[0]),row[1]);
            assertNotNull(row[0]+" action",action);
            assertTrue(row[0]+" must be explicitly manual",action.optBoolean("manual_rider_resolution_required"));
            JSONObject result=action.has("to_hit")
                ?StatblockCombatRuntimeEngine.attack(row[0],row[1],20,1,20,40)
                :StatblockCombatRuntimeEngine.savingThrowAction(row[0],row[1],40);
            assertTrue(row[0]+" must block: "+result,result.optBoolean("blocked"));
            assertEquals("manual_source_action_resolution_required",result.optString("reason"));
            assertFalse(result.has("damage_expression"));
        }
    }

    @Test public void unmodeledSourceRidersFailBeforeDamage(){
        String[][] manual={
            {"Giant Axe Beak","Sharpened Beak"}, {"Balor","Lightning Blade"},
            {"Fire Giant","Hammer Throw"}, {"Bearded Devil","Infernal Glaive"},
            {"Shadow","Draining Swipe"}, {"Goblin Boss","Scimitar"},
            {"Mummy","Rotting Fist"}, {"Adult Copper Dragon","Slowing Breath"}
        };
        for(String[] row:manual){
            JSONObject action=findAction(CompiledStatblockEngine.runtimeProfile(row[0]),row[1]);
            assertNotNull(row[0]+" action",action);
            assertTrue(row[0]+" must be explicitly manual",action.optBoolean("manual_rider_resolution_required"));
            assertTrue(row[0]+" must identify its untyped source effect",
                action.optBoolean("manual_unmodeled_source_effect_required"));
            JSONObject result=action.has("to_hit")
                ?StatblockCombatRuntimeEngine.attack(row[0],row[1],20,1,20,40)
                :StatblockCombatRuntimeEngine.savingThrowAction(row[0],row[1],40);
            assertTrue(row[0]+" must block: "+result,result.optBoolean("blocked"));
            assertEquals("manual_source_action_resolution_required",result.optString("reason"));
            assertFalse(result.has("damage_expression"));
        }
    }

    @Test public void unmodeledLegendarySaveEffectFailsClosed(){
        JSONObject action=findAction(CompiledStatblockEngine.runtimeProfile("Adult Black Dragon"),"Cloud of Insects");
        assertNotNull(action);
        assertTrue(action.optBoolean("manual_unmodeled_source_effect_required"));
        JSONObject result=StatblockCombatRuntimeEngine.savingThrowAction("Adult Black Dragon","Cloud of Insects",99);
        assertTrue(result.toString(),result.optBoolean("blocked"));
        assertEquals("manual_source_action_resolution_required",result.optString("reason"));
        assertFalse(result.has("damage_expression"));
    }

    @Test public void compoundOrRecurringSourceConditionsFailClosed(){
        String[][] cases={
            {"Adult Brass Dragon","Sleep Breath"},
            {"Adult Silver Dragon","Paralyzing Breath"},
            {"Pseudodragon","Sting"},
            {"Tarrasque","Thunderous Bellow"},
            {"Carrion Crawler","Paralyzing Tentacles"}
        };
        for(String[] row:cases){
            JSONObject action=findAction(CompiledStatblockEngine.runtimeProfile(row[0]),row[1]);
            assertNotNull(row[0]+" action",action);
            assertTrue(row[0]+"/"+row[1]+" needs dedicated manual handling",
                action.optBoolean("manual_unmodeled_source_effect_required"));
            JSONObject result=StatblockCombatRuntimeEngine.savingThrowAction(row[0],row[1],99);
            assertTrue(row[0]+"/"+row[1]+" must block: "+result,result.optBoolean("blocked"));
            assertEquals("manual_source_action_resolution_required",result.optString("reason"));
            assertFalse(result.has("damage_expression"));
        }
    }

    private static JSONObject findAction(JSONObject profile,String name){
        org.json.JSONArray actions=profile.optJSONArray("actions");
        if(actions!=null)for(int i=0;i<actions.length();i++){
            JSONObject action=actions.optJSONObject(i);
            if(action!=null&&name.equals(action.optString("name")))return action;
        }
        actions=profile.optJSONArray("special_actions");
        if(actions!=null)for(int i=0;i<actions.length();i++){
            JSONObject action=actions.optJSONObject(i);
            if(action!=null&&name.equals(action.optString("name")))return action;
        }
        return null;
    }
}
