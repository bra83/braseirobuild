package com.braseiro.dnd5e;

import java.util.*;

/**
 * Pure Java 8 executor for the 12 source-pinned XMM damage/save branch-04 actions.
 * No dice are rolled here. Every roll, target identity, range and defense is caller supplied.
 * Commit mutates only this Session, never combat SQLite or Android UI.
 *
 * Unresolved conditions, long-running curses, rest interference, incubation and
 * timed effects are emitted as typed effects and MUST be applied by a later
 * authoritative combat adapter. They are NOT silently marked as completed.
 */
public final class XmmDamageOrSaveBranch04 {
    private XmmDamageOrSaveBranch04() {}
    public static final String SOURCE_COMMIT = "8c026b807fac21862a309379a0c3228a0683198b";
    public static final String SOURCE_BLOB = "9a4137b508ef37562ed948894a70b1594b57e85c";

    public enum Type { BLUDGEONING, NECROTIC, PIERCING, SLASHING, POISON, ACID, RADIANT, PSYCHIC }
    public enum Defense { NORMAL, RESISTANT, VULNERABLE, BOTH, IMMUNE }
    public enum Kind { HUMANOID, CREATURE, OBJECT }
    public enum Choice { NONE, CHARMED, POISONED, SWORD_RETURNED, SWORD_HOVERS }
    public enum Rider {
        MUMMY_ROT_CURSE, MUMMY_LORD_ROT_CURSE, LONG_REST_MAX_HP_NOT_RESTORED,
        LONG_REST_NO_BENEFIT, CANNOT_REGAIN_HP, MAX_HP_LOSS_EVERY_24H,
        DUST_ON_DEATH, HEART_REMOVED_ON_DEATH, CHARMED, POISONED,
        NEXT_ATTACK_ADVANTAGE, INT_SAVE_DISADVANTAGE, RESTLESS_CURSE,
        SLAAD_EGG_CURSE, SLAAD_EGG_GESTATION_DAYS, STRENGTH_REDUCED,
        SHADOW_RISE_IN_HOURS, SWORD_RETURNED, SWORD_HOVERS,
        MAX_HP_REDUCTION, RIDDLE_CURSE, RIDDLE_DISADVANTAGE,
        MAGIC_ACTION_SAVE_DC_15, STUDY_ACTION_CHECK_DC_15, CURSE_REPLACES_PRIOR
    }
    public static final class Dice {
        public final Type type;
        public final int count, sides, bonus;
        Dice(Type t,int c,int s,int b) { type=t; count=c; sides=s; bonus=b; }
        public int total(int[] dice) {
            require(dice!=null&&dice.length==count,"dice_count");
            int out=bonus;
            for(int die:dice) { require(die>=1&&die<=sides,"die_out_of_range"); out+=die; }
            return out;
        }
    }
    private static Dice d(Type t,int count,int sides,int bonus) {
        return new Dice(t,count,sides,bonus);
    }
    public enum Action {
        MUMMY("/monster/311/action/1/entries/0",5,5,0,false,0,d(Type.BLUDGEONING,1,10,3),d(Type.NECROTIC,3,6,0)),
        MUMMY_LORD("/monster/312/action/1/entries/0",9,5,0,false,0,d(Type.BLUDGEONING,2,10,4),d(Type.NECROTIC,3,6,0)),
        PERYTON("/monster/339/action/2/entries/0",5,5,0,false,0,d(Type.PIERCING,2,4,3),null),
        PIRATE_ADMIRAL("/monster/344/action/1/entries/0",10,5,0,false,0,d(Type.SLASHING,3,6,6),d(Type.POISON,2,6,0)),
        PIRATE_CAPTAIN("/monster/345/action/1/entries/0",7,5,0,false,0,d(Type.PIERCING,2,8,4),null),
        PSYCHIC_GRAY_OOZE("/monster/358/action/0/entries/0",3,5,0,false,0,d(Type.ACID,3,6,1),null),
        RAKSHASA("/monster/365/action/1/entries/0",10,5,0,false,0,d(Type.SLASHING,2,6,5),d(Type.NECROTIC,3,12,0)),
        RED_SLAAD("/monster/369/action/1/entries/0",6,10,0,false,14,d(Type.PIERCING,2,6,3),null),
        SHADOW("/monster/393/action/0/entries/0",4,5,0,false,0,d(Type.NECROTIC,1,6,2),null),
        SOLAR("/monster/403/action/1/entries/0",15,10,120,true,0,d(Type.SLASHING,4,6,8),d(Type.RADIANT,8,8,0)),
        SPECTER("/monster/405/action/0/entries/0",4,5,0,false,0,d(Type.NECROTIC,2,6,0),null),
        SPHINX("/monster/407/action/2/entries/0",0,0,60,false,15,d(Type.PSYCHIC,6,6,0),null);
        public final String pointer;
        public final int toHit,reachFeet,rangeFeet,saveDc;
        public final boolean allowRangedAttack;
        public final Dice first,second;
        Action(String p,int h,int r,int range,boolean ranged,int save,Dice a,Dice b) {
            pointer=p;toHit=h;reachFeet=r;rangeFeet=range;
            allowRangedAttack=ranged;saveDc=save;first=a;second=b;
        }
        public static Action fromPointer(String ptr) {
            for(Action a:values())if(a.pointer.equals(ptr))return a;
            throw new IllegalArgumentException("unverified_source_pointer");
        }
    }
    public static final class Target {
        public final String id;
        public final Kind kind;
        public final int hp,maxHp,strength;
        private final EnumMap<Type,Defense> defenses;
        public Target(String id,Kind kind,int hp,int maxHp,int strength,Map<Type,Defense> defenses) {
            require(id!=null&&!id.trim().isEmpty()&&kind!=null,"invalid_target");
            require(maxHp>=0&&hp>=0&&hp<=maxHp&&strength>=0&&strength<=30,"invalid_target_stats");
            this.id=id;this.kind=kind;this.hp=hp;this.maxHp=maxHp;this.strength=strength;
            this.defenses=new EnumMap<>(Type.class);
            if(defenses!=null)this.defenses.putAll(defenses);
        }
        private Target updated(int hp,int maxHp,int strength) {
            return new Target(id,kind,hp,maxHp,strength,defenses);
        }
        public Defense defense(Type t) { return defenses.get(t); }
    }
    public static final class Effect {
        public final Rider kind;
        public final String actor,target;
        public final int value;
        public final String expiry;
        Effect(Rider k,String a,String t,int value,String expiry) {
            kind=k;actor=a;target=t;this.value=value;this.expiry=expiry;
        }
    }
    public static final class Request {
        public final Action action;
        public final String actor,event,target;
        public final long revision;
        public final int distanceFeet;
        public final boolean ranged;
        public final Boolean hit,saveSucceeded;
        public final int[] firstDice,secondDice,extraDice;
        public final Choice choice;
        public final boolean lineOfSight;
        public Request(Action action,String actor,String event,String target,long revision,
                int distanceFeet,boolean ranged,Boolean hit,Boolean saveSucceeded,
                int[] firstDice,int[] secondDice,int[] extraDice,Choice choice,boolean lineOfSight) {
            this.action=Objects.requireNonNull(action);
            this.actor=actor;this.event=event;this.target=target;this.revision=revision;
            this.distanceFeet=distanceFeet;this.ranged=ranged;this.hit=hit;this.saveSucceeded=saveSucceeded;
            this.firstDice=firstDice==null?new int[0]:firstDice.clone();
            this.secondDice=secondDice==null?new int[0]:secondDice.clone();
            this.extraDice=extraDice==null?new int[0]:extraDice.clone();
            this.choice=choice==null?Choice.NONE:choice;this.lineOfSight=lineOfSight;
        }
    }
    public static final class Result {
        public final Action action;
        public final int appliedDamage,hitPoints,maxHitPoints,strength;
        public final boolean dead;
        public final List<Effect> effects;
        public final boolean combatPersisted=false;
        private Result(Action a,int damage,int hp,int max,int str,boolean dead,List<Effect> effects) {
            action=a;appliedDamage=damage;hitPoints=hp;maxHitPoints=max;
            strength=str;this.dead=dead;
            this.effects=Collections.unmodifiableList(new ArrayList<>(effects));
        }
    }
    public static final class Proposal {
        private final Session owner;
        private final long revision;
        private final String token,target;
        public final Result result;
        private Proposal(Session owner,long revision,String token,String target,Result result) {
            this.owner=owner;this.revision=revision;this.token=token;this.target=target;
            this.result=result;
        }
    }
    public static final class Session {
        private final Map<String,Target> targets=new LinkedHashMap<>();
        private final Set<String> consumed=new HashSet<>();
        private final Map<String,List<Effect>> pendingEffects=new LinkedHashMap<>();
        private long revision;
        public Session(Collection<Target> initial) {
            require(initial!=null&&!initial.isEmpty(),"targets_required");
            for(Target t:initial) {
                require(t!=null&&!targets.containsKey(t.id),"duplicate_target");
                targets.put(t.id,t);
            }
        }
        public synchronized long revision(){return revision;}
        public synchronized Target target(String id){return targets.get(id);}
        public synchronized List<Effect> effects(String id){
            List<Effect> result=pendingEffects.get(id);
            return Collections.unmodifiableList(result==null?new ArrayList<Effect>():new ArrayList<>(result));
        }
        public synchronized Proposal propose(Request r){
            require(r!=null&&r.actor!=null&&!r.actor.trim().isEmpty()
                    &&r.event!=null&&!r.event.trim().isEmpty(),"event_or_actor_missing");
            require(!consumed.contains(r.event)&&r.revision==revision,"stale_or_replayed");
            Target t=targets.get(r.target);
            require(t!=null,"target_missing");
            Action a=r.action;
            require(r.distanceFeet>=0&&r.lineOfSight,"target_not_visible");
            if(a==Action.SPHINX) {
                require(r.distanceFeet<=a.rangeFeet&&!r.ranged&&r.hit==null&&r.saveSucceeded!=null,"invalid_sphinx_save");
            } else {
                require(r.hit!=null,"attack_result_required");
                require(!r.ranged||a.allowRangedAttack,"ranged_not_allowed");
                require(r.distanceFeet<=(r.ranged?a.rangeFeet:a.reachFeet),"range_exceeded");
            }
            List<Effect> fx=new ArrayList<>();
            boolean active=a==Action.SPHINX?!r.saveSucceeded:r.hit;
            int damage=0,max=t.maxHp,str=t.strength;
            int hp=t.hp;
            if(active){
                damage+=mitigate(t,a.first,r.firstDice);
                if(a.second!=null) damage+=mitigate(t,a.second,r.secondDice);
                else require(r.secondDice.length==0,"unexpected_secondary_dice");
                hp=Math.max(0,hp-damage);
                boolean creature=t.kind!=Kind.OBJECT;
                switch(a) {
                    case MUMMY:
                    case MUMMY_LORD:
                        if(creature) {
                            fx.add(effect(a==Action.MUMMY?Rider.MUMMY_ROT_CURSE:Rider.MUMMY_LORD_ROT_CURSE,r,0,"until_curse_removed"));
                            fx.add(effect(Rider.CANNOT_REGAIN_HP,r,0,"until_curse_removed"));
                            fx.add(effect(a==Action.MUMMY?Rider.LONG_REST_MAX_HP_NOT_RESTORED:Rider.LONG_REST_NO_BENEFIT,r,0,"until_curse_removed"));
                            fx.add(effect(Rider.MAX_HP_LOSS_EVERY_24H,r,0,"3d6_manual_roll_each_24_hours_until_curse_removed"));
                            if(hp==0)fx.add(effect(Rider.DUST_ON_DEATH,r,0,"immediate_on_attack_kill"));
                        }
                        require(r.extraDice.length==0&&r.choice==Choice.NONE,"unexpected_mummy_context");
                        break;
                    case PERYTON:
                        if(hp==0&&t.kind==Kind.HUMANOID)fx.add(effect(Rider.HEART_REMOVED_ON_DEATH,r,0,"immediate"));
                        require(r.extraDice.length==0&&r.choice==Choice.NONE,"unexpected_peryton_context");
                        break;
                    case PIRATE_ADMIRAL:
                        require(creature&&(r.choice==Choice.CHARMED||r.choice==Choice.POISONED),"admiral_choice_required");
                        fx.add(effect(r.choice==Choice.CHARMED?Rider.CHARMED:Rider.POISONED,r,0,"start_of_pirate_next_turn"));
                        require(r.extraDice.length==0,"unexpected_admiral_dice");
                        break;
                    case PIRATE_CAPTAIN:
                        fx.add(new Effect(Rider.NEXT_ATTACK_ADVANTAGE,r.actor,r.actor,0,"next_attack_before_end_of_current_turn"));
                        require(r.choice==Choice.NONE&&r.extraDice.length==0,"unexpected_captain_context");
                        break;
                    case PSYCHIC_GRAY_OOZE:
                        require(creature,"ooze_int_save_target_requires_creature");
                        fx.add(effect(Rider.INT_SAVE_DISADVANTAGE,r,0,"end_of_ooze_next_turn"));
                        require(r.choice==Choice.NONE&&r.extraDice.length==0,"unexpected_ooze_context");
                        break;
                    case RAKSHASA:
                        if(creature) {
                            fx.add(effect(Rider.RESTLESS_CURSE,r,0,"until_curse_removed"));
                            fx.add(effect(Rider.LONG_REST_NO_BENEFIT,r,0,"until_curse_removed"));
                        }
                        require(r.extraDice.length==0&&r.choice==Choice.NONE,"unexpected_rakshasa_context");
                        break;
                    case RED_SLAAD:
                        if(t.kind==Kind.HUMANOID && !hasEffect(r.target,Rider.SLAAD_EGG_CURSE)) {
                            require(r.saveSucceeded!=null,"slaad_con_save_dc14_required");
                            if(!r.saveSucceeded){
                                require(r.extraDice.length==2,"slaad_2d4_roll_required");
                                int days=0;
                                for(int die:r.extraDice){require(die>=1&&die<=4,"slaad_gestation_die");days+=die*10;}
                                fx.add(effect(Rider.SLAAD_EGG_CURSE,r,0,"until_curse_removed"));
                                fx.add(effect(Rider.SLAAD_EGG_GESTATION_DAYS,r,days,"last_day_speed_halved_and_d20_disadvantage_then_death"));
                            } else require(r.extraDice.length==0,"unexpected_slaad_gestation");
                        } else require(r.saveSucceeded==null&&r.extraDice.length==0,"slaad_ineligible_save");
                        require(r.choice==Choice.NONE,"unexpected_slaad_choice");
                        break;
                    case SHADOW:
                        require(creature&&r.extraDice.length>=1,"strength_drain_1d4_required");
                        int loss=r.extraDice[0];require(loss>=1&&loss<=4,"strength_drain_die");
                        str=Math.max(0,str-loss);
                        fx.add(effect(Rider.STRENGTH_REDUCED,r,loss,"immediate"));
                        if(str==0) hp=0;
                        if(hp==0&&t.kind==Kind.HUMANOID){
                            require(r.extraDice.length==2,"shadow_spawn_1d4_hours_required");
                            int hours=r.extraDice[1];require(hours>=1&&hours<=4,"shadow_spawn_die");
                            fx.add(effect(Rider.SHADOW_RISE_IN_HOURS,r,hours,"after_hours_if_corpse_valid"));
                        } else require(r.extraDice.length==1,"unexpected_shadow_hours");
                        require(r.choice==Choice.NONE,"unexpected_shadow_choice");
                        break;
                    case SOLAR:
                        if(r.ranged){
                            require(r.choice==Choice.SWORD_RETURNED||r.choice==Choice.SWORD_HOVERS,"solar_sword_location_required");
                            fx.add(effect(r.choice==Choice.SWORD_RETURNED?Rider.SWORD_RETURNED:Rider.SWORD_HOVERS,r,0,"immediately_after_ranged_attack"));
                        } else require(r.choice==Choice.NONE,"unexpected_solar_sword_choice");
                        require(r.extraDice.length==0,"unexpected_solar_context");
                        break;
                    case SPECTER:
                        if(creature) {
                            max=Math.max(0,max-damage);
                            hp=Math.min(hp,max);
                            fx.add(effect(Rider.MAX_HP_REDUCTION,r,damage,"until_max_hp_restored"));
                        }
                        require(r.choice==Choice.NONE&&r.extraDice.length==0,"unexpected_specter_context");
                        break;
                    case SPHINX:
                        require(creature&&r.choice==Choice.NONE&&r.extraDice.length==0,"invalid_sphinx_curse_target");
                        fx.add(effect(Rider.RIDDLE_CURSE,r,15,"until_study_dc15_or_curse_removed_or_new_sphinx_target"));
                        fx.add(effect(Rider.RIDDLE_DISADVANTAGE,r,0,"ability_checks_and_attack_rolls"));
                        fx.add(effect(Rider.MAGIC_ACTION_SAVE_DC_15,r,15,"failed_save_wastes_magic_action"));
                        fx.add(effect(Rider.STUDY_ACTION_CHECK_DC_15,r,15,"success_ends_curse"));
                        fx.add(effect(Rider.CURSE_REPLACES_PRIOR,r,0,"same_sphinx_only"));
                        break;
                    default: throw new IllegalArgumentException("unmapped_action");
                }
            } else {
                require(r.firstDice.length==0&&r.secondDice.length==0
                        &&r.extraDice.length==0,"unused_rolls");
                if(a==Action.SOLAR&&r.ranged){
                    require(r.choice==Choice.SWORD_RETURNED||r.choice==Choice.SWORD_HOVERS,"solar_sword_location_required");
                    fx.add(new Effect(r.choice==Choice.SWORD_RETURNED?Rider.SWORD_RETURNED:Rider.SWORD_HOVERS,
                            r.actor,r.target,0,"immediately_after_ranged_attack"));
                } else require(r.choice==Choice.NONE,"unused_choice");
                if(a==Action.RED_SLAAD)require(r.saveSucceeded==null,"miss_cannot_trigger_save");
            }
            boolean dead=hp==0 || str==0 || (t.kind!=Kind.OBJECT&&max<=0);
            return new Proposal(this,revision,r.event,r.target,new Result(a,damage,hp,max,str,dead,fx));
        }
        private boolean hasEffect(String target,Rider rider) {
            List<Effect> effects=pendingEffects.get(target);
            if(effects==null)return false;
            for(Effect e:effects)if(e.kind==rider)return true;
            return false;
        }
        public synchronized Result execute(Proposal p){
            require(p!=null&&p.owner==this&&p.revision==revision
                    &&!consumed.contains(p.token),"stale_replay_or_foreign_proposal");
            Target existing=targets.get(p.target);
            require(existing!=null,"missing_target");
            Result result=p.result;
            targets.put(p.target,existing.updated(result.hitPoints,result.maxHitPoints,result.strength));
            for(Effect fx:result.effects){
                if(fx.kind==Rider.RIDDLE_CURSE) {
                    // The sphinx may have only one riddle target. Avoid expiration
                    // ambiguity by clearing all riddle riders from its prior targets.
                    for(List<Effect> all:pendingEffects.values())all.removeIf(e->
                        e.actor.equals(fx.actor)&&isRiddleRider(e.kind));
                }
                List<Effect> list=pendingEffects.get(fx.target);
                if(list==null){list=new ArrayList<>();pendingEffects.put(fx.target,list);}
                list.add(fx);
            }
            consumed.add(p.token);revision++;
            return result;
        }
        public synchronized boolean removeCurse(String target,Rider source){
            require(source==Rider.MUMMY_ROT_CURSE||source==Rider.MUMMY_LORD_ROT_CURSE
                    ||source==Rider.RESTLESS_CURSE||source==Rider.SLAAD_EGG_CURSE
                    ||source==Rider.RIDDLE_CURSE,"unsupported_curse");
            List<Effect> fx=pendingEffects.get(target);
            if(fx==null)return false;
            // Clear the exact curse family and all its consequences.
            boolean found=false;for(Effect e:fx)if(e.kind==source)found=true;
            if(!found)return false;
            fx.removeIf(e->inCurseFamily(source,e.kind));
            revision++;return true;
        }
    }
    private static boolean isRiddleRider(Rider r){
        return r==Rider.RIDDLE_CURSE||r==Rider.RIDDLE_DISADVANTAGE
            ||r==Rider.MAGIC_ACTION_SAVE_DC_15||r==Rider.STUDY_ACTION_CHECK_DC_15
            ||r==Rider.CURSE_REPLACES_PRIOR;
    }
    private static boolean inCurseFamily(Rider source,Rider kind){
        if(source==Rider.RIDDLE_CURSE)return isRiddleRider(kind);
        if(source==Rider.SLAAD_EGG_CURSE)return kind==Rider.SLAAD_EGG_CURSE||kind==Rider.SLAAD_EGG_GESTATION_DAYS;
        if(source==Rider.RESTLESS_CURSE)return kind==Rider.RESTLESS_CURSE||kind==Rider.LONG_REST_NO_BENEFIT;
        return kind==source||kind==Rider.CANNOT_REGAIN_HP||kind==Rider.LONG_REST_MAX_HP_NOT_RESTORED
            ||kind==Rider.LONG_REST_NO_BENEFIT||kind==Rider.MAX_HP_LOSS_EVERY_24H;
    }
    private static Effect effect(Rider kind,Request r,int value,String expiry){
        return new Effect(kind,r.actor,r.target,value,expiry);
    }
    private static int mitigate(Target t,Dice part,int[] dice){
        int raw=part.total(dice);
        Defense defense=t.defense(part.type);
        require(defense!=null,"missing_typed_defense_"+part.type);
        return DamageMitigationRules.apply(raw,defense==Defense.IMMUNE,
                defense==Defense.RESISTANT||defense==Defense.BOTH,
                defense==Defense.VULNERABLE||defense==Defense.BOTH);
    }
    private static void require(boolean ok,String message){
        if(!ok)throw new IllegalArgumentException(message);
    }
}
