package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** XSAC source address contracts: all 163 numbered rulings, plus separate DM policy. */
public final class XsacSourceLocatorTest {
    @Test public void all163OfficialAnswersHaveUniqueSourceLocations() throws Exception {
        assertEquals(163,XsacSourceLocator.mappedQas());
        java.util.Set<String> sourcePointers=new java.util.HashSet<>();
        for(Object[] item:SageAdvice163RuntimeContractTest.cases()) {
            String ruleId=(String)item[0];
            JSONObject answer=new SageAdvice2025RulesEngine().resolve(ruleId,null);
            assertEquals("OFFICIAL_QA",answer.getString("source_record_kind"));
            assertTrue(sourcePointers.add(answer.getString("source_pointer")));
            assertEquals(XsacSourceLocator.SOURCE_BLOB_SHA1,answer.getString("source_blob_sha1"));
            assertFalse(answer.getBoolean("source_semantics_certified"));
        }
        assertEquals(163,sourcePointers.size());
    }

    @Test public void finalSayIsSeparateFromOfficialQaCount() throws Exception {
        JSONObject answer=new SageAdvice2025RulesEngine().resolve("dm_final_say",null);
        assertEquals("EDITORIAL_DM_POLICY",answer.getString("source_record_kind"));
        assertEquals("POLICY",answer.getString("source_qa_entry_id"));
        assertEquals(163,XsacSourceLocator.mappedQas());
        assertFalse(XsacSourceLocator.hasQa("dm_final_say"));
    }
}
