package com.braseiro.dnd5e;

import org.junit.Test;
import static org.junit.Assert.*;

/** Run as JVM tests and again as part of the Android build gate. */
public class Core2024BlessTurnRulesTest {
    @Test public void persistsUntilExactlyTenRoundBoundaries(){
        for(int cast=1;cast<=40;cast++){
            for(int elapsed=0;elapsed<10;elapsed++)
                assertEquals(Core2024BlessTurnRules.Decision.KEEP,
                    Core2024BlessTurnRules.reconcile(cast,cast+elapsed,"Bênção",false));
            assertEquals(Core2024BlessTurnRules.Decision.EXPIRE,
                Core2024BlessTurnRules.reconcile(cast,cast+10,"Bênção",false));
            assertEquals(Core2024BlessTurnRules.Decision.EXPIRE,
                Core2024BlessTurnRules.reconcile(cast,cast+12,"Bênção",false));
        }
    }
    @Test public void endsWhenConcentrationReplacedOrIncapacitated(){
        assertEquals(Core2024BlessTurnRules.Decision.END_CONCENTRATION,
            Core2024BlessTurnRules.reconcile(1,2,"Escudo da Fé",false));
        assertEquals(Core2024BlessTurnRules.Decision.END_CONCENTRATION,
            Core2024BlessTurnRules.reconcile(1,2,"",false));
        assertEquals(Core2024BlessTurnRules.Decision.END_CONCENTRATION,
            Core2024BlessTurnRules.reconcile(1,2,"Bênção",true));
    }
    @Test public void malformedTimelineCannotSilentlyExpire(){
        for(int[] pair:new int[][]{{0,1},{5,4},{-1,9},{1,1000001}}){
            try{Core2024BlessTurnRules.reconcile(pair[0],pair[1],"Bênção",false);
                fail("invalid timeline accepted");}
            catch(IllegalArgumentException expected){}
        }
    }
}
