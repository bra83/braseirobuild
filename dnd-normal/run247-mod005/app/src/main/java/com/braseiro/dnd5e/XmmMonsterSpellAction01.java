package com.braseiro.dnd5e;

import java.util.*;

/**
 * XMM monster_spell_action-01: Blue Slaad / Mutating Claw.
 *
 * Source pointer /monster/71/action/1/entries/0, upstream 5etools-src
 * 8c026b807fac21862a309379a0c3228a0683198b.
 *
 * "Wish" in the source is the ONLY SPELL which reverses a transformation;
 * this action does NOT cast Wish or grant spellcasting abilities.
 *
 * Pure in-memory Java8 rules executor. Android combat/SQLite, clocks,
 * spell-slot adjudication and stat-block replacement require external
 * adapters. Host provides all dice, attack/save outcomes and 24h ticks.
 */
public final class XmmMonsterSpellAction01 {
    private XmmMonsterSpellAction01(){}
    public static final String SOURCE_POINTER="/monster/71/action/1/entries/0";
    public static final int TO_HIT=8,REACH_FEET=10,CON_SAVE_DC=15;
    public static final String ONLY_REVERSAL_SPELL="Wish";

    public enum Type { SLASHING, POISON }
    public enum Defense { NORMAL, RESISTANT, VULNERABLE, BOTH, IMMUNE }
    public enum Kind { HUMANOID, CREATURE, OBJECT }
    public enum State { NORMAL, CURSED, PENDING_RED_SLAAD, PENDING_GREEN_SLAAD, WISH_REVERSAL_PENDING }
    public enum Rider {
        SLAAD_CURSE_BLOCKS_HEALING, MAX_HP_MINUS_3D6_PER_24H,
        MAX_HP_NOT_RESTORED_AFTER_LONG_REST,
        CURSE_ENDS_AT_ZERO_MAX_HP,
        REPLACE_STAT_BLOCK_WITH_RED_SLAAD,
        REPLACE_STAT_BLOCK_WITH_GREEN_SLAAD,
        ONLY_WISH_REVERSES_SLAAD_TRANSFORMATION
    }

    public static final class Target {
        public final String id;
        public final Kind kind;
        public final int hp,maxHp;
        public final boolean canCastSpellsLevel3Plus;
        private final EnumMap<Type,Defense> defense;
        public Target(String id,Kind kind,int hp,int maxHp,boolean cast3Plus,
                Map<Type,Defense> defenses){
            require(id!=null&&!id.trim().isEmpty()&&kind!=null&&maxHp>=0
                &&hp>=0&&hp<=maxHp,"invalid_target");
            this.id=id;this.kind=kind;this.hp=hp;this.maxHp=maxHp;
            canCastSpellsLevel3Plus=cast3Plus;
            defense=new EnumMap<>(Type.class);
            if(defenses!=null)defense.putAll(defenses);
        }
        Defense defense(Type kind){return defense.get(kind);}
        Target withStats(int hp,int max){
            return new Target(id,kind,hp,max,canCastSpellsLevel3Plus,defense);
        }
    }
    public static final class Effect {
        public final Rider rider;
        public final String actor,target,trigger;
        public final int magnitude;
        Effect(Rider r,String actor,String target,int amount,String trigger){
            rider=r;this.actor=actor;this.target=target;
            magnitude=amount;this.trigger=trigger;
        }
    }
    public static final class Request {
        public final String actor,target,event;
        public final long revision;
        public final int distanceFt;
        public final Boolean hit,conSaveSucceeded;
        public final int[] slashingDice,poisonDice;
        public Request(String actor,String target,String event,long revision,int distanceFt,
                Boolean hit,Boolean conSaveSucceeded,int[] slashing,int[] poison){
            this.actor=actor;this.target=target;this.event=event;
            this.revision=revision;this.distanceFt=distanceFt;this.hit=hit;
            this.conSaveSucceeded=conSaveSucceeded;
            slashingDice=slashing==null?new int[0]:slashing.clone();
            poisonDice=poison==null?new int[0]:poison.clone();
        }
    }
    public static final class Result {
        public final int slashingDamage,poisonDamage,totalDamage,hp,maxHp;
        public final boolean createsCurse;
        public final List<Effect> pendingEffects;
        /** A result is not a persisted Android-combat receipt. */
        public final boolean integratedIntoCombat=false;
        Result(int slash,int poison,int hp,int max,boolean curse,List<Effect> effects){
            slashingDamage=slash;poisonDamage=poison;totalDamage=slash+poison;
            this.hp=hp;this.maxHp=max;createsCurse=curse;
            pendingEffects=Collections.unmodifiableList(new ArrayList<>(effects));
        }
    }
    public static final class Proposal {
        private final Session owner;
        private final String actor,event,target;
        private final long revision;
        public final Result result;
        Proposal(Session owner,String actor,String event,String target,long rev,Result result){
            this.owner=owner;this.actor=actor;this.event=event;this.target=target;
            revision=rev;this.result=result;
        }
    }
    public static final class Curse {
        public final String source,target;
        public final int lastTickIndex;
        Curse(String source,String target,int lastTick){
            this.source=source;this.target=target;lastTickIndex=lastTick;
        }
    }
    public static final class Tick {
        public final int loss,hp,maxHp,tickIndex;
        public final State newState;
        public final List<Effect> effects;
        public final boolean integratedIntoCombat=false;
        Tick(int loss,int hp,int max,int index,State state,List<Effect> effects){
            this.loss=loss;this.hp=hp;this.maxHp=max;tickIndex=index;newState=state;
            this.effects=Collections.unmodifiableList(new ArrayList<>(effects));
        }
    }
    public static final class Session {
        private final Map<String,Target> targets=new LinkedHashMap<>();
        private final Map<String,Curse> curses=new HashMap<>();
        private final Map<String,State> state=new HashMap<>();
        private final Set<String> consumed=new HashSet<>();
        private long revision;
        public Session(Collection<Target> initial){
            require(initial!=null&&!initial.isEmpty(),"initial_targets_required");
            for(Target t:initial){
                require(t!=null&&!targets.containsKey(t.id),"invalid_or_duplicate_target");
                targets.put(t.id,t);
                state.put(t.id,State.NORMAL);
            }
        }
        public synchronized long revision(){return revision;}
        public synchronized Target target(String id){return targets.get(id);}
        public synchronized State state(String id){return state.get(id);}
        public synchronized Curse curse(String id){return curses.get(id);}
        public synchronized boolean canRegainHp(String id){
            require(targets.containsKey(id),"target_missing");
            State current=state.get(id);
            require(current==State.NORMAL||current==State.CURSED,
                "transformed_statblock_requires_authoritative_adapter");
            return !curses.containsKey(id);
        }
        private void event(String id){
            require(id!=null&&!id.trim().isEmpty()&&!consumed.contains(id),
                "event_missing_or_replayed");
        }
        private static int roll(int[] dice,int count,int sides,int bonus){
            require(dice.length==count,"missing_or_extra_dice");
            int raw=bonus;
            for(int die:dice){
                require(die>=1&&die<=sides,"die_out_of_range");raw+=die;
            }
            return raw;
        }
        private static int mitigate(Target t,Type type,int raw){
            Defense d=t.defense(type);
            require(d!=null,"typed_defense_required_"+type);
            return DamageMitigationRules.apply(raw,d==Defense.IMMUNE,
                d==Defense.RESISTANT||d==Defense.BOTH,
                d==Defense.VULNERABLE||d==Defense.BOTH);
        }
        public synchronized Proposal propose(Request r){
            require(r!=null&&r.actor!=null&&!r.actor.trim().isEmpty(),"actor_missing");
            event(r.event);
            require(r.revision==revision&&r.distanceFt>=0&&r.distanceFt<=REACH_FEET,
                "stale_or_out_of_reach");
            Target t=targets.get(r.target);
            require(t!=null&&r.hit!=null,"target_or_hit_missing");
            State current=state.get(t.id);
            require(current==State.NORMAL||current==State.CURSED,
                "transformation_requires_authoritative_statblock_replacement");
            int slashing=0,poison=0;
            boolean curse=false;
            List<Effect> effects=new ArrayList<>();
            if(r.hit){
                slashing=mitigate(t,Type.SLASHING,roll(r.slashingDice,2,6,5));
                poison=mitigate(t,Type.POISON,roll(r.poisonDice,1,6,0));
                if(t.kind==Kind.HUMANOID&&!curses.containsKey(t.id)){
                    require(r.conSaveSucceeded!=null,"eligible_humanoid_con15_save_required");
                    if(!r.conSaveSucceeded){
                        curse=true;
                        effects.add(new Effect(Rider.SLAAD_CURSE_BLOCKS_HEALING,
                            r.actor,t.id,0,"until_curse_removed_or_transformation"));
                        effects.add(new Effect(Rider.MAX_HP_MINUS_3D6_PER_24H,
                            r.actor,t.id,0,"each_24h_manual_host_tick"));
                        effects.add(new Effect(Rider.MAX_HP_NOT_RESTORED_AFTER_LONG_REST,
                            r.actor,t.id,0,"until_curse_removed"));
                    }
                }else require(r.conSaveSucceeded==null,"no_save_for_ineligible_or_already_cursed_target");
            }else {
                require(r.slashingDice.length==0&&r.poisonDice.length==0
                    &&r.conSaveSucceeded==null,"miss_cannot_roll_damage_or_curse_save");
            }
            return new Proposal(this,r.actor,r.event,r.target,revision,
                new Result(slashing,poison,Math.max(0,t.hp-slashing-poison),t.maxHp,curse,effects));
        }
        public synchronized Result commit(Proposal p){
            require(p!=null&&p.owner==this&&p.revision==revision,
                "stale_or_foreign_proposal");
            event(p.event);
            Target t=targets.get(p.target);require(t!=null,"target_missing");
            targets.put(t.id,t.withStats(p.result.hp,p.result.maxHp));
            if(p.result.createsCurse){
                require(!curses.containsKey(p.target),"curse_already_present");
                curses.put(p.target,new Curse(p.actor,p.target,0));
                state.put(p.target,State.CURSED);
            }
            consumed.add(p.event);revision++;
            return p.result;
        }
        /**
         * Host confirms a complete 24h interval since the last authorized tick.
         * Three d6 rolls are supplied, NEVER generated; prior curse must exist.
         * The transformation is emitted as a stat-block replacement obligation,
         * not executed by this pure core.
         */
        public synchronized Tick curse24h(String victim,int elapsed24HourIndex,
                boolean externallyConfirmed24h,int[] threeD6,String event){
            event(event);
            Curse old=curses.get(victim);
            require(old!=null&&state.get(victim)==State.CURSED,"slaad_curse_required");
            require(externallyConfirmed24h&&elapsed24HourIndex==old.lastTickIndex+1,
                "authoritative_elapsed_24h_required");
            Target t=targets.get(victim);
            require(t!=null,"victim_missing");
            int loss=roll(threeD6==null?new int[0]:threeD6,3,6,0);
            int maxHp=Math.max(0,t.maxHp-loss);
            int hp=Math.min(t.hp,maxHp);
            List<Effect> fx=new ArrayList<>();
            State next=State.CURSED;
            if(maxHp==0){
                // Instead of dying, the target MUST be replaced by a Slaad
                // stat block. The real VTT adapter must authorize this.
                next=t.canCastSpellsLevel3Plus?State.PENDING_GREEN_SLAAD:State.PENDING_RED_SLAAD;
                fx.add(new Effect(Rider.CURSE_ENDS_AT_ZERO_MAX_HP,old.source,victim,0,
                    "immediate_when_max_hp_reaches_zero"));
                fx.add(new Effect(t.canCastSpellsLevel3Plus?
                    Rider.REPLACE_STAT_BLOCK_WITH_GREEN_SLAAD:
                    Rider.REPLACE_STAT_BLOCK_WITH_RED_SLAAD,
                    old.source,victim,0,"pending_authoritative_statblock_change"));
                fx.add(new Effect(Rider.ONLY_WISH_REVERSES_SLAAD_TRANSFORMATION,
                    old.source,victim,0,"after_transformation"));
                curses.remove(victim);
            }else {
                fx.add(new Effect(Rider.MAX_HP_MINUS_3D6_PER_24H,
                    old.source,victim,loss,"next_authoritative_24h_tick"));
                curses.put(victim,new Curse(old.source,victim,elapsed24HourIndex));
            }
            targets.put(victim,t.withStats(hp,maxHp));
            state.put(victim,next);
            consumed.add(event);revision++;
            return new Tick(loss,hp,maxHp,elapsed24HourIndex,next,fx);
        }
        /** A verified external event may remove the curse BEFORE transformation. */
        public synchronized boolean removeCurse(String victim,boolean externallyVerified,
                String event){
            event(event);
            require(externallyVerified,"external_curse_removal_authority_required");
            require(state.get(victim)==State.CURSED&&curses.containsKey(victim),
                "no_active_curse_to_remove");
            curses.remove(victim);
            state.put(victim,State.NORMAL);
            consumed.add(event);revision++;
            return true;
        }
        /**
         * ONLY an external, fully adjudicated Wish can begin reversal.
         * Does not fabricate restored statistics or cast a spell.
         */
        public synchronized void verifiedWishReversal(String victim,
                boolean externallyVerifiedWish,String event){
            event(event);
            require(externallyVerifiedWish,"verified_wish_required");
            State s=state.get(victim);
            require(s==State.PENDING_GREEN_SLAAD||s==State.PENDING_RED_SLAAD,
                "no_slaad_transformation_to_reverse");
            state.put(victim,State.WISH_REVERSAL_PENDING);
            consumed.add(event);revision++;
        }
    }
    private static void require(boolean ok,String reason){
        if(!ok)throw new IllegalArgumentException(reason);
    }
}
