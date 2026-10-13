package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Test official source reference sections through the same Android routing gateway. */
public final class XsacEditorialSourceGatewayTest {
    @Test public void referenceSectionProvidesActualThreeBookIdsAndSourceURL() throws Exception {
        OfficialBookRuntimeGateway gateway=new OfficialBookRuntimeGateway(null);
        JSONObject result=gateway.execute("xsac","rules_references",new JSONObject());
        assertTrue(result.getBoolean("executed"));
        assertFalse(result.getBoolean("auto_mutation_allowed"));
        JSONObject ref=result.getJSONObject("result");
        assertEquals("sec_0_0",ref.getString("section_id"));
        assertEquals("XPHB",ref.getJSONArray("core_rulebook_ids").getString(0));
        assertEquals("XMM",ref.getJSONArray("core_rulebook_ids").getString(1));
        assertEquals("XDMG",ref.getJSONArray("core_rulebook_ids").getString(2));
        assertTrue(ref.getString("errata_url").startsWith("https://www.dndbeyond.com/"));
    }
    @Test public void rulingPolicyRejectsExternalStatementsAsOfficialRules() throws Exception {
        OfficialBookRuntimeGateway gateway=new OfficialBookRuntimeGateway(null);
        JSONObject result=gateway.execute("xsac","official_rulings",new JSONObject());
        assertTrue(result.getBoolean("executed"));
        JSONObject policy=result.getJSONObject("result");
        assertTrue(policy.getBoolean("dm_has_final_say"));
        assertFalse(policy.getBoolean("public_statements_are_official_rulings"));
        assertFalse(policy.getBoolean("auto_mutation_allowed"));
    }
}
