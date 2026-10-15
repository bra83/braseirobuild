package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmOtherSourceEffect02.*;

/** Java 8 standalone tests; all dice and target choices supplied by the caller. */
public final class XmmOtherSourceEffect02Scenarios {
    private static int checks;
    private static void ok(boolean value){checks++;if(!value)throw new AssertionError("check_"+checks);}
    private static void eq(int actual,int expected){ok(actual==expected);}
    private static void blocked(Runnable call){
        checks++;
        try{call.run();}catch(IllegalArgumentException expected){return;}
        throw new AssertionError("expected_rejection_"+checks);
    }
    private static Map<Type,Defense> defs(Defense defense){
        Map<Type,Defense> out=new EnumMap<>(Type.class);
        for(Type t:Type.values())out.put(t,defense);
        return out;
    }
    private static Target target(String id,Kind kind,int hp,int tempHp){
        return new Target(id,kind,hp,hp,tempHp,defs(Defense.NORMAL));
    }
    private static Session session(Target... targets){
        return new Session(Arrays.asList(targets));
    }
    private static Selection choice(String id,int dist,Boolean save,int[] primary,int[] secondary){
        return new Selection(id,dist,true,true,true,true,false,false,false,save,
            primary,secondary);
    }
    private static Selection simple(String id,int dist){
        return choice(id,dist,null,null,null);
    }
    private static Selection special(String id,int dist,boolean visibility,boolean area,
            boolean entered,boolean entirely,boolean corpse,boolean rampage,
            boolean accept,Boolean saved,int[] diceA,int[] diceB){
        return new Selection(id,dist,visibility,area,entered,entirely,corpse,rampage,
            accept,saved,diceA,diceB);
    }
    private static Request request(Session s,Action a,String event,String turn,
            int variant,Selection... selected){
        return new Request(a,"actor",event,turn,s.revision(),Arrays.asList(selected),
            variant,30,10,0,null,1,1,true,true,true,true,true,true,true,true);
    }
    private static Request full(Session s,Action a,String event,String turn,
            int variant,int point,int toTarget,int incoming,Type trigger,int warA,int warB,
            boolean visible,boolean grounded,boolean reaction,boolean recharge,
            boolean destination,boolean ended,boolean attack,boolean daily,Selection... selected){
        return new Request(a,"actor",event,turn,s.revision(),Arrays.asList(selected),
            variant,point,toTarget,incoming,trigger,warA,warB,visible,grounded,reaction,
            recharge,destination,ended,attack,daily);
    }
    private static boolean has(Result r,Rider rider){
        for(Effect e:r.effects)if(e.rider==rider)return true;
        return false;
    }
    private static int count(Result r,Rider rider){
        int n=0;for(Effect e:r.effects)if(e.rider==rider)n++;return n;
    }
    private static Effect find(Result r,Rider rider){
        for(Effect e:r.effects)if(e.rider==rider)return e;
        throw new AssertionError("missing_rider_"+rider);
    }
    private static int[] ones(int n) {
        int[] d=new int[n];Arrays.fill(d,1);return d;
    }
    private static void identities(){
        ok(Action.values().length==10);
        Set<String> pointers=new HashSet<>();
        for(Action a:Action.values()){
            ok(a.pointer.startsWith("/monster/"));
            ok(pointers.add(a.pointer));
            ok(Action.fromPointer(a.pointer)==a);
        }
        blocked(()->Action.fromPointer("/wrong"));
        eq(Action.DEMILICH_ENERGY_DRAIN.saveDc,19);
        eq(Action.CATACLYSMIC_EVENT.saveDc,23);
        eq(Action.GOLD_WYRMLING_WEAKENING.saveDc,13);
        eq(Action.NEGATIVE_ENERGY_CONE.rangeFeet,150);
        eq(Action.PIRATE_ADMIRAL_RALLY.rangeFeet,30);
    }
    private static void tyrant(){
        Session s=session(target("actor",Kind.CREATURE,100,0),
            target("hero",Kind.HUMANOID,50,0),
            target("corpse",Kind.HUMANOID_CORPSE,0,0));
        Selection a=simple("hero",100);
        Selection corpse=special("corpse",120,true,true,false,false,
            true,false,false,null,null,null);
        Proposal p=s.propose(request(s,Action.NEGATIVE_ENERGY_CONE,
            "cone","turn1",0,a,corpse));
        ok(has(p.result,Rider.NO_HEAL_UNTIL_TYRANT_NEXT_TURN));
        ok(has(p.result,Rider.RAISE_ZOMBIE_FROM_INTACT_HUMANOID_CORPSE));
        ok(!p.result.persistedToCombat);
        eq(s.target("hero").hp,50);
        s.commit(p);
        eq(s.effects("hero").size(),1);
        blocked(()->s.commit(p));
        blocked(()->s.propose(request(s,Action.NEGATIVE_ENERGY_CONE,
            "cone","turn1",0,a)));
    }
    private static void demilich(){
        Session s=session(target("actor",Kind.CREATURE,100,0),
            target("hero",Kind.CREATURE,30,0));
        Selection fail=choice("hero",100,false,new int[]{1,2,3,4},null);
        Proposal p=s.propose(request(s,Action.DEMILICH_ENERGY_DRAIN,
            "drain1","one",0,fail));
        eq(find(p.result,Rider.MAX_HP_DRAIN_4D6).magnitude,10);
        eq(s.target("hero").maxHp,30);
        s.commit(p);
        eq(s.target("hero").maxHp,20);
        ok(has(p.result,Rider.DEMILICH_UNAVAILABLE_UNTIL_ITS_NEXT_TURN));
        blocked(()->s.propose(request(s,Action.DEMILICH_ENERGY_DRAIN,
            "drain2","one",0,fail)));
        Result okSave=s.propose(request(s,Action.DEMILICH_ENERGY_DRAIN,
            "drain3","two",0,choice("hero",100,true,null,null))).result;
        ok(!has(okSave,Rider.MAX_HP_DRAIN_4D6));
        blocked(()->s.propose(request(s,Action.DEMILICH_ENERGY_DRAIN,
            "drain4","two",0,choice("hero",121,false,ones(4),null))));
    }
    private static void cataclysm(){
        for(int variant=1;variant<=4;variant++){
            Session s=session(target("actor",Kind.CREATURE,100,0),
                target("hero",Kind.CREATURE,100,0));
            int first=variant==1?13:variant==2?5:4;
            int second=variant==1?0:variant==2?5:4;
            Selection fail=choice("hero",20,false,ones(first),ones(second));
            Request req=request(s,Action.CATACLYSMIC_EVENT,
                "cat"+variant,"turn1",variant,fail);
            Proposal p=s.propose(req);
            ok(has(p.result,Rider.RECHARGE_FOUR_TO_SIX));
            ok(!p.result.persistedToCombat);
            int expected=first+second;
            eq(p.result.damageByTarget.get("hero"),expected);
            if(variant==1)ok(has(p.result,Rider.BURNING));
            if(variant==2){
                ok(has(p.result,Rider.SPEED_ZERO_UNTIL_NEXT_TURN));
                ok(has(p.result,Rider.PRONE));
            }
            if(variant==3){
                ok(has(p.result,Rider.STORM_CLOUD_ONE_MINUTE_REPLACES_PRIOR));
                ok(has(p.result,Rider.CLOUD_BLIND_DEAF_NO_VERBAL_WHILE_INSIDE));
            }
            if(variant==4){
                ok(has(p.result,Rider.BURIED_RESTRAINED_TOTAL_COVER_SUFFOCATING_ESCAPE_ATHLETICS_DC18));
                eq(find(p.result,Rider.BURIED_RESTRAINED_TOTAL_COVER_SUFFOCATING_ESCAPE_ATHLETICS_DC18)
                    .magnitude,18);
            }
            eq(s.target("hero").hp,100);
            s.commit(p);
            eq(s.target("hero").hp,100-expected);
            blocked(()->s.commit(p));

            Session success=session(target("actor",Kind.CREATURE,100,0),
                target("hero",Kind.CREATURE,100,0));
            Selection save=choice("hero",10,true,ones(first),ones(second));
            Result half=success.propose(request(success,Action.CATACLYSMIC_EVENT,
                "saved","turn1",variant,save)).result;
            // Rounding happens for each typed damage component, before defenses.
            eq(half.damageByTarget.get("hero"),first/2+second/2);
            if(variant==1)ok(has(half,Rider.BURNING));
            if(variant==2)ok(has(half,Rider.SPEED_ZERO_UNTIL_NEXT_TURN));
        }
        Session s=session(target("actor",Kind.CREATURE,100,0),
            target("hero",Kind.CREATURE,100,0));
        blocked(()->s.propose(request(s,Action.CATACLYSMIC_EVENT,
            "invalid","one",5,choice("hero",10,false,ones(13),null))));
    }
    private static void absorptionAndBolster(){
        Session c=session(target("actor",Kind.CREATURE,40,3));
        Selection self=simple("actor",0);
        Request cult=full(c,Action.CULTIST_ELEMENTAL_ABSORPTION,
            "resist","turn1",0,0,0,17,Type.FIRE,1,1,
            true,true,true,true,true,true,false,true,self);
        Proposal p=c.propose(cult);
        ok(has(p.result,Rider.REACTION_RESISTANCE_THIS_DAMAGE_INSTANCE));
        eq(find(p.result,Rider.REACTION_RESISTANCE_THIS_DAMAGE_INSTANCE).magnitude,17);
        eq(c.target("actor").tempHp,3);
        c.commit(p);
        eq(c.target("actor").tempHp,10);
        blocked(()->c.propose(full(c,Action.CULTIST_ELEMENTAL_ABSORPTION,
            "resist2","turn2",0,0,0,10,Type.ACID,1,1,
            true,true,true,true,true,true,false,true,self)));

        Session emp=session(target("actor",Kind.CREATURE,50,0),
            target("ally",Kind.CREATURE,20,0));
        Proposal bolster=emp.propose(request(emp,Action.EMPYREAN_BOLSTER,
            "bolster","turn1",0,simple("ally",25)));
        ok(has(bolster.result,Rider.TEMP_HP_10));
        ok(has(bolster.result,Rider.ALLY_D20_ADVANTAGE_END_OF_EMPYREAN_NEXT_TURN));
        ok(has(bolster.result,Rider.SELF_D20_ADVANTAGE_END_OF_EMPYREAN_NEXT_TURN));
        emp.commit(bolster);
        eq(emp.target("actor").tempHp,10);
        blocked(()->emp.propose(request(emp,Action.EMPYREAN_BOLSTER,
            "bolster2","turn1",0,simple("ally",25))));
        ok(has(emp.propose(request(emp,Action.EMPYREAN_BOLSTER,
            "bolster3","turn2",0,simple("ally",25))).result,Rider.TEMP_HP_10));
    }
    private static void warGnollGold(){
        Session f=session(target("actor",Kind.CREATURE,80,0),
            target("ally",Kind.CREATURE,20,1));
        Request cry=full(f,Action.FROST_GIANT_WAR_CRY,"cry","turn1",0,
            0,0,0,null,2,3,true,true,true,true,true,true,false,true,simple("ally",100));
        Result fr=f.commit(f.propose(cry));
        eq(find(fr,Rider.WAR_CRY_TEMP_HP_2D10_PLUS_5).magnitude,10);
        eq(f.target("ally").tempHp,10);
        ok(has(fr,Rider.WAR_CRY_ATTACK_ADVANTAGE_NEXT_TURN));
        ok(has(fr,Rider.RECHARGE_FIVE_TO_SIX));

        Session g=session(target("actor",Kind.CREATURE,80,0),
            target("ally",Kind.CREATURE,30,0));
        Selection ramp=special("ally",50,true,false,false,false,false,true,
            true,null,null,null);
        Result gr=g.propose(request(g,Action.GNOLL_INCITE_RAMPAGE,
            "incite","one",0,ramp)).result;
        ok(has(gr,Rider.GNOLL_RAMPAGE_ALLY_REACTION_MELEE_ATTACK));
        ok(has(gr,Rider.RECHARGE_FIVE_TO_SIX));
        Selection notRamp=special("ally",50,true,false,false,false,false,false,
            true,null,null,null);
        blocked(()->g.propose(request(g,Action.GNOLL_INCITE_RAMPAGE,
            "incite2","one",0,notRamp)));

        Session w=session(target("actor",Kind.CREATURE,40,0),
            target("hero",Kind.CREATURE,30,0));
        Result weak=w.commit(w.propose(request(w,Action.GOLD_WYRMLING_WEAKENING,
            "weak","one",0,choice("hero",10,false,null,null))));
        ok(has(weak,Rider.WEAKENING_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_1D4));
        eq(find(weak,Rider.WEAKENING_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_1D4).magnitude,4);
        ok(has(weak,Rider.WEAKENING_REPEAT_STR13_SAVE_END_TURN_AUTO_END_ONE_MINUTE));
        blocked(()->w.propose(request(w,Action.GOLD_WYRMLING_WEAKENING,
            "weak2","two",0,choice("hero",10,false,null,null))));
        Result saved=w.propose(request(w,Action.GOLD_WYRMLING_WEAKENING,
            "weak3","two",0,choice("hero",10,true,null,null))).result;
        ok(!has(saved,Rider.WEAKENING_STR_TEST_DISADVANTAGE_DAMAGE_MINUS_1D4));
    }
    private static void teleportAndRally(){
        Session n=session(target("actor",Kind.CREATURE,100,0),
            target("hero",Kind.CREATURE,50,0));
        Proposal pursue=n.propose(request(n,Action.NALFESHNEE_PURSUIT,
            "pursue","turn1",0,simple("hero",119)));
        ok(has(pursue.result,Rider.NALFESHNEE_TELEPORT_NEAR_TRIGGER));
        eq(find(pursue.result,Rider.NALFESHNEE_TELEPORT_NEAR_TRIGGER).magnitude,10);
        n.commit(pursue);

        Session pirate=session(target("actor",Kind.CREATURE,50,0),
            target("a",Kind.CREATURE,20,0),
            target("b",Kind.CREATURE,20,0));
        Request rally=request(pirate,Action.PIRATE_ADMIRAL_RALLY,
            "rally","turn1",0,simple("a",10),simple("b",20));
        Result r=pirate.commit(pirate.propose(rally));
        eq(count(r,Rider.RALLY_ATTACK_AND_SAVE_ADVANTAGE_START_NEXT_TURN),2);
        blocked(()->pirate.propose(request(pirate,Action.PIRATE_ADMIRAL_RALLY,
            "rally2","turn2",0,simple("a",10))));
        blocked(()->pirate.propose(request(pirate,Action.PIRATE_ADMIRAL_RALLY,
            "rally3","turn3",0,simple("a",10),simple("a",10))));
    }
    public static void main(String[] args){
        identities();
        tyrant();
        demilich();
        cataclysm();
        absorptionAndBolster();
        warGnollGold();
        teleportAndRally();
        System.out.println("PASS other_source_effect-02 "+checks+
            " independent JVM assertions; 10 actions; Android and SQLite NOT integrated");
    }
}
