package com.braseiro.dnd5e;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Source-bound, deterministic condition rider. Never interprets free-form side effects.
 * Pure Java: AppDb persists the resulting state and receipt atomically. */
public final class MonsterConditionRiderPlan {
    private MonsterConditionRiderPlan() { }
    public interface Immunity { boolean immune(String canonicalCondition); }
    public static final class Result {
        public final String before,after,blocked;
        public final List<String> applied,immune,alreadyPresent;
        private Result(String previous,String updated,String reason,
            List<String> changed,List<String> immuneConditions,List<String> existing) {
            before=previous; after=updated; blocked=reason;
            applied=Collections.unmodifiableList(new ArrayList<String>(changed));
            immune=Collections.unmodifiableList(new ArrayList<String>(immuneConditions));
            alreadyPresent=Collections.unmodifiableList(new ArrayList<String>(existing));
        }
        public boolean valid(){return blocked.isEmpty();}
    }
    public static Result plan(String sourceConditions,String currentConditions,Immunity immunity) {
        String before=currentConditions==null?"":currentConditions;
        String after=before;
        List<String> applied=new ArrayList<String>(),blockedByImmunity=new ArrayList<String>(),present=new ArrayList<String>();
        if(sourceConditions==null||sourceConditions.trim().isEmpty())
            return new Result(before,after,"",applied,blockedByImmunity,present);
        for(String segment:sourceConditions.split("[;,]",-1)) {
            String raw=segment.trim();
            // A condition with a duration or a turn trigger requires a separate
            // duration/recovery engine; an indefinite status is never a substitute.
            if(raw.isEmpty()||raw.matches(".*[0-9].*")||raw.matches("(?i).*(?:\\buntil\\b|\\bduring\\b|\\bdurante\\b|\\bate\\b).*"))
                return new Result(before,before,"unsupported_condition_duration_or_token",
                    Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList());
            String canonical=CompiledMonsterActionGuard.canonicalCondition(raw);
            if(canonical.isEmpty())return new Result(before,before,"unsupported_secondary_condition",
                Collections.<String>emptyList(),Collections.<String>emptyList(),Collections.<String>emptyList());
            if(ConditionTokenGuard.has(after,canonical))present.add(canonical);
            else if(immunity!=null&&immunity.immune(canonical))blockedByImmunity.add(canonical);
            else {
                after=after.trim().isEmpty()?canonical:after+", "+canonical;
                applied.add(canonical);
            }
        }
        return new Result(before,after,"",applied,blockedByImmunity,present);
    }
}
