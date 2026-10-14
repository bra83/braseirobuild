package com.braseiro.dnd5e;

import org.json.JSONObject;

/** Source-gated attack roll modifiers which depend on live target state. */
public final class XmmConditionalAttackRollRules {
    private XmmConditionalAttackRollRules() { }
    public static final class Result {
        public final String blocked;
        public final boolean advantage;
        private Result(String blocked,boolean advantage){this.blocked=blocked;this.advantage=advantage;}
        public boolean legal(){return blocked.isEmpty();}
    }
    public static Result advantage(JSONObject action,int targetHp,int targetMaxHp){
        if(action==null||!action.optBoolean("attack_advantage_if_target_missing_hit_points",false))
            return new Result("",false);
        if(targetMaxHp<=0||targetHp<0||targetHp>targetMaxHp)
            return new Result("source_advantage_target_hit_points_unverified",false);
        return new Result("",targetHp<targetMaxHp);
    }
}
