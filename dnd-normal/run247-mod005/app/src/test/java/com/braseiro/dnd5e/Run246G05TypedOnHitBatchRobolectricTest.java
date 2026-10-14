package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** RUN246: source-bound no-save on-hit conditions with next-source-turn expiry.
 * Source matches are auxiliary 5e.tools notes; licensed PDF validation remains open.
 * These tests reject partial riders, guessed save DCs and unbounded conditions. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run246G05TypedOnHitBatchRobolectricTest {
    static final class Case {
        final String creature, action, condition;
        Case(String c,String a,String s){creature=c;action=a;condition=s;}
    }
    private static final Case[] CASES={
        new Case("Azalin Rex","Paralyzing Touch","paralyzed"),
        new Case("Assassin","Shortsword","poisoned"),
        new Case("Giant Centipede","Bite","poisoned"),
        new Case("Centaur Warden","Sun Ray","blinded"),
        new Case("Ettercap","Bite","poisoned"),
        new Case("Githyanki Dracomancer","Draconic Strike","frightened"),
        new Case("Gnoll Fang of Yeenoghu","Bite","poisoned"),
        new Case("Lich","Paralyzing Touch","paralyzed"),
        new Case("Noble Prodigy","Beguiling Strike","charmed"),
        new Case("Oni","Nightmare Ray","frightened"),
        new Case("Quasit","Rend","poisoned"),
        new Case("Satyr Revelmaster","Prance","charmed"),
        new Case("Sprite","Enchanting Bow","charmed"),
        new Case("Vampire Umbral Lord","Sickening Ray","poisoned"),
        new Case("Wyvern","Sting","poisoned"),
    };
    private static JSONObject findAction(JSONObject profile,String name)throws Exception {
        JSONArray actions=profile.optJSONArray("actions");
        assertNotNull("missing actions",actions);
        for(int i=0;i<actions.length();i++) {
            JSONObject row=actions.optJSONObject(i);
            if(row!=null&&name.equals(row.optString("name")))return row;
        }
        throw new AssertionError("missing compiled action "+name);
    }
    @Test public void allFifteenSourceBoundRidersAreExecutableWithoutInventedSavingThrows()throws Exception {
        assertEquals(0,RavenloftBestiaryRuntimeEngine.install().optInt("blocked"));
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed")>0);
        for(Case c:CASES){
            assertNotEquals("action label must be Portuguese: "+c.action,
                c.action,DndPtBrLocalizer.localizeText(c.action));
            JSONObject profile=CompiledStatblockEngine.runtimeProfile(c.creature);
            assertTrue("missing "+c.creature,profile.optBoolean("found",false));
            JSONObject action=findAction(profile,c.action);
            assertTrue(c.creature+"/"+c.action,action.has("to_hit"));
            assertFalse("no save allowed "+c.creature,action.has("save_dc"));
            assertFalse(action.has("save_ability"));
            assertFalse(action.has("condition_on_fail"));
            assertFalse(action.has("effect_on_fail"));
            assertEquals(c.condition,action.optString("condition_on_hit"));
            assertEquals(1,MonsterConditionTimerLedger.compiledDuration(action));
            assertEquals("source_turn_start",action.optString("condition_duration_tick"));
            assertEquals("",MonsterActionEconomyGuard.attackRiderBlock(action));
            assertEquals("unsupported_multiattack_on_hit_rider",
                MonsterActionEconomyGuard.attackRiderBlock(action,false));
            JSONObject hit=StatblockCombatRuntimeEngine.attack(c.creature,c.action,19,1);
            assertTrue(hit.toString(),hit.optBoolean("resolved",false));
            assertTrue(hit.toString(),hit.optBoolean("hit",false));
            assertEquals(c.condition,hit.optString("rider_on_hit_condition"));
            assertEquals(1,hit.optInt("rider_on_hit_duration_rounds"));
            assertEquals("source_turn_start",hit.optString("rider_on_hit_duration_tick"));
            assertFalse("save must not be invented",hit.optBoolean("rider_save_required",true));
            JSONObject miss=StatblockCombatRuntimeEngine.attack(c.creature,c.action,1,30);
            assertTrue(miss.optBoolean("resolved",false));
            assertFalse(miss.optBoolean("hit",true));
            assertEquals("",miss.optString("rider_on_hit_condition"));
            assertEquals(0,miss.optInt("rider_on_hit_duration_rounds"));
        }
    }
    @Test public void timerExpiresOnlyAtSourceTurnAndHasNoDuplicateTransition()throws Exception {
        JSONObject action=new JSONObject().put("to_hit",8).put("condition_on_hit","poisoned")
            .put("condition_duration_rounds",1).put("condition_duration_tick","source_turn_start");
        assertEquals("",MonsterActionEconomyGuard.attackRiderBlock(action));
        MonsterConditionRiderPlan.Result staged=MonsterConditionRiderPlan.plan("poisoned","",null);
        assertTrue(staged.valid());
        JSONArray timers=MonsterConditionTimerLedger.stage("[]",staged,1,71L,"Assassin",
            "Shortsword","ui-monster-run246-source","source_turn_start");
        assertEquals(1,timers.length());
        MonsterConditionTimerLedger.Transition notYet=MonsterConditionTimerLedger.advance(
            timers.toString(),72L,72L);
        assertFalse(notYet.changed);assertEquals(1,notYet.timers.length());
        MonsterConditionTimerLedger.Transition expired=MonsterConditionTimerLedger.advance(
            notYet.timers.toString(),72L,71L);
        assertTrue(expired.changed);assertEquals(0,expired.timers.length());
        assertEquals(1,expired.expired.size());
        assertEquals("envenenado",expired.expired.get(0));
        MonsterConditionTimerLedger.Transition replay=MonsterConditionTimerLedger.advance(
            expired.timers.toString(),72L,71L);
        assertFalse(replay.changed);assertTrue(replay.expired.isEmpty());
    }
    @Test public void noDurationOrProseCannotBecomePermanentCondition()throws Exception {
        JSONObject unbound=new JSONObject().put("to_hit",8).put("condition_on_hit","poisoned");
        assertEquals("unresolved_attack_hit_rider_requires_typed_handler",
            MonsterActionEconomyGuard.attackRiderBlock(unbound));
        JSONObject ambiguous=new JSONObject().put("to_hit",8).put("condition_on_hit","poisoned")
            .put("condition_duration_rounds",1).put("condition_duration_tick","at_will");
        assertEquals("unresolved_attack_hit_rider_requires_typed_handler",
            MonsterActionEconomyGuard.attackRiderBlock(ambiguous));
        JSONObject inventedSave=new JSONObject().put("to_hit",8).put("condition_on_hit","poisoned")
            .put("save_dc",15).put("condition_duration_rounds",1)
            .put("condition_duration_tick","source_turn_start");
        assertEquals("missing_or_invalid_compiled_save_ability",
            MonsterActionEconomyGuard.attackRiderBlock(inventedSave));
    }
    @Test public void fixedMultiConditionBundleNeedsAValidTimerForEveryToken()throws Exception {
        JSONObject bundle=new JSONObject().put("to_hit",14)
            .put("condition_on_hit","blinded, deafened")
            .put("condition_duration_rounds",1).put("condition_duration_tick","source_turn_start");
        assertEquals("",MonsterActionEconomyGuard.attackRiderBlock(bundle));
        assertEquals("unsupported_multiattack_on_hit_rider",
            MonsterActionEconomyGuard.attackRiderBlock(bundle,false));
        bundle.remove("condition_duration_tick");
        assertEquals("unresolved_attack_hit_rider_requires_typed_handler",
            MonsterActionEconomyGuard.attackRiderBlock(bundle));
        bundle.put("condition_duration_tick","source_turn_start")
            .put("condition_on_hit","blinded, unknown");
        assertEquals("unsupported_on_hit_condition",MonsterActionEconomyGuard.attackRiderBlock(bundle));
    }
}
