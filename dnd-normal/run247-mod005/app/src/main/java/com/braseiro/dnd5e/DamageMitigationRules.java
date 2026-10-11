package com.braseiro.dnd5e;

/** Deterministic D&D 5e damage-type modifier. Pure Java, no AI or database. */
public final class DamageMitigationRules {
    private DamageMitigationRules(){}
    public static int apply(int raw,boolean immune,boolean resistant,boolean vulnerable){
        int safe=Math.max(0,raw);
        if(immune)return 0;
        // PHB 2024 p.28: resistance (floor) precedes vulnerability (double).
        if(resistant&&vulnerable)return (safe/2)*2;
        if(resistant)return safe/2;
        if(vulnerable)return safe>Integer.MAX_VALUE/2?Integer.MAX_VALUE:safe*2;
        return safe;
    }
}
