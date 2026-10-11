package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

/** Pure rules regression; no SQLite or Android claims. */
public final class Run243WeightedTerrainRouteTest {
    private static boolean[][] grid(int w,int h){return new boolean[h][w];}
    @Test public void choosesCheaperDetourRatherThanShortestDifficultRoute(){
        boolean[][] blocked=grid(8,6),hard=grid(8,6);
        hard[2][2]=true;
        TacticalGridMovementRouteRules.Route route=TacticalGridMovementRouteRules.plan(
            8,6,1.5/8,2.5/6,3,2,blocked,hard,false,false);
        assertTrue(route.error,route.ok());
        assertEquals(2,route.steps);assertEquals(10,route.budgetFeet);
        assertEquals(0,route.difficultSteps);
        assertEquals(3,route.cells.size());
        assertFalse(route.cells.get(1).x==2&&route.cells.get(1).y==2);
    }
    @Test public void forcedDifficultCellChargesExtraAndCrawlingStacks(){
        boolean[][] blocked=grid(8,6),hard=grid(8,6);
        hard[2][2]=true;blocked[1][2]=true;blocked[3][2]=true;
        TacticalGridMovementRouteRules.Route walk=TacticalGridMovementRouteRules.plan(
            8,6,1.5/8,2.5/6,3,2,blocked,hard,false,false);
        assertTrue(walk.error,walk.ok());assertEquals(15,walk.budgetFeet);
        assertEquals(1,walk.difficultSteps);
        TacticalGridMovementRouteRules.Route crawl=TacticalGridMovementRouteRules.plan(
            8,6,1.5/8,2.5/6,3,2,blocked,hard,false,true);
        assertTrue(crawl.error,crawl.ok());assertEquals(25,crawl.budgetFeet);
        TacticalGridMovementRouteRules.Route gm=TacticalGridMovementRouteRules.plan(
            8,6,1.5/8,2.5/6,3,2,blocked,hard,true,true);
        assertTrue(gm.error,gm.ok());assertEquals(30,gm.budgetFeet);
        assertEquals(2,gm.difficultSteps);
    }
    @Test public void previewFingerprintBindsTerrainAndChosenPath(){
        boolean[][] blocked=grid(8,6),hard=grid(8,6);
        TacticalGridMovementRouteRules.Route a=TacticalGridMovementRouteRules.plan(
            8,6,1.5/8,2.5/6,3,2,blocked,hard,false,false);
        hard[2][2]=true;
        TacticalGridMovementRouteRules.Route b=TacticalGridMovementRouteRules.plan(
            8,6,1.5/8,2.5/6,3,2,blocked,hard,false,false);
        assertTrue(a.ok()&&b.ok());assertNotEquals(a.fingerprint,b.fingerprint);
        assertTrue(TacticalGridMovementRouteRules.validReceipt(
            TacticalGridMovementRouteRules.receipt("scene:xyz",b),b.budgetFeet));
    }
    @Test public void noCornerCuttingAndNoBlockedDestination(){
        boolean[][] blocked=grid(8,6),hard=grid(8,6);
        blocked[2][2]=true;blocked[3][1]=true;
        TacticalGridMovementRouteRules.Route route=TacticalGridMovementRouteRules.plan(
            8,6,1.5/8,2.5/6,2,3,blocked,hard,false,false);
        assertTrue(route.error,route.ok());assertTrue(route.steps>1);
        blocked[3][2]=true;
        assertEquals("destination_occupied_or_blocked",TacticalGridMovementRouteRules.plan(
            8,6,1.5/8,2.5/6,2,3,blocked,hard,false,false).error);
    }
    @Test public void reactionPathMustFollowWeightedRouteNotUnweightedShortestPath(){
        boolean[][] blocked=grid(8,6),hard=grid(8,6);
        hard[1][2]=true;
        TacticalGridMovementRouteRules.Route weighted=TacticalGridMovementRouteRules.plan(
            8,6,1.5/8,2.5/6,3,2,blocked,hard,false,false);
        assertTrue(weighted.error,weighted.ok());
        java.util.List<OpportunityAttackRules.Position> unweighted=
            OpportunityAttackRules.shortestPath(8,6,
                new OpportunityAttackRules.Position(1,2,0,1),
                new OpportunityAttackRules.Position(3,2,0,1),blocked);
        assertEquals(3,unweighted.size());
        assertEquals(2,unweighted.get(1).x);assertEquals(1,unweighted.get(1).y);
        assertFalse(weighted.cells.get(1).x==unweighted.get(1).x&&
            weighted.cells.get(1).y==unweighted.get(1).y);
        for(int i=1;i<weighted.cells.size();i++){
            TacticalGridMovementRouteRules.Cell before=weighted.cells.get(i-1),
                after=weighted.cells.get(i);
            assertTrue(Math.max(Math.abs(before.x-after.x),Math.abs(before.y-after.y))==1);
        }
    }
    @Test public void invalidExplicitTerrainShapeFailsClosed(){
        boolean[][] blocked=grid(8,6),hard=grid(7,6);
        assertEquals("invalid_grid",TacticalGridMovementRouteRules.plan(
            8,6,1.5/8,2.5/6,3,2,blocked,hard,false,false).error);
    }
}
