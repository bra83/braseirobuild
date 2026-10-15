package com.braseiro.dnd5e;

import java.util.*;

/** Independent, deterministic executor. No database, dice generation or combat integration.
 * Normal-hit outcomes and geometry are supplied explicitly by the caller. Critical hits
 * and additional external roll modifiers are outside this contract. A Session owns only
 * this isolated simulation: its results must never be presented as persisted combat changes.
 */
public final class XmmDamageOrSaveBranch03 {
    public static final String SOURCE_COMMIT="8c026b807fac21862a309379a0c3228a0683198b";
    public static final String SOURCE_BLOB="9a4137b508ef37562ed948894a70b1594b57e85c";
    private XmmDamageOrSaveBranch03() {}
    public enum Type { SLASHING, PIERCING, BLUDGEONING, ACID, COLD, FIRE, LIGHTNING, POISON }
    public enum Action {
        BEAK("/monster/171/action/1/entries/0",8,10,0,0),
        CLAW("/monster/236/action/1/entries/0",7,10,0,0),
        SLAM("/monster/238/action/1/entries/0",9,10,30,90),
        BITE("/monster/282/action/1/entries/0",5,5,0,0);
        public final String pointer;
        public final int toHit, reach, normalRange, longRange;
        Action(String p,int h,int r,int n,int l){pointer=p;toHit=h;reach=r;normalRange=n;longRange=l;}
        public static Action at(String pointer){
            for(Action a:values())if(a.pointer.equals(pointer))return a;
            throw new IllegalArgumentException("source_contract_not_implemented:"+pointer);
        }
    }
    public enum RollMode { NORMAL, ADVANTAGE, DISADVANTAGE }
    public enum Kind { CREATURE, CONSTRUCT, UNDEAD, OBJECT }
    public static final class Target {
        public final String id;
        public final Kind kind;
        public final int hp;
        private final Set<Type> immune,resistant,vulnerable;
        public Target(String id,Kind kind,int hp,Set<Type> immune,Set<Type> resistant,Set<Type> vulnerable){
            require(id!=null&&!id.isEmpty()&&kind!=null&&hp>=0,"target");
            this.id=id;this.kind=kind;this.hp=hp;
            this.immune=copy(immune);this.resistant=copy(resistant);this.vulnerable=copy(vulnerable);
        }
        private static Set<Type> copy(Set<Type> s){require(s!=null,"defenses");for(Type t:s)require(t!=null,"defenses");return new HashSet<>(s);}
        int damage(int raw,Type t){return DamageMitigationRules.apply(raw,immune.contains(t),resistant.contains(t),vulnerable.contains(t));}
        Target after(int damage){return new Target(id,kind,Math.max(0,hp-damage),immune,resistant,vulnerable);}
    }
    public static final class Context {
        public final Action action;
        public final String primary,secondary;
        public final boolean hit,ranged,insideRevenantSpace;
        public final RollMode rollMode;
        public final int distance,secondaryDistance;
        public final Type origin;
        private final int[] dice;
        public Context(Action action,String primary,String secondary,boolean hit,boolean ranged,
                int distance,int secondaryDistance,boolean insideRevenantSpace,RollMode rollMode,
                Type origin,int... dice){
            require(action!=null&&primary!=null&&dice!=null,"context");
            this.action=action;this.primary=primary;this.secondary=secondary;this.hit=hit;
            this.ranged=ranged;this.distance=distance;this.secondaryDistance=secondaryDistance;
            this.insideRevenantSpace=insideRevenantSpace;this.rollMode=rollMode;
            this.origin=origin;this.dice=dice.clone();
        }
    }
    public static final class Result {
        public final Map<String,Integer> damage;
        public final int temporaryHp;
        public final boolean actionConsumed=true;
        public final boolean combatPersisted=false;
        private Result(Map<String,Integer> d,int t){damage=Collections.unmodifiableMap(new LinkedHashMap<>(d));temporaryHp=t;}
    }
    public static final class Proposal {
        private final Session owner;
        private final long version;
        public final Result result;
        private Proposal(Session s,long v,Result r){owner=s;version=v;result=r;}
    }
    /** One action budget; start a new isolated session for a later turn. */
    public static final class Session {
        private final Map<String,Target> targets=new LinkedHashMap<>();
        private long version;
        private boolean consumed;
        private int temporaryHp;
        public Session(Collection<Target> targets,int temporaryHp){
            require(targets!=null&&temporaryHp>=0,"session");this.temporaryHp=temporaryHp;
            for(Target t:targets)require(t!=null&&this.targets.put(t.id,t)==null,"duplicate_target");
        }
        public synchronized Target target(String id){return targets.get(id);}
        public synchronized Proposal propose(Context c){
            require(!consumed,"action_spent");
            return new Proposal(this,version,resolve(c,targets,temporaryHp));
        }
        public synchronized Result execute(Proposal p){
            require(p!=null&&p.owner==this&&p.version==version&&!consumed,"stale_or_foreign_proposal");
            for(Map.Entry<String,Integer> e:p.result.damage.entrySet()){
                Target t=targets.get(e.getKey());targets.put(t.id,t.after(e.getValue()));
            }
            temporaryHp=p.result.temporaryHp;consumed=true;version++;return p.result;
        }
    }
    private static Result resolve(Context c,Map<String,Target> targets,int temporaryHp){
        require(c!=null,"context");Action a=c.action;Target primary=targets.get(c.primary);
        require(primary!=null,"primary");
        require(c.distance>=0&&c.distance<=(c.ranged?a.longRange:a.reach),"range");
        require(!c.ranged||a==Action.SLAM,"attack_mode");
        require(a==Action.SLAM||!c.insideRevenantSpace,"unexpected_space_context");
        boolean advantage=a==Action.SLAM&&c.insideRevenantSpace;
        boolean disadvantage=c.ranged&&c.distance>a.normalRange;
        RollMode expected=advantage==disadvantage?RollMode.NORMAL:advantage?RollMode.ADVANTAGE:RollMode.DISADVANTAGE;
        require(c.rollMode==expected,"source_roll_mode");
        require(c.secondary==null||a==Action.BEAK,"unexpected_secondary");
        Target secondary=c.secondary==null?null:targets.get(c.secondary);
        if(c.secondary!=null)require(secondary!=null&&!c.primary.equals(c.secondary)&&secondary.kind!=Kind.OBJECT
                &&c.secondaryDistance>=0&&c.secondaryDistance<=5,"secondary_creature_within_5ft");
        if(a==Action.CLAW)require(c.origin==Type.ACID||c.origin==Type.COLD||c.origin==Type.FIRE
                ||c.origin==Type.LIGHTNING||c.origin==Type.POISON,"draconic_origin");
        else require(c.origin==null,"unexpected_origin");
        Map<String,Integer> damage=new LinkedHashMap<>();
        if(!c.hit){require(c.dice.length==0,"miss_has_no_damage_dice");return new Result(damage,temporaryHp);}
        int raw;Type type;
        switch(a){
            case BEAK:
                require(c.dice.length==(secondary==null?2:3),"dice_count");
                raw=die(c,0,12)+die(c,1,12)+5;type=Type.SLASHING;
                if(secondary!=null)damage.put(secondary.id,secondary.damage(die(c,2,12),Type.SLASHING));break;
            case CLAW:
                require(c.dice.length==3,"dice_count");
                raw=die(c,0,4)+4;type=Type.SLASHING;
                damage.put(primary.id,primary.damage(raw,type)+primary.damage(die(c,1,6)+die(c,2,6),c.origin));
                return new Result(damage,temporaryHp);
            case SLAM:
                require(c.dice.length==5,"dice_count");raw=5;
                for(int i=0;i<5;i++)raw+=die(c,i,8);type=Type.BLUDGEONING;break;
            case BITE:
                require(c.dice.length==1,"dice_count");raw=die(c,0,10)+3;type=Type.PIERCING;break;
            default:throw new IllegalArgumentException("unsupported");
        }
        int dealt=primary.damage(raw,type);damage.put(primary.id,dealt);
        if(a==Action.BITE&&primary.kind==Kind.CREATURE)temporaryHp=Math.max(temporaryHp,dealt);
        return new Result(damage,temporaryHp);
    }
    private static int die(Context c,int i,int sides){int d=c.dice[i];require(d>=1&&d<=sides,"die_out_of_range");return d;}
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalArgumentException(reason);}
}
