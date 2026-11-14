package com.braseiro.dnd5e;

import android.content.Context;
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
 * Integrated, disk-backed sandbox acceptance gate.
 * Uses the REAL DB and world engines, not a mocked story state.
 * This test deliberately fails if world ticks cannot be persisted atomically.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public class PostStrixhavenWorldLifecycleRobolectricTest {
    private Context context;
    private AppDb db;
    private String dbName;

    @Before public void setUp() throws Exception {
        context=RuntimeEnvironment.getApplication();
        dbName="post_strixhaven_"+UUID.randomUUID().toString().substring(0,8)+".db";
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

    private JSONObject preview(String action) throws Exception {
        JSONObject result=new WorldStateEngine(db).preview(action);
        assertEquals("World tick preview failed: "+result.optString("error"),"",
                     result.optString("error"));
        assertTrue("No preview flag", result.optBoolean("preview_only"));
        return result;
    }

    private JSONObject apply(String action) throws Exception {
        JSONObject plan=preview(action);
        assertTrue("World commit failed: "+plan.optString("commit_error"),
                   new WorldStateEngine(db).commit(plan));
        assertTrue(plan.optBoolean("committed"));
        return plan;
    }

    @Test public void everydayActionsAdvanceCanonicalTimeAndPersistAcrossReopen() throws Exception {
        JSONObject request=preview("Procuro alguém que esteja contratando aventureiros");
        assertEquals(1d,request.optDouble("hours_advanced"),0.001d);
        assertEquals("procurar_trabalho",request.optString("activity_category"));
        // Planning is not a side effect.
        assertEquals(8d,db.getSceneState().optDouble("clock_hour"),0.001d);
        assertTrue(new WorldStateEngine(db).commit(request));
        assertEquals(9d,db.getSceneState().optDouble("clock_hour"),0.001d);
        assertFalse("Replay must be rejected",new WorldStateEngine(db).commit(request));

        JSONObject hunting=apply("Saio para caçar");
        assertEquals(2d,hunting.optDouble("hours_advanced"),0.001d);
        assertEquals(11d,db.getSceneState().optDouble("clock_hour"),0.001d);

        JSONObject observation=apply("Observo os arredores");
        assertEquals(.10d,observation.optDouble("hours_advanced"),.001d);
        assertEquals(11.10d,db.getSceneState().optDouble("clock_hour"),.001d);

        JSONObject discussion=apply("Converso com o taverneiro");
        assertEquals(.25d,discussion.optDouble("hours_advanced"),.001d);
        assertEquals(11.35d,db.getSceneState().optDouble("clock_hour"),.001d);

        db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
        assertEquals(1,db.getSceneState().optInt("day_number"));
        assertEquals(11.35d,db.getSceneState().optDouble("clock_hour"),.001d);
        assertEquals("Yartar",db.getSceneState().optString("location"));
    }

    @Test public void nightCrossingAndRestMinimumAreNotNarratedAsRecovery() throws Exception {
        JSONObject partial=apply("Durmo por 2 horas");
        assertFalse(partial.optBoolean("long_rest"));
        assertTrue(partial.optBoolean("rest_insufficient_duration"));
        assertEquals(10d,db.getSceneState().optDouble("clock_hour"),.001d);
        JSONObject night=apply("Passo 12 horas escrevendo relatórios");
        assertEquals(22d,db.getSceneState().optDouble("clock_hour"),.001d);
        JSONObject another=apply("Passo 4 horas estudando");
        assertEquals(2,db.getSceneState().optInt("day_number"));
        assertEquals(2d,db.getSceneState().optDouble("clock_hour"),.001d);
        assertTrue(another.optString("time_transition_notice").contains("Novo dia"));
    }

    @Test public void adviceIsNonAdvancingAndExplicitMinutesStayFractional() throws Exception {
        JSONObject question=preview("Posso caçar aqui?");
        assertEquals(0d,question.optDouble("hours_advanced"),.001d);
        JSONObject restQuestion=preview("Preciso dormir?");
        assertEquals(0d,restQuestion.optDouble("hours_advanced"),.001d);
        assertFalse(restQuestion.optBoolean("long_rest"));
        JSONObject durationQuestion=preview("Quanto tempo levaria dormir por 8 horas?");
        assertEquals(0d,durationQuestion.optDouble("hours_advanced"),.001d);
        assertFalse(durationQuestion.optBoolean("long_rest"));
        JSONObject timed=apply("Faço anotações durante 15 minutos");
        assertEquals(.25d,timed.optDouble("hours_advanced"),.001d);
        assertEquals(8.25d,db.getSceneState().optDouble("clock_hour"),.001d);
    }

    @Test public void narrativeCannotFastForwardDayOrOverwriteClock() throws Exception {
        JSONObject ai=new JSONObject().put("scene_state",new JSONObject()
                .put("day_number",99).put("clock_hour",23d)
                .put("time_of_day","Noite").put("location","Yartar"));
        new SceneStateEngine(db).apply(ai,
                "Ao amanhecer do dia seguinte todos se reúnem na praça.",new JSONObject());
        JSONObject actual=db.getSceneState();
        assertEquals(1,actual.optInt("day_number"));
        assertEquals(8d,actual.optDouble("clock_hour"),.001d);
        assertEquals("Manhã",actual.optString("time_of_day"));
    }

    @Test public void confirmedMapTravelAdvancesLivingWorldWithoutChargingResourcesTwice() throws Exception {
        WorldStateEngine engine=new WorldStateEngine(db);
        JSONObject confirmed=engine.previewConfirmedMapElapsed(1.75d);
        assertEquals("",confirmed.optString("error"));
        assertTrue(confirmed.optBoolean("external_travel_resources_handled"));
        assertEquals("confirmed_map_segment",confirmed.optString("duration_source"));
        assertEquals(1.75d,confirmed.optDouble("hours_advanced"),.001d);
        assertFalse("Map travel must not consume the same provisions a second time",
                confirmed.optJSONObject("company_resources_consumed").optBoolean("mechanics_apply"));
        assertEquals(8d,db.getSceneState().optDouble("clock_hour"),.001d);
        assertTrue("Canonical map tick failed: "+confirmed.optString("commit_error"),engine.commit(confirmed));
        assertEquals(9.75d,db.getSceneState().optDouble("clock_hour"),.001d);
        assertFalse("Map travel preview was replayed",engine.commit(confirmed));
        JSONObject invalid=engine.previewConfirmedMapElapsed(Double.NaN);
        assertTrue(invalid.optString("error").length()>0);
    }

    @Test public void mapTravelPersistsDayNightWeatherAndDirectorAcrossReopen() throws Exception {
        JSONObject initialEnvironment=db.getEnvironmentalDirectorState();
        JSONObject first=new ConfirmedMapTravelWorldGateway(db).commit(13.5d,"Bosque ao norte de Yartar");
        assertTrue("Map world/weather gateway failed: "+first.optString("error"),first.optBoolean("committed"));
        assertEquals(1,db.getSceneState().optInt("day_number"));
        assertEquals(21.5d,db.getSceneState().optDouble("clock_hour"),.001d);
        assertNotNull(first.optJSONObject("weather_system"));
        JSONObject persisted=db.getEnvironmentalDirectorState();
        assertTrue("Environmental director has no persisted state",persisted.length()>0);
        assertTrue("Environment should record the current campaign day",persisted.optInt("last_day",0)>=1);
        assertTrue("No last-hour synchronization recorded",
                persisted.has("last_clock_hour"));
        JSONObject second=new ConfirmedMapTravelWorldGateway(db).commit(8d,"Bosque profundo");
        assertTrue("Cross-midnight gateway failed: "+second.optString("error"),second.optBoolean("committed"));
        assertEquals(2,db.getSceneState().optInt("day_number"));
        assertEquals(5.5d,db.getSceneState().optDouble("clock_hour"),.001d);
        assertEquals(2,db.getEnvironmentalDirectorState().optInt("last_day",0));
        String savedWeather=db.getSceneState().optString("weather");
        db.close();db=new AppDb(context,dbName);db.setActiveMapProfile("dnd");
        assertEquals(2,db.getSceneState().optInt("day_number"));
        assertEquals(5.5d,db.getSceneState().optDouble("clock_hour"),.001d);
        assertEquals(savedWeather,db.getSceneState().optString("weather"));
        assertEquals(2,db.getEnvironmentalDirectorState().optInt("last_day",0));
    }

    @Test public void twoCompetingPreviewsCannotBothCommit() throws Exception {
        WorldStateEngine engine=new WorldStateEngine(db);
        JSONObject first=engine.preview("Investigo os arredores da taverna");
        JSONObject second=engine.preview("Procuro um contrato");
        assertTrue("First time advance failed: "+first.optString("commit_error"),engine.commit(first));
        assertFalse("A stale world tick was accepted",engine.commit(second));
        assertEquals(9d,db.getSceneState().optDouble("clock_hour"),.001d);
    }
}