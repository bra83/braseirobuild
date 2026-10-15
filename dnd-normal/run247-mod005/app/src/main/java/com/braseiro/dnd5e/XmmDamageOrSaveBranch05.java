package com.braseiro.dnd5e;

import java.util.*;

/**
 * XMM source-pinned 12-action independent deterministic core (batch 05).
 *
 * All dice and save/attack outcomes arrive from the host. Proposals must be
 * approved before committing to this ephemeral Session. Timed conditions,
 * healing, teleportation, creature transformation, recharge and world events
 * are external adapter obligations, NOT live Android combat/SQLite actions.
 */
public final class XmmDamageOrSaveBranch05 {
    private XmmDamageOrSaveBranch05(){}
    public static final String SOURCE_COMMIT="8c026b807fac21862a309379a0c3228a0683198b";
    public enum Type { FIRE, FORCE, PIERCING, NECROTIC, SLASHING }
    public enum Defense { NORMAL, RESISTANT, VULNERABLE, BOTH, IMMUNE }
    public enum Kind { HUMANOID, CREATURE, CONSTRUCT, UNDEAD, OBJECT }
    public enum Form { BEAST, HYBRID, HUMANOID, OTHER }
    public enum Choice { NONE, TELEPORT, SAP, MANEUVER }
    public enum Rider {
        SPEED_MINUS_10_UNTIL_NEXT_TURN, RECHARGE_SIX,
        TELEPORT_TARGET_UP_TO_10FT, ACTOR_HEALS_NECROTIC_DAMAGE,
        MAX_HP_DRAIN_NECROTIC, MAX_HP_DRAIN_TOTAL,
        WARRIOR_SAP_NEXT_ATTACK_DISADVANTAGE, WARRIOR_MANEUVER_ALLY_REACTION_HALF_SPEED,
        LYCANTHROPY_CURSE, IMMUNE_TO_THIS_LYCANTHROPE_CURSE_24H,
        TRANSFORM_TO_LYCANTHROPE_AT_ZERO_HP_DM_CONTROL,
        ZOMBIE_RISES_AFTER_24H_CAP_12, NEXT_ATTACK_AGAINST_TARGET_ADVANTAGE
    }
    public static final class Dice {
        public final Type type;
        public final int count,sides,bonus;
        Dice(Type t,int c,int s,int b){type=t;count=c;sides=s;bonus=b;}
        int roll(int[] values){
            need(values!=null&&values.length==count,"missing_or_extra_dice");
            int total=bonus;
            for(int n:values){need(n>=1&&n<=sides,"invalid_die");total+=n;}
            return total;
        }
    }
    private static Dice dice(Type t,int count,int sides,int bonus){return new Dice(t,count,sides,bonus);}
    public enum Action {
        STEAM_MEPHIT("/monster/416/action/1/entries/0",0,0,15,10,Kind.CREATURE,
                dice(Type.FIRE,2,4,0),null),
        ULTROLOTH("/monster/447/action/1/entries/0",9,15,0,0,null,
                dice(Type.FORCE,6,6,4),null),
        VAMPIRE_NIGHTBRINGER("/monster/452/action/1/entries/0",7,5,0,0,null,
                dice(Type.PIERCING,1,6,4),dice(Type.NECROTIC,3,6,0)),
        VAMPIRE_UMBRAL_LORD("/monster/454/bonus/0/entries/0",0,0,30,18,Kind.CREATURE,
                dice(Type.NECROTIC,4,6,0),null),
        WARRIOR_COMMANDER("/monster/463/action/1/entries/0",9,5,0,0,null,
                dice(Type.SLASHING,4,6,5),null),
        WEREBEAR("/monster/469/action/1/entries/0",7,5,0,14,null,
                dice(Type.PIERCING,2,12,4),null),
        WEREBOAR("/monster/470/action/1/entries/0",5,5,0,12,null,
                dice(Type.PIERCING,2,8,3),null),
        WERERAT("/monster/471/action/1/entries/0",5,5,0,11,null,
                dice(Type.PIERCING,2,4,3),null),
        WERETIGER("/monster/472/action/1/entries/0",5,5,0,13,null,
                dice(Type.PIERCING,2,8,3),null),
        WEREWOLF("/monster/473/action/1/entries/0",5,5,0,12,null,
                dice(Type.PIERCING,2,8,3),null),
        WIGHT("/monster/475/action/3/entries/0",0,0,5,13,Kind.CREATURE,
                dice(Type.NECROTIC,1,8,2),null),
        WORG("/monster/480/action/0/entries/0",5,5,0,0,null,
                dice(Type.PIERCING,1,8,3),null);
        public final String pointer;
        public final int toHit,reachFt,rangeFt,saveDc;
        public final Kind saveCreatureKind;
        public final Dice first,second;
        Action(String p,int hit,int reach,int range,int save,Kind k,Dice a,Dice b) {
            pointer=p;toHit=hit;reachFt=reach;rangeFt=range;saveDc=save;
            saveCreatureKind=k;first=a;second=b;
        }
        public boolean savesInsteadOfAttack(){
            return this==STEAM_MEPHIT||this==VAMPIRE_UMBRAL_LORD||this==WIGHT;
        }
        public boolean isLycanthrope(){
            return this==WEREBEAR||this==WEREBOAR||this==WERERAT
                ||this==WERETIGER||this==WEREWOLF;
        }
        public static Action source(String ptr){
            for(Action a:values())if(a.pointer.equals(ptr))return a;
            throw new IllegalArgumentException("source_pointer_mismatch");
        }
    }
    public static final class Target {
        public final String id;
        public final Kind kind;
        public final int hp,maxHp;
        private final EnumMap<Type,Defense> defenses;
        public Target(String id,Kind kind,int hp,int maxHp,Map<Type,Defense> defenses){
            need(id!=null&&!id.trim().isEmpty()&&kind!=null&&maxHp>=0&&hp>=0&&hp<=maxHp,
                "invalid_target");
            this.id=id;this.kind=kind;this.hp=hp;this.maxHp=maxHp;
            this.defenses=new EnumMap<>(Type.class);
            if(defenses!=null)this.defenses.putAll(defenses);
        }
        Target withStats(int hp,int max){return new Target(id,kind,hp,max,defenses);}
        Defense defense(Type t){return defenses.get(t);}
    }
    public static final class Effect {
        public final Rider rider;
        public final String source,target,expires;
        public final int amount;
        Effect(Rider kind,String source,String target,int amount,String expires){
            rider=kind;this.source=source;this.target=target;this.amount=amount;this.expires=expires;
        }
    }
    public static final class Request {
        public final Action action;
        public final String actor,target,event,ally;
        public final int distanceFt,teleportFeet,actorZombiesControlled;
        public final long revision;
        public final Boolean hit,saveSucceeded;
        public final int[] firstDice,secondDice;
        public final Choice choice;
        public final Form form;
        public final boolean targetVisible,underwaterResistanceOnly,allyCanReact,allyCanSeeOrHear,teleportSpaceValid;
        public Request(Action action,String actor,String target,String event,long revision,
                int distanceFt,Boolean hit,Boolean saveSucceeded,int[] first,int[] second,
                Choice choice,Form form,String ally,int teleportFeet,int actorZombiesControlled,
                boolean targetVisible,boolean underwaterResistanceOnly,boolean allyCanReact,
                boolean allyCanSeeOrHear,boolean teleportSpaceValid){
            this.action=Objects.requireNonNull(action);this.actor=actor;this.target=target;this.event=event;
            this.revision=revision;this.distanceFt=distanceFt;this.hit=hit;this.saveSucceeded=saveSucceeded;
            firstDice=first==null?new int[0]:first.clone();secondDice=second==null?new int[0]:second.clone();
            this.choice=choice==null?Choice.NONE:choice;this.form=form==null?Form.OTHER:form;
            this.ally=ally;this.teleportFeet=teleportFeet;this.actorZombiesControlled=actorZombiesControlled;
            this.targetVisible=targetVisible;this.underwaterResistanceOnly=underwaterResistanceOnly;
            this.allyCanReact=allyCanReact;this.allyCanSeeOrHear=allyCanSeeOrHear;
            this.teleportSpaceValid=teleportSpaceValid;
        }
    }
    public static final class Result {
        public final Action action;
        public final int totalDamage,necroticDamage,hp,maxHp;
        public final List<Effect> pending;
        /** This is a pure in-memory proof, not an Android combat receipt. */
        public final boolean persistedToAndroid=false;
        Result(Action action,int total,int necrotic,int hp,int max,List<Effect> pending){
            this.action=action;totalDamage=total;necroticDamage=necrotic;this.hp=hp;
            maxHp=max;this.pending=Collections.unmodifiableList(new ArrayList<>(pending));
        }
    }
    public static final class Proposal {
        private final Session owner;
        private final long revision;
        private final String event,target;
        public final Result result;
        Proposal(Session owner,long revision,String event,String target,Result result){
            this.owner=owner;this.revision=revision;this.event=event;this.target=target;this.result=result;
        }
    }
    public static final class Session {
        private final Map<String,Target> targets=new LinkedHashMap<>();
        private final Map<String,List<Effect>> pending=new LinkedHashMap<>();
        private final Set<String> consumed=new HashSet<>();
        private long revision;
        public Session(Collection<Target> initial){
            need(initial!=null&&!initial.isEmpty(),"missing_targets");
            for(Target t:initial){
                need(t!=null&&!targets.containsKey(t.id),"duplicate_target");
                targets.put(t.id,t);
            }
        }
        public synchronized long revision(){return revision;}
        public synchronized Target target(String id){return targets.get(id);}
        public synchronized List<Effect> pending(String id){
            List<Effect> list=pending.get(id);
            return Collections.unmodifiableList(list==null?Collections.<Effect>emptyList():new ArrayList<>(list));
        }
        private boolean hasImmunity(String target,String actor,Action a){
            List<Effect> list=pending.get(target);
            if(list==null)return false;
            for(Effect e:list)if(e.rider==Rider.IMMUNE_TO_THIS_LYCANTHROPE_CURSE_24H
                    &&e.source.equals(actor)&&e.expires.equals("24_hours_"+a.name()))return true;
            return false;
        }
        public synchronized Proposal propose(Request r){
            need(r!=null&&r.actor!=null&&!r.actor.trim().isEmpty()
                    &&r.event!=null&&!r.event.trim().isEmpty(),"missing_actor_or_event");
            need(r.revision==revision&&!consumed.contains(r.event),"stale_or_duplicate_event");
            Target t=targets.get(r.target);
            need(t!=null&&r.distanceFt>=0,"missing_target_or_range");
            Action a=r.action;
            if(a.savesInsteadOfAttack()){
                need(r.hit==null&&r.saveSucceeded!=null,"save_result_required");
                need(r.distanceFt<=a.rangeFt&&r.targetVisible,"save_area_or_visibility_required");
                if(a==Action.STEAM_MEPHIT)need(t.kind!=Kind.OBJECT,"steam_targets_creatures");
                if(a==Action.VAMPIRE_UMBRAL_LORD)
                    need(t.kind!=Kind.CONSTRUCT&&t.kind!=Kind.UNDEAD&&t.kind!=Kind.OBJECT,
                        "vampire_drain_ineligible_target");
                if(a==Action.WIGHT)need(t.kind!=Kind.OBJECT,"wight_targets_creatures");
            }else{
                need(r.hit!=null&&r.distanceFt<=a.reachFt,"attack_hit_and_reach_required");
                if(a==Action.ULTROLOTH)need(r.targetVisible,"whip_teleport_visibility_context");
                if(a.isLycanthrope())need(r.form==Form.BEAST||r.form==Form.HYBRID,
                    "lycanthrope_requires_beast_or_hybrid_form");
            }
            need(r.teleportFeet>=0&&r.actorZombiesControlled>=0&&r.actorZombiesControlled<=12,
                "invalid_optional_state");
            boolean active=a.savesInsteadOfAttack()?(a==Action.STEAM_MEPHIT||!r.saveSucceeded):r.hit;
            boolean applyDamage=active;
            boolean lycanEligible=applyDamage&&a.isLycanthrope()&&t.kind==Kind.HUMANOID;
            boolean immune=lycanEligible&&hasImmunity(r.target,r.actor,a);
            if(a.isLycanthrope()){
                if(lycanEligible&&!immune)need(r.saveSucceeded!=null,"lycanthrope_con_save_required");
                else need(r.saveSucceeded==null,"lycanthrope_save_not_applicable");
            }else if(!a.savesInsteadOfAttack())need(r.saveSucceeded==null,"unexpected_save");
            if(a==Action.STEAM_MEPHIT && r.underwaterResistanceOnly){
                // Caller proves the resistance arises solely from being underwater.
                // Source explicitly forbids that resistance for this attack.
                need(t.defense(Type.FIRE)==Defense.RESISTANT,"invalid_water_only_resistance_context");
            }else need(!r.underwaterResistanceOnly,"underwater_override_not_applicable");
            List<Effect> effects=new ArrayList<>();
            int total=0,necro=0,max=t.maxHp,hp=t.hp;
            if(applyDamage){
                int first;
                if(a==Action.STEAM_MEPHIT && r.saveSucceeded){
                    // A successful save halves raw damage BEFORE resistance or
                    // vulnerability (PHB 2024 order for damage modifiers).
                    first=mitigateRaw(t,a.first.type,a.first.roll(r.firstDice)/2,
                        r.underwaterResistanceOnly);
                }else first=mitigate(t,a.first,r.firstDice,r.underwaterResistanceOnly);
                int second=a.second==null?0:mitigate(t,a.second,r.secondDice,false);
                if(a.second==null)need(r.secondDice.length==0,"unexpected_secondary_dice");
                total=first+second;
                necro=(a.first.type==Type.NECROTIC?first:0)
                    +(a.second!=null&&a.second.type==Type.NECROTIC?second:0);
                hp=Math.max(0,hp-total);
                switch(a){
                    case STEAM_MEPHIT:
                        if(!r.saveSucceeded)effects.add(effect(Rider.SPEED_MINUS_10_UNTIL_NEXT_TURN,r,10,"end_of_mephit_next_turn"));
                        effects.add(new Effect(Rider.RECHARGE_SIX,r.actor,r.actor,6,"next_action_requires_recharge"));
                        need(r.choice==Choice.NONE,"unexpected_choice");
                        break;
                    case ULTROLOTH:
                        need(r.choice==Choice.NONE||r.choice==Choice.TELEPORT,"unexpected_choice");
                        if(r.choice==Choice.TELEPORT){
                            need(r.teleportFeet<=10&&r.teleportSpaceValid&&r.targetVisible,
                                "unverified_teleport_destination");
                            effects.add(effect(Rider.TELEPORT_TARGET_UP_TO_10FT,r,r.teleportFeet,"immediate_after_hit"));
                        } else need(r.teleportFeet==0,"teleport_without_choice");
                        break;
                    case VAMPIRE_NIGHTBRINGER:
                    case VAMPIRE_UMBRAL_LORD:
                        max=Math.max(0,max-necro);
                        hp=Math.min(hp,max);
                        effects.add(effect(Rider.MAX_HP_DRAIN_NECROTIC,r,necro,"until_restoration"));
                        effects.add(new Effect(Rider.ACTOR_HEALS_NECROTIC_DAMAGE,r.actor,r.actor,necro,"immediate_heal_capped_by_actor_max_hp"));
                        need(r.choice==Choice.NONE,"unexpected_choice");
                        break;
                    case WARRIOR_COMMANDER:
                        need(r.choice==Choice.SAP||r.choice==Choice.MANEUVER,"commander_must_choose_effect");
                        if(r.choice==Choice.SAP)effects.add(effect(
                            Rider.WARRIOR_SAP_NEXT_ATTACK_DISADVANTAGE,r,0,"start_of_warrior_next_turn"));
                        else{
                            need(r.ally!=null&&!r.ally.isEmpty()&&r.allyCanReact&&r.allyCanSeeOrHear,
                                "ally_reaction_and_perception_required");
                            effects.add(new Effect(Rider.WARRIOR_MANEUVER_ALLY_REACTION_HALF_SPEED,
                                r.actor,r.ally,0,"immediate_reaction_without_opportunity_attacks"));
                        }
                        break;
                    case WEREBEAR:case WEREBOAR:case WERERAT:case WERETIGER:case WEREWOLF:
                        need(r.choice==Choice.NONE,"unexpected_choice");
                        if(lycanEligible&&!immune){
                            if(r.saveSucceeded)effects.add(effect(
                                Rider.IMMUNE_TO_THIS_LYCANTHROPE_CURSE_24H,r,0,"24_hours_"+a.name()));
                            else{
                                effects.add(effect(Rider.LYCANTHROPY_CURSE,r,0,"until_curse_removed_"+a.name()));
                                if(hp==0)effects.add(effect(
                                    Rider.TRANSFORM_TO_LYCANTHROPE_AT_ZERO_HP_DM_CONTROL,r,10,
                                    "immediate_replace_death_with_transformation_"+a.name()));
                            }
                        }
                        break;
                    case WIGHT:
                        max=Math.max(0,max-total);hp=Math.min(hp,max);
                        effects.add(effect(Rider.MAX_HP_DRAIN_TOTAL,r,total,"until_restoration"));
                        if(t.kind==Kind.HUMANOID&&hp==0&&r.actorZombiesControlled<12)
                            effects.add(effect(Rider.ZOMBIE_RISES_AFTER_24H_CAP_12,r,24,
                                "after_24h_unless_restored_or_body_destroyed"));
                        need(r.choice==Choice.NONE,"unexpected_choice");
                        break;
                    case WORG:
                        effects.add(effect(Rider.NEXT_ATTACK_AGAINST_TARGET_ADVANTAGE,r,0,
                            "start_of_worg_next_turn"));
                        need(r.choice==Choice.NONE,"unexpected_choice");
                        break;
                    default:throw new IllegalArgumentException("unsupported_action");
                }
            } else {
                need(r.firstDice.length==0&&r.secondDice.length==0&&r.choice==Choice.NONE,
                    "unused_attack_or_save_rolls");
            }
            return new Proposal(this,revision,r.event,r.target,new Result(a,total,necro,hp,max,effects));
        }
        public synchronized Result execute(Proposal p){
            need(p!=null&&p.owner==this&&p.revision==revision
                    &&!consumed.contains(p.event),"stale_replayed_or_foreign_proposal");
            Target t=targets.get(p.target);need(t!=null,"target_disappeared");
            targets.put(p.target,t.withStats(p.result.hp,p.result.maxHp));
            for(Effect e:p.result.pending){
                List<Effect> list=pending.get(e.target);
                if(list==null){list=new ArrayList<>();pending.put(e.target,list);}
                list.add(e);
            }
            consumed.add(p.event);revision++;return p.result;
        }
    }
    private static Effect effect(Rider k,Request r,int amount,String expiry){
        return new Effect(k,r.actor,r.target,amount,expiry);
    }
    private static int mitigate(Target t,Dice component,int[] rolled,boolean ignoreWaterOnly){
        return mitigateRaw(t,component.type,component.roll(rolled),ignoreWaterOnly);
    }
    private static int mitigateRaw(Target t,Type type,int raw,boolean ignoreWaterOnly){
        Defense def=t.defense(type);
        need(def!=null,"typed_damage_defense_required_"+type);
        if(ignoreWaterOnly)def=Defense.NORMAL;
        return DamageMitigationRules.apply(raw,def==Defense.IMMUNE,
            def==Defense.RESISTANT||def==Defense.BOTH,
            def==Defense.VULNERABLE||def==Defense.BOTH);
    }
    private static void need(boolean ok,String reason){if(!ok)throw new IllegalArgumentException(reason);}
}
