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
    private static JSONArray sampleDamage(JSONObject action)throws Exception {
        JSONArray specs=StatblockCombatFlowEngine.damageSpecs(
            action.optString("damage_expression",action.optString("damage","")),
            action.optString("damage_type",""),action.optString("extra_damage_expression",""));
        JSONArray rolls=new JSONArray();
        for(int i=0;i<specs.length();i++){
            JSONObject spec=specs.getJSONObject(i);
            CompiledMonsterDamageGuard.Range range=CompiledMonsterDamageGuard.range(
                spec.optString("expression"),"",false);
            assertNotNull("source damage expression invalid: "+spec,range);
            rolls.put(new JSONObject().put("expression",spec.optString("expression"))
                .put("damage_type",spec.optString("type"))
                .put("damage_total",range.min+(range.max-range.min)/2));
        }
        return rolls;
    }

    private void assertTimedConditionAttack(String monster,String actionName,String condition,
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
        String dbName="xmm_timed_condition_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try {
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM "+monster+" "+suffix,20,100,100,11,"",false);
            target=db.addCombatant("Turn End Target",10,1000,1000,15,"",true);
            bind(db,actor,monster);
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,3,2);
            JSONObject rollMode=db.previewCompiledMonsterAttackRollMode(actor,target,monster,actionName);
            assertTrue(rollMode.toString(),rollMode.optBoolean("monster_roll_preview_valid"));
            int secondD20=rollMode.optBoolean("second_d20_required")?18:0;
            JSONObject attack=db.applyCompiledStatblockAttack(actor,target,monster,actionName,
                19,secondD20,sampleDamage(action),0,0L,-1,-1,
                "ui-monster-source-turn-end-"+suffix);
            assertTrue(monster+" / "+actionName+": "+attack,
                attack.optBoolean("statblock_attack_committed"));
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
                long nextActor=0;int turnLimit=db.listCombatants().size()+1;
                for(int turn=0;turn<turnLimit;turn++){
                    nextActor=db.nextTurn().optLong("id");
                    if(nextActor==actor)break;
                    assertTrue(monster+" condition expired before source turn",
                        ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),condition));
                }
                assertEquals(monster+" target turn should precede source turn; state="+
                    db.getCombatState()+"; combatants="+db.listCombatants(),actor,nextActor);
            }
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),condition));
            assertEquals(0,new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]")).length());
        } finally {db.close();context.deleteDatabase(dbName);}
    }

    @Test public void scarecrowFearsomeClawExpiresAtEndOfItsNextTurn()throws Exception {
        assertTimedConditionAttack("Scarecrow","Fearsome Claw","frightened","source_turn_end",3,"scarecrow");
    }

    @Test public void merrowBiteExpiresPoisonAtEndOfItsNextTurn()throws Exception {
        assertTimedConditionAttack("Merrow","Bite","poisoned","source_turn_end",5,"merrow");
    }

    @Test public void giantVultureGougeExpiresPoisonAtEndOfTargetsNextTurn()throws Exception {
        assertEquals("Golpe Dilacerante",DndPtBrLocalizer.localizeText("Gouge"));
        assertTimedConditionAttack("Giant Vulture","Gouge","poisoned","target_turn_end",6,"giant-vulture");
    }

    @Test public void gasSporeTendrilExpiresPoisonAtEndOfTargetsNextTurn()throws Exception {
        assertEquals("Tentáculo",DndPtBrLocalizer.localizeText("Tendril"));
        assertTimedConditionAttack("Gas Spore Fungus","Tendril","poisoned","target_turn_end",4,"gas-spore");
    }

    @Test public void cloudGiantThundercloudExpiresIncapacitatedAtEndOfTargetsNextTurn()throws Exception {
        assertEquals("Nuvem Trovejante",DndPtBrLocalizer.localizeText("Thundercloud"));
        assertTimedConditionAttack("Cloud Giant","Thundercloud","incapacitated","target_turn_end",14,"cloud-giant");
    }

    @Test public void hillGiantTrashLobExpiresPoisonAtEndOfTargetsNextTurn()throws Exception {
        assertEquals("Arremesso de Detritos",DndPtBrLocalizer.localizeText("Trash Lob"));
        assertTimedConditionAttack("Hill Giant","Trash Lob","poisoned","target_turn_end",15,"hill-giant");
    }

    @Test public void couatlBiteExpiresPoisonAtEndOfCouatlsNextTurn()throws Exception {
        assertTimedConditionAttack("Couatl","Bite","poisoned","source_turn_end",11,"couatl");
    }

    @Test public void ghastGravecallerHorrificNecrosisExpiresAtTargetsNextTurnEnd()throws Exception {
        assertEquals("Necrose Horrífica",DndPtBrLocalizer.localizeText("Horrific Necrosis"));
        assertTimedConditionAttack("Ghast Gravecaller","Horrific Necrosis","frightened",
            "target_turn_end",15,"ghast-gravecaller-necrosis");
    }

    @Test public void fourteenXmmOnHitConditionsApplyAndExpireAtStartOfSourceTurn()throws Exception {
        String[][] cases={
            {"Assassin","Shortsword","poisoned","7"},
            {"Giant Centipede","Bite","poisoned","4"},
            {"Centaur Warden","Sun Ray","blinded","14"},
            {"Ettercap","Bite","poisoned","5"},
            {"Githyanki Dracomancer","Draconic Strike","frightened","12"},
            {"Gnoll Fang of Yeenoghu","Bite","poisoned","6"},
            {"Lich","Paralyzing Touch","paralyzed","15"},
            {"Noble Prodigy","Beguiling Strike","charmed","18"},
            {"Oni","Nightmare Ray","frightened","9"},
            {"Quasit","Rend","poisoned","5"},
            {"Satyr Revelmaster","Prance","charmed","13"},
            {"Sprite","Enchanting Bow","charmed","1"},
            {"Vampire Umbral Lord","Sickening Ray","poisoned","16"},
            {"Wyvern","Sting","poisoned","11"}
        };
        for(int i=0;i<cases.length;i++)
            assertTimedConditionAttack(cases[i][0],cases[i][1],cases[i][2],
                "source_turn_start",Integer.parseInt(cases[i][3]),"xmm-source-start-"+i);
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
