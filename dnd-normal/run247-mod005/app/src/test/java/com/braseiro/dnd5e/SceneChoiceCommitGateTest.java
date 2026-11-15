package com.braseiro.dnd5e;
import org.junit.Test;
import static org.junit.Assert.*;

public final class SceneChoiceCommitGateTest {
    @Test public void rejectsEveryIncompletePlayableScene(){
        for(int n=0;n<=3;n++)assertFalse("n="+n,
          SceneChoiceCommitGate.canCommit(true,false,false,n));
        assertTrue(SceneChoiceCommitGate.canCommit(true,false,false,4));
        assertFalse("Five options is not the four-choice contract",
          SceneChoiceCommitGate.canCommit(true,false,false,5));
    }
    @Test public void preservesObservationsAndPendingRollWithoutFabrication(){
        assertTrue(SceneChoiceCommitGate.canCommit(true,true,false,0));
        assertTrue(SceneChoiceCommitGate.canCommit(true,false,true,0));
        assertTrue(SceneChoiceCommitGate.canCommit(false,false,false,0));
    }
    @Test public void failureIsExplicitAndRecoverable(){
        String reason=SceneChoiceCommitGate.reason(1);
        assertTrue(reason.startsWith("ESCOLHAS:"));
        assertTrue(reason.contains("Cena anterior preservada"));
        assertFalse(reason.contains("sucesso"));
    }
}
