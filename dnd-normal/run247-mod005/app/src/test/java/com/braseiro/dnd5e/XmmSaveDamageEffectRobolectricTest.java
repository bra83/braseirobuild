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

/** Source-pinned XMM save-for-half damage reaches and persists in combat. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmSaveDamageEffectRobolectricTest {
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    private static void bind(AppDb db,long id,String block)throws Exception {
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        resources.put("block_id",block);ContentValues values=new ContentValues();
        values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }

    @Test public void saveSuccessRejectsFullDamageAndCommitsSourceHalfDamage()throws Exception {
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        JSONObject rule=Xmm2025SaveDamageEngine.rule("Glabrezu","action","Pummel");
        assertTrue(rule.toString(),rule.optBoolean("resolved"));
        assertEquals(17,rule.optInt("dc"));assertEquals(15,rule.optInt("average"));
        JSONObject result=Xmm2025SaveDamageEngine.resolve(new JSONObject()
            .put("monster","Glabrezu").put("category","action").put("action","Pummel")
            .put("target_confirmed",true).put("recharge_ready_confirmed",true)
            .put("save_total",17).put("damage_mode","average"));
        assertTrue(result.toString(),result.optBoolean("resolved"));
        assertTrue(result.optBoolean("save_success"));
        assertEquals(7,result.optInt("damage_before_defenses"));

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_save_half_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try {
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Glabrezu",20,100,100,17,"",false);
            target=db.addCombatant("XMM Save Target",10,40,40,14,"",true);
            bind(db,actor,"Glabrezu");db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            int initialHp=db.getCombatant(target).optInt("hp");

            JSONObject forgedFull=db.applyCompiledStatblockSaveAction(actor,target,"Glabrezu",
                "Pummel",17,15,"ui-monster-save-xmm-full-damage");
            assertEquals("damage_total_outside_compiled_save_dice",
                forgedFull.optString("statblock_save_blocked"));
            assertEquals(initialHp,db.getCombatant(target).optInt("hp"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));

            JSONObject committed=db.applyCompiledStatblockSaveAction(actor,target,"Glabrezu",
                "Pummel",17,result.optInt("damage_before_defenses"),
                "ui-monster-save-xmm-half-damage");
            assertTrue(committed.toString(),committed.optBoolean("statblock_save_committed"));
            assertTrue(committed.getJSONObject("resolved").optBoolean("save_success"));
            assertEquals(7,committed.getInt("damage_total"));
            assertEquals(initialHp-7,db.getCombatant(target).optInt("hp"));
            JSONObject replay=db.applyCompiledStatblockSaveAction(actor,target,"Glabrezu",
                "Pummel",17,7,"ui-monster-save-xmm-half-damage");
            assertEquals("already_resolved_or_replayed",replay.optString("statblock_save_blocked"));
            JSONObject backup=db.exportAll();assertEquals("",Backup21IntegrityEnvelope.check(backup));
            db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
            JSONObject restored=db.findCombatantByName("XMM Save Target");
            assertEquals(initialHp-7,restored.optInt("hp"));
            assertEquals("",Backup21IntegrityEnvelope.check(backup));db.importAll(backup);
            restored=db.findCombatantByName("XMM Save Target");
            assertEquals(initialHp-7,restored.optInt("hp"));
        } finally {
            db.close();context.deleteDatabase(dbName);
        }
    }

    @Test public void rakshasaBalefulCommandPersistsBothConditionsUntilRakshasaTurn()throws Exception {
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        JSONObject action=null;JSONArray actions=CompiledStatblockEngine.runtimeProfile("Rakshasa").optJSONArray("actions");
        for(int i=0;actions!=null&&i<actions.length();i++){
            JSONObject candidate=actions.optJSONObject(i);
            if(candidate!=null&&"Baleful Command".equals(candidate.optString("name")))action=candidate;
        }
        assertNotNull(action);
        assertFalse(action.optBoolean("manual_unmodeled_source_effect_required"));
        assertEquals("frightened, incapacitated",action.optString("condition_on_fail"));
        assertEquals("source_turn_start",action.optString("condition_duration_tick"));
        JSONObject runtime=StatblockCombatRuntimeEngine.savingThrowAction("Rakshasa","Baleful Command",1);
        assertTrue(runtime.toString(),runtime.optBoolean("resolved"));
        assertEquals("frightened, incapacitated",runtime.optString("condition_on_fail"));

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_rakshasa_conditions_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try{
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Rakshasa",20,110,110,16,"",false);
            target=db.addCombatant("XMM Baleful Target",10,80,80,14,"",true);
            bind(db,actor,"Rakshasa");db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            int hpBefore=db.getCombatant(target).optInt("hp");
            JSONObject committed=db.applyCompiledStatblockSaveAction(actor,target,"Rakshasa",
                "Baleful Command",1,28,"ui-monster-save-rakshasa-baleful-command");
            assertTrue(committed.toString(),committed.optBoolean("statblock_save_committed"));
            assertEquals(hpBefore-28,db.getCombatant(target).optInt("hp"));
            JSONObject savedTarget=db.getCombatant(target);
            assertTrue(savedTarget.optString("conditions"),ConditionTokenGuard.has(savedTarget.optString("conditions"),"frightened"));
            assertTrue(savedTarget.optString("conditions"),ConditionTokenGuard.has(savedTarget.optString("conditions"),"incapacitated"));
            JSONArray timers=new JSONArray(savedTarget.optString("condition_timers_json","[]"));
            assertEquals(2,timers.length());
            assertEquals("source_turn_start",timers.getJSONObject(0).optString("trigger"));
            assertEquals(actor,timers.getJSONObject(0).optLong("source_actor_id"));
            assertEquals(target,db.nextTurn().optLong("id"));
            savedTarget=db.getCombatant(target);
            assertTrue(ConditionTokenGuard.has(savedTarget.optString("conditions"),"frightened"));
            assertTrue(ConditionTokenGuard.has(savedTarget.optString("conditions"),"incapacitated"));
            assertEquals(actor,db.nextTurn().optLong("id"));
            savedTarget=db.getCombatant(target);
            assertFalse(ConditionTokenGuard.has(savedTarget.optString("conditions"),"frightened"));
            assertFalse(ConditionTokenGuard.has(savedTarget.optString("conditions"),"incapacitated"));
            assertEquals(0,new JSONArray(savedTarget.optString("condition_timers_json","[]")).length());
        }finally{db.close();context.deleteDatabase(dbName);}
    }

    @Test public void stormGiantThunderboltPersistsBothHitConditionsUntilGiantTurn()throws Exception {
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
        JSONObject action=null;JSONArray actions=CompiledStatblockEngine.runtimeProfile("Storm Giant").optJSONArray("actions");
        for(int i=0;actions!=null&&i<actions.length();i++){
            JSONObject candidate=actions.optJSONObject(i);
            if(candidate!=null&&"Thunderbolt".equals(candidate.optString("name")))action=candidate;
        }
        assertNotNull(action);
        assertEquals("blinded, deafened",action.optString("condition_on_hit"));
        assertEquals("source_turn_start",action.optString("condition_duration_tick"));
        assertFalse(action.optBoolean("manual_unmodeled_source_effect_required"));
        JSONObject runtime=StatblockCombatRuntimeEngine.attack("Storm Giant","Thunderbolt",19,1);
        assertTrue(runtime.toString(),runtime.optBoolean("resolved"));
        assertTrue(runtime.optBoolean("hit"));
        assertEquals("blinded, deafened",runtime.optString("rider_on_hit_condition"));

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_storm_giant_conditions_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try{
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Storm Giant",20,230,230,16,"",false);
            target=db.addCombatant("XMM Thunderbolt Target",10,100,100,1,"",true);
            bind(db,actor,"Storm Giant");db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,4,2);
            JSONObject committed=db.applyCompiledStatblockAttack(actor,target,"Storm Giant","Thunderbolt",
                19,11,0,"ui-monster-xmm-storm-thunderbolt");
            assertTrue(committed.toString(),committed.optBoolean("statblock_attack_committed"));
            assertEquals(89,db.getCombatant(target).optInt("hp"));
            JSONObject affected=db.getCombatant(target);
            assertTrue(ConditionTokenGuard.has(affected.optString("conditions"),"blinded"));
            assertTrue(ConditionTokenGuard.has(affected.optString("conditions"),"deafened"));
            JSONArray timers=new JSONArray(affected.optString("condition_timers_json","[]"));
            assertEquals(2,timers.length());
            assertEquals("source_turn_start",timers.getJSONObject(0).optString("trigger"));
            assertEquals(actor,timers.getJSONObject(0).optLong("source_actor_id"));
            assertEquals(target,db.nextTurn().optLong("id"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"blinded"));
            assertEquals(actor,db.nextTurn().optLong("id"));
            affected=db.getCombatant(target);
            assertFalse(ConditionTokenGuard.has(affected.optString("conditions"),"blinded"));
            assertFalse(ConditionTokenGuard.has(affected.optString("conditions"),"deafened"));
            assertEquals(0,new JSONArray(affected.optString("condition_timers_json","[]")).length());
        }finally{db.close();context.deleteDatabase(dbName);}
    }
}
