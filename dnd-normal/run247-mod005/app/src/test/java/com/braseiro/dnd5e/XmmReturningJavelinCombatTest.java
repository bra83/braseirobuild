package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** The source's returning ranged weapon remains usable after both a miss and a turn change. */
public final class XmmReturningJavelinCombatTest {
    @Test public void windJavelinReturnsAfterMissAndCanAttackNextTurn()throws Exception{
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        clearEncounter();
        StatblockTurnEngine.reset();
        try {
            assertTrue(StatblockEncounterStateEngine.spawn("skirmisher","Aarakocra Skirmisher")
                .optBoolean("spawned"));
            assertTrue(StatblockEncounterStateEngine.spawn("golem","Stone Golem")
                .optBoolean("spawned"));
            StatblockTurnEngine.setInitiative("skirmisher",20);
            StatblockTurnEngine.setInitiative("golem",10);
            assertTrue(StatblockTurnEngine.start().optBoolean("started"));
            int startingHp=StatblockEncounterStateEngine.view("golem").optInt("hp");
            JSONObject missed=StatblockCombatFlowEngine.attack("skirmisher","golem",
                "Wind Javelin",1,1,30d);
            assertTrue(missed.toString(),missed.optBoolean("resolved"));
            assertFalse(missed.toString(),missed.optBoolean("hit"));
            assertTrue(missed.toString(),missed.optBoolean("weapon_returned"));
            assertEquals(startingHp,StatblockEncounterStateEngine.view("golem").optInt("hp"));
            StatblockTurnEngine.next();
            StatblockTurnEngine.next();
            JSONObject hit=StatblockCombatFlowEngine.attack("skirmisher","golem",
                "Wind Javelin",20,20,30d);
            assertTrue(hit.toString(),hit.optBoolean("resolved"));
            assertTrue(hit.toString(),hit.optBoolean("hit"));
            assertTrue(hit.toString(),hit.optBoolean("weapon_returned"));
            assertTrue(hit.toString(),StatblockEncounterStateEngine.view("golem")
                .optInt("hp")<startingHp);
        } finally {
            StatblockTurnEngine.reset();
            clearEncounter();
        }
    }

    private static void clearEncounter()throws Exception{
        StatblockEncounterStateEngine.restore(new JSONObject()
            .put("schema","statblock_encounter_state_v1")
            .put("tokens",new JSONObject()));
    }
}
