package com.braseiro.dnd5e;

/** Pure deterministic D&D 5e nonlethal melee knockout eligibility. No RNG, AI or persistence. */
public final class NonlethalKnockoutRules {
    private NonlethalKnockoutRules(){}
    public static boolean qualifies(boolean declaredMeleeNonlethal,int hpBefore,int hpAfter,boolean alreadyDead){
        return declaredMeleeNonlethal && hpBefore>0 && hpAfter==0 && !alreadyDead;
    }
    public static int resultingHitPoints(boolean knockout,int ordinaryAfter){return knockout?0:Math.max(0,ordinaryAfter);}
    public static boolean stableOnKnockout(boolean knockout){return knockout;}
    public static boolean requiresRecoveryRoll(boolean knockout){return knockout;}
}
