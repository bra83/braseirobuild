package com.braseiro.dnd5e;

/** D&D 5e (2014/2024) saving throws vs. area damage. Pure deterministic rules.
 * No automatic natural-1/20 outcome on ability saving throws. Resistance and
 * vulnerability are applied later by the existing combat damage engine.
 * This is not a spell caster/slot/targeting engine. */
public final class AreaSaveDamageRules {
    private AreaSaveDamageRules() {}
    public static final String HALF = "half";
    public static final String NONE = "none";
    public static final String FULL = "full";
    public static final class Outcome {
        public final int rawDamage, damage, natural, bonus, total, dc;
        public final boolean saved, evasionUsed;
        Outcome(int raw,int damage,int natural,int bonus,int total,int dc,boolean saved,boolean evasion){
            this.rawDamage=raw;this.damage=damage;this.natural=natural;this.bonus=bonus;
            this.total=total;this.dc=dc;this.saved=saved;this.evasionUsed=evasion;
        }
    }
    public static boolean validAbility(String ability){
        return "str".equals(ability)||"dex".equals(ability)||"con".equals(ability)||
            "int".equals(ability)||"wis".equals(ability)||"cha".equals(ability);
    }
    public static boolean validSuccessPolicy(String policy){
        return HALF.equals(policy)||NONE.equals(policy)||FULL.equals(policy);
    }
    public static Outcome resolve(int rawDamage,String saveAbility,int dc,int natural,int bonus,
                                  String successPolicy,boolean hasEvasion,boolean incapacitated){
        if(rawDamage<0||rawDamage>1000000)throw new IllegalArgumentException("invalid_raw_damage");
        if(!validAbility(saveAbility))throw new IllegalArgumentException("invalid_save_ability");
        if(dc<1||dc>40)throw new IllegalArgumentException("invalid_save_dc");
        if(natural<1||natural>20)throw new IllegalArgumentException("invalid_d20");
        if(bonus< -50||bonus>100)throw new IllegalArgumentException("invalid_save_bonus");
        if(!validSuccessPolicy(successPolicy))throw new IllegalArgumentException("invalid_success_policy");
        int total=natural+bonus;
        boolean success=total>=dc;
        int amount=success?(NONE.equals(successPolicy)?0:HALF.equals(successPolicy)?rawDamage/2:rawDamage):rawDamage;
        boolean evasion=hasEvasion&&!incapacitated&&"dex".equals(saveAbility)&&HALF.equals(successPolicy);
        if(evasion)amount=success?0:rawDamage/2;
        return new Outcome(rawDamage,amount,natural,bonus,total,dc,success,evasion);
    }
}
