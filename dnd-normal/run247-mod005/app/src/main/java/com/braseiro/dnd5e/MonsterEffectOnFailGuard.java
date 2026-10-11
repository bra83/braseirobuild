package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.List;

/**
 * A compiled statblock may repeat its condition in effect_on_fail. That is not
 * an additional effect: applying the condition once is sufficient. Everything
 * else (grapple, movement, max HP, multi-effects, prose) remains source-gated.
 */
public final class MonsterEffectOnFailGuard {
    private MonsterEffectOnFailGuard() { }

    public static boolean supported(String declaredConditions,String effect) {
        if(effect==null||effect.trim().isEmpty())return true;
        if(declaredConditions==null||declaredConditions.trim().isEmpty())return false;
        List<String> declared=canonicalTokens(declaredConditions);
        List<String> repeated=canonicalTokens(effect);
        return !declared.isEmpty()&&declared.equals(repeated);
    }

    private static List<String> canonicalTokens(String value) {
        List<String> result=new ArrayList<String>();
        for(String segment:value.split("[;,]",-1)) {
            String raw=segment.trim();
            // CanonicalCondition accepts metadata in some contexts; do not
            // accept metadata or prose as an equivalent effect declaration.
            if(raw.isEmpty()||raw.matches(".*[0-9].*")||
               raw.matches("(?i).*(?:\\buntil\\b|\\bduring\\b|\\bdurante\\b|\\bwhen\\b|\\bafter\\b).*"))
                return new ArrayList<String>();
            String condition=CompiledMonsterActionGuard.canonicalCondition(raw);
            if(condition.isEmpty()||result.contains(condition))return new ArrayList<String>();
            result.add(condition);
        }
        java.util.Collections.sort(result);
        return result;
    }
}
