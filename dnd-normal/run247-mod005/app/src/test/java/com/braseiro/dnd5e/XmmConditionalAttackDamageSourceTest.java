package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/** Source-verified alternatives must select one branch or block before damage. */
public final class XmmConditionalAttackDamageSourceTest {
    private static final Object[][] CHARGE_EXTRA={
        {"Boar","Gore","1d6 + 1","piercing","1d6","piercing","medium",20},
        {"Elk","Ram","1d6 + 3","bludgeoning","1d6","bludgeoning","large",20},
        {"Galeb Duhr","Avalanche Slam","2d6 + 5","bludgeoning","2d6","bludgeoning","large",20},
        {"Giant Boar","Gore","2d6 + 3","piercing","2d6","piercing","large",20},
        {"Giant Elk","Ram","2d6 + 4 + 2d4","bludgeoning + radiant","2d4","bludgeoning","huge",20},
        {"Giant Goat","Ram","1d6 + 3","bludgeoning","2d4","bludgeoning","large",20},
        {"Minotaur of Baphomet","Gore","4d6 + 4","piercing","3d6","piercing","large",10},
        {"Minotaur Skeleton","Gore","2d6 + 4","piercing","2d8","piercing","large",20},
        {"Primeval Owlbear","Ravage","2d8 + 6","slashing","2d8","slashing","huge",20},
        {"Rhinoceros","Gore","2d8 + 5","piercing","2d8","piercing","large",20},
        {"Triceratops","Gore","2d12 + 6","piercing","2d8","piercing","huge",20},
        {"Warhorse","Hooves","2d4 + 4","bludgeoning","2d4","bludgeoning","large",20},
        {"Wereboar","Tusk (Boar or Hybrid Form Only)","2d6 + 3","piercing","2d6","piercing","medium",20}
    };
    private static final Object[][] BLOODIED={
        {"Blood Hawk","Beak","1d4 + 2","1d8 + 2","piercing",false},
        {"Quaggoth","Claw","1d6 + 3","3d6 + 3","slashing",false},
        {"Swarm of Bats","Bites","2d4","1d4","piercing",false},
        {"Swarm of Crawling Claws","Swarm of Grasping Hands","4d8 + 2","2d8 + 2","necrotic",false},
        {"Swarm of Dretches","Rend","3d6 + 2","3d4 + 2","slashing",false},
        {"Swarm of Insects","Bites","2d4 + 1","1d4 + 1","poison",false},
        {"Swarm of Larvae","Bites","2d6 + 2","2d4 + 2","necrotic",false},
        {"Swarm of Lemures","Vile Slime","2d8 + 2","2d6 + 2","poison",false},
        {"Swarm of Piranhas","Bites","2d4 + 3","1d4 + 3","piercing",false},
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
        String[][] noSave={
            {"Allosaurus","Claws"},
            {"Bugbear Stalker","Morningstar"},
            {"Bugbear Warrior","Light Hammer"},{"Cloaker","Attach"},{"Darkmantle","Crush"},
            {"Death Slaad","Chaos Blade"},{"Elephant","Gore"},
            {"Empyrean","Sacred Weapon"},{"Ghast Gravecaller","Claw"},{"Giant Frog","Swallow"},
            {"Giant Toad","Swallow"},{"Githzerai Psion","Psychic Warp"},{"Gorgon","Gore"},
            {"Gray Slaad","Chaos Claw"},{"Green Slaad","Chaos Staff"},
            {"Kraken","Tentacle"},{"Mammoth","Gore"},{"Phase Spider","Bite"},{"Pixie","Faerie Dust"},
            {"Pixie Wonderbringer","Faerie Dust"},{"Roper","Reel"},{"Roper","Tentacle"},
            {"Tarrasque","Bite"},{"Vampire Familiar","Umbral Dagger"},{"Warhorse Skeleton","Hooves"}
        };
        for(String[] row:noSave){
            JSONObject action=findAction(CompiledStatblockEngine.runtimeProfile(row[0]),row[1]);
            assertNotNull(row[0]+" action",action);
            assertFalse(row[0]+" has no source save to fail",action.has("condition_on_fail"));
            assertTrue(row[0]+" remains blocked until its effect is executable",
                action.optBoolean("manual_rider_resolution_required"));
        }
        JSONObject cloaker=findAction(CompiledStatblockEngine.runtimeProfile("Cloaker"),"Attach");
        assertEquals(14,cloaker.optInt("detach_dc"));
        assertFalse(cloaker.has("save_dc"));
        JSONObject kraken=findAction(CompiledStatblockEngine.runtimeProfile("Kraken"),"Tentacle");
        assertEquals(20,kraken.optInt("escape_dc"));
        assertFalse(kraken.has("save_dc"));
        for(Object[] row:CHARGE_EXTRA){
            String id=(String)row[0],actionName=(String)row[1];
            JSONObject action=findAction(CompiledStatblockEngine.runtimeProfile(id),actionName);
            assertNotNull(id+" action",action);
            assertEquals(id+" normal damage",row[2],action.optString("damage"));
            assertEquals(id+" normal damage types",row[3],action.optString("damage_type"));
            assertEquals(id+" conditional extra",row[4],action.optString("extra_damage"));
            assertEquals(id+" extra damage type",row[5],action.optString("extra_damage_type",(String)row[5]));
            assertEquals(id+" target size gate",row[6],action.optString("extra_damage_max_target_size"));
            assertEquals(id+" movement gate","moved_"+row[7]+"ft_straight_toward_target_immediately_before_hit",
                action.optString("extra_damage_condition"));
            assertEquals(id+" conditional prone","prone",action.optString("extra_damage_condition_on_hit"));
            assertFalse(id+" prone cannot mean failed save/attack",action.has("condition_on_fail"));
            assertTrue(id+" must remain manual",action.optBoolean("manual_rider_resolution_required"));
            assertTrue(id+" may execute as an ordinary attack",
                MonsterActionEconomyGuard.attackRiderBlock(action,id).isEmpty());
            assertEquals(id+" remains unavailable as an unverified multiattack component",
                "unsupported_multiattack_charge_rider",MonsterActionEconomyGuard.attackRiderBlock(action,false));
            JSONObject ordinary=StatblockCombatRuntimeEngine.attack(id,actionName,20,1,20,40);
            assertTrue(id+" ordinary attack: "+ordinary,ordinary.optBoolean("resolved"));
            assertFalse(id+" cannot get conditional damage without proof",ordinary.optBoolean("charge_qualified"));
            assertEquals(id+" conditional damage absent without proof","",
                ordinary.optString("extra_damage_expression"));
            JSONObject charged=StatblockCombatRuntimeEngine.attack(id,actionName,20,1,20,40,true);
            assertTrue(id+" qualified charge branch: "+charged,charged.optBoolean("resolved"));
            assertTrue(id+" charge marker",charged.optBoolean("charge_qualified"));
            assertEquals(id+" extra damage only on qualified charge",row[4],charged.optString("extra_damage_expression"));
            assertEquals(id+" prone only on qualified charge","prone",
                charged.optString("rider_on_hit_condition"));
        }
        JSONObject perytonGore=findAction(CompiledStatblockEngine.runtimeProfile("Peryton"),"Gore");
        assertNotNull(perytonGore);
        assertEquals("1d8 + 3",perytonGore.optString("damage"));
        assertEquals("2d8",perytonGore.optString("extra_damage"));
        assertEquals("moved_30ft_straight_toward_target_immediately_before_hit",
            perytonGore.optString("extra_damage_condition"));
        assertFalse("source branch is additive, not an alternate base expression",
            perytonGore.has("damage_alternative_expression"));
        assertTrue("movement rider still uses source route validation",
            perytonGore.optBoolean("manual_rider_resolution_required"));
        JSONObject ordinaryPeryton=StatblockCombatRuntimeEngine.attack("Peryton","Gore",20,1,20,40);
        assertTrue(ordinaryPeryton.optBoolean("resolved"));
        assertEquals("Peryton extra damage is suppressed without proof","",
            ordinaryPeryton.optString("extra_damage_expression"));
        JSONObject chargedPeryton=StatblockCombatRuntimeEngine.attack("Peryton","Gore",20,1,20,40,true);
        assertEquals("2d8",chargedPeryton.optString("extra_damage_expression"));
        JSONObject mimicBite=findAction(CompiledStatblockEngine.runtimeProfile("Mimic"),"Bite");
        assertNotNull(mimicBite);
        assertEquals("1d8 + 3 + 1d8",mimicBite.optString("damage"));
        assertEquals("piercing + acid",mimicBite.optString("damage_type"));
        assertFalse("grapple is a prerequisite, not a failed-save rider",mimicBite.has("condition_on_fail"));
        assertTrue("alternate damage and advantage still require target/source grappling context",
            mimicBite.optBoolean("manual_rider_resolution_required"));
        String[][] manual={
            {"Chimera","Bite"},{"Mimic","Bite"}
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
        for(String[] row:new String[][]{{"Aarakocra Skirmisher","Talons"},
            {"Giant Seahorse","Ram"},{"Goat","Ram"}}){
            JSONObject ordinary=StatblockCombatRuntimeEngine.attack(row[0],row[1],20,1,20,40);
            assertTrue(row[0]+" base branch: "+ordinary,ordinary.optBoolean("resolved"));
            JSONObject charged=StatblockCombatRuntimeEngine.attack(row[0],row[1],20,1,20,40,true);
            assertTrue(row[0]+" movement branch: "+charged,charged.optBoolean("resolved"));
            assertTrue(charged.optBoolean("charge_qualified"));
            String sourceAlternative=findAction(CompiledStatblockEngine.runtimeProfile(row[0]),row[1])
                .optString("damage_alternative_expression");
            assertEquals(sourceAlternative,charged.optString("damage_expression"));
        }
    }

    @Test public void everyPinnedChargeActionHasTheTypedMovementGate(){
        org.json.JSONArray names=Xmm2025CompiledBestiaryEngine.catalog().optJSONArray("names");
        int supported=0;
        for(int i=0;names!=null&&i<names.length();i++){
            String id=names.optString(i);
            org.json.JSONArray actions=CompiledStatblockEngine.runtimeProfile(id).optJSONArray("actions");
            for(int j=0;actions!=null&&j<actions.length();j++){
                JSONObject action=actions.optJSONObject(j);if(action==null)continue;
                String movement=action.optString("extra_damage_condition",
                    action.optString("conditional_condition",action.optString("damage_alternative_condition","")));
                if(!movement.startsWith("moved_")||!movement.endsWith("_straight_toward_target_immediately_before_hit"))continue;
                assertTrue(id+" / "+action.optString("name"),XmmChargeMovementRules.supported(action));
                int feet=XmmChargeMovementRules.requiredFeet(action);
                assertTrue(id+" / "+action.optString("name")+" threshold",feet==10||feet==20||feet==30);
                assertEquals(id+" / "+action.optString("name")+" unavailable to multiattack",
                    "unsupported_multiattack_charge_rider",MonsterActionEconomyGuard.attackRiderBlock(action,false));
                supported++;
            }
        }
        assertEquals("all source-pinned XMM charge actions",22,supported);
    }

    @Test public void unmodeledSourceRidersFailBeforeDamage(){
        String[][] manual={
            {"Giant Axe Beak","Sharpened Beak"}, {"Balor","Lightning Blade"},
            {"Fire Giant","Hammer Throw"}, {"Bearded Devil","Infernal Glaive"},
            {"Shadow","Draining Swipe"}, {"Doppelganger","Slam"},
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

    @Test public void ongoingAndAreaDamageAreNeverFoldedIntoTheImmediateHit(){
        String[][] cases={
            {"Animated Rug of Smothering","Smother","2d6 + 3","bludgeoning"},
            {"Dao","Earth Burst","2d8 + 6","bludgeoning"},
            {"Half-Dragon","Claw","1d4 + 4","slashing"},
            {"Pit Fiend","Bite","3d6 + 8","piercing"},
            {"Stirge","Proboscis","1d6 + 3","piercing"},
            {"Vine Blight","Constricting Vine","1d8 + 2","bludgeoning"}
        };
        for(String[] row:cases){
            JSONObject action=findAction(CompiledStatblockEngine.runtimeProfile(row[0]),row[1]);
            assertNotNull(row[0]+" source action",action);
            assertEquals(row[0]+" immediate hit damage",row[2],action.optString("damage"));
            assertEquals(row[0]+" immediate hit type",row[3],action.optString("damage_type"));
            assertTrue(row[0]+" secondary effect stays explicitly manual",
                action.optBoolean("manual_rider_resolution_required"));
            JSONObject blocked=StatblockCombatRuntimeEngine.attack(row[0],row[1],20,1,1,1);
            assertTrue(row[0]+" must not apply an incomplete effect: "+blocked,
                blocked.optBoolean("blocked"));
            assertEquals("manual_source_action_resolution_required",blocked.optString("reason"));
            assertFalse(blocked.has("damage_expression"));
        }
        JSONObject rug=findAction(CompiledStatblockEngine.runtimeProfile("Animated Rug of Smothering"),"Smother");
        assertFalse("the grapple is a choice instead of damage, not an automatic hit condition",
            rug.has("condition_on_hit"));
        assertTrue(rug.optBoolean("manual_effect_choice_required"));
        JSONObject dao=findAction(CompiledStatblockEngine.runtimeProfile("Dao"),"Earth Burst");
        assertEquals("3d6",dao.optString("secondary_save_damage"));
        assertEquals("thunder",dao.optString("secondary_save_damage_type"));
        assertEquals(10,dao.optInt("secondary_save_area_ft"));
        JSONObject halfDragon=findAction(CompiledStatblockEngine.runtimeProfile("Half-Dragon"),"Claw");
        assertEquals("2d6",halfDragon.optString("extra_damage"));
        assertEquals("draconic_origin",halfDragon.optString("extra_damage_type"));
        JSONObject pitFiend=findAction(CompiledStatblockEngine.runtimeProfile("Pit Fiend"),"Bite");
        assertEquals("6d6",pitFiend.optString("condition_recurring_damage"));
        assertEquals("target_turn_start",pitFiend.optString("condition_recurring_damage_tick"));
        JSONObject stirge=findAction(CompiledStatblockEngine.runtimeProfile("Stirge"),"Proboscis");
        assertEquals("source_turn_start",stirge.optString("condition_recurring_damage_tick"));
        JSONObject vine=findAction(CompiledStatblockEngine.runtimeProfile("Vine Blight"),"Constricting Vine");
        assertEquals("target_turn_start",vine.optString("condition_recurring_damage_tick"));
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
