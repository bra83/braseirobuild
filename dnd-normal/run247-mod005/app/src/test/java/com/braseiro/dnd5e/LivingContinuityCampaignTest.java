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

/** Cross-adventure world/hero/NPC continuity, using production SQLite tables. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class LivingContinuityCampaignTest {
    private Context context;
    private String file;
    private AppDb db;
    private long hero;
    private long alda;
    private long bruno;
    private long distant;

    @Before public void setup() throws Exception {
        context=RuntimeEnvironment.getApplication();
        file="living-continuity-"+Long.toHexString(System.nanoTime())+".db";
        db=new AppDb(context,file);db.setActiveMapProfile("dnd");
        hero=db.addCharacter("Kvothe","Humano","Mago",1,7,7,13,8,16,13,15,10,13,true,"");
        assertTrue(hero>0);
        alda=npc("Alda","Povoado A","conversar com os amigos sobre a jornada");
        bruno=npc("Bruno","Povoado A","descansar depois da viagem");
        distant=npc("Distant","Povoado B","comer");
    }
    @After public void cleanup(){
        if(db!=null)db.close();context.deleteDatabase(file);
    }
    private long npc(String name,String place,String agenda)throws Exception{
        db.upsertNpcMemoryChecked(new JSONObject().put("name",name)
            .put("summary","Pessoa do mundo vivo").put("active",1));
        JSONObject row=db.findNpcMemoryByName(name);long id=row.optLong("id");
        assertTrue(id>0);
        db.upsertNpcCognitiveStateChecked(new JSONObject()
            .put("name",name).put("current_location",place)
            .put("next_action",agenda).put("goals_json",new JSONArray().put("viver").toString()));
        return id;
    }
    private JSONObject event(String id,long witness) throws Exception {
        JSONObject deed=new JSONObject().put("canonical_fact",true).put("actor_character_id",hero)
            .put("event_key",id).put("title","Kvothe salvou a caravana")
            .put("summary","Uma pessoa foi resgatada em Povoado A.")
            .put("witness_npc_ids",new JSONArray().put(witness))
            .put("reputation_effect",new JSONObject().put("sentiment","admiration")
                .put("region","Povoado A").put("delta",14))
            .put("npc_attitude_effects",new JSONArray()
                .put(new JSONObject().put("npc_id",witness)
                     .put("sentiment","gratitude").put("delta",17)));
        return new JSONObject().put("journal_updates",new JSONArray().put(deed));
    }
    private JSONObject adventure(String id)throws Exception {
        return new JSONObject().put("schema","braseiro.adventure/v1")
            .put("id",id).put("system","dnd5e-2024")
            .put("worldId","world-test").put("timelineId","principal")
            .put("title","Aventura "+id)
            .put("start",new JSONObject().put("locationId","povoado"))
            .put("story",new JSONObject().put("premise","Uma nova aventura")
                .put("background","Passado conhecido somente pelo Mestre"))
            .put("locations",new JSONArray().put(new JSONObject()
                .put("id","povoado").put("name","Povoado A")));
    }
    private void reopen(){
        db.close();db=new AppDb(context,file);db.setActiveMapProfile("dnd");
    }

    @Test public void witnessRumorIsHearsayAfterLifeTickAndThreeReopenings()throws Exception{
        AdventureContinuityEngine engine=new AdventureContinuityEngine(db);
        assertEquals(1,engine.recordConfirmed("committed-story-a-10001",event("saved",alda)));
        assertEquals(1,engine.factsKnownByNpc(alda,10).length());
        assertEquals("witness",engine.factsKnownByNpc(alda,10)
            .getJSONObject(0).optString("npc_knowledge_kind"));
        assertEquals(0,engine.factsKnownByNpc(bruno,10).length());
        assertEquals(0,engine.factsKnownByNpc(distant,10).length());
        assertEquals(17,engine.attitudesKnownToNpc(alda,4).getJSONObject(0).optInt("score"));
        assertEquals(0,engine.attitudesKnownToNpc(bruno,4).length());

        assertEquals(1,engine.propagateScheduledContacts(5));
        assertEquals(0,engine.propagateScheduledContacts(5));
        assertEquals("hearsay",engine.factsKnownByNpc(bruno,10)
            .getJSONObject(0).optString("npc_knowledge_kind"));
        assertEquals(alda,engine.factsKnownByNpc(bruno,10)
            .getJSONObject(0).optLong("heard_from_npc_id"));
        assertEquals(0,engine.factsKnownByNpc(distant,10).length());
        assertEquals(0,engine.attitudesKnownToNpc(bruno,4).length());
        for(int i=0;i<3;i++){
            reopen();engine=new AdventureContinuityEngine(db);
            assertEquals("reopen "+i,1,engine.factsKnownByNpc(alda,10).length());
            assertEquals("reopen "+i,1,engine.factsKnownByNpc(bruno,10).length());
            assertEquals("reopen "+i,0,engine.factsKnownByNpc(distant,10).length());
            assertEquals(17,engine.attitudesKnownToNpc(alda,10).getJSONObject(0).optInt("score"));
            assertEquals(14,engine.regionalReputation(hero,10).getJSONObject(0).optInt("score"));
        }
    }

    @Test public void cannotCreateUnknownRumorsOrRecognitionOutsideWorld()throws Exception {
        AdventureContinuityEngine e=new AdventureContinuityEngine(db);
        assertEquals(1,e.recordConfirmed("committed-private-10002",new JSONObject()
            .put("journal_updates",new JSONArray().put(new JSONObject()
                .put("canonical_fact",true).put("actor_character_id",hero)
                .put("title","Achado oculto")))));
        assertEquals(0,e.factsKnownByNpc(alda,10).length());
        assertFalse(e.transmitFact("committed-private-10002:0",alda,bruno,1,"confirmed_contact"));
        assertFalse(e.transmitFact("invented",alda,bruno,1,"confirmed_contact"));
        db.setActiveMapProfile("alternate-world");
        assertEquals(0,new AdventureContinuityEngine(db).factsKnownByNpc(alda,10).length());
        db.setActiveMapProfile("dnd");
        assertEquals(1,db.queryOne("SELECT COUNT(*) n FROM braseiro_adventure_causal_facts "+
            "WHERE actor_character_id=?",new String[]{String.valueOf(hero)}).optInt("n"));
    }

    @Test public void twoImportedAdventuresKeepCanonicalWitnessMemory()throws Exception {
        BraseiroAdventureStore store=new BraseiroAdventureStore(db);
        store.importJson(adventure("first").toString());
        AdventureContinuityEngine engine=new AdventureContinuityEngine(db);
        assertEquals(1,engine.recordConfirmed("committed-a-10003",event("first-rescue",alda)));
        assertTrue(store.finish());
        store.importJson(adventure("second").toString());
        assertEquals(1,engine.factsKnownByNpc(alda,10).length());
        assertEquals("first",engine.factsKnownByNpc(alda,10)
            .getJSONObject(0).optString("adventure_id"));
        assertEquals(1,engine.recordConfirmed("committed-b-10004",event("second-rescue",alda)));
        assertEquals(2,engine.factsKnownByNpc(alda,10).length());
        reopen();
        assertEquals(2,new AdventureContinuityEngine(db).factsKnownByNpc(alda,10).length());
    }

    @Test public void backupIncludesHeardReportWitnessAndIndividualAttitude() throws Exception {
        AdventureContinuityEngine e=new AdventureContinuityEngine(db);
        assertEquals(1,e.recordConfirmed("committed-backup-10005",event("backup",alda)));
        assertTrue(e.transmitFact("committed-backup-10005:0",alda,bruno,8,"confirmed_contact"));
        JSONObject backup=new JSONObject();BraseiroAdventureStore store=new BraseiroAdventureStore(db);
        store.attachPortableBackup(backup);
        assertEquals(2,backup.getJSONArray("braseiroNpcKnowledge").length());
        assertEquals(1,backup.getJSONArray("braseiroNpcHeroAttitudes").length());
        db.getWritableDatabase().delete("braseiro_hero_npc_knowledge",null,null);
        db.getWritableDatabase().delete("braseiro_hero_npc_attitudes",null,null);
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){
            public void run()throws Exception{
                new BraseiroAdventureStore(db).restorePortableBackup(
                    db.getWritableDatabase(),backup,"dnd");
            }
        });
        assertEquals("hearsay",e.factsKnownByNpc(bruno,10)
            .getJSONObject(0).optString("npc_knowledge_kind"));
        assertEquals(17,e.attitudesKnownToNpc(alda,8).getJSONObject(0).optInt("score"));
    }

    @Test public void onlyHostConfirmedContactCanTransmitAndNoReputationInflation()throws Exception {
        AdventureContinuityEngine engine=new AdventureContinuityEngine(db);
        JSONObject deed=event("commit-contact",alda);
        assertEquals(1,engine.recordConfirmed("committed-contact-10006",deed));
        JSONObject fake=new JSONObject().put("npc_knowledge_transfers",new JSONArray()
            .put(new JSONObject().put("fact_key","committed-contact-10006:0")
                .put("source_npc_id",alda).put("recipient_npc_id",bruno)));
        assertEquals(0,engine.commitKnowledgeTransfers(fake));
        fake.getJSONArray("npc_knowledge_transfers").getJSONObject(0)
            .put("confirmed_contact",true);
        assertEquals(1,engine.commitKnowledgeTransfers(fake));
        assertEquals(0,engine.commitKnowledgeTransfers(fake));
        assertEquals(14,engine.regionalReputation(hero,10).getJSONObject(0).optInt("score"));
        assertEquals(1,engine.factsKnownByNpc(bruno,10).length());
    }
}
