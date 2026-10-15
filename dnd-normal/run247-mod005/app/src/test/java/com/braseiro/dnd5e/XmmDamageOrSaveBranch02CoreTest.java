package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmDamageOrSaveBranch02.*;

/** Runs with plain javac/java via tools/test_xmm_damage_or_save_branch02.py. */
public final class XmmDamageOrSaveBranch02CoreTest {
    private static int checks;
    static void eq(Object a,Object b) { checks++; if(!Objects.equals(a,b)) throw new AssertionError(a+" != "+b); }
    static void rejects(Runnable r) {
        checks++; try {r.run();} catch(IllegalArgumentException|IllegalStateException ex) {return;}
        throw new AssertionError("expected rejection");
    }
    static Component part(Type t,String f) { return new Component(t,f); }
    static Contract hit(Component... p) {
        return new Contract("/monster/153/action/2/entries/0",Trigger.HIT,Economy.ACTION,1,2,null,0,Success.NONE,Arrays.asList(p));
    }
    static Target target(String id,boolean hit,int natural,Type t,Defense d,Integer... dice) {
        return new Target(id,hit,natural,0,Collections.singletonList(Arrays.asList(dice)),Collections.singletonMap(t,d));
    }
    static Session state() {
        Map<String,Integer> hp=new LinkedHashMap<>(); hp.put("a",100); hp.put("b",100);
        return new Session(hp,2,2);
    }
    public static void main(String[] args) {
        for(Type t:Type.values()) for(Defense d:Defense.values()) {
            Session s=state(); Contract c=hit(part(t,"1d10 + 3"));
            Proposal p=s.propose("hit",c,Arrays.asList(target("a",true,0,t,d,8)));
            eq(s.hitPoints().get("a"),100); eq(s.remaining(Economy.ACTION),2);
            int damage=d==Defense.IMMUNE?0:d==Defense.RESISTANT?5:d==Defense.VULNERABLE?22:d==Defense.RESISTANT_AND_VULNERABLE?10:11;
            s.execute(p); eq(s.hitPoints().get("a"),100-damage); eq(s.remaining(Economy.ACTION),1);
            rejects(()->s.execute(p)); rejects(()->s.propose("hit",c,Arrays.asList(target("a",true,0,t,d,8))));
        }
        for(Success policy:Success.values()) for(int natural:new int[]{1,18,20}) {
            Session s=state(); Contract c=new Contract("/monster/117/legendary/1/entries/0",Trigger.SAVE,
                Economy.LEGENDARY,2,2,"con",18,policy,Arrays.asList(part(Type.NECROTIC,"1d10")));
            Proposal p=s.propose("save",c,Arrays.asList(target("a",false,natural,Type.NECROTIC,Defense.RESISTANT,9),
                target("b",false,1,Type.NECROTIC,Defense.IMMUNE,9)));
            int raw=natural<18?9:policy==Success.NONE?0:policy==Success.HALF?4:9;
            s.execute(p); eq(s.hitPoints().get("a"),100-raw/2); eq(s.hitPoints().get("b"),100);
            eq(s.remaining(Economy.LEGENDARY),0); eq(s.remaining(Economy.ACTION),2);
            rejects(()->s.propose("again",c,Arrays.asList(target("a",false,1,Type.NECROTIC,Defense.NORMAL,9))));
        }
        Session s=state(); Contract c=hit(part(Type.FIRE,"1d6 + 3"));
        Target a=target("a",true,0,Type.FIRE,Defense.NORMAL,6);
        Proposal first=s.propose("one",c,Arrays.asList(a));
        Proposal stale=s.propose("two",c,Arrays.asList(a));
        rejects(()->state().execute(first)); s.execute(first); rejects(()->s.execute(stale));
        rejects(()->s.propose("dup",c,Arrays.asList(a,a)));
        rejects(()->s.propose("unknown",c,Arrays.asList(target("x",true,0,Type.FIRE,Defense.NORMAL,1))));
        rejects(()->s.propose("range",c,Arrays.asList(target("a",true,0,Type.FIRE,Defense.NORMAL,7))));
        rejects(()->s.propose("count",c,Arrays.asList(target("a",true,0,Type.FIRE,Defense.NORMAL,1,2))));
        rejects(()->s.propose("defense",c,Arrays.asList(target("a",true,0,Type.COLD,Defense.NORMAL,1))));
        rejects(()->new Component(Type.FIRE,"2d6 + 3d8"));
        rejects(()->hit(part(Type.FIRE,"1d6"),part(Type.FIRE,"1d8")));
        rejects(()->new Contract("/monster/117/legendary/1/entries/0",Trigger.SAVE,Economy.LEGENDARY,1,1,null,18,Success.HALF,Arrays.asList(part(Type.FIRE,"1d6"))));
        rejects(()->s.propose("empty",c,Collections.emptyList()));
        eq(s.hitPoints().get("a"),91); eq(s.remaining(Economy.ACTION),1);
        Proposal miss=s.propose("miss",c,Arrays.asList(target("a",false,0,Type.FIRE,Defense.NORMAL,6)));
        s.execute(miss); eq(s.hitPoints().get("a"),91); eq(s.remaining(Economy.ACTION),0);
        rejects(()->s.propose("spent",c,Arrays.asList(a)));
        // Split types must retain separate defenses. Manual values, no automatic rolls.
        Session mixed=state(); Contract compound=hit(part(Type.BLUDGEONING,"3d10 + 7"),part(Type.FIRE,"1d8"));
        Map<Type,Defense> defs=new EnumMap<>(Type.class); defs.put(Type.BLUDGEONING,Defense.RESISTANT);defs.put(Type.FIRE,Defense.IMMUNE);
        Target both=new Target("a",true,0,0,Arrays.asList(Arrays.asList(10,10,10),Arrays.asList(8)),defs);
        mixed.execute(mixed.propose("mixed",compound,Arrays.asList(both)));eq(mixed.hitPoints().get("a"),82);
        // Input snapshots cannot mutate a previously created target/contract.
        defs.put(Type.BLUDGEONING,Defense.VULNERABLE);
        Session copy=state(); copy.execute(copy.propose("copy",compound,Arrays.asList(both))); eq(copy.hitPoints().get("a"),82);
        // Saving throws do not use automatic natural 1/20 success/failure.
        eq(AreaSaveDamageRules.resolve(9,"con",25,20,0,"none",false,false).damage,9);
        eq(AreaSaveDamageRules.resolve(9,"con",2,1,2,"none",false,false).damage,0);
        System.out.println("PASS "+checks+" deterministic core checks; no XMM action certified");
    }
}
