package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

/** Unit tests for the screen-to-cell boundary; no Android dependence. */
public final class Run242TacticalGridTapRulesTest {
    @Test public void exactEdgesAndCenterMapIntoExpectedGridCell(){
        TacticalGridTapRules.Cell a=TacticalGridTapRules.fromTap(480,320,12,8,0f,0f);
        assertTrue(a.valid());assertEquals(0,a.x);assertEquals(0,a.y);
        a=TacticalGridTapRules.fromTap(480,320,12,8,240f,160f);
        assertTrue(a.valid());assertEquals(6,a.x);assertEquals(4,a.y);
        a=TacticalGridTapRules.fromTap(480,320,12,8,479.99f,319.99f);
        assertTrue(a.valid());assertEquals(11,a.x);assertEquals(7,a.y);
    }
    @Test public void invalidOrOutsideTouchesAreAlwaysRejected(){
        assertFalse(TacticalGridTapRules.fromTap(480,320,12,8,480,0).valid());
        assertFalse(TacticalGridTapRules.fromTap(480,320,12,8,-1,0).valid());
        assertFalse(TacticalGridTapRules.fromTap(480,320,12,8,Float.NaN,1).valid());
        assertFalse(TacticalGridTapRules.fromTap(480,320,12,8,1,Float.POSITIVE_INFINITY).valid());
        assertFalse(TacticalGridTapRules.fromTap(0,320,12,8,0,0).valid());
        assertFalse(TacticalGridTapRules.fromTap(480,320,101,8,1,1).valid());
    }
    @Test public void mappedDestinationUsesSamePersistedRouteRules(){
        TacticalGridTapRules.Cell c=TacticalGridTapRules.fromTap(480,320,12,8,200,80);
        assertTrue(c.valid());
        boolean[][] obstacles=new boolean[8][12];
        TacticalGridMovementRouteRules.Route route=TacticalGridMovementRouteRules.plan(
            12,8,.125,.125,c.x,c.y,obstacles,false,false);
        assertTrue(route.error,route.ok());
        assertEquals(5*route.steps,route.traveledFeet);
        obstacles[c.y][c.x]=true;
        assertEquals("destination_occupied_or_blocked",TacticalGridMovementRouteRules.plan(
            12,8,.125,.125,c.x,c.y,obstacles,false,false).error);
    }
}
