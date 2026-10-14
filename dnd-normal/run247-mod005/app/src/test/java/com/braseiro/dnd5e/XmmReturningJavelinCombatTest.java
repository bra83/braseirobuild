package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/** The source's returning ranged weapon remains usable after both a miss and a turn change. */
public final class XmmReturningJavelinCombatTest {
    @Test public void sourceBoundSimpleReturnWeaponsReturnOnlyAfterRangedAttacks()throws Exception{
        String[][] weapons={
            {"Kenku","Shadow Blade"},{"Mezzoloth","Mercurial Trident"},
            {"Modron Duodrone","Clockwork Blade"},{"Modron Tridrone","Clockwork Spear"},
            {"Nycaloth","Mercurial Axe"},{"Salamander","Flame Spear"},
            {"Salamander Inferno Master","Flame Trident"}
        };
        assertEquals(503,Xmm2025CompiledBestiaryEngine.install().optInt("statblocks_installed"));
        int ordinal=0;
        for(String[] weapon:weapons){
            clearEncounter();StatblockTurnEngine.reset();
            String actor="returning-weapon-"+ordinal,target="target-"+ordinal;
            try{
                assertTrue(weapon[0],StatblockEncounterStateEngine.spawn(actor,weapon[0])
                    .optBoolean("spawned"));
                assertTrue("Stone Golem",StatblockEncounterStateEngine.spawn(target,"Stone Golem")
                    .optBoolean("spawned"));
                StatblockTurnEngine.setInitiative(actor,20);StatblockTurnEngine.setInitiative(target,10);
                assertTrue(StatblockTurnEngine.start().optBoolean("started"));
                JSONObject rangedMiss=StatblockCombatFlowEngine.attack(actor,target,weapon[1],1,1,30d);
                assertTrue(weapon[0]+" / "+weapon[1]+": "+rangedMiss,
                    rangedMiss.optBoolean("resolved"));
                assertFalse(rangedMiss.optBoolean("hit"));
                assertTrue(weapon[0]+" / "+weapon[1],rangedMiss.optBoolean("weapon_returned"));
                StatblockTurnEngine.next();StatblockTurnEngine.next();
                JSONObject meleeMiss=StatblockCombatFlowEngine.attack(actor,target,weapon[1],1,1,0d);
                assertTrue(weapon[0]+" / "+weapon[1]+": "+meleeMiss,
                    meleeMiss.optBoolean("resolved"));
                assertFalse(weapon[0]+" / "+weapon[1],meleeMiss.optBoolean("weapon_returned"));
            }finally{
                StatblockTurnEngine.reset();clearEncounter();
            }
            ordinal++;
        }
    }

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
