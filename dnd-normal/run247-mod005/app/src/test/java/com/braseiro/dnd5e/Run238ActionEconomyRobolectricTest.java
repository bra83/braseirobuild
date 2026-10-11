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

/** Android SQLite/AppDb contract. Written in RUN238; only PASS when CI executes. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run238ActionEconomyRobolectricTest {
    private static JSONObject action(String name,String category,boolean extraRider)throws Exception{
        JSONObject a=new JSONObject().put("name",name).put("category",category)
            .put("to_hit",10).put("damage","1d6").put("damage_type","bludgeoning")
            .put("reach_ft",5);
        if(extraRider)a.put("condition_on_fail","paralyzed");
        return a;
    }
    @Test public void bonusAttackConsumesOnlyBonusAndUnsupportedRidersDoNotCommit()throws Exception{
        Context context=RuntimeEnvironment.getApplication();String name="run238_economy_"+System.nanoTime()+".db";
        AppDb db=new AppDb(context,name);
        try{
            db.setActiveMapProfile("dnd");
            String id="run238-fixture-"+System.nanoTime();
            JSONObject basis=new JSONObject().put("source_type","compiled_statblock")
                .put("content_kind","statblock").put("content_name",id)
                .put("source_book","RUN238 test fixture").put("source_locator","run238-test-only");
            JSONArray actions=new JSONArray()
                .put(action("Bonus Strike","bonus_action",false))
                .put(action("Normal Strike","action",false))
                .put(action("Unmodeled Touch","action",true))
                .put(action("Legendary Strike","legendary_action",false))
                .put(action("Reaction Strike","reaction",false))
                .put(action("Unknown Save Strike","action",false).put("save_dc",14));
            CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id",id)
                .put("source_book","RUN238 test fixture").put("creature_type","construct")
                .put("armor_class",15).put("hit_points",35).put("actions",actions)
                .put("mechanical_basis",basis));
            long source=db.addCombatant("RUN238 Source",20,35,35,15,"",false);
            long target=db.addCombatant("RUN238 Target",10,60,60,13,"",true);
            JSONObject resources=new JSONObject(db.getCombatant(source).optString("resources_json","{}"));
            resources.put("block_id",id);
            ContentValues actor=new ContentValues();actor.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",actor,
                "id=? AND world_profile=?",new String[]{String.valueOf(source),"dnd"}));
            db.setCombatRunning(true);
            // Use the production turn transition so action/bonus budgets are
            // prepared by AppDb itself, not by an artificial SQL active flag.
            JSONObject turn=db.nextTurn();
            assertEquals("first turn: "+turn.toString(),source,turn.optLong("id"));
            JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            assertEquals(12,scene.optInt("width_cells"));
            db.ensureTacticalTokensForCombat();
            ContentValues from=new ContentValues();from.put("x",(2.5)/12d);from.put("y",(2.5)/8d);
            ContentValues to=new ContentValues();to.put("x",(3.5)/12d);to.put("y",(2.5)/8d);
            assertEquals(1,db.getWritableDatabase().update("tactical_tokens",from,
                "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(source)}));
            assertEquals(1,db.getWritableDatabase().update("tactical_tokens",to,
                "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(target)}));
            int before=db.getCombatant(target).optInt("hp");
            JSONObject bonus=db.applyCompiledStatblockAttack(source,target,id,"Bonus Strike",10,3,0,
                "ui-monster-run238-bonus");
            assertTrue(bonus.toString(),bonus.optBoolean("statblock_attack_committed"));
            JSONObject r=new JSONObject(db.getCombatant(source).optString("resources_json","{}"));
            assertFalse(r.optBoolean("bonus_action_available",true));
            assertTrue(r.optBoolean("action_available",true));
            assertEquals("bonus_action",bonus.getJSONObject("resolved").getString("resource_cost"));
            JSONObject again=db.applyCompiledStatblockAttack(source,target,id,"Bonus Strike",10,3,0,
                "ui-monster-run238-bonus-again");
            assertTrue(again.toString(),again.has("statblock_attack_blocked"));
            assertEquals(before-3,db.getCombatant(target).optInt("hp"));
            JSONObject normal=db.applyCompiledStatblockAttack(source,target,id,"Normal Strike",10,3,0,
                "ui-monster-run238-normal");
            assertTrue(normal.toString(),normal.optBoolean("statblock_attack_committed"));
            r=new JSONObject(db.getCombatant(source).optString("resources_json","{}"));
            assertFalse(r.optBoolean("action_available",true));
            assertEquals(before-6,db.getCombatant(target).optInt("hp"));
            for(String special:new String[]{"Legendary Strike","Reaction Strike","Unmodeled Touch","Unknown Save Strike"}){
                JSONObject blocked=db.applyCompiledStatblockAttack(source,target,id,special,10,3,0,
                    "ui-monster-run238-blocked-"+special.replace(' ','-'));
                assertTrue(special+" "+blocked,blocked.has("statblock_attack_blocked"));
                assertEquals(before-6,db.getCombatant(target).optInt("hp"));
            }
            JSONObject backup=db.exportAll();db.importAll(backup);
            JSONObject restored=db.findCombatantByName("RUN238 Target");
            assertEquals(before-6,restored.optInt("hp"));
        }finally{db.close();context.deleteDatabase(name);}
    }
}
