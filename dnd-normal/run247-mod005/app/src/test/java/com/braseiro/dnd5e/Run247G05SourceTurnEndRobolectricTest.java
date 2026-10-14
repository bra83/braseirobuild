package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Source-turn-end expiry is distinct from the next source-turn start. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run247G05SourceTurnEndRobolectricTest {
    private static void bind(AppDb db,long id,String block)throws Exception {
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        resources.put("block_id",block);
        ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }

    private void assertTurnEndAttack(String monster,String actionName,String condition,
        String tick,int minimumDamage,String suffix)throws Exception {
        Xmm2025CompiledBestiaryEngine.install();
        JSONObject profile=CompiledStatblockEngine.runtimeProfile(monster);
        JSONObject action=null;JSONArray actions=profile.optJSONArray("actions");
        for(int i=0;actions!=null&&i<actions.length();i++)
            if(actionName.equals(actions.getJSONObject(i).optString("name")))action=actions.getJSONObject(i);
        assertNotNull(monster+" action missing",action);
        assertEquals(condition,action.optString("condition_on_hit"));
        assertFalse(action.has("condition_on_fail"));
        assertEquals(1,MonsterConditionTimerLedger.compiledDuration(action));
        assertEquals(tick,action.optString("condition_duration_tick"));
        assertEquals("",MonsterActionEconomyGuard.attackRiderBlock(action));

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_turn_end_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try {
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM "+monster+" "+suffix,20,100,100,11,"",false);
            target=db.addCombatant("Turn End Target",10,30,30,15,"",true);
            bind(db,actor,monster);
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,3,2);
            JSONObject rollMode=db.previewCompiledMonsterAttackRollMode(actor,target,monster,actionName);
            assertTrue(rollMode.toString(),rollMode.optBoolean("monster_roll_preview_valid"));
            int secondD20=rollMode.optBoolean("second_d20_required")?18:0;
            JSONObject attack=db.applyCompiledStatblockAttack(actor,target,monster,actionName,
                19,secondD20,minimumDamage,0,"ui-monster-source-turn-end-"+suffix);
            assertTrue(attack.toString(),attack.optBoolean("statblock_attack_committed"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),condition));
            JSONArray timers=new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]"));
            assertEquals(1,timers.length());
            if("source_turn_end".equals(tick)){
                assertTrue(timers.getJSONObject(0).optBoolean("skip_first_source_turn_end"));
                assertEquals(target,db.nextTurn().optLong("id"));
                assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),condition));
                timers=new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]"));
                assertFalse(timers.getJSONObject(0).optBoolean("skip_first_source_turn_end"));
                assertEquals(actor,db.nextTurn().optLong("id"));
                assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),condition));
                assertEquals(target,db.nextTurn().optLong("id"));
            }else{
                assertFalse(timers.getJSONObject(0).optBoolean("skip_first_target_turn_end"));
                assertEquals(target,db.nextTurn().optLong("id"));
                assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),condition));
                assertEquals(actor,db.nextTurn().optLong("id"));
            }
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),condition));
            assertEquals(0,new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]")).length());
        } finally {db.close();context.deleteDatabase(dbName);}
    }

    @Test public void scarecrowFearsomeClawExpiresAtEndOfItsNextTurn()throws Exception {
        assertTurnEndAttack("Scarecrow","Fearsome Claw","frightened","source_turn_end",3,"scarecrow");
    }

    @Test public void merrowBiteExpiresPoisonAtEndOfItsNextTurn()throws Exception {
        assertTurnEndAttack("Merrow","Bite","poisoned","source_turn_end",5,"merrow");
    }

    @Test public void giantVultureGougeExpiresPoisonAtEndOfTargetsNextTurn()throws Exception {
        assertEquals("Golpe Dilacerante",DndPtBrLocalizer.localizeText("Gouge"));
        assertTurnEndAttack("Giant Vulture","Gouge","poisoned","target_turn_end",6,"giant-vulture");
    }

    @Test public void gasSporeTendrilExpiresPoisonAtEndOfTargetsNextTurn()throws Exception {
        assertEquals("Tentáculo",DndPtBrLocalizer.localizeText("Tendril"));
        assertTurnEndAttack("Gas Spore Fungus","Tendril","poisoned","target_turn_end",4,"gas-spore");
    }

    @Test public void cloudGiantThundercloudExpiresIncapacitatedAtEndOfTargetsNextTurn()throws Exception {
        assertEquals("Nuvem Trovejante",DndPtBrLocalizer.localizeText("Thundercloud"));
        assertTurnEndAttack("Cloud Giant","Thundercloud","incapacitated","target_turn_end",14,"cloud-giant");
    }

    @Test public void hillGiantTrashLobExpiresPoisonAtEndOfTargetsNextTurn()throws Exception {
        assertEquals("Arremesso de Detritos",DndPtBrLocalizer.localizeText("Trash Lob"));
        assertTurnEndAttack("Hill Giant","Trash Lob","poisoned","target_turn_end",15,"hill-giant");
    }

    @Test public void ledgerDoesNotExpireAtSourceStartAndHonorsCurrentTurnSkip()throws Exception {
        MonsterConditionRiderPlan.Result plan=MonsterConditionRiderPlan.plan("frightened","",null);
        JSONArray timers=MonsterConditionTimerLedger.stage("[]",plan,1,91,"Scarecrow","Fearsome Claw",
            "turn-end-ledger-test","source_turn_end",true);
        assertFalse(MonsterConditionTimerLedger.advance(timers.toString(),92,91).changed);
        MonsterConditionTimerLedger.Transition skipped=MonsterConditionTimerLedger.advanceSourceTurnEnd(
            timers.toString(),91);
        assertTrue(skipped.changed);assertTrue(skipped.expired.isEmpty());assertEquals(1,skipped.timers.length());
        MonsterConditionTimerLedger.Transition expired=MonsterConditionTimerLedger.advanceSourceTurnEnd(
            skipped.timers.toString(),91);
        assertEquals(0,expired.timers.length());assertEquals(1,expired.expired.size());
        assertEquals("amedrontado",expired.expired.get(0));

        JSONArray targetEnd=MonsterConditionTimerLedger.stage("[]",plan,1,91,"Giant Vulture","Gouge",
            "target-turn-end-ledger-test","target_turn_end",true);
        MonsterConditionTimerLedger.Transition targetSkipped=
            MonsterConditionTimerLedger.advanceSourceTurnEnd(targetEnd.toString(),92,92);
        assertTrue(targetSkipped.changed);assertTrue(targetSkipped.expired.isEmpty());
        MonsterConditionTimerLedger.Transition targetExpired=
            MonsterConditionTimerLedger.advanceSourceTurnEnd(targetSkipped.timers.toString(),92,92);
        assertEquals(0,targetExpired.timers.length());assertEquals(1,targetExpired.expired.size());
        assertEquals("amedrontado",targetExpired.expired.get(0));
    }
}
