package com.braseiro.dnd5e;

/** RAW 5e 2014/2024: difficult terrain and crawling each add one foot of cost
 * per foot traveled; multiple sources of difficult terrain do not stack.
 * Standing from prone is resolved separately by ConditionRulesEngine.
 * Pure JVM; no automatic terrain claims or unverified GM modifiers. */
public final class TacticalMovementCostRules {
    private TacticalMovementCostRules() {}
    public static int costFeet(int distanceFeet,boolean difficultTerrain,boolean crawling){
        if(distanceFeet<=0||distanceFeet>600)throw new IllegalArgumentException("invalid_tactical_distance");
        int multiplier=1+(difficultTerrain?1:0)+(crawling?1:0);
        return Math.multiplyExact(distanceFeet,multiplier);
    }
    public static String label(boolean difficultTerrain,boolean crawling){
        if(difficultTerrain&&crawling)return "Rastejar em terreno difícil (3 pés por pé)";
        if(difficultTerrain)return "Terreno difícil (2 pés por pé)";
        if(crawling)return "Rastejar (2 pés por pé)";
        return "Terreno normal (1 pé por pé)";
    }
}
