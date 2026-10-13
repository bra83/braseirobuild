package com.braseiro.dnd5e;

import java.util.Arrays;
import java.util.Collection;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/**
 * One runtime test for each of the 14 editorial XSAC section IDs.
 * The first two sections verify the published runtime index and total ruling
 * count. The remaining 12 actually execute every classified ruling from the
 * static 163-case snapshot. Does not claim independent source-text accuracy.
 */
@RunWith(Parameterized.class)
public final class Xsac14SectionRuntimeTest {
    @Parameterized.Parameters(name="XSAC:{0} [{1}]")
    public static Collection<Object[]> sections() {
        return Arrays.asList(new Object[][] {
        {"sec_0_0", "rules_references"},
        {"sec_0_1", "official_rulings"},
        {"sec_0_2", "ability_checks"},
        {"sec_0_3", "adventuring"},
        {"sec_0_4", "combat"},
        {"sec_0_5", "multiclassing"},
        {"sec_0_6", "class_features"},
        {"sec_0_7", "backgrounds"},
        {"sec_0_8", "species_traits"},
        {"sec_0_9", "feats"},
        {"sec_0_10", "equipment"},
        {"sec_0_11", "spellcasting"},
        {"sec_0_12", "monsters"},
        {"sec_0_13", "magic_items"}
        });
    }
    private final String sectionId;
    private final String key;
    public Xsac14SectionRuntimeTest(String sectionId, String key) {
        this.sectionId=sectionId;
        this.key=key;
    }

    @Test public void executeSectionScopedRulings() throws Exception {
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
        assertTrue("No functional rulings linked to XSAC:"+sectionId,actual>0);
        assertEquals("Rulings missing in editorial XSAC section "+sectionId,
                cov.getJSONObject("category_counts").getInt(key),actual);
    }
}
