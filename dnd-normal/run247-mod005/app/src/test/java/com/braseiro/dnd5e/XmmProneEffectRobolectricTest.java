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

/** Links the pinned XMM Ankylosaurus rider to the durable combat transaction. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmProneEffectRobolectricTest {
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    private static void bind(AppDb db,long id,String block,String size)throws Exception {
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        if(block!=null)resources.put("block_id",block);
        if(size!=null)resources.put("size_category",size);
        ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }

    @Test public void sourceProneRiderFailsClosedThenPersistsAfterARealHit()throws Exception {
        JSONObject install=Xmm2025CompiledBestiaryEngine.install();
        assertEquals(503,install.optInt("statblocks_installed"));
        JSONObject attack=CompiledStatblockEngine.runtimeProfile("Ankylosaurus")
            .getJSONArray("actions").getJSONObject(1);
        assertEquals("Tail",attack.optString("name"));
        assertEquals("prone",attack.optString("condition_on_hit"));
        assertEquals("huge",attack.optString("condition_max_target_size"));
        JSONObject catalog=Xmm2025AttackConditionEngine.profile("Ankylosaurus","Tail");
        assertEquals("/monster/33/action/1/entries/0",catalog.optString("ptr"));
        assertEquals("9a4137b508ef37562ed948894a70b1594b57e85c",
            Xmm2025AttackConditionEngine.SOURCE_BLOB);

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_prone_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try {
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Ankylosaurus",20,60,60,15,"",false);
            target=db.addCombatant("XMM Target",10,50,50,12,"",true);
            bind(db,actor,"Ankylosaurus",null);
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            assertEquals(12,scene.optInt("width_cells"));db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,3,2);

            int initialHp=db.getCombatant(target).optInt("hp");
            JSONObject unknownSize=db.applyCompiledStatblockAttack(actor,target,"Ankylosaurus",
                "Tail",15,0,5,0,"ui-monster-xmm-prone-size-required");
            assertEquals("target_size_not_verified",unknownSize.optString("statblock_attack_blocked"));
            assertEquals(initialHp,db.getCombatant(target).optInt("hp"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));

            bind(db,target,null,"medium");
            String token="ui-monster-xmm-prone-commit";
            JSONObject committed=db.applyCompiledStatblockAttack(actor,target,"Ankylosaurus",
                "Tail",15,0,5,0,token);
            assertTrue(committed.toString(),committed.optBoolean("statblock_attack_committed"));
            assertEquals(initialHp-5,db.getCombatant(target).optInt("hp"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"prone"));
            JSONObject transition=committed.getJSONObject("resolved")
                .getJSONObject("on_hit_condition_transition");
            assertEquals(1,transition.getJSONArray("applied").length());
            assertEquals("caido",transition.getJSONArray("applied").getString(0));

            JSONObject replay=db.applyCompiledStatblockAttack(actor,target,"Ankylosaurus",
                "Tail",15,0,5,0,token);
            assertEquals("already_resolved_or_replayed",replay.optString("statblock_attack_blocked"));
            JSONObject backup=db.exportAll();assertEquals("",Backup21IntegrityEnvelope.check(backup));
            db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
            JSONObject restored=db.findCombatantByName("XMM Target");
            assertEquals(initialHp-5,restored.optInt("hp"));
            assertTrue(ConditionTokenGuard.has(restored.optString("conditions"),"prone"));
            assertEquals(0,new JSONArray(restored.optString("condition_timers_json","[]")).length());
            assertEquals("",Backup21IntegrityEnvelope.check(backup));db.importAll(backup);
            restored=db.findCombatantByName("XMM Target");
            assertEquals(initialHp-5,restored.optInt("hp"));
            assertTrue(ConditionTokenGuard.has(restored.optString("conditions"),"prone"));
        } finally {
            db.close();context.deleteDatabase(dbName);
        }
    }
}
