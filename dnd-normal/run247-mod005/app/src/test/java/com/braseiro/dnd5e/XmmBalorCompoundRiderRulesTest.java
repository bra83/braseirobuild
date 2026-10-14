package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** Balor's Flame Whip applies its pull and persistent Prone under one size gate. */
public final class XmmBalorCompoundRiderRulesTest {
    @Test public void onlyTheSourceBoundHugeOrSmallerCompoundRiderIsAccepted()throws Exception{
        JSONObject action=new JSONObject().put("to_hit",14).put("effect_on_hit","pull:25ft;prone")
            .put("effect_on_hit_max_target_size","Huge").put("condition_on_hit","prone")
            .put("condition_ends_when","stands_up").put("condition_max_target_size","Huge");
        assertEquals("",MonsterActionEconomyGuard.attackRiderBlock(action));
        assertTrue(MonsterOnHitProneRules.validSource(action));
        assertTrue(MonsterOnHitProneRules.eligible(action,"Large"));
        assertFalse(MonsterOnHitProneRules.eligible(action,"Gargantuan"));
        assertNull(MonsterForcedMovementRules.compiled("pull:25ft;prone","restrained"));
        MonsterForcedMovementRules.Spec infernalChain=
            MonsterForcedMovementRules.compiled("pull:30ft;restrained","restrained");
        assertNotNull(infernalChain);
        assertEquals("pull",infernalChain.direction);
        assertEquals(30,infernalChain.maximumFeet);
        assertEquals("restrained",infernalChain.accompanyingCondition);
        assertNull(MonsterForcedMovementRules.compiled("pull:30ft;restrained","prone"));
        action.put("effect_on_hit_max_target_size","Gargantuan");
        assertEquals("unsupported_untimed_on_hit_rider",
            MonsterActionEconomyGuard.attackRiderBlock(action));
    }
}
