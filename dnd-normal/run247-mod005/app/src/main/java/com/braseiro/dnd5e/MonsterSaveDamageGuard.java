package com.braseiro.dnd5e;

/** Validates submitted damage against a source-compiled saving throw action.
 * Rolls remain manual: bounds prove legality, not the underlying physical dice. */
public final class MonsterSaveDamageGuard {
    private MonsterSaveDamageGuard() { }
    public static boolean accepts(String failureExpression,String successExpression,
        boolean saveSucceeded,int submittedDamage) {
        if(submittedDamage<0)return false;
        String full=failureExpression==null?"":failureExpression.trim();
        String success=successExpression==null?"":successExpression.trim();
        if(!saveSucceeded)return CompiledMonsterDamageGuard.accepts(full,"",submittedDamage,false);
        if(success.isEmpty())return submittedDamage==0;
        if("half".equalsIgnoreCase(success)) {
            if(full.isEmpty())return false;
            CompiledMonsterDamageGuard.Range range=CompiledMonsterDamageGuard.range(full,"",false);
            return range!=null&&submittedDamage>=range.min/2&&submittedDamage<=range.max/2;
        }
        return CompiledMonsterDamageGuard.accepts(success,"",submittedDamage,false);
    }
}
