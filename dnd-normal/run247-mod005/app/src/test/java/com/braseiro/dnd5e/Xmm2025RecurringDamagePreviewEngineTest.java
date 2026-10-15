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
public final class Xmm2025RecurringDamagePreviewEngineTest {
    @BeforeClass public static void install(){assertTrue(
        Xmm2025CompiledBestiaryEngine.install().optBoolean("complete"));}

    @Test public void allThreeSourceProfilesRetainTheirDistinctTicks()throws Exception{
        String[][] expected={{"Pit Fiend","Bite","6d6","poison","target_turn_start","poisoned"},
            {"Stirge","Proboscis","2d4","necrotic","source_turn_start","attached"},
            {"Vine Blight","Constricting Vine","1d8","bludgeoning","target_turn_start","grappled"}};
        assertEquals(3,Xmm2025RecurringDamagePreviewEngine.catalog().length());
        for(String[] row:expected){
            JSONObject profile=Xmm2025RecurringDamagePreviewEngine.profile(row[0],row[1]);
            assertTrue(profile.toString(),profile.optBoolean("resolved"));
            assertTrue(profile.optString("ptr").startsWith("/monster/"));
            assertEquals(row[2],profile.optString("damage"));
            assertEquals(row[3],profile.optString("damage_type"));
            assertEquals(row[4],profile.optString("tick"));
            assertEquals(row[5],profile.optString("active_condition"));
            assertFalse(profile.optBoolean("damage_applied",true));
            assertFalse(profile.optBoolean("persisted",true));
        }
    }

    @Test public void explicitTickAndDamageTotalAreReadOnly()throws Exception{
        JSONObject out=Xmm2025RecurringDamagePreviewEngine.resolve(request("Pit Fiend","Bite",
            "/monster/346/action/1/entries/0","target_turn_start")
            .put("lifecycle_active",true).put("damage_total",21));
        assertTrue(out.toString(),out.optBoolean("resolved"));
        assertEquals(21,out.optInt("damage_total"));
        assertEquals("poisoned",out.optString("lifecycle_condition"));
        assertTrue(out.optBoolean("defenses_pending"));
        assertFalse(out.optBoolean("damage_applied",true));
        assertFalse(out.optBoolean("auto_rolls",true));
        assertFalse(out.optBoolean("auto_mutation_allowed",true));
    }

    @Test public void inactiveLifecycleAndInvalidTickOrDamageDoNotProposeDamage()throws Exception{
        String m="Stirge",a="Proboscis",p="/monster/417/action/0/entries/0";
        JSONObject inactive=Xmm2025RecurringDamagePreviewEngine.resolve(request(m,a,p,"source_turn_start")
            .put("lifecycle_active",false));
        assertTrue(inactive.optBoolean("resolved"));
        assertEquals("lifecycle_not_active",inactive.optString("status"));
        JSONObject wrongTick=Xmm2025RecurringDamagePreviewEngine.resolve(request(m,a,p,"target_turn_start")
            .put("lifecycle_active",true).put("damage_total",5));
        assertEquals("wrong_turn_boundary",wrongTick.optString("reason"));
        JSONObject tooMuch=Xmm2025RecurringDamagePreviewEngine.resolve(request(m,a,p,"source_turn_start")
            .put("lifecycle_active",true).put("damage_total",9));
        assertEquals("damage_total_out_of_source_bounds",tooMuch.optString("reason"));
        JSONObject noActive=Xmm2025RecurringDamagePreviewEngine.resolve(request(m,a,p,"source_turn_start")
            .put("damage_total",5));
        assertEquals("explicit_lifecycle_state_required",noActive.optString("reason"));
    }

    @Test public void hybridBatchRoutesRecurringDamageOnlyAtExactSourcePointer()throws Exception{
        JSONObject request=new JSONObject().put("request_id","vine-tick").put("book_id","xmm")
            .put("edition","2025").put("monster","Vine Blight")
            .put("source_pointer","/monster/456/action/0/entries/0")
            .put("operation","recurring_damage_resolve").put("inputs",new JSONObject()
                .put("action","Constricting Vine").put("turn_boundary","target_turn_start")
                .put("lifecycle_active",true).put("damage_total",8));
        JSONObject packet=new JSONObject().put("origin","player_preview")
            .put("source_blob_sha1",Xmm2025SourceIndex.SOURCE_BLOB)
            .put("requests",new JSONArray().put(request));
        JSONObject result=new OfficialBookRuntimeGateway(null).execute("xmm",
            "hybrid_batch_preview",packet).getJSONObject("result");
        assertEquals(1,result.optInt("resolved_count"));
        JSONObject preview=result.getJSONArray("results").getJSONObject(0)
            .getJSONObject("runtime_result");
        assertEquals(8,preview.optInt("damage_total"));
        assertFalse(preview.optBoolean("damage_applied",true));
        request.put("source_pointer","/monster/456/action/0");
        result=new OfficialBookRuntimeGateway(null).execute("xmm",
            "hybrid_batch_preview",packet).getJSONObject("result");
        assertEquals(0,result.optInt("resolved_count"));
        assertEquals("recurring_damage_pointer_mismatch",
            result.getJSONArray("results").getJSONObject(0).optString("reason"));
    }

    private static JSONObject request(String monster,String action,String pointer,String tick)throws Exception{
        return new JSONObject().put("monster",monster).put("action",action)
            .put("source_pointer",pointer).put("source_blob_sha1",
                Xmm2025RecurringDamagePreviewEngine.SOURCE_BLOB).put("turn_boundary",tick);
    }
}
