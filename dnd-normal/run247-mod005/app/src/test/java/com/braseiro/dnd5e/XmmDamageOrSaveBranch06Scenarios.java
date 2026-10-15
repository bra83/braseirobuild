package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmDamageOrSaveBranch06.*;

/** JVM-only tests for one source-pinned XMM Wraith attack. */
public final class XmmDamageOrSaveBranch06Scenarios {
    private static int checks;
    private static void ok(boolean value) {
        checks++;if(!value)throw new AssertionError("failed_check_"+checks);
    }
    private static void equal(int actual,int expected){ok(actual==expected);}
    private static void rejects(Runnable call) {
        checks++;
        try{call.run();}catch(IllegalArgumentException expected){return;}
        throw new AssertionError("expected_rejection_"+checks);
    }
    private static Target target(Kind kind,int hp,int max,Defense defense) {
        return new Target("hero",kind,hp,max,defense);
    }
    private static Session session(Kind kind,int hp,int max,Defense defense) {
        return new Session(Collections.singletonList(target(kind,hp,max,defense)));
    }
    private static Request request(Session session,String event,Boolean hit,int... dice) {
        return new Request(SOURCE_POINTER,"wraith","hero",event,
            session.revision(),ATTACK_REACH_FEET,hit,dice);
    }
    private static void damagesAndDefenses() {
        ok(SOURCE_POINTER.equals("/monster/481/action/0/entries/0"));
        equal(ATTACK_TO_HIT,6);
        equal(ATTACK_REACH_FEET,5);
        ok("necrotic".equals(DAMAGE_TYPE));
        ok("4d8 + 3".equals(DAMAGE_FORMULA));
        for(Defense d:Defense.values()) {
            Session s=session(Kind.CREATURE,100,100,d);
            Proposal p=s.propose(request(s,"evt-"+d,Boolean.TRUE,1,2,3,4));
            equal(p.result.rolledDamage,13);
            int damage=d==Defense.IMMUNE?0:
                d==Defense.RESISTANT?6:
                d==Defense.VULNERABLE?26:
                d==Defense.BOTH?12:13;
            equal(p.result.damageTaken,damage);
            equal(p.result.maxHpLost,damage);
            equal(p.result.hp,100-damage);
            equal(p.result.maxHp,100-damage);
            ok(p.result.attackHit);
            ok(!p.result.persistedToCombat);
            equal(s.target("hero").hp,100); // proposal does NOT mutate
            Result committed=s.commit(p);
            equal(committed.damageTaken,damage);
            equal(s.target("hero").hp,100-damage);
            equal(s.target("hero").maxHp,100-damage);
            equal((int)s.revision(),1);
            rejects(()->s.commit(p));
            rejects(()->s.propose(request(s,"evt-"+d,Boolean.TRUE,1,2,3,4)));
        }
    }
    private static void missesObjectsAndDeath() {
        Session miss=session(Kind.CREATURE,25,25,Defense.NORMAL);
        Proposal p=miss.propose(request(miss,"miss",Boolean.FALSE));
        equal(p.result.damageTaken,0);
        equal(p.result.maxHpLost,0);
        miss.commit(p);
        equal(miss.target("hero").hp,25);
        equal(miss.target("hero").maxHp,25);

        Session object=session(Kind.OBJECT,25,25,Defense.NORMAL);
        Result hit=object.commit(object.propose(request(object,"object",Boolean.TRUE,1,1,1,1)));
        equal(hit.damageTaken,7);
        equal(hit.maxHpLost,0);
        equal(object.target("hero").hp,18);
        equal(object.target("hero").maxHp,25);

        Session dying=session(Kind.CREATURE,3,3,Defense.NORMAL);
        Result kill=dying.commit(dying.propose(request(dying,"kill",Boolean.TRUE,8,8,8,8)));
        equal(kill.rolledDamage,35);
        equal(kill.damageTaken,35);
        equal(kill.hp,0);
        equal(kill.maxHp,0);
        equal(kill.maxHpLost,35);
        ok(!kill.persistedToCombat);
        // Create Specter is an OTHER action, not part of Life Drain;
        // this engine has no creature creation or timed specter spawning.
    }
    private static void invalidAndReplay() {
        Session s=session(Kind.CREATURE,100,100,Defense.NORMAL);
        rejects(()->s.propose(new Request("/wrong","wraith","hero","ptr",0,5,true,new int[]{1,1,1,1})));
        rejects(()->s.propose(new Request(SOURCE_POINTER,"wraith","hero","range",0,6,true,new int[]{1,1,1,1})));
        rejects(()->s.propose(new Request(SOURCE_POINTER,"wraith","hero","null-hit",0,5,null,new int[0])));
        rejects(()->s.propose(new Request(SOURCE_POINTER,"wraith","hero","miss-rolled",0,5,false,new int[]{1,1,1,1})));
        rejects(()->s.propose(new Request(SOURCE_POINTER,"wraith","hero","few-dice",0,5,true,new int[]{1,1,1})));
        rejects(()->s.propose(new Request(SOURCE_POINTER,"wraith","hero","bad-die",0,5,true,new int[]{1,1,1,9})));
        rejects(()->s.propose(new Request(SOURCE_POINTER,"wraith","ghost","bad-target",0,5,true,new int[]{1,1,1,1})));
        Session t=session(Kind.CREATURE,100,100,Defense.NORMAL);
        Proposal p=s.propose(request(s,"good",Boolean.TRUE,1,1,1,1));
        rejects(()->t.commit(p));
        s.commit(p);
        rejects(()->s.commit(p));
        rejects(()->s.propose(new Request(SOURCE_POINTER,"wraith","hero","new-old-revision",0,5,true,new int[]{1,1,1,1})));
        Proposal fresh=s.propose(request(s,"next",Boolean.FALSE));
        s.commit(fresh);
        equal((int)s.revision(),2);
    }
    public static void main(String[] args) {
        damagesAndDefenses();
        missesObjectsAndDeath();
        invalidAndReplay();
        System.out.println("PASS branch-06 "+checks+
            " JVM assertions; Wraith Life Drain core; Android/SQLite NOT integrated");
    }
}
