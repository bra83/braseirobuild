package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmSwallowAndEscape01.*;

/** JDK8-only source-specific regression tests, manual inputs throughout. */
public final class XmmSwallowAndEscape01Scenarios {
    private static int checks;
    private static void yes(boolean v){
        checks++;if(!v)throw new AssertionError("failed_assertion_"+checks);
    }
    private static void eq(int actual,int expected){yes(actual==expected);}
    private static void failClosed(Runnable test){
        checks++;
        try{test.run();}catch(IllegalArgumentException ok){return;}
        throw new AssertionError("expected_rejection_"+checks);
    }
    private static Session session(Size size,int hp,Defense defense){
        return new Session(Collections.singletonList(
            new Target("hero",Kind.CREATURE,size,hp,hp,defense)));
    }
    private static void grapple(Session s,Species a){
        Status g=s.registerBiteGrapple(a,"monster","hero",true,"bite1");
        yes(g.phase==Phase.GRAPPLED);
        yes(g.conditions.contains(Condition.GRAPPLED));
        yes(!g.conditions.contains(Condition.BLINDED));
    }
    private static Proposal swallow(Session s,Species a,int turn){
        return s.propose(new SwallowRequest(a,"monster","hero","swallow1",s.revision(),turn));
    }
    private static void sourceAndSize(){
        yes(Species.values().length==2);
        yes(Species.fromSource("/monster/182/action/1/entries/0")==Species.GIANT_FROG);
        yes(Species.fromSource("/monster/194/action/1/entries/0")==Species.GIANT_TOAD);
        failClosed(()->Species.fromSource("/monster/194/action/0/entries/0"));
        eq(Species.GIANT_FROG.acidDieCount,2);
        eq(Species.GIANT_TOAD.acidDieCount,3);
        eq(Species.GIANT_FROG.acidDieSides,4);
        eq(Species.GIANT_TOAD.acidDieSides,6);
        eq(Species.GIANT_FROG.biteToHit,3);
        eq(Species.GIANT_TOAD.biteToHit,4);
        eq(Species.GIANT_FROG.biteEscapeDc,11);
        eq(Species.GIANT_TOAD.biteEscapeDc,12);
        eq(Species.GIANT_FROG.swallowMaxSize,Size.SMALL.ordinal());
        eq(Species.GIANT_TOAD.swallowMaxSize,Size.MEDIUM.ordinal());
        for(Species a:Species.values()){
            for(Size size:Size.values()){
                Session s=session(size,100,Defense.NORMAL);
                if(size.ordinal()<=Size.MEDIUM.ordinal()){
                    grapple(s,a);
                    if(size.ordinal()<=a.swallowMaxSize){
                        Proposal p=swallow(s,a,3);
                        yes(p.status.phase==Phase.SWALLOWED);
                        yes(p.status.conditions.contains(Condition.BLINDED));
                        yes(p.status.conditions.contains(Condition.RESTRAINED));
                        yes(p.status.conditions.contains(Condition.TOTAL_COVER_AGAINST_OUTSIDE));
                        yes(!p.status.conditions.contains(Condition.GRAPPLED));
                        yes(s.status("hero").phase==Phase.GRAPPLED);
                        s.commit(p);
                        yes(!s.canBite("monster"));
                        eq(s.target("hero").hp,100);
                        yes(s.status("hero").phase==Phase.SWALLOWED);
                        failClosed(()->s.commit(p));
                        failClosed(()->swallow(s,a,3));
                        failClosed(()->s.registerBiteGrapple(a,"monster","hero",
                            true,"bite2"));
                    }else {
                        failClosed(()->swallow(s,a,3));
                        yes(s.canBite("monster"));
                    }
                }else {
                    failClosed(()->s.registerBiteGrapple(a,"monster","hero",true,"bite1"));
                }
            }
        }
    }
    private static void frogDigestion(){
        Session frog=session(Size.SMALL,20,Defense.NORMAL);
        grapple(frog,Species.GIANT_FROG);
        frog.commit(swallow(frog,Species.GIANT_FROG,10));
        failClosed(()->frog.endActorTurn("monster","hero",10,new int[]{1,1},"tooEarly"));
        failClosed(()->frog.endActorTurn("monster","hero",12,new int[]{1,1},"tooLate"));
        failClosed(()->frog.endActorTurn("monster","hero",11,new int[]{1,5},"invalidDie"));
        Tick tick=frog.endActorTurn("monster","hero",11,new int[]{1,2},"end11");
        eq(tick.rolledAcid,3);
        eq(tick.acidDamage,3);
        eq(tick.targetHp,17);
        yes(tick.disgorgedProne);
        yes(!tick.killedByAcid);
        yes(!tick.combatPersisted);
        yes(tick.status.phase==Phase.DISGORGED_PRONE);
        yes(tick.status.conditions.contains(Condition.PRONE));
        yes(!tick.status.conditions.contains(Condition.RESTRAINED));
        yes(frog.canBite("monster"));
        failClosed(()->frog.endActorTurn("monster","hero",12,new int[]{1,1},"repeat"));

        Session weak=session(Size.TINY,1,Defense.NORMAL);
        grapple(weak,Species.GIANT_FROG);
        weak.commit(swallow(weak,Species.GIANT_FROG,4));
        Tick death=weak.endActorTurn("monster","hero",5,new int[]{1,1},"frogKills");
        yes(death.killedByAcid);
        yes(!death.disgorgedProne);
        eq(death.targetHp,0);
        yes(death.status.phase==Phase.SWALLOWED);
        yes(!weak.canBite("monster"));
        failClosed(()->weak.endActorTurn("monster","hero",6,new int[]{1,1},"repeatDigestion"));

        Session immune=session(Size.SMALL,9,Defense.IMMUNE);
        grapple(immune,Species.GIANT_FROG);
        immune.commit(swallow(immune,Species.GIANT_FROG,7));
        Tick harmless=immune.endActorTurn("monster","hero",8,new int[]{4,4},"immuneAcid");
        eq(harmless.rolledAcid,8);
        eq(harmless.acidDamage,0);
        eq(harmless.targetHp,9);
        yes(harmless.disgorgedProne);
        Session resistant=session(Size.SMALL,9,Defense.RESISTANT);
        grapple(resistant,Species.GIANT_FROG);
        resistant.commit(swallow(resistant,Species.GIANT_FROG,1));
        eq(resistant.endActorTurn("monster","hero",2,new int[]{3,4},"resist").acidDamage,3);
    }
    private static void toadDigestion(){
        Session s=session(Size.MEDIUM,60,Defense.NORMAL);
        grapple(s,Species.GIANT_TOAD);
        s.commit(swallow(s,Species.GIANT_TOAD,20));
        Tick sameTurn=s.endActorTurn("monster","hero",20,new int[]{1,1,1},"toad20");
        eq(sameTurn.acidDamage,3);
        eq(sameTurn.targetHp,57);
        yes(!sameTurn.disgorgedProne);
        yes(sameTurn.status.phase==Phase.SWALLOWED);
        yes(!s.canBite("monster"));
        failClosed(()->s.endActorTurn("monster","hero",20,new int[]{1,1,1},"duplicatedTurn"));
        Tick next=s.endActorTurn("monster","hero",21,new int[]{2,3,4},"toad21");
        eq(next.acidDamage,9);
        eq(next.targetHp,48);
        yes(next.status.phase==Phase.SWALLOWED);
        failClosed(()->s.endActorTurn("monster","hero",22,new int[]{1,1},"missingThirdD6"));
        eq(s.endActorTurn("monster","hero",22,new int[]{6,6,6},"toad22").acidDamage,18);
        eq(s.target("hero").hp,30);
        failClosed(()->s.registerBiteGrapple(Species.GIANT_TOAD,"monster","hero",
            true,"attackWhenSwallowing"));
        Session victim=session(Size.MEDIUM,1,Defense.NORMAL);
        grapple(victim,Species.GIANT_TOAD);
        victim.commit(swallow(victim,Species.GIANT_TOAD,2));
        Tick died=victim.endActorTurn("monster","hero",2,new int[]{1,1,1},"died");
        yes(died.killedByAcid);
        yes(died.status.phase==Phase.SWALLOWED);
        yes(!victim.canBite("monster"));
    }
    private static void corpseEscape(){
        for(Species a:Species.values()){
            Session s=session(Size.SMALL,12,Defense.NORMAL);
            grapple(s,a);
            s.commit(swallow(s,a,2));
            s.actorDies("monster","death");
            Status st=s.status("hero");
            yes(st.phase==Phase.CORPSE_EXIT_READY);
            yes(!st.conditions.contains(Condition.RESTRAINED));
            yes(st.conditions.contains(Condition.MUST_USE_5FT_MOVEMENT_TO_EXIT_CORPSE));
            yes(st.conditions.contains(Condition.BLINDED));
            failClosed(()->s.exitDeadSwallower("monster","hero",4,"tooLittle"));
            failClosed(()->s.exitDeadSwallower("monster","hero",6,"tooMuch"));
            failClosed(()->s.endActorTurn("monster","hero",3,new int[]{1,1},"deadCannotTick"));
            st=s.exitDeadSwallower("monster","hero",5,"exit");
            yes(st.phase==Phase.EXITED_PRONE);
            yes(st.conditions.contains(Condition.PRONE));
            yes(!st.conditions.contains(Condition.BLINDED));
            yes(!st.conditions.contains(Condition.TOTAL_COVER_AGAINST_OUTSIDE));
            failClosed(()->s.exitDeadSwallower("monster","hero",5,"exitAgain"));
            yes(!s.canBite("monster"));
            failClosed(()->s.actorDies("monster","secondDeath"));
        }
        Session justGrappled=session(Size.MEDIUM,10,Defense.NORMAL);
        grapple(justGrappled,Species.GIANT_FROG);
        justGrappled.actorDies("monster","deadBeforeSwallow");
        yes(justGrappled.status("hero")==null);
    }
    private static void failClosedAndReplay(){
        Session s=session(Size.SMALL,30,Defense.NORMAL);
        failClosed(()->swallow(s,Species.GIANT_FROG,1));
        failClosed(()->s.registerBiteGrapple(Species.GIANT_FROG,"monster",
            "hero",false,"unconfirmed"));
        grapple(s,Species.GIANT_FROG);
        failClosed(()->s.registerBiteGrapple(Species.GIANT_FROG,"monster",
            "hero",true,"bite1"));
        Proposal p=swallow(s,Species.GIANT_FROG,1);
        Session other=session(Size.SMALL,30,Defense.NORMAL);
        failClosed(()->other.commit(p));
        s.commit(p);
        failClosed(()->s.commit(p));
        failClosed(()->s.propose(new SwallowRequest(Species.GIANT_FROG,"monster",
            "hero","fresh",0,1)));
        eq((int)s.revision(),2);
    }
    public static void main(String[] args){
        sourceAndSize();
        frogDigestion();
        toadDigestion();
        corpseEscape();
        failClosedAndReplay();
        System.out.println("PASS swallow_and_escape-01 "+checks+
            " independent Java assertions; 2 official XMM contracts; Android/SQLite NOT integrated");
    }
}
