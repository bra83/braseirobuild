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
    @Test public void spoofedHastenCannotEnableExtraSlamFromUi() throws Exception {
        bind("Clay Golem");
        JSONObject forged = new JSONObject().put("variant_index", 1)
            .put("hasten_used_this_turn", true)
            .put("selected_actions", new JSONArray().put("Slam").put("Slam").put("Slam"));
        JSONObject denied = db.applyCompiledStatblockMultiattack(actor, "Clay Golem",
            new JSONArray(), "ui-monster-multi-hasten-spoof", forged);
        assertEquals("multiattack_context_not_authorized_by_host",
            denied.optString("statblock_multiattack_blocked"));
        unchanged();
        // The baseline two attacks remain selectable without trusting a UI flag.
        JSONObject base = new JSONObject().put("variant_index", 0)
            .put("selected_actions", new JSONArray().put("Slam").put("Slam"));
        JSONObject resources = new JSONObject(db.getCombatant(actor).getString("resources_json"));
        JSONObject plan = XmmMultiattackHostPlan.declaration("Clay Golem", base, resources);
        assertFalse(plan.toString(), plan.has("statblock_multiattack_blocked"));
        assertEquals(2, plan.getInt("count"));
        // Preview may describe a selected variant, but is explicitly non-authoritative.
        JSONObject preview = XmmMultiattackHostPlan.declaration("Clay Golem", forged);
        assertFalse(preview.toString(), preview.has("statblock_multiattack_blocked"));
        assertTrue(preview.optBoolean("preview_only"));
        assertEquals(3, preview.optInt("count"));
        // A real host may authorize the same variant only from a persisted boolean.
        resources.put("hasten_used_this_turn", true);
        JSONObject authorized = XmmMultiattackHostPlan.declaration("Clay Golem", forged, resources);
        assertFalse(authorized.toString(), authorized.has("statblock_multiattack_blocked"));
        assertFalse(authorized.optBoolean("preview_only"));
        assertEquals(3, authorized.optInt("count"));
        // A non-boolean persisted value must fail closed rather than be coerced.
        resources.put("hasten_used_this_turn", "true");
        assertEquals("multiattack_context_persisted_invalid",
            XmmMultiattackHostPlan.declaration("Clay Golem", base, resources)
                .optString("statblock_multiattack_blocked"));
    }
    @Test public void persistedHastenSurvivesReopenAndCannotBeOverriddenByUi() throws Exception {
        bind("Clay Golem");
        JSONObject resources=new JSONObject(db.getCombatant(actor).getString("resources_json"));
        resources.put("hasten_used_this_turn",true);
        ContentValues v=new ContentValues();v.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",v,
            "id=?",new String[]{String.valueOf(actor)}));
        db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");
        JSONObject restored=new JSONObject(db.getCombatant(actor).getString("resources_json"));
        assertTrue("host context must survive SQLite reopen",
            restored.optBoolean("hasten_used_this_turn"));
        JSONObject triple=new JSONObject().put("variant_index",1)
            .put("hasten_used_this_turn",true)
            .put("selected_actions",new JSONArray().put("Slam").put("Slam").put("Slam"));
        JSONObject declared=XmmMultiattackHostPlan.declaration("Clay Golem",triple,restored);
        assertFalse(declared.toString(),declared.has("statblock_multiattack_blocked"));
        assertEquals(3,declared.optInt("count"));
        assertFalse(declared.optBoolean("preview_only"));
        JSONObject forged=new JSONObject(triple.toString()).put("hasten_used_this_turn",false);
        JSONObject denied=db.applyCompiledStatblockMultiattack(actor,"Clay Golem",
            new JSONArray(),"ui-monster-multi-hasten-reopen-denied",forged);
        assertEquals("multiattack_context_not_authorized_by_host",
            denied.optString("statblock_multiattack_blocked"));
        assertEquals(0,DatabaseUtils.longForQuery(db.getReadableDatabase(),
            "SELECT COUNT(*) FROM effect_receipts",null));
        JSONObject noRolls=db.applyCompiledStatblockMultiattack(actor,"Clay Golem",
            new JSONArray(),"ui-monster-multi-hasten-reopen-count",triple);
        assertEquals("multiattack_count_mismatch",
            noRolls.optString("statblock_multiattack_blocked"));
        assertEquals(50,db.getCombatant(target).optInt("hp"));
        assertEquals(0,DatabaseUtils.longForQuery(db.getReadableDatabase(),
            "SELECT COUNT(*) FROM effect_receipts",null));
    }
    @Test public void hybridMultiattackDeclarationPermitsKnownSpellsAndBlocksUnknown() throws Exception {
        bind("Adult Red Dragon");
        assertTrue(XmmMultiattackHostPlan.actionExistsInStatblock("Adult Red Dragon", "Scorching Ray"));
        assertTrue(XmmMultiattackHostPlan.actionExistsInStatblock("Adult Red Dragon", "Fire Breath"));
        assertFalse(XmmMultiattackHostPlan.actionExistsInStatblock("Adult Red Dragon", "Wish"));

        // Valid registered variant with Scorching Ray + 2x Rend
        JSONObject selection=new JSONObject().put("variant_index",1)
            .put("selected_actions",new JSONArray().put("Scorching Ray").put("Rend").put("Rend"));
        JSONObject decl = XmmMultiattackHostPlan.declaration("Adult Red Dragon", selection, new JSONObject());
        assertFalse("Known spell Scorching Ray must be permitted: " + decl, decl.has("statblock_multiattack_blocked"));
        assertEquals(3, decl.optInt("count"));

        // Rejection when rolls mismatch count
        JSONObject countMismatch = db.applyCompiledStatblockMultiattack(actor, "Adult Red Dragon", new JSONArray(),
            "ui-monster-multi-spell-count", selection);
        assertEquals("multiattack_count_mismatch", countMismatch.optString("statblock_multiattack_blocked"));
        unchanged();
    }
    @Test public void hastenStateSurvivesBackup21RestoreAndThreeSqliteReopens() throws Exception {
        bind("Clay Golem");
        JSONObject resources=new JSONObject(db.getCombatant(actor).getString("resources_json"));
        resources.put("hasten_used_this_turn",true);
        ContentValues changed=new ContentValues();changed.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",changed,
            "id=?",new String[]{String.valueOf(actor)}));
        JSONObject backup=db.exportAll();
        assertEquals(21,backup.getInt("version"));
        assertEquals("dnd",backup.getString("worldProfile"));
        JSONArray saved=backup.getJSONArray("combatants");
        boolean found=false;
        for(int i=0;i<saved.length();i++){
            JSONObject row=saved.getJSONObject(i);
            if(row.optLong("id")!=actor)continue;
            JSONObject stored=new JSONObject(row.getString("resources_json"));
            assertEquals("Clay Golem",stored.getString("block_id"));
            assertTrue(stored.getBoolean("hasten_used_this_turn"));
            found=true;break;
        }
        assertTrue("combatant resource must be in the sealed Backup21",found);
        resources.put("hasten_used_this_turn",false);
        changed.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",changed,
            "id=?",new String[]{String.valueOf(actor)}));
        db.importAll(backup);
        for(int reopen=0;reopen<3;reopen++){
            db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");
            String restoredJson=DatabaseUtils.stringForQuery(db.getReadableDatabase(),
                "SELECT resources_json FROM combatants WHERE world_profile=? AND name=? ORDER BY id DESC LIMIT 1",
                new String[]{"dnd","Ape"});
            JSONObject restored=new JSONObject(restoredJson);
            assertEquals("Clay Golem",restored.getString("block_id"));
            assertTrue("Backup21 must restore persisted Hasten after reopen "+reopen,
                restored.getBoolean("hasten_used_this_turn"));
            JSONObject triple=new JSONObject().put("variant_index",1)
                .put("hasten_used_this_turn",true)
                .put("selected_actions",new JSONArray().put("Slam").put("Slam").put("Slam"));
            JSONObject restoredPlan=XmmMultiattackHostPlan.declaration("Clay Golem",triple,restored);
            assertFalse(restoredPlan.toString(),restoredPlan.has("statblock_multiattack_blocked"));
            assertEquals(3,restoredPlan.getInt("count"));
            JSONObject uiOverride=new JSONObject(triple.toString()).put("hasten_used_this_turn",false);
            JSONObject denied=XmmMultiattackHostPlan.declaration("Clay Golem",uiOverride,restored);
            assertEquals("multiattack_context_not_authorized_by_host",
                denied.optString("statblock_multiattack_blocked"));
            long restoredActor=DatabaseUtils.longForQuery(db.getReadableDatabase(),
                "SELECT id FROM combatants WHERE world_profile=? AND name=? ORDER BY id DESC LIMIT 1",
                new String[]{"dnd","Ape"});
            assertTrue(restoredActor>0);
            JSONObject rejected=db.applyCompiledStatblockMultiattack(restoredActor,"Clay Golem",
                new JSONArray(),"ui-monster-multi-backup21-denied-"+reopen,uiOverride);
            assertEquals("multiattack_context_not_authorized_by_host",
                rejected.optString("statblock_multiattack_blocked"));
            assertEquals(0,DatabaseUtils.longForQuery(db.getReadableDatabase(),
                "SELECT COUNT(*) FROM effect_receipts",null));
        }
    }
}
