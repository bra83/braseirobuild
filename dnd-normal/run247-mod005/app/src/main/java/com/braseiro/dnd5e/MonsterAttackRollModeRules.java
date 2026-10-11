package com.braseiro.dnd5e;

/** G05 / RUN228. Deterministic two-d20 contract for compiled monster attacks.
 * Sources: D&D SRD 5.1 and 5.2 attack roll, close-ranged combat and conditions.
 * Never rolls dice; always uses explicit natural faces supplied by the player.
 * A selected opposing target is a known hostile creature for the adjacent-ranged
 * rule. Other adjacent enemies are detected only from a typed directed ledger.
 */
public final class MonsterAttackRollModeRules {
    private MonsterAttackRollModeRules() {}
    public static final int NORMAL=0, ADVANTAGE=1, DISADVANTAGE=-1;
    public static final class Result {
        public final String blocked;
        public final int mode;
        public final boolean longRange, adjacentHostile, advantage, disadvantage, criticalOnHit;
        private Result(String blocked, int mode, boolean longRange, boolean adjacentHostile,
                       boolean advantage, boolean disadvantage, boolean criticalOnHit) {
            this.blocked=blocked;this.mode=mode;this.longRange=longRange;
            this.adjacentHostile=adjacentHostile;this.advantage=advantage;
            this.disadvantage=disadvantage;this.criticalOnHit=criticalOnHit;
        }
        public boolean legal(){return blocked.isEmpty();}
        public String modeName(){return mode==ADVANTAGE?"Vantagem":mode==DISADVANTAGE?"Desvantagem":"Normal";}
        public String blockDice(int first,int second){
            if(!legal())return blocked;
            if(first<1||first>20)return "invalid_first_d20";
            return mode!=NORMAL?(second<1||second>20?"attack_mode_requires_two_d20":""):
                (second!=0?"unexpected_second_d20":"");
        }
        public int effective(int first,int second){
            String reason=blockDice(first,second);
            if(!reason.isEmpty())throw new IllegalArgumentException(reason);
            return mode==ADVANTAGE?Math.max(first,second):mode==DISADVANTAGE?
                Math.min(first,second):first;
        }
    }
    private static boolean has(String text,String key){return ConditionTokenGuard.has(text,key);}
    private static boolean incapacitated(String text){return has(text,"incapacitado")||has(text,"paralisado")||
        has(text,"petrificado")||has(text,"aturdido")||has(text,"inconsciente");}
    private static Result blocked(String why){return new Result(why,NORMAL,false,false,false,false,false);}
    public static Result evaluate(String attackMode,int distanceFt,boolean longRange,
                                  String attackerConditions,String targetConditions){
        return evaluate(attackMode,distanceFt,longRange,attackerConditions,targetConditions,false);
    }
    /** Additional hostile must have been explicitly confirmed by the player
     * and checked against live tactical state by AppDb, never inferred from NPC names. */
    public static Result evaluate(String attackMode,int distanceFt,boolean longRange,
                                  String attackerConditions,String targetConditions,
                                  boolean confirmedAdditionalAdjacentHostile){
        if((!"melee".equals(attackMode)&&!"ranged".equals(attackMode))||
            distanceFt<5||distanceFt>1200||distanceFt%5!=0||
            (longRange&&!"ranged".equals(attackMode)))return blocked("invalid_attack_mode_context");
        if(confirmedAdditionalAdjacentHostile&&!"ranged".equals(attackMode))
            return blocked("additional_hostile_requires_ranged_attack");
        if(incapacitated(attackerConditions))return blocked("attacker_incapacitated");
        // Visibility of a fear source or unusual senses is not represented in
        // this geometry contract. We refuse to silently guess its roll mode.
        if(has(attackerConditions,"amedrontado"))return blocked("fear_source_visibility_requires_review");
        if(has(attackerConditions,"invisivel")||has(targetConditions,"invisivel"))
            return blocked("invisibility_senses_require_review");
        boolean targetBlind=has(targetConditions,"cego");
        boolean targetIncap=incapacitated(targetConditions);
        boolean threatened="ranged".equals(attackMode)&&
            ((distanceFt==5&&!targetBlind&&!targetIncap)||confirmedAdditionalAdjacentHostile);
        boolean advantage=targetBlind||has(targetConditions,"impedido")||
            has(targetConditions,"aturdido")||has(targetConditions,"paralisado")||
            has(targetConditions,"inconsciente")||has(targetConditions,"petrificado")||
            (has(targetConditions,"caido")&&distanceFt<=5);
        boolean disadvantage=longRange||threatened||has(attackerConditions,"cego")||
            has(attackerConditions,"envenenado")||has(attackerConditions,"impedido")||
            has(attackerConditions,"caido")||
            (has(targetConditions,"caido")&&distanceFt>5);
        // One source of advantage cancels all sources of disadvantage, and vice
        // versa. Multiple sources never grant extra d20s.
        int mode=advantage==disadvantage?NORMAL:advantage?ADVANTAGE:DISADVANTAGE;
        boolean crit=distanceFt<=5&&(has(targetConditions,"paralisado")||
            has(targetConditions,"inconsciente"));
        return new Result("",mode,longRange,threatened,advantage,disadvantage,crit);
    }
}
