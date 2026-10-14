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
        return advantage(action,targetHp,targetMaxHp,null);
    }
    public static Result advantage(JSONObject action,int targetHp,int targetMaxHp,
        Boolean targetGrappledByAttacker){
        if(action==null)return new Result("",false);
        boolean wounded=action.optBoolean("attack_advantage_if_target_missing_hit_points",false);
        boolean grappled=action.optBoolean("attack_advantage_if_target_grappled_by_attacker",false)||
            "target_grappled_by_attacker".equals(action.optString("attack_advantage_condition",""));
        if(!wounded&&!grappled)return new Result("",false);
        if(wounded&&(targetMaxHp<=0||targetHp<0||targetHp>targetMaxHp))
            return new Result("source_advantage_target_hit_points_unverified",false);
        if(grappled&&targetGrappledByAttacker==null)
            return new Result("source_advantage_grapple_link_unverified",false);
        return new Result("",(wounded&&targetHp<targetMaxHp)||
            (grappled&&Boolean.TRUE.equals(targetGrappledByAttacker)));
    }
}
