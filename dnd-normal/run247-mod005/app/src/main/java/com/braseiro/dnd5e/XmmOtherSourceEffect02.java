package com.braseiro.dnd5e;

import java.util.*;

/**
 * Ten official 2025 XMM other_source_effect source-pointer contracts.
 *
 * Pure Java 8: deterministic rolls and explicit target consent/visibility,
 * replay-safe proposal/commit against IN-MEMORY state. Timed conditions,
 * actor reactions, world entities, recharge, persistent resources and combat
 * positioning are emitted as typed integration obligations; they are NOT
 * automatically applied in an Android battle or written to SQLite.
 */
public final class XmmOtherSourceEffect02 {
    private XmmOtherSourceEffect02(){}
    public static final String SOURCE_COMMIT="8c026b807fac21862a309379a0c3228a0683198b";

    public enum Action {
        NEGATIVE_ENERGY_CONE("/monster/120/bonus/0/entries/0",150,0,"bonus"),
        DEMILICH_ENERGY_DRAIN("/monster/122/legendary/0/entries/0",120,19,"legendary"),
        CATACLYSMIC_EVENT("/monster/140/action/2/entries/0",150,23,"action"),
        CULTIST_ELEMENTAL_ABSORPTION("/monster/141/reaction/0/entries/0",0,0,"reaction"),
        EMPYREAN_BOLSTER("/monster/144/legendary/0/entries/0",30,0,"legendary"),
        FROST_GIANT_WAR_CRY("/monster/161/bonus/0/entries/0",0,0,"bonus"),
        GNOLL_INCITE_RAMPAGE("/monster/211/action/3/entries/0",60,0,"action"),
        GOLD_WYRMLING_WEAKENING("/monster/218/action/3/entries/0",15,13,"action"),
        NALFESHNEE_PURSUIT("/monster/317/reaction/0/entries/0",120,0,"reaction"),
        PIRATE_ADMIRAL_RALLY("/monster/344/bonus/0/entries/0",30,0,"bonus");
        public final String pointer,actionEconomy;
        public final int rangeFeet,saveDc;
        Action(String pointer,int range,int dc,String economy){
            this.pointer=pointer;rangeFeet=range;saveDc=dc;actionEconomy=economy;
        }
        public static Action fromPointer(String p){
            for(Action a:values())if(a.pointer.equals(p))return a;
            throw new IllegalArgumentException("unverified_xmm_pointer");
        }
    }
    public enum Type { ACID, COLD, FIRE, LIGHTNING, THUNDER, BLUDGEONING }
    public enum Defense { NORMAL, RESISTANT, VULNERABLE, BOTH, IMMUNE }
    public enum Kind { HUMANOID, CREATURE, HUMANOID_CORPSE, OBJECT }
    public enum Rider {
        NO_HEAL_UNTIL_TYRANT_NEXT_TURN, RAISE_ZOMBIE_FROM_INTACT_HUMANOID_CORPSE,
        MAX_HP_DRAIN_4D6, DEMILICH_UNAVAILABLE_UNTIL_ITS_NEXT_TURN,
        BURNING, PRONE, SPEED_ZERO_UNTIL_NEXT_TURN,
        STORM_CLOUD_ONE_MINUTE_REPLACES_PRIOR,
        CLOUD_BLIND_DEAF_NO_VERBAL_WHILE_INSIDE,
        BURIED_RESTRAINED_TOTAL_COVER_SUFFOCATING_ESCAPE_ATHLETICS_DC18,
        TEMP_HP_10, REACTION_RESISTANCE_THIS_DAMAGE_INSTANCE,
        ALLY_D20_ADVANTAGE_END_OF_EMPYREAN_NEXT_TURN,
        SELF_D20_ADVANTAGE_END_OF_EMPYREAN_NEXT_TURN,
        WAR_CRY_TEMP_HP_2D10_PLUS_5, WAR_CRY_ATTACK_ADVANTAGE_NEXT_TURN,
        GNOLL_RAMPAGE_ALLY_REACTION_MELEE_ATTACK,
        WEAKENING_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_1D4,
        WEAKENING_REPEAT_STR13_SAVE_END_TURN_AUTO_END_ONE_MINUTE,
        NALFESHNEE_TELEPORT_NEAR_TRIGGER,
        RALLY_ATTACK_AND_SAVE_ADVANTAGE_START_NEXT_TURN,
        RECHARGE_FOUR_TO_SIX, RECHARGE_FIVE_TO_SIX
    }

    public static final class Target {
        public final String id;
        public final Kind kind;
        public final int hp,maxHp,tempHp;
        private final EnumMap<Type,Defense> defenses;
        public Target(String id,Kind kind,int hp,int maxHp,int tempHp,Map<Type,Defense> defense){
            need(id!=null&&!id.trim().isEmpty()&&kind!=null,"target_id_kind_required");
            need(maxHp>=0&&hp>=0&&hp<=maxHp&&tempHp>=0,"invalid_target_hp");
            this.id=id;this.kind=kind;this.hp=hp;this.maxHp=maxHp;this.tempHp=tempHp;
            defenses=new EnumMap<>(Type.class);
            if(defense!=null)defenses.putAll(defense);
        }
        public Defense defense(Type type){return defenses.get(type);}
        private Target update(int hp,int maxHp,int tempHp){
            return new Target(id,kind,hp,maxHp,tempHp,defenses);
        }
    }
    /**
     * Each selected creature/area inhabitant has separately supplied geometry,
     * saving throw, dice and reaction context. No target is inferred by proximity.
     */
    public static final class Selection {
        public final String id;
        public final int distanceFeet;
        public final boolean visibleOrHeard,withinArea,entersOrStartsCloud,
            fullyInsideCloud,intactCorpse,hasRampage,acceptsReaction;
        public final Boolean saveSucceeded;
        public final int[] primaryDice,secondaryDice;
        public Selection(String id,int distanceFeet,boolean visibleOrHeard,
                boolean withinArea,boolean entersOrStartsCloud,boolean fullyInsideCloud,
                boolean intactCorpse,boolean hasRampage,boolean acceptsReaction,
                Boolean saved,int[] primary,int[] secondary){
            this.id=id;this.distanceFeet=distanceFeet;
            this.visibleOrHeard=visibleOrHeard;this.withinArea=withinArea;
            this.entersOrStartsCloud=entersOrStartsCloud;this.fullyInsideCloud=fullyInsideCloud;
            this.intactCorpse=intactCorpse;this.hasRampage=hasRampage;
            this.acceptsReaction=acceptsReaction;saveSucceeded=saved;
            primaryDice=primary==null?new int[0]:primary.clone();
            secondaryDice=secondary==null?new int[0]:secondary.clone();
        }
    }
    public static final class Request {
        public final Action action;
        public final String actor,event,turn;
        public final long revision;
        public final List<Selection> selected;
        public final int cataclysmRoll,sourceToPointFeet,destinationToTriggerFeet,
            triggeringDamage,warCryD10A,warCryD10B;
        public final Type triggeringType;
        public final boolean sourcePointVisible,sourcePointGrounded,
            reactionAvailable,rechargeReady,teleportDestinationValid,
            triggeringCreatureEndedMove,triggerAttackRoll,onePerDayReady;
        public Request(Action action,String actor,String event,String turn,long revision,
                List<Selection> selected,int variant,int pointFeet,int teleportFeet,
                int triggeringDamage,Type triggeringType,
                int warCryA,int warCryB,
                boolean pointVisible,boolean pointGrounded,boolean reactionAvailable,
                boolean rechargeReady,boolean teleportValid,boolean endedMove,
                boolean triggeringAttackRoll,boolean onePerDayReady){
            this.action=Objects.requireNonNull(action);this.actor=actor;this.event=event;this.turn=turn;
            this.revision=revision;
            this.selected=Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(selected)));
            cataclysmRoll=variant;sourceToPointFeet=pointFeet;
            destinationToTriggerFeet=teleportFeet;this.triggeringDamage=triggeringDamage;
            this.triggeringType=triggeringType;warCryD10A=warCryA;warCryD10B=warCryB;
            sourcePointVisible=pointVisible;sourcePointGrounded=pointGrounded;
            this.reactionAvailable=reactionAvailable;this.rechargeReady=rechargeReady;
            teleportDestinationValid=teleportValid;triggeringCreatureEndedMove=endedMove;
            this.triggerAttackRoll=triggeringAttackRoll;this.onePerDayReady=onePerDayReady;
        }
    }
    public static final class Effect {
        public final Rider rider;
        public final String source,target,expiry;
        public final int magnitude;
        Effect(Rider rider,String source,String target,int value,String expiry){
            this.rider=rider;this.source=source;this.target=target;
            magnitude=value;this.expiry=expiry;
        }
    }
    public static final class Result {
        public final Action action;
        public final List<Effect> effects;
        public final Map<String,Integer> damageByTarget;
        /** This only updates Session memory, never Android/SQLite. */
        public final boolean persistedToCombat=false;
        private final Map<String,int[]> proposedStats;
        Result(Action action,List<Effect> effects,Map<String,Integer> damage,Map<String,int[]> stats){
            this.action=action;
            this.effects=Collections.unmodifiableList(new ArrayList<>(effects));
            this.damageByTarget=Collections.unmodifiableMap(new LinkedHashMap<>(damage));
            this.proposedStats=stats;
        }
    }
    public static final class Proposal {
        private final Session owner;
        private final long revision;
        private final String event;
        public final Result result;
        Proposal(Session owner,long revision,String event,Result result){
            this.owner=owner;this.revision=revision;this.event=event;this.result=result;
        }
    }
    public static final class Session {
        private final Map<String,Target> targets=new LinkedHashMap<>();
        private final Map<String,List<Effect>> activeEffects=new HashMap<>();
        private final Set<String> consumedEvents=new HashSet<>();
        private final Set<String> usedTurnActions=new HashSet<>();
        private final Set<String> usedDayActions=new HashSet<>();
        private long revision;
        public Session(Collection<Target> initial){
            need(initial!=null&&!initial.isEmpty(),"initial_targets");
            for(Target t:initial){
                need(t!=null&&!targets.containsKey(t.id),"duplicate_target");
                targets.put(t.id,t);
            }
        }
        public synchronized long revision(){return revision;}
        public synchronized Target target(String id){return targets.get(id);}
        public synchronized List<Effect> effects(String id){
            List<Effect> e=activeEffects.get(id);
            return Collections.unmodifiableList(e==null?Collections.<Effect>emptyList():new ArrayList<>(e));
        }
        private boolean hasWeakening(String target,String actor) {
            for(Effect e:effects(target))
                if(e.rider==Rider.WEAKENING_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_1D4
                   &&e.source.equals(actor))return true;
            return false;
        }
        public synchronized Proposal propose(Request req){
            need(req!=null&&req.actor!=null&&!req.actor.trim().isEmpty()
                &&req.event!=null&&!req.event.trim().isEmpty()
                &&req.turn!=null&&!req.turn.trim().isEmpty(),"actor_event_turn_required");
            need(req.revision==revision&&!consumedEvents.contains(req.event),
                "stale_or_replayed_event");
            need(req.sourceToPointFeet>=0&&req.destinationToTriggerFeet>=0
                &&req.triggeringDamage>=0,"bad_geometry_or_damage");
            Action action=req.action;
            String onceTurn=req.actor+"|"+action.name()+"|"+req.turn;
            String onceDay=req.actor+"|"+action.name();
            if(action==Action.DEMILICH_ENERGY_DRAIN||action==Action.EMPYREAN_BOLSTER)
                need(!usedTurnActions.contains(onceTurn),"once_until_start_next_turn");
            if(action==Action.CULTIST_ELEMENTAL_ABSORPTION
                ||action==Action.PIRATE_ADMIRAL_RALLY)
                need(req.onePerDayReady&&!usedDayActions.contains(onceDay),"daily_use_spent");
            if(action==Action.CATACLYSMIC_EVENT||action==Action.FROST_GIANT_WAR_CRY
                ||action==Action.GNOLL_INCITE_RAMPAGE)
                need(req.rechargeReady,"recharge_not_available");
            if(action==Action.CULTIST_ELEMENTAL_ABSORPTION||action==Action.NALFESHNEE_PURSUIT)
                need(req.reactionAvailable,"actor_reaction_required");

            LinkedHashMap<String,Integer> damage=new LinkedHashMap<>();
            LinkedHashMap<String,int[]> stats=new LinkedHashMap<>();
            ArrayList<Effect> effects=new ArrayList<>();
            HashSet<String> seen=new HashSet<>();
            boolean catEvent=action==Action.CATACLYSMIC_EVENT;
            int variant=req.cataclysmRoll;
            if(catEvent) {
                need(variant>=1&&variant<=4,"cataclysm_random_1d4_required");
                need(req.sourceToPointFeet<=150&&req.sourcePointVisible,
                    "cataclysm_area_point_must_be_visible_within_150ft");
                if(variant==4)need(req.sourcePointGrounded,"swallowing_earth_ground_point_required");
                if(variant==3)effects.add(effect(Rider.STORM_CLOUD_ONE_MINUTE_REPLACES_PRIOR,req.actor,
                    req.actor,60,"one_minute_or_next_cataclysmic_event"));
                effects.add(effect(Rider.RECHARGE_FOUR_TO_SIX,req.actor,req.actor,4,
                    "until_recharge_roll_4_to_6"));
            }
            if(action==Action.FROST_GIANT_WAR_CRY)
                effects.add(effect(Rider.RECHARGE_FIVE_TO_SIX,req.actor,req.actor,5,
                    "until_recharge_roll_5_to_6"));
            if(action==Action.GNOLL_INCITE_RAMPAGE)
                effects.add(effect(Rider.RECHARGE_FIVE_TO_SIX,req.actor,req.actor,5,
                    "until_recharge_roll_5_to_6"));

            if(action==Action.CULTIST_ELEMENTAL_ABSORPTION) {
                need(req.selected.size()==1&&req.triggeringType!=null
                    &&req.triggeringDamage>0&&reactiveType(req.triggeringType),
                    "elemental_damage_trigger_required");
                need(req.selected.get(0).id.equals(req.actor),"cultist_must_be_target_of_damage");
                Target actor=targets.get(req.actor);need(actor!=null,"actor_missing");
                effects.add(effect(Rider.REACTION_RESISTANCE_THIS_DAMAGE_INSTANCE,req.actor,
                    req.actor,req.triggeringDamage,"only_triggering_damage_instance"));
                effects.add(effect(Rider.TEMP_HP_10,req.actor,req.actor,10,"immediate"));
                stats.put(actor.id,new int[]{actor.hp,actor.maxHp,Math.max(actor.tempHp,10)});
            }
            if(action==Action.EMPYREAN_BOLSTER) {
                Target actor=targets.get(req.actor);need(actor!=null,"empyrean_missing");
                effects.add(effect(Rider.TEMP_HP_10,req.actor,req.actor,10,"immediate"));
                effects.add(effect(Rider.SELF_D20_ADVANTAGE_END_OF_EMPYREAN_NEXT_TURN,
                    req.actor,req.actor,0,"end_of_empyrean_next_turn"));
                stats.put(actor.id,new int[]{actor.hp,actor.maxHp,Math.max(actor.tempHp,10)});
            }
            need(!req.selected.isEmpty()||action==Action.EMPYREAN_BOLSTER,
                "at_least_one_explicit_target_required");
            if(action==Action.DEMILICH_ENERGY_DRAIN||action==Action.GNOLL_INCITE_RAMPAGE
                    ||action==Action.GOLD_WYRMLING_WEAKENING&&req.selected.size()>99
                    ||action==Action.FROST_GIANT_WAR_CRY||action==Action.NALFESHNEE_PURSUIT)
                if(action!=Action.GOLD_WYRMLING_WEAKENING)
                    need(req.selected.size()==1,"single_target_required");
            if(action==Action.PIRATE_ADMIRAL_RALLY)
                need(req.selected.size()>=1&&req.selected.size()<=3,"rally_one_to_three_targets");
            for(Selection choice:req.selected) {
                need(choice!=null&&choice.id!=null&&!choice.id.trim().isEmpty()
                    &&seen.add(choice.id),"duplicate_or_missing_target");
                Target t=targets.get(choice.id);need(t!=null,"unknown_target");
                need(choice.distanceFeet>=0,"invalid_distance");
                if(action==Action.NEGATIVE_ENERGY_CONE) {
                    need(choice.withinArea&&choice.distanceFeet<=150,"tyrant_150ft_cone_required");
                    if(t.kind==Kind.HUMANOID_CORPSE){
                        need(choice.intactCorpse,"intact_humanoid_corpse_required");
                        effects.add(effect(Rider.RAISE_ZOMBIE_FROM_INTACT_HUMANOID_CORPSE,
                            req.actor,t.id,0,"immediate_zombie_same_initiative"));
                    } else if(t.kind!=Kind.OBJECT)
                        effects.add(effect(Rider.NO_HEAL_UNTIL_TYRANT_NEXT_TURN,
                            req.actor,t.id,0,"start_of_tyrant_next_turn"));
                } else if(action==Action.DEMILICH_ENERGY_DRAIN) {
                    need(choice.distanceFeet<=120&&choice.visibleOrHeard
                        &&t.kind!=Kind.OBJECT&&t.kind!=Kind.HUMANOID_CORPSE,
                        "demilich_creature_visible_120ft_required");
                    need(choice.saveSucceeded!=null,"con19_save_required");
                    if(!choice.saveSucceeded){
                        int lost=rolled(choice.primaryDice,4,6,0,"demilich_4d6");
                        int newMax=Math.max(0,t.maxHp-lost);
                        stats.put(t.id,new int[]{Math.min(t.hp,newMax),newMax,t.tempHp});
                        effects.add(effect(Rider.MAX_HP_DRAIN_4D6,req.actor,t.id,lost,
                            "until_max_hp_restored"));
                    } else noRolls(choice);
                    effects.add(effect(Rider.DEMILICH_UNAVAILABLE_UNTIL_ITS_NEXT_TURN,
                        req.actor,req.actor,0,"start_of_demilich_next_turn"));
                } else if(catEvent) {
                    need(choice.withinArea&&t.kind!=Kind.OBJECT&&t.kind!=Kind.HUMANOID_CORPSE,
                        "cataclysm_creature_within_area_required");
                    if(variant==3){
                        if(choice.fullyInsideCloud)effects.add(effect(
                            Rider.CLOUD_BLIND_DEAF_NO_VERBAL_WHILE_INSIDE,req.actor,t.id,0,
                            "while_entirely_inside_storm_cloud"));
                        if(!choice.entersOrStartsCloud){noRolls(choice);continue;}
                    }
                    need(choice.saveSucceeded!=null,"cataclysm_save_dc23_required");
                    int amount=0;
                    if(variant==1) {
                        amount=typedDamage(t,Type.FIRE,choice.primaryDice,13,6,0,
                            choice.saveSucceeded);
                        need(choice.secondaryDice.length==0,"unexpected_cataclysm_secondary_dice");
                        effects.add(effect(Rider.BURNING,req.actor,t.id,0,"until_burning_extinguished"));
                    } else if(variant==2) {
                        amount=typedDamage(t,Type.BLUDGEONING,choice.primaryDice,5,8,0,
                            choice.saveSucceeded)+typedDamage(t,Type.COLD,choice.secondaryDice,5,8,0,
                            choice.saveSucceeded);
                        effects.add(effect(Rider.SPEED_ZERO_UNTIL_NEXT_TURN,req.actor,t.id,0,
                            "end_of_target_next_turn"));
                        if(!choice.saveSucceeded)effects.add(effect(Rider.PRONE,req.actor,t.id,0,"immediate"));
                    } else if(variant==3) {
                        amount=typedDamage(t,Type.LIGHTNING,choice.primaryDice,4,8,0,
                            choice.saveSucceeded)+typedDamage(t,Type.THUNDER,choice.secondaryDice,4,8,0,
                            choice.saveSucceeded);
                    } else if(variant==4) {
                        amount=typedDamage(t,Type.BLUDGEONING,choice.primaryDice,4,8,0,
                            choice.saveSucceeded)+typedDamage(t,Type.ACID,choice.secondaryDice,4,8,0,
                            choice.saveSucceeded);
                        if(!choice.saveSucceeded){
                            effects.add(effect(Rider.PRONE,req.actor,t.id,0,"immediate"));
                            effects.add(effect(
                                Rider.BURIED_RESTRAINED_TOTAL_COVER_SUFFOCATING_ESCAPE_ATHLETICS_DC18,
                                req.actor,t.id,18,"until_athletics_dc18_escape"));
                        }
                    }
                    applyDamage(t,amount,damage,stats);
                } else if(action==Action.CULTIST_ELEMENTAL_ABSORPTION){
                    need(t.id.equals(req.actor),"cultist_self_only");
                    noRolls(choice);
                } else if(action==Action.EMPYREAN_BOLSTER) {
                    need(t.id.equals(req.actor)||choice.distanceFeet<=30,
                        "empyrean_ally_out_of_30ft");
                    need(t.id.equals(req.actor)||choice.visibleOrHeard,
                        "explicit_empyrean_ally_context_required");
                    noRolls(choice);
                    if(!t.id.equals(req.actor))
                        effects.add(effect(Rider.ALLY_D20_ADVANTAGE_END_OF_EMPYREAN_NEXT_TURN,
                            req.actor,t.id,0,"end_of_empyrean_next_turn"));
                } else if(action==Action.FROST_GIANT_WAR_CRY) {
                    need(choice.visibleOrHeard&&t.kind!=Kind.OBJECT,
                        "war_cry_target_must_see_or_hear");
                    int thp=rolled(new int[]{req.warCryD10A,req.warCryD10B},2,10,5,"war_cry_2d10_5");
                    stats.put(t.id,new int[]{t.hp,t.maxHp,Math.max(t.tempHp,thp)});
                    effects.add(effect(Rider.WAR_CRY_TEMP_HP_2D10_PLUS_5,req.actor,t.id,thp,
                        "immediate"));
                    effects.add(effect(Rider.WAR_CRY_ATTACK_ADVANTAGE_NEXT_TURN,req.actor,t.id,0,
                        "start_of_frost_giant_next_turn"));
                    noRolls(choice);
                } else if(action==Action.GNOLL_INCITE_RAMPAGE) {
                    need(!t.id.equals(req.actor)&&choice.distanceFeet<=60
                        &&choice.visibleOrHeard&&choice.hasRampage,
                        "gnoll_target_requires_visible_other_rampage_creature_60ft");
                    noRolls(choice);
                    if(choice.acceptsReaction)
                        effects.add(effect(Rider.GNOLL_RAMPAGE_ALLY_REACTION_MELEE_ATTACK,
                            req.actor,t.id,0,"immediate_reaction_requires_available_reaction"));
                } else if(action==Action.GOLD_WYRMLING_WEAKENING) {
                    need(choice.withinArea&&choice.distanceFeet<=15
                        &&t.kind!=Kind.OBJECT&&t.kind!=Kind.HUMANOID_CORPSE,
                        "gold_wyrmling_str13_cone_15ft");
                    need(choice.saveSucceeded!=null,"gold_wyrmling_str13_save_required");
                    noRolls(choice);
                    if(!choice.saveSucceeded) {
                        need(!hasWeakening(t.id,req.actor),"already_affected_by_same_breath");
                        effects.add(effect(Rider.WEAKENING_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_1D4,
                            req.actor,t.id,4,"repeated_str13_save_or_one_minute"));
                        effects.add(effect(Rider.WEAKENING_REPEAT_STR13_SAVE_END_TURN_AUTO_END_ONE_MINUTE,
                            req.actor,t.id,13,"auto_success_after_one_minute"));
                    }
                } else if(action==Action.NALFESHNEE_PURSUIT) {
                    need(!t.id.equals(req.actor)&&choice.distanceFeet<=120
                        &&choice.visibleOrHeard&&req.triggeringCreatureEndedMove
                        &&req.teleportDestinationValid&&req.destinationToTriggerFeet<=10,
                        "pursuit_requires_visible_creature_ending_move_and_valid_teleport");
                    noRolls(choice);
                    effects.add(effect(Rider.NALFESHNEE_TELEPORT_NEAR_TRIGGER,req.actor,
                        req.actor,req.destinationToTriggerFeet,
                        "immediate_reaction_teleport_to_within_10ft_of_target"));
                } else if(action==Action.PIRATE_ADMIRAL_RALLY) {
                    need(!t.id.equals(req.actor)&&choice.distanceFeet<=30
                        &&choice.visibleOrHeard&&t.kind!=Kind.OBJECT,
                        "rally_other_visible_creature_within_30ft");
                    noRolls(choice);
                    effects.add(effect(Rider.RALLY_ATTACK_AND_SAVE_ADVANTAGE_START_NEXT_TURN,
                        req.actor,t.id,0,"start_of_pirate_next_turn"));
                }
            }
            return new Proposal(this,revision,req.event,new Result(action,effects,damage,stats));
        }
        public synchronized Result commit(Proposal p){
            need(p!=null&&p.owner==this&&p.revision==revision
                &&!consumedEvents.contains(p.event),"stale_or_foreign_proposal");
            for(Map.Entry<String,int[]> e:p.result.proposedStats.entrySet()){
                Target old=targets.get(e.getKey());
                need(old!=null,"proposed_target_missing");
                int[] v=e.getValue();
                targets.put(e.getKey(),old.update(v[0],v[1],v[2]));
            }
            for(Effect e:p.result.effects){
                List<Effect> list=activeEffects.get(e.target);
                if(list==null){list=new ArrayList<>();activeEffects.put(e.target,list);}
                list.add(e);
            }
            Action a=p.result.action;
            // Reservation must be keyed by actor and turn, not the target.
            for(Effect e:p.result.effects) {
                if(a==Action.DEMILICH_ENERGY_DRAIN
                    &&e.rider==Rider.DEMILICH_UNAVAILABLE_UNTIL_ITS_NEXT_TURN)
                    usedTurnActions.add(e.source+"|"+a.name()+"|"+currentTurnFromEvent(p.event));
            }
            consumedEvents.add(p.event);revision++;
            return p.result;
        }
        private String currentTurnFromEvent(String event){return event.split("#",2)[0];}
    }
    private static boolean reactiveType(Type t){
        return t==Type.ACID||t==Type.COLD||t==Type.FIRE
            ||t==Type.LIGHTNING||t==Type.THUNDER;
    }
    private static void noRolls(Selection s){
        need(s.primaryDice.length==0&&s.secondaryDice.length==0
            &&s.saveSucceeded==null,"unexpected_roll_or_save");
    }
    private static int rolled(int[] dice,int count,int sides,int modifier,String name){
        need(dice!=null&&dice.length==count,name+"_dice_required");
        int sum=modifier;
        for(int die:dice){need(die>=1&&die<=sides,name+"_die_out_of_range");sum+=die;}
        return sum;
    }
    private static int typedDamage(Target t,Type type,int[] dice,int count,int sides,
            int modifier,boolean saved){
        int raw=rolled(dice,count,sides,modifier,type.name());
        if(saved)raw/=2;
        Defense def=t.defense(type);
        need(def!=null,"missing_typed_defense_"+type.name());
        return DamageMitigationRules.apply(raw,def==Defense.IMMUNE,
            def==Defense.RESISTANT||def==Defense.BOTH,
            def==Defense.VULNERABLE||def==Defense.BOTH);
    }
    private static void applyDamage(Target target,int amount,Map<String,Integer> damage,
            Map<String,int[]> stats){
        damage.put(target.id,amount);
        int tempSpent=Math.min(target.tempHp,amount);
        int hp=Math.max(0,target.hp-(amount-tempSpent));
        stats.put(target.id,new int[]{hp,target.maxHp,target.tempHp-tempSpent});
    }
    private static Effect effect(Rider r,String source,String target,int amount,String expiry){
        return new Effect(r,source,target,amount,expiry);
    }
    private static void need(boolean yes,String reason){
        if(!yes)throw new IllegalArgumentException(reason);
    }
}
