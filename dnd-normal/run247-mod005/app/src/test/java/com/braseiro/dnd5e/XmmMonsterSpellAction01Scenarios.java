package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmMonsterSpellAction01.*;

/** Pure Java 8 source-specific regression suite for Blue Slaad Mutating Claw. */
public final class XmmMonsterSpellAction01Scenarios {
    private static int assertions;
    private static void yes(boolean b){assertions++;if(!b)throw new AssertionError("check_"+assertions);}
    private static void eq(int got,int expected){yes(got==expected);}
    private static void rejects(Runnable action){
        assertions++;
        try{action.run();}catch(IllegalArgumentException correct){return;}
        throw new AssertionError("expected_rejection_"+assertions);
    }
    private static Map<Type,Defense> defenses(Defense selected) {
        EnumMap<Type,Defense> d=new EnumMap<>(Type.class);
        for(Type t:Type.values())d.put(t,selected);
        return d;
    }
    private static Session session(Kind kind,int hp,int max,boolean casts,Defense defense) {
        return new Session(Collections.singletonList(new Target(
            "hero",kind,hp,max,casts,defenses(defense))));
    }
    private static Request attack(Session s,String event,Boolean hit,Boolean save,
            int[] slash,int[] poison){
        return new Request("blueSlaad","hero",event,s.revision(),10,hit,save,
            slash,poison);
    }
    private static Request attack(Session s,String event,Boolean save) {
        return attack(s,event,true,save,new int[]{1,1},new int[]{1});
    }
    private static boolean has(List<Effect> effects,Rider rider){
        for(Effect e:effects)if(e.rider==rider)return true;
        return false;
    }
    private static void constantsAndDefenses() {
        yes(SOURCE_POINTER.equals("/monster/71/action/1/entries/0"));
        yes(ONLY_REVERSAL_SPELL.equals("Wish"));
        eq(TO_HIT,8);eq(REACH_FEET,10);eq(CON_SAVE_DC,15);
        for(Defense defense:Defense.values()) {
            Session s=session(Kind.HUMANOID,100,100,false,defense);
            Proposal p=s.propose(attack(s,"def-"+defense,true));
            int slash=defense==Defense.IMMUNE?0:
                defense==Defense.RESISTANT?3:
                defense==Defense.VULNERABLE?14:
                defense==Defense.BOTH?6:7;
            int poison=defense==Defense.IMMUNE?0:
                defense==Defense.RESISTANT?0:
                defense==Defense.VULNERABLE?2:
                defense==Defense.BOTH?0:1;
            eq(p.result.slashingDamage,slash);
            eq(p.result.poisonDamage,poison);
            eq(p.result.totalDamage,slash+poison);
            yes(!p.result.createsCurse);
            yes(!p.result.integratedIntoCombat);
            eq(s.target("hero").hp,100);
            s.commit(p);
            eq(s.target("hero").hp,100-slash-poison);
            yes(s.state("hero")==State.NORMAL);
            yes(s.canRegainHp("hero"));
            rejects(()->s.commit(p));
        }
    }
    private static void eligibilityAndCurse() {
        Session s=session(Kind.HUMANOID,100,100,false,Defense.NORMAL);
        Proposal miss=s.propose(attack(s,"miss",false,null,new int[0],new int[0]));
        eq(miss.result.totalDamage,0);
        yes(!miss.result.createsCurse);
        rejects(()->s.propose(attack(s,"miss-save",false,false,new int[0],new int[0])));
        rejects(()->s.propose(attack(s,"missing-save",true,null,new int[]{1,1},new int[]{1})));
        rejects(()->s.propose(attack(s,"bad-die",true,false,new int[]{7,1},new int[]{1})));
        rejects(()->s.propose(new Request("blueSlaad","hero","out-of-reach",0,11,
            true,false,new int[]{1,1},new int[]{1})));
        Session monster=session(Kind.CREATURE,80,80,false,Defense.NORMAL);
        yes(!monster.propose(attack(monster,"creature-no-save",null)).result.createsCurse);
        rejects(()->monster.propose(attack(monster,"wrong-save",false)));
        Session object=session(Kind.OBJECT,80,80,false,Defense.NORMAL);
        yes(!object.propose(attack(object,"object-no-save",null)).result.createsCurse);

        Proposal initial=s.propose(attack(s,"infect",false));
        eq(initial.result.totalDamage,8);
        yes(initial.result.createsCurse);
        yes(has(initial.result.pendingEffects,Rider.SLAAD_CURSE_BLOCKS_HEALING));
        yes(has(initial.result.pendingEffects,Rider.MAX_HP_MINUS_3D6_PER_24H));
        yes(has(initial.result.pendingEffects,Rider.MAX_HP_NOT_RESTORED_AFTER_LONG_REST));
        eq(s.target("hero").hp,100);
        s.commit(initial);
        eq(s.target("hero").hp,92);
        yes(s.state("hero")==State.CURSED);
        yes(!s.canRegainHp("hero"));
        yes(s.curse("hero")!=null);
        yes(!s.propose(attack(s,"second-hit",null)).result.createsCurse);
        rejects(()->s.propose(attack(s,"already-cursed-save",true)));
        rejects(()->s.curse24h("hero",1,false,new int[]{1,1,1},"not24h"));
        rejects(()->s.curse24h("hero",2,true,new int[]{1,1,1},"skip24h"));
        rejects(()->s.curse24h("hero",1,true,new int[]{1,1},"bad-rolls"));
        rejects(()->s.curse24h("hero",1,true,new int[]{1,1,7},"bad-die"));
        Tick tick=s.curse24h("hero",1,true,new int[]{1,2,3},"tick24h-1");
        eq(tick.loss,6);
        eq(tick.maxHp,94);
        eq(tick.hp,92);
        eq(tick.tickIndex,1);
        yes(tick.newState==State.CURSED);
        yes(has(tick.effects,Rider.MAX_HP_MINUS_3D6_PER_24H));
        yes(!tick.integratedIntoCombat);
        eq(s.curse("hero").lastTickIndex,1);
        rejects(()->s.curse24h("hero",1,true,new int[]{1,1,1},"repeatIndex"));
        rejects(()->s.curse24h("hero",2,true,new int[]{1,1,1},"tick24h-1"));
        yes(s.removeCurse("hero",true,"removeExternal"));
        yes(s.canRegainHp("hero"));
        yes(s.curse("hero")==null);
        yes(s.state("hero")==State.NORMAL);
        rejects(()->s.removeCurse("hero",true,"removeTwice"));
        rejects(()->s.curse24h("hero",2,true,new int[]{1,1,1},"postRemoval"));
    }
    private static void transformations() {
        for(boolean canCast:new boolean[]{false,true}) {
            Session s=session(Kind.HUMANOID,10,10,canCast,Defense.IMMUNE);
            Result hit=s.commit(s.propose(attack(s,"infect",false)));
            eq(hit.totalDamage,0);
            yes(hit.createsCurse);
            Tick first=s.curse24h("hero",1,true,new int[]{1,1,1},"oneDay");
            eq(first.loss,3);
            eq(first.maxHp,7);
            yes(first.newState==State.CURSED);
            Tick next=s.curse24h("hero",2,true,new int[]{3,3,3},"secondDay");
            eq(next.maxHp,0);
            eq(next.hp,0);
            yes(s.curse("hero")==null);
            State expected=canCast?State.PENDING_GREEN_SLAAD:State.PENDING_RED_SLAAD;
            yes(next.newState==expected);
            yes(s.state("hero")==expected);
            yes(has(next.effects,Rider.CURSE_ENDS_AT_ZERO_MAX_HP));
            yes(has(next.effects,canCast?
                Rider.REPLACE_STAT_BLOCK_WITH_GREEN_SLAAD:
                Rider.REPLACE_STAT_BLOCK_WITH_RED_SLAAD));
            yes(has(next.effects,Rider.ONLY_WISH_REVERSES_SLAAD_TRANSFORMATION));
            rejects(()->s.propose(attack(s,"cannot-attack-pending",null)));
            rejects(()->s.curse24h("hero",3,true,new int[]{1,1,1},"noMoreCurse"));
            rejects(()->s.canRegainHp("hero"));
            rejects(()->s.removeCurse("hero",true,"notAfterTransform"));
            rejects(()->s.verifiedWishReversal("hero",false,"unverifiedWish"));
            s.verifiedWishReversal("hero",true,"wishFromHost");
            yes(s.state("hero")==State.WISH_REVERSAL_PENDING);
            rejects(()->s.verifiedWishReversal("hero",true,"secondWish"));
            rejects(()->s.canRegainHp("hero"));
        }
    }
    private static void crossSession() {
        Session a=session(Kind.HUMANOID,100,100,false,Defense.NORMAL);
        Session b=session(Kind.HUMANOID,100,100,false,Defense.NORMAL);
        Proposal p=a.propose(attack(a,"one",false));
        rejects(()->b.commit(p));
        a.commit(p);
        rejects(()->a.commit(p));
        eq((int)a.revision(),1);
        rejects(()->a.propose(new Request("blueSlaad","hero","stale",0,10,
            true,false,new int[]{1,1},new int[]{1})));
    }
    public static void main(String[] args) {
        constantsAndDefenses();
        eligibilityAndCurse();
        transformations();
        crossSession();
        System.out.println("PASS monster_spell_action-01 "+assertions+
            " independent Java assertions; Blue Slaad Mutating Claw; Wish NOT cast; Android/SQLite NOT integrated");
    }
}
