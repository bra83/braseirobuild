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
import java.util.UUID;
import static org.junit.Assert.*;

/**
 * Functional subset of the post-Strixhaven sandbox.
 * Records what is actually persisted; narrative completion is NEVER
 * mistaken for verified payment, provisions, XP or hunt success.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class PostStrixhavenSandboxSubsystemRobolectricTest {
    private Context context;
    private AppDb db;
    private String dbName;

    @Before public void setUp() throws Exception {
        context=RuntimeEnvironment.getApplication();
        dbName="post_sandbox_"+UUID.randomUUID().toString().substring(0,8)+".db";
        db=new AppDb(context,dbName);
        db.setActiveMapProfile("dnd");
        JSONObject scene=db.getSceneState();
        scene.put("day_number",1).put("clock_hour",8d).put("time_of_day","Manhã")
             .put("location","Yartar").put("weather","Céu limpo");
        db.saveSceneState(scene);
    }
    @After public void tearDown(){
        if(db!=null)db.close();
        if(context!=null&&dbName!=null)context.deleteDatabase(dbName);
    }

    @Test public void guildContractBecomesQuestAndSurvivesDatabaseReopen() throws Exception {
        GuildEngine guilds=new GuildEngine(db);
        assertTrue("Sandbox should have plausible local guilds",guilds.seedSuggestedGuilds("fantasia")>=1);
        assertFalse("No guild was persisted",db.listGuilds(10).isEmpty());
        long guildId=db.listGuilds(10).get(0).optLong("id");
        assertTrue(guildId>0);
        JSONObject offer=new JSONObject().put("guild_id",guildId)
                .put("title","Localizar carga extraviada")
                .put("description","Recuperar a carga que desapareceu no porto de Yartar.")
                .put("issuer","Mestre da guilda").put("status","open")
                .put("reward_json",new JSONObject().put("gold_delta",12).toString())
                .put("requirements_json","{}").put("penalty_json","{}");
        long contractId=db.addGuildContractChecked(offer);
        assertTrue(contractId>0);
        assertEquals("open",db.getGuildContract(contractId).optString("status"));

        long questId=db.acceptGuildContractChecked(contractId,0);
        assertTrue("Acceptance did not create a quest",questId>0);
        assertEquals("accepted",db.getGuildContract(contractId).optString("status"));
        assertEquals(questId,db.getGuildContract(contractId).optLong("linked_quest_id"));

        db.completeGuildContractChecked(contractId);
        JSONObject complete=db.getGuildContract(contractId);
        assertEquals("completed",complete.optString("status"));
        assertTrue("Unverified reward must remain pending",
                new JSONObject(complete.optString("progress_json","{}"))
                       .optBoolean("reward_pending_review"));

        db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
        assertEquals("completed",db.getGuildContract(contractId).optString("status"));
        assertEquals(questId,db.getGuildContract(contractId).optLong("linked_quest_id"));
    }

    @Test public void jobAndHuntArePersistedButCannotInventRewards() throws Exception {
        LivingWorldEngine life=new LivingWorldEngine(db);
        long job=life.planActivity(0L,"trabalho","Trabalhar na estalagem","Yartar",1,"Empregador local");
        long hunt=life.planActivity(0L,"caça","Caçar alimento","Bosque próximo",1,"Procurar caça");
        assertTrue(job>0&&hunt>0);
        assertEquals("trabalho",life.activity(job).optString("activity_type"));
        assertEquals("caca",life.activity(hunt).optString("activity_type"));
        assertEquals("job",new JSONObject(life.activity(job).optString("procedure_json","{}"))
                .optString("activity_family"));

        assertEquals(8d,db.getSceneState().optDouble("clock_hour"),.001d);
        WorldStateEngine world=new WorldStateEngine(db);
        JSONObject task=world.preview("Trabalho na taverna");
        assertEquals("",task.optString("error"));
        assertEquals(4d,task.optDouble("hours_advanced"),.001d);
        assertTrue(world.commit(task));
        assertEquals(12d,db.getSceneState().optDouble("clock_hour"),.001d);

        JSONObject completedJob=life.completeNarrativeActivity(job,"Turno concluído; pagamento a acertar.");
        assertTrue(completedJob.optBoolean("completed"));
        assertFalse("Narration must not grant wages",completedJob.optBoolean("mechanics_applied"));
        JSONObject completedHunt=life.completeNarrativeActivity(hunt,"Rastros encontrados; presa não comprovada.");
        assertTrue(completedHunt.optBoolean("completed"));
        assertFalse("Narration must not create meat",completedHunt.optBoolean("mechanics_applied"));

        db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
        life=new LivingWorldEngine(db);
        assertEquals("concluida",life.activity(job).optString("status"));
        assertEquals("concluida",life.activity(hunt).optString("status"));
        assertEquals(12d,db.getSceneState().optDouble("clock_hour"),.001d);
    }

    @Test public void randomEncountersAreOptionalAndContextBound() throws Exception {
        JSONObject state=new JSONObject().put("turns",2).put("event_cooldown",0)
                .put("surprise_pressure",1d);
        int proposed=0,quiet=0;
        for(int i=0;i<50;i++){
            JSONObject event=SpontaneousEventEngine.candidate(state,"Yartar","Faerun",
                    "scene-"+i,false,false);
            if(event.length()==0){quiet++;continue;}
            proposed++;
            assertEquals("suggestion_only",event.optString("kind"));
            assertTrue(event.optBoolean("requires_canonical_guard"));
            assertTrue(event.optBoolean("requires_state_guard"));
            assertFalse("Event proposal cannot deal damage",event.has("damage"));
        }
        assertTrue("Expected some spontaneous events",proposed>0);
        assertTrue("Events must not be mandatory every scene",quiet>0);
        assertEquals(0,SpontaneousEventEngine.candidate(state,"Yartar","Faerun",
                "protected-scene",true,false).length());
        assertEquals(0,SpontaneousEventEngine.candidate(state,"Yartar","Faerun",
                "question",false,true).length());
        assertEquals(0,SpontaneousEventEngine.candidate(
                new JSONObject(state.toString()).put("event_cooldown",3),
                "Yartar","Faerun","cooldown",false,false).length());
    }

    @Test public void environmentalAndEmergentWorldProduceGroundedSnapshots() throws Exception {
        EnvironmentalDirector environmental=new EnvironmentalDirector(db);
        JSONObject weather=environmental.preview("Observo as nuvens sobre Yartar");
        assertEquals("Environment preview failure: "+weather.optString("error"),
                "",weather.optString("error"));
        assertNotNull("Missing weather system",weather.optJSONObject("weather_system"));
        assertTrue(weather.optJSONObject("weather_system").has("display_weather"));

        JSONObject world6=new WorldSixCoordinator(db).tick();
        assertEquals("World 6.0 failed: "+world6.optString("error"),"",world6.optString("error"));
        assertTrue("World history not available",world6.has("history"));
        assertTrue("Living settlements not available",world6.has("settlements"));
        assertTrue("Rumor system not available",world6.has("rumors"));
        assertTrue("Emergent campaign director not available",world6.has("director"));
    }
}