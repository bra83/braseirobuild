package com.braseiro.dnd5e;

/** RUN197: domain-only input and replay policy for a manually approved area effect.
 * Does not roll dice, choose targets, mutate state or infer resources. */
public final class CombatBatchPlanGuard {
    private CombatBatchPlanGuard(){}
    public static boolean validResource(String resource){
        return "none".equals(resource)||"action".equals(resource)||
            "bonus_action".equals(resource)||"reaction".equals(resource);
    }
    public static boolean validBatchSize(int count){return count>=1&&count<=64;}
    public static boolean validTarget(long id,int amount){return id>0&&amount>=0&&amount<=1000000;}
    public static boolean replayConsistent(int replayed,int total,boolean actorCharged,boolean actorReplayed){
        if(!validBatchSize(total)||replayed<0||replayed>total)return false;
        if(replayed!=0&&replayed!=total)return false;
        return !actorCharged||(actorReplayed==(replayed==total));
    }
}
