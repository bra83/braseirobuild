package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public final class Xmm2025ZeroHpConditionPreviewEngineTest {
    @BeforeClass public static void install() throws Exception { Xmm2025CompiledBestiaryEngine.install(); }

    @Test public void bothProfilesMatchTheirTypedSourceContract() throws Exception {
        assertEquals(2, Xmm2025ZeroHpConditionPreviewEngine.catalog().length());
        for (String[] row : new String[][]{{"Phase Spider","Bite","/monster/340/action/1/entries/0"},
                {"Vampire Familiar","Umbral Dagger","/monster/451/action/1/entries/0"}}) {
            JSONObject p=Xmm2025ZeroHpConditionPreviewEngine.profile(row[0],row[1]);
            assertTrue(row[0]+p,p.optBoolean("resolved"));assertEquals(row[2],p.optString("ptr"));
            assertEquals("poisoned, paralyzed",p.optString("conditions"));
            assertFalse(p.optBoolean("condition_applied",true));assertFalse(p.optBoolean("persisted",true));
        }
    }

    @Test public void proposesStablePoisonAndLinkedParalysisOnlyAtZeroHp() {
        JSONObject result=resolve("Phase Spider","Bite",true,true);
        assertTrue(result.toString(),result.optBoolean("resolved"));
        assertEquals("condition_proposal_pending_host_integration",result.optString("status"));
        JSONArray conditions=result.optJSONArray("proposed_conditions");
        assertEquals("poisoned",conditions.optString(0));assertEquals("paralyzed",conditions.optString(1));
        assertTrue(result.optBoolean("target_stable"));assertEquals(60,result.optInt("duration_minutes"));
        assertEquals("paralyzed while poisoned",result.optString("condition_link"));
        assertFalse(result.optBoolean("condition_applied"));assertFalse(result.optBoolean("persisted"));
    }

    @Test public void nonZeroHpHitProducesNoConditionProposal() {
        JSONObject result=resolve("Vampire Familiar","Umbral Dagger",true,false);
        assertTrue(result.optBoolean("resolved"));assertEquals("zero_hp_trigger_not_met",result.optString("status"));
        assertEquals(0,result.optJSONArray("proposed_conditions").length());
        assertFalse(result.optBoolean("target_stable"));
    }

    @Test public void invalidSourceOrContradictoryOutcomeFailsClosed() throws Exception {
        JSONObject request=request("Phase Spider","Bite",true,true);
        request.put("source_pointer","/monster/451/action/1/entries/0");
        assertEquals("source_pointer_mismatch",Xmm2025ZeroHpConditionPreviewEngine.resolve(request).optString("reason"));
        request=request("Phase Spider","Bite",false,true);
        assertEquals("zero_hp_without_hit_rejected",Xmm2025ZeroHpConditionPreviewEngine.resolve(request).optString("reason"));
    }

    @Test public void hybridRouteRequiresExactPointerAndReturnsReadOnlyProposal() throws Exception {
        JSONObject request=new JSONObject().put("request_id","phase-spider-zero-hp")
                .put("book_id","xmm").put("edition","2025").put("monster","Phase Spider")
                .put("source_pointer","/monster/340/action/1/entries/0")
                .put("operation","zero_hp_condition_resolve")
                .put("inputs",new JSONObject().put("action","Bite").put("attack_hit",true)
                        .put("target_reduced_to_zero_hp",true));
        JSONObject envelope=new JSONObject().put("origin","player_preview")
                .put("source_blob_sha1",Xmm2025SourceIndex.SOURCE_BLOB)
                .put("requests",new JSONArray().put(request));
        JSONObject out=new OfficialBookRuntimeGateway(null).execute("xmm","hybrid_batch_preview",envelope)
                .getJSONObject("result");
        assertEquals(1,out.optInt("resolved_count"));
        JSONObject proposal=out.getJSONArray("results").getJSONObject(0).getJSONObject("runtime_result");
        assertEquals("condition_proposal_pending_host_integration",proposal.optString("status"));
        assertFalse(proposal.optBoolean("condition_applied"));assertFalse(proposal.optBoolean("persisted"));
    }

    private static JSONObject resolve(String monster,String action,boolean hit,boolean zero) {
        return Xmm2025ZeroHpConditionPreviewEngine.resolve(request(monster,action,hit,zero));
    }
    private static JSONObject request(String monster,String action,boolean hit,boolean zero) {
        try {
            String pointer="Phase Spider".equals(monster)?"/monster/340/action/1/entries/0":"/monster/451/action/1/entries/0";
            return new JSONObject().put("monster",monster).put("action",action).put("source_pointer",pointer)
                    .put("source_blob_sha1",Xmm2025ZeroHpConditionPreviewEngine.SOURCE_BLOB)
                    .put("attack_hit",hit).put("target_reduced_to_zero_hp",zero);
        } catch(Exception impossible) { throw new AssertionError(impossible); }
    }
}
