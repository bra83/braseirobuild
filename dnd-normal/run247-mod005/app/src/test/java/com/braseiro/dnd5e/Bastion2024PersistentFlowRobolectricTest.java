package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public final class Bastion2024PersistentFlowRobolectricTest {
    private static JSONArray d(int... faces) {JSONArray a=new JSONArray();for(int v:faces)a.put(v);return a;}
    private static String name() {return "bastion2024_"+Long.toHexString(System.nanoTime())+".db";}

    @Test public void bastionTreasuryDefendersAndClosedFacilityPersistAcrossThreeReopensAndBackup21() throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        String dbName=name();
        AppDb db=new AppDb(ctx,dbName);
        try {
            db.setActiveMapProfile("dnd");
            db.saveCampaign("DMG Bastion V1",DndRulesEngine.PROFILE_2024,
                "Sandbox",true,true,false,"");
            long hero=db.addCharacter("Arian","Humano","Guerreiro",5,44,44,17,
                16,13,15,10,12,8,true,"Soldado");
            JSONObject init=db.configureBastion2024(hero,1000,3,
                new JSONArray().put("smithy").put("library"),true);
            assertTrue(init.toString(),init.optBoolean("bastion_configured"));
            assertEquals(1000,db.getBastion2024State(hero).optInt("gold_gp"));
            JSONObject unconfirmed=db.commitBastion2024Event(hero,
                new JSONObject().put("d100_roll",64).put("payment_d6",5),false,
                "ui-bastion-test-not-approved");
            assertEquals("explicit_dm_confirmation_required",
                unconfirmed.optString("bastion_blocked"));
            assertEquals(1000,db.getBastion2024State(hero).optInt("gold_gp"));

            JSONObject visitors=new JSONObject().put("d100_roll",64).put("payment_d6",5);
            JSONObject applied=db.commitBastion2024Event(hero,visitors,true,"ui-bastion-visitors-001");
            assertTrue(applied.toString(),applied.optBoolean("bastion_event_committed"));
            assertEquals(1500,db.getBastion2024State(hero).optInt("gold_gp"));
            JSONObject replay=db.commitBastion2024Event(hero,visitors,true,"ui-bastion-visitors-001");
            assertEquals("already_resolved_or_replayed",replay.optString("bastion_blocked"));
            assertEquals(1500,db.getBastion2024State(hero).optInt("gold_gp"));
            JSONObject attack=db.commitBastion2024Event(hero,
                new JSONObject().put("d100_roll",51).put("defenders_before",3)
                    .put("attack_6d6",d(1,1,1,2,3,4))
                    .put("facility_id","smithy"),
                true,"ui-bastion-attack-001");
            assertTrue(attack.toString(),attack.optBoolean("bastion_event_committed"));
            assertEquals(0,db.getBastion2024State(hero).optInt("defenders"));
            assertEquals(1,db.getBastion2024State(hero).getJSONObject("facilities")
                .getJSONObject("smithy").getInt("unavailable_turn"));
            JSONObject turn=db.advanceBastion2024Turn(hero,true,"ui-bastion-turn-001");
            assertTrue(turn.toString(),turn.optBoolean("bastion_turn_committed"));
            assertEquals(1,db.getBastion2024State(hero).optInt("turn_number"));
            JSONObject backup=db.exportAll();
            assertEquals(21,backup.getInt("version"));
            assertEquals("",Backup21IntegrityEnvelope.check(backup));
            db.importAll(backup);
            db.importAll(backup);
            for(int k=0;k<3;k++){
                db.close();
                db=new AppDb(ctx,dbName);
                db.setActiveMapProfile("dnd");
                JSONObject h=db.findCharacterByName("Arian");
                assertTrue(h.optLong("id")>0);
                JSONObject state=db.getBastion2024State(h.optLong("id"));
                assertEquals(1500,state.optInt("gold_gp"));
                assertEquals(0,state.optInt("defenders"));
                assertEquals(1,state.optInt("turn_number"));
                assertEquals(3,state.getJSONArray("event_history").length());
                assertEquals(1,state.getJSONObject("facilities")
                    .getJSONObject("smithy").getInt("unavailable_turn"));
                assertEquals("",Backup21IntegrityEnvelope.check(db.exportAll()));
            }
        } finally {db.close();ctx.deleteDatabase(dbName);}
    }

    @Test public void declinedOrInvalidBastionActionsNeverSpendGoldOrOverwriteSheet() throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        String dbName=name();
        AppDb db=new AppDb(ctx,dbName);
        try {
            db.setActiveMapProfile("dnd");
            db.saveCampaign("DMG Bastion Constraints",DndRulesEngine.PROFILE_2024,
                "Sandbox",true,true,false,"");
            long hero=db.addCharacter("Nia","Elfa","Mago",5,28,28,13,
                9,15,13,17,11,10,true,"Sábio");
            JSONObject init=db.configureBastion2024(hero,100,2,
                new JSONArray().put("arcane_study"),true);
            assertTrue(init.toString(),init.optBoolean("bastion_configured"));
            JSONObject second=db.configureBastion2024(hero,100000,100,
                new JSONArray().put("smithy"),true);
            assertEquals("bastion_already_configured",second.optString("bastion_blocked"));
            JSONObject expense=db.commitBastion2024Event(hero,
                new JSONObject().put("d100_roll",59).put("accept_opportunity",true),
                true,"ui-bastion-cost-001");
            assertEquals("bastion_treasury_insufficient",expense.optString("bastion_blocked"));
            JSONObject invalid=db.commitBastion2024Event(hero,
                new JSONObject().put("d100_roll",51).put("defenders_before",2)
                    .put("attack_6d6",d(1,1,1,1,1,1))
                    .put("facility_id","UNKNOWN_FACILITY"),
                true,"ui-bastion-invalid-002");
            assertEquals("affected_facility_not_registered",invalid.optString("bastion_blocked"));
            assertEquals(100,db.getBastion2024State(hero).optInt("gold_gp"));
            assertEquals(2,db.getBastion2024State(hero).optInt("defenders"));
            assertEquals(0,db.getBastion2024State(hero).getJSONArray("event_history").length());
        } finally {db.close();ctx.deleteDatabase(dbName);}
    }

    @Test public void legacyHeroAndLevelFourCannotConfigureBastion() throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        String dbName=name();
        AppDb db=new AppDb(ctx,dbName);
        try {
            db.setActiveMapProfile("dnd");
            db.saveCampaign("Legacy Campaign",DndRulesEngine.PROFILE_2014,
                "Sandbox",true,true,false,"");
            long legacy=db.addCharacter("Rook","Humano","Guerreiro",5,30,30,14,
                16,13,15,10,12,8,true,"Soldado");
            JSONObject no=db.configureBastion2024(legacy,0,0,new JSONArray(),true);
            assertEquals("2024_core_rules_required",no.optString("bastion_blocked"));
            assertTrue(db.getBastion2024State(legacy).length()==0);
            db.saveCampaign("2024 Campaign",DndRulesEngine.PROFILE_2024,
                "Sandbox",true,true,false,"");
            long young=db.addCharacter("Young","Humano","Mago",4,20,20,12,
                8,15,10,17,10,12,true,"Sábio");
            JSONObject blocked=db.configureBastion2024(young,0,0,new JSONArray(),true);
            assertEquals("hero_level_five_required",blocked.optString("bastion_blocked"));
        } finally {db.close();ctx.deleteDatabase(dbName);}
    }
    @Test public void friendlyGuestCancelsOneAttackAndIsConsumedExactlyOnce() throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        String dbName=name();
        AppDb db=new AppDb(ctx,dbName);
        try {
            db.setActiveMapProfile("dnd");
            db.saveCampaign("DMG Friendly Guest",DndRulesEngine.PROFILE_2024,
                "Sandbox",true,true,false,"");
            long hero=db.addCharacter("Mira","Humano","Bardo",6,39,39,16,
                10,14,12,12,13,18,true,"Artista");
            JSONObject initial=db.configureBastion2024(hero,0,2,
                new JSONArray().put("inn"),true);
            assertTrue(initial.toString(),initial.optBoolean("bastion_configured"));
            JSONObject guest=db.commitBastion2024Event(hero,
                new JSONObject().put("d100_roll",73).put("guest_d4",4),
                true,"ui-bastion-guest-protection-001");
            assertTrue(guest.toString(),guest.optBoolean("bastion_event_committed"));
            assertEquals(1,db.getBastion2024State(hero).optInt("friendly_monster_protection"));
            JSONObject attack=db.commitBastion2024Event(hero,
                new JSONObject().put("d100_roll",51)
                    .put("friendly_monster_available",false),
                true,"ui-bastion-protected-attack-002");
            assertTrue(attack.toString(),attack.optBoolean("bastion_event_committed"));
            assertTrue(attack.getJSONObject("event_resolution")
                .optBoolean("friendly_monster_attack_prevented"));
            assertEquals(2,db.getBastion2024State(hero).optInt("defenders"));
            assertEquals(0,db.getBastion2024State(hero).optInt("friendly_monster_protection"));
            JSONObject second=db.commitBastion2024Event(hero,
                new JSONObject().put("d100_roll",51),true,"ui-bastion-attack-003");
            assertEquals("defenders_before_required",second.optString("bastion_blocked"));
            assertEquals(2,db.getBastion2024State(hero).optInt("defenders"));
            JSONObject backup=db.exportAll();db.importAll(backup);
            long restored=db.findCharacterByName("Mira").optLong("id");
            assertEquals(0,db.getBastion2024State(restored).optInt("friendly_monster_protection"));
        }finally{db.close();ctx.deleteDatabase(dbName);}
    }

}
