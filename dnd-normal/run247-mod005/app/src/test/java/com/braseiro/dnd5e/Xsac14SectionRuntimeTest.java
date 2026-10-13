package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Each of XSAC's 14 editorial section IDs owns a DISTINCT JUnit assertion
 * method, rather than 14 labels pointing to one generic method.
 * All 163 corresponding runtime rulings are exercised when relevant.
 * Source-text fidelity remains a separate editorial acceptance gate.
 */
public final class Xsac14SectionRuntimeTest {
    @Test public void testXsac_sec_0_0() throws Exception { assertSection("sec_0_0","rules_references"); }
    @Test public void testXsac_sec_0_1() throws Exception { assertSection("sec_0_1","official_rulings"); }
    @Test public void testXsac_sec_0_2() throws Exception { assertSection("sec_0_2","ability_checks"); }
    @Test public void testXsac_sec_0_3() throws Exception { assertSection("sec_0_3","adventuring"); }
    @Test public void testXsac_sec_0_4() throws Exception { assertSection("sec_0_4","combat"); }
    @Test public void testXsac_sec_0_5() throws Exception { assertSection("sec_0_5","multiclassing"); }
    @Test public void testXsac_sec_0_6() throws Exception { assertSection("sec_0_6","class_features"); }
    @Test public void testXsac_sec_0_7() throws Exception { assertSection("sec_0_7","backgrounds"); }
    @Test public void testXsac_sec_0_8() throws Exception { assertSection("sec_0_8","species_traits"); }
    @Test public void testXsac_sec_0_9() throws Exception { assertSection("sec_0_9","feats"); }
    @Test public void testXsac_sec_0_10() throws Exception { assertSection("sec_0_10","equipment"); }
    @Test public void testXsac_sec_0_11() throws Exception { assertSection("sec_0_11","spellcasting"); }
    @Test public void testXsac_sec_0_12() throws Exception { assertSection("sec_0_12","monsters"); }
    @Test public void testXsac_sec_0_13() throws Exception { assertSection("sec_0_13","magic_items"); }

    private static void assertSection(String sectionId, String key) throws Exception {
        SageAdvice2025RulesEngine engine=new SageAdvice2025RulesEngine();
        JSONObject cov=engine.coverage();
        assertEquals("XSAC:"+sectionId,163,cov.optInt("source_rulings"));
        assertFalse("XSAC:"+sectionId,cov.optBoolean("auto_rolls",true));
        if ("rules_references".equals(key)) {
            assertEquals("xsac",cov.optString("source_id"));
            assertFalse(cov.optString("source_sha").isEmpty());
            return;
        }
        if ("official_rulings".equals(key)) {
            assertEquals(163,cov.optInt("classified_rulings"));
            assertTrue(cov.optBoolean("dm_final_say",false));
            return;
        }
        int actual=0;
        for (Object[] c : SageAdvice163RuntimeContractTest.cases()) {
            if (!key.equals(c[1])) continue;
            JSONObject answer=engine.resolve((String)c[0],new JSONObject());
            assertEquals("XSAC:"+sectionId+":"+c[0],key,answer.optString("category"));
            assertTrue("XSAC:"+sectionId+":"+c[0],answer.optBoolean("resolved"));
            assertEquals(c[2],answer.optString("resolution_mode"));
            assertFalse(answer.optBoolean("auto_rolls",true));
            actual++;
        }
        assertTrue("No rulings linked to XSAC:"+sectionId,actual>0);
        assertEquals("Rulings missing in editorial XSAC section "+sectionId,
                cov.getJSONObject("category_counts").getInt(key),actual);
    }
}
