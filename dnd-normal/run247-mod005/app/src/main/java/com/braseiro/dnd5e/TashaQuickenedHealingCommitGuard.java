package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;

/** Tasha optional Monk feature (2014): action, two Ki, Martial Arts die + PB.
 * Pure validation: no inferred rolls, edition upgrades, or silently free actions. */
public final class TashaQuickenedHealingCommitGuard {
    public static final String RULE_ID="tasha.class.monk.quickened_healing";
    private TashaQuickenedHealingCommitGuard() {}
    private static String key(String text) {
        String base=Normalizer.normalize(text==null?"":text,Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
        return base.trim();
    }
    public static boolean isMonk(String text) {
        String x=key(text);return x.equals("monge")||x.equals("monk");
    }
    public static int dieFaces(int monkLevel) {
        return monkLevel>=17?10:monkLevel>=11?8:monkLevel>=5?6:4;
    }
    public static int proficiencyBonus(int totalLevel) {
        return 2+(Math.max(1,Math.min(20,totalLevel))-1)/4;
    }
    public static String reject(String channel,String ruleId,String actionId,
                                boolean edition2014,String className,int monkLevel,
                                int totalLevel,int roll,int hp,int maxHp,int currentKi,
                                boolean combatRunning,boolean actorReady) {
        if(!"PLAYER_REACTION".equals(channel))return "blocked_invalid_action_channel";
        if(!RULE_ID.equals(ruleId))return "blocked_unrecognized_tasha_feature";
        if(actionId==null||actionId.length()<12||actionId.length()>75)return "blocked_invalid_action_id";
        for(int i=0;i<actionId.length();i++) {
            char c=actionId.charAt(i);
            if(!((c>='a'&&c<='z')||(c>='A'&&c<='Z')||(c>='0'&&c<='9')||c=='_'||c=='-'))
                return "blocked_invalid_action_id";
        }
        if(!edition2014)return "blocked_requires_2014_monk_optional_feature";
        if(!isMonk(className)||monkLevel<4||monkLevel>20||totalLevel<monkLevel||totalLevel>20)
            return "blocked_monk_level_or_class";
        if(roll<1||roll>dieFaces(monkLevel))return "blocked_invalid_martial_arts_die_roll";
        if(maxHp<1||hp<1||hp>=maxHp)return "blocked_no_healing_possible";
        if(currentKi<2)return "blocked_insufficient_ki";
        if(combatRunning&&!actorReady)return "blocked_not_active_monk_combat_turn";
        return "";
    }
    public static int healed(int roll,int totalLevel,int hp,int maxHp) {
        return Math.max(0,Math.min(maxHp-hp,roll+proficiencyBonus(totalLevel)));
    }
}
