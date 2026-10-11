package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** RHW (2026) p.245, p.253 and p.263 source-turn-bound on-hit conditions. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run245G05RhwTurnBoundHitRidersRobolectricTest {
    private static final class ActionCase {
        final String block, action, condition, url;
        final int page, attackBonus, reach, range, damage;
        final String damageExpression, damageType;
        ActionCase(String block,String action,String condition,String url,int page,
            int attackBonus,int reach,int range,String damageExpression,String damageType,int damage) {
            this.block=block;this.action=action;this.condition=condition;this.url=url;
            this.page=page;this.attackBonus=attackBonus;this.reach=reach;this.range=range;
            this.damageExpression=damageExpression;this.damageType=damageType;this.damage=damage;
        }
    }
    private static final ActionCase[] CASES={
        new ActionCase("Elder Thing","Soothing Tentacle","charmed",
            "https://5e.tools/bestiary.html#elder%20thing_rhw",245,10,10,0,"2d8 + 5","psychic",14),
        new ActionCase("Madam Eva","Radiant Flame","blinded",
            "https://5e.tools/bestiary.html#madam%20eva_rhw",253,9,0,60,"3d10 + 5","radiant",21),
        new ActionCase("Relentless Juggernaut","Crushing Stone","incapacitated",
            "https://5e.tools/bestiary.html#relentless%20juggernaut_rhw",263,10,0,60,"2d6 + 6","bludgeoning",13)
    };

    private static JSONObject action(JSONObject profile,String name)throws Exception {
        JSONArray actions=profile.getJSONArray("actions");
        for(int i=0;i<actions.length();i++)if(name.equals(actions.getJSONObject(i).optString("name")))
            return actions.getJSONObject(i);
        throw new AssertionError("action missing: "+name);
    }
    private static long bindActor(AppDb db,ActionCase c,int initiative)throws Exception {
        long actor=db.addCombatant("RUN245 "+c.block,initiative,200,200,16,"",false);
        JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
        resources.put("block_id",c.block);ContentValues bind=new ContentValues();
        bind.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",bind,
            "id=? AND world_profile=?",new String[]{String.valueOf(actor),"dnd"}));
        return actor;
    }
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    private static void readyGeometry(AppDb db,long actor,long target,int targetX)throws Exception {
        JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
        assertEquals(12,scene.optInt("width_cells"));db.ensureTacticalTokensForCombat();
        position(db,actor,2,2);position(db,target,targetX,2);
    }
    private static long timerReceiptCount(AppDb db) {
        return DatabaseUtils.longForQuery(db.getReadableDatabase(),
            "SELECT COUNT(*) FROM effect_receipts WHERE world_profile=? AND effect_key LIKE 'monster-condition-turn-v1:%'",
            new String[]{"dnd"});
    }

    @Test public void verifiedTurnBoundRidersResolvePersistBackupAndExpireForEachAction()throws Exception {
        JSONObject installed=RavenloftBestiaryRuntimeEngine.install();
        assertEquals(installed.toString(),0,installed.optInt("blocked",0));
        Context context=RuntimeEnvironment.getApplication();
        for(int index=0;index<CASES.length;index++){
            ActionCase c=CASES[index];JSONObject profile=CompiledStatblockEngine.get(c.block);
            assertTrue(profile.toString(),profile.length()>0);
            assertEquals("Ravenloft: The Horrors Within",profile.optString("source_book"));
            JSONObject basis=profile.getJSONObject("mechanical_basis");
            assertEquals("RHW (2026)",basis.optString("source_edition"));
            assertEquals(c.page,basis.optInt("source_page"));
            assertEquals(c.block,basis.optString("source_entry_id"));
            assertEquals(c.action,basis.optString("source_action_id"));
            assertEquals(c.url,basis.optString("source_url"));
            JSONObject sourceAction=action(profile,c.action);
            assertEquals("action",sourceAction.optString("category"));
            assertEquals(c.attackBonus,sourceAction.optInt("to_hit"));
            assertEquals(c.damageExpression,sourceAction.optString("damage"));
            assertEquals(c.damageType,sourceAction.optString("damage_type"));
            assertEquals(c.condition,sourceAction.optString("condition_on_hit"));
            assertFalse(sourceAction.has("condition_on_fail"));
            assertFalse(sourceAction.has("effect_on_fail"));
            assertEquals(1,sourceAction.optInt("condition_duration_rounds"));
            assertEquals("source_turn_start",sourceAction.optString("condition_duration_tick"));
            assertEquals("",MonsterActionEconomyGuard.attackRiderBlock(sourceAction));

            int targetX=c.reach>0?4:6;
            JSONObject range=StatblockCombatRuntimeEngine.rangeProfile(c.block,c.action,c.reach>0?10d:20d);
            assertTrue(range.toString(),range.optBoolean("resolved",false));
            assertFalse(range.toString(),range.optBoolean("out_of_range",true));
            if(c.range>0){
                assertEquals("ranged",range.optString("attack_mode"));
                assertEquals(c.range,range.optInt("range_long_ft"));
            } else {
                assertEquals("melee",range.optString("attack_mode"));
                assertEquals(c.reach,range.optInt("melee_reach_ft"));
            }

            String dbName="r245r_"+Long.toHexString(System.nanoTime() & 0xFFFFFFFL)+"_"+index+".db";
            AppDb db=new AppDb(context,dbName);long actor=0,target=0;
            String token="ui-monster-run245-rhw-turn-hit-"+index;
            try {
                db.setActiveMapProfile("dnd");actor=bindActor(db,c,20);
                target=db.addCombatant("RUN245 Target "+index,10,50,50,13,"",true);
                readyGeometry(db,actor,target,targetX);db.setCombatRunning(true);
                assertEquals(actor,db.nextTurn().optLong("id"));
                JSONObject preview=db.previewCompiledMonsterAttackRollMode(actor,target,c.block,c.action);
                assertTrue(preview.toString(),preview.optBoolean("monster_roll_preview_valid",false));
                JSONObject applied=db.applyCompiledStatblockAttack(actor,target,c.block,c.action,
                    15,0,c.damage,0,0L,token);
                assertTrue(applied.toString(),applied.optBoolean("statblock_attack_committed",false));
                String attackReceipt=applied.optString("statblock_attack_receipt","");
                assertFalse(applied.toString(),attackReceipt.isEmpty());
                assertEquals(1,DatabaseUtils.longForQuery(db.getReadableDatabase(),
                    "SELECT COUNT(*) FROM effect_receipts WHERE world_profile=? AND effect_key=?",
                    new String[]{"dnd",attackReceipt}));
                JSONObject resolved=applied.getJSONObject("resolved");
                assertTrue(resolved.toString(),resolved.optBoolean("hit",false));
                assertEquals(c.condition,resolved.optString("rider_on_hit_condition"));
                assertTrue(resolved.has("on_hit_condition_transition"));
                assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),c.condition));
                JSONArray timers=new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]"));
                assertEquals(1,timers.length());
                assertEquals(actor,timers.getJSONObject(0).optLong("source_actor_id"));
                assertEquals("source_turn_start",timers.getJSONObject(0).optString("trigger"));
                assertEquals(1,timers.getJSONObject(0).optInt("rounds"));

                JSONObject backup=db.exportAll();assertEquals("",Backup21IntegrityEnvelope.check(backup));
                db.importAll(backup);
                JSONObject restoredActor=db.findCombatantByName("RUN245 "+c.block);
                JSONObject restoredTarget=db.findCombatantByName("RUN245 Target "+index);
                assertNotEquals(actor,restoredActor.optLong("id"));
                assertNotEquals(target,restoredTarget.optLong("id"));
                actor=restoredActor.optLong("id");target=restoredTarget.optLong("id");
                JSONArray restoredTimers=new JSONArray(restoredTarget.optString("condition_timers_json","[]"));
                assertEquals(actor,restoredTimers.getJSONObject(0).optLong("source_actor_id"));
                assertEquals("source_turn_start",restoredTimers.getJSONObject(0).optString("trigger"));
                JSONObject replay=db.applyCompiledStatblockAttack(actor,target,c.block,c.action,
                    15,0,c.damage,0,0L,token);
                assertEquals("already_resolved_or_replayed",replay.optString("statblock_attack_blocked"));
                assertEquals(50-c.damage,db.getCombatant(target).optInt("hp"));

                db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
                assertEquals(target,db.nextTurn().optLong("id"));
                assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),c.condition));
                assertEquals(actor,db.nextTurn().optLong("id"));
                assertFalse(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions"),c.condition));
                assertEquals(0,new JSONArray(db.getCombatant(target).optString("condition_timers_json","[]")).length());
                assertEquals(1,timerReceiptCount(db));
                JSONObject secondBackup=db.exportAll();assertEquals("",Backup21IntegrityEnvelope.check(secondBackup));
                db.importAll(secondBackup);
                assertEquals(1,timerReceiptCount(db));
            } finally {
                try { if (db != null) db.close(); } finally { context.deleteDatabase(dbName); }
            }
        }
    }
}
