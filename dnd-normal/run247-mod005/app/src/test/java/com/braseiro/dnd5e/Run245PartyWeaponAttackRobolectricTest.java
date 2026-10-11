package com.braseiro.dnd5e;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Direct G02/G03 coverage for the normal equipped-weapon attack receipt path. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Run245PartyWeaponAttackRobolectricTest {
    private static final String WEAPON="run245-g03-longsword";

    private static final class Fixture implements AutoCloseable {
        final Context context=RuntimeEnvironment.getApplication();
        final String dbName="run245_party_attack_"+System.nanoTime()+".db";
        final AppDb db=new AppDb(context,dbName);
        final long character,actor,target,action;
        final JSONObject preview;
        Fixture()throws Exception{
            db.setActiveMapProfile("dnd");
            character=db.addCharacter("RUN245 Hero","Human","Fighter",1,20,20,16,
                16,12,14,10,10,10,true,"fixture");
            JSONObject weapon=new JSONObject().put("id",WEAPON).put("name","Longsword")
                .put("equipped",true).put("quantity",1).put("weapon_attack_bonus",5)
                .put("weapon_damage_formula","1d8+3").put("weapon_damage_type","cortante");
            JSONObject sheet=new JSONObject().put("species","Human").put("class_name","Fighter")
                .put("inventory_ledger",new JSONArray().put(weapon));
            db.updateCharacterSheet(character,sheet.toString());
            assertTrue(db.configureEquippedWeaponAttack(character,WEAPON,5,"1d8+3","cortante",
                "melee",5,0,0));
            actor=db.addCombatantWithVisual("RUN245 Hero",20,20,20,16,"",true,
                "character",character,"");
            target=db.addCombatantWithVisual("RUN245 Target",10,30,30,13,"",false,
                "statblock",0,"");
            action=db.addPartyAction(character,"RUN245 Hero","Atacar","combate");
            assertEquals(1,db.markPartyActionsNarrated(new JSONArray().put(action)));
            new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
            db.ensureTacticalTokensForCombat();
            place(actor,2.5/12d,2.5/8d);place(target,3.5/12d,2.5/8d);
            db.setCombatRunning(true);
            assertEquals(actor,db.nextTurn().optLong("id"));
            preview=db.previewTacticalWeaponAttack(actor,target,WEAPON);
            assertTrue(preview.toString(),preview.optBoolean("attack_geometry_valid"));
            assertEquals(13,preview.optInt("effective_ac"));
        }
        private void place(long id,double x,double y){
            ContentValues values=new ContentValues();values.put("x",x);values.put("y",y);
            assertEquals(1,db.getWritableDatabase().update("tactical_tokens",values,
                "world_profile=? AND combatant_id=?",new String[]{"dnd",String.valueOf(id)}));
        }
        JSONObject attack(String token){return db.applySingleWeaponAttackForPartyAction(action,
            actor,target,15,5,8,"cortante",token,WEAPON,"1d8+3",13,
            preview.optString("attack_geometry_fingerprint"),"Normal");}
        int targetHp(){return db.getCombatant(target).optInt("hp");}
        JSONObject actorResources()throws Exception{
            return new JSONObject(db.getCombatant(actor).optString("resources_json","{}"));
        }
        @Override public void close(){db.close();context.deleteDatabase(dbName);}
    }

    @Test public void exactEquippedProfileCommitsDamageAndCausalReceiptOnlyOnce()throws Exception{
        try(Fixture f=new Fixture()){
            int before=f.targetHp();
            JSONObject resolved=f.attack("ui-weapon2-run245-positive");
            assertTrue(resolved.toString(),resolved.optBoolean("party_action_resolved"));
            assertEquals(before-8,f.targetHp());
            assertEquals("resolved",f.db.listPartyActions("resolved",10).get(0).optString("status"));
            assertTrue(resolved.optString("party_action_receipt").startsWith("party-attack-v1:"));
            JSONObject replay=f.attack("ui-weapon2-run245-positive");
            assertTrue(replay.toString(),replay.has("party_attack_blocked"));
            assertEquals(before-8,f.targetHp());
        }
    }

    @Test public void legacyManualAndUnprovenWeaponProfilesAreRejectedWithoutMutation()throws Exception{
        try(Fixture f=new Fixture()){
            int before=f.targetHp();
            JSONObject legacy=f.db.applySingleWeaponAttackForPartyAction(f.action,f.actor,f.target,
                15,5,8,"cortante","ui-weapon2-run245-legacy");
            assertEquals("equipped_weapon_required",legacy.optString("party_attack_blocked"));
            JSONObject wrongBonus=f.db.applySingleWeaponAttackForPartyAction(f.action,f.actor,f.target,
                15,6,8,"cortante","ui-weapon2-run245-wrong-bonus",WEAPON,"1d8+3",13,
                f.preview.optString("attack_geometry_fingerprint"),"Normal");
            assertEquals("equipped_weapon_or_damage_invalid",wrongBonus.optString("party_attack_blocked"));
            JSONObject wrongFormula=f.db.applySingleWeaponAttackForPartyAction(f.action,f.actor,f.target,
                15,5,6,"cortante","ui-weapon2-run245-wrong-formula",WEAPON,"1d6+3",13,
                f.preview.optString("attack_geometry_fingerprint"),"Normal");
            assertEquals("equipped_weapon_or_damage_invalid",wrongFormula.optString("party_attack_blocked"));
            JSONObject wrongId=f.db.applySingleWeaponAttackForPartyAction(f.action,f.actor,f.target,
                15,5,8,"cortante","ui-weapon2-run245-wrong-id","unconfigured-item","1d8+3",13,
                f.preview.optString("attack_geometry_fingerprint"),"Normal");
            assertEquals("weapon_profile_missing",wrongId.optString("party_attack_blocked"));
            assertEquals(before,f.targetHp());
            assertTrue(f.actorResources().optBoolean("action_available",true));
            assertEquals(0,f.db.countPartyActions("resolved"));
        }
    }

    @Test public void sqliteFailureRollsBackDamageResourcesAndReceiptThenRetryCommits()throws Exception{
        try(Fixture f=new Fixture()){
            int before=f.targetHp();SQLiteDatabase sql=f.db.getWritableDatabase();
            sql.execSQL("CREATE TRIGGER run245_abort_attack BEFORE UPDATE OF status ON party_action_queue " +
                "WHEN NEW.status='resolved' BEGIN SELECT RAISE(ABORT,'injected resolution failure'); END");
            JSONObject failed=f.attack("ui-weapon2-run245-retry");
            assertTrue(failed.toString(),failed.has("operation_error"));
            assertEquals(before,f.targetHp());
            assertTrue(f.actorResources().optBoolean("action_available",true));
            assertEquals(0,f.db.countPartyActions("resolved"));
            assertEquals(0,f.db.listRecentPartyActionReceipts(10).size());
            sql.execSQL("DROP TRIGGER run245_abort_attack");
            JSONObject retry=f.attack("ui-weapon2-run245-retry");
            assertTrue(retry.toString(),retry.optBoolean("party_action_resolved"));
            assertEquals(before-8,f.targetHp());
        }
    }
}
