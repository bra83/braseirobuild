package com.braseiro.dnd5e;

import java.util.*;

/**
 * Source-pinned XMM attack_hit_condition-01: Mind Flayer Tentacles, Roper
 * Tentacle, Swarm of Stirges Proboscises, Tarrasque Bite.
 *
 * Pure JVM8 model with explicit hit/outcome/dice inputs, replay-safe proposals
 * and source-owned grapple ledger. Commit only changes THIS session: combat UI,
 * Android SQLite, physical tokens and turn scheduler are not integrated.
 */
public final class XmmAttackHitCondition01 {
    private XmmAttackHitCondition01() {}
    public static final String SOURCE_COMMIT="8c026b807fac21862a309379a0c3228a0683198b";
    public enum Action {
        MIND_FLAYER("/monster/300/action/0/entries/0",7,5,14,0),
        ROPER("/monster/376/action/2/entries/0",7,60,14,6),
        SWARM_STIRGES("/monster/431/action/0/entries/0",5,5,13,0),
        TARRASQUE("/monster/433/action/1/entries/0",19,15,20,0);
        public final String pointer;
        public final int toHit,reachFeet,escapeDc,roperTentacleCount;
        Action(String pointer,int toHit,int reach,int dc,int slots){
            this.pointer=pointer;this.toHit=toHit;reachFeet=reach;
            escapeDc=dc;roperTentacleCount=slots;
        }
        public static Action source(String pointer){
            for(Action a:values())if(a.pointer.equals(pointer))return a;
            throw new IllegalArgumentException("unverified_source_pointer");
        }
    }
    public enum Type { PSYCHIC, PIERCING, NECROTIC, POISON, OTHER }
    public enum Defense { NORMAL, RESISTANT, VULNERABLE, BOTH, IMMUNE }
    public enum Size { TINY, SMALL, MEDIUM, LARGE, HUGE, GARGANTUAN }
    public enum Condition { GRAPPLED, STUNNED, POISONED, RESTRAINED, NO_TELEPORT, END_TURN_NECROTIC_2D6 }
    public static final class Target {
        public final String id;
        public final int hp,maxHp;
        public final Size size;
        public final boolean creature;
        private final EnumMap<Type,Defense> defenses;
        public Target(String id,int hp,int maxHp,Size size,boolean creature,
                Map<Type,Defense> defenses){
            need(id!=null&&!id.trim().isEmpty()&&size!=null&&maxHp>=0
                &&hp>=0&&hp<=maxHp,"invalid_target");
            this.id=id;this.hp=hp;this.maxHp=maxHp;this.size=size;this.creature=creature;
            this.defenses=new EnumMap<>(Type.class);
            if(defenses!=null)this.defenses.putAll(defenses);
        }
        public Defense defense(Type t){return defenses.get(t);}
        Target damaged(int amount){return new Target(id,Math.max(0,hp-amount),maxHp,size,creature,defenses);}
    }
    public static final class Grapple {
        public final Action source;
        public final String actor,target,id;
        public final int escapeDc,roperSlot;
        public final Set<Condition> conditions;
        Grapple(Action source,String actor,String target,int slot){
            this.source=source;this.actor=actor;this.target=target;roperSlot=slot;
            escapeDc=source.escapeDc;
            id=actor+"|"+source.name()+"|"+slot+"|"+target;
            EnumSet<Condition> c=EnumSet.of(Condition.GRAPPLED);
            if(source==Action.MIND_FLAYER)c.add(Condition.STUNNED);
            if(source==Action.ROPER)c.add(Condition.POISONED);
            if(source==Action.TARRASQUE){c.add(Condition.RESTRAINED);c.add(Condition.NO_TELEPORT);}
            if(source==Action.SWARM_STIRGES)c.add(Condition.END_TURN_NECROTIC_2D6);
            conditions=Collections.unmodifiableSet(c);
        }
    }
    public static final class Request {
        public final Action action;
        public final String actor,target,event;
        public final long revision;
        public final int distanceFeet,roperSlot;
        public final boolean hit,swarmBloodied,swarmTargetInSpace;
        public final int[] damageDice;
        public Request(Action action,String actor,String target,String event,long revision,
                int distanceFeet,boolean hit,boolean swarmBloodied,
                boolean swarmTargetInSpace,int roperSlot,int[] rolledDice){
            this.action=Objects.requireNonNull(action);this.actor=actor;this.target=target;
            this.event=event;this.revision=revision;this.distanceFeet=distanceFeet;
            this.hit=hit;this.swarmBloodied=swarmBloodied;
            this.swarmTargetInSpace=swarmTargetInSpace;this.roperSlot=roperSlot;
            damageDice=rolledDice==null?new int[0]:rolledDice.clone();
        }
    }
    public static final class Result {
        public final Action action;
        public final int damage,hp;
        public final Grapple newGrapple;
        public final boolean combatPersisted=false;
        Result(Action action,int damage,int hp,Grapple grapple){
            this.action=action;this.damage=damage;this.hp=hp;newGrapple=grapple;
        }
    }
    public static final class Proposal {
        private final Session owner;
        private final String event,target;
        private final long revision;
        public final Result result;
        Proposal(Session owner,String event,String target,long revision,Result result){
            this.owner=owner;this.event=event;this.target=target;
            this.revision=revision;this.result=result;
        }
    }
    public static final class Tentacle {
        public final String actor;
        public final int slot,ac,hp;
        public final boolean destroyed;
        Tentacle(String actor,int slot,int hp,boolean destroyed){
            this.actor=actor;this.slot=slot;ac=20;this.hp=hp;this.destroyed=destroyed;
        }
    }
    public static final class Session {
        private final Map<String,Target> targets=new LinkedHashMap<>();
        private final Map<String,Grapple> grapples=new LinkedHashMap<>();
        private final Map<String,Tentacle> roperTentacles=new HashMap<>();
        private final Set<String> events=new HashSet<>();
        private final Set<String> turnTicks=new HashSet<>();
        private final Set<String> startTurnEvents=new HashSet<>();
        private long revision;
        public Session(Collection<Target> initial){
            need(initial!=null&&!initial.isEmpty(),"targets_required");
            for(Target t:initial){
                need(t!=null&&!targets.containsKey(t.id),"duplicate_or_invalid_target");
                targets.put(t.id,t);
            }
        }
        public synchronized long revision(){return revision;}
        public synchronized Target target(String id){return targets.get(id);}
        public synchronized List<Grapple> grapples(String target){
            ArrayList<Grapple> out=new ArrayList<>();
            for(Grapple g:grapples.values())if(g.target.equals(target))out.add(g);
            return Collections.unmodifiableList(out);
        }
        public synchronized Tentacle tentacle(String actor,int slot){
            need(actor!=null&&!actor.isEmpty()&&slot>=1&&slot<=6,"invalid_tentacle_id");
            Tentacle item=roperTentacles.get(actor+"|"+slot);
            return item!=null?item:new Tentacle(actor,slot,10,false);
        }
        private static int rolled(int[] dice,int count,int sides,int bonus){
            need(dice.length==count,"incorrect_dice_count");
            int sum=bonus;
            for(int die:dice){need(die>=1&&die<=sides,"out_of_range_die");sum+=die;}
            return sum;
        }
        private static int mitigated(Target t,Type type,int raw){
            Defense defense=t.defense(type);
            need(defense!=null,"typed_damage_defense_missing_"+type);
            return DamageMitigationRules.apply(raw,defense==Defense.IMMUNE,
                defense==Defense.RESISTANT||defense==Defense.BOTH,
                defense==Defense.VULNERABLE||defense==Defense.BOTH);
        }
        public synchronized Proposal propose(Request r){
            need(r!=null&&r.actor!=null&&!r.actor.trim().isEmpty()
                &&r.event!=null&&!r.event.trim().isEmpty(),"actor_event_required");
            need(!events.contains(r.event)&&r.revision==revision,"replayed_or_stale");
            Target t=targets.get(r.target);
            need(t!=null&&r.distanceFeet>=0&&r.distanceFeet<=r.action.reachFeet,
                "unknown_or_out_of_reach_target");
            Action a=r.action;
            if(a==Action.ROPER){
                need(r.roperSlot>=1&&r.roperSlot<=6,"roper_slot_1_to_6_required");
                need(!tentacle(r.actor,r.roperSlot).destroyed,"roper_tentacle_destroyed");
            }else need(r.roperSlot==0,"nonroper_has_no_tentacle_slot");
            need(a==Action.SWARM_STIRGES||!r.swarmBloodied&&!r.swarmTargetInSpace,
                "unexpected_swarm_context");
            if(!r.hit){
                need(r.damageDice.length==0,"miss_must_not_roll_damage");
                return new Proposal(this,r.event,r.target,revision,new Result(a,0,t.hp,null));
            }
            int raw=0;
            Type type=Type.OTHER;
            if(a==Action.MIND_FLAYER){
                raw=rolled(r.damageDice,4,8,4);type=Type.PSYCHIC;
            }else if(a==Action.ROPER){
                need(r.damageDice.length==0,"roper_tentacle_no_damage");
            }else if(a==Action.SWARM_STIRGES){
                raw=rolled(r.damageDice,r.swarmBloodied?1:2,10,3);type=Type.PIERCING;
            }else if(a==Action.TARRASQUE){
                raw=rolled(r.damageDice,4,12,10);type=Type.PIERCING;
            }
            int damage=a==Action.ROPER?0:mitigated(t,type,raw);
            Grapple next=null;
            boolean eligible=t.creature;
            if(a==Action.MIND_FLAYER||a==Action.SWARM_STIRGES)
                eligible=eligible&&t.size.ordinal()<=Size.MEDIUM.ordinal();
            if(a==Action.SWARM_STIRGES)eligible=eligible&&r.swarmTargetInSpace;
            if(eligible){
                next=new Grapple(a,r.actor,r.target,r.roperSlot);
                if(a!=Action.ROPER && grapples.containsKey(next.id)){
                    // A subsequent successful attack still deals its damage.
                    // It does not stack a second copy of the same grapple.
                    next=null;
                }
                if(a==Action.ROPER){
                    for(Grapple g:grapples.values())
                        need(!(g.source==Action.ROPER&&g.actor.equals(r.actor)
                            &&g.roperSlot==r.roperSlot),"roper_tentacle_slot_occupied");
                }
            }
            return new Proposal(this,r.event,r.target,revision,
                new Result(a,damage,Math.max(0,t.hp-damage),next));
        }
        public synchronized Result commit(Proposal p){
            need(p!=null&&p.owner==this&&p.revision==revision
                &&!events.contains(p.event),"stale_foreign_or_replayed_proposal");
            Target target=targets.get(p.target);need(target!=null,"target_disappeared");
            if(p.result.newGrapple!=null){
                Grapple g=p.result.newGrapple;
                need(!grapples.containsKey(g.id),"grapple_duplicate_on_commit");
                if(g.source==Action.ROPER){
                    need(!tentacle(g.actor,g.roperSlot).destroyed,"tentacle_died_since_proposal");
                    for(Grapple used:grapples.values())need(!(used.source==Action.ROPER
                        &&used.actor.equals(g.actor)&&used.roperSlot==g.roperSlot),
                        "tentacle_slot_no_longer_free");
                }
                grapples.put(g.id,g);
            }
            targets.put(p.target,target.damaged(p.result.damage));
            events.add(p.event);revision++;return p.result;
        }
        /** Explicit successful escape (or authoritative release) ends ALL linked conditions. */
        public synchronized boolean escape(String grappleId,boolean success,String event){
            need(event!=null&&!event.trim().isEmpty()&&!events.contains(event),"escape_event_replayed");
            Grapple g=grapples.get(grappleId);
            need(g!=null,"no_active_grapple");
            if(success)grapples.remove(grappleId);
            events.add(event);revision++;
            return success;
        }
        /**
         * Roper tentacle: AC20, HP10, Poison/Psychic immunity.
         * Damage never applies to the Roper itself. Destruction frees its prey.
         * No roll is performed; host supplies the resolved attack total/damage.
         */
        public synchronized Tentacle damageRoperTentacle(String actor,int slot,
                int attackTotal,Type type,int damage,String event){
            need(event!=null&&!event.trim().isEmpty()&&!events.contains(event),
                "tentacle_event_replayed");
            need(type!=null&&damage>=0&&attackTotal>=0,"invalid_tentacle_attack");
            Tentacle old=tentacle(actor,slot);
            need(!old.destroyed,"tentacle_already_destroyed");
            int applied=attackTotal>=20 && type!=Type.POISON&&type!=Type.PSYCHIC?damage:0;
            int nextHp=Math.max(0,old.hp-applied);
            Tentacle next=new Tentacle(actor,slot,nextHp,nextHp==0);
            roperTentacles.put(actor+"|"+slot,next);
            if(next.destroyed)grapples.values().removeIf(g->
                g.source==Action.ROPER&&g.actor.equals(actor)&&g.roperSlot==slot);
            events.add(event);revision++;
            return next;
        }
        /** Explicit start-of-next-Roper-turn event; authoritative clock is external. */
        public synchronized void roperTurnStart(String actor,String turnEvent){
            need(actor!=null&&!actor.trim().isEmpty()
                &&turnEvent!=null&&!turnEvent.trim().isEmpty()
                &&startTurnEvents.add(actor+"|"+turnEvent),"duplicate_roper_turn");
            for(int i=1;i<=6;i++){
                String key=actor+"|"+i;
                Tentacle t=roperTentacles.get(key);
                if(t!=null&&t.destroyed)roperTentacles.put(key,new Tentacle(actor,i,10,false));
            }
            revision++;
        }
        /** Stirge swarm deals 2d6 Necrotic at end of each grappled target's turn. */
        public synchronized int swarmEndTargetTurn(String grappleId,String turnId,
                int[] twoD6,String event){
            need(event!=null&&!event.trim().isEmpty()&&!events.contains(event)
                &&turnId!=null&&!turnId.trim().isEmpty(),"invalid_turn_or_event");
            Grapple g=grapples.get(grappleId);
            need(g!=null&&g.source==Action.SWARM_STIRGES,"not_active_stirge_grapple");
            need(!turnTicks.contains(grappleId+"|"+turnId),"duplicate_turn_damage");
            Target t=targets.get(g.target);need(t!=null,"target_missing");
            int raw=rolled(twoD6==null?new int[0]:twoD6,2,6,0);
            int amount=mitigated(t,Type.NECROTIC,raw);
            targets.put(g.target,t.damaged(amount));
            turnTicks.add(grappleId+"|"+turnId);
            events.add(event);revision++;
            return amount;
        }
    }
    private static void need(boolean value,String reason){
        if(!value)throw new IllegalArgumentException(reason);
    }
}
