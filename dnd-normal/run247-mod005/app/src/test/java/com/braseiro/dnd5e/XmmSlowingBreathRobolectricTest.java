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

/** SQLite boundaries, one-action area activation, reopen, replay and backup restore. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmSlowingBreathRobolectricTest {
    private static final class Fixture implements AutoCloseable {
        final Context context=RuntimeEnvironment.getApplication();
        final String name="xmm_slow_"+System.nanoTime()+".db";
        AppDb db;long source,target,second;String block;
        Fixture(String block)throws Exception {
            this.block=block;assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
            db=new AppDb(context,name);db.setActiveMapProfile("dnd");
            source=db.addCombatant("Sopro origem",30,100,100,15,"",false);
            target=db.addCombatant("Sopro alvo",20,100,100,12,"",false);
            second=db.addCombatant("Sopro segundo",10,100,100,12,"",false);
            String size=block.startsWith("Ancient")?"Gargantuan":block.startsWith("Adult")?"Huge":
                block.startsWith("Young")?"Large":"Medium";
            int footprint="Gargantuan".equals(size)?4:"Huge".equals(size)?3:"Large".equals(size)?2:1;
            resources(source,new JSONObject().put("block_id",block).put("size_category",size));
            resources(target,new JSONObject().put("size_category","Medium"));
            resources(second,new JSONObject().put("size_category","Medium"));
            db.setCombatRunning(true);assertEquals(source,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            ContentValues v=new ContentValues();v.put("obstacles_json","[]");
            assertEquals(1,db.getWritableDatabase().update("tactical_scenes",v,
                "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
            db.ensureTacticalTokensForCombat();position(source,1,1);
            position(target,footprint+2,1);position(second,footprint+2,2);
        }
        void resources(long id,JSONObject r){ContentValues v=new ContentValues();v.put("resources_json",r.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",v,"id=?",new String[]{String.valueOf(id)}));}
        JSONObject r(long id)throws Exception{return new JSONObject(db.getCombatant(id).optString("resources_json","{}"));}
        void position(long id,int x,int y){ContentValues v=new ContentValues();v.put("x",(x+0.5)/12d);v.put("y",(y+0.5)/8d);
            assertEquals(1,db.getWritableDatabase().update("tactical_tokens",v,"combatant_id=? AND world_profile=?",
                new String[]{String.valueOf(id),"dnd"}));}
        JSONObject apply(int total,String token){return db.applyCompiledStatblockSaveAction(source,target,block,
            "Slowing Breath",total,0,token);}
        public void close(){db.close();context.deleteDatabase(name);}
    }
    @Test public void fourVariantsConsumeActionWithoutInventingRechargeOrCondition()throws Exception {
        for(String block:new String[]{"Adult Copper Dragon","Ancient Copper Dragon","Copper Dragon Wyrmling","Young Copper Dragon"})
            try(Fixture f=new Fixture(block)){
                JSONObject result=f.apply(1,"ui-monster-save-variants-test");
                assertTrue(result.toString(),result.optBoolean("statblock_save_committed"));
                assertFalse(result.optBoolean("recharge_spent"));assertFalse(f.r(f.source).optBoolean("action_available"));
                assertTrue(XmmSaveEffectLedger.slowed(f.r(f.target),f.target));
                assertEquals("",f.db.getCombatant(f.target).optString("conditions"));
                assertEquals(100,f.db.getCombatant(f.target).optInt("hp"));
            }
    }
    @Test public void successAtDcConsumesActionButAppliesNoEffect()throws Exception {
        try(Fixture f=new Fixture("Adult Copper Dragon")){
            assertTrue(f.apply(18,"ui-monster-save-success-test").optBoolean("statblock_save_committed"));
            assertFalse(XmmSaveEffectLedger.slowed(f.r(f.target),f.target));assertFalse(f.r(f.source).optBoolean("action_available"));
        }
    }
    @Test public void targetTurnUsesHalfSpeedActionOrBonusAndThenExpires()throws Exception {
        try(Fixture f=new Fixture("Adult Copper Dragon")){
            assertTrue(f.apply(1,"ui-monster-save-economy-test").optBoolean("statblock_save_committed"));
            assertEquals("slowing_breath_no_reactions",f.db.spendCombatantTurnResource(f.target,"reaction").optString("resource_blocked"));
            assertEquals(f.target,f.db.nextTurn().optLong("id"));assertEquals(15,f.r(f.target).optInt("movement_max_ft"));
            assertFalse(f.db.spendCombatantTurnResource(f.target,"bonus_action").has("resource_blocked"));
            assertEquals("slowing_breath_action_or_bonus",f.db.spendCombatantTurnResource(f.target,"action").optString("resource_blocked"));
            assertEquals(f.second,f.db.nextTurn().optLong("id"));assertFalse(XmmSaveEffectLedger.slowed(f.r(f.target),f.target));
            assertFalse(f.db.spendCombatantTurnResource(f.target,"reaction").has("resource_blocked"));
        }
    }
    @Test public void coneBatchConsumesOneActionForSuccessAndFailureTargets()throws Exception {
        try(Fixture f=new Fixture("Adult Copper Dragon")){
            JSONArray saves=new JSONArray().put(new JSONObject().put("target_id",f.target).put("save_total",1))
                .put(new JSONObject().put("target_id",f.second).put("save_total",18));
            JSONObject result=f.db.applyCompiledSaveEffectBatch(f.source,f.block,"Slowing Breath",saves,"ui-monster-save-batch-test");
            assertTrue(result.toString(),result.optBoolean("save_effect_batch_committed"));
            assertTrue(XmmSaveEffectLedger.slowed(f.r(f.target),f.target));assertFalse(XmmSaveEffectLedger.slowed(f.r(f.second),f.second));
            assertFalse(f.r(f.source).optBoolean("action_available"));
            assertEquals("already_resolved_or_replayed",f.db.applyCompiledSaveEffectBatch(f.source,f.block,"Slowing Breath",saves,
                "ui-monster-save-batch-test").optString("save_effect_batch_blocked"));
        }
    }
    @Test public void invalidLaterTargetRollsBackWholeConeAndAction()throws Exception {
        try(Fixture f=new Fixture("Adult Copper Dragon")){
            JSONArray saves=new JSONArray().put(new JSONObject().put("target_id",f.target).put("save_total",1))
                .put(new JSONObject().put("target_id",f.second).put("save_total",101));
            JSONObject result=f.db.applyCompiledSaveEffectBatch(f.source,f.block,"Slowing Breath",saves,"ui-monster-save-rollback-test");
            assertFalse(result.optBoolean("save_effect_batch_committed"));
            assertFalse(XmmSaveEffectLedger.slowed(f.r(f.target),f.target));assertTrue(f.r(f.source).optBoolean("action_available"));
        }
    }
    @Test public void invalidDamageAndDuplicateTargetsDoNotSpendAction()throws Exception {
        try(Fixture f=new Fixture("Adult Copper Dragon")){
            assertEquals("damage_total_outside_compiled_save_dice",f.db.applyCompiledStatblockSaveAction(f.source,f.target,f.block,
                "Slowing Breath",1,1,"ui-monster-save-bad-damage-test").optString("statblock_save_blocked"));
            JSONObject row=new JSONObject().put("target_id",f.target).put("save_total",1);
            assertEquals("invalid_or_duplicate_target_save",f.db.applyCompiledSaveEffectBatch(f.source,f.block,"Slowing Breath",
                new JSONArray().put(row).put(row),"ui-monster-save-duplicate-test").optString("save_effect_batch_blocked"));
            assertTrue(f.r(f.source).optBoolean("action_available"));
        }
    }
    @Test public void reopenPreservesEffectAndPersistentReplayReceipt()throws Exception {
        try(Fixture f=new Fixture("Adult Copper Dragon")){
            assertTrue(f.apply(1,"ui-monster-save-reopen-test").optBoolean("statblock_save_committed"));
            f.db.close();f.db=new AppDb(f.context,f.name);f.db.setActiveMapProfile("dnd");
            assertTrue(XmmSaveEffectLedger.slowed(f.r(f.target),f.target));
            assertEquals("already_resolved_or_replayed",f.apply(1,"ui-monster-save-reopen-test").optString("statblock_save_blocked"));
            assertEquals(1,XmmSaveEffectLedger.read(f.r(f.target),f.target).length());
        }
    }
    @Test public void backupRestoreRemapsSourceTargetAndKeepsExpiry()throws Exception {
        try(Fixture f=new Fixture("Adult Copper Dragon")){
            assertTrue(f.apply(1,"ui-monster-save-backup-test").optBoolean("statblock_save_committed"));
            JSONObject backup=f.db.exportAll();f.db.importAll(backup);
            long source=f.db.findCombatantByName("Sopro origem").optLong("id"),target=f.db.findCombatantByName("Sopro alvo").optLong("id");
            JSONObject row=XmmSaveEffectLedger.read(f.r(target),target).getJSONObject(0);
            assertEquals(source,row.optLong("source_actor_id"));assertEquals(target,row.optLong("target_id"));
            assertEquals("already_resolved_or_replayed",f.db.applyCompiledStatblockSaveAction(source,target,f.block,
                "Slowing Breath",1,0,"ui-monster-save-backup-test").optString("statblock_save_blocked"));
            assertNotEquals(f.source,source);assertEquals(target,f.db.nextTurn().optLong("id"));
            f.db.nextTurn();assertFalse(XmmSaveEffectLedger.slowed(f.r(target),target));
        }
    }
}
