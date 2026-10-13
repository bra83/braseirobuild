package com.braseiro.dnd5e;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.*;

/**
 * Individually exercises the 25 XMM A-Y editorial monster-entry sections.
 * Each section must have actual registered monsters and playable fields.
 * This is a tactical runtime-index test, NOT book-statblock fidelity proof.
 */
@RunWith(Parameterized.class)
public final class Xmm25AlphabetSectionRuntimeTest {
    @Parameterized.Parameters(name="XMM:{0} Monsters ({1})")
    public static Collection<Object[]> alphabetSections() {
        return Arrays.asList(new Object[][] {
        {"sec_1_0","A"},
        {"sec_1_1","B"},
        {"sec_1_2","C"},
        {"sec_1_3","D"},
        {"sec_1_4","E"},
        {"sec_1_5","F"},
        {"sec_1_6","G"},
        {"sec_1_7","H"},
        {"sec_1_8","I"},
        {"sec_1_9","J"},
        {"sec_1_10","K"},
        {"sec_1_11","L"},
        {"sec_1_12","M"},
        {"sec_1_13","N"},
        {"sec_1_14","O"},
        {"sec_1_15","P"},
        {"sec_1_16","Q"},
        {"sec_1_17","R"},
        {"sec_1_18","S"},
        {"sec_1_19","T"},
        {"sec_1_20","U"},
        {"sec_1_21","V"},
        {"sec_1_22","W"},
        {"sec_1_23","X"},
        {"sec_1_24","Y"}
        });
    }
    @BeforeClass public static void prepareBestiary() {
        JSONObject installed=Xmm2025CompiledBestiaryEngine.install();
        assertEquals("Not all bestiary entries registered",503,installed.optInt("statblocks_installed"));
        assertEquals("Registration failures",0,installed.optInt("blocked",-1));
    }
    private final String sectionId;
    private final String initial;
    public Xmm25AlphabetSectionRuntimeTest(String sectionId,String initial) {
        this.sectionId=sectionId; this.initial=initial;
    }

    @Test public void bookAlphabetSectionMustExposeRealTacticalProfiles() {
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
