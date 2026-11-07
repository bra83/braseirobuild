package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

/**
 * A missing Python runtime must not prevent the authoritative Java D&D
 * mechanics/knowledge/agency guard from accepting an otherwise valid turn.
 * Genuine negotiated host-contract mismatches must still fail closed.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public final class BarbaraColdStartValidationRegressionTest {
    @Test public void uninitializedBarbaraDelegatesToJavaWithoutInventingContractMismatch() {
        BarbaraSupervisor barbara=new BarbaraSupervisor(null);
        JSONObject response=barbara.validate(new JSONObject(),"Observo o pátio",
            "normal","continuation","PLAYER_ACTION","dnd");
        assertTrue(response.toString(),response.optBoolean("valid"));
        assertTrue(response.toString(),response.optBoolean("bypassed"));
        assertEquals("barbara_unavailable_java_guard_required",response.optString("reason"));
    }

    @Test public void failedPythonBootstrapRetainsJavaFallback() {
        BarbaraSupervisor barbara=new BarbaraSupervisor(null);
        JSONObject plan=barbara.plan("Observo o pátio","normal",true,false,
            "continuation","PLAYER_ACTION","dnd");
        assertFalse(plan.toString(),plan.optBoolean("available"));
        assertTrue(plan.toString(),plan.optBoolean("fallback_java_policy"));
        assertFalse("Cold start was mistaken for a negotiated mismatch: "+plan,
            plan.has("host_contract_problem"));
        JSONObject response=barbara.validate(new JSONObject(),"Observo o pátio",
            "normal","continuation","PLAYER_ACTION","dnd");
        assertTrue(response.toString(),response.optBoolean("valid"));
    }

    @Test public void explicitHostContractMismatchStillBlocks() throws Exception {
        BarbaraSupervisor barbara=new BarbaraSupervisor(null);
        Field error=BarbaraSupervisor.class.getDeclaredField("lastError");
        error.setAccessible(true);
        error.set(barbara,"host_contract_mismatch: versão incompatível");
        JSONObject response=barbara.validate(new JSONObject(),"Observo o pátio",
            "normal","continuation","PLAYER_ACTION","dnd");
        assertFalse(response.toString(),response.optBoolean("valid",true));
        assertEquals("host_contract_invalid",response.optString("reason"));
        assertTrue(response.optString("detail").contains("versão incompatível"));
    }
}
