package com.braseiro.dnd5e;

/** Pure concentration save and expiry rules for explicit PHB 2014/2024 profiles.
 * The caller persists the active effect and applies the returned end decision in
 * the same transaction as combat state. Each separate damage source calls resolve.
 */
public final class SpellConcentrationDurationRules {
    private SpellConcentrationDurationRules(){}
    public static final class Outcome {
        public final int dc,damage,d20,saveBonus,total;
        public final boolean saveRequired,maintained,ended,expired;
        private Outcome(int dc,int damage,int d20,int saveBonus,boolean required,
                        boolean maintained,boolean ended,boolean expired){
            this.dc=dc;this.damage=damage;this.d20=d20;this.saveBonus=saveBonus;
            this.total=required?d20+saveBonus:0;this.saveRequired=required;
            this.maintained=maintained;this.ended=ended;this.expired=expired;
        }
    }
    private static void profile(String rulesProfile){
        if(!"2014".equals(rulesProfile)&&!"2024".equals(rulesProfile))
            throw new IllegalArgumentException("explicit_2014_or_2024_profile_required");
    }
    public static int dc(String rulesProfile,int damage){
        profile(rulesProfile);
        if(damage<0||damage>1000000)throw new IllegalArgumentException("invalid_damage");
        int result=Math.max(10,damage/2);
        return "2024".equals(rulesProfile)?Math.min(30,result):result;
    }
    /** currentRound is measured at the same caster-turn boundary as castRound. */
    public static boolean expired(String rulesProfile,int castRound,int durationRounds,int currentRound){
        profile(rulesProfile);
        if(castRound<1||durationRounds<1||durationRounds>14400||currentRound<castRound)
            throw new IllegalArgumentException("invalid_concentration_timeline");
        return (long)currentRound>=(long)castRound+durationRounds;
    }
    /** d20=0 is accepted only when no save is due. Natural 1/20 have no automatic
     * failure/success on this Constitution saving throw. Incapacitation ends first. */
    public static Outcome resolve(String rulesProfile,int castRound,int durationRounds,int currentRound,
                                  int damage,int d20,int saveBonus,boolean incapacitated){
        boolean timeUp=expired(rulesProfile,castRound,durationRounds,currentRound);
        int targetDc=dc(rulesProfile,damage);
        if(saveBonus< -50||saveBonus>100)throw new IllegalArgumentException("invalid_save_bonus");
        if(timeUp||incapacitated)return new Outcome(targetDc,damage,0,saveBonus,false,false,true,timeUp);
        if(damage==0){
            if(d20!=0)throw new IllegalArgumentException("save_not_due");
            return new Outcome(targetDc,damage,0,saveBonus,false,true,false,false);
        }
        if(d20<1||d20>20)throw new IllegalArgumentException("invalid_d20");
        boolean saved=d20+saveBonus>=targetDc;
        return new Outcome(targetDc,damage,d20,saveBonus,true,saved,!saved,false);
    }
}
