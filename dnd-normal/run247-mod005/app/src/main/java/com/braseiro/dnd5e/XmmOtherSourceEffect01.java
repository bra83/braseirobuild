package com.braseiro.dnd5e;

import java.util.*;

/**
 * XMM other_source_effect batch-01: 10 source-pinned action contracts.
 * Deterministic and Java 8 only: all dice, attack/save outcomes, distances,
 * reactions and ray selection are supplied explicitly by the caller.
 *
 * A Session is in-memory only. Typed pending riders are NOT yet applied by
 * Android combat/SQLite: do not claim that conditions, turn limits, and
 * durations are running in the VTT.
 */
public final class XmmOtherSourceEffect01 {
    private XmmOtherSourceEffect01() {}
    public static final String SOURCE_COMMIT = "8c026b807fac21862a309379a0c3228a0683198b";
    public enum Kind { CREATURE, CONSTRUCT, UNDEAD, OBJECT }
    public enum Defense { NORMAL, RESISTANT, VULNERABLE, BOTH, IMMUNE }
    public enum Type { PSYCHIC, NECROTIC, POISON, FORCE }
    public enum Ability { CON, STR, WIS, DEX, NONE }
    public enum Rider {
        SLOWING_BREATH_NO_REACTION_HALF_SPEED_ACTION_OR_BONUS,
        WEAKENING_BREATH_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_DIE,
        REPEAT_SAVE_EACH_TURN_UNTIL_ONE_MINUTE,
        CHARMED_UNTIL_DAMAGE_OR_ONE_HOUR,
        PARALYZED_REPEAT_SAVE_UNTIL_ONE_MINUTE,
        FRIGHTENED_END_NEXT_TURN,
        SLOWED_RAY_END_NEXT_TURN,
        POISONED_NO_HEAL_END_NEXT_TURN,
        TELEKINETIC_MOVE_UP_TO_30FT_RESTRAINED,
        UNCONSCIOUS_UNTIL_DAMAGE_OR_AWAKENED_ONE_MINUTE,
        RESTRAINED_PETRIFY_FIRST_SAVE,
        PETRIFY_SECOND_SAVE_END_NEXT_TURN,
        DISINTEGRATE_OBJECT_CUBE_10FT,
        DISINTEGRATE_CREATURE_AT_ZERO_HP,
        DEATH_RAY_KILLS_AT_ZERO_HP,
        ALLY_REACTION_HALF_MOVE_NO_OPPORTUNITY_ATTACK,
        ACTOR_HALF_MOVE_NO_OPPORTUNITY_ATTACK,
        ATTACKER_DISADVANTAGE_CURRENT_ROLL,
        CYCLOPS_ADVANTAGE_VS_ATTACKER_NEXT_TURN,
        RECHARGE_SIX_REQUIRED
    }
    public enum Action {
        ADULT_COPPER("/monster/9/action/3/entries/0",Ability.CON,18,60),
        ADULT_GOLD("/monster/10/action/3/entries/0",Ability.STR,21,60),
        ANCIENT_COPPER("/monster/21/action/3/entries/0",Ability.CON,22,90),
        ANCIENT_GOLD("/monster/22/action/3/entries/0",Ability.STR,24,90),
        BEHOLDER("/monster/60/action/2/entries/0",Ability.NONE,16,120),
        BEHOLDER_ZOMBIE("/monster/61/action/2/entries/0",Ability.NONE,14,120),
        BERSERKER_COMMANDER("/monster/63/bonus/0/entries/0",Ability.NONE,0,30),
        COPPER_WYRMLING("/monster/103/action/2/entries/0",Ability.CON,11,15),
        CYCLOPS_SENTRY("/monster/112/reaction/0/entries/0",Ability.NONE,0,0),
        DEATH_TYRANT("/monster/120/action/2/entries/0",Ability.NONE,17,120);
        public final String pointer;
        public final Ability ability;
        public final int dc,rangeFt;
        Action(String pointer,Ability ability,int dc,int range){
            this.pointer=pointer;this.ability=ability;this.dc=dc;rangeFt=range;
        }
        public boolean slowing(){return this==ADULT_COPPER||this==ANCIENT_COPPER||this==COPPER_WYRMLING;}
        public boolean weakening(){return this==ADULT_GOLD||this==ANCIENT_GOLD;}
        public boolean ray(){return this==BEHOLDER||this==BEHOLDER_ZOMBIE||this==DEATH_TYRANT;}
        public static Action source(String pointer){
            for(Action a:values())if(a.pointer.equals(pointer))return a;
            throw new IllegalArgumentException("unverified_source_pointer");
        }
    }
    public enum Ray {
        CHARM, PARALYZE, FEAR, SLOW, ENERVATE, TELEKINESIS, SLEEP,
        PETRIFY, DISINTEGRATE, DEATH
    }
    public static final class RayRule {
        public final Ray ray;
        public final Ability save;
        public final Type damage;
        public final int count,sides;
        public final boolean halfOnSave;
        RayRule(Ray ray,Ability save,Type damage,int count,int sides,boolean half) {
            this.ray=ray;this.save=save;this.damage=damage;
            this.count=count;this.sides=sides;halfOnSave=half;
        }
    }
    private static RayRule rule(Ray r,Ability s,Type type,int n,int sides,boolean half) {
        return new RayRule(r,s,type,n,sides,half);
    }
    private static final RayRule[] BEHOLDER_RAYS = {
        rule(Ray.CHARM,Ability.WIS,Type.PSYCHIC,3,8,true),
        rule(Ray.PARALYZE,Ability.CON,null,0,0,false),
        rule(Ray.FEAR,Ability.WIS,Type.PSYCHIC,4,6,true),
        rule(Ray.SLOW,Ability.CON,Type.NECROTIC,4,8,true),
        rule(Ray.ENERVATE,Ability.CON,Type.POISON,3,8,true),
        rule(Ray.TELEKINESIS,Ability.STR,null,0,0,false),
        rule(Ray.SLEEP,Ability.WIS,null,0,0,false),
        rule(Ray.PETRIFY,Ability.CON,null,0,0,false),
        rule(Ray.DISINTEGRATE,Ability.DEX,Type.FORCE,8,8,true),
        rule(Ray.DEATH,Ability.DEX,Type.NECROTIC,10,10,true)
    };
    private static final RayRule[] ZOMBIE_RAYS = {
        rule(Ray.PARALYZE,Ability.CON,null,0,0,false),
        rule(Ray.FEAR,Ability.WIS,Type.PSYCHIC,3,8,false),
        rule(Ray.ENERVATE,Ability.CON,Type.NECROTIC,3,6,true),
        rule(Ray.DISINTEGRATE,Ability.DEX,Type.FORCE,5,10,true)
    };
    private static final RayRule[] TYRANT_RAYS = {
        rule(Ray.CHARM,Ability.WIS,Type.PSYCHIC,3,8,true),
        rule(Ray.PARALYZE,Ability.CON,null,0,0,false),
        rule(Ray.FEAR,Ability.WIS,Type.PSYCHIC,3,6,true),
        rule(Ray.SLOW,Ability.CON,Type.NECROTIC,4,8,true),
        rule(Ray.ENERVATE,Ability.CON,Type.POISON,3,10,true),
        rule(Ray.TELEKINESIS,Ability.STR,null,0,0,false),
        rule(Ray.SLEEP,Ability.WIS,null,0,0,false),
        rule(Ray.PETRIFY,Ability.CON,null,0,0,false),
        rule(Ray.DISINTEGRATE,Ability.DEX,Type.FORCE,8,8,true),
        rule(Ray.DEATH,Ability.DEX,Type.NECROTIC,10,10,true)
    };
    public static RayRule rayRule(Action a,int rolledIndex) {
        require(a.ray(),"not_a_ray_action");
        RayRule[] table=a==Action.BEHOLDER?BEHOLDER_RAYS:
            a==Action.DEATH_TYRANT?TYRANT_RAYS:ZOMBIE_RAYS;
        require(rolledIndex>=1&&rolledIndex<=table.length,"invalid_ray_roll");
        return table[rolledIndex-1];
    }
    public static final class Target {
        public final String id;
        public final Kind kind;
        public final int hp,maxHp;
        public final boolean gargantuan,nonmagicalObject;
        private final EnumMap<Type,Defense> defenses;
        public Target(String id,Kind kind,int hp,int maxHp,boolean gargantuan,
                boolean nonmagicalObject,Map<Type,Defense> defenses) {
            require(id!=null&&!id.trim().isEmpty()&&kind!=null&&hp>=0&&maxHp>=0&&hp<=maxHp,
                "invalid_target");
            this.id=id;this.kind=kind;this.hp=hp;this.maxHp=maxHp;
            this.gargantuan=gargantuan;this.nonmagicalObject=nonmagicalObject;
            this.defenses=new EnumMap<>(Type.class);
            if(defenses!=null)this.defenses.putAll(defenses);
        }
        private Target withHp(int hp){return new Target(id,kind,hp,maxHp,gargantuan,nonmagicalObject,defenses);}
        public Defense defense(Type t){return defenses.get(t);}
    }
    public static final class Effect {
        public final Rider rider;
        public final String actor,target,expiry;
        /** For penalties, movement caps, save DCs, etc.; NEVER automatic rolls. */
        public final int magnitude;
        Effect(Rider rider,String actor,String target,int magnitude,String expiry){
            this.rider=rider;this.actor=actor;this.target=target;this.magnitude=magnitude;this.expiry=expiry;
        }
    }
    public static final class Request {
        public final Action action;
        public final String actor,target,event,turn,ally;
        public final long revision;
        public final int distanceFt,rayIndex,telekineticMoveFt,allyDistanceFt;
        public final Boolean saveSucceeded;
        public final int[] damageDice;
        public final boolean targetVisible,inCone,actorCanReact,rechargeReady,
            allyCanReact,allyAccepts,actorMoves,targetIncapacitated;
        public Request(Action a,String actor,String target,String event,String turn,long revision,
                int distanceFt,int rayIndex,Boolean saved,int[] dice,
                boolean visible,boolean inCone,boolean canReact,boolean rechargeReady,
                String ally,int allyDistanceFt,boolean allyCanReact,boolean allyAccepts,
                boolean actorMoves,int telekineticMoveFt,boolean targetIncapacitated){
            action=Objects.requireNonNull(a);this.actor=actor;this.target=target;this.event=event;
            this.turn=turn;this.revision=revision;this.distanceFt=distanceFt;this.rayIndex=rayIndex;
            saveSucceeded=saved;damageDice=dice==null?new int[0]:dice.clone();
            targetVisible=visible;this.inCone=inCone;actorCanReact=canReact;
            this.rechargeReady=rechargeReady;this.ally=ally;this.allyDistanceFt=allyDistanceFt;
            this.allyCanReact=allyCanReact;this.allyAccepts=allyAccepts;this.actorMoves=actorMoves;
            this.telekineticMoveFt=telekineticMoveFt;this.targetIncapacitated=targetIncapacitated;
        }
    }
    public static final class Result {
        public final Action action;
        public final Ray selectedRay;
        public final int damage,hp;
        public final List<Effect> effects;
        public final boolean persistedInAndroid=false;
        Result(Action action,Ray ray,int damage,int hp,List<Effect> effects){
            this.action=action;selectedRay=ray;this.damage=damage;this.hp=hp;
            this.effects=Collections.unmodifiableList(new ArrayList<>(effects));
        }
    }
    public static final class Proposal {
        private final Session owner;
        private final long revision;
        private final String event,target,usedRayKey;
        private final int rayIndex;
        public final Result result;
        Proposal(Session owner,long revision,String event,String target,
                String usedRayKey,int rayIndex,Result result){
            this.owner=owner;this.revision=revision;this.event=event;this.target=target;
            this.usedRayKey=usedRayKey;this.rayIndex=rayIndex;this.result=result;
        }
    }
    public static final class Session {
        private final Map<String,Target> targets=new LinkedHashMap<>();
        private final Map<String,List<Effect>> effects=new LinkedHashMap<>();
        private final Set<String> consumed=new HashSet<>();
        private final Map<String,Set<Integer>> usedRays=new HashMap<>();
        private long revision;
        public Session(Collection<Target> initial){
            require(initial!=null&&!initial.isEmpty(),"targets_required");
            for(Target t:initial){require(t!=null&&!targets.containsKey(t.id),"duplicate_target");targets.put(t.id,t);}
        }
        public synchronized long revision(){return revision;}
        public synchronized Target target(String id){return targets.get(id);}
        public synchronized List<Effect> effects(String id){
            List<Effect> list=effects.get(id);
            return Collections.unmodifiableList(list==null?Collections.<Effect>emptyList():new ArrayList<>(list));
        }
        private String rayKey(Request r){return r.actor+"|"+r.action.name()+"|"+r.turn;}
        public synchronized Proposal propose(Request r){
            require(r!=null&&r.actor!=null&&!r.actor.trim().isEmpty()
                &&r.event!=null&&!r.event.trim().isEmpty()
                &&r.turn!=null&&!r.turn.trim().isEmpty(),"actor_event_turn_required");
            require(r.revision==revision&&!consumed.contains(r.event),"replayed_or_stale");
            Target t=targets.get(r.target);
            require(t!=null&&r.distanceFt>=0,"target_or_distance_invalid");
            require(r.telekineticMoveFt>=0,"invalid_telekinetic_move");
            Action a=r.action;
            List<Effect> out=new ArrayList<>();
            int damage=0,hp=t.hp;
            Ray selected=null;
            String rayKey=null;
            if(a.slowing()||a.weakening()){
                require(t.kind!=Kind.OBJECT&&r.distanceFt<=a.rangeFt&&r.inCone,
                    "creature_in_cone_required");
                require(r.saveSucceeded!=null&&r.rayIndex==0&&r.damageDice.length==0,
                    "breath_requires_explicit_save_no_dice");
                if(a.weakening()){
                    // A creature already affected cannot be targeted by this breath.
                    for(Effect e:effects(r.target))if(e.rider==Rider.WEAKENING_BREATH_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_DIE
                        &&e.actor.equals(r.actor))throw new IllegalArgumentException("weakening_already_affects_target");
                }
                if(!r.saveSucceeded){
                    if(a.slowing())out.add(effect(Rider.SLOWING_BREATH_NO_REACTION_HALF_SPEED_ACTION_OR_BONUS,
                        r,0,"end_of_target_next_turn"));
                    else{
                        int dieSides=a==Action.ANCIENT_GOLD?10:6;
                        out.add(effect(Rider.WEAKENING_BREATH_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_DIE,
                            r,dieSides,"repeat_save_end_each_turn_or_one_minute"));
                        out.add(effect(Rider.REPEAT_SAVE_EACH_TURN_UNTIL_ONE_MINUTE,r,a.dc,"one_minute_auto_success"));
                    }
                }
            }else if(a.ray()){
                require(r.distanceFt<=a.rangeFt&&r.targetVisible&&r.rayIndex>0&&r.saveSucceeded!=null,
                    "ray_target_selection_or_save_missing");
                rayKey=rayKey(r);
                if(usedRays.containsKey(rayKey)&&usedRays.get(rayKey).contains(r.rayIndex))
                    throw new IllegalArgumentException("ray_used_this_turn_reroll_required");
                RayRule rr=rayRule(a,r.rayIndex);selected=rr.ray;
                boolean saved=r.saveSucceeded;
                if(selected==Ray.TELEKINESIS&&t.gargantuan)require(saved,"gargantuan_automatically_succeeds");
                if(selected==Ray.SLEEP&&(t.kind==Kind.CONSTRUCT||t.kind==Kind.UNDEAD))
                    require(saved,"construct_undead_automatically_succeeds");
                if(rr.count>0){
                    require(r.damageDice.length==rr.count,"ray_damage_dice_missing");
                    int raw=0;for(int die:r.damageDice){require(die>=1&&die<=rr.sides,"ray_die_invalid");raw+=die;}
                    // Half damage on a successful save, before resistance/vulnerability.
                    if(saved)raw=rr.halfOnSave?raw/2:0;
                    Defense defense=t.defense(rr.damage);
                    require(defense!=null,"missing_typed_defense");
                    damage=DamageMitigationRules.apply(raw,defense==Defense.IMMUNE,
                        defense==Defense.RESISTANT||defense==Defense.BOTH,
                        defense==Defense.VULNERABLE||defense==Defense.BOTH);
                    hp=Math.max(0,t.hp-damage);
                }else require(r.damageDice.length==0,"unexpected_ray_dice");
                if(!saved){
                    switch(selected){
                        case CHARM:out.add(effect(Rider.CHARMED_UNTIL_DAMAGE_OR_ONE_HOUR,r,0,"one_hour_or_damage"));break;
                        case PARALYZE:out.add(effect(Rider.PARALYZED_REPEAT_SAVE_UNTIL_ONE_MINUTE,
                            r,a.dc,"repeat_save_end_each_turn_or_one_minute"));break;
                        case FEAR:out.add(effect(Rider.FRIGHTENED_END_NEXT_TURN,r,0,"end_of_target_next_turn"));break;
                        case SLOW:out.add(effect(Rider.SLOWED_RAY_END_NEXT_TURN,r,0,"end_of_target_next_turn"));break;
                        case ENERVATE:out.add(effect(Rider.POISONED_NO_HEAL_END_NEXT_TURN,r,0,"end_of_target_next_turn"));break;
                        case TELEKINESIS:
                            require(r.telekineticMoveFt<=30,"telekinetic_move_exceeds_30ft");
                            out.add(effect(Rider.TELEKINETIC_MOVE_UP_TO_30FT_RESTRAINED,r,
                                r.telekineticMoveFt,"start_of_actor_next_turn_or_incapacitated"));break;
                        case SLEEP:out.add(effect(Rider.UNCONSCIOUS_UNTIL_DAMAGE_OR_AWAKENED_ONE_MINUTE,
                            r,0,"one_minute_or_damage_or_ally_wake_action"));break;
                        case PETRIFY:
                            out.add(effect(Rider.RESTRAINED_PETRIFY_FIRST_SAVE,r,0,
                                "until_end_next_turn_repeat_save"));
                            out.add(effect(Rider.PETRIFY_SECOND_SAVE_END_NEXT_TURN,r,a.dc,
                                "on_second_failure_replace_restrained_with_petrified"));break;
                        case DISINTEGRATE:
                            if(t.kind==Kind.OBJECT&&t.nonmagicalObject)
                                out.add(effect(Rider.DISINTEGRATE_OBJECT_CUBE_10FT,r,10,"immediate"));
                            break;
                        case DEATH:break;
                    }
                }else require(r.telekineticMoveFt==0,"telekinesis_move_on_success");
                if(selected==Ray.DISINTEGRATE&&t.kind!=Kind.OBJECT&&hp==0&&t.hp>0)
                    out.add(effect(Rider.DISINTEGRATE_CREATURE_AT_ZERO_HP,r,0,"immediate_if_ray_reduces_to_zero"));
                if(selected==Ray.DEATH&&t.kind!=Kind.OBJECT&&hp==0&&t.hp>0)
                    out.add(effect(Rider.DEATH_RAY_KILLS_AT_ZERO_HP,r,0,"immediate_if_ray_reduces_to_zero"));
            }else if(a==Action.BERSERKER_COMMANDER){
                require(r.distanceFt<=30&&r.rayIndex==0&&r.saveSucceeded==null
                    &&r.damageDice.length==0,"invalid_frenzied_rush_context");
                if(r.allyAccepts){
                    require(r.ally!=null&&!r.ally.trim().isEmpty()
                        &&r.allyDistanceFt>=0&&r.allyDistanceFt<=30&&r.allyCanReact,
                        "ally_reaction_out_of_range");
                    out.add(new Effect(Rider.ALLY_REACTION_HALF_MOVE_NO_OPPORTUNITY_ATTACK,
                        r.actor,r.ally,0,"immediate_ally_reaction"));
                }
                if(r.actorMoves)out.add(new Effect(Rider.ACTOR_HALF_MOVE_NO_OPPORTUNITY_ATTACK,
                    r.actor,r.actor,0,"immediate_actor_move"));
            }else if(a==Action.CYCLOPS_SENTRY){
                require(r.actorCanReact&&r.rechargeReady&&r.targetVisible,
                    "cyclops_trigger_reaction_recharge_required");
                require(r.rayIndex==0&&r.damageDice.length==0&&r.saveSucceeded==null,
                    "cyclops_does_not_roll_save_or_damage");
                out.add(effect(Rider.ATTACKER_DISADVANTAGE_CURRENT_ROLL,r,0,
                    "triggering_attack_roll_before_resolution"));
                out.add(effect(Rider.CYCLOPS_ADVANTAGE_VS_ATTACKER_NEXT_TURN,r,0,
                    "end_of_cyclops_next_turn"));
                out.add(new Effect(Rider.RECHARGE_SIX_REQUIRED,r.actor,r.actor,6,
                    "recharge_on_d6_roll_of_six"));
            }
            return new Proposal(this,revision,r.event,r.target,rayKey,r.rayIndex,
                new Result(a,selected,damage,hp,out));
        }
        public synchronized Result execute(Proposal p){
            require(p!=null&&p.owner==this&&p.revision==revision
                    &&!consumed.contains(p.event),"stale_replay_or_foreign_proposal");
            Target t=targets.get(p.target);require(t!=null,"missing_target");
            targets.put(p.target,t.withHp(p.result.hp));
            for(Effect e:p.result.effects){
                List<Effect> list=effects.get(e.target);
                if(list==null){list=new ArrayList<>();effects.put(e.target,list);}
                list.add(e);
            }
            if(p.usedRayKey!=null){
                Set<Integer> used=usedRays.get(p.usedRayKey);
                if(used==null){used=new HashSet<>();usedRays.put(p.usedRayKey,used);}
                require(!used.contains(p.rayIndex),"ray_reused");
                used.add(p.rayIndex);
            }
            consumed.add(p.event);revision++;return p.result;
        }
    }
    private static Effect effect(Rider kind,Request r,int amount,String expiry){
        return new Effect(kind,r.actor,r.target,amount,expiry);
    }
    private static void require(boolean valid,String reason){
        if(!valid)throw new IllegalArgumentException(reason);
    }
}
