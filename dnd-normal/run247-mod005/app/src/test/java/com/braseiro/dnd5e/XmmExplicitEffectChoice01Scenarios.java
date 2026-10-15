package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmExplicitEffectChoice01.*;

/** Standalone JDK8 tests; no real combat and no invisible dice or timers. */
public final class XmmExplicitEffectChoice01Scenarios {
    private static int checks;
    private static void yes(boolean v){checks++;if(!v)throw new AssertionError("failed_"+checks);}
    private static void eq(int actual,int expected){yes(actual==expected);}
    private static void blocked(Runnable test){
        checks++;
        try{test.run();}catch(IllegalArgumentException correct){return;}
        throw new AssertionError("expected_rejection_"+checks);
    }
    private static Target target(String id,Size size,int hp,boolean creature,Defense defense){
        return new Target(id,hp,hp,size,creature,defense);
    }
    private static Session session(Size size,int victimHp,Defense defense){
        return new Session(Arrays.asList(
            target("rug",Size.LARGE,50,false,Defense.NORMAL),
            target("hero",size,victimHp,true,defense)));
    }
    private static Request req(Session s,String event,boolean hit,Choice choice,int... dice){
        return new Request("rug","hero",event,s.revision(),5,hit,choice,dice);
    }
    private static Grapple grapple(Session s,String event) {
        Proposal p=s.propose(req(s,event,true,Choice.GRAPPLE));
        eq(p.result.damage,0);
        yes(p.result.grappled);
        yes(!p.result.combatPersisted);
        eq(s.target("hero").hp,100);
        s.commit(p);
        return s.grapple("rug");
    }
    private static void constants(){
        yes(POINTER.equals("/monster/31/action/0/entries/0"));
        eq(TO_HIT,5);eq(REACH_FEET,5);eq(ESCAPE_DC,13);
        yes(Condition.values().length==5);
        yes(Choice.values().length==2);
    }
    private static void choiceAndDamage(){
        for(Defense defense:Defense.values()){
            Session s=session(Size.MEDIUM,100,defense);
            Request r=req(s,"attack-"+defense,true,Choice.DAMAGE,2,3);
            Proposal p=s.propose(r);
            eq(p.result.rolledDamage,8);
            int expected=defense==Defense.IMMUNE?0:
                defense==Defense.RESISTANT?4:
                defense==Defense.VULNERABLE?16:
                defense==Defense.BOTH?8:8;
            eq(p.result.damage,expected);
            yes(!p.result.grappled);
            yes(s.grapple("rug")==null);
            eq(s.target("hero").hp,100);
            s.commit(p);
            eq(s.target("hero").hp,100-expected);
            yes(s.grapple("rug")==null);
            blocked(()->s.commit(p));
            blocked(()->s.propose(r));
        }
        Session missed=session(Size.MEDIUM,100,Defense.NORMAL);
        Proposal miss=missed.propose(req(missed,"miss",false,Choice.DAMAGE));
        eq(miss.result.damage,0);
        yes(!miss.result.grappled);
        blocked(()->missed.propose(req(missed,"miss-choice",false,Choice.GRAPPLE)));
        blocked(()->missed.propose(req(missed,"miss-roll",false,Choice.DAMAGE,2,3)));
        blocked(()->missed.propose(req(missed,"bad-roll",true,Choice.DAMAGE,7,1)));
        blocked(()->missed.propose(req(missed,"missing-roll",true,Choice.DAMAGE)));
    }
    private static void grappleStates(){
        Session s=session(Size.MEDIUM,100,Defense.NORMAL);
        Grapple g=grapple(s,"smother");
        eq(g.escapeDc,13);
        yes(g.rug.equals("rug")&&g.target.equals("hero"));
        for(Condition c:Condition.values())yes(g.conditions.contains(c));
        yes(!s.canSmother("rug"));
        blocked(()->s.propose(req(s,"secondAction",true,Choice.DAMAGE,1,1)));
        blocked(()->s.propose(req(s,"secondGrapple",true,Choice.GRAPPLE)));
        yes(!s.release("rug",false,"failed-escape"));
        yes(s.grapple("rug")!=null);
        yes(s.release("rug",true,"successful-escape"));
        yes(s.grapple("rug")==null);
        yes(s.canSmother("rug"));
        blocked(()->s.release("rug",true,"second-escape"));
        // A previously smothered target can be targeted again after release.
        Proposal after=s.propose(req(s,"fresh-smother",true,Choice.GRAPPLE));
        yes(after.result.grappled);
        s.commit(after);
        yes(s.grapple("rug")!=null);

        for(Size size:Size.values()){
            Session other=session(size,100,Defense.NORMAL);
            if(size.ordinal()<=Size.MEDIUM.ordinal()){
                yes(other.propose(req(other,"legal",true,Choice.GRAPPLE)).result.grappled);
            }else{
                blocked(()->other.propose(req(other,"oversize",true,Choice.GRAPPLE)));
                yes(other.propose(req(other,"damage",true,Choice.DAMAGE,1,1)).result.damage>0);
            }
        }
        Session s2=session(Size.SMALL,100,Defense.NORMAL);
        blocked(()->s2.propose(new Request("rug","hero","out-of-reach",0,6,
            true,Choice.GRAPPLE,new int[0])));
        blocked(()->s2.propose(new Request("rug","hero","null-choice",0,5,
            true,null,new int[0])));
    }
    private static void periodicDamage(){
        for(Defense defense:Defense.values()){
            Session s=session(Size.SMALL,100,defense);
            grapple(s,"start");
            int expected=defense==Defense.IMMUNE?0:
                defense==Defense.RESISTANT?3:
                defense==Defense.VULNERABLE?12:
                defense==Defense.BOTH?6:6;
            eq(s.targetTurnStart("rug","hero","heroTurn1",new int[]{1,2},"tick1"),expected);
            eq(s.target("hero").hp,100-expected);
            blocked(()->s.targetTurnStart("rug","hero","heroTurn1",
                new int[]{1,2},"duplicateTick"));
            eq(s.targetTurnStart("rug","hero","heroTurn2",
                new int[]{2,3},"tick2"),defense==Defense.IMMUNE?0:
                    defense==Defense.RESISTANT?4:
                    defense==Defense.VULNERABLE?16:
                    defense==Defense.BOTH?8:8);
            blocked(()->s.targetTurnStart("rug","hero","heroTurn3",
                new int[]{7,1},"badDie"));
            yes(s.release("rug",true,"escape"));
            blocked(()->s.targetTurnStart("rug","hero","heroTurn3",
                new int[]{1,1},"notGrappled"));
        }
    }
    private static void damageTransferAndDeath(){
        Session s=session(Size.MEDIUM,100,Defense.NORMAL);
        grapple(s,"start");
        DamageSplit split=s.rugDamaged("rug",9,"rugHit");
        eq(split.rugDamage,4);
        eq(split.grappledTargetDamage,4);
        eq(split.remainingRugHp,46);
        eq(split.remainingTargetHp,96);
        yes(split.transfersWhileGrappling);
        yes(!split.combatPersisted);
        blocked(()->s.rugDamaged("rug",9,"rugHit"));
        DamageSplit odd=s.rugDamaged("rug",1,"oddHit");
        eq(odd.rugDamage,0);
        eq(odd.grappledTargetDamage,0);
        DamageSplit fatal=s.rugDamaged("rug",100,"killedRug");
        eq(fatal.rugDamage,50);
        eq(fatal.grappledTargetDamage,50);
        eq(fatal.remainingRugHp,0);
        eq(fatal.remainingTargetHp,46);
        yes(s.grapple("rug")==null);
        yes(!s.canSmother("rug"));
        blocked(()->s.propose(req(s,"deadRugAttack",true,Choice.GRAPPLE)));

        Session alone=session(Size.MEDIUM,100,Defense.NORMAL);
        DamageSplit whole=alone.rugDamaged("rug",10,"outsideGrapple");
        eq(whole.rugDamage,10);
        eq(whole.grappledTargetDamage,0);
        yes(!whole.transfersWhileGrappling);
        eq(alone.target("hero").hp,100);
        Session death=session(Size.SMALL,100,Defense.NORMAL);
        grapple(death,"hold");
        death.rugDies("rug","externalDeath");
        yes(death.grapple("rug")==null);
        eq(death.target("rug").hp,0);
        blocked(()->death.rugDies("rug","externalDeath"));
    }
    private static void isolation(){
        Session s=session(Size.MEDIUM,100,Defense.NORMAL);
        Session alien=session(Size.MEDIUM,100,Defense.NORMAL);
        Proposal p=s.propose(req(s,"attack1",true,Choice.GRAPPLE));
        blocked(()->alien.commit(p));
        s.commit(p);
        blocked(()->s.commit(p));
        eq((int)s.revision(),1);
    }
    public static void main(String[] args){
        constants();
        choiceAndDamage();
        grappleStates();
        periodicDamage();
        damageTransferAndDeath();
        isolation();
        System.out.println("PASS explicit_effect_choice-01 "+checks+
            " independent Java assertions; Animated Rug of Smothering contract; Android/SQLite NOT integrated");
    }
}
