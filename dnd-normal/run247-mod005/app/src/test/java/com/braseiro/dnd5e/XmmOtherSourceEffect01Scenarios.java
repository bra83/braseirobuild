package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmOtherSourceEffect01.*;

/** Runnable JDK 8 tests, independent of Android, Gradle, Gemini and Codex. */
public final class XmmOtherSourceEffect01Scenarios {
    private static int checks;
    private static void yes(boolean ok) {checks++;if(!ok)throw new AssertionError("check_"+checks);}
    private static void equal(int actual,int expected){yes(actual==expected);}
    private static void rejects(Runnable f) {
        checks++;
        try{f.run();}catch(IllegalArgumentException expected){return;}
        throw new AssertionError("expected_rejection_"+checks);
    }
    private static Map<Type,Defense> defs(Defense d) {
        EnumMap<Type,Defense> result=new EnumMap<>(Type.class);
        for(Type t:Type.values())result.put(t,d);
        return result;
    }
    private static Session session(Kind kind,int hp,Defense d) {
        return new Session(Collections.singletonList(new Target(
            "hero",kind,hp,hp,false,kind==Kind.OBJECT,defs(d))));
    }
    private static boolean has(Result r,Rider rider) {
        for(Effect fx:r.effects)if(fx.rider==rider)return true;
        return false;
    }
    private static Effect effect(Result r,Rider rider){
        for(Effect fx:r.effects)if(fx.rider==rider)return fx;
        throw new AssertionError("missing_rider_"+rider);
    }
    private static int[] low(RayRule ray){
        int[] result=new int[ray.count];Arrays.fill(result,1);
        return result;
    }
    private static Request req(Session s,Action a,String event,String turn,
            int dist,int ray,Boolean saved,int[] dice,boolean inCone,
            boolean canReact,boolean recharge,String ally,int allyDist,boolean allyReaction,
            boolean allyAccepts,boolean actorMoves,int move){
        return new Request(a,"monster","hero",event,turn,s.revision(),dist,ray,saved,dice,
            true,inCone,canReact,recharge,ally,allyDist,allyReaction,allyAccepts,actorMoves,
            move,false);
    }
    private static Request breath(Session s,Action a,String event,boolean saved) {
        return req(s,a,event,"turn1",a.rangeFt,0,saved,new int[0],true,
            false,false,null,0,false,false,false,0);
    }
    private static Request ray(Session s,Action a,String event,String turn,
            int idx,boolean saved,int[] dice,int move){
        return req(s,a,event,turn,120,idx,saved,dice,false,
            false,false,null,0,false,false,false,move);
    }
    private static void exactContracts(){
        yes(Action.values().length==10);
        Set<String> pointers=new HashSet<>();
        for(Action a:Action.values()){
            yes(pointers.add(a.pointer));
            yes(Action.source(a.pointer)==a);
            yes(a.pointer.startsWith("/monster/"));
        }
        rejects(()->Action.source("/monster/0/action/0/entries/0"));
        equal(Action.ADULT_COPPER.dc,18);equal(Action.ADULT_GOLD.dc,21);
        equal(Action.ANCIENT_COPPER.dc,22);equal(Action.ANCIENT_GOLD.dc,24);
        equal(Action.COPPER_WYRMLING.dc,11);
        equal(Action.BEHOLDER.dc,16);equal(Action.BEHOLDER_ZOMBIE.dc,14);
        equal(Action.DEATH_TYRANT.dc,17);
        yes(Action.BEHOLDER.ray()&&Action.DEATH_TYRANT.ray());
        yes(Action.BEHOLDER_ZOMBIE.ray());
        equal(Action.ADULT_COPPER.rangeFt,60);
        equal(Action.ANCIENT_COPPER.rangeFt,90);
        equal(Action.COPPER_WYRMLING.rangeFt,15);
        equal(Action.BEHOLDER.rangeFt,120);
        equal(rayRule(Action.BEHOLDER,9).count,8);
        equal(rayRule(Action.BEHOLDER_ZOMBIE,4).sides,10);
        equal(rayRule(Action.DEATH_TYRANT,5).sides,10);
        yes(rayRule(Action.BEHOLDER_ZOMBIE,2).ray==Ray.FEAR);
        rejects(()->rayRule(Action.BEHOLDER,11));
        rejects(()->rayRule(Action.BEHOLDER_ZOMBIE,5));
        rejects(()->rayRule(Action.ADULT_GOLD,1));
    }
    private static void dragonBreaths(){
        for(Action a:Action.values()){
            if(!a.slowing()&&!a.weakening())continue;
            Session s=session(Kind.CREATURE,100,Defense.NORMAL);
            Request request=breath(s,a,"breath-"+a.name(),false);
            Proposal p=s.propose(request);
            equal(p.result.damage,0);
            equal(s.target("hero").hp,100);
            yes(p.result.selectedRay==null);
            yes(!p.result.persistedInAndroid);
            if(a.slowing()){
                yes(has(p.result,Rider.SLOWING_BREATH_NO_REACTION_HALF_SPEED_ACTION_OR_BONUS));
                yes(!has(p.result,Rider.WEAKENING_BREATH_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_DIE));
            }else{
                yes(has(p.result,Rider.WEAKENING_BREATH_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_DIE));
                equal(effect(p.result,Rider.WEAKENING_BREATH_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_DIE)
                    .magnitude,a==Action.ANCIENT_GOLD?10:6);
                yes(has(p.result,Rider.REPEAT_SAVE_EACH_TURN_UNTIL_ONE_MINUTE));
            }
            s.execute(p);
            equal((int)s.revision(),1);
            rejects(()->s.execute(p));
            rejects(()->s.propose(request));
            if(a.weakening()){
                Request repeat=breath(s,a,"new-breath-"+a.name(),false);
                rejects(()->s.propose(repeat));
            }
            Session saved=session(Kind.CREATURE,100,Defense.NORMAL);
            Result success=saved.propose(breath(saved,a,"success",true)).result;
            yes(success.effects.isEmpty());
            Session tooFar=session(Kind.CREATURE,100,Defense.NORMAL);
            Request outside=req(tooFar,a,"outside","turn1",a.rangeFt+1,0,false,
                new int[0],true,false,false,null,0,false,false,false,0);
            rejects(()->tooFar.propose(outside));
        }
    }
    private static void eyeRays(){
        Action[] eyes={Action.BEHOLDER,Action.BEHOLDER_ZOMBIE,Action.DEATH_TYRANT};
        for(Action action:eyes){
            int count=action==Action.BEHOLDER_ZOMBIE?4:10;
            for(int i=1;i<=count;i++){
                RayRule rule=rayRule(action,i);
                Session s=session(Kind.CREATURE,200,Defense.NORMAL);
                Request failed=ray(s,action,"r-"+i,"turn1",i,false,low(rule),0);
                Proposal p=s.propose(failed);
                equal(s.target("hero").hp,200);
                equal(p.result.damage,rule.count);
                yes(p.result.selectedRay==rule.ray);
                yes(!p.result.persistedInAndroid);
                s.execute(p);
                equal(s.target("hero").hp,200-rule.count);
                rejects(()->s.execute(p));
                // New attack in the SAME turn must reroll the random die
                // if the numbered ray was already used.
                Request repeat=ray(s,action,"repeat-"+i,"turn1",i,true,low(rule),0);
                rejects(()->s.propose(repeat));
                // A different turn resets ray-use eligibility via the
                // explicitly supplied turn identity, not wall-clock time.
                Request nextTurn=ray(s,action,"next-"+i,"turn2",i,true,low(rule),0);
                Result saved=s.propose(nextTurn).result;
                equal(saved.damage,rule.count>0?
                    (rule.halfOnSave?rule.count/2:0):0);
                yes(saved.effects.isEmpty());
            }
        }
        Session s=session(Kind.CREATURE,100,Defense.NORMAL);
        Result charm=s.propose(ray(s,Action.BEHOLDER,"charm","turn1",1,false,new int[]{1,1,1},0)).result;
        yes(has(charm,Rider.CHARMED_UNTIL_DAMAGE_OR_ONE_HOUR));
        Result paralysis=s.propose(ray(s,Action.BEHOLDER,"paralysis","turn1",2,false,new int[0],0)).result;
        yes(has(paralysis,Rider.PARALYZED_REPEAT_SAVE_UNTIL_ONE_MINUTE));
        Result petrify=s.propose(ray(s,Action.BEHOLDER,"petrify","turn1",8,false,new int[0],0)).result;
        yes(has(petrify,Rider.RESTRAINED_PETRIFY_FIRST_SAVE));
        yes(has(petrify,Rider.PETRIFY_SECOND_SAVE_END_NEXT_TURN));
        Session immune=session(Kind.CREATURE,100,Defense.IMMUNE);
        equal(immune.propose(ray(immune,Action.BEHOLDER,"immune","turn1",9,false,
            new int[]{1,1,1,1,1,1,1,1},0)).result.damage,0);
        Session both=session(Kind.CREATURE,100,Defense.BOTH);
        equal(both.propose(ray(both,Action.BEHOLDER,"both","turn1",9,false,
            new int[]{1,1,1,1,1,1,1,1},0)).result.damage,8);
        Session tele=session(Kind.CREATURE,100,Defense.NORMAL);
        Result moved=tele.propose(ray(tele,Action.BEHOLDER,"move","turn1",6,false,new int[0],30)).result;
        equal(effect(moved,Rider.TELEKINETIC_MOVE_UP_TO_30FT_RESTRAINED).magnitude,30);
        rejects(()->tele.propose(ray(tele,Action.BEHOLDER,"too-far","turn1",6,false,new int[0],31)));
        Session autoCon=new Session(Collections.singletonList(
            new Target("hero",Kind.CONSTRUCT,100,100,false,false,defs(Defense.NORMAL))));
        rejects(()->autoCon.propose(ray(autoCon,Action.BEHOLDER,"sleep","turn1",7,false,new int[0],0)));
        Result sleeping=autoCon.propose(ray(autoCon,Action.BEHOLDER,"save","turn1",7,true,new int[0],0)).result;
        yes(sleeping.effects.isEmpty());
        Session object=session(Kind.OBJECT,10,Defense.NORMAL);
        Result dust=object.propose(ray(object,Action.BEHOLDER,"dis","turn1",9,false,
            new int[]{1,1,1,1,1,1,1,1},0)).result;
        yes(has(dust,Rider.DISINTEGRATE_OBJECT_CUBE_10FT));
        Session creature=session(Kind.CREATURE,1,Defense.NORMAL);
        Result death=creature.propose(ray(creature,Action.BEHOLDER,"death","turn1",10,false,
            new int[]{1,1,1,1,1,1,1,1,1,1},0)).result;
        yes(has(death,Rider.DEATH_RAY_KILLS_AT_ZERO_HP));
    }
    private static void reactionsAndMovement(){
        Session commander=session(Kind.CREATURE,100,Defense.NORMAL);
        Request rush=req(commander,Action.BERSERKER_COMMANDER,"rush","turn1",
            0,0,null,new int[0],false,false,false,"ally1",20,true,true,true,0);
        Result r=commander.execute(commander.propose(rush));
        yes(has(r,Rider.ALLY_REACTION_HALF_MOVE_NO_OPPORTUNITY_ATTACK));
        yes(has(r,Rider.ACTOR_HALF_MOVE_NO_OPPORTUNITY_ATTACK));
        yes(effect(r,Rider.ALLY_REACTION_HALF_MOVE_NO_OPPORTUNITY_ATTACK).target.equals("ally1"));
        equal(r.damage,0);
        Session invalid=session(Kind.CREATURE,100,Defense.NORMAL);
        Request far=req(invalid,Action.BERSERKER_COMMANDER,"far","turn1",
            0,0,null,new int[0],false,false,false,"ally1",31,true,true,false,0);
        rejects(()->invalid.propose(far));
        Session cyclops=session(Kind.CREATURE,100,Defense.NORMAL);
        Request foresight=req(cyclops,Action.CYCLOPS_SENTRY,"reaction","turn1",
            0,0,null,new int[0],false,true,true,null,0,false,false,false,0);
        Result applied=cyclops.execute(cyclops.propose(foresight));
        yes(has(applied,Rider.ATTACKER_DISADVANTAGE_CURRENT_ROLL));
        yes(has(applied,Rider.CYCLOPS_ADVANTAGE_VS_ATTACKER_NEXT_TURN));
        yes(has(applied,Rider.RECHARGE_SIX_REQUIRED));
        Session noRecharge=session(Kind.CREATURE,100,Defense.NORMAL);
        Request blocked=req(noRecharge,Action.CYCLOPS_SENTRY,"no-recharge","turn1",
            0,0,null,new int[0],false,true,false,null,0,false,false,false,0);
        rejects(()->noRecharge.propose(blocked));
    }
    public static void main(String[] args){
        exactContracts();
        dragonBreaths();
        eyeRays();
        reactionsAndMovement();
        System.out.println("PASS other_source_effect-01 "+checks+
            " independent Java assertions; 10 contracts; Android/SQLite not integrated");
    }
}
