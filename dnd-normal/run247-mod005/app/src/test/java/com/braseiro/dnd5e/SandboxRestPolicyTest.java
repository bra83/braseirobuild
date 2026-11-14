package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

/** Pure policy tests; only an authorized gateway applies mechanical recovery. */
public class SandboxRestPolicyTest {
    @Test public void shortSleepDoesNotCompleteLongRest(){
        assertFalse(SandboxRestPolicy.validLong(true,2d));
        assertFalse(SandboxRestPolicy.validLong(true,7.99d));
        assertTrue(SandboxRestPolicy.validLong(true,8d));
        assertFalse(SandboxRestPolicy.validLong(false,8d));
    }
    @Test public void shortRestNeedsAtLeastOneHour(){
        assertFalse(SandboxRestPolicy.validShort(true,.5d,false));
        assertTrue(SandboxRestPolicy.validShort(true,1d,false));
        assertFalse(SandboxRestPolicy.validShort(true,1d,true));
        assertFalse(SandboxRestPolicy.validShort(false,1d,false));
    }
    @Test public void remindersDoNotInferPenalties(){
        assertFalse(SandboxRestPolicy.recommended(15.99d));
        assertTrue(SandboxRestPolicy.recommended(16d));
        assertFalse(SandboxRestPolicy.overdue(23.99d));
        assertTrue(SandboxRestPolicy.overdue(24d));
        assertEquals(0d,SandboxRestPolicy.hoursSinceLongRest(10d,34d,true),.0001d);
        assertEquals(24d,SandboxRestPolicy.hoursSinceLongRest(10d,34d,false),.0001d);
    }
}