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

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class PostStrixhavenActivitiesRobolectricTest {
    private Context context;
    private AppDb db;
    private PostStrixhavenActivitiesEngine engine;
    private String file;
    private long hero;

    @Before public void setup() throws Exception {
        context=RuntimeEnvironment.getApplication();
        file="openworld_receipts_"+UUID.randomUUID().toString().substring(0,8)+".db";
        db=new AppDb(context,file);
        db.setActiveMapProfile("dnd");
        JSONObject scene=db.getSceneState();
        scene.put("day_number",1).put("clock_hour",8d)
                .put("time_of_day","Manhã").put("location","Yartar")
                .put("weather","Céu limpo");
        db.saveSceneState(scene);
        hero=db.addCharacter("Explorador","Humano","Patrulheiro",1,11,11,14,
                10,14,12,10,12,10,true,"Herói ativo de teste");
        assertTrue(hero>0);
        engine=new PostStrixhavenActivitiesEngine(db);
    }
    @After public void cleanup(){
        if(db!=null)db.close();
        if(file!=null&&context!=null)context.deleteDatabase(file);
    }
    private JSONObject supplies() {
        String raw=db.getPartyState().optString("company_resources_json","{}");
        try{return new JSONObject(raw);}catch(Exception ignored){return new JSONObject();}
    }
    @Test public void forageSuccessAndPartialPartyDaysPersistExactlyOnce() throws Exception {
        JSONObject planned=engine.planForage(hero,"Floresta",true);
        assertTrue(planned.toString(),planned.optBoolean("ok"));
        assertEquals(10,planned.optInt("dc"));
        long id=planned.optLong("activity_id");
        assertFalse("Repeat forage same day should be blocked",
                engine.planForage(hero,"Floresta",true).optBoolean("ok"));
        JSONObject result=engine.resolveForage(id,19,6,5);
        assertTrue(result.toString(),result.optBoolean("ok"));
        JSONObject receipt=result.optJSONObject("receipt");
        assertTrue(receipt.optBoolean("success"));
        assertEquals(7,receipt.optInt("food_lb"));
        assertEquals(6,receipt.optInt("water_gal"));
        assertEquals(7,supplies().optInt("provisions_days"));
        assertEquals(6,supplies().optInt("water_days"));
        assertEquals(10d,db.getSceneState().optDouble("clock_hour"),.001d);
        JSONObject replay=engine.resolveForage(id,20,6,6);
        assertTrue(replay.optBoolean("idempotent"));
        assertEquals(7,supplies().optInt("provisions_days"));
        db.close();db=new AppDb(context,file);db.setActiveMapProfile("dnd");
        engine=new PostStrixhavenActivitiesEngine(db);
        assertEquals("completed",engine.get(id).optString("status"));
        assertEquals(7,supplies().optInt("provisions_days"));
        assertEquals(6,supplies().optInt("water_days"));
        assertEquals(10d,db.getSceneState().optDouble("clock_hour"),.001d);
    }
    @Test public void failedForageStillSpendsTimeAndNeverCreatesSupplies() throws Exception {
        JSONObject plan=engine.planForage(hero,"Montanha",false);
        assertTrue(plan.toString(),plan.optBoolean("ok"));
        assertEquals(20,plan.optInt("dc"));
        JSONObject result=engine.resolveForage(plan.optLong("activity_id"),1,6,6);
        assertTrue(result.toString(),result.optBoolean("ok"));
        assertFalse(result.optJSONObject("receipt").optBoolean("success"));
        assertEquals(0,supplies().optInt("provisions_days"));
        assertEquals(0,supplies().optInt("water_days"));
        assertEquals(10d,db.getSceneState().optDouble("clock_hour"),.001d);
    }
    @Test public void jobRequiresAgreementAndPaysOnlyOnce() throws Exception {
        JSONObject offer=engine.offerJob(hero,"Estalajadeiro contratado",3,4d);
        assertTrue(offer.toString(),offer.optBoolean("ok"));
        long id=offer.optLong("activity_id");
        assertFalse(engine.finishJob(id).optBoolean("ok"));
        assertTrue(engine.acceptJob(id).optBoolean("ok"));
        JSONObject success=engine.finishJob(id);
        assertTrue(success.toString(),success.optBoolean("ok"));
        assertEquals(3,success.optJSONObject("receipt").optInt("gold_earned"));
        assertEquals(3,new JSONObject(db.getCharacter(hero).optString("sheet_json","{}")).optInt("gold"));
        assertEquals(12d,db.getSceneState().optDouble("clock_hour"),.001d);
        JSONObject replay=engine.finishJob(id);
        assertTrue(replay.optBoolean("idempotent"));
        assertEquals(3,new JSONObject(db.getCharacter(hero).optString("sheet_json","{}")).optInt("gold"));
        db.close();db=new AppDb(context,file);db.setActiveMapProfile("dnd");
        engine=new PostStrixhavenActivitiesEngine(db);
        assertEquals("completed",engine.get(id).optString("status"));
        assertEquals(3,new JSONObject(db.getCharacter(hero).optString("sheet_json","{}")).optInt("gold"));
        assertEquals(12d,db.getSceneState().optDouble("clock_hour"),.001d);
    }
    @Test public void rejectedRollsAndCancelledJobsDoNotMutateWorld() throws Exception {
        JSONObject f=engine.planForage(hero,"Floresta",false);
        assertTrue(f.optBoolean("ok"));
        assertFalse(engine.resolveForage(f.optLong("activity_id"),0,6,6).optBoolean("ok"));
        assertEquals("planned",engine.get(f.optLong("activity_id")).optString("status"));
        assertTrue(engine.cancel(f.optLong("activity_id")).optBoolean("ok"));
        assertFalse(engine.resolveForage(f.optLong("activity_id"),20,6,6).optBoolean("ok"));
        JSONObject offer=engine.offerJob(hero,"Trabalho ocasional",8,8d);
        assertTrue(offer.optBoolean("ok"));
        assertTrue(engine.cancel(offer.optLong("activity_id")).optBoolean("ok"));
        assertFalse(engine.finishJob(offer.optLong("activity_id")).optBoolean("ok"));
        assertEquals(8d,db.getSceneState().optDouble("clock_hour"),.001d);
        assertEquals(0,new JSONObject(db.getCharacter(hero).optString("sheet_json","{}")).optInt("gold"));
    }
    @Test public void forageDcsUseReal2024TerrainAndUnknownNeverDefaults() {
        assertEquals(10,PostStrixhavenActivitiesEngine.foragingDc("Floresta"));
        assertEquals(10,PostStrixhavenActivitiesEngine.foragingDc("Pântano"));
        assertEquals(15,PostStrixhavenActivitiesEngine.foragingDc("Colina"));
        assertEquals(15,PostStrixhavenActivitiesEngine.foragingDc("Pradaria"));
        assertEquals(20,PostStrixhavenActivitiesEngine.foragingDc("Deserto"));
        assertEquals(20,PostStrixhavenActivitiesEngine.foragingDc("Montanha"));
        try{PostStrixhavenActivitiesEngine.foragingDc("local desconhecido");fail("unknown terrain defaulted");}
        catch(IllegalArgumentException expected){}
    }
}