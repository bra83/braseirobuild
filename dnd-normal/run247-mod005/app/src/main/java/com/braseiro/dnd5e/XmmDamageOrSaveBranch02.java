package com.braseiro.dnd5e;

import java.util.*;
import java.util.regex.*;

/** Independent, deterministic damage transaction core. No registered XMM actions:
 * source riders must be verified before a combat adapter may construct a contract.
 * Caller supplies dice individually; this class never rolls or writes SQLite.
 * Instant damage only: durations/conditions require a separate verified executor.
 */
public final class XmmDamageOrSaveBranch02 {
    private XmmDamageOrSaveBranch02() {}
    public enum Type { ACID, BLUDGEONING, COLD, FIRE, FORCE, LIGHTNING, NECROTIC,
        PIERCING, POISON, PSYCHIC, RADIANT, SLASHING, THUNDER }
    public enum Defense { NORMAL, RESISTANT, IMMUNE, VULNERABLE, RESISTANT_AND_VULNERABLE }
    public enum Trigger { HIT, SAVE }
    public enum Success { NONE, HALF, FULL }
    public enum Economy { ACTION, LEGENDARY }

    public static final class Component {
        public final Type type;
        public final String formula;
        final int count, sides, bonus;
        public Component(Type type, String formula) {
            this.type=Objects.requireNonNull(type);
            Matcher m=Pattern.compile("([1-9][0-9]?)d([1-9][0-9]?)(?: \\+ ([0-9]{1,3}))?").matcher(Objects.requireNonNull(formula));
            if(!m.matches()) throw new IllegalArgumentException("unsupported formula");
            this.formula=formula; count=Integer.parseInt(m.group(1));
            sides=Integer.parseInt(m.group(2)); bonus=m.group(3)==null?0:Integer.parseInt(m.group(3));
        }
        int damage(List<Integer> dice) {
            if(dice==null || dice.size()!=count) throw new IllegalArgumentException("dice count");
            int total=bonus;
            for(Integer die:dice) {
                if(die==null || die<1 || die>sides) throw new IllegalArgumentException("die range");
                total+=die;
            }
            return total;
        }
    }
    public static final class Contract {
        public final String pointer;
        public final Trigger trigger;
        public final Economy economy;
        public final Success success;
        public final String ability;
        public final int dc, cost, maxTargets;
        public final List<Component> components;
        public Contract(String pointer, Trigger trigger, Economy economy, int cost,
                        int maxTargets, String ability, int dc, Success success,
                        List<Component> components) {
            if(pointer==null || !pointer.matches("/monster/[0-9]+/(action|legendary)/[0-9]+/entries/0"))
                throw new IllegalArgumentException("source pointer");
            this.pointer=pointer; this.trigger=Objects.requireNonNull(trigger);
            this.economy=Objects.requireNonNull(economy); this.success=Objects.requireNonNull(success);
            if(pointer.contains("/legendary/") != (economy==Economy.LEGENDARY))
                throw new IllegalArgumentException("source/economy mismatch");
            if(cost<1 || maxTargets<1 || components==null || components.isEmpty()) throw new IllegalArgumentException("incomplete contract");
            if(economy==Economy.ACTION && cost!=1) throw new IllegalArgumentException("action cost");
            if(trigger==Trigger.SAVE && (!AreaSaveDamageRules.validAbility(ability) || dc<1 || dc>40))
                throw new IllegalArgumentException("save contract");
            if(trigger==Trigger.HIT && (ability!=null || dc!=0 || success!=Success.NONE))
                throw new IllegalArgumentException("hit contract");
            this.cost=cost; this.maxTargets=maxTargets; this.ability=ability; this.dc=dc;
            HashSet<Type> types=new HashSet<>();
            for(Component c:components) if(c==null || !types.add(c.type)) throw new IllegalArgumentException("combine same-type damage before defenses");
            this.components=Collections.unmodifiableList(new ArrayList<>(components));
        }
    }
    public static final class Target {
        public final String id;
        final boolean hit;
        final int natural, bonus;
        final List<List<Integer>> dice;
        final Map<Type,Defense> defenses;
        public Target(String id, boolean hit, int natural, int bonus,
                      List<List<Integer>> dice, Map<Type,Defense> defenses) {
            if(id==null || id.trim().isEmpty()) throw new IllegalArgumentException("target id");
            this.id=id; this.hit=hit; this.natural=natural; this.bonus=bonus;
            List<List<Integer>> copy=new ArrayList<>();
            for(List<Integer> d:Objects.requireNonNull(dice)) copy.add(Collections.unmodifiableList(new ArrayList<>(d)));
            this.dice=Collections.unmodifiableList(copy);
            this.defenses=Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(defenses)));
        }
    }
    /** Owned independent state, deliberately not a combat/database adapter. */
    public static final class Session {
        private final Map<String,Integer> hp;
        private final Set<String> consumed=new HashSet<>();
        private int actions, legendary;
        private long revision;
        public Session(Map<String,Integer> hp, int actions, int legendary) {
            this.hp=new LinkedHashMap<>(Objects.requireNonNull(hp));
            for(Map.Entry<String,Integer> e:this.hp.entrySet())
                if(e.getKey()==null || e.getValue()==null || e.getValue()<0) throw new IllegalArgumentException("hp");
            if(actions<0 || legendary<0) throw new IllegalArgumentException("budget");
            this.actions=actions; this.legendary=legendary;
        }
        public synchronized Map<String,Integer> hitPoints() { return Collections.unmodifiableMap(new LinkedHashMap<>(hp)); }
        public synchronized int remaining(Economy e) { return e==Economy.ACTION?actions:legendary; }
        public synchronized Proposal propose(String token, Contract c, List<Target> targets) {
            if(token==null || token.trim().isEmpty() || consumed.contains(token)) throw new IllegalArgumentException("replay/token");
            Objects.requireNonNull(c);
            if(remaining(c.economy)<c.cost) throw new IllegalStateException("budget");
            if(targets==null || targets.isEmpty() || targets.size()>c.maxTargets) throw new IllegalArgumentException("target count");
            Map<String,Integer> next=new LinkedHashMap<>();
            for(Target t:targets) {
                if(t==null || !hp.containsKey(t.id) || next.containsKey(t.id)) throw new IllegalArgumentException("target");
                if(t.dice.size()!=c.components.size()) throw new IllegalArgumentException("components");
                int damage=0;
                for(int i=0;i<c.components.size();i++) {
                    Component part=c.components.get(i);
                    Defense defense=t.defenses.get(part.type);
                    if(defense==null) throw new IllegalArgumentException("explicit defense required");
                    int amount=part.damage(t.dice.get(i));
                    if(c.trigger==Trigger.HIT) { if(!t.hit) amount=0; }
                    else amount=AreaSaveDamageRules.resolve(amount,c.ability,c.dc,t.natural,t.bonus,
                        c.success.name().toLowerCase(Locale.ROOT),false,false).damage;
                    switch(defense) {
                        case IMMUNE: amount=0; break;
                        case RESISTANT: amount/=2; break;
                        case VULNERABLE: amount*=2; break;
                        case RESISTANT_AND_VULNERABLE: amount=(amount/2)*2; break;
                        default: break;
                    }
                    damage=Math.addExact(damage,amount);
                }
                next.put(t.id,Math.max(0,hp.get(t.id)-damage));
            }
            return new Proposal(this,revision,token,c.economy,c.cost,next);
        }
        public synchronized void execute(Proposal p) {
            if(p==null || p.owner!=this || p.revision!=revision || consumed.contains(p.token))
                throw new IllegalStateException("foreign/stale/replayed proposal");
            if(remaining(p.economy)<p.cost) throw new IllegalStateException("budget");
            hp.putAll(p.next); consumed.add(p.token);
            if(p.economy==Economy.ACTION) actions-=p.cost; else legendary-=p.cost;
            revision++;
        }
    }
    public static final class Proposal {
        private final Session owner;
        private final long revision;
        private final String token;
        private final Economy economy;
        private final int cost;
        private final Map<String,Integer> next;
        private Proposal(Session owner,long revision,String token,Economy economy,int cost,Map<String,Integer> next) {
            this.owner=owner;this.revision=revision;this.token=token;this.economy=economy;this.cost=cost;
            this.next=Collections.unmodifiableMap(new LinkedHashMap<>(next));
        }
        public Map<String,Integer> resultingHitPoints() { return next; }
    }
}
