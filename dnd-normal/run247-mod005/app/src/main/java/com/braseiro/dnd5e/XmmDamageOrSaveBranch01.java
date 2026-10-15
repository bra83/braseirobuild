package com.braseiro.dnd5e;

import java.util.*;

/** Independent deterministic executor. No dice, Android, database or combat mutation.
 * A Session is an in-memory encounter ledger, not a persistence substitute.
 * Callers must supply the complete authoritative target set, geometry and defenses.
 */
public final class XmmDamageOrSaveBranch01 {
    private XmmDamageOrSaveBranch01() {}
    public static final String SOURCE_COMMIT = "8c026b807fac21862a309379a0c3228a0683198b";
    public static final String SOURCE_BLOB = "9a4137b508ef37562ed948894a70b1594b57e85c";
    public enum Type { POISON, FIRE, COLD, RADIANT, LIGHTNING, FORCE }
    public enum Economy { ACTION, BONUS, LEGENDARY }
    public enum Kind { INSECTS, SANDS, GALE, SPIRIT, WAVE, WITCH, BLADE, AIM, FLAME }
    public enum Form { FORAGER, HUNTER, SAGE }
    public enum Defense { NORMAL, RESISTANT, VULNERABLE, RESISTANT_AND_VULNERABLE, IMMUNE }
    public enum End { TARGET_TURN_END, ACTOR_NEXT_TURN_START, ACTOR_NEXT_TURN_END, CURRENT_TURN_END }
    public enum EffectKind { CONCENTRATION_DISADVANTAGE, HALF_SPEED, PUSH_AWAY,
        TEMPORARY_HP, PREY_ADVANTAGE, ATTACK_AND_CHECK_DISADVANTAGE,
        CURSED_NO_REACTIONS, NO_REACTIONS, BURNING, NEXT_ATTACK_ADVANTAGE }

    public static final class Component {
        public final Type type;
        public final String formula;
        public final int dice, sides, bonus;
        Component(Type type, int dice, int sides, int bonus) {
            this.type=type; this.dice=dice; this.sides=sides; this.bonus=bonus;
            formula=dice+"d"+sides+(bonus==0?"":" + "+bonus);
        }
        void validate(int value) { require(value>=dice+bonus && value<=dice*sides+bonus,"damage_outside_formula"); }
    }
    public static final class Contract {
        public final String monster, action, pointer;
        public final Kind kind;
        public final Economy economy;
        public final int dc, range, width, toHit;
        public final String saveAbility;
        public final boolean halfOnSuccess;
        public final List<Component> damage;
        Contract(String monster, String action, String pointer, Kind kind, Economy economy,
                 int dc, int range, int width, int toHit, boolean half, Component... damage) {
            this.monster=monster; this.action=action; this.pointer=pointer; this.kind=kind;
            this.economy=economy; this.dc=dc; this.range=range; this.width=width; this.toHit=toHit;
            this.saveAbility=dc==0?null:"dex"; halfOnSuccess=half;
            this.damage=Collections.unmodifiableList(Arrays.asList(damage));
        }
    }
    private static Component c(Type t,int n,int s,int b) { return new Component(t,n,s,b); }
    private static final List<Contract> CONTRACTS=Collections.unmodifiableList(Arrays.asList(
        new Contract("Adult Black Dragon","Cloud of Insects","/monster/5/legendary/0/entries/0",Kind.INSECTS,Economy.LEGENDARY,17,120,0,0,false,c(Type.POISON,4,10,0)),
        new Contract("Adult Brass Dragon","Scorching Sands","/monster/7/legendary/2/entries/0",Kind.SANDS,Economy.LEGENDARY,16,120,0,0,false,c(Type.FIRE,6,8,0)),
        new Contract("Adult Silver Dragon","Cold Gale","/monster/13/legendary/1/entries/0",Kind.GALE,Economy.LEGENDARY,19,60,10,0,true,c(Type.COLD,4,6,0)),
        new Contract("Ancient Black Dragon","Cloud of Insects","/monster/17/legendary/0/entries/0",Kind.INSECTS,Economy.LEGENDARY,21,120,0,0,false,c(Type.POISON,6,10,0)),
        new Contract("Ancient Brass Dragon","Scorching Sands","/monster/19/legendary/2/entries/0",Kind.SANDS,Economy.LEGENDARY,20,120,0,0,false,c(Type.FIRE,8,8,0)),
        new Contract("Ancient Silver Dragon","Cold Gale","/monster/25/legendary/1/entries/0",Kind.GALE,Economy.LEGENDARY,23,60,10,0,true,c(Type.COLD,4,6,0)),
        new Contract("Animal Lord","Animal Spirit","/monster/27/action/3/entries/0",Kind.SPIRIT,Economy.ACTION,20,120,0,0,true,c(Type.RADIANT,4,10,6)),
        new Contract("Arch-hag","Crackling Wave","/monster/36/action/2/entries/0",Kind.WAVE,Economy.ACTION,22,60,0,0,true,c(Type.LIGHTNING,5,12,0)),
        new Contract("Arch-hag","Witch Strike","/monster/36/bonus/0/entries/0",Kind.WITCH,Economy.BONUS,0,60,0,0,false,c(Type.LIGHTNING,4,6,0)),
        new Contract("Balor","Lightning Blade","/monster/48/action/2/entries/0",Kind.BLADE,Economy.ACTION,0,10,0,14,false,c(Type.FORCE,3,8,8),c(Type.LIGHTNING,4,10,0)),
        new Contract("Bandit Crime Lord","Deadly Aim","/monster/51/bonus/0/entries/0",Kind.AIM,Economy.BONUS,0,0,0,0,false,c(Type.POISON,8,6,0)),
        new Contract("Barbed Devil","Hurl Flame","/monster/54/action/3/entries/0",Kind.FLAME,Economy.ACTION,0,150,0,5,false,c(Type.FIRE,5,6,0))
    ));
    public static List<Contract> contracts() { return CONTRACTS; }
    public static Contract contract(String monster,String action,String pointer) {
        for(Contract c:CONTRACTS) if(c.monster.equals(monster)&&c.action.equals(action)&&c.pointer.equals(pointer)) return c;
        throw new IllegalArgumentException("exact_source_identity_required");
    }
    public static final class Effect {
        public final EffectKind kind;
        public final String actor, target;
        public final End end; // null means hazard lifecycle belongs to external rules
        public final int amount;
        Effect(EffectKind kind,String actor,String target,End end,int amount) {
            this.kind=kind; this.actor=actor; this.target=target; this.end=end; this.amount=amount;
        }
    }
    /** All booleans are required explicit facts (boxed to reject missing values). */
    public static final class Target {
        public final String id;
        public final int distance, pushFeet;
        public final Boolean visible, inArea, cursedByActor, creature, flammable, wornOrCarried;
        public final Integer saveTotal;
        public final Boolean hit;
        public final List<Integer> rolledDamage;
        public final Map<Type,Defense> defenses;
        public Target(String id,int distance,int pushFeet,Boolean visible,Boolean inArea,
                      Boolean cursedByActor,Boolean creature,Boolean flammable,Boolean wornOrCarried,
                      Integer saveTotal,Boolean hit,List<Integer> rolledDamage,Map<Type,Defense> defenses) {
            this.id=id; this.distance=distance; this.pushFeet=pushFeet; this.visible=visible;
            this.inArea=inArea; this.cursedByActor=cursedByActor; this.creature=creature;
            this.flammable=flammable; this.wornOrCarried=wornOrCarried;
            this.saveTotal=saveTotal; this.hit=hit;
            this.rolledDamage=Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(rolledDamage)));
            this.defenses=Collections.unmodifiableMap(new EnumMap<Type,Defense>(copyDefenses(defenses)));
        }
    }
    private static EnumMap<Type,Defense> copyDefenses(Map<Type,Defense> input) {
        EnumMap<Type,Defense> result=new EnumMap<>(Type.class);
        result.putAll(Objects.requireNonNull(input)); return result;
    }
    public static final class Request {
        public final Contract contract;
        public final String actor, event;
        public final long revision;
        public final boolean completeTargetSet, geometryConfirmed;
        public final Form form;
        public final List<Target> targets;
        public Request(Contract contract,String actor,String event,long revision,boolean completeTargetSet,
                       boolean geometryConfirmed,Form form,List<Target> targets) {
            this.contract=Objects.requireNonNull(contract); this.actor=actor; this.event=event;
            this.revision=revision; this.completeTargetSet=completeTargetSet;
            this.geometryConfirmed=geometryConfirmed; this.form=form;
            this.targets=Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(targets)));
        }
    }
    public static final class TargetResult {
        public final String target;
        public final Map<Type,Integer> damage;
        public final List<Effect> effects;
        TargetResult(String target,Map<Type,Integer> damage,List<Effect> effects) {
            this.target=target; this.damage=Collections.unmodifiableMap(new EnumMap<>(damage));
            this.effects=Collections.unmodifiableList(new ArrayList<>(effects));
        }
    }
    /** Proposal contains all results; commit revalidates the ledger revision and consumes it once. */
    public static final class Proposal {
        private final Session owner;
        private final Request request;
        public final List<TargetResult> results;
        private Proposal(Session owner,Request request,List<TargetResult> results) {
            this.owner=owner; this.request=request;
            this.results=Collections.unmodifiableList(results);
        }
        public boolean combatApplied() { return false; }
    }
    private static int defend(int damage,Defense defense) {
        require(defense!=null,"explicit_defense_required");
        if(defense==Defense.IMMUNE) return 0;
        // Apply resistance then vulnerability, including rounding, per separate modifiers.
        if(defense==Defense.RESISTANT || defense==Defense.RESISTANT_AND_VULNERABLE) damage/=2;
        if(defense==Defense.VULNERABLE || defense==Defense.RESISTANT_AND_VULNERABLE) damage*=2;
        return damage;
    }
    private static void require(boolean value,String reason) {
        if(!value) throw new IllegalArgumentException(reason);
    }
    private static void textRequired(String value) { require(value!=null&&!value.trim().isEmpty(),"identity_required"); }

    public static final class Session {
        private long revision;
        private final Set<String> events=new HashSet<>(), locked=new HashSet<>();
        private final Map<String,Integer> actions=new HashMap<>(), bonuses=new HashMap<>(), legendary=new HashMap<>();
        private final Map<String,String> aim=new HashMap<>();
        private final Map<String,Long> turns=new HashMap<>();
        private final List<Effect> effects=new ArrayList<>();
        private String currentActor;
        private boolean legendaryWindow;
        private final Set<String> legendaryWindowUsed=new HashSet<>();
        public synchronized long revision() { return revision; }
        public synchronized List<Effect> effects() { return Collections.unmodifiableList(new ArrayList<>(effects)); }
        /** Explicit encounter turn boundary, never inferred from an action or UI click. */
        public synchronized void startTurn(String actor,int actionUses,int bonusUses,int legendaryUses) {
            textRequired(actor); require(currentActor==null,"end_current_turn_first");
            require(actionUses>=0&&bonusUses>=0&&legendaryUses>=0,"invalid_budget");
            currentActor=actor; legendaryWindow=false; legendaryWindowUsed.clear(); turns.put(actor,turns.getOrDefault(actor,0L)+1);
            actions.put(actor,actionUses); bonuses.put(actor,bonusUses); legendary.put(actor,legendaryUses);
            locked.removeIf(k->k.startsWith(actor+"\u0000"));
            effects.removeIf(e->e.actor.equals(actor)&&e.end==End.ACTOR_NEXT_TURN_START);
            revision++;
        }
        /** Open only after the current creature has finished its turn activities. */
        public synchronized void openLegendaryWindow(String actor) {
            require(Objects.equals(currentActor,actor),"wrong_turn");
            legendaryWindow=true; revision++;
        }
        public synchronized void endTurn(String actor) {
            require(Objects.equals(currentActor,actor),"wrong_turn");
            long turn=turns.getOrDefault(actor,0L);
            effects.removeIf(e->(e.target.equals(actor)&&e.end==End.TARGET_TURN_END&&turn>=effectTurn.getOrDefault(e,Long.MAX_VALUE))
                ||(e.actor.equals(actor)&&e.end==End.CURRENT_TURN_END)
                ||(e.actor.equals(actor)&&e.end==End.ACTOR_NEXT_TURN_END
                   && turn>=effectTurn.getOrDefault(e,Long.MAX_VALUE)));
            aim.remove(actor); currentActor=null; legendaryWindow=false; revision++;
        }
        private final Map<Effect,Long> effectTurn=new IdentityHashMap<>();
        private String key(Request r) { return r.actor+"\u0000"+r.contract.pointer; }
        private void validateLedger(Request r) {
            textRequired(r.actor); textRequired(r.event);
            require(r.revision==revision,"stale_context"); require(!events.contains(r.event),"replay");
            require(CONTRACTS.contains(r.contract),"unknown_contract");
            require(r.completeTargetSet&&r.geometryConfirmed,"complete_context_required");
            if(r.contract.economy==Economy.LEGENDARY) {
                require(legendaryWindow&&currentActor!=null&&!currentActor.equals(r.actor)&&!legendaryWindowUsed.contains(r.actor),"legendary_other_turn_end_required");
                require(legendary.getOrDefault(r.actor,0)>0&&!locked.contains(key(r)),"legendary_unavailable");
            } else {
                require(!legendaryWindow&&r.actor.equals(currentActor),"actor_turn_required");
                require((r.contract.economy==Economy.BONUS?bonuses:actions).getOrDefault(r.actor,0)>0,"economy_unavailable");
            }
        }
        public synchronized Proposal propose(Request r) {
            Objects.requireNonNull(r); validateLedger(r);
            Contract c=r.contract;
            boolean multi=c.kind==Kind.GALE||c.kind==Kind.WAVE||c.kind==Kind.WITCH;
            require(c.kind==Kind.AIM?r.targets.isEmpty():multi||r.targets.size()==1,"target_cardinality");
            if(c.kind==Kind.SPIRIT) require(r.form!=null,"animal_form_required");
            List<TargetResult> results=new ArrayList<>(); Set<String> ids=new HashSet<>();
            for(Target t:r.targets) {
                textRequired(t.id); require(ids.add(t.id),"duplicate_target");
                require(t.distance>=0&&t.distance<=c.range,"out_of_range");
                require(t.creature!=null,"target_kind_required");
                if(c.dc>0||c.kind==Kind.WITCH) require(t.creature,"creature_required");
                if(c.kind==Kind.INSECTS||c.kind==Kind.SANDS||c.kind==Kind.SPIRIT)
                    require(Boolean.TRUE.equals(t.visible),"visible_target_required");
                if(c.kind==Kind.GALE||c.kind==Kind.WAVE) require(Boolean.TRUE.equals(t.inArea),"area_membership_required");
                if(c.kind==Kind.WITCH) require(Boolean.TRUE.equals(t.cursedByActor),"actor_curse_required");
                if(c.dc>0) require(t.saveTotal!=null,"save_total_required");
                if(c.toHit>0) require(t.hit!=null,"manual_attack_outcome_required");
                if(c.kind==Kind.FLAME) require(t.flammable!=null&&t.wornOrCarried!=null,"object_context_required");
                require(t.rolledDamage.size()==c.damage.size(),"all_damage_components_required");
                boolean success=c.dc>0&&t.saveTotal>=c.dc;
                boolean hit=c.toHit==0||t.hit;
                Map<Type,Integer> damage=new EnumMap<>(Type.class);
                for(int i=0;i<c.damage.size();i++) {
                    Component component=c.damage.get(i); Integer roll=t.rolledDamage.get(i);
                    require(roll!=null,"manual_damage_required"); component.validate(roll);
                    int amount=!hit?0:success?(c.halfOnSuccess?roll/2:0):roll;
                    damage.put(component.type,defend(amount,t.defenses.get(component.type)));
                }
                List<Effect> out=new ArrayList<>();
                if(!success&&c.kind==Kind.INSECTS) out.add(new Effect(EffectKind.CONCENTRATION_DISADVANTAGE,r.actor,t.id,End.TARGET_TURN_END,0));
                if(!success&&c.kind==Kind.SANDS) out.add(new Effect(EffectKind.HALF_SPEED,r.actor,t.id,End.TARGET_TURN_END,0));
                if(!success&&c.kind==Kind.GALE) {
                    require(t.pushFeet>=0&&t.pushFeet<=30,"push_choice_required");
                    out.add(new Effect(EffectKind.PUSH_AWAY,r.actor,t.id,null,t.pushFeet));
                }
                if(c.kind==Kind.SPIRIT) {
                    if(r.form==Form.FORAGER) out.add(new Effect(EffectKind.TEMPORARY_HP,r.actor,r.actor,null,20));
                    if(r.form==Form.HUNTER) out.add(new Effect(EffectKind.PREY_ADVANTAGE,r.actor,t.id,End.ACTOR_NEXT_TURN_START,0));
                    if(r.form==Form.SAGE) out.add(new Effect(EffectKind.ATTACK_AND_CHECK_DISADVANTAGE,r.actor,t.id,End.TARGET_TURN_END,0));
                }
                if(c.kind==Kind.WAVE) out.add(new Effect(EffectKind.CURSED_NO_REACTIONS,r.actor,t.id,End.ACTOR_NEXT_TURN_END,0));
                if(c.kind==Kind.BLADE&&hit) out.add(new Effect(EffectKind.NO_REACTIONS,r.actor,t.id,End.ACTOR_NEXT_TURN_START,0));
                if(c.kind==Kind.FLAME&&hit&&!t.creature&&t.flammable&&!t.wornOrCarried)
                    out.add(new Effect(EffectKind.BURNING,r.actor,t.id,null,0));
                results.add(new TargetResult(t.id,damage,out));
            }
            return new Proposal(this,r,results);
        }
        public synchronized void commit(Proposal p) {
            require(p!=null&&p.owner==this,"foreign_proposal"); Request r=p.request; validateLedger(r);
            Map<String,Integer> budget=r.contract.economy==Economy.LEGENDARY?legendary:r.contract.economy==Economy.BONUS?bonuses:actions;
            budget.put(r.actor,budget.get(r.actor)-1); events.add(r.event);
            if(r.contract.economy==Economy.LEGENDARY) { locked.add(key(r)); legendaryWindowUsed.add(r.actor); }
            if(r.contract.kind==Kind.AIM) {
                aim.put(r.actor,r.event);
                effects.add(new Effect(EffectKind.NEXT_ATTACK_ADVANTAGE,r.actor,r.actor,End.CURRENT_TURN_END,0));
            }
            for(TargetResult result:p.results) for(Effect e:result.effects) {
                // Instant effects are returned for the adapter; they are not duration state.
                if(e.end!=null) { effects.add(e); effectTurn.put(e,turns.getOrDefault(e.end==End.TARGET_TURN_END?e.target:e.actor,0L)+1); }
            }
            revision++;
        }
        /** Must be called for the very next attack, hit OR miss, by the adapter.
         * Returns extra poison damage; base attack is resolved by its own executor.
         */
        public synchronized int nextAttack(String actor,String event,long expectedRevision,
                                           boolean hit,Integer manualPoison,Defense defense) {
            textRequired(event); require(expectedRevision==revision&&!events.contains(event),"stale_or_replay");
            require(!legendaryWindow&&actor.equals(currentActor)&&aim.containsKey(actor),"no_active_aim");
            int damage=0;
            if(hit) { require(manualPoison!=null,"manual_damage_required");
                c(Type.POISON,8,6,0).validate(manualPoison); damage=defend(manualPoison,defense); }
            aim.remove(actor); events.add(event);
            effects.removeIf(e->e.actor.equals(actor)&&e.kind==EffectKind.NEXT_ATTACK_ADVANTAGE);
            revision++; return damage;
        }
    }
}
