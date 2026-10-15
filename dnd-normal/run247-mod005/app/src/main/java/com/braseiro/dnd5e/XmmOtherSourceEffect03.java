package com.braseiro.dnd5e;

import java.util.*;

/**
 * Pure Java 8 XMM other_source_effect-03 / lot 09 (nine source-pinned actions).
 *
 * All saving throw and dice outcomes are MANUAL INPUTS. No Android UI,
 * SQLite persistence, timer, world-state, or autonomous dice. A pending
 * effect is a typed integration obligation, not an effect already running
 * in the live combat engine.
 */
public final class XmmOtherSourceEffect03 {
    private XmmOtherSourceEffect03() {}
    public static final String SOURCE_COMMIT="8c026b807fac21862a309379a0c3228a0683198b";

    public enum Action {
        REVENANT("/monster/372/bonus/0/entries/0",0,30),
        SATYR("/monster/386/action/2/entries/0",14,60),
        SPECTATOR("/monster/404/action/2/entries/0",12,90),
        SPHINX_VALOR("/monster/408/action/2/entries/0",20,500),
        SPHINX_WONDER("/monster/409/reaction/0/entries/0",0,30),
        UMBER_HULK("/monster/448/bonus/0/entries/0",14,30),
        UNICORN("/monster/449/legendary/1/entries/0",0,60),
        YOUNG_COPPER("/monster/490/action/3/entries/0",14,30),
        YOUNG_GOLD("/monster/491/action/3/entries/0",17,30);
        public final String pointer;
        public final int saveDc,rangeFt;
        Action(String pointer,int dc,int range){this.pointer=pointer;saveDc=dc;rangeFt=range;}
        public static Action source(String pointer){
            for(Action a:values())if(a.pointer.equals(pointer))return a;
            throw new IllegalArgumentException("unverified_source_pointer");
        }
    }
    public enum Kind { CREATURE, OBJECT }
    public enum Type { PSYCHIC, NECROTIC, THUNDER }
    public enum Defense { NORMAL, RESISTANT, VULNERABLE, BOTH, IMMUNE }
    public enum Choice { NONE, CHARMING, FRIGHTENING }
    public enum Rider {
        VOW_TRACK_DISTANCE_AND_DIRECTION_ANY_PLANE,
        SATYR_CHARMED_INCAPACITATED_DANCE_UNTIL_DAMAGE_OR_ONE_MINUTE,
        SATYR_FRIGHTENED_ONE_MINUTE_OR_LINE_OF_SIGHT_LOST_AT_END_TURN,
        SPECTATOR_CONFUSED_NO_REACTIONS_RANDOM_ATTACK_NEXT_TURN,
        SPECTATOR_PARALYZED_REPEAT_SAVE_END_TURN_ONE_MINUTE,
        SPECTATOR_FRIGHTENED_END_NEXT_TURN,
        SPHINX_VALOR_FRIGHTENED_ONE_MINUTE,
        SPHINX_VALOR_PARALYZED_REPEAT_SAVE_ONE_MINUTE,
        SPHINX_VALOR_PRONE,
        SPHINX_WONDER_ADD_2_TO_TRIGGERING_ROLL,
        UMBER_NO_REACTIONS_UNTIL_START_NEXT_TURN,
        UMBER_NEXT_TURN_DO_NOTHING,
        UMBER_NEXT_TURN_RANDOM_MOVE_NO_ACTION,
        UMBER_NEXT_TURN_RANDOM_MELEE_OR_NOTHING,
        UNICORN_TEMP_HP_3D6,
        UNICORN_AC_PLUS_TWO_UNTIL_END_NEXT_TURN,
        COPPER_NO_REACTION_HALF_SPEED_ACTION_OR_BONUS_UNTIL_END_NEXT_TURN,
        GOLD_STR_D20_DISADVANTAGE_DAMAGE_MINUS_1D6,
        GOLD_REPEAT_SAVE_END_TURN_UNTIL_ONE_MINUTE
    }
    public static final class Target {
        public final String id;
        public final Kind kind;
        public final int hp,maxHp,tempHp;
        private final EnumMap<Type,Defense> defenses;
        public Target(String id,Kind kind,int hp,int maxHp,int tempHp,Map<Type,Defense> defenses){
            need(id!=null&&!id.trim().isEmpty()&&kind!=null
                &&maxHp>=0&&hp>=0&&hp<=maxHp&&tempHp>=0,"invalid_target");
            this.id=id;this.kind=kind;this.hp=hp;this.maxHp=maxHp;this.tempHp=tempHp;
            this.defenses=new EnumMap<>(Type.class);
            if(defenses!=null)this.defenses.putAll(defenses);
        }
        Target updated(int nextHp,int nextTemp){
            return new Target(id,kind,nextHp,maxHp,nextTemp,defenses);
        }
        public Defense defense(Type type){return defenses.get(type);}
    }
    public static final class Selection {
        public final String id;
        public final int distanceFt;
        public final boolean visible,inArea,isEnemy,triggeringAbilityOrSaveCheck;
        public final Boolean saveSucceeded;
        /** Manual dice for this target, none on saves not requiring dice. */
        public final int[] rolledDamage;
        /** An explicit 1d8 for a failed Umber gaze. */
        public final int umberD8;
        public Selection(String id,int distanceFt,boolean visible,boolean inArea,
                boolean isEnemy,boolean triggeredCheck,Boolean saveSucceeded,
                int[] rolledDamage,int umberD8){
            this.id=id;this.distanceFt=distanceFt;this.visible=visible;
            this.inArea=inArea;this.isEnemy=isEnemy;
            this.triggeringAbilityOrSaveCheck=triggeredCheck;
            this.saveSucceeded=saveSucceeded;
            this.rolledDamage=rolledDamage==null?new int[0]:rolledDamage.clone();
            this.umberD8=umberD8;
        }
    }
    public static final class Request {
        public final Action action;
        public final String actor,event,turn;
        public final long revision;
        public final List<Selection> targets;
        /** For Satyr melody; NONE for other actions. */
        public final Choice melody;
        /** Explicit 1d4 ray result; never silently randomised. */
        public final int rayIndex;
        public final boolean rechargeReady,reactionReady,dailyUseReady;
        public Request(Action action,String actor,String event,String turn,long revision,
                List<Selection> selected,Choice melody,int rayIndex,
                boolean rechargeReady,boolean reactionReady,boolean dailyUseReady){
            this.action=Objects.requireNonNull(action);
            this.actor=actor;this.event=event;this.turn=turn;this.revision=revision;
            this.targets=Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(selected)));
            this.melody=melody==null?Choice.NONE:melody;
            this.rayIndex=rayIndex;this.rechargeReady=rechargeReady;
            this.reactionReady=reactionReady;this.dailyUseReady=dailyUseReady;
        }
    }
    public static final class Effect {
        public final Rider kind;
        public final String source,target,expiry;
        public final int magnitude;
        Effect(Rider kind,String source,String target,int magnitude,String expiry){
            this.kind=kind;this.source=source;this.target=target;
            this.magnitude=magnitude;this.expiry=expiry;
        }
    }
    public static final class Result {
        public final Action action;
        public final int rayIndex,roarNumber;
        public final Map<String,Integer> damage;
        public final List<Effect> pending;
        /** No Android/SQLite combat receipt has been created. */
        public final boolean integratedIntoCombat=false;
        private final Map<String,int[]> newStats;
        Result(Action action,int rayIndex,int roar,List<Effect> effects,
                Map<String,Integer> damage,Map<String,int[]> stats){
            this.action=action;this.rayIndex=rayIndex;roarNumber=roar;
            pending=Collections.unmodifiableList(new ArrayList<>(effects));
            this.damage=Collections.unmodifiableMap(new LinkedHashMap<>(damage));
            newStats=stats;
        }
    }
    public static final class Proposal {
        private final Session owner;
        private final String actor,event,turn;
        private final long revision;
        public final Result result;
        Proposal(Session owner,String actor,String event,String turn,long revision,Result result){
            this.owner=owner;this.actor=actor;this.event=event;this.turn=turn;
            this.revision=revision;this.result=result;
        }
    }
    public static final class Session {
        private final Map<String,Target> targets=new LinkedHashMap<>();
        private final Map<String,List<Effect>> active=new LinkedHashMap<>();
        private final Map<String,Integer> roarUses=new HashMap<>();
        private final Map<String,Integer> dailyUses=new HashMap<>();
        private final Set<String> usedTurnActions=new HashSet<>();
        private final Map<String,Set<Integer>> usedRays=new HashMap<>();
        private final Set<String> consumed=new HashSet<>();
        private long revision;
        public Session(Collection<Target> initial){
            need(initial!=null&&!initial.isEmpty(),"missing_initial_targets");
            for(Target t:initial){
                need(t!=null&&!targets.containsKey(t.id),"duplicate_or_invalid_target");
                targets.put(t.id,t);
            }
        }
        public synchronized long revision(){return revision;}
        public synchronized Target target(String id){return targets.get(id);}
        public synchronized List<Effect> effects(String id){
            List<Effect> list=active.get(id);
            return Collections.unmodifiableList(list==null?Collections.<Effect>emptyList():new ArrayList<>(list));
        }
        public synchronized int roarsUsed(String actor){return count(roarUses,actor);}
        private boolean hasWeakening(String target,String actor) {
            for(Effect e:effects(target))if(e.source.equals(actor)
                &&e.kind==Rider.GOLD_STR_D20_DISADVANTAGE_DAMAGE_MINUS_1D6)return true;
            return false;
        }
        private static int count(Map<String,Integer> entries,String id) {
            Integer n=entries.get(id);return n==null?0:n;
        }
        private String key(String actor,Action a,String turn){
            return actor+"|"+a.name()+"|"+turn;
        }
        private static Effect effect(Rider r,Request req,String target,int amount,String expiry){
            return new Effect(r,req.actor,target,amount,expiry);
        }
        private static void diceNone(Selection s){
            need(s.rolledDamage.length==0&&s.umberD8==0,"unused_dice");
        }
        private static int dice(Selection s,int number,int sides,int bonus){
            need(s.rolledDamage.length==number,"missing_or_extra_damage_dice");
            int total=bonus;
            for(int die:s.rolledDamage){
                need(die>=1&&die<=sides,"damage_die_out_of_range");
                total+=die;
            }
            return total;
        }
        private static int mitigate(Target target,Type type,int rolled){
            Defense defense=target.defense(type);
            need(defense!=null,"typed_defense_required_"+type.name());
            return DamageMitigationRules.apply(rolled,defense==Defense.IMMUNE,
                defense==Defense.RESISTANT||defense==Defense.BOTH,
                defense==Defense.VULNERABLE||defense==Defense.BOTH);
        }
        private static int applied(Target t,int damage,Map<String,int[]> stats){
            int absorbed=Math.min(t.tempHp,damage);
            int hp=Math.max(0,t.hp-(damage-absorbed));
            stats.put(t.id,new int[]{hp,t.tempHp-absorbed});
            return hp;
        }
        public synchronized Proposal propose(Request req){
            need(req!=null&&req.actor!=null&&!req.actor.trim().isEmpty()
                &&req.event!=null&&!req.event.trim().isEmpty()
                &&req.turn!=null&&!req.turn.trim().isEmpty(),"missing_event_actor_turn");
            need(req.revision==revision&&!consumed.contains(req.event),
                "stale_or_replayed_event");
            Action a=req.action;
            need(!req.targets.isEmpty(),"explicit_targets_required");
            need(req.rayIndex==0||a==Action.SPECTATOR,"unexpected_ray_roll");
            need(req.melody==Choice.NONE||a==Action.SATYR,"unexpected_melody");
            if(a==Action.SPECTATOR){
                need(req.rayIndex>=1&&req.rayIndex<=4,"1d4_ray_required");
                Set<Integer> used=usedRays.get(key(req.actor,a,req.turn));
                need(used==null||!used.contains(req.rayIndex),
                    "ray_already_used_this_turn_reroll_1d4");
            }
            if(a==Action.SATYR||a==Action.UMBER_HULK)
                need(req.rechargeReady,"recharge_not_ready");
            if(a==Action.SPHINX_WONDER)
                need(req.reactionReady,"sphinx_reaction_not_available");
            if(a==Action.REVENANT||a==Action.SPHINX_WONDER)
                need(req.dailyUseReady,"daily_use_not_available");
            int usedDay=count(dailyUses,key(req.actor,a,"daily"));
            if(a==Action.REVENANT)need(usedDay<1,"revenant_vow_1_per_day");
            if(a==Action.SPHINX_WONDER)need(usedDay<2,"sphinx_burst_2_per_day");
            int roarNumber=0;
            if(a==Action.SPHINX_VALOR){
                roarNumber=count(roarUses,req.actor)+1;
                need(roarNumber<=3,"roar_sequence_exhausted_until_long_rest");
            }
            if(a==Action.UNICORN)
                need(!usedTurnActions.contains(key(req.actor,a,req.turn)),
                    "unicorn_shield_unavailable_until_next_turn");
            HashSet<String> seen=new HashSet<>();
            ArrayList<Effect> effects=new ArrayList<>();
            Map<String,Integer> damage=new LinkedHashMap<>();
            Map<String,int[]> stats=new LinkedHashMap<>();
            for(Selection s:req.targets){
                need(s!=null&&s.id!=null&&!s.id.trim().isEmpty()
                    &&seen.add(s.id)&&s.distanceFt>=0,"duplicate_missing_or_negative_distance");
                Target t=targets.get(s.id);need(t!=null,"unknown_target");
                need(s.distanceFt<=a.rangeFt,"out_of_source_range");
                if(a!=Action.REVENANT&&a!=Action.UNICORN&&a!=Action.SPHINX_WONDER)
                    need(t.kind==Kind.CREATURE,"creature_required");
                if(a==Action.REVENANT){
                    need(req.targets.size()==1&&s.visible,"revenant_requires_visible_single_target");
                    need(s.saveSucceeded==null,"revenant_no_save");
                    diceNone(s);
                    effects.add(effect(Rider.VOW_TRACK_DISTANCE_AND_DIRECTION_ANY_PLANE,
                        req,t.id,0,"until_revenant_vows_again"));
                }else if(a==Action.SATYR){
                    need(s.isEnemy&&s.inArea,"satyr_enemy_within_60ft_emanation");
                    need(req.melody==Choice.CHARMING||req.melody==Choice.FRIGHTENING,
                        "satyr_melody_must_be_selected");
                    need(s.saveSucceeded!=null,"wis14_save_required");
                    if(!s.saveSucceeded){
                        if(req.melody==Choice.CHARMING){
                            diceNone(s);
                            effects.add(effect(Rider.SATYR_CHARMED_INCAPACITATED_DANCE_UNTIL_DAMAGE_OR_ONE_MINUTE,
                                req,t.id,0,"one_minute_or_any_damage"));
                        }else {
                            int dealt=mitigate(t,Type.PSYCHIC,dice(s,2,6,3));
                            need(s.umberD8==0,"unexpected_umber_roll");
                            damage.put(t.id,dealt);
                            applied(t,dealt,stats);
                            effects.add(effect(Rider.SATYR_FRIGHTENED_ONE_MINUTE_OR_LINE_OF_SIGHT_LOST_AT_END_TURN,
                                req,t.id,0,"one_minute_or_end_turn_out_of_line_of_sight"));
                        }
                    }else diceNone(s);
                }else if(a==Action.SPECTATOR){
                    need(req.targets.size()==1&&s.visible,"spectator_single_visible_target");
                    need(s.saveSucceeded!=null,"spectator_wis_or_con12_save_required");
                    int dealt=0;
                    if(req.rayIndex==1||req.rayIndex==3) {
                        if(!s.saveSucceeded){
                            dealt=mitigate(t,Type.PSYCHIC,dice(s,2,4,0));
                            effects.add(effect(req.rayIndex==1?
                                Rider.SPECTATOR_CONFUSED_NO_REACTIONS_RANDOM_ATTACK_NEXT_TURN:
                                Rider.SPECTATOR_FRIGHTENED_END_NEXT_TURN,
                                req,t.id,0,"end_of_target_next_turn"));
                        }else need(s.rolledDamage.length==0,"no_damage_on_success_ray_1_or_3");
                    }else if(req.rayIndex==2) {
                        need(s.rolledDamage.length==0,"paralysis_has_no_damage");
                        if(!s.saveSucceeded)
                            effects.add(effect(Rider.SPECTATOR_PARALYZED_REPEAT_SAVE_END_TURN_ONE_MINUTE,
                                req,t.id,12,"repeat_con12_save_end_turn_or_one_minute"));
                    }else{
                        int raw=dice(s,3,10,0);
                        dealt=mitigate(t,Type.NECROTIC,s.saveSucceeded?raw/2:raw);
                    }
                    need(s.umberD8==0,"unexpected_umber_roll");
                    damage.put(t.id,dealt);
                    applied(t,dealt,stats);
                }else if(a==Action.SPHINX_VALOR){
                    need(s.isEnemy&&s.inArea,"sphinx_500ft_enemy_emanation_required");
                    need(s.saveSucceeded!=null,"sphinx_wis_or_con20_save_required");
                    if(roarNumber==3) {
                        int raw=dice(s,8,10,0);
                        int dealt=mitigate(t,Type.THUNDER,s.saveSucceeded?raw/2:raw);
                        damage.put(t.id,dealt);
                        applied(t,dealt,stats);
                        if(!s.saveSucceeded)effects.add(effect(Rider.SPHINX_VALOR_PRONE,
                            req,t.id,0,"immediate"));
                    }else{
                        need(s.rolledDamage.length==0,"first_two_roars_have_no_damage");
                        if(!s.saveSucceeded)
                            effects.add(effect(roarNumber==1?
                                Rider.SPHINX_VALOR_FRIGHTENED_ONE_MINUTE:
                                Rider.SPHINX_VALOR_PARALYZED_REPEAT_SAVE_ONE_MINUTE,
                                req,t.id,20,roarNumber==1?"one_minute":
                                    "repeat_wis20_save_end_turn_or_one_minute"));
                    }
                    need(s.umberD8==0,"unexpected_umber_roll");
                }else if(a==Action.SPHINX_WONDER){
                    need(req.targets.size()==1&&s.triggeringAbilityOrSaveCheck,
                        "reaction_trigger_is_ability_check_or_save");
                    need(s.saveSucceeded==null,"reaction_has_no_saving_throw");
                    need(s.id.equals(req.actor)||s.distanceFt<=30,
                        "sphinx_reaction_target_out_of_30ft");
                    diceNone(s);
                    effects.add(effect(Rider.SPHINX_WONDER_ADD_2_TO_TRIGGERING_ROLL,
                        req,t.id,2,"immediate_before_triggering_roll_resolves"));
                }else if(a==Action.UMBER_HULK){
                    need(s.inArea,"umber_hulk_30ft_cone_required");
                    need(s.saveSucceeded!=null,"umber_wis14_save_required");
                    need(s.rolledDamage.length==0,"umber_no_damage_roll");
                    if(!s.saveSucceeded){
                        need(s.umberD8>=1&&s.umberD8<=8,"umber_1d8_outcome_required");
                        effects.add(effect(Rider.UMBER_NO_REACTIONS_UNTIL_START_NEXT_TURN,
                            req,t.id,0,"start_of_umber_next_turn"));
                        Rider rider=s.umberD8<=4?Rider.UMBER_NEXT_TURN_DO_NOTHING:
                            s.umberD8<=6?Rider.UMBER_NEXT_TURN_RANDOM_MOVE_NO_ACTION:
                            Rider.UMBER_NEXT_TURN_RANDOM_MELEE_OR_NOTHING;
                        effects.add(effect(rider,req,t.id,s.umberD8,
                            "next_target_turn_only"));
                    }else need(s.umberD8==0,"umber_success_has_no_1d8");
                }else if(a==Action.UNICORN){
                    need(req.targets.size()==1,"unicorn_targets_one_creature");
                    need(s.id.equals(req.actor)||s.visible,"unicorn_target_not_visible");
                    need(s.saveSucceeded==null,"unicorn_no_save");
                    int temp=dice(s,3,6,0);
                    need(s.umberD8==0,"unicorn_no_umber_die");
                    stats.put(t.id,new int[]{t.hp,Math.max(t.tempHp,temp)});
                    effects.add(effect(Rider.UNICORN_TEMP_HP_3D6,req,t.id,temp,"immediate"));
                    effects.add(effect(Rider.UNICORN_AC_PLUS_TWO_UNTIL_END_NEXT_TURN,
                        req,t.id,2,"end_of_unicorn_next_turn"));
                }else if(a==Action.YOUNG_COPPER){
                    need(s.inArea,"copper_30ft_cone_required");
                    need(s.saveSucceeded!=null,"copper_con14_save_required");
                    diceNone(s);
                    if(!s.saveSucceeded)effects.add(effect(
                        Rider.COPPER_NO_REACTION_HALF_SPEED_ACTION_OR_BONUS_UNTIL_END_NEXT_TURN,
                        req,t.id,0,"end_of_target_next_turn"));
                }else if(a==Action.YOUNG_GOLD){
                    need(s.inArea,"gold_30ft_cone_required");
                    need(!hasWeakening(t.id,req.actor),"target_already_affected_by_weakening");
                    need(s.saveSucceeded!=null,"gold_str17_save_required");
                    diceNone(s);
                    if(!s.saveSucceeded){
                        effects.add(effect(Rider.GOLD_STR_D20_DISADVANTAGE_DAMAGE_MINUS_1D6,
                            req,t.id,6,"repeat_str17_save_or_one_minute"));
                        effects.add(effect(Rider.GOLD_REPEAT_SAVE_END_TURN_UNTIL_ONE_MINUTE,
                            req,t.id,17,"auto_success_after_one_minute"));
                    }
                }
            }
            return new Proposal(this,req.actor,req.event,req.turn,revision,
                new Result(a,req.rayIndex,roarNumber,effects,damage,stats));
        }
        public synchronized Result commit(Proposal p){
            need(p!=null&&p.owner==this&&p.revision==revision
                &&!consumed.contains(p.event),"stale_or_foreign_proposal");
            for(Map.Entry<String,int[]> e:p.result.newStats.entrySet()){
                Target target=targets.get(e.getKey());
                need(target!=null,"missing_target_during_commit");
                targets.put(e.getKey(),target.updated(e.getValue()[0],e.getValue()[1]));
            }
            if(p.result.action==Action.REVENANT){
                for(List<Effect> list:active.values())
                    list.removeIf(e->e.kind==Rider.VOW_TRACK_DISTANCE_AND_DIRECTION_ANY_PLANE
                        &&e.source.equals(p.actor));
            }
            for(Effect effect:p.result.pending){
                List<Effect> list=active.get(effect.target);
                if(list==null){list=new ArrayList<>();active.put(effect.target,list);}
                list.add(effect);
            }
            if(p.result.action==Action.SPECTATOR){
                String k=key(p.actor,p.result.action,p.turn);
                Set<Integer> rays=usedRays.get(k);
                if(rays==null){rays=new HashSet<>();usedRays.put(k,rays);}
                need(rays.add(p.result.rayIndex),"ray_repeat_on_commit");
            }
            if(p.result.action==Action.SPHINX_VALOR)
                roarUses.put(p.actor,p.result.roarNumber);
            if(p.result.action==Action.REVENANT||p.result.action==Action.SPHINX_WONDER){
                String k=key(p.actor,p.result.action,"daily");
                dailyUses.put(k,count(dailyUses,k)+1);
            }
            if(p.result.action==Action.UNICORN)
                usedTurnActions.add(key(p.actor,p.result.action,p.turn));
            consumed.add(p.event);revision++;
            return p.result;
        }
        /** Explicit rest event supplied by authoritative session; no clock inferred. */
        public synchronized void longRestForSphinx(String actor){
            need(actor!=null&&!actor.trim().isEmpty(),"sphinx_actor_missing");
            roarUses.remove(actor);
            revision++;
        }
        /** Explicit day rollover. Never called implicitly by this engine. */
        public synchronized void newDayFor(String actor){
            need(actor!=null&&!actor.trim().isEmpty(),"actor_missing");
            dailyUses.remove(key(actor,Action.REVENANT,"daily"));
            dailyUses.remove(key(actor,Action.SPHINX_WONDER,"daily"));
            revision++;
        }
    }
    private static void need(boolean ok,String reason){
        if(!ok)throw new IllegalArgumentException(reason);
    }
}
