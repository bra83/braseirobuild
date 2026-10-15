package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmAttackHitCondition01.*;

/** Self-contained JDK 8 regression, no dice rolled by the executor. */
public final class XmmAttackHitCondition01Scenarios {
    private static int checks;
    private static void yes(boolean v){checks++;if(!v)throw new AssertionError("failed_check_"+checks);}
    private static void eq(int got,int wanted){yes(got==wanted);}
    private static void rejects(Runnable call){
        checks++;
        try{call.run();}catch(IllegalArgumentException ok){return;}
        throw new AssertionError("expected_rejection_"+checks);
    }
    private static Map<Type,Defense> defenses(Defense d){
        EnumMap<Type,Defense> ds=new EnumMap<>(Type.class);
        for(Type t:Type.values())ds.put(t,d);
        return ds;
    }
    private static Target target(String id,int hp,int max,Size size,Defense d){
        return new Target(id,hp,max,size,true,defenses(d));
    }
    private static Session session(Target... targets){return new Session(Arrays.asList(targets));}
    private static Request attack(Session s,Action a,String event,String target,
            int reach,boolean hit,boolean bloodied,boolean inSpace,int slot,int... rolls){
        return new Request(a,"enemy",target,event,s.revision(),reach,hit,
            bloodied,inSpace,slot,rolls);
    }
    private static boolean has(Grapple g,Condition c){return g.conditions.contains(c);}
    private static void identities(){
        yes(Action.values().length==4);
        Set<String> ptr=new HashSet<>();
        for(Action a:Action.values()){
            yes(a.pointer.startsWith("/monster/"));
            yes(ptr.add(a.pointer));
            yes(Action.source(a.pointer)==a);
        }
        rejects(()->Action.source("/other"));
        eq(Action.MIND_FLAYER.toHit,7);
        eq(Action.ROPER.toHit,7);
        eq(Action.SWARM_STIRGES.toHit,5);
        eq(Action.TARRASQUE.toHit,19);
        eq(Action.ROPER.reachFeet,60);
        eq(Action.TARRASQUE.reachFeet,15);
        eq(Action.MIND_FLAYER.escapeDc,14);
        eq(Action.SWARM_STIRGES.escapeDc,13);
        eq(Action.TARRASQUE.escapeDc,20);
        eq(Action.ROPER.roperTentacleCount,6);
    }
    private static void mindFlayer(){
        for(Defense defense:Defense.values()){
            Session s=session(target("hero",100,100,Size.MEDIUM,defense));
            Request r=attack(s,Action.MIND_FLAYER,"mind-"+defense,
                "hero",5,true,false,false,0,1,1,1,1);
            Proposal p=s.propose(r);
            int expected=defense==Defense.IMMUNE?0:
                defense==Defense.RESISTANT?4:
                defense==Defense.VULNERABLE?16:
                defense==Defense.BOTH?8:8;
            eq(p.result.damage,expected);
            eq(s.target("hero").hp,100);
            yes(!p.result.combatPersisted);
            yes(p.result.newGrapple!=null);
            eq(p.result.newGrapple.escapeDc,14);
            yes(has(p.result.newGrapple,Condition.GRAPPLED));
            yes(has(p.result.newGrapple,Condition.STUNNED));
            s.commit(p);
            eq(s.target("hero").hp,100-expected);
            yes(s.grapples("hero").size()==1);
            rejects(()->s.commit(p));
            rejects(()->s.propose(r));
            String id=s.grapples("hero").get(0).id;
            yes(!s.escape(id,false,"escape-failed-"+defense));
            eq(s.grapples("hero").size(),1);
            yes(s.escape(id,true,"escape-success-"+defense));
            eq(s.grapples("hero").size(),0);
            rejects(()->s.escape(id,true,"escape-again-"+defense));
        }
        Session large=session(target("giant",100,100,Size.LARGE,Defense.NORMAL));
        Result r=large.commit(large.propose(attack(large,Action.MIND_FLAYER,"large",
            "giant",5,true,false,false,0,1,1,1,1)));
        eq(r.damage,8);
        yes(r.newGrapple==null);
        eq(large.grapples("giant").size(),0);
        Session outOfRange=session(target("hero",100,100,Size.MEDIUM,Defense.NORMAL));
        rejects(()->outOfRange.propose(attack(outOfRange,Action.MIND_FLAYER,
            "too-far","hero",6,true,false,false,0,1,1,1,1)));
        rejects(()->outOfRange.propose(attack(outOfRange,Action.MIND_FLAYER,
            "wrong-die","hero",5,true,false,false,0,1,1,1,9)));
        Result missed=outOfRange.propose(attack(outOfRange,Action.MIND_FLAYER,
            "missed","hero",5,false,false,false,0)).result;
        eq(missed.damage,0);
        yes(missed.newGrapple==null);
        rejects(()->outOfRange.propose(attack(outOfRange,Action.MIND_FLAYER,
            "miss-with-roll","hero",5,false,false,false,0,1,1,1,1)));
    }
    private static void roper(){
        Session s=session(target("hero",50,50,Size.GARGANTUAN,Defense.NORMAL),
            target("second",50,50,Size.MEDIUM,Defense.NORMAL));
        Request r=attack(s,Action.ROPER,"roper1","hero",60,true,false,false,1);
        Proposal p=s.propose(r);
        eq(p.result.damage,0);
        Grapple grapple=p.result.newGrapple;
        yes(grapple!=null);
        yes(has(grapple,Condition.POISONED));
        yes(has(grapple,Condition.GRAPPLED));
        eq(grapple.escapeDc,14);
        s.commit(p);
        eq(s.target("hero").hp,50);
        eq(s.grapples("hero").size(),1);
        rejects(()->s.propose(attack(s,Action.ROPER,"reuse1",
            "second",60,true,false,false,1)));
        rejects(()->s.propose(attack(s,Action.ROPER,"invalid-slot",
            "second",60,true,false,false,7)));
        eq(s.tentacle("enemy",1).ac,20);
        eq(s.tentacle("enemy",1).hp,10);
        Tentacle missed=s.damageRoperTentacle("enemy",1,19,Type.OTHER,999,"tentacleMiss");
        eq(missed.hp,10);
        Tentacle immune=s.damageRoperTentacle("enemy",1,20,Type.PSYCHIC,999,"immune");
        eq(immune.hp,10);
        Tentacle poisoned=s.damageRoperTentacle("enemy",1,20,Type.POISON,999,"poisonImmune");
        eq(poisoned.hp,10);
        Tentacle partial=s.damageRoperTentacle("enemy",1,20,Type.OTHER,9,"damaged");
        eq(partial.hp,1);
        yes(!partial.destroyed);
        Tentacle destroyed=s.damageRoperTentacle("enemy",1,20,Type.OTHER,1,"destroyed");
        eq(destroyed.hp,0);
        yes(destroyed.destroyed);
        eq(s.grapples("hero").size(),0);
        rejects(()->s.propose(attack(s,Action.ROPER,"destroyed-attack",
            "hero",60,true,false,false,1)));
        rejects(()->s.damageRoperTentacle("enemy",1,20,Type.OTHER,1,"repeat-destroyed"));
        s.roperTurnStart("enemy","fresh-turn-01");
        eq(s.tentacle("enemy",1).hp,10);
        rejects(()->s.roperTurnStart("enemy","fresh-turn-01"));
        Result fresh=s.commit(s.propose(attack(s,Action.ROPER,"new-attack",
            "second",60,true,false,false,1)));
        yes(fresh.newGrapple!=null);
        eq(s.grapples("second").size(),1);
        yes(s.tentacle("enemy",2).hp==10);
    }
    private static void stirges(){
        Session s=session(target("hero",100,100,Size.SMALL,Defense.NORMAL));
        Request full=attack(s,Action.SWARM_STIRGES,"swarmHit",
            "hero",5,true,false,true,0,1,1);
        Proposal p=s.propose(full);
        eq(p.result.damage,5);
        yes(p.result.newGrapple!=null);
        yes(has(p.result.newGrapple,Condition.END_TURN_NECROTIC_2D6));
        s.commit(p);
        eq(s.target("hero").hp,95);
        String grapple=s.grapples("hero").get(0).id;
        eq(s.swarmEndTargetTurn(grapple,"turn01",new int[]{2,3},"tick01"),5);
        eq(s.target("hero").hp,90);
        rejects(()->s.swarmEndTargetTurn(grapple,"turn01",new int[]{2,3},"tick02"));
        eq(s.swarmEndTargetTurn(grapple,"turn02",new int[]{1,1},"tick03"),2);
        eq(s.target("hero").hp,88);
        yes(s.escape(grapple,true,"released"));
        rejects(()->s.swarmEndTargetTurn(grapple,"turn03",new int[]{1,1},"tick04"));
        Session bloodied=session(target("hero",100,100,Size.MEDIUM,Defense.NORMAL));
        Result reduced=bloodied.propose(attack(bloodied,Action.SWARM_STIRGES,
            "bloodied","hero",5,true,true,true,0,1)).result;
        eq(reduced.damage,4);
        yes(reduced.newGrapple!=null);
        Session outside=session(target("hero",100,100,Size.MEDIUM,Defense.NORMAL));
        Result distant=outside.propose(attack(outside,Action.SWARM_STIRGES,
            "out-of-space","hero",5,true,false,false,0,1,1)).result;
        eq(distant.damage,5);
        yes(distant.newGrapple==null);
        Session huge=session(target("hero",100,100,Size.HUGE,Defense.NORMAL));
        Result tooLarge=huge.propose(attack(huge,Action.SWARM_STIRGES,
            "huge","hero",5,true,false,true,0,1,1)).result;
        eq(tooLarge.damage,5);
        yes(tooLarge.newGrapple==null);
    }
    private static void tarrasque(){
        Session s=session(target("hero",60,60,Size.GARGANTUAN,Defense.NORMAL));
        Proposal p=s.propose(attack(s,Action.TARRASQUE,"bite","hero",
            15,true,false,false,0,1,1,1,1));
        eq(p.result.damage,14);
        yes(p.result.newGrapple!=null);
        yes(has(p.result.newGrapple,Condition.RESTRAINED));
        yes(has(p.result.newGrapple,Condition.NO_TELEPORT));
        eq(p.result.newGrapple.escapeDc,20);
        s.commit(p);
        eq(s.target("hero").hp,46);
        String id=s.grapples("hero").get(0).id;
        yes(s.escape(id,true,"tarrasque-escape"));
        eq(s.grapples("hero").size(),0);
        rejects(()->s.propose(attack(s,Action.TARRASQUE,
            "wrong-tentacle","hero",15,true,false,false,1,1,1,1,1)));
        Session missing=session(target("hero",10,10,Size.MEDIUM,Defense.NORMAL));
        rejects(()->missing.propose(attack(missing,Action.TARRASQUE,
            "missing-dice","hero",15,true,false,false,0,1,1)));
    }
    private static void crossSession(){
        Session one=session(target("hero",50,50,Size.MEDIUM,Defense.NORMAL));
        Session other=session(target("hero",50,50,Size.MEDIUM,Defense.NORMAL));
        Proposal p=one.propose(attack(one,Action.MIND_FLAYER,"once",
            "hero",5,true,false,false,0,1,1,1,1));
        rejects(()->other.commit(p));
        one.commit(p);
        rejects(()->one.commit(p));
        eq((int)one.revision(),1);
    }
    public static void main(String[] args){
        identities();
        mindFlayer();
        roper();
        stirges();
        tarrasque();
        crossSession();
        System.out.println("PASS attack_hit_condition-01 "+checks+
            " independent Java assertions; 4 XMM contracts; Android/SQLite NOT integrated");
    }
}
