package com.braseiro.dnd5e;

import static org.junit.Assert.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class Xmm2025SaveConditionPreviewEngineTest {
    @BeforeClass public static void install(){
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
    }

    @Test public void allFiftyEightProfilesRemainSourceBoundAndManual()throws Exception{
        JSONArray rows=Xmm2025SaveConditionPreviewEngine.catalog();
        assertEquals(58,rows.length());
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.getJSONObject(i);
            JSONObject profile=Xmm2025SaveConditionPreviewEngine.profile(
                row.getString("id"),row.getString("action"));
            assertTrue(row.toString(),profile.optBoolean("resolved"));
            assertEquals(row.getString("source_pointer"),profile.optString("source_pointer"));
            assertEquals(Xmm2025SaveConditionPreviewEngine.SOURCE_BLOB,
                profile.optString("source_blob_sha1"));
            JSONObject stat=CompiledStatblockEngine.get(row.getString("id"));
            JSONArray actions="action".equals(row.optString("category"))?
                stat.optJSONArray("actions"):stat.optJSONArray("special_actions");
            JSONObject compiled=null;
            for(int j=0;actions!=null&&j<actions.length();j++){
                JSONObject candidate=actions.optJSONObject(j);
                if(candidate!=null&&row.getString("action").equals(candidate.optString("name"))
                    &&row.getString("category").equals(candidate.optString("category")))
                    compiled=candidate;
            }
            assertNotNull(row.toString(),compiled);
            assertTrue(row.toString(),compiled.optBoolean("source_bound"));
            assertTrue(row.toString(),compiled.optBoolean("manual_unmodeled_source_effect_required"));
            assertFalse(profile.optBoolean("condition_applied",true));
            assertFalse(profile.optBoolean("auto_mutation_allowed",true));
        }
    }

    @Test public void explicitFailedSaveProposesConditionsWithoutLifecycleOrMutation()throws Exception{
        String monster="Aboleth",action="Consume Memories",pointer="/monster/3/action/2/entries/0";
        JSONObject out=Xmm2025SaveConditionPreviewEngine.resolve(request(monster,action,pointer)
            .put("save_succeeded",false).put("target_eligibility_confirmed",true)
            .put("prerequisite_confirmed",true));
        assertTrue(out.toString(),out.optBoolean("resolved"));
        assertEquals("condition_proposal_pending_lifecycle_and_host_integration",
            out.optString("status"));
        assertEquals("charmed",out.getJSONArray("proposed_conditions").getString(0));
        assertEquals("grappled",out.getJSONArray("proposed_conditions").getString(1));
        assertTrue(out.optBoolean("condition_lifecycle_manual"));
        assertTrue(out.optBoolean("additional_source_effects_manual"));
        assertFalse(out.optBoolean("condition_applied",true));
        assertFalse(out.optBoolean("persisted",true));
        assertFalse(out.optBoolean("auto_mutation_allowed",true));
    }

    @Test public void saveSuccessOrFailedPrerequisiteProposesNoCondition()throws Exception{
        String m="Aboleth",a="Consume Memories",p="/monster/3/action/2/entries/0";
        JSONObject success=Xmm2025SaveConditionPreviewEngine.resolve(request(m,a,p)
            .put("save_succeeded",true).put("target_eligibility_confirmed",true)
            .put("prerequisite_confirmed",true));
        assertTrue(success.optBoolean("resolved"));
        assertEquals(0,success.getJSONArray("proposed_conditions").length());
        JSONObject ineligible=Xmm2025SaveConditionPreviewEngine.resolve(request(m,a,p)
            .put("save_succeeded",false).put("target_eligibility_confirmed",false)
            .put("prerequisite_confirmed",true));
        assertTrue(ineligible.optBoolean("resolved"));
        assertEquals(0,ineligible.getJSONArray("proposed_conditions").length());
    }

    @Test public void incompleteSourceAndInvalidInputsFailClosed()throws Exception{
        JSONObject noDc=Xmm2025SaveConditionPreviewEngine.resolve(request("Aboleth",
            "Psychic Drain","/monster/3/legendary/1/entries/0")
            .put("save_succeeded",false).put("target_eligibility_confirmed",true)
            .put("prerequisite_confirmed",true));
        assertEquals("source_save_contract_incomplete",noDc.optString("reason"));
        JSONObject unknown=Xmm2025SaveConditionPreviewEngine.resolve(request("Aboleth",
            "Consume Memories","/monster/3/action/2/entries/0")
            .put("save_succeeded",false).put("target_eligibility_confirmed",true)
            .put("prerequisite_confirmed",true).put("set_hp",0));
        assertEquals("unrecognized_save_condition_input",unknown.optString("reason"));
        JSONObject noResult=Xmm2025SaveConditionPreviewEngine.resolve(request("Aboleth",
            "Consume Memories","/monster/3/action/2/entries/0")
            .put("target_eligibility_confirmed",true).put("prerequisite_confirmed",true));
        assertEquals("explicit_save_result_required",noResult.optString("reason"));
    }

    @Test public void hybridBatchRoutesExactSaveConditionPointers()throws Exception{
        JSONObject request=new JSONObject().put("request_id","aboleth-condition")
            .put("book_id","xmm").put("edition","2025").put("monster","Aboleth")
            .put("source_pointer","/monster/3/action/2/entries/0")
            .put("operation","save_condition_resolve").put("inputs",new JSONObject()
                .put("action","Consume Memories").put("save_succeeded",false)
                .put("target_eligibility_confirmed",true).put("prerequisite_confirmed",true));
        JSONObject packet=new JSONObject().put("origin","player_preview")
            .put("source_blob_sha1",Xmm2025SourceIndex.SOURCE_BLOB)
            .put("requests",new JSONArray().put(request));
        JSONObject result=new OfficialBookRuntimeGateway(null).execute("xmm",
            "hybrid_batch_preview",packet).getJSONObject("result");
        assertEquals(1,result.optInt("resolved_count"));
        JSONObject runtime=result.getJSONArray("results").getJSONObject(0)
            .getJSONObject("runtime_result");
        assertEquals(2,runtime.getJSONArray("proposed_conditions").length());
        assertFalse(runtime.optBoolean("condition_applied",true));
        request.put("source_pointer","/monster/3/action/2");
        result=new OfficialBookRuntimeGateway(null).execute("xmm",
            "hybrid_batch_preview",packet).getJSONObject("result");
        assertEquals(0,result.optInt("resolved_count"));
        assertEquals("save_condition_pointer_mismatch",
            result.getJSONArray("results").getJSONObject(0).optString("reason"));
    }

    private static JSONObject request(String monster,String action,String pointer)throws Exception{
        return new JSONObject().put("monster",monster).put("action",action)
            .put("source_pointer",pointer).put("source_blob_sha1",
                Xmm2025SaveConditionPreviewEngine.SOURCE_BLOB);
    }
}
