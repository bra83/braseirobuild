package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** XMM Aboleth source rider through persisted XPHB escape, replay and backup. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmGrappleEscapeRobolectricTest {
    private static void bind(AppDb db,long id,String block,String size)throws Exception {
        JSONObject resources=new JSONObject(db.getCombatant(id).optString("resources_json","{}"));
        if(block!=null)resources.put("block_id",block);
        if(size!=null)resources.put("size_category",size);
        ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }

    @Test public void simpleSourceGrappleCanBeEscapedWithExplicitActionAndPersists()throws Exception {
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        JSONObject rider=Xmm2025AttackConditionEngine.profile("Aboleth","Tentacle");
        assertEquals("/monster/3/action/1/entries/0",rider.optString("ptr"));
        assertEquals(14,rider.optInt("dc"));assertFalse(rider.optBoolean("other"));
        JSONObject attack=CompiledStatblockEngine.runtimeProfile("Aboleth").getJSONArray("actions").getJSONObject(1);
        assertTrue(Xmm2025GrappleEscapeRules.validSource("Aboleth",attack));

        Context context=RuntimeEnvironment.getApplication();
        String dbName="xmm_grapple_"+Long.toHexString(System.nanoTime())+".db";
        AppDb db=new AppDb(context,dbName);long actor=0,target=0;
        try {
            db.setActiveMapProfile("dnd");
            actor=db.addCombatant("XMM Aboleth",20,100,100,17,"",false);
            target=db.addCombatant("XMM Grapple Target",10,40,40,14,"",true);
            bind(db,actor,"Aboleth",null);
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,3,2);
            int hp=db.getCombatant(target).optInt("hp");

            JSONObject unverified=db.applyCompiledStatblockAttack(actor,target,"Aboleth","Tentacle",
                20,0,10,0,"ui-monster-xmm-grapple-size-required");
            assertEquals("target_size_not_verified",unverified.optString("statblock_attack_blocked"));
            assertEquals(hp,db.getCombatant(target).optInt("hp"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));

            bind(db,target,null,"medium");
            String attackToken="ui-monster-xmm-grapple-hit";
            JSONObject hit=db.applyCompiledStatblockAttack(actor,target,"Aboleth","Tentacle",
                20,0,10,0,attackToken);
            assertTrue(hit.toString(),hit.optBoolean("statblock_attack_committed"));
            JSONObject grabbed=db.getCombatant(target);
            assertTrue(ConditionTokenGuard.has(grabbed.optString("conditions"),"grappled"));
            JSONObject state=new JSONObject(grabbed.optString("resources_json","{}")).optJSONObject("xmm_grapple_state");
            assertNotNull(state);assertEquals(14,state.optInt("escape_dc"));
            assertEquals(actor,state.optLong("source_actor_id"));
            assertEquals("/monster/3/action/1/entries/0",state.optString("source_pointer"));
            assertEquals("",Backup21IntegrityEnvelope.check(db.exportAll()));

            position(db,target,8,2);
            JSONObject outOfReach=db.reconcileXmmGrappleStates();
            assertTrue(outOfReach.toString(),outOfReach.optBoolean("grapple_reconciliation_committed"));
            assertEquals("target_outside_grapple_reach",outOfReach.getJSONArray("released")
                .getJSONObject(0).optString("reason"));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"grappled"));
            position(db,target,3,2);
            assertEquals(target,db.nextTurn().optLong("id"));
            assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject secondHit=db.applyCompiledStatblockAttack(actor,target,"Aboleth","Tentacle",
                20,0,10,0,"ui-monster-xmm-grapple-hit-after-range");
            assertTrue(secondHit.toString(),secondHit.optBoolean("statblock_attack_committed"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"grappled"));
            assertEquals(target,db.nextTurn().optLong("id"));
            JSONObject prompt=db.getXmmGrappleEscapeInfo(target);
            assertTrue(prompt.toString(),prompt.optBoolean("grapple_escape_available"));
            assertEquals(14,prompt.optInt("escape_dc"));
            JSONObject invalid=db.resolveXmmGrappleEscape(target,"strength_save",20,
                "ui-grapple-escape-invalid");
            assertEquals("invalid_grapple_escape_skill",invalid.optString("grapple_escape_blocked"));
            JSONObject fail=db.resolveXmmGrappleEscape(target,"athletics",13,"ui-grapple-escape-fail");
            assertTrue(fail.toString(),fail.optBoolean("grapple_escape_committed"));
            assertFalse(fail.getJSONObject("resolved").optBoolean("success"));
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"grappled"));
            assertFalse(new JSONObject(db.getCombatant(target).optString("resources_json","{}"))
                .optBoolean("action_available",true));
            JSONObject replay=db.resolveXmmGrappleEscape(target,"athletics",20,"ui-grapple-escape-fail");
            assertEquals("already_resolved_or_replayed",replay.optString("grapple_escape_blocked"));

            assertEquals(actor,db.nextTurn().optLong("id"));assertEquals(target,db.nextTurn().optLong("id"));
            bind(db,target,null,"medium");
            JSONObject success=db.resolveXmmGrappleEscape(target,"acrobatics",14,"ui-grapple-escape-success");
            assertTrue(success.toString(),success.optBoolean("grapple_escape_committed"));
            assertTrue(success.getJSONObject("resolved").optBoolean("success"));
            JSONObject free=db.getCombatant(target);
            assertFalse(ConditionTokenGuard.has(free.optString("conditions"),"grappled"));
            assertFalse(new JSONObject(free.optString("resources_json","{}")).has("xmm_grapple_state"));
            assertEquals(hp-10,free.optInt("hp"));
            JSONObject backup=db.exportAll();assertEquals("",Backup21IntegrityEnvelope.check(backup));
            db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
            JSONObject reopened=db.findCombatantByName("XMM Grapple Target");
            assertFalse(ConditionTokenGuard.has(reopened.optString("conditions"),"grappled"));
            assertFalse(new JSONObject(reopened.optString("resources_json","{}")).has("xmm_grapple_state"));
            db.importAll(backup);reopened=db.findCombatantByName("XMM Grapple Target");
            assertEquals(hp-10,reopened.optInt("hp"));
            assertFalse(ConditionTokenGuard.has(reopened.optString("conditions"),"grappled"));
            assertFalse(new JSONObject(reopened.optString("resources_json","{}")).has("xmm_grapple_state"));

            assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject thirdHit=db.applyCompiledStatblockAttack(actor,target,"Aboleth","Tentacle",
                20,0,10,0,"ui-monster-xmm-grapple-hit-before-incapacitated-source");
            assertTrue(thirdHit.toString(),thirdHit.optBoolean("statblock_attack_committed"));
            db.updateConditions(actor,"Incapacitado");
            assertEquals(target,db.nextTurn().optLong("id"));
            JSONObject incapacitated=db.resolveXmmGrappleEscape(target,"athletics",30,
                "ui-grapple-escape-source-incapacitated");
            assertTrue(incapacitated.toString(),incapacitated.optBoolean("automatic_release"));
            assertFalse(incapacitated.optBoolean("action_spent",true));
            assertTrue(new JSONObject(db.getCombatant(target).optString("resources_json","{}"))
                .optBoolean("action_available",false));
            assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),"grappled"));
        } finally { db.close();context.deleteDatabase(dbName); }
    }
}
