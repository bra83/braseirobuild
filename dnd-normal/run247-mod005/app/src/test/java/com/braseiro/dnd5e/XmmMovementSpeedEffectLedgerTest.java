package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public final class XmmMovementSpeedEffectLedgerTest {
    @Test public void sourceTurnPenaltyPersistsUntilOwnersNextTurnStart()throws Exception{
        JSONObject source=new JSONObject().put("speed_reduction_ft",5)
            .put("speed_reduction_duration_tick","source_turn_start");
        XmmMovementSpeedEffectLedger.Spec spec=XmmMovementSpeedEffectLedger.compiled(source);
        assertNotNull(spec);
        XmmMovementSpeedEffectLedger.Transition applied=XmmMovementSpeedEffectLedger.apply(
            new JSONArray(),spec,10,20,"Lacedon Ghoul","Icy Bite","effect-1",1,0);
        assertTrue(applied.valid());
        assertEquals(5,XmmMovementSpeedEffectLedger.penalty(applied.effects,20));
        assertEquals("speed_effect_replayed",XmmMovementSpeedEffectLedger.apply(applied.effects,
            spec,10,20,"Lacedon Ghoul","Icy Bite","effect-1",1,0).blocked);
        XmmMovementSpeedEffectLedger.Transition sameTurn=XmmMovementSpeedEffectLedger.advance(
            applied.effects,10,"source_turn_start",1,0);
        assertEquals(1,sameTurn.effects.length());
        XmmMovementSpeedEffectLedger.Transition expired=XmmMovementSpeedEffectLedger.advance(
            applied.effects,10,"source_turn_start",2,0);
        assertEquals(1,expired.expired.length());
        assertEquals(0,XmmMovementSpeedEffectLedger.penalty(expired.effects,20));
    }

    @Test public void targetTurnPenaltyExpiresOnlyAfterItsLaterTurnEnds()throws Exception{
        JSONObject source=new JSONObject().put("speed_reduction_ft",10)
            .put("speed_reduction_duration_tick","target_turn_end");
        XmmMovementSpeedEffectLedger.Spec spec=XmmMovementSpeedEffectLedger.compiled(source);
        XmmMovementSpeedEffectLedger.Transition applied=XmmMovementSpeedEffectLedger.apply(
            new JSONArray(),spec,10,20,"Warrior Commander","Longbow","effect-2",1,0);
        assertTrue(applied.valid());
        assertEquals(10,XmmMovementSpeedEffectLedger.penalty(applied.effects,20));
        XmmMovementSpeedEffectLedger.Transition sameTurn=XmmMovementSpeedEffectLedger.advance(
            applied.effects,20,"target_turn_end",1,0);
        assertEquals(1,sameTurn.effects.length());
        XmmMovementSpeedEffectLedger.Transition expired=XmmMovementSpeedEffectLedger.advance(
            applied.effects,20,"target_turn_end",1,2);
        assertEquals(1,expired.expired.length());
        assertEquals(0,XmmMovementSpeedEffectLedger.penalty(expired.effects,20));
    }
}
