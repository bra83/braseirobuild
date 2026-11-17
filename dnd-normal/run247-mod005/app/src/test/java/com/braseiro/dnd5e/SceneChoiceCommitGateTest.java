package com.braseiro.dnd5e;
import org.junit.Test;
import static org.junit.Assert.*;

public final class SceneChoiceCommitGateTest {
    @Test public void threeGroundedChoicesDoNotWasteGeminiResponseOrPlayerAction(){
        // Reported from physical Android: three total validated actions caused
        // a blocking ESCOLHAS dialog even though the scene was narrated.
        assertTrue("Valid three-card menu cannot roll back a whole scene",
          SceneChoiceCommitGate.canCommit(true,false,false,3));
        assertTrue("Two real actions and momentum are playable",
          SceneChoiceCommitGate.canCommit(true,false,false,2));
        assertTrue("Sparse scene with only voluntary momentum is playable",
          SceneChoiceCommitGate.canCommit(true,false,false,1));
        assertTrue(SceneChoiceCommitGate.canCommit(true,false,false,4));
    }
    @Test public void invalidEmptyOrOverfilledMenuStillFailsClosed(){
        assertFalse(SceneChoiceCommitGate.canCommit(true,false,false,0));
        assertFalse(SceneChoiceCommitGate.canCommit(true,false,false,5));
    }
    @Test public void preservesObservationsAndPendingRollWithoutFabrication(){
        assertTrue(SceneChoiceCommitGate.canCommit(true,true,false,0));
        assertTrue(SceneChoiceCommitGate.canCommit(true,false,true,0));
        assertTrue(SceneChoiceCommitGate.canCommit(false,false,false,0));
    }
    @Test public void failureIsExplicitAndDoesNotDemandAnotherGeminiCall(){
        String reason=SceneChoiceCommitGate.reason(0);
        assertTrue(reason.startsWith("ESCOLHAS:"));
        assertTrue(reason.contains("preservou a ação"));
        assertFalse(reason.contains("Tente novamente"));
    }
}
