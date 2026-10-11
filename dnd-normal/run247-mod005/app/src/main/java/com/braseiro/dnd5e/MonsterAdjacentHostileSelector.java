package com.braseiro.dnd5e;

/** RUN231/G05. Deterministic selection from an EXPLICIT, directed hostility ledger.
 * Never guesses a faction. Only living, tokenized, adjacent creatures that can
 * see the attacker and are not incapacitated qualify. Missing tokens are ignored;
 * uncertain special senses fail closed instead of silently removing disadvantage.
 */
public final class MonsterAdjacentHostileSelector {
    private MonsterAdjacentHostileSelector() { }
    public static final class Candidate {
        public final long id;
        public final int x,y,hp;
        public final String conditions;
        public Candidate(long id,int x,int y,int hp,String conditions) {
            this.id=id;this.x=x;this.y=y;this.hp=hp;this.conditions=conditions==null?"":conditions;
        }
    }
    public static final class Result {
        public final long hostileId;
        public final String blocked;
        private Result(long id,String reason){hostileId=id;blocked=reason;}
        public boolean valid(){return blocked.isEmpty();}
    }
    public static Result select(int width,int height,int ax,int ay,boolean[][] opaque,
        String attackerConditions,long actorId,long targetId,long[] declared,Candidate[] candidates){
        if(actorId<=0||targetId<=0||actorId==targetId||declared==null||candidates==null)
            return new Result(0,"invalid_hostility_selection");
        try{MonsterHostilityLedgerRules.canonical(actorId,declared);}
        catch(IllegalArgumentException invalid){return new Result(0,"invalid_hostility_selection");}
        long selected=0;
        for(long id:declared){
            if(id==targetId)continue; // Target is already handled by attack-roll rules.
            Candidate candidate=null;
            for(Candidate item:candidates)if(item!=null&&item.id==id){
                if(candidate!=null)return new Result(0,"duplicate_hostile_token");
                candidate=item;
            }
            if(candidate==null||candidate.hp<=0)continue;
            if(Math.max(Math.abs(ax-candidate.x),Math.abs(ay-candidate.y))!=1)continue;
            String reason=MonsterAdjacentHostileRules.check(width,height,ax,ay,
                candidate.x,candidate.y,opaque,attackerConditions,candidate.conditions,true);
            if("attacker_visibility_requires_special_senses_review".equals(reason))
                return new Result(0,reason);
            if(reason.isEmpty()&&(selected==0||id<selected))selected=id;
            else if(!reason.isEmpty()&&!(reason.equals("additional_hostile_line_of_sight_blocked")||
                reason.equals("additional_hostile_cannot_see_attacker")||
                reason.equals("additional_hostile_incapacitated")||
                reason.equals("additional_hostile_not_within_five_feet")))
                return new Result(0,reason);
        }
        return new Result(selected,"");
    }
}
