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

/** XMM Brown Bear: source-bound Bite then Claw, final on-hit Prone persisted. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmMultiattackProneRobolectricTest {
    private static void bind(AppDb db,long id,String key,String value)throws Exception {
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        resources.put(key,value);
        ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }
    private static void position(AppDb db,long id,int x,int y){
        ContentValues v=new ContentValues();v.put("x",(x+0.5)/12d);v.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",v,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    private static JSONObject step(String action,long target,int roll,int damage)throws Exception {
        return new JSONObject().put("action",action).put("target_id",target)
            .put("natural",roll).put("damage_total",damage).put("rider_save_roll",0)
            .put("long_range_second_d20",0).put("additional_hostile_id",0L)
            .put("forced_destination_x",-1).put("forced_destination_y",-1);
    }
    @Test public void finalSourceProneIsAtomicSizeGatedAndDurable()throws Exception {
        JSONObject installed=Xmm2025CompiledBestiaryEngine.install();
        assertEquals(503,installed.optInt("statblocks_installed"));
        JSONObject source=CompiledStatblockEngine.runtimeProfile("Brown Bear");
        JSONObject last=source.getJSONArray("actions").getJSONObject(2);
        assertEquals("Claw",last.optString("name"));
        assertTrue(MonsterOnHitProneRules.validSource(last));
        assertEquals("/monster/78/action/2/entries/0",
            Xmm2025AttackConditionEngine.profile("Brown Bear","Claw").optString("ptr"));
        Context context=RuntimeEnvironment.getApplication();
        String name="xmm_multi_prone_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,name);
        try {
            db.setActiveMapProfile("dnd");
            long actor=db.addCombatant("XMM Brown Bear",20,50,50,15,"",false);
            long target=db.addCombatant("XMM Prone Target",10,50,50,12,"",true);
            bind(db,actor,"block_id","Brown Bear");
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            assertEquals(12,scene.optInt("width_cells"));db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,3,2);
            JSONArray rolls=new JSONArray().put(step("Bite",target,15,5))
                .put(step("Claw",target,15,5));
            JSONObject unknown=db.applyCompiledStatblockMultiattack(actor,"Brown Bear",rolls,
                "ui-monster-multi-xmm-prone-unknown");
            assertEquals("target_size_not_verified_1",
                unknown.optString("statblock_multiattack_blocked"));
            assertEquals(50,db.getCombatant(target).optInt("hp"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));
            bind(db,target,"size_category","medium");
            String token="ui-monster-multi-xmm-prone-commit";
            JSONObject committed=db.applyCompiledStatblockMultiattack(actor,"Brown Bear",rolls,token);
            assertTrue(committed.toString(),committed.optBoolean("statblock_multiattack_committed"));
            assertTrue(committed.optBoolean("action_spent_once"));
            assertEquals(40,db.getCombatant(target).optInt("hp"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions",""),"prone"));
            JSONObject first=committed.getJSONArray("components").getJSONObject(0).getJSONObject("resolved");
            JSONObject second=committed.getJSONArray("components").getJSONObject(1).getJSONObject("resolved");
            assertFalse(first.has("on_hit_condition_transition"));
            assertEquals("caido",second.getJSONObject("on_hit_condition_transition")
                .getJSONArray("applied").getString(0));
            assertEquals(0,new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]")).length());
            JSONObject replay=db.applyCompiledStatblockMultiattack(actor,"Brown Bear",rolls,token);
            assertEquals("already_resolved_or_replayed",replay.optString("statblock_multiattack_blocked"));
            JSONObject backup=db.exportAll();assertEquals("",Backup21IntegrityEnvelope.check(backup));
            db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");
            JSONObject restored=db.findCombatantByName("XMM Prone Target");
            assertEquals(40,restored.optInt("hp"));
            assertTrue(ConditionTokenGuard.has(restored.optString("conditions",""),"prone"));
            db.importAll(backup);
            restored=db.findCombatantByName("XMM Prone Target");
            assertEquals(40,restored.optInt("hp"));
            assertTrue(ConditionTokenGuard.has(restored.optString("conditions",""),"prone"));
        } finally {db.close();context.deleteDatabase(name);}
    }
}
