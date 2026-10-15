package com.braseiro.dnd5e;
import java.util.*;
import static com.braseiro.dnd5e.XmmDamageOrSaveBranch03.*;
/** Standalone JVM regression suite; intentionally usable without Android/JUnit dependencies. */
public final class XmmDamageOrSaveBranch03Test {
    static int checks;
    static void eq(Object a,Object b){checks++;if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b);}
    static void blocked(Runnable r){checks++;try{r.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("must reject");}
    static Set<Type> none(){return new HashSet<>();}
    static Target target(String id,Kind k,Type... immunity){return new Target(id,k,100,new HashSet<>(Arrays.asList(immunity)),none(),none());}
    static Session session(Target... targets){return new Session(Arrays.asList(targets),0);}
    static Context ctx(Action a,String second,boolean hit,Type origin,int... dice){return new Context(a,"p",second,hit,false,5,5,false,RollMode.NORMAL,origin,dice);}
    public static void main(String[] args){
        Session s=session(target("p",Kind.CREATURE),target("s",Kind.CREATURE,Type.SLASHING));
        Proposal p=s.propose(ctx(Action.BEAK,"s",true,null,12,12,12));
        eq(s.target("p").hp,100);eq(p.result.damage.get("p"),29);eq(p.result.damage.get("s"),0);
        Proposal alternate=s.propose(ctx(Action.BEAK,null,true,null,1,1));
        s.execute(p);eq(s.target("p").hp,71);eq(p.result.combatPersisted,false);
        blocked(()->s.execute(p));blocked(()->s.execute(alternate));blocked(()->s.propose(ctx(Action.BEAK,null,false,null)));
        Session other=session(target("p",Kind.CREATURE));blocked(()->other.execute(p));
        eq(other.execute(other.propose(ctx(Action.BEAK,null,false,null))).damage.size(),0);
        Session claw=session(target("p",Kind.CREATURE,Type.FIRE));
        eq(claw.execute(claw.propose(ctx(Action.CLAW,null,true,Type.FIRE,4,6,6))).damage.get("p"),8);
        for(Type origin: new Type[]{Type.ACID,Type.COLD,Type.FIRE,Type.LIGHTNING,Type.POISON}){
            Session c=session(target("p",Kind.CREATURE));eq(c.propose(ctx(Action.CLAW,null,true,origin,4,6,6)).result.damage.get("p"),20);
        }
        blocked(()->session(target("p",Kind.CREATURE)).propose(ctx(Action.CLAW,null,true,null,4,6,6)));
        blocked(()->session(target("p",Kind.CREATURE)).propose(ctx(Action.CLAW,null,true,Type.PIERCING,4,6,6)));
        for(Kind k:Kind.values()){
            Session bite=session(target("p",k));Result r=bite.execute(bite.propose(ctx(Action.BITE,null,true,null,10)));
            eq(r.damage.get("p"),13);eq(r.temporaryHp,k==Kind.CREATURE?13:0);
        }
        Target resistant=new Target("p",Kind.CREATURE,100,none(),new HashSet<>(Arrays.asList(Type.PIERCING)),none());
        eq(session(resistant).propose(ctx(Action.BITE,null,true,null,10)).result.temporaryHp,6);
        Session high=new Session(Arrays.asList(target("p",Kind.CREATURE)),30);
        eq(high.propose(ctx(Action.BITE,null,true,null,10)).result.temporaryHp,30);
        for(boolean inside:new boolean[]{false,true})for(int distance:new int[]{10,30,90}){
            boolean far=distance>30;RollMode mode=inside==far?RollMode.NORMAL:inside?RollMode.ADVANTAGE:RollMode.DISADVANTAGE;
            Context c=new Context(Action.SLAM,"p",null,true,true,distance,0,inside,mode,null,8,8,8,8,8);
            eq(session(target("p",Kind.CREATURE)).propose(c).result.damage.get("p"),45);
        }
        blocked(()->session(target("p",Kind.CREATURE)).propose(new Context(Action.SLAM,"p",null,true,true,91,0,false,RollMode.DISADVANTAGE,null,1,1,1,1,1)));
        blocked(()->session(target("p",Kind.CREATURE)).propose(new Context(Action.SLAM,"p",null,true,false,5,0,true,RollMode.NORMAL,null,1,1,1,1,1)));
        blocked(()->session(target("p",Kind.CREATURE)).propose(ctx(Action.BEAK,"p",true,null,1,1,1)));
        blocked(()->session(target("p",Kind.CREATURE),target("s",Kind.OBJECT)).propose(ctx(Action.BEAK,"s",true,null,1,1,1)));
        blocked(()->session(target("p",Kind.CREATURE)).propose(ctx(Action.BITE,null,true,null,11)));
        blocked(()->session(target("p",Kind.CREATURE)).propose(ctx(Action.CLAW,null,true,Type.COLD,4)));
        blocked(()->session(target("p",Kind.CREATURE)).propose(ctx(Action.BITE,null,false,null,1)));
        for(Action a:Action.values()){
            Session miss=session(target("p",Kind.CREATURE));
            Result result=miss.execute(miss.propose(ctx(a,null,false,a==Action.CLAW?Type.ACID:null)));
            eq(result.damage.size(),0);eq(miss.target("p").hp,100);eq(result.actionConsumed,true);
        }
        Set<Type> piercing=Collections.singleton(Type.PIERCING);
        Target vulnerable=new Target("p",Kind.CREATURE,100,none(),none(),piercing);
        eq(session(vulnerable).propose(ctx(Action.BITE,null,true,null,10)).result.damage.get("p"),26);
        Target both=new Target("p",Kind.CREATURE,100,none(),piercing,piercing);
        eq(session(both).propose(ctx(Action.BITE,null,true,null,10)).result.damage.get("p"),12);
        eq(session(target("p",Kind.CREATURE,Type.PIERCING)).propose(ctx(Action.BITE,null,true,null,10)).result.temporaryHp,0);
        Session invalid=session(target("p",Kind.CREATURE));
        blocked(()->invalid.propose(ctx(Action.BITE,null,true,null,0)));
        eq(invalid.target("p").hp,100);
        eq(invalid.execute(invalid.propose(ctx(Action.BITE,null,true,null,1))).damage.get("p"),4);
        blocked(()->session(target("p",Kind.CREATURE)).propose(new Context(Action.BITE,"p",null,true,true,5,0,false,RollMode.NORMAL,null,1)));
        blocked(()->session(target("p",Kind.CREATURE),target("s",Kind.CREATURE)).propose(new Context(Action.BEAK,"p","s",true,false,5,6,false,RollMode.NORMAL,null,1,1,1)));
        eq(Action.CLAW.toHit,7);eq(Action.SLAM.normalRange,30);eq(Action.SLAM.longRange,90);
        for(String ptr:args)blocked(()->Action.at(ptr));
        eq(Action.at(Action.BEAK.pointer),Action.BEAK);
        System.out.println("PASS "+checks+" deterministic assertions");
    }
}
