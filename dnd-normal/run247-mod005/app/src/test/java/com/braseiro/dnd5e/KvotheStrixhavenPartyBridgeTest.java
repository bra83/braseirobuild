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

    @Test public void studentFriendsStayNpcBeforeD040AndBecomeIndependentHeroesAtMeeting()throws Exception{
        scene("Strixhaven • Campus Central","",true);
        JSONObject campus=bridge.context();
        assertTrue(campus.toString(),campus.optBoolean("enabled"));
        JSONArray friends=campus.getJSONArray("strixhaven_npc_friends");
        assertEquals(3,friends.length());
        for(String name:new String[]{"Wilem","Simmon","Auri"}){
            JSONObject npc=db.findNpcMemoryByName(name);
            assertEquals(1,npc.optInt("active",0));
            JSONObject saved=new JSONObject(npc.getString("stats_json"))
                .getJSONObject("strixhaven_2024_sheet");
            assertEquals(1,saved.optInt("level"));
            assertEquals("pre_marco_npc",new JSONObject(npc.getString("stats_json"))
                .optString("strixhaven_friend_status"));
        }
        assertEquals(1,new PartyDirector(db).activeHeroRoster().size());
        assertEquals("not_at_marco_d040",
            bridge.recruitAtMarco(true,true).optString("reason"));
        scene("Marco do Percurso","D040",false);
        JSONObject blocked=bridge.recruitAtMarco(true,true);
        assertEquals("student_npc_not_present",blocked.optString("reason"));
        assertEquals(1,new PartyDirector(db).activeHeroRoster().size());
        JSONObject arrived=db.getSceneState();
        arrived.put("present_npcs_json",new JSONArray()
            .put("Wilem").put("Simmon").put("Auri").toString());
        db.saveSceneState(arrived);
        assertEquals("meeting_and_consent_not_confirmed",
            bridge.recruitAtMarco(false,true).optString("reason"));
        JSONObject accepted=bridge.recruitAtMarco(true,true);
        assertTrue(accepted.toString(),accepted.optBoolean("joined"));
        assertTrue(accepted.optBoolean("independent_progression"));
        assertEquals(3,accepted.getJSONArray("hero_ids").length());
        assertEquals(4,new PartyDirector(db).activeHeroRoster().size());
        for(String name:new String[]{"Wilem","Simmon","Auri"}){
            JSONObject npc=db.findNpcMemoryByName(name);
            assertEquals(0,npc.optInt("active",1));
            JSONObject hero=null;
            for(JSONObject found:db.listActiveCharacters())
                if(name.equals(found.optString("name")))hero=found;
            assertNotNull(hero);
            assertEquals(1,hero.optInt("level"));
            assertEquals("manual",db.getCharacterControl(hero.optLong("id"))
                .optString("control_mode"));
            JSONObject sheet=new JSONObject(hero.getString("sheet_json"));
            assertEquals("independent_hero",
                sheet.getJSONObject("npc_lifecycle").getString("phase"));
        }
    }
    @Test public void friendsUseValid27PointBuyAnd2024StartingClasses()throws Exception{
        for(String name:new String[]{"Wilem","Simmon","Auri"}){
            JSONObject sheet=StrixhavenFriends2024.initialSheet(name);
            assertEquals("5e 2024",sheet.getString("rules_source"));
            assertEquals(27,sheet.getJSONObject("ability_purchase").getInt("cost"));
            assertEquals("",sheet.getString("subclass_name"));
            assertTrue(sheet.getJSONArray("skill_proficiencies").length()>=4);
        }
        JSONObject wilem=StrixhavenFriends2024.initialSheet("Wilem");
        assertEquals("Anão",wilem.getString("species"));
        assertEquals(15,wilem.getInt("max_hp")); // Dwarf 2024 +1 HP per level
        assertEquals("Bárbaro",wilem.getString("class_name"));
        JSONObject simmon=StrixhavenFriends2024.initialSheet("Simmon");
        assertEquals("Golias",simmon.getString("species"));
        assertEquals("Monge",simmon.getString("class_name"));
        assertEquals("Médio",simmon.getString("size"));
        JSONObject auri=StrixhavenFriends2024.initialSheet("Auri");
        assertEquals("Elfo",auri.getString("species"));
        assertEquals("Druida",auri.getString("class_name"));
        assertEquals(15,auri.getInt("ac")); // Leather 11+2 DEX+2 shield
        assertEquals(5,auri.getJSONArray("skill_proficiencies").length());
    }

    @Test public void tieflingKvotheRestoresHumanWithoutLosingPlayerChoices()throws Exception{
        JSONObject before=new JSONObject(db.getCharacter(kvothe).optString("sheet_json","{}"));
        before.put("rules_source","5e 2024").put("species","Tiferino")
            .put("personality_profile",new JSONObject().put("traits",new JSONArray()
                .put(new JSONObject().put("id","lealdade"))))
            .put("inventory_ledger",new JSONArray()
                .put(new JSONObject().put("id","alaude").put("quantity",1)));
        db.updateCharacterSheetChecked(kvothe,before.toString());
        android.content.ContentValues species=new android.content.ContentValues();
        species.put("species","Tiferino");
        db.getWritableDatabase().update("characters",species,"id=?",
            new String[]{String.valueOf(kvothe)});
        JSONObject repaired=KvotheCanonical2024.restoreIfNeeded(db);
        assertTrue(repaired.toString(),repaired.optBoolean("changed"));
        JSONObject hero=db.getCharacter(kvothe);
        assertEquals("Humano",hero.optString("species"));
        assertEquals(8,hero.optInt("str"));
        assertEquals(16,hero.optInt("dex"));
        assertEquals(13,hero.optInt("con"));
        assertEquals(15,hero.optInt("int_score"));
        assertEquals(10,hero.optInt("wis"));
        assertEquals(13,hero.optInt("cha"));
        JSONObject restored=new JSONObject(hero.optString("sheet_json","{}"));
        assertEquals("Humano",restored.optString("species"));
        assertEquals("Médio",restored.optString("size"));
        assertEquals(27,restored.getJSONObject("ability_purchase").getInt("cost"));
        assertEquals(5,restored.getJSONArray("skill_proficiencies").length());
        assertEquals(2,restored.getJSONArray("selected_origin_feats").length());
        assertEquals(1,restored.getJSONObject("personality_profile")
            .getJSONArray("traits").length());
        assertEquals("alaude",restored.getJSONArray("inventory_ledger")
            .getJSONObject(0).getString("id"));
        assertFalse(KvotheCanonical2024.restoreIfNeeded(db).optBoolean("changed"));
    }

    @Test public void strixhavenFriendsFollowLevelThreeBeforeJoiningButNotAfter()throws Exception{
        scene("Strixhaven • Campus Central","",true);
        bridge.context();
        // Explicit 2024 level increase in the protagonist's persisted campaign.
        android.content.ContentValues leveled=new android.content.ContentValues();
        leveled.put("level",3);
        db.getWritableDatabase().update("characters",leveled,"id=?",
            new String[]{String.valueOf(kvothe)});
        JSONObject sheet=new JSONObject(db.getCharacter(kvothe).getString("sheet_json"));
        sheet.put("level",3);
        db.updateCharacterSheetChecked(kvothe,sheet.toString());
        JSONObject context=bridge.context();
        JSONArray friends=context.getJSONArray("strixhaven_npc_friends");
        assertEquals(3,friends.length());
        for(int i=0;i<friends.length();i++){
            JSONObject summary=friends.getJSONObject(i);
            assertEquals(3,summary.optInt("level"));
            assertFalse(summary.optBoolean("requires_level_choices"));
            JSONObject student=db.findNpcMemoryByName(summary.getString("name"));
            JSONObject candidate=new JSONObject(student.getString("stats_json"))
                .getJSONObject("strixhaven_2024_sheet");
            assertEquals(3,candidate.optInt("level"));
            assertTrue(Core2024SubclassRuntimeEngine.isSubclassSelectionValid(
                candidate.optString("class_name"),3,candidate.optString("subclass_name")));
        }
        assertEquals(1,new PartyDirector(db).activeHeroRoster().size());
        scene("Marco do Percurso","D040",false);
        JSONObject atMarco=db.getSceneState();
        atMarco.put("present_npcs_json",new JSONArray().put("Wilem")
            .put("Simmon").put("Auri").toString());db.saveSceneState(atMarco);
        JSONObject joined=bridge.recruitAtMarco(true,true);
        assertTrue(joined.toString(),joined.optBoolean("joined"));
        long wilemId=joined.getJSONArray("hero_ids").getLong(0);
        int beforeLevel=db.getCharacter(wilemId).optInt("level");
        // A later gain for Kvothe never changes an independently recruited hero.
        leveled.put("level",4);
        db.getWritableDatabase().update("characters",leveled,"id=?",
            new String[]{String.valueOf(kvothe)});
        assertEquals(3,beforeLevel);
        assertEquals(3,db.getCharacter(wilemId).optInt("level"));
        assertEquals(4,new PartyDirector(db).activeHeroRoster().size());
    }

    @Test public void firstYearLevelFourNpcAsisAreLegalAndReunionMaintainsParity()throws Exception{
        scene("Strixhaven • Campus Central","",true);
        bridge.context();
        android.content.ContentValues leveled=new android.content.ContentValues();
        leveled.put("level",4);
        db.getWritableDatabase().update("characters",leveled,"id=?",
            new String[]{String.valueOf(kvothe)});
        JSONObject heroSheet=new JSONObject(db.getCharacter(kvothe).getString("sheet_json"));
        heroSheet.put("level",4);db.updateCharacterSheetChecked(kvothe,heroSheet.toString());
        JSONArray friends=bridge.context().getJSONArray("strixhaven_npc_friends");
        assertEquals(3,friends.length());
        for(int i=0;i<friends.length();i++){
            JSONObject state=friends.getJSONObject(i);
            assertEquals(4,state.optInt("level"));
            assertFalse("Preconfigured 2024 ASI should be resolved",state.optBoolean("requires_level_choices"));
            JSONObject memory=db.findNpcMemoryByName(state.optString("name"));
            JSONObject sheet=new JSONObject(memory.getString("stats_json"))
                .getJSONObject("strixhaven_2024_sheet");
            assertEquals(4,sheet.optInt("level"));
            assertEquals(1,sheet.getJSONArray("npc_level_decisions").length());
            assertEquals(2,sheet.getJSONArray("npc_level_decisions")
                .getJSONObject(0).getJSONArray("ability_increases").length());
        }
        assertEquals(19,new JSONObject(db.findNpcMemoryByName("Wilem")
            .getString("stats_json")).getJSONObject("strixhaven_2024_sheet").getInt("str"));
        assertEquals(19,new JSONObject(db.findNpcMemoryByName("Simmon")
            .getString("stats_json")).getJSONObject("strixhaven_2024_sheet").getInt("dex"));
        assertEquals(19,new JSONObject(db.findNpcMemoryByName("Auri")
            .getString("stats_json")).getJSONObject("strixhaven_2024_sheet").getInt("wis"));
        scene("Marco do Percurso","D040",false);
        JSONObject st=db.getSceneState();st.put("present_npcs_json",
            new JSONArray().put("Wilem").put("Simmon").put("Auri").toString());
        db.saveSceneState(st);
        JSONObject accepted=bridge.recruitAtMarco(true,true);
        assertTrue(accepted.toString(),accepted.optBoolean("joined"));
        for(int i=0;i<3;i++){
            long id=accepted.getJSONArray("hero_ids").getLong(i);
            assertEquals(4,db.getCharacter(id).optInt("level"));
        }
    }

    @Test public void personalDeedsAwardXpOnlyToThePromotedHeroAndCannotReplay()throws Exception{
        scene("Strixhaven • Campus Central","",true);
        bridge.context();
        scene("Marco do Percurso","D040",false);
        JSONObject scene=db.getSceneState();
        scene.put("present_npcs_json",new JSONArray().put("Wilem")
            .put("Simmon").put("Auri").toString());db.saveSceneState(scene);
        JSONObject joined=bridge.recruitAtMarco(true,true);
        assertTrue(joined.toString(),joined.optBoolean("joined"));
        JSONArray ids=joined.getJSONArray("hero_ids");
        long wilemId=ids.getLong(0),simmonId=ids.getLong(1),auriId=ids.getLong(2);
        String receipt="wilem-solo-deed-receipt-2024";
        JSONObject reward=new PartyDirector(db).awardKvotheFriendIndividualXp(
            wilemId,300,"Wilem salvou a caravana sozinho",receipt);
        assertEquals(300,reward.optInt("xp_after"));
        assertTrue(reward.optBoolean("level_up_pending"));
        assertEquals(1,db.getCharacter(wilemId).optInt("level"));
        assertEquals(300,new JSONObject(db.getCharacter(wilemId)
            .getString("sheet_json")).optInt("xp"));
        for(long id:new long[]{simmonId,auriId,kvothe})
            assertEquals(0,new JSONObject(db.getCharacter(id).getString("sheet_json"))
                .optInt("xp",0));
        try{
            new PartyDirector(db).awardKvotheFriendIndividualXp(
                wilemId,300,"Wilem salvou a caravana sozinho",receipt);
            fail("Duplicate feat cannot grant XP twice");
        }catch(IllegalStateException expected){}
        try{
            new PartyDirector(db).awardKvotheFriendIndividualXp(
                kvothe,300,"Kvothe salvou a caravana",receipt+"-kvothe");
            fail("Personal companion XP cannot apply to player hero Kvothe");
        }catch(IllegalStateException expected){}
        assertEquals(300,new JSONObject(db.getCharacter(wilemId)
            .getString("sheet_json")).optInt("xp"));
    }

    @Test public void npcStatsExposeReal2024LevelAndContextCannotHealAnInjury()throws Exception{
        scene("Strixhaven • Campus Central","",true);
        bridge.context();
        JSONObject npc=db.findNpcMemoryByName("Wilem");
        JSONObject stats=new JSONObject(npc.getString("stats_json"));
        assertEquals(1,stats.optInt("level"));
        assertEquals("Bárbaro",stats.optString("class_name"));
        assertEquals(17,stats.getJSONObject("ability_scores").optInt("str"));
        assertEquals(15,stats.optInt("max_hp"));
        stats.put("hp",4);
        JSONObject wounded=new JSONObject(npc.toString()).put("stats_json",stats.toString());
        db.upsertNpcMemory(wounded);
        bridge.context();
        JSONObject refreshed=new JSONObject(db.findNpcMemoryByName("Wilem")
            .getString("stats_json"));
        assertEquals(4,refreshed.optInt("hp"));
        assertEquals(1,refreshed.optInt("level"));
        assertEquals(15,refreshed.optInt("max_hp"));
        assertEquals(1,new PartyDirector(db).activeHeroRoster().size());
        // On a genuine advancement the additional hit points are earned,
        // but the eleven hit points of injury remain missing.
        StrixhavenFriends2024.syncBeforeMarco(db,4);
        JSONObject afterLevel=new JSONObject(db.findNpcMemoryByName("Wilem")
            .getString("stats_json"));
        assertEquals(4,afterLevel.optInt("level"));
        assertTrue(afterLevel.optInt("max_hp")>15);
        assertEquals(11,afterLevel.optInt("max_hp")-afterLevel.optInt("hp"));
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
