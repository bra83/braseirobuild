package com.braseiro.dnd5e;

import java.util.*;

/**
 * Source-pinned XMM swallow_and_escape-01 (Giant Frog / Giant Toad).
 * Pure Java8 deterministic in-memory model. NEVER rolls dice, never runs
 * Android combat, never persists SQLite, and never advances world time.
 *
 * Bite/grapple is established by an authoritative EXTERNAL combat adapter.
 * A caller-supplied "confirmed" flag is a contract requirement, not proof
 * of actual combat integration. All turns and end-turn rolls are explicit.
 */
public final class XmmSwallowAndEscape01 {
    private XmmSwallowAndEscape01(){}
    public static final String SOURCE_COMMIT="8c026b807fac21862a309379a0c3228a0683198b";
    public enum Species {
        GIANT_FROG("/monster/182/action/1/entries/0",1,4,2,4,11,3),
        GIANT_TOAD("/monster/194/action/1/entries/0",2,6,3,6,12,4);
        public final String pointer;
        /** 1=Small, 2=Medium; supported bite-grapple max is Medium for both. */
        public final int swallowMaxSize,acidDieCount,acidDieSides,biteEscapeDc,biteToHit;
        Species(String ptr,int maxSize,int ignored,int dieCount,int dieSides,int dc,int attack) {
            pointer=ptr;swallowMaxSize=maxSize;
            acidDieCount=dieCount;acidDieSides=dieSides;biteEscapeDc=dc;
            biteToHit=attack;
        }
        public static Species fromSource(String p){
            for(Species s:values())if(s.pointer.equals(p))return s;
            throw new IllegalArgumentException("unverified_swallow_source_pointer");
        }
    }
    public enum Size { TINY, SMALL, MEDIUM, LARGE, HUGE, GARGANTUAN }
    public enum Kind { CREATURE, OBJECT }
    public enum Defense { NORMAL, RESISTANT, VULNERABLE, BOTH, IMMUNE }
    public enum Condition {
        GRAPPLED, BLINDED, RESTRAINED, TOTAL_COVER_AGAINST_OUTSIDE,
        MUST_USE_5FT_MOVEMENT_TO_EXIT_CORPSE, PRONE
    }
    public enum Phase { GRAPPLED, SWALLOWED, CORPSE_EXIT_READY, DISGORGED_PRONE, EXITED_PRONE }
    public static final class Target {
        public final String id;
        public final Kind kind;
        public final Size size;
        public final int hp,maxHp;
        public final Defense acidDefense;
        public Target(String id,Kind kind,Size size,int hp,int maxHp,Defense defense){
            check(id!=null&&!id.trim().isEmpty()&&kind!=null&&size!=null&&defense!=null,
                "missing_target_metadata");
            check(maxHp>=0&&hp>=0&&hp<=maxHp,"invalid_hit_points");
            this.id=id;this.kind=kind;this.size=size;this.hp=hp;
            this.maxHp=maxHp;acidDefense=defense;
        }
        private Target withHp(int hp){
            return new Target(id,kind,size,hp,maxHp,acidDefense);
        }
    }
    public static final class Status {
        public final Species species;
        public final String actor,target;
        public final Phase phase;
        public final int swallowedAtTurn,lastAcidTurn;
        public final Set<Condition> conditions;
        private Status(Species species,String actor,String target,Phase phase,
                int swallowedAtTurn,int lastAcidTurn){
            this.species=species;this.actor=actor;this.target=target;
            this.phase=phase;this.swallowedAtTurn=swallowedAtTurn;
            this.lastAcidTurn=lastAcidTurn;
            EnumSet<Condition> c=EnumSet.noneOf(Condition.class);
            switch(phase) {
                case GRAPPLED:c.add(Condition.GRAPPLED);break;
                case SWALLOWED:
                    c.add(Condition.BLINDED);
                    c.add(Condition.RESTRAINED);
                    c.add(Condition.TOTAL_COVER_AGAINST_OUTSIDE);
                    break;
                case CORPSE_EXIT_READY:
                    c.add(Condition.BLINDED);
                    c.add(Condition.TOTAL_COVER_AGAINST_OUTSIDE);
                    c.add(Condition.MUST_USE_5FT_MOVEMENT_TO_EXIT_CORPSE);
                    break;
                case DISGORGED_PRONE:case EXITED_PRONE:
                    c.add(Condition.PRONE);break;
                default:throw new IllegalArgumentException("unexpected_phase");
            }
            conditions=Collections.unmodifiableSet(c);
        }
        private Status next(Phase phase,int turn){
            return new Status(species,actor,target,phase,swallowedAtTurn,turn);
        }
    }
    public static final class SwallowRequest {
        public final Species species;
        public final String actor,target,event;
        public final long revision;
        /** Host-supplied actor turn index; NOT guessed from wall time. */
        public final int actorTurn;
        public SwallowRequest(Species species,String actor,String target,String event,
                long revision,int actorTurn){
            this.species=Objects.requireNonNull(species);
            this.actor=actor;this.target=target;this.event=event;
            this.revision=revision;this.actorTurn=actorTurn;
        }
    }
    public static final class Proposal {
        private final Session owner;
        private final long revision;
        private final String event;
        public final Status status;
        private Proposal(Session owner,long revision,String event,Status status){
            this.owner=owner;this.revision=revision;this.event=event;this.status=status;
        }
    }
    public static final class Tick {
        public final int rolledAcid,acidDamage,targetHp,turn;
        public final boolean disgorgedProne,killedByAcid;
        public final Status status;
        /** Tick is in-memory only; Android/SQLite adapter remains pending. */
        public final boolean combatPersisted=false;
        private Tick(int rolled,int damage,int hp,int turn,boolean disgorged,
                boolean killed,Status status){
            rolledAcid=rolled;acidDamage=damage;targetHp=hp;this.turn=turn;
            disgorgedProne=disgorged;killedByAcid=killed;this.status=status;
        }
    }
    public static final class Session {
        private final Map<String,Target> targets=new LinkedHashMap<>();
        private final Map<String,Status> statuses=new LinkedHashMap<>();
        private final Set<String> consumedEvents=new HashSet<>();
        private final Set<String> deadActors=new HashSet<>();
        private long revision;
        public Session(Collection<Target> initial){
            check(initial!=null&&!initial.isEmpty(),"targets_required");
            for(Target t:initial){
                check(t!=null&&!targets.containsKey(t.id),"duplicate_or_missing_target");
                targets.put(t.id,t);
            }
        }
        private void event(String token){
            check(token!=null&&!token.trim().isEmpty()
                &&!consumedEvents.contains(token),"event_missing_or_reused");
        }
        private void actor(String id){
            check(id!=null&&!id.trim().isEmpty()&&!deadActors.contains(id),
                "actor_missing_or_dead");
        }
        public synchronized long revision(){return revision;}
        public synchronized Target target(String id){return targets.get(id);}
        public synchronized Status status(String victim){return statuses.get(victim);}
        public synchronized boolean canBite(String actor){
            if(actor==null||actor.trim().isEmpty()||deadActors.contains(actor))return false;
            for(Status s:statuses.values())
                if(s.actor.equals(actor)&&(s.phase==Phase.SWALLOWED
                    ||s.phase==Phase.CORPSE_EXIT_READY))return false;
            return true;
        }
        /**
         * Record only an ALREADY-RESOLVED Bite grapple. The authoritative host
         * must verify the attack and actor/target identities before calling.
         */
        public synchronized Status registerBiteGrapple(Species species,String actor,
                String victim,boolean externallyConfirmedHitAndGrapple,String event){
            check(species!=null&&externallyConfirmedHitAndGrapple,
                "external_hit_and_grapple_confirmation_required");
            event(event);actor(actor);
            Target t=targets.get(victim);
            check(t!=null&&t.kind==Kind.CREATURE
                &&t.size.ordinal()<=Size.MEDIUM.ordinal(),"bite_target_requires_medium_or_smaller_creature");
            check(canBite(actor),"swallower_cannot_bite_while_swallowing");
            Status old=statuses.get(victim);
            check(old==null||old.phase==Phase.DISGORGED_PRONE||old.phase==Phase.EXITED_PRONE,
                "victim_already_grappled_or_swallowed");
            Status next=new Status(species,actor,victim,Phase.GRAPPLED,-1,-1);
            statuses.put(victim,next);
            consumedEvents.add(event);revision++;
            return next;
        }
        public synchronized Proposal propose(SwallowRequest r){
            check(r!=null,"swallow_request_missing");
            event(r.event);actor(r.actor);
            check(r.revision==revision&&r.actorTurn>=0,"stale_revision_or_invalid_turn");
            Target t=targets.get(r.target);
            Status pre=statuses.get(r.target);
            check(t!=null&&t.kind==Kind.CREATURE&&pre!=null
                &&pre.phase==Phase.GRAPPLED&&pre.actor.equals(r.actor)
                &&pre.species==r.species,"swallow_requires_own_active_grapple");
            check(t.size.ordinal()<=r.species.swallowMaxSize,
                "target_too_large_for_swallow");
            for(Status state:statuses.values())
                if(state.actor.equals(r.actor)&&(state.phase==Phase.SWALLOWED
                    ||state.phase==Phase.CORPSE_EXIT_READY))
                    throw new IllegalArgumentException("actor_already_has_swallowed_target");
            Status after=new Status(r.species,r.actor,r.target,Phase.SWALLOWED,r.actorTurn,-1);
            return new Proposal(this,revision,r.event,after);
        }
        public synchronized Status commit(Proposal p){
            check(p!=null&&p.owner==this&&p.revision==revision,
                "stale_or_foreign_swallow_proposal");
            event(p.event);actor(p.status.actor);
            Status prior=statuses.get(p.status.target);
            check(prior!=null&&prior.phase==Phase.GRAPPLED
                &&prior.actor.equals(p.status.actor),"swallow_grapple_no_longer_present");
            statuses.put(p.status.target,p.status);
            consumedEvents.add(p.event);revision++;
            return p.status;
        }
        private static int roll(int[] dice,int count,int sides){
            check(dice!=null&&dice.length==count,"acid_dice_count_mismatch");
            int total=0;
            for(int n:dice){check(n>=1&&n<=sides,"invalid_acid_die");total+=n;}
            return total;
        }
        /**
         * The FROG deals acid ONLY at the end of its NEXT turn, and disgorges
         * a survivor immediately. If that damage kills, it does NOT disgorge.
         * The TOAD deals acid at the end of EACH of its turns (including the
         * swallowing turn), and does not auto-disgorge.
         */
        public synchronized Tick endActorTurn(String actor,String victim,int turn,
                int[] manualDice,String event){
            event(event);actor(actor);
            Status s=statuses.get(victim);
            check(s!=null&&s.phase==Phase.SWALLOWED&&s.actor.equals(actor),
                "victim_not_swallowed_by_actor");
            if(s.species==Species.GIANT_FROG)
                check(turn==s.swallowedAtTurn+1&&s.lastAcidTurn==-1,
                    "frog_damage_only_end_of_next_turn");
            else check(turn>=s.swallowedAtTurn&&turn>s.lastAcidTurn,
                "toad_requires_new_end_of_turn_event");
            int raw=roll(manualDice,s.species.acidDieCount,s.species.acidDieSides);
            Target t=targets.get(victim);check(t!=null,"swallowed_target_missing");
            Defense d=t.acidDefense;
            int damage=DamageMitigationRules.apply(raw,
                d==Defense.IMMUNE,d==Defense.RESISTANT||d==Defense.BOTH,
                d==Defense.VULNERABLE||d==Defense.BOTH);
            int hp=Math.max(0,t.hp-damage);
            boolean killed=t.hp>0&&hp==0;
            boolean frogDisgorges=s.species==Species.GIANT_FROG&&hp>0;
            Status next=s.next(frogDisgorges?Phase.DISGORGED_PRONE:Phase.SWALLOWED,turn);
            targets.put(victim,t.withHp(hp));
            statuses.put(victim,next);
            consumedEvents.add(event);revision++;
            return new Tick(raw,damage,hp,turn,frogDisgorges,killed,next);
        }
        /** External combat system reports death; this model does not infer it. */
        public synchronized void actorDies(String actor,String event){
            event(event);actor(actor);
            deadActors.add(actor);
            for(Map.Entry<String,Status> e:statuses.entrySet()){
                Status s=e.getValue();
                if(!s.actor.equals(actor))continue;
                if(s.phase==Phase.SWALLOWED)
                    e.setValue(s.next(Phase.CORPSE_EXIT_READY,s.lastAcidTurn));
                else if(s.phase==Phase.GRAPPLED)
                    e.setValue(s.next(Phase.EXITED_PRONE,s.lastAcidTurn));
            }
            consumedEvents.add(event);revision++;
        }
        /** Host must spend five feet of movement; result exits corpse Prone. */
        public synchronized Status exitDeadSwallower(String actor,String victim,
                int movementSpentFeet,String event){
            event(event);
            check(deadActors.contains(actor),"swallower_not_dead");
            Status s=statuses.get(victim);
            check(s!=null&&s.actor.equals(actor)&&s.phase==Phase.CORPSE_EXIT_READY
                &&movementSpentFeet==5,"corpse_exit_requires_5ft_movement");
            Status next=s.next(Phase.EXITED_PRONE,s.lastAcidTurn);
            statuses.put(victim,next);
            consumedEvents.add(event);revision++;
            return next;
        }
    }
    private static void check(boolean value,String reason){
        if(!value)throw new IllegalArgumentException(reason);
    }
}
