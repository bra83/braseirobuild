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

/** Real AppDb tests for BRELGA. Passing the baseline atomic tests does not
 * certify any of the 62 standalone families as integrated. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class XmmCombatAdmissionRobolectricTest {
    private static String databaseName(){return "xmm_admit_"+System.nanoTime()+".db";}
    private static void bind(AppDb db,long actor,String monster)throws Exception {
        JSONObject resources=new JSONObject(db.getCombatant(actor).getString("resources_json"));
        resources.put("block_id",monster);
        ContentValues values=new ContentValues();values.put("resources_json",resources.toString());
        assertEquals(1,db.getWritableDatabase().update("combatants",values,"id=?",
            new String[]{String.valueOf(actor)}));
    }
    private static long receipts(AppDb db){
        return DatabaseUtils.longForQuery(db.getReadableDatabase(),
            "SELECT count(*) FROM effect_receipts",null);
    }
    private static void position(AppDb db,long id,int x)throws Exception {
        ContentValues values=new ContentValues();values.put("x",(x+0.5)/12d);values.put("y",2.5/8d);
        assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
            "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
    }
    @Test public void everyFamilyBlocksWithoutDamageResourceTimerOrReceiptWrites()throws Exception {
        Context context=RuntimeEnvironment.getApplication();String name=databaseName();
        AppDb db=new AppDb(context,name);
        try {
            db.setActiveMapProfile("dnd");
            long actor=db.addCombatant("Source",20,100,100,12,"",false);
            long target=db.addCombatant("Target",10,100,100,12,"",true);
            db.setCombatRunning(true);assertEquals(actor,db.nextTurn().optLong("id"));
            for(XmmCombatSourceRegistry.Entry entry:XmmCombatSourceRegistry.entries().values()) {
                bind(db,actor,entry.monster);
                String beforeActor=db.getCombatant(actor).toString(),beforeTarget=db.getCombatant(target).toString();
                long beforeReceipts=receipts(db);
                for(int attempt=0;attempt<2;attempt++) {
                    JSONObject result=db.applyXmmSourceAction(new XmmCombatAdmission.Request(
                        entry.pointer,XmmCombatSourceRegistry.SOURCE_COMMIT,XmmCombatSourceRegistry.SOURCE_BLOB,
                        "ui-monster-family-admission-"+entry.lot,actor,target,20,0,1,
                        XmmCombatAdmission.Choice.GRAPPLE,new int[]{8,8,8,8}));
                    assertEquals(entry.pointer+": "+result,"NOT_INTEGRATED",result.optString("xmm_action_blocked"));
                    assertFalse(result.getBoolean("persisted_to_combat"));
                    assertEquals(entry.blocker,result.getString("blocker_reason"));
                    assertEquals(beforeActor,db.getCombatant(actor).toString());
                    assertEquals(beforeTarget,db.getCombatant(target).toString());
                    assertEquals(beforeReceipts,receipts(db));
                }
            }
        } finally {db.close();context.deleteDatabase(name);}
    }

    /** Inject failure at the FINAL receipt, after action, damage and timed rider.
     * Reuse the failed token, then prove durable replay across three reopenings
     * and a new token in a new round, using source-backed existing host actions.
     */
    @Test public void attackAndSaveFinalReceiptFailureRollBackWholeSqlTransaction()throws Exception {
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        Context context=RuntimeEnvironment.getApplication();
        for(boolean save:new boolean[]{false,true}) {
            String name=databaseName(),monster=save?"Bone Naga":"Bearded Devil";
            String action=save?"Serpentine Gaze":"Beard";
            String token=save?"ui-monster-save-admit-proof":"ui-monster-admit-proof";
            AppDb db=new AppDb(context,name);
            try {
                db.setActiveMapProfile("dnd");
                long actor=db.addCombatant("Atomic source",20,100,100,12,"",false);
                long target=db.addCombatant("Atomic target",10,200,200,12,"",true);
                bind(db,actor,monster);db.setCombatRunning(true);
                assertEquals(actor,db.nextTurn().optLong("id"));
                new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
                db.ensureTacticalTokensForCombat();position(db,actor,2);position(db,target,3);
                JSONArray rolls=new JSONArray();
                if(!save) {
                    JSONObject declared=null;
                    JSONArray actions=CompiledStatblockEngine.runtimeProfile(monster).getJSONArray("actions");
                    for(int i=0;i<actions.length();i++)if(action.equals(actions.getJSONObject(i).optString("name")))
                        declared=actions.getJSONObject(i);
                    assertNotNull(declared);
                    JSONArray specs=StatblockCombatFlowEngine.damageSpecs(declared.getString("damage"),
                        declared.getString("damage_type"),"");
                    for(int i=0;i<specs.length();i++) {
                        JSONObject spec=specs.getJSONObject(i);
                        CompiledMonsterDamageGuard.Range range=CompiledMonsterDamageGuard.range(
                            spec.getString("expression"),"",false);
                        assertNotNull(range);
                        rolls.put(new JSONObject().put("expression",spec.getString("expression"))
                            .put("damage_type",spec.getString("type")).put("damage_total",range.min));
                    }
                }
                String beforeActor=db.getCombatant(actor).toString(),beforeTarget=db.getCombatant(target).toString();
                long beforeReceipts=receipts(db);
                db.getWritableDatabase().execSQL("CREATE TRIGGER xmm_admit_fail BEFORE INSERT ON effect_receipts "
                    +"WHEN NEW.effect_key LIKE '%"+(save?"-save":"-statblock")+"' "
                    +"BEGIN SELECT RAISE(ABORT, 'xmm final receipt fault'); END");
                JSONObject failed=execute(db,save,actor,target,monster,action,rolls,token);
                assertEquals(failed.toString(),"transaction_rolled_back",failed.optString("operation_error"));
                assertEquals(beforeActor,db.getCombatant(actor).toString());
                assertEquals(beforeTarget,db.getCombatant(target).toString());
                assertEquals(beforeReceipts,receipts(db));
                db.getWritableDatabase().execSQL("DROP TRIGGER xmm_admit_fail");
                JSONObject success=execute(db,save,actor,target,monster,action,rolls,token);
                assertTrue(success.toString(),success.optBoolean(save?"statblock_save_committed":"statblock_attack_committed"));
                assertTrue(db.getCombatant(target).getInt("hp")<200);
                assertEquals(1,new JSONArray(db.getCombatant(target).getString("condition_timers_json")).length());
                int hp=db.getCombatant(target).getInt("hp");
                long committedReceipts=receipts(db);
                for(int reopen=0;reopen<3;reopen++) {
                    db.close();db=new AppDb(context,name);db.setActiveMapProfile("dnd");
                    JSONObject replay=execute(db,save,actor,target,monster,action,rolls,token);
                    assertEquals(replay.toString(),"already_resolved_or_replayed",
                        replay.optString(save?"statblock_save_blocked":"statblock_attack_blocked"));
                    assertEquals(hp,db.getCombatant(target).getInt("hp"));
                    assertEquals(committedReceipts,receipts(db));
                }
                for(int round=0;round<2;round++) {
                    assertEquals(target,db.nextTurn().optLong("id"));
                    assertEquals(actor,db.nextTurn().optLong("id"));
                    assertEquals(0,new JSONArray(db.getCombatant(target).getString("condition_timers_json")).length());
                    JSONObject next=execute(db,save,actor,target,monster,action,rolls,token+"-round-"+round);
                    assertTrue(next.toString(),next.optBoolean(save?"statblock_save_committed":"statblock_attack_committed"));
                    assertTrue(db.getCombatant(target).getInt("hp")<hp);
                    hp=db.getCombatant(target).getInt("hp");
                }
            } finally {db.close();context.deleteDatabase(name);}
        }
    }
    private static JSONObject execute(AppDb db,boolean save,long actor,long target,String monster,
            String action,JSONArray rolls,String token) {
        return save?db.applyCompiledStatblockSaveAction(actor,target,monster,action,1,6,token):
            db.applyCompiledStatblockAttack(actor,target,monster,action,18,0,rolls,0,0L,-1,-1,token);
    }
}
