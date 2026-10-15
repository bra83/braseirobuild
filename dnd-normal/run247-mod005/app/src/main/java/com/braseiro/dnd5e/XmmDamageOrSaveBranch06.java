package com.braseiro.dnd5e;

import java.util.*;

/**
 * XMM source-pinned damage_or_save_branch-06: Wraith's Life Drain only.
 * Pure Java 8. This executor does not roll dice, decide hits, access Android,
 * persist SQLite, spawn Specters, or apply state to a live combatant.
 *
 * Source: 5etools-src@8c026b807fac21862a309379a0c3228a0683198b
 * path: /monster/481/action/0/entries/0
 * "Create Specter" is a different action and intentionally OUT OF SCOPE.
 */
public final class XmmDamageOrSaveBranch06 {
    private XmmDamageOrSaveBranch06() {}

    public static final String SOURCE_POINTER="/monster/481/action/0/entries/0";
    public static final int ATTACK_TO_HIT=6;
    public static final int ATTACK_REACH_FEET=5;
    public static final String DAMAGE_TYPE="necrotic";
    public static final String DAMAGE_FORMULA="4d8 + 3";

    public enum Defense { NORMAL, RESISTANT, VULNERABLE, BOTH, IMMUNE }
    public enum Kind { CREATURE, OBJECT }

    public static final class Target {
        public final String id;
        public final Kind kind;
        public final int hp,maxHp;
        public final Defense necroticDefense;
        public Target(String id,Kind kind,int hp,int maxHp,Defense defense) {
            check(id!=null&&!id.trim().isEmpty()&&kind!=null&&defense!=null,
                "missing_target_identity_or_defense");
            check(maxHp>=0&&hp>=0&&hp<=maxHp,"invalid_hp");
            this.id=id;this.kind=kind;this.hp=hp;this.maxHp=maxHp;
            necroticDefense=defense;
        }
        Target after(Result r){return new Target(id,kind,r.hp,r.maxHp,necroticDefense);}
    }

    public static final class Request {
        public final String sourcePointer,actor,target,event;
        public final long revision;
        public final int distanceFeet;
        public final Boolean hit;
        public final int[] damageDice;
        public Request(String pointer,String actor,String target,String event,
                long revision,int distanceFeet,Boolean hit,int[] dice) {
            sourcePointer=pointer;this.actor=actor;this.target=target;this.event=event;
            this.revision=revision;this.distanceFeet=distanceFeet;this.hit=hit;
            damageDice=dice==null?new int[0]:dice.clone();
        }
    }

    public static final class Result {
        public final int rolledDamage,damageTaken,hp,maxHp,maxHpLost;
        public final boolean attackHit;
        /** JVM-only proposal and commit, not Android/SQLite persistence. */
        public final boolean persistedToCombat=false;
        private Result(boolean hit,int rolled,int damage,int hp,int maxHp,int loss) {
            attackHit=hit;rolledDamage=rolled;damageTaken=damage;this.hp=hp;
            this.maxHp=maxHp;maxHpLost=loss;
        }
    }
    public static final class Proposal {
        private final Session session;
        private final long revision;
        private final String target,event;
        public final Result result;
        private Proposal(Session session,long revision,String target,String event,Result result) {
            this.session=session;this.revision=revision;this.target=target;
            this.event=event;this.result=result;
        }
    }

    public static final class Session {
        private final Map<String,Target> targets=new LinkedHashMap<>();
        private final Set<String> consumedEvents=new HashSet<>();
        private long revision;

        public Session(Collection<Target> initial) {
            check(initial!=null&&!initial.isEmpty(),"targets_required");
            for(Target t:initial) {
                check(t!=null&&!targets.containsKey(t.id),"duplicate_target");
                targets.put(t.id,t);
            }
        }
        public synchronized long revision(){return revision;}
        public synchronized Target target(String id){return targets.get(id);}

        public synchronized Proposal propose(Request request) {
            check(request!=null&&SOURCE_POINTER.equals(request.sourcePointer),
                "source_pointer_not_pinned");
            check(request.actor!=null&&!request.actor.trim().isEmpty()
                    &&request.event!=null&&!request.event.trim().isEmpty(),
                "missing_actor_or_event");
            check(request.revision==revision&&!consumedEvents.contains(request.event),
                "stale_or_replayed_event");
            Target target=targets.get(request.target);
            check(target!=null,"target_not_found");
            check(request.distanceFeet>=0&&request.distanceFeet<=ATTACK_REACH_FEET,
                "target_out_of_reach");
            check(request.hit!=null,"attack_hit_result_required");

            if(!request.hit) {
                check(request.damageDice.length==0,"miss_must_not_roll_damage");
                return new Proposal(this,revision,request.target,request.event,
                    new Result(false,0,0,target.hp,target.maxHp,0));
            }
            check(request.damageDice.length==4,"wraith_requires_4d8");
            int raw=3;
            for(int die:request.damageDice) {
                check(die>=1&&die<=8,"invalid_d8_roll");
                raw+=die;
            }
            Defense defense=target.necroticDefense;
            int taken=DamageMitigationRules.apply(raw,
                defense==Defense.IMMUNE,
                defense==Defense.RESISTANT||defense==Defense.BOTH,
                defense==Defense.VULNERABLE||defense==Defense.BOTH);
            int hp=Math.max(0,target.hp-taken);
            // Per source, maximum HP decreases by damage taken, not a second
            // damage roll or the base 4d8+3 before resistances.
            int loss=target.kind==Kind.CREATURE?taken:0;
            int maxHp=Math.max(0,target.maxHp-loss);
            hp=Math.min(hp,maxHp);
            return new Proposal(this,revision,request.target,request.event,
                new Result(true,raw,taken,hp,maxHp,loss));
        }

        public synchronized Result commit(Proposal p) {
            check(p!=null&&p.session==this&&p.revision==revision
                &&!consumedEvents.contains(p.event),"stale_foreign_or_replayed_proposal");
            Target t=targets.get(p.target);
            check(t!=null,"target_not_found");
            targets.put(p.target,t.after(p.result));
            consumedEvents.add(p.event);
            revision++;
            return p.result;
        }
    }

    private static void check(boolean yes,String reason) {
        if(!yes)throw new IllegalArgumentException(reason);
    }
}
