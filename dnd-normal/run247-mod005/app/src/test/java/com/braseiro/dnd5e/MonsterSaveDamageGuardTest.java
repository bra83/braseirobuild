package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MonsterSaveDamageGuardTest {
    @Test public void savedDamageRangeAppliesSourceMultiplierBeforeRoundingDown() {
        assertTrue(MonsterSaveDamageGuard.accepts("3d6 + 5","3d6 + 5",true,0.5d,7));
        assertTrue(MonsterSaveDamageGuard.accepts("3d6 + 5","3d6 + 5",true,0.5d,4));
        assertTrue(MonsterSaveDamageGuard.accepts("3d6 + 5","3d6 + 5",true,0.5d,11));
        assertFalse(MonsterSaveDamageGuard.accepts("3d6 + 5","3d6 + 5",true,0.5d,15));
        assertFalse(MonsterSaveDamageGuard.accepts("3d6 + 5","3d6 + 5",true,0.5d,3));
    }

    @Test public void failureUsesFullDamageAndInvalidMultipliersFailClosed() {
        assertTrue(MonsterSaveDamageGuard.accepts("3d6 + 5","3d6 + 5",false,0.5d,15));
        assertFalse(MonsterSaveDamageGuard.accepts("3d6 + 5","3d6 + 5",false,0.5d,7));
        assertFalse(MonsterSaveDamageGuard.accepts("3d6 + 5","3d6 + 5",true,Double.NaN,0));
        assertFalse(MonsterSaveDamageGuard.accepts("3d6 + 5","3d6 + 5",true,1.5d,15));
    }
}
