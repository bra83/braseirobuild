package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Independent, explicit expected values from:
 * https://github.com/BTMorton/dnd-5e-srd/blob/004cc01b5e6228b7eafcae29830cd2d9d47584db/json/06%20mechanics.json
 * OGL SRD5 (2014). Includes combat examples from JSON 07 combat.json
 * (SHA 8498fb3a779d7bc781d059e48ab66f77c0e8ed12).
 * This checks the 2014 compatibility profile ONLY and does not certify
 * revised 2024/2025 rulings or any supplemental book section.
 */
public final class Srd5OriginalMechanicsJsonContractTest {
    private final DndRulesEngine dnd = new DndRulesEngine();

    @Test public void srd2014AbilityScoresUseRoundedDownHalfDifference() {
        assertEquals(-5,dnd.modifier(1));
        assertEquals(-1,dnd.modifier(8));
        assertEquals(-1,dnd.modifier(9));
        assertEquals(0,dnd.modifier(10));
        assertEquals(0,dnd.modifier(11));
        assertEquals(1,dnd.modifier(12));
        assertEquals(1,dnd.modifier(13));
        assertEquals(5,dnd.modifier(20));
        assertEquals(10,dnd.modifier(30));
    }

    @Test public void srd2014AllSixAbilityAbbreviationsAndLongNamesResolve() throws Exception {
        JSONObject hero=new JSONObject().put("str",18).put("dex",16)
                .put("con",14).put("int_score",12).put("wis",10).put("cha",8);
        final String[] abbreviations={"str","dex","con","int","wis","cha"};
        final String[] longNames={"Strength","Dexterity","Constitution",
                "Intelligence","Wisdom","Charisma"};
        final int[] expected={18,16,14,12,10,8};
        for(int i=0;i<abbreviations.length;i++){
            assertEquals(abbreviations[i],expected[i],dnd.abilityScore(hero,abbreviations[i]));
            assertEquals(longNames[i],expected[i],dnd.abilityScore(hero,longNames[i]));
        }
        assertEquals(15,dnd.abilityScore(new JSONObject().put("int",15),"INT"));
    }

    @Test public void srd2014ProficiencyProgressesAtLevelsFiveNineThirteenSeventeen() {
        final int[] levels={1,4,5,8,9,12,13,16,17,20};
        final int[] expected={2,2,3,3,4,4,5,5,6,6};
        for(int i=0;i<levels.length;i++)
            assertEquals("Character level "+levels[i],expected[i],dnd.proficiencyBonus(levels[i]));
    }

    @Test public void srd2014PassiveCheckHasPlusFiveMinusFiveAndCancellation() throws Exception {
        JSONObject character=new JSONObject().put("wis",14).put("level",1);
        assertEquals(12,dnd.passiveScore(character,"wis","Perception"));
        assertEquals(17,dnd.passiveScore(character,"wis","Perception",true,false));
        assertEquals(7,dnd.passiveScore(character,"wis","Perception",false,true));
        assertEquals(12,dnd.passiveScore(character,"wis","Perception",true,true));
        assertEquals(12,dnd.passiveScore(character,"wis","Perception",false,false));
    }

    @Test public void srd2014AdvantageChoosesHighDisadvantageChoosesLow() {
        assertEquals(17,dnd.chooseD20(17,5,true,false));
        assertEquals(5,dnd.chooseD20(17,5,false,true));
        assertEquals(17,dnd.chooseD20(17,5,true,true));
        assertEquals(17,dnd.chooseD20(17,5,false,false));
    }

    @Test public void srd2014CarryingCapacityAndPushDragLiftUseStrengthMultiplier() {
        assertEquals(150,dnd.carryingCapacity(10));
        assertEquals(300,dnd.pushDragLiftCapacity(10));
        assertEquals(270,dnd.carryingCapacity(18));
        assertEquals(540,dnd.pushDragLiftCapacity(18));
    }

    @Test public void srd2014SizeAdjustmentsToStrengthCapacityFollowSrdJson() {
        assertEquals(75,dnd.carryingCapacity(10,"Tiny"));
        assertEquals(150,dnd.carryingCapacity(10,"Small"));
        assertEquals(150,dnd.carryingCapacity(10,"Medium"));
        assertEquals(300,dnd.carryingCapacity(10,"Large"));
        assertEquals(600,dnd.carryingCapacity(10,"Huge"));
        assertEquals(1200,dnd.carryingCapacity(10,"Gargantuan"));
        assertEquals(600,dnd.pushDragLiftCapacity(10,"Large"));
        assertEquals(75,dnd.carryingCapacity(10,"Miúdo"));
        boolean blocked=false;
        try{dnd.carryingCapacity(10,"unknown size");}
        catch(IllegalArgumentException expected){blocked=true;}
        assertTrue("Unknown creature size must fail closed",blocked);
    }

    @Test public void srd2014CombatNaturalOneMissesAndTwentyAlwaysHits() {
        JSONObject automaticMiss=dnd.resolveAttack(1,100,1,false,false);
        assertFalse(automaticMiss.optBoolean("hit",true));
        assertTrue(automaticMiss.optBoolean("automatic_miss"));
        JSONObject automaticHit=dnd.resolveAttack(20,-5,50,false,false);
        assertTrue(automaticHit.optBoolean("hit"));
        assertTrue(automaticHit.optBoolean("critical"));
    }

    @Test public void srd2014DeathSavesRollOneCountsTwoFailuresRollTwentyRegainsOneHp() {
        JSONObject criticalFailure=dnd.resolveDeathSave(1,0,0);
        assertEquals(2,criticalFailure.optInt("failures"));
        assertFalse(criticalFailure.optBoolean("dead"));
        JSONObject stable=dnd.resolveDeathSave(10,2,0);
        assertTrue(stable.optBoolean("stable"));
        JSONObject revived=dnd.resolveDeathSave(20,1,2);
        assertEquals(1,revived.optInt("regains_hp"));
        assertEquals(0,revived.optInt("successes"));
        assertEquals(0,revived.optInt("failures"));
    }

    @Test public void srd2014ConcentrationUsesTenOrHalfDamageWhicheverHigher() {
        assertEquals(10,dnd.concentrationDc(10,DndRulesEngine.PROFILE_2014));
        assertEquals(10,dnd.concentrationDc(20,DndRulesEngine.PROFILE_2014));
        assertEquals(25,dnd.concentrationDc(50,DndRulesEngine.PROFILE_2014));
        assertEquals(40,dnd.concentrationDc(80,DndRulesEngine.PROFILE_2014));
        assertEquals(30,dnd.concentrationDc(80,DndRulesEngine.PROFILE_2024));
    }
}
