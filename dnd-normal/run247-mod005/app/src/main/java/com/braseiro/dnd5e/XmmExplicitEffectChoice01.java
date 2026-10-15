package com.braseiro.dnd5e;

import java.util.*;

/**
 * XMM explicit_effect_choice-01, Animated Rug of Smothering:
 * /monster/31/action/0/entries/0, 5etools-src@8c026b807fac21862a309379a0c3228a0683198b.
 *
 * This is an in-memory Java 8 source-specific executor, not a combat adapter.
 * All hit decisions, dice and turn boundaries come from the authoritative host.
 */
public final class XmmExplicitEffectChoice01 {
    private XmmExplicitEffectChoice01(){}
    public static final String POINTER="/monster/31/action/0/entries/0";
    public static final int TO_HIT=5, REACH_FEET=5, ESCAPE_DC=13;
    public enum Size { TINY, SMALL, MEDIUM, LARGE, HUGE, GARGANTUAN }
    public enum Defense { NORMAL, RESISTANT, VULNERABLE, BOTH, IMMUNE }
    public enum Choice { DAMAGE, GRAPPLE }
    public enum Condition { GRAPPLED, BLINDED, RESTRAINED, SUFFOCATING, START_TURN_BLUDGEONING_2D6_PLUS_3 }
    public static final class Target {
        public final String id;
        public final int hp,maxHp;
        public final boolean creature;
        public final Size size;
        public final Defense bludgeoningDefense;
        public Target(String id,int hp,int maxHp,Size size,boolean creature,Defense defense){
            require(id!=null&&!id.trim().isEmpty()&&size!=null&&defense!=null
                &&maxHp>=0&&hp>=0&&hp<=maxHp,"invalid_target");
            this.id=id;this.hp=hp;this.maxHp=maxHp;this.size=size;
            this.creature=creature;bludgeoningDefense=defense;
        }
        Target damaged(int amount){
            return new Target(id,Math.max(0,hp-amount),maxHp,size,creature,bludgeoningDefense);
        }
    }
    public static final class Grapple {
        public final String rug,target;
        public final int escapeDc=ESCAPE_DC;
        public final Set<Condition> conditions;
        private Grapple(String rug,String target){
            this.rug=rug;this.target=target;
            conditions=Collections.unmodifiableSet(EnumSet.allOf(Condition.class));
        }
    }
    public static final class Request {
        public final String rug,target,event;
        public final long revision;
        public final int distanceFeet;
        public final boolean hit;
        public final Choice choice;
        public final int[] dice;
        public Request(String rug,String target,String event,long revision,int distanceFeet,
                boolean hit,Choice choice,int[] damageDice){
            this.rug=rug;this.target=target;this.event=event;
            this.revision=revision;this.distanceFeet=distanceFeet;this.hit=hit;
            this.choice=choice;dice=damageDice==null?new int[0]:damageDice.clone();
        }
    }
    public static final class Result {
        public final int rolledDamage,damage,hp;
        public final boolean hit,grappled;
        /** No Android or SQLite persistence. */
        public final boolean combatPersisted=false;
        private Result(boolean hit,int rolled,int dealt,int hp,boolean grappled){
            this.hit=hit;rolledDamage=rolled;damage=dealt;this.hp=hp;this.grappled=grappled;
        }
    }
    public static final class Proposal {
        private final Session owner;
        private final String rug,target,event;
        private final long revision;
        public final Result result;
        private Proposal(Session owner,String rug,String target,String event,long revision,Result result){
            this.owner=owner;this.rug=rug;this.target=target;this.event=event;
            this.revision=revision;this.result=result;
        }
    }
    public static final class DamageSplit {
        public final int rugDamage,grappledTargetDamage,remainingRugHp,remainingTargetHp;
        public final boolean transfersWhileGrappling;
        public final boolean combatPersisted=false;
        private DamageSplit(int rug,int target,int rugHp,int targetHp,boolean transferred){
            rugDamage=rug;grappledTargetDamage=target;
            remainingRugHp=rugHp;remainingTargetHp=targetHp;
            transfersWhileGrappling=transferred;
        }
    }
    public static final class Session {
        private final Map<String,Target> targets=new LinkedHashMap<>();
        private final Map<String,Grapple> grapplesByRug=new HashMap<>();
        private final Set<String> consumedEvents=new HashSet<>();
        private final Set<String> usedTicks=new HashSet<>();
        private long revision;
        public Session(Collection<Target> initial){
            require(initial!=null&&!initial.isEmpty(),"initial_targets_required");
            for(Target t:initial){
                require(t!=null&&!targets.containsKey(t.id),"duplicate_target");
                targets.put(t.id,t);
            }
        }
        public synchronized long revision(){return revision;}
        public synchronized Target target(String id){return targets.get(id);}
        public synchronized Grapple grapple(String rug){return grapplesByRug.get(rug);}
        public synchronized boolean canSmother(String rug){
            Target actor=targets.get(rug);
            return actor!=null&&actor.hp>0&&!grapplesByRug.containsKey(rug);
        }
        private void event(String event){
            require(event!=null&&!event.trim().isEmpty()&&!consumedEvents.contains(event),
                "missing_or_replayed_event");
        }
        private static int roll2d6Plus3(int[] dice){
            require(dice!=null&&dice.length==2,"2d6_damage_roll_required");
            require(dice[0]>=1&&dice[0]<=6&&dice[1]>=1&&dice[1]<=6,
                "2d6_out_of_range");
            return dice[0]+dice[1]+3;
        }
        private static int mitigate(Target t,int raw){
            Defense d=t.bludgeoningDefense;
            return DamageMitigationRules.apply(raw,d==Defense.IMMUNE,
                d==Defense.RESISTANT||d==Defense.BOTH,
                d==Defense.VULNERABLE||d==Defense.BOTH);
        }
        public synchronized Proposal propose(Request r){
            require(r!=null&&r.choice!=null,"request_and_choice_required");
            event(r.event);
            require(r.revision==revision,"stale_revision");
            Target rug=targets.get(r.rug),victim=targets.get(r.target);
            require(rug!=null&&victim!=null&&!r.rug.equals(r.target),
                "distinct_rug_and_target_required");
            require(r.distanceFeet>=0&&r.distanceFeet<=REACH_FEET,"target_out_of_reach");
            require(canSmother(r.rug),"rug_cannot_smother_while_grappling");
            int raw=0,dealt=0;
            boolean createGrapple=false;
            if(!r.hit){
                require(r.dice.length==0,"miss_must_not_roll_damage");
                require(r.choice==Choice.DAMAGE,"miss_has_no_grapple_choice");
            }else if(r.choice==Choice.GRAPPLE){
                require(r.dice.length==0,"grapple_instead_of_damage");
                require(victim.creature&&victim.size.ordinal()<=Size.MEDIUM.ordinal(),
                    "grapple_only_medium_or_smaller_creature");
                for(Grapple g:grapplesByRug.values())
                    require(!g.target.equals(r.target),"target_already_smothered");
                createGrapple=true;
            }else{
                raw=roll2d6Plus3(r.dice);
                dealt=mitigate(victim,raw);
            }
            return new Proposal(this,r.rug,r.target,r.event,revision,
                new Result(r.hit,raw,dealt,Math.max(0,victim.hp-dealt),createGrapple));
        }
        public synchronized Result commit(Proposal p){
            require(p!=null&&p.owner==this&&p.revision==revision,
                "stale_or_foreign_proposal");
            event(p.event);
            Target rug=targets.get(p.rug),victim=targets.get(p.target);
            require(rug!=null&&victim!=null&&canSmother(p.rug),"rug_or_victim_changed");
            if(p.result.grappled){
                require(!grapplesByRug.containsKey(p.rug),"rug_already_grappling");
                grapplesByRug.put(p.rug,new Grapple(p.rug,p.target));
            }
            targets.put(p.target,victim.damaged(p.result.damage));
            consumedEvents.add(p.event);revision++;
            return p.result;
        }
        /** Escape roll is evaluated outside; successful escape clears all linked riders. */
        public synchronized boolean release(String rug,boolean escapeSucceeded,String event){
            event(event);
            Grapple g=grapplesByRug.get(rug);
            require(g!=null,"no_active_smother_grapple");
            if(escapeSucceeded)grapplesByRug.remove(rug);
            consumedEvents.add(event);revision++;
            return escapeSucceeded;
        }
        /** The victim takes 2d6+3 bludgeoning at START of each of its turns. */
        public synchronized int targetTurnStart(String rug,String target,String turn,
                int[] damageDice,String event){
            event(event);
            require(turn!=null&&!turn.trim().isEmpty()
                &&!usedTicks.contains(target+"|"+turn),"tick_requires_new_target_turn");
            Grapple g=grapplesByRug.get(rug);
            require(g!=null&&g.target.equals(target),"smother_not_active_for_target");
            Target victim=targets.get(target);require(victim!=null,"victim_missing");
            int damage=mitigate(victim,roll2d6Plus3(damageDice));
            targets.put(target,victim.damaged(damage));
            usedTicks.add(target+"|"+turn);
            consumedEvents.add(event);revision++;
            return damage;
        }
        /**
         * Caller supplies already-resolved incoming damage to the RUG BEFORE
         * the rug's special 50% reduction. The rug takes floor(amount/2),
         * and the grappled target takes the SAME number of damage. The host
         * must resolve attack hit, original damage type and modifiers.
         */
        public synchronized DamageSplit rugDamaged(String rug,int resolvedIncomingDamage,
                String event){
            event(event);
            require(resolvedIncomingDamage>=0,"invalid_incoming_damage");
            Target rugTarget=targets.get(rug);require(rugTarget!=null,"rug_target_missing");
            Grapple g=grapplesByRug.get(rug);
            int rugDamage=g==null?resolvedIncomingDamage:resolvedIncomingDamage/2;
            int victimDamage=g==null?0:rugDamage;
            Target victim=g==null?null:targets.get(g.target);
            if(g!=null)require(victim!=null,"grappled_victim_missing");
            targets.put(rug,rugTarget.damaged(rugDamage));
            if(victim!=null)targets.put(victim.id,victim.damaged(victimDamage));
            if(targets.get(rug).hp==0)grapplesByRug.remove(rug);
            consumedEvents.add(event);revision++;
            return new DamageSplit(rugDamage,victimDamage,targets.get(rug).hp,
                victim==null?-1:targets.get(victim.id).hp,g!=null);
        }
        public synchronized void rugDies(String rug,String event){
            event(event);
            require(targets.containsKey(rug),"rug_missing");
            Target before=targets.get(rug);
            targets.put(rug,before.damaged(before.hp));
            grapplesByRug.remove(rug);
            consumedEvents.add(event);revision++;
        }
    }
    private static void require(boolean ok,String reason){
        if(!ok)throw new IllegalArgumentException(reason);
    }
}
