package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class ManualAdjudicationEngineTest {
    private JSONObject proposal() throws Exception {
        return new JSONObject().put("situation", "A ponte se rompe.")
            .put("check", "Teste de perícia")
            .put("on_success", "Alcança o outro lado.")
            .put("on_failure", "Fica pendurado na viga.");
    }
    private JSONObject answer(String id,String outcome,String notes) throws Exception {
        return new JSONObject().put("request_id",id).put("outcome",outcome).put("notes",notes);
    }
    @Test public void successPersistsAcrossStateRoundTripWithoutChangingHpOrTime() throws Exception {
        AdventureState state = new AdventureState("ponte");
        JSONObject proposed = ManualAdjudicationEngine.propose(state, proposal());
        assertEquals("requires_manual_result", proposed.optString("status"));
        String id=proposed.getJSONObject("pending").getString("request_id");
        state=AdventureState.fromJson(new JSONObject(state.toJson().toString()));
        JSONObject done=ManualAdjudicationEngine.resolve(state,answer(id,"success","")
            .put("observed_facts",new JSONArray().put("A ponte está danificada.")));
        assertEquals("resolved_manual",done.getString("status"));
        assertFalse(done.getJSONObject("resolution").getBoolean("effects_applied"));
        assertTrue(state.getDiscoveredFacts().contains("A ponte está danificada."));
        assertEquals("ponte",state.getCurrentNodeId());
        state=AdventureState.fromJson(new JSONObject(state.toJson().toString()));
        assertEquals(0,state.getPendingManualAdjudication().length());
        assertEquals(1,state.getManualAdjudicationHistory().length());
        assertEquals("Alcança o outro lado.",state.getManualAdjudicationHistory().getJSONObject(0).getString("narrative_consequence"));
        assertEquals("already_resolved",ManualAdjudicationEngine.resolve(state,answer(id,"success","")).optString("status"));
        assertEquals(1,state.getManualAdjudicationHistory().length());
    }
    @Test public void failureAndMixedResultsRemainDistinct() throws Exception {
        AdventureState fail = new AdventureState("ponte");
        String id=ManualAdjudicationEngine.propose(fail,proposal()).getJSONObject("pending").getString("request_id");
        JSONObject result=ManualAdjudicationEngine.resolve(fail,answer(id,"failure",""));
        assertEquals("Fica pendurado na viga.", result.getJSONObject("resolution").getString("narrative_consequence"));
        AdventureState mixed=new AdventureState("ruinas");
        id=ManualAdjudicationEngine.propose(mixed,proposal()).getJSONObject("pending").getString("request_id");
        assertEquals("blocked",ManualAdjudicationEngine.resolve(mixed,answer(id,"mixed","")).getString("status"));
        assertEquals("resolved_manual",ManualAdjudicationEngine.resolve(mixed,answer(id,"mixed","Salta, mas perde um item sem especificá-lo.")).getString("status"));
    }
    @Test public void staleSceneWrongIdAndDuplicateReinterpretationAreRejected() throws Exception {
        AdventureState state=new AdventureState("rua");
        String id=ManualAdjudicationEngine.propose(state,proposal()).getJSONObject("pending").getString("request_id");
        assertEquals("blocked",ManualAdjudicationEngine.resolve(state,answer("outro","success","")).getString("status"));
        state.setCurrentNodeId("praça");
        assertEquals("blocked",ManualAdjudicationEngine.resolve(state,answer(id,"success","")).getString("status"));
        state.setCurrentNodeId("rua");
        assertEquals("resolved_manual",ManualAdjudicationEngine.resolve(state,answer(id,"success","")).getString("status"));
        assertEquals("blocked",ManualAdjudicationEngine.resolve(state,answer(id,"failure","")).getString("status"));
    }
    @Test public void neverAcceptsSilentMissingChoicesOrUnboundedFacts() throws Exception {
        AdventureState state=new AdventureState("ponte");
        assertEquals("blocked",ManualAdjudicationEngine.propose(state,new JSONObject()).getString("status"));
        String id=ManualAdjudicationEngine.propose(state,proposal()).getJSONObject("pending").getString("request_id");
        assertEquals("blocked",ManualAdjudicationEngine.propose(state,proposal()).getString("status"));
        assertEquals("blocked",ManualAdjudicationEngine.resolve(state,answer(id,"","")).getString("status"));
        JSONArray tooMany=new JSONArray();for(int i=0;i<21;i++)tooMany.put("fato-"+i);
        assertEquals("blocked",ManualAdjudicationEngine.resolve(state,answer(id,"success","").put("observed_facts",tooMany)).getString("status"));
        assertEquals(0,state.getManualAdjudicationHistory().length());
    }
}
