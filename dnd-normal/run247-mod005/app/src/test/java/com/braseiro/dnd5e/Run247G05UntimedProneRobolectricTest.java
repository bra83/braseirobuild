package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Regression on strictly source-declared Prone, with no invented save/timer. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run247G05UntimedProneRobolectricTest {
    private static final String[][] CASES = {
        {"Ankylosaurus","Tail","huge"},
        {"Barlgura","Thrash","large"},
        {"Brown Bear","Claw","large"},
        {"Colossus","Radiant Ray","large"},
        {"Cyclops Sentry","Stone Club","huge"},
        {"Dao","Earthen Maul","large"},
        {"Dire Wolf","Bite","large"},
        {"Displacer Beast","Rend","large"},
        {"Djinni","Storm Bolt","large"},
        {"Dragon Turtle","Tail","huge"},
        {"Earth Elemental","Rock Launch","large"},
        {"Ettin","Battleaxe","large"},
        {"Giant Axe Beak","Talons","large"},
        {"Giant Crocodile","Tail","large"},
        {"Grick Ancient","Slam","large"},
        {"Hill Giant","Tree Club","large"},
        {"Lizardfolk Sovereign","Earthen Maul","medium"},
        {"Mastiff","Bite","medium"},
        {"Merfolk Wavebender","Aquatic Burst","large"},
        {"Stone Giant","Boulder","large"},
        {"Tarrasque","Tail","huge"},
        {"Tiger","Rend","large"},
        {"Tyrannosaurus Rex","Tail","huge"},
        {"Water Elemental","Slam","medium"},
        {"Winter Wolf","Bite","large"},
        {"Wolf","Bite","medium"}
    };
    private JSONObject action(String creature,String name) throws Exception {
        JSONObject p=CompiledStatblockEngine.runtimeProfile(creature);
        assertTrue(creature+": profile absent",p.optBoolean("found",false));
        JSONArray actions=p.optJSONArray("actions");
        assertNotNull(actions);
        for(int i=0;i<actions.length();i++){
            JSONObject row=actions.getJSONObject(i);
            if(name.equals(row.optString("name")))return row;
        }
        throw new AssertionError(creature+":"+name+" absent");
    }
    @Test public void allTwentySixActionsHaveExplicitSizeAndStandUpEndCondition() throws Exception {
        Xmm2025CompiledBestiaryEngine.install();
        for(String[] c:CASES){
            JSONObject a=action(c[0],c[1]);
            assertTrue(c[0]+":"+c[1],MonsterOnHitProneRules.validSource(a));
            assertEquals(c[2],a.optString("condition_max_target_size"));
            assertEquals("",MonsterActionEconomyGuard.attackRiderBlock(a));
            assertEquals("unsupported_multiattack_on_hit_rider",MonsterActionEconomyGuard.attackRiderBlock(a,false));
            assertEquals(0,MonsterConditionTimerLedger.compiledDuration(a));
            assertEquals("",MonsterOnHitProneRules.targetBlock(a,c[2]));
            assertEquals("",MonsterOnHitProneRules.targetBlock(a,"gargantuan"));
            assertFalse(MonsterOnHitProneRules.eligible(a,"gargantuan"));
            assertEquals("target_size_not_verified",MonsterOnHitProneRules.targetBlock(a,""));
            JSONObject hit=StatblockCombatRuntimeEngine.attack(c[0],c[1],20,10);
            assertTrue(hit.optBoolean("hit",false));
            assertEquals("prone",hit.optString("rider_on_hit_condition"));
            JSONObject miss=StatblockCombatRuntimeEngine.attack(c[0],c[1],1,100);
            assertFalse(miss.optBoolean("hit",true));
            assertEquals("",miss.optString("rider_on_hit_condition"));
        }
    }
    @Test public void noInventedDurationSaveOrMissingSizeIsAllowed() throws Exception {
        JSONObject ok=new JSONObject().put("to_hit",5).put("condition_on_hit","prone")
            .put("condition_ends_when","stands_up").put("condition_max_target_size","medium");
        assertEquals("",MonsterActionEconomyGuard.attackRiderBlock(ok));
        assertEquals("",MonsterOnHitProneRules.targetBlock(ok,"pequeno"));
        assertEquals("",MonsterOnHitProneRules.targetBlock(ok,"médio"));
        assertEquals("",MonsterOnHitProneRules.targetBlock(ok,"large"));
        assertFalse(MonsterOnHitProneRules.eligible(ok,"large"));
        assertTrue(MonsterOnHitProneRules.eligible(ok,"medium"));
        JSONObject inventedSave=new JSONObject(ok.toString()).put("save_dc",13);
        assertEquals("missing_or_invalid_compiled_save_ability",MonsterActionEconomyGuard.attackRiderBlock(inventedSave));
        JSONObject timer=new JSONObject(ok.toString()).put("condition_duration_rounds",1).put("condition_duration_tick","source_turn_start");
        assertEquals("unsupported_untimed_on_hit_rider",MonsterActionEconomyGuard.attackRiderBlock(timer));
        JSONObject other=new JSONObject(ok.toString()).put("condition_on_hit","paralyzed");
        assertEquals("unsupported_untimed_on_hit_rider",MonsterActionEconomyGuard.attackRiderBlock(other));
        JSONObject missingSize=new JSONObject(ok.toString());missingSize.remove("condition_max_target_size");
        assertEquals("unsupported_untimed_on_hit_rider",MonsterActionEconomyGuard.attackRiderBlock(missingSize));
    }
}
