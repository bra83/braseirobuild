package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class AlignmentNarrativePolicyTest {
    @Test public void allNineAlignmentsCanonicalize() {
        String[] english={"Lawful Good","Neutral Good","Chaotic Good","Lawful Neutral","True Neutral","Chaotic Neutral","Lawful Evil","Neutral Evil","Chaotic Evil"};
        String[] portuguese={"Leal e Bom","Neutro e Bom","Caótico e Bom","Leal e Neutro","Neutro","Caótico e Neutro","Leal e Mau","Neutro e Mau","Caótico e Mau"};
        for(int i=0;i<english.length;i++) {
            assertEquals(portuguese[i],AlignmentNarrativePolicy.canonical(english[i]));
            assertEquals(portuguese[i],AlignmentNarrativePolicy.canonical(portuguese[i]));
        }
    }
    @Test public void neverAssumesAlignmentFromClassRaceOrMissingRecord() {
        assertEquals("",AlignmentNarrativePolicy.canonical(null));
        assertEquals("",AlignmentNarrativePolicy.canonical("Mago"));
        assertEquals("",AlignmentNarrativePolicy.fromNpcMemory(new JSONObject()));
    }
    @Test public void npcExistingStatblockAlignmentCanBeUsedWithoutMutation() throws Exception {
        JSONObject npc=new JSONObject().put("stats_json",new JSONObject().put("alignment","Chaotic Good").toString());
        assertEquals("Caótico e Bom",AlignmentNarrativePolicy.fromNpcMemory(npc));
        assertFalse(npc.has("alignment"));
    }
}
