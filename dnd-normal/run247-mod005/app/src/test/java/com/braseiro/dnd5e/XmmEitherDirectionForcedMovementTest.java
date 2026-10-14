package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

/** The Hook Horror chooses toward or away; the recorded destination is that choice. */
public final class XmmEitherDirectionForcedMovementTest {
    @Test public void eitherDirectionAcceptsOnlyStraightValidTravelWithinFiveFeet(){
        MonsterForcedMovementRules.Spec spec=MonsterForcedMovementRules.compiled("either:5ft","");
        assertNotNull(spec);
        boolean[][] blocked=new boolean[8][12],occupied=new boolean[8][12];
        assertTrue(MonsterForcedMovementRules.plan(spec,2,2,4,2,5,2,12,8,blocked,occupied).valid());
        assertTrue(MonsterForcedMovementRules.plan(spec,2,2,4,2,3,2,12,8,blocked,occupied).valid());
        assertEquals("forced_movement_distance_invalid",MonsterForcedMovementRules.plan(
            spec,2,2,4,2,6,2,12,8,blocked,occupied).blocked);
        assertEquals("forced_movement_wrong_direction",MonsterForcedMovementRules.plan(
            spec,2,2,4,2,4,3,12,8,blocked,occupied).blocked);
        assertEquals("forced_movement_distance_invalid",MonsterForcedMovementRules.plan(
            spec,2,2,4,2,4,2,12,8,blocked,occupied).blocked);
        assertNull(MonsterForcedMovementRules.compiled("either:5ft;prone",""));
    }
}
