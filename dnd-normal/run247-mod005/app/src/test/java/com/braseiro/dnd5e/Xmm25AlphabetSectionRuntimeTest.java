package com.braseiro.dnd5e;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Separate JUnit assertion methods for all 25 XMM alphabetical book sections.
 * Every relevant compiled creature gets actual mechanical profile checks.
 * Independent editorial source-fidelity comparisons remain outstanding.
 */
public final class Xmm25AlphabetSectionRuntimeTest {
    @BeforeClass public static void prepareBestiary() {
        JSONObject installed=Xmm2025CompiledBestiaryEngine.install();
        assertEquals("Not all bestiary entries registered",503,installed.optInt("statblocks_installed"));
        assertEquals("Registration failures",0,installed.optInt("blocked",-1));
    }
    @Test public void testXmm_sec_1_0() { assertLetter("sec_1_0","A"); }
    @Test public void testXmm_sec_1_1() { assertLetter("sec_1_1","B"); }
    @Test public void testXmm_sec_1_2() { assertLetter("sec_1_2","C"); }
    @Test public void testXmm_sec_1_3() { assertLetter("sec_1_3","D"); }
    @Test public void testXmm_sec_1_4() { assertLetter("sec_1_4","E"); }
    @Test public void testXmm_sec_1_5() { assertLetter("sec_1_5","F"); }
    @Test public void testXmm_sec_1_6() { assertLetter("sec_1_6","G"); }
    @Test public void testXmm_sec_1_7() { assertLetter("sec_1_7","H"); }
    @Test public void testXmm_sec_1_8() { assertLetter("sec_1_8","I"); }
    @Test public void testXmm_sec_1_9() { assertLetter("sec_1_9","J"); }
    @Test public void testXmm_sec_1_10() { assertLetter("sec_1_10","K"); }
    @Test public void testXmm_sec_1_11() { assertLetter("sec_1_11","L"); }
    @Test public void testXmm_sec_1_12() { assertLetter("sec_1_12","M"); }
    @Test public void testXmm_sec_1_13() { assertLetter("sec_1_13","N"); }
    @Test public void testXmm_sec_1_14() { assertLetter("sec_1_14","O"); }
    @Test public void testXmm_sec_1_15() { assertLetter("sec_1_15","P"); }
    @Test public void testXmm_sec_1_16() { assertLetter("sec_1_16","Q"); }
    @Test public void testXmm_sec_1_17() { assertLetter("sec_1_17","R"); }
    @Test public void testXmm_sec_1_18() { assertLetter("sec_1_18","S"); }
    @Test public void testXmm_sec_1_19() { assertLetter("sec_1_19","T"); }
    @Test public void testXmm_sec_1_20() { assertLetter("sec_1_20","U"); }
    @Test public void testXmm_sec_1_21() { assertLetter("sec_1_21","V"); }
    @Test public void testXmm_sec_1_22() { assertLetter("sec_1_22","W"); }
    @Test public void testXmm_sec_1_23() { assertLetter("sec_1_23","X"); }
    @Test public void testXmm_sec_1_24() { assertLetter("sec_1_24","Y"); }

    private static void assertLetter(String sectionId, String initial) {
        JSONArray names=Xmm2025CompiledBestiaryEngine.catalog().optJSONArray("names");
        assertNotNull(names);
        int present=0;
        Set<String> checked=new HashSet<>();
        for(int i=0;i<names.length();i++){
            String id=names.optString(i,"").trim();
            if(!id.toUpperCase(Locale.US).startsWith(initial))continue;
            assertTrue("Duplicate creature in "+sectionId+": "+id,checked.add(id));
            JSONObject source=CompiledStatblockEngine.get(id);
            JSONObject runtime=CompiledStatblockEngine.runtimeProfile(id);
            assertTrue(sectionId+": "+id+" is not executable",runtime.optBoolean("found"));
            assertEquals(id,runtime.optString("id"));
            assertTrue(sectionId+": "+id+" invalid AC",runtime.optInt("armor_class")>0);
            assertTrue(sectionId+": "+id+" invalid HP",runtime.optInt("hit_points")>0);
            assertNotNull(sectionId+": "+id+" missing abilities",runtime.optJSONObject("abilities"));
            assertNotNull(sectionId+": "+id+" missing movement",runtime.optJSONObject("movement"));
            assertNotNull(sectionId+": "+id+" missing actions",runtime.optJSONArray("actions"));
            assertFalse(sectionId+": "+id+" hidden RNG enabled",source.optBoolean("auto_rolls",true));
            present++;
        }
        assertTrue("No compiled creatures for XMM:"+sectionId+" ("+initial+")",present>0);
    }
}
