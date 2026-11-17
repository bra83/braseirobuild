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
import java.util.ArrayList;
import static org.junit.Assert.*;

/**
 * Regression: offline button must not require the Master preparation pool, RAG,
 * Gemini, network, or a new story. Use a real, reopened SQLite campaign.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class IndependentOfflineRecoveryRobolectricTest {
    private Context context;
    private String filename;
    private AppDb db;
    @Before public void start(){
        context=RuntimeEnvironment.getApplication();
        filename="offline-recovery-"+Long.toHexString(System.nanoTime())+".db";
        db=new AppDb(context,filename);
        db.setActiveMapProfile("dnd");
    }
    @After public void close(){
        if(db!=null)db.close();
        context.deleteDatabase(filename);
    }
    private void persistedBiblioplexScene() throws Exception {
        JSONObject scene=db.getSceneState();
        scene.put("location","Biblioplex");
        scene.put("day_number",1);
        scene.put("clock_hour",10.2667);
        scene.put("objective","acompanhar os acontecimentos na biblioteca");
        scene.put("present_npcs_json","[]");
        db.saveSceneState(scene);
        db.saveStoryState(true,"Urzmaktok Grojsh prende a estaca final junto às raízes do arbusto.",
            "","[]","","olhar a planta");
    }
    @Test public void recoversReopenedSavedSceneWithoutRemotePreparationOrMutation() throws Exception {
        persistedBiblioplexScene();
        String story=db.getStoryState().optString("last_narration");
        String location=db.getSceneState().optString("location");
        db.close();db=new AppDb(context,filename);db.setActiveMapProfile("dnd");
        MasterOrchestrator.Plan plan=OfflineMasterRecoveryContext.fromSaved(
            db,"Observar a ressonância das folhas","PLAYER_ACTION");
        assertTrue(plan.contextReady);
        JSONObject snapshot=new JSONObject(plan.compactContext);
        assertEquals(location,snapshot.getJSONObject("scene").getString("location"));
        assertEquals(story,snapshot.getJSONObject("story").getString("last_narration"));
        assertTrue(snapshot.getJSONObject("offline_recovery").getBoolean("external_ai_skipped"));
        assertTrue(snapshot.getJSONObject("offline_recovery").getBoolean("rule_search_skipped"));
        JSONObject answer=new OfflineMasterEngine().generate("Narrador",
            "Observar a ressonância das folhas",plan,new ArrayList<JSONObject>());
        assertTrue(answer.optString("narration").contains("Biblioplex"));
        assertEquals(0,answer.optJSONArray("state_updates").length());
        assertEquals(0,answer.optJSONArray("quest_updates").length());
        assertEquals("none",answer.optJSONObject("combat").optString("action"));
        assertEquals(story,db.getStoryState().optString("last_narration"));
        assertEquals(location,db.getSceneState().optString("location"));
    }
    @Test public void independentRecoveryPoolCannotInheritMasterPoolSaturation(){
        assertNotSame(AppExecutors.masterPreparation(),AppExecutors.offlineRecovery());
        assertNotSame(AppExecutors.io(),AppExecutors.offlineRecovery());
        assertFalse(AppExecutors.offlineRecovery().isShutdown());
    }
    @Test public void incompleteLocationFailsClosedAndDoesNotInventPlace() throws Exception {
        JSONObject scene=new JSONObject().put("location","");
        JSONObject story=new JSONObject().put("started",1).put("last_narration","Cena existente");
        try{
            OfflineMasterRecoveryContext.fromSnapshot(scene,story,new JSONObject(),
                "Continuar","PLAYER_ACTION");
            fail("Must reject missing scene position");
        }catch(IllegalArgumentException expected){
            assertTrue(expected.getMessage().contains("Local da cena"));
        }
    }
    @Test public void pureRecoveryContextNeverMutatesPassedObjects() throws Exception {
        JSONObject scene=new JSONObject().put("location","Biblioplex");
        JSONObject story=new JSONObject().put("last_narration","Cena salva");
        JSONObject bus=new JSONObject().put("active_heroes_full",new JSONArray());
        String originalScene=scene.toString(),originalStory=story.toString(),originalBus=bus.toString();
        MasterOrchestrator.Plan plan=OfflineMasterRecoveryContext.fromSnapshot(scene,story,bus,
            "Continuar","PLAYER_ACTION");
        assertEquals(originalScene,scene.toString());
        assertEquals(originalStory,story.toString());
        assertEquals(originalBus,bus.toString());
        assertEquals("PLAYER_ACTION",plan.inputChannel);
        assertEquals("continuation",plan.barbaraStoryOccasion);
    }
}
