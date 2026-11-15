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
public class PostStrixhavenEncounterRobolectricTest {
    private AppDb db;
    private Context context;
    private String databaseName;
    private PostStrixhavenEncounterEngine encounters;

    @Before public void before() throws Exception {
        context=RuntimeEnvironment.getApplication();
        databaseName="sandbox_encounter_"+UUID.randomUUID().toString().substring(0,8)+".db";
        db=new AppDb(context,databaseName);
        db.setActiveMapProfile("dnd");
        JSONObject scene=db.getSceneState();
        scene.put("day_number",1).put("clock_hour",8d)
             .put("time_of_day","Manhã").put("location","Yartar");
        db.saveSceneState(scene);
        encounters=new PostStrixhavenEncounterEngine(db);
    }
    @After public void after(){
        if(db!=null)db.close();
        if(context!=null&&databaseName!=null)context.deleteDatabase(databaseName);
    }
    private JSONObject tick(int dayBefore,double hourBefore,int dayAfter,double hourAfter,double elapsed)
            throws Exception {
        return new JSONObject().put("day_before",dayBefore).put("clock_hour_before",hourBefore)
                .put("day_after",dayAfter).put("clock_hour_after",hourAfter)
                .put("hours_advanced",elapsed).put("living_world_enabled",true);
    }

    @Test public void timeOnlyCanProposeAndOffersRemainOptional() throws Exception {
        int original=encounters.recent(50).size();
        JSONObject zero=encounters.onWorldTick(tick(1,8d,1,8d,0d));
        assertFalse(zero.optBoolean("offered"));
        assertEquals(original,encounters.recent(50).size());
        int proposals=0;
        for(int i=0;i<200&&proposals==0;i++){
            // distinct absolute six-hour buckets, no random roll performed by Gemini
            int beforeDay=1+i/4;
            int afterDay=beforeDay;
            double hourBefore=(i%4)*6d, hourAfter=hourBefore+6d;
            if(hourAfter>=24d){afterDay++;hourAfter-=24d;}
            JSONObject report=encounters.onWorldTick(
                    tick(beforeDay,hourBefore,afterDay,hourAfter,6d));
            if(report.optBoolean("offered"))proposals++;
        }
        assertTrue("Local seeded encounter never appeared",proposals>0);
        JSONObject proposal=encounters.recent(1).get(0);
        assertEquals("offered",proposal.optString("status"));
        assertTrue(proposal.optString("description").length()>12);
        assertFalse("Encounter must not have automatic loot",proposal.has("reward_json"));
        long id=proposal.optLong("id");
        JSONObject ignore=encounters.choose(id,"ignore","");
        assertTrue(ignore.toString(),ignore.optBoolean("ok"));
        assertEquals("ignored",encounters.get(id).optString("status"));
        assertTrue(encounters.choose(id,"ignore","").optBoolean("idempotent"));

        db.close();db=new AppDb(context,databaseName);db.setActiveMapProfile("dnd");
        encounters=new PostStrixhavenEncounterEngine(db);
        assertEquals("ignored",encounters.get(id).optString("status"));
    }

    @Test public void choiceCreatesNoUnverifiedCombatOrItems() throws Exception {
        long id=0;
        for(int i=0;i<250&&id==0;i++){
            int d=1+i/4, next=d;
            double before=(i%4)*6d,after=before+6d;
            if(after>=24d){next++;after-=24d;}
            JSONObject offer=encounters.onWorldTick(tick(d,before,next,after,6d));
            if(offer.optBoolean("offered"))id=offer.optLong("encounter_id");
        }
        assertTrue(id>0);
        JSONObject engaged=encounters.choose(id,"investigate","O grupo aproxima-se com cautela.");
        assertTrue(engaged.toString(),engaged.optBoolean("ok"));
        assertEquals("engaged",encounters.get(id).optString("status"));
        assertEquals(0,db.getCombatState().optInt("running",0));
        assertFalse(encounters.choose(id,"resolve","").optBoolean("ok") &&
                db.getCombatState().optInt("running",0)>0);
        // No combat may ever be declared concluded by this encounters layer.
        JSONObject closed=encounters.get(id);
        if("resolved".equals(closed.optString("status"))){
            assertEquals(0,db.getCombatState().optInt("running",0));
            assertEquals(0,new JSONObject(closed.optString("outcome_json")).optInt("xp_delta"));
        }
        assertEquals(0,db.getCombatState().optInt("running",0));
    }

    @Test public void nativeCombatStartAndEndReconcileOnlyBoundEncounter() throws Exception {
        long id=0;
        for(int i=0;i<250&&id==0;i++){
            int beforeDay=1+i/4,afterDay=beforeDay;
            double beforeHour=(i%4)*6d,afterHour=beforeHour+6d;
            if(afterHour>=24d){afterDay++;afterHour-=24d;}
            JSONObject proposed=encounters.onWorldTick(
                    tick(beforeDay,beforeHour,afterDay,afterHour,6d));
            if(proposed.optBoolean("offered"))id=proposed.optLong("encounter_id");
        }
        assertTrue(id>0);
        JSONObject request=encounters.choose(id,"combat","Hostilidade será julgada pelo Mestre.");
        assertTrue(request.toString(),request.optBoolean("ok"));
        assertEquals("combat_pending",encounters.get(id).optString("status"));
        JSONObject unrelated=encounters.reconcileNativeCombat(
                new JSONObject().put("action","start"),"Outro combate em outra região");
        assertFalse("Unrelated combat linked to encounter",unrelated.optBoolean("reconciled"));
        db.setCombatRunningChecked(true);
        String nativeId=db.getCombatState().optString("encounter_id");
        assertTrue(nativeId.length()>10);
        JSONObject linked=encounters.reconcileNativeCombat(
                new JSONObject().put("action","start"),"ENCONTRO OPCIONAL #"+id+"; validar criatura.");
        assertTrue(linked.toString(),linked.optBoolean("reconciled"));
        assertEquals(nativeId,
                new JSONObject(encounters.get(id).optString("outcome_json")).optString("native_combat_id"));
        assertFalse("Active combat must never resolve encounter",
                encounters.reconcileNativeCombat(new JSONObject().put("action","end"),"")
                     .optBoolean("reconciled"));
        db.setCombatRunningChecked(false);
        JSONObject unrelatedEnd=encounters.reconcileNativeCombat(
                new JSONObject().put("action","end"),"","enc-unrelated-other-combat");
        assertFalse("Unrelated native battle falsely closed encounter",
                unrelatedEnd.optBoolean("reconciled"));
        assertEquals("combat_pending",encounters.get(id).optString("status"));
        JSONObject unverifiedEnd=encounters.reconcileNativeCombat(
                new JSONObject().put("action","end"),"");
        assertFalse("No host-confirmed ID should never close battle",
                unverifiedEnd.optBoolean("reconciled"));
        assertEquals("combat_pending",encounters.get(id).optString("status"));
        JSONObject closed=encounters.reconcileNativeCombat(
                new JSONObject().put("action","end"),"",nativeId);
        assertTrue(closed.toString(),closed.optBoolean("reconciled"));
        assertEquals("resolved",encounters.get(id).optString("status"));
        assertEquals(0,new JSONObject(encounters.get(id).optString("outcome_json")).optInt("xp_delta"));
        assertEquals(0,new JSONObject(encounters.get(id).optString("outcome_json")).optInt("gold_delta"));
        assertFalse("Combat completion was applied twice",
                encounters.reconcileNativeCombat(new JSONObject().put("action","end"),"",nativeId)
                    .optBoolean("reconciled"));
        db.close();db=new AppDb(context,databaseName);db.setActiveMapProfile("dnd");
        encounters=new PostStrixhavenEncounterEngine(db);
        assertEquals("resolved",encounters.get(id).optString("status"));
    }

    @Test public void worldTickIntegratesWithoutForcingEncounter() throws Exception {
        WorldStateEngine world=new WorldStateEngine(db);
        JSONObject plan=world.preview("Passo 6 horas estudando os arredores de Yartar");
        assertEquals("",plan.optString("error"));
        assertEquals(6d,plan.optDouble("hours_advanced"),.001d);
        assertTrue("World failed to commit: "+plan.optString("commit_error"),world.commit(plan));
        JSONObject encounter=plan.optJSONObject("spontaneous_encounter");
        assertNotNull("World time commit did not visit encounter gateway",encounter);
        assertEquals(14d,db.getSceneState().optDouble("clock_hour"),.001d);
        assertFalse("World tick replay accepted",world.commit(plan));
    }
}