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
public final class Xmm2025ConditionChoiceEngineTest {
    private static final String PIN=Xmm2025ConditionChoiceEngine.SOURCE_BLOB;
    @BeforeClass public static void install(){
        assertTrue(Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));
    }
    private static JSONObject request(String monster,String action,String pointer,boolean hit)
        throws Exception{
        return new JSONObject().put("monster",monster).put("action",action)
            .put("source_pointer",pointer).put("source_blob_sha1",PIN).put("attack_hit",hit);
    }
    @Test public void allSixSourceContractsRemainManualAndSourceBound()throws Exception{
        String[][] rows={
            {"Death Slaad","Chaos Blade","/monster/119/action/1/entries/0","d4"},
            {"Gray Slaad","Chaos Claw","/monster/223/action/1/entries/0","d4"},
            {"Green Slaad","Chaos Staff","/monster/226/action/1/entries/0","d4"},
            {"Githzerai Psion","Psychic Warp","/monster/205/action/1/entries/0","player_choice"},
            {"Pixie","Faerie Dust","/monster/347/action/0/entries/0","player_choice"},
            {"Pixie Wonderbringer","Faerie Dust","/monster/348/action/1/entries/0","player_choice"}
        };
        for(String[] row:rows){
            JSONObject profile=Xmm2025ConditionChoiceEngine.profile(row[0],row[1]);
            assertTrue(row[0]+" profile",profile.optBoolean("resolved"));
            assertEquals(row[2],profile.optString("source_pointer"));
            assertEquals(row[3],profile.optString("choice_mode"));
            assertFalse(profile.optBoolean("application_integrated",true));
            JSONObject live=CompiledStatblockEngine.runtimeProfile(row[0]);
            JSONObject action=null;JSONArray actions=live.optJSONArray("actions");
            for(int i=0;actions!=null&&i<actions.length();i++)
                if(row[1].equals(actions.optJSONObject(i).optString("name")))action=actions.optJSONObject(i);
            assertNotNull(row[0]+" compiled action",action);
            assertTrue(action.optBoolean("manual_unmodeled_source_effect_required"));
        }
    }
    @Test public void slaadChoiceComesOnlyFromExplicitD4AndAppliesNoMutation()throws Exception{
        String[][] rows={{"Death Slaad","Chaos Blade","/monster/119/action/1/entries/0"},
            {"Gray Slaad","Chaos Claw","/monster/223/action/1/entries/0"},
            {"Green Slaad","Chaos Staff","/monster/226/action/1/entries/0"}};
        String[] result={"charmed","frightened","poisoned","incapacitated"};
        for(int i=0;i<rows.length;i++){
            for(int roll=1;roll<=4;roll++){
                JSONObject out=Xmm2025ConditionChoiceEngine.resolve(request(rows[i][0],rows[i][1],rows[i][2],true).put("d4_roll",roll));
                assertTrue(out.toString(),out.optBoolean("resolved"));
                assertEquals(result[roll-1],out.optString("proposed_condition"));
                assertEquals("source_turn_start",out.optString("duration_tick"));
                assertFalse(out.optBoolean("effect_applied",true));
                assertFalse(out.optBoolean("persisted",true));
            }
        }
        JSONObject missing=Xmm2025ConditionChoiceEngine.resolve(request("Death Slaad","Chaos Blade",rows[0][2],true));
        assertEquals("explicit_d4_roll_required",missing.optString("reason"));
        JSONObject outOfRange=Xmm2025ConditionChoiceEngine.resolve(request("Death Slaad","Chaos Blade",rows[0][2],true).put("d4_roll",5));
        assertEquals("d4_roll_out_of_range",outOfRange.optString("reason"));
        JSONObject fractional=Xmm2025ConditionChoiceEngine.resolve(request("Death Slaad","Chaos Blade",rows[0][2],true).put("d4_roll",2.5));
        assertEquals("d4_roll_out_of_range",fractional.optString("reason"));
        JSONObject notAPlayerChoice=Xmm2025ConditionChoiceEngine.resolve(request("Death Slaad","Chaos Blade",rows[0][2],true).put("chosen_condition","charmed"));
        assertEquals("explicit_d4_roll_required",notAPlayerChoice.optString("reason"));
    }
    @Test public void pixieChoiceAndMissAreExplicitAndReadOnly()throws Exception{
        JSONObject hit=Xmm2025ConditionChoiceEngine.resolve(request("Pixie","Faerie Dust","/monster/347/action/0/entries/0",true).put("chosen_condition","poisoned"));
        assertTrue(hit.toString(),hit.optBoolean("resolved"));
        assertEquals("poisoned",hit.optString("proposed_condition"));
        assertEquals(1,hit.optInt("duration_rounds"));
        assertEquals("source_turn_start",hit.optString("duration_tick"));
        JSONObject invalid=Xmm2025ConditionChoiceEngine.resolve(request("Pixie","Faerie Dust","/monster/347/action/0/entries/0",true).put("chosen_condition","prone"));
        assertEquals("condition_choice_not_declared",invalid.optString("reason"));
        JSONObject miss=Xmm2025ConditionChoiceEngine.resolve(request("Pixie Wonderbringer","Faerie Dust","/monster/348/action/1/entries/0",false));
        assertTrue(miss.optBoolean("resolved"));assertEquals("",miss.optString("proposed_condition"));
        assertFalse(miss.optBoolean("effect_applied",true));
        JSONObject decisionOnMiss=Xmm2025ConditionChoiceEngine.resolve(request("Pixie","Faerie Dust","/monster/347/action/0/entries/0",false).put("chosen_condition","charmed"));
        assertEquals("decision_not_allowed_on_miss",decisionOnMiss.optString("reason"));
        JSONObject unknown=Xmm2025ConditionChoiceEngine.resolve(request("Pixie","Faerie Dust","/monster/347/action/0/entries/0",true).put("concentration_override",true));
        assertEquals("unrecognized_condition_choice_input",unknown.optString("reason"));
    }
    @Test public void githzeraiChoiceHonorsConditionSpecificSizeCap()throws Exception{
        String m="Githzerai Psion",a="Psychic Warp",p="/monster/205/action/1/entries/0";
        JSONObject charmed=Xmm2025ConditionChoiceEngine.resolve(request(m,a,p,true).put("chosen_condition","charmed"));
        assertTrue(charmed.toString(),charmed.optBoolean("resolved"));
        assertEquals("charmed",charmed.optString("proposed_condition"));
        assertEquals("source_turn_start",charmed.optString("duration_tick"));
        assertEquals(1,charmed.optInt("duration_rounds"));
        JSONObject prone=Xmm2025ConditionChoiceEngine.resolve(request(m,a,p,true).put("chosen_condition","prone").put("target_size","Large"));
        assertTrue(prone.toString(),prone.optBoolean("resolved"));
        assertTrue(prone.optBoolean("ends_when_stands_up"));
        JSONObject huge=Xmm2025ConditionChoiceEngine.resolve(request(m,a,p,true).put("chosen_condition","prone").put("target_size","Huge"));
        assertEquals("target_exceeds_source_size_limit",huge.optString("reason"));
        JSONObject unknown=Xmm2025ConditionChoiceEngine.resolve(request(m,a,p,true).put("chosen_condition","prone"));
        assertEquals("explicit_target_size_required",unknown.optString("reason"));
    }
    @Test public void hybridBatchRoutesOnlyExactPinnedConditionChoicePointers()throws Exception{
        JSONObject request=new JSONObject().put("request_id","pixie").put("book_id","xmm").put("edition","2025")
            .put("monster","Pixie").put("source_pointer","/monster/347/action/0/entries/0")
            .put("operation","condition_choice_resolve").put("inputs",new JSONObject()
                .put("action","Faerie Dust").put("attack_hit",true).put("chosen_condition","charmed"));
        JSONObject packet=new JSONObject().put("origin","player_preview").put("source_blob_sha1",Xmm2025SourceIndex.SOURCE_BLOB)
            .put("requests",new JSONArray().put(request));
        JSONObject result=new OfficialBookRuntimeGateway(null).execute("xmm","hybrid_batch_preview",packet).getJSONObject("result");
        assertEquals(1,result.optInt("resolved_count"));
        JSONObject runtime=result.getJSONArray("results").getJSONObject(0).getJSONObject("runtime_result");
        assertEquals("charmed",runtime.optString("proposed_condition"));
        assertFalse(runtime.optBoolean("persisted",true));
        JSONObject wrong=new JSONObject(request.toString()).put("source_pointer","/monster/347/action/0");
        packet.put("requests",new JSONArray().put(wrong));
        result=new OfficialBookRuntimeGateway(null).execute("xmm","hybrid_batch_preview",packet).getJSONObject("result");
        assertEquals(0,result.optInt("resolved_count"));
        assertEquals("condition_choice_pointer_mismatch",result.getJSONArray("results").getJSONObject(0).optString("reason"));
    }
}
