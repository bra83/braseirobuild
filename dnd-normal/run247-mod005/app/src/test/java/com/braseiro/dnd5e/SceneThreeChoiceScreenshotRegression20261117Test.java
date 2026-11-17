package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Real Android report: narration about Urzmaktok Groish setting a stake
 * against roots reached the strict 4-card gate with only 3 valid choices.
 * A card shortage must never discard a validated scene or require Gemini retry.
 */
public final class SceneThreeChoiceScreenshotRegression20261117Test {
    private static final String ONSTAGE =
        "Urzmaktok Groish prende a estaca final junto às raízes, ajeitando a terra "
       +"para mantê-la firme. Kvothe observa o trabalho antes de decidir se intervém.";

    @Test public void namedVisibleWorkerAndPhysicalFociCanProposeDifferentChoices(){
        java.util.List<JSONObject> candidates=SceneOpenWorldActionPlanner.candidates(ONSTAGE);
        assertEquals("The named actor is visibly acting even without dialogue",3,candidates.size());
        for(JSONObject candidate:candidates) {
            assertTrue("No imaginary actor or location: "+candidate,
                SceneActionEvidence.valid(candidate,ONSTAGE));
        }
        JSONArray choices=SceneOptionDiversifier.ensureAlways(
            new JSONArray(),new JSONArray(),ONSTAGE,"",new JSONObject(),12);
        assertTrue("Scene must be playable: "+choices,choices.length()>=1);
        assertTrue(StoryMomentumAction.isRequest(choices.optJSONObject(0).optString("action")));
        assertTrue(SceneChoiceCommitGate.canCommit(true,false,false,choices.length()));
        for(int i=1;i<choices.length();i++){
            JSONObject option=choices.optJSONObject(i);
            assertTrue("Every card must be grounded: "+option,
                SceneActionEvidence.valid(option,ONSTAGE));
            assertFalse("No phantom guard",option.optString("action").toLowerCase().contains("guarda"));
        }
    }

    @Test public void screenshotCaseWithOnlyThreeValidCardsMustCommit(){
        assertTrue(SceneChoiceCommitGate.canCommit(true,false,false,3));
        assertFalse(SceneChoiceCommitGate.canCommit(true,false,false,0));
    }

    @Test public void distantReportDoesNotCreateAVisibleActor(){
        String distant="Uma estudante relata que Urzmaktok Groish prende a estaca "
            +"junto às raízes em outro jardim distante.";
        assertTrue(SceneOpenWorldActionPlanner.candidates(distant).isEmpty());
    }
}
