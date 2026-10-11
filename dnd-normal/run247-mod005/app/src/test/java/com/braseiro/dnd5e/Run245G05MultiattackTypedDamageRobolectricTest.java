package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** G05: multiattack keeps damage-type components source-bound through SQLite. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run245G05MultiattackTypedDamageRobolectricTest {
    private static JSONObject step(String action,long target,int natural,int damage)throws Exception {
        return new JSONObject().put("action",action).put("target_id",target)
            .put("natural",natural).put("damage_total",damage).put("rider_save_roll",0)
            .put("long_range_second_d20",0).put("additional_hostile_id",0L)
            .put("forced_destination_x",-1).put("forced_destination_y",-1);
    }
    private static JSONArray components(int piercing,int psychic)throws Exception {
        return new JSONArray().put(new JSONObject().put("expression","1d4 + 1")
            .put("damage_type","piercing").put("damage_total",piercing))
            .put(new JSONObject().put("expression","1d6")
                .put("damage_type","psychic").put("damage_total",psychic));
    }
    private static void position(AppDb db,long id,int x,int y) {
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);
        values.put("y",(y+0.5)/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }

    @Test public void multiattackRequiresAndPersistsTypedDamageProofs()throws Exception {
        Context context=RuntimeEnvironment.getApplication();
        String dbName="run245_multi_typed_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");
            String id="run245-multi-typed-"+System.nanoTime();
            JSONObject basis=new JSONObject().put("source_type","compiled_statblock")
                .put("content_kind","statblock").put("content_name",id)
                .put("source_book","RUN245 test-only fixture").put("source_locator","test-only");
            JSONArray actions=new JSONArray()
                .put(new JSONObject().put("name","Bite").put("category","action")
                    .put("to_hit",10).put("damage","1d4 + 1 + 1d6")
                    .put("damage_type","piercing + psychic").put("reach_ft",5))
                .put(new JSONObject().put("name","Echo Bite").put("category","action")
                    .put("to_hit",10).put("damage","1d4 + 1 + 1d6")
                    .put("damage_type","piercing + piercing").put("reach_ft",5))
                .put(new JSONObject().put("name","Claw").put("category","action")
                    .put("to_hit",10).put("damage","1d4 + 2")
                    .put("damage_type","slashing").put("reach_ft",5));
            JSONObject multi=new JSONObject().put("count",2).put("sequence",
                new JSONArray().put("Bite").put("Claw"));
            CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id",id)
                .put("source_book","RUN245 test-only fixture").put("creature_type","monstrosity")
                .put("armor_class",13).put("hit_points",30).put("actions",actions)
                .put("multiattack",multi).put("mechanical_basis",basis));
            long actor=db.addCombatant("RUN245 Typed Multi Actor",20,30,30,13,"",false);
            long target=db.addCombatant("RUN245 Typed Multi Target",10,50,50,13,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            resources.put("block_id",id);ContentValues bind=new ContentValues();
            bind.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",bind,
                "id=? AND world_profile=?",new String[]{String.valueOf(actor),"dnd"}));
            ContentValues resistance=new ContentValues();resistance.put("resistances","psychic");
            assertEquals(1,db.getWritableDatabase().update("combatants",resistance,
                "id=? AND world_profile=?",new String[]{String.valueOf(target),"dnd"}));
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            assertEquals(12,scene.optInt("width_cells"));db.ensureTacticalTokensForCombat();
            position(db,actor,2,2);position(db,target,3,2);

            JSONArray repeatedTypeDamage=new JSONArray()
                .put(new JSONObject().put("expression","1d4 + 1").put("damage_type","piercing")
                    .put("damage_total",2))
                .put(new JSONObject().put("expression","1d6").put("damage_type","piercing")
                    .put("damage_total",3));
            JSONObject repeatedType=db.applyCompiledStatblockAttack(actor,target,id,"Echo Bite",
                10,0,repeatedTypeDamage,0,0L,-1,-1,"ui-monster-run245-repeated-type");
            assertEquals("typed_damage_type_repeated_requires_source_aggregation",
                repeatedType.optString("statblock_attack_blocked"));
            assertEquals(50,db.getCombatant(target).optInt("hp"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));
            assertEquals(0,DatabaseUtils.longForQuery(db.getReadableDatabase(),
                "SELECT COUNT(*) FROM effect_receipts WHERE world_profile=? AND effect_key LIKE ?",
                new String[]{"dnd","%run245-repeated-type%"}));

            JSONArray aggregate=new JSONArray().put(step("Bite",target,10,5))
                .put(step("Claw",target,1,0));
            JSONObject refused=db.applyCompiledStatblockMultiattack(actor,id,aggregate,
                "ui-monster-multi-run245-aggregate");
            assertEquals("typed_damage_components_required_0",
                refused.optString("statblock_multiattack_blocked"));
            assertEquals(50,db.getCombatant(target).optInt("hp"));
            assertTrue(new JSONObject(db.getCombatant(actor).optString("resources_json","{}"))
                .optBoolean("action_available",false));

            JSONObject typedBite=step("Bite",target,10,0);
            typedBite.remove("damage_total");typedBite.put("damage_components",components(2,3));
            JSONArray typed=new JSONArray().put(typedBite).put(step("Claw",target,1,0));
            String token="ui-monster-multi-run245-typed";
            JSONObject committed=db.applyCompiledStatblockMultiattack(actor,id,typed,token);
            assertTrue(committed.toString(),committed.optBoolean("statblock_multiattack_committed",false));
            assertEquals(47,db.getCombatant(target).optInt("hp"));
            JSONObject first=committed.getJSONArray("components").getJSONObject(0)
                .getJSONObject("resolved");
            assertEquals(5,first.optInt("damage_total"));
            assertEquals(3,first.optInt("damage_after_type_modifiers"));
            assertEquals(2,first.getJSONArray("damage_components").length());
            JSONObject replay=db.applyCompiledStatblockMultiattack(actor,id,typed,token);
            assertEquals("already_resolved_or_replayed",
                replay.optString("statblock_multiattack_blocked"));
            JSONObject backup=db.exportAll();assertEquals("",Backup21IntegrityEnvelope.check(backup));
            db.importAll(backup);
            JSONObject restoredActor=db.findCombatantByName("RUN245 Typed Multi Actor");
            JSONObject restoredTarget=db.findCombatantByName("RUN245 Typed Multi Target");
            assertEquals(47,restoredTarget.optInt("hp"));
            JSONObject replayAfterBackup=db.applyCompiledStatblockMultiattack(
                restoredActor.optLong("id"),id,typed,token);
            assertEquals("already_resolved_or_replayed",
                replayAfterBackup.optString("statblock_multiattack_blocked"));
            assertEquals(47,db.findCombatantByName("RUN245 Typed Multi Target").optInt("hp"));
        } finally {db.close();context.deleteDatabase(dbName);}
    }
}
