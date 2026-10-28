package com.braseiro.dnd5e;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import static org.junit.Assert.*;

/** Real SQLite, two game sessions and explicit verified witness / reputation. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class AdventureContinuityPersistenceTest {
    private Context ctx;
    private AppDb db;
    private String filename;
    private long characterId;

    @Before public void start() throws Exception {
        ctx=ApplicationProvider.getApplicationContext();
        filename="kvothe-legacy-"+Long.toHexString(System.nanoTime())+".db";
        db=new AppDb(ctx,filename);
        db.setActiveMapProfile("dnd");
        characterId=db.addCharacter("Kvothe","Humano","Mago",1,7,7,13,8,16,13,15,10,13,true,"");
        assertTrue(db.getCharacter(characterId).length()>0);
    }
    @After public void stop() {
        if(db!=null)db.close();
        ctx.deleteDatabase(filename);
    }
    private JSONObject deed(long npcId,String sentiment) throws Exception {
        JSONObject event=new JSONObject()
            .put("canonical_fact",true)
            .put("actor_character_id",characterId)
            .put("event_key","rescued-traveler-001")
            .put("title","Resgatou um viajante")
            .put("summary","Kvothe socorreu uma pessoa na estrada.");
        if(npcId>0)event.put("witness_npc_ids",new JSONArray().put(npcId));
        if(!sentiment.isEmpty())event.put("reputation_effect",new JSONObject()
            .put("sentiment",sentiment).put("region","Estrada Velha").put("delta",12));
        return new JSONObject().put("journal_updates",new JSONArray().put(event));
    }

    @Test public void noImportedAdventureStillRecordsPrivateCommittedDeed() throws Exception {
        AdventureContinuityEngine engine=new AdventureContinuityEngine(db);
        assertEquals(1,engine.recordConfirmed("scene-commit-private-001",deed(0,"")));
        assertEquals(0,engine.factsKnownByNpc(4567,10).length());
        assertEquals(1,db.queryOne("SELECT COUNT(*) AS n FROM braseiro_adventure_causal_facts "+
             "WHERE actor_character_id=?",new String[]{String.valueOf(characterId)}).optInt("n"));
        assertEquals(0,engine.recordConfirmed("scene-commit-private-001",deed(0,"")));
        assertEquals(1,db.queryOne("SELECT COUNT(*) AS n FROM braseiro_adventure_causal_facts "+
             "WHERE actor_character_id=?",new String[]{String.valueOf(characterId)}).optInt("n"));
    }

    @Test public void knownWitnessRemembersAfterReopenAndNoOtherNpcDoes() throws Exception {
        JSONObject npc=new JSONObject().put("name","Testemunha da Estrada")
            .put("summary","Mercador local").put("active",1);
        db.upsertNpcMemoryChecked(npc);
        JSONArray npcs=db.listNpcMemories(30);
        long npcId=0;
        for(int i=0;i<npcs.length();i++){
            JSONObject n=npcs.optJSONObject(i);
            if(n!=null&&"Testemunha da Estrada".equals(n.optString("name")))
                npcId=n.optLong("id",0);
        }
        assertTrue("Persistent NPC ID not created",npcId>0);
        AdventureContinuityEngine engine=new AdventureContinuityEngine(db);
        assertEquals(1,engine.recordConfirmed("scene-commit-witness-001",deed(npcId,"admiration")));
        assertEquals(1,engine.factsKnownByNpc(npcId,10).length());
        assertEquals(0,engine.factsKnownByNpc(npcId+999,10).length());
        assertEquals(12,engine.regionalReputation(characterId,5).getJSONObject(0).optInt("score"));
        assertEquals(0,engine.recordConfirmed("scene-commit-witness-001",deed(npcId,"admiration")));
        assertEquals(12,engine.regionalReputation(characterId,5).getJSONObject(0).optInt("score"));

        db.close();
        db=new AppDb(ctx,filename);
        db.setActiveMapProfile("dnd");
        engine=new AdventureContinuityEngine(db);
        assertEquals(1,engine.factsKnownByNpc(npcId,10).length());
        assertEquals(0,engine.factsKnownByNpc(npcId+999,10).length());
        assertEquals(12,engine.regionalReputation(characterId,5).getJSONObject(0).optInt("score"));
        assertTrue(engine.factsKnownByNpc(npcId,10).getJSONObject(0)
            .optString("adventure_id").startsWith("freeform:"));
    }

    @Test public void proposedOrUnwitnessedRumorCannotCreateReputation() throws Exception {
        AdventureContinuityEngine engine=new AdventureContinuityEngine(db);
        JSONObject uncommitted=deed(0,"hostility");
        uncommitted.getJSONArray("journal_updates").getJSONObject(0)
            .put("canonical_fact",false);
        assertEquals(0,engine.recordConfirmed("scene-commit-rejected-001",uncommitted));
        assertEquals(1,engine.recordConfirmed("scene-commit-private-002",deed(0,"hostility")));
        assertEquals(0,engine.regionalReputation(characterId,5).length());
    }

    @Test public void backupRoundTripPreservesReputationRecords() throws Exception {
        JSONObject npc=new JSONObject().put("name","Mercador")
            .put("summary","Viu um resgate").put("active",1);
        db.upsertNpcMemoryChecked(npc);
        JSONArray npcs=db.listNpcMemories(50);long npcId=0;
        for(int i=0;i<npcs.length();i++){
            JSONObject n=npcs.optJSONObject(i);
            if(n!=null&&"Mercador".equals(n.optString("name")))npcId=n.optLong("id");
        }
        assertTrue(npcId>0);
        AdventureContinuityEngine engine=new AdventureContinuityEngine(db);
        engine.recordConfirmed("scene-commit-backup-001",deed(npcId,"trust"));
        JSONObject backup=new JSONObject();
        new BraseiroAdventureStore(db).attachPortableBackup(backup);
        assertEquals(1,backup.getJSONArray("braseiroHeroRegionalReputation").length());
        db.getWritableDatabase().delete("braseiro_hero_regional_reputation",null,null);
        db.runInTransactionChecked(new AppDb.CheckedTransaction(){
            public void run() throws Exception {
                new BraseiroAdventureStore(db).restorePortableBackup(db.getWritableDatabase(),backup,"dnd");
            }
        });
        assertEquals(12,engine.regionalReputation(characterId,5).getJSONObject(0).optInt("score"));
    }
}
