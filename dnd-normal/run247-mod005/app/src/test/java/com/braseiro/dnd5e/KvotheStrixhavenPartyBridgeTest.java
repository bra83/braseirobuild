package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import static org.junit.Assert.*;

/** RPG end-to-end SQLite: campus NPC consent, return to Faerun, D040 player party. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public final class KvotheStrixhavenPartyBridgeTest {
    private AppDb db;private Context ctx;private String filename;
    private long kvothe,wilem,simmon,auri;
    private KvotheCampaignPartyBridge bridge;

    @Before public void setup() throws Exception {
        ctx=RuntimeEnvironment.getApplication();
        filename="kvothe-strixhaven-"+Long.toHexString(System.nanoTime())+".db";
        db=new AppDb(ctx,filename);db.setActiveMapProfile("dnd");
        kvothe=hero("Kvothe","Humano");
        bridge=new KvotheCampaignPartyBridge(db);
    }
    @After public void close() {
        if(db!=null)db.close();
        if(ctx!=null)ctx.deleteDatabase(filename);
    }
    private long hero(String name,String species){
        long id=db.addCharacter(name,species,"Mago",1,7,7,13,8,16,13,15,10,13,true,"");
        assertTrue(id>0);
        return id;
    }
    private void scene(final String location,final String hex,final boolean strix)throws Exception{
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){
            public void run() throws Exception {
                JSONObject s=db.getSceneState();
                s.put("location",location).put("hex_id",hex);
                if(strix)s.put("adventure_profile","strixhaven_curriculum");
                else {s.remove("adventure_profile");s.remove("strixhaven_map_id");s.remove("strixhaven_poi_id");}
                s.put("present_npcs_json","[]");
                db.saveSceneStateChecked(s);
            }
        });
    }
    private void threeHeroes(){
        wilem=hero("Wilem","Anão");
        simmon=hero("Simmon","Pequenino");
        auri=hero("Auri","Elfo");
    }
    private void npc(String name,String location)throws Exception{
        db.upsertNpcMemoryChecked(new JSONObject().put("name",name)
            .put("summary","Estudante de Strixhaven").put("last_seen",location)
            .put("active",1));
        db.upsertNpcCognitiveStateChecked(new JSONObject()
            .put("name",name).put("current_location",location)
            .put("next_action","Estudar na biblioteca").put("goals_json","[]"));
    }

    @Test public void campusClassmateNeedsInvitationConsentAndPhysicalPresence()throws Exception{
        scene("Strixhaven • Campus Central","",true);
        npc("Nalia","Strixhaven • Campus Central");
        assertEquals("invitation_or_npc_consent_missing",
            bridge.inviteCampusNpc("Nalia",true,false).optString("reason"));
        assertEquals(0,db.listPartyMemberships("active",20).size());
        assertTrue(bridge.inviteCampusNpc("Nalia",true,true).optBoolean("joined"));
        assertEquals(1,db.listPartyMemberships("active",20).stream()
            .filter(x->"npc".equals(x.optString("actor_kind"))).count());
        assertEquals(1,new PartyDirector(db).activeHeroRoster().size());
        assertEquals("Kvothe",new PartyDirector(db).activeHeroRoster().get(0).optString("name"));
        assertEquals("manual",CompanionAgencyEngine.mode(
            db.getCharacterControl(kvothe).optString("control_mode","manual")));
    }

    @Test public void companionsCannotTeleportFromOtherCampusLocation()throws Exception {
        scene("Strixhaven • Campus Central","",true);
        npc("Alda","Strixhaven • Biblioplex");
        assertEquals("npc_not_present_no_teleport",
            bridge.inviteCampusNpc("Alda",true,true).optString("reason"));
        assertEquals(0,db.findActivePartyNpc("Alda").length());
    }

    @Test public void meetingAtD040IsOptionalAndImportsNoInventedSheets()throws Exception{
        scene("Marco do Percurso","D040",false);
        assertEquals("missing_matching_hero_sheet",
            bridge.recruitAtMarco(1,2,3,true,true).optString("reason"));
        threeHeroes();
        assertEquals("meeting_and_consent_not_confirmed",
            bridge.recruitAtMarco(wilem,simmon,auri,false,true).optString("reason"));
        assertEquals("meeting_and_consent_not_confirmed",
            bridge.recruitAtMarco(wilem,simmon,auri,true,false).optString("reason"));
        assertEquals(1,new PartyDirector(db).activeHeroRoster().size());
        db.saveCharacterControl(wilem,"companion","{}");
        JSONObject accepted=bridge.recruitAtMarco(wilem,simmon,auri,true,true);
        assertTrue(accepted.toString(),accepted.optBoolean("joined"));
        assertEquals("manual",db.getCharacterControl(wilem).optString("control_mode"));
        assertEquals(4,new PartyDirector(db).activeHeroRoster().size());
        assertEquals(1,db.getCharacter(wilem).optInt("level"));
        assertEquals(1,db.getCharacter(simmon).optInt("level"));
        assertEquals(1,db.getCharacter(auri).optInt("level"));
        assertEquals(0,new PartyDirector(db).availableHeroesToRecruit().length());
    }

    @Test public void recruitmentRejectsLevelMismatchWithoutSilentlyChangingSheets()throws Exception{
        scene("Marco do Percurso","D040",false);
        wilem=db.addCharacter("Wilem","Anão","Bárbaro",2,14,14,14,
            16,10,12,8,11,10,true,"");
        simmon=hero("Simmon","Golias");
        auri=hero("Auri","Elfo");
        JSONObject rejected=bridge.recruitAtMarco(wilem,simmon,auri,true,true);
        assertFalse(rejected.optBoolean("joined"));
        assertEquals("companion_level_mismatch",rejected.optString("reason"));
        assertEquals("Wilem",rejected.optString("required_character"));
        assertEquals(1,rejected.optInt("required_level"));
        assertEquals(2,rejected.optInt("actual_level"));
        assertEquals(2,db.getCharacter(wilem).optInt("level"));
        assertEquals(1,new PartyDirector(db).activeHeroRoster().size());
    }

    @Test public void meetingCannotHappenAtWrongHexOrBeforeCampusExit() throws Exception {
        threeHeroes();
        scene("Marco do Percurso","D041",false);
        assertEquals("not_at_marco_d040",
            bridge.recruitAtMarco(wilem,simmon,auri,true,true).optString("reason"));
        scene("Strixhaven • Campus Central","D040",true);
        assertEquals("academic_prologue_not_exited",
            bridge.recruitAtMarco(wilem,simmon,auri,true,true).optString("reason"));
        assertEquals(1,new PartyDirector(db).activeHeroRoster().size());
    }

    @Test public void restartAndSeparationPreserveIndividualControl()throws Exception{
        threeHeroes();
        scene("Marco do Percurso","D040",false);
        assertTrue(bridge.recruitAtMarco(wilem,simmon,auri,true,true).optBoolean("joined"));
        assertTrue(new PartyDirector(db).releaseHero(auri,"Vila vizinha","Visitar amigos"));
        assertEquals(3,new PartyDirector(db).activeHeroRoster().size());
        db.close();db=new AppDb(ctx,filename);db.setActiveMapProfile("dnd");
        assertEquals(3,new PartyDirector(db).activeHeroRoster().size());
        assertEquals("manual",db.getCharacterControl(wilem).optString("control_mode"));
        assertTrue(new PartyDirector(db).reactivateHero(auri));
        assertEquals(4,new PartyDirector(db).activeHeroRoster().size());
    }

    @Test public void encountersNeedActualPartyReviewAndStrixhavenSceneEndsCleanly()throws Exception{
        JSONObject guard=KvotheCampaignPartyBridge.encounterPolicy(1,0,false);
        assertTrue(guard.optBoolean("combat_encounter_requires_review"));
        assertFalse(guard.optBoolean("unmodified_official_encounter_authorized"));
        assertFalse(KvotheCampaignPartyBridge.encounterPolicy(1,2,false)
            .optBoolean("solo"));
        assertFalse(KvotheCampaignPartyBridge.encounterPolicy(1,0,true)
            .optBoolean("combat_encounter_requires_review"));
        JSONObject campus=new JSONObject().put("location","Strixhaven • Universidade")
            .put("adventure_profile","strixhaven_curriculum")
            .put("strixhaven_map_id","central");
        JSONObject returned=StrixhavenSceneBoundary.returnToFaerun(
            null,campus,"Marco do Percurso","D040");
        assertFalse(KvotheCampaignPartyBridge.inCampus(returned));
        assertTrue(KvotheCampaignPartyBridge.atMarco(returned,null));
    }
}
