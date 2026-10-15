package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** New source plans must use the existing SQLite transaction, including replay guards. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class XmmMultiattackHostRobolectricTest {
    Context context; AppDb db; String name; JSONObject registry; long actor,target;
    @Before public void setup() throws Exception {
        context=RuntimeEnvironment.getApplication();name="xmm-host-"+System.nanoTime()+".db";
        registry=CompiledStatblockEngine.snapshot();Xmm2025CompiledBestiaryEngine.install();
        db=new AppDb(context,name);db.setActiveMapProfile("dnd");
        actor=db.addCombatant("Ape",20,30,30,12,"",false);
        target=db.addCombatant("Alvo",10,50,50,15,"",true);bind("Ape");
        db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
        JSONObject scene=new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
        ContentValues clear=new ContentValues();clear.put("obstacles_json","[]");
        assertEquals(1,db.getWritableDatabase().update("tactical_scenes",clear,
            "world_profile=? AND scene_key=?",new String[]{"dnd",scene.optString("scene_key")}));
        db.ensureTacticalTokensForCombat();
        position(actor,2);position(target,3);
    }
    @After public void close(){db.close();context.deleteDatabase(name);CompiledStatblockEngine.restoreReplacing(registry);}
    void bind(String block) throws Exception {
        JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
        resources.put("block_id",block);ContentValues v=new ContentValues();v.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",v,"id=?",new String[]{String.valueOf(actor)}));
    }
    void position(long id,int x){ContentValues v=new ContentValues();v.put("x",(x+.5)/12d);v.put("y",2.5/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",v,"world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));}
    JSONArray rolls(String action) throws Exception {
        JSONArray rows=new JSONArray();for(int i=0;i<2;i++)rows.put(new JSONObject().put("action",action)
            .put("target_id",target).put("natural",1).put("long_range_second_d20",0)
            .put("damage_total",0).put("rider_save_roll",0));return rows;
    }
    void unchanged() throws Exception {
        assertEquals(50,db.getCombatant(target).optInt("hp"));
        assertTrue(new JSONObject(db.getCombatant(actor).getString("resources_json")).optBoolean("action_available"));
        assertEquals(0,DatabaseUtils.longForQuery(db.getReadableDatabase(),"SELECT COUNT(*) FROM effect_receipts",null));
    }
    @Test public void typedPlanCommitsOnceAndReceiptSurvivesReopen() throws Exception {
        String token="ui-monster-multi-xmm-host-replay";
        JSONObject result=db.applyCompiledStatblockMultiattack(actor,"Ape",rolls("Fist"),token,new JSONObject());
        assertTrue(result.toString(),result.optBoolean("statblock_multiattack_committed"));
        assertTrue(result.optBoolean("action_spent_once"));assertEquals(2,result.getJSONArray("components").length());
        assertEquals(50,db.getCombatant(target).optInt("hp"));
        String receipt=result.getString("statblock_multiattack_receipt");assertTrue(db.hasEffectReceipt(receipt));
        db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");assertTrue(db.hasEffectReceipt(receipt));
        JSONObject replay=db.applyCompiledStatblockMultiattack(actor,"Ape",rolls("Fist"),token,new JSONObject());
        assertEquals("already_resolved_or_replayed",replay.optString("statblock_multiattack_blocked"));
    }
    @Test public void inventedSequenceAndLateInvalidTargetDoNotSpendAction() throws Exception {
        JSONObject selection=new JSONObject().put("selected_actions",new JSONArray().put("Fist").put("Fireball"));
        assertEquals("attack_choice_not_allowed",db.applyCompiledStatblockMultiattack(actor,"Ape",rolls("Fist"),
            "ui-monster-multi-invalid-choice",selection).optString("statblock_multiattack_blocked"));unchanged();
        JSONArray late=rolls("Fist");late.getJSONObject(1).put("target_id",-1);
        assertEquals("invalid_or_covered_target_1",db.applyCompiledStatblockMultiattack(actor,"Ape",late,
            "ui-monster-multi-invalid-late",new JSONObject()).optString("statblock_multiattack_blocked"));unchanged();
    }
    @Test public void spellCompositionBlocksBeforeMutation() throws Exception {
        bind("Adult Red Dragon");JSONObject selection=new JSONObject().put("variant_index",1)
            .put("selected_actions",new JSONArray().put("Scorching Ray").put("Rend").put("Rend"));
        JSONObject result=db.applyCompiledStatblockMultiattack(actor,"Adult Red Dragon",new JSONArray(),
            "ui-monster-multi-spell-block",selection);
        assertEquals("multiattack_component_executor_not_installed_spell_0",result.optString("statblock_multiattack_blocked"));unchanged();
    }
}
