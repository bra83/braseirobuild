package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import android.database.DatabaseUtils;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/**
 * V1 release safety: undoing an earlier XMM manual ruling must not erase
 * subsequent target resource changes, even after Backup21 and SQLite reopen.
 * Isolated E2E; no changes to the Antigravity-owned production classes.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public final class V1ManualRevertResourceIsolationRobolectricTest {
    private Context context;
    private AppDb db;
    private String dbName;
    private long actor, target;
    private String receipt;

    @Before public void setUp() throws Exception {
        Xmm2025CompiledBestiaryEngine.install();
        context=RuntimeEnvironment.getApplication();
        dbName="v1res_"+Long.toHexString(System.nanoTime() & 0xFFFFFL)+".db";
        db=new AppDb(context,dbName);
        db.setActiveMapProfile("dnd");
        actor=db.addCombatant("Mind Flayer",20,80,80,15,"",false);
        target=db.addCombatant("Rosa",10,40,40,16,"",true);
        JSONObject resources=new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
        resources.put("block_id","Mind Flayer").put("action_available",true);
        writeResources(actor,resources);
        db.setCombatRunning(true);
        assertEquals(actor,db.nextTurn().optLong("id"));
        JSONObject applied=db.applyXmmManualArbitration(actor,target,"Mind Flayer",
            "Multiattack","action",10,"psychic","stunned","",true,
            "Explicit player-confirmed manual arbitration","ui-monster-resource-001");
        assertTrue(applied.toString(),applied.optBoolean("manual_arbitration_applied"));
        receipt=applied.getString("receipt_key");
        assertEquals(30,db.getCombatant(target).optInt("hp"));
    }

    private void writeResources(long id,JSONObject resources) {
        ContentValues values=new ContentValues();
        values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,
            "id=? AND world_profile=?",new String[]{String.valueOf(id),"dnd"}));
    }

    private void changeResourcesAfterArbitration() throws Exception {
        JSONObject resources=new JSONObject(db.getCombatant(target).getString("resources_json"));
        resources.put("v1_post_arbitration_resource","MUST_SURVIVE_UNDO");
        writeResources(target,resources);
    }

    private void assertSafeRefusal(String token) throws Exception {
        JSONObject undone=db.revertXmmManualArbitration(actor,receipt,token);
        assertFalse("stale undo must not commit: "+undone,
            undone.optBoolean("manual_arbitration_reverted",false));
        assertEquals("target_state_changed_since_arbitration",
            undone.optString("manual_arbitration_revert_blocked"));
        assertEquals(30,db.getCombatant(target).optInt("hp"));
        assertTrue(db.getCombatant(target).optString("conditions","").contains("stunned"));
        JSONObject resources=new JSONObject(db.getCombatant(target).getString("resources_json"));
        assertEquals("MUST_SURVIVE_UNDO",resources.optString("v1_post_arbitration_resource"));
        assertFalse(new JSONObject(db.getCombatant(actor).getString("resources_json"))
            .optBoolean("action_available",true));
    }

    @Test public void newerTargetResourcesCannotBeErasedByStaleUndo() throws Exception {
        changeResourcesAfterArbitration();
        long receipts=DatabaseUtils.longForQuery(db.getReadableDatabase(),
            "SELECT COUNT(*) FROM effect_receipts WHERE world_profile=?",new String[]{"dnd"});
        assertSafeRefusal("ui-monster-resource-002");
        assertEquals("rejected undo must not create a receipt",receipts,
            DatabaseUtils.longForQuery(db.getReadableDatabase(),
                "SELECT COUNT(*) FROM effect_receipts WHERE world_profile=?",new String[]{"dnd"}));
    }

    @Test public void backup21AndThreeSqliteReopensMustPreserveNewerResources() throws Exception {
        changeResourcesAfterArbitration();
        JSONObject backup=db.exportAll();
        assertEquals(21,backup.getInt("version"));
        assertEquals("",Backup21IntegrityEnvelope.check(backup));
        db.importAll(backup);
        for(int i=0;i<3;i++){
            db.close();
            db=new AppDb(context,dbName);
            db.setActiveMapProfile("dnd");
            long restoredActor=DatabaseUtils.longForQuery(db.getReadableDatabase(),
                "SELECT id FROM combatants WHERE world_profile=? AND name=? ORDER BY id DESC LIMIT 1",
                new String[]{"dnd","Mind Flayer"});
            long restoredTarget=DatabaseUtils.longForQuery(db.getReadableDatabase(),
                "SELECT id FROM combatants WHERE world_profile=? AND name=? ORDER BY id DESC LIMIT 1",
                new String[]{"dnd","Rosa"});
            assertTrue(restoredActor>0);
            assertTrue(restoredTarget>0);
            actor=restoredActor;target=restoredTarget;
            assertSafeRefusal("ui-monster-resource-reopen-"+i);
        }
    }

    @After public void tearDown() {
        if(db!=null)db.close();
        if(context!=null&&dbName!=null)context.deleteDatabase(dbName);
    }
}
