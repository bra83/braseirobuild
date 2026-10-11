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

/** Android SQLite/AppDb scenario. Written but NOT claimed PASS without CI run. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public class Run233SecondaryRiderRobolectricTest {
    @Test public void conditionRiderCommitsWithReceiptAndRejectsReplayAndForgedSaveDamage() throws Exception {
        String dbName="run233_rider_"+System.nanoTime()+".db";
        Context context=RuntimeEnvironment.getApplication();AppDb db=new AppDb(context,dbName);
        try {
            db.setActiveMapProfile("dnd");
            String id="run233-fixture-"+System.nanoTime();
            JSONObject basis=new JSONObject().put("source_type","compiled_statblock").put("content_kind","statblock")
                .put("content_name",id).put("source_book","RUN233 test-only")
                .put("source_locator","robolectric-test-only");
            JSONArray actions=new JSONArray().put(new JSONObject().put("name","Poison Pulse")
                .put("save_dc",13).put("save_ability","CON").put("condition_on_fail","poisoned")
                .put("effect_on_fail","").put("damage","2d6").put("damage_on_save","half")
                .put("damage_type","poison"));
            CompiledStatblockEngine.registerAuthorized(new JSONObject().put("id",id)
                .put("source_book","RUN233 test-only").put("creature_type","undead")
                .put("armor_class",15).put("hit_points",35).put("actions",actions)
                .put("mechanical_basis",basis));
            long actor=db.addCombatant("Poison source",20,35,35,15,"",false);
            long target=db.addCombatant("Victim",10,30,30,13,"",false);
            assertTrue(actor>0&&target>0);
            JSONObject res=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
            res.put("block_id",id);
            ContentValues v=new ContentValues();v.put("resources_json",res.toString());
            assertEquals(1,db.getWritableDatabase().update("combatants",v,"id=? AND world_profile=?",
                new String[]{String.valueOf(actor),"dnd"}));
            db.setCombatRunning(true);
            ContentValues active=new ContentValues();active.put("active_turn",1);
            assertEquals(1,db.getWritableDatabase().update("combatants",active,"id=? AND world_profile=?",
                new String[]{String.valueOf(actor),"dnd"}));
            JSONObject forged=db.applyCompiledStatblockSaveAction(actor,target,id,"Poison Pulse",5,100,
                "ui-monster-save-run233-forged");
            assertEquals("damage_total_outside_compiled_save_dice",forged.optString("statblock_save_blocked"));
            JSONObject allowed=db.applyCompiledStatblockSaveAction(actor,target,id,"Poison Pulse",5,8,
                "ui-monster-save-run233-confirmed");
            assertTrue(allowed.toString(),allowed.optBoolean("statblock_save_committed"));
            JSONObject proof=allowed.getJSONObject("condition_transition");
            assertEquals(1,proof.getJSONArray("applied").length());
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions",""),"poisoned"));
            JSONObject receipt=db.queryOne("SELECT * FROM effect_receipts WHERE world_profile=? AND effect_key=?",
                new String[]{"dnd",allowed.getString("statblock_save_receipt")});
            assertTrue(receipt.toString(),receipt.length()>0);
            assertTrue(new JSONObject(receipt.optString("source","{}")).has("rider_condition_transition"));
            JSONObject replay=db.applyCompiledStatblockSaveAction(actor,target,id,"Poison Pulse",5,8,
                "ui-monster-save-run233-confirmed");
            assertEquals("already_resolved_or_replayed",replay.optString("statblock_save_blocked"));
            db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
            assertTrue(ConditionTokenGuard.has(db.getCombatant(target).optString("conditions",""),"poisoned"));
        } finally {db.close();context.deleteDatabase(dbName);}
    }
}
