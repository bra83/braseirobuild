package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Every source-bound simple XMM save rider must require visible dice results. */
public final class XmmSaveConditionSourceBoundTest {
    @Test public void allSimpleSaveRidersRequireExplicitSaveAndManualApplication() throws Exception {
        JSONObject coverage=Xmm2025SaveConditionEngine.coverage();
        assertTrue(coverage.toString(),coverage.optBoolean("resolved"));
        assertEquals(79,coverage.getInt("single_condition_save_actions"));
        JSONArray names=Xmm2025CompiledBestiaryEngine.catalog().getJSONArray("names");
        int checked=0;
        for (int i=0;i<names.length();i++) {
            String monster=names.getString(i);
            JSONObject row=CompiledStatblockEngine.get(monster);
            JSONArray actions=row.getJSONArray("actions");
            for (int j=0;j<actions.length();j++) {
                JSONObject action=actions.getJSONObject(j);
                String name=action.getString("name");
                JSONObject profile=Xmm2025SaveConditionEngine.profile(monster,name);
                if (!profile.optBoolean("resolved")) continue;
                assertEquals(j,profile.getInt("source_action_slot"));
                assertEquals(action.getString("condition_on_fail"),
                    profile.getString("condition_on_failed_save"));
                assertEquals(action.getInt("save_dc"),profile.getInt("save_dc"));
                JSONObject in=new JSONObject().put("monster",monster).put("action",name)
                    .put("target_is_creature",true);
                String trigger=profile.getBoolean("attack_hit_required")?
                    "attack_hit":"target_affected";
                in.put(trigger,true);
                JSONObject pending=Xmm2025SaveConditionEngine.resolve(in);
                assertTrue(pending.toString(),pending.optBoolean("resolved"));
                assertEquals("manual_save_result_required",pending.getString("status"));
                assertEquals(0,pending.getJSONArray("proposed_conditions").length());
                JSONObject fail=Xmm2025SaveConditionEngine.resolve(in.put("save_success",false));
                assertTrue(fail.toString(),fail.optBoolean("resolved"));
                assertEquals(1,fail.getJSONArray("proposed_conditions").length());
                assertEquals(action.getString("condition_on_fail"),
                    fail.getJSONArray("proposed_conditions").getJSONObject(0).getString("condition"));
                assertFalse(fail.optBoolean("auto_rolls"));
                assertFalse(fail.optBoolean("auto_mutation_allowed"));
                assertFalse(fail.optBoolean("condition_applied"));
                assertFalse(fail.optBoolean("damage_applied"));
                JSONObject pass=Xmm2025SaveConditionEngine.resolve(in.put("save_success",true));
                assertTrue(pass.toString(),pass.optBoolean("resolved"));
                assertEquals(0,pass.getJSONArray("proposed_conditions").length());
                JSONObject bad=Xmm2025SaveConditionEngine.resolve(in.put("save_success","false"));
                assertFalse(bad.toString(),bad.optBoolean("resolved"));
                JSONObject miss=Xmm2025SaveConditionEngine.resolve(in.put("save_success",false).put(trigger,false));
                assertTrue(miss.toString(),miss.optBoolean("resolved"));
                assertEquals(0,miss.getJSONArray("proposed_conditions").length());
                checked++;
            }
        }
        assertEquals(79,checked);
    }

    @Test public void complexAndUnknownRidersAreNeverAutoResolved() throws Exception {
        Xmm2025CompiledBestiaryEngine.install();
        assertFalse(Xmm2025SaveConditionEngine.profile("Grell","Paralyzing Tentacles").optBoolean("resolved"));
        assertFalse(Xmm2025SaveConditionEngine.profile("Cockatrice","Petrifying Bite").optBoolean("resolved"));
        assertFalse(Xmm2025SaveConditionEngine.resolve(new JSONObject()
            .put("monster","Grell").put("action","Paralyzing Tentacles")
            .put("target_is_creature",true).put("attack_hit",true)
            .put("save_success",false)).optBoolean("resolved"));
        JSONObject invalid=Xmm2025SaveConditionEngine.resolve(new JSONObject()
            .put("monster","Ghoul").put("action","Claw")
            .put("target_is_creature","true").put("attack_hit",true)
            .put("save_success",false));
        assertFalse(invalid.optBoolean("resolved"));
    }
}
