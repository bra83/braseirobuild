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

/** G05/RUN232: executable Android SQLite/AppDb regression once Gradle/Robolectric is available.
 * No result from this class is claimed until it has actually been run.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public class Run232RechargeRobolectricTest {
    @Test public void rechargeLegacyAliasesCannotBypassSpendOrRoundOnRestart() throws Exception {
        String name="run232_recharge_"+System.nanoTime()+".db";
        Context context=RuntimeEnvironment.getApplication();
        AppDb db=new AppDb(context,name);db.setActiveMapProfile("dnd");
        try {
            String block="run232-recharge-"+System.nanoTime();
            JSONObject source=new JSONObject().put("source_type","compiled_statblock")
                .put("content_kind","statblock").put("content_name",block)
                .put("source_book","RUN232 local test fixture")
                .put("source_locator","robolectric-only");
            JSONObject breath=new JSONObject().put("name","Breath Weapon")
                .put("category","action").put("save_dc",13).put("save_ability","DEX")
                .put("damage","2d6").put("damage_type","fire")
                .put("recharge","5-6");
            CompiledStatblockEngine.registerAuthorized(new JSONObject()
                .put("id",block).put("source_book","RUN232 local test fixture")
                .put("creature_type","dragon").put("armor_class",14)
                .put("hit_points",30).put("actions",new JSONArray().put(breath))
                .put("mechanical_basis",source));
            long actor=db.addCombatant("Cuspideiro",20,30,30,14,"",false);
            assertTrue(actor>0);
            JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            resources.put("block_id",block);
            resources.put("statblock_recharge_spent",new JSONObject()
                .put("Breath Weapon",true).put("BREATH WEAPON",false));
            resources.put("statblock_recharge_spent_round",new JSONObject().put("Breath Weapon",1));
            ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",values,"id=? AND world_profile=?",
                new String[]{String.valueOf(actor),"dnd"}));
            db.setCombatRunning(true);
            ContentValues active=new ContentValues();active.put("active_turn",1);
            assertEquals(1,db.getWritableDatabase().update("combatants",active,"id=? AND world_profile=?",
                new String[]{String.valueOf(actor),"dnd"}));
            ContentValues round=new ContentValues();round.put("round_no",2);
            db.getWritableDatabase().update("combat_state_profiles",round,"world_profile=?",new String[]{"dnd"});
            JSONObject fail=db.rechargeCompiledStatblockAction(actor,block," breath weapon ",4,
                "ui-monster-recharge-run232-01");
            assertTrue(fail.toString(),fail.optBoolean("statblock_recharge_committed"));
            assertFalse(fail.getJSONObject("resolved").optBoolean("recharged"));
            JSONObject replay=db.rechargeCompiledStatblockAction(actor,block,"BREATH WEAPON",6,
                "ui-monster-recharge-run232-02");
            assertFalse(replay.toString(),replay.optBoolean("statblock_recharge_committed"));
            assertEquals("recharge_already_rolled_this_turn",replay.optString("statblock_recharge_blocked"));
            JSONObject saved=new JSONObject(db.getCombatant(actor).optString("resources_json"));
            JSONObject spent=saved.getJSONObject("statblock_recharge_spent");
            assertEquals(1,spent.length());assertTrue(spent.optBoolean("breath weapon"));
            db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");
            ContentValues next=new ContentValues();next.put("round_no",3);
            db.getWritableDatabase().update("combat_state_profiles",next,"world_profile=?",new String[]{"dnd"});
            JSONObject success=db.rechargeCompiledStatblockAction(actor,block,"Breath Weapon",6,
                "ui-monster-recharge-run232-03");
            assertTrue(success.toString(),success.optBoolean("statblock_recharge_committed"));
            assertTrue(success.getJSONObject("resolved").optBoolean("recharged"));
            JSONObject after=new JSONObject(db.getCombatant(actor).optString("resources_json"));
            assertFalse(after.getJSONObject("statblock_recharge_spent").optBoolean("breath weapon",true));
            assertEquals(3,after.getJSONObject("statblock_recharge_rolled_round").optInt("breath weapon"));
        } finally {db.close();context.deleteDatabase(name);}
    }
}
