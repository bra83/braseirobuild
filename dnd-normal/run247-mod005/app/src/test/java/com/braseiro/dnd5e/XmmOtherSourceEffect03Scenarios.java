package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmOtherSourceEffect03.*;

/** Independent JDK8 regression suite for nine XMM lot-09 action contracts. */
public final class XmmOtherSourceEffect03Scenarios {
    private static int checks;
    private static void yes(boolean actual){checks++;if(!actual)throw new AssertionError("check_"+checks);}
    private static void equal(int actual,int expected){yes(actual==expected);}
    private static void blocked(Runnable test){
        checks++;
        try{test.run();}catch(IllegalArgumentException correct){return;}
        throw new AssertionError("expected_validation_rejection_"+checks);
    }
    private static Map<Type,Defense> defenses(Defense choice){
        EnumMap<Type,Defense> d=new EnumMap<>(Type.class);
        for(Type t:Type.values())d.put(t,choice);
        return d;
    }
    private static Target target(String id,int hp,int temp,Defense defense){
        return new Target(id,Kind.CREATURE,hp,hp,temp,defenses(defense));
    }
    private static Session session(){
        return new Session(Arrays.asList(target("actor",80,0,Defense.NORMAL),
            target("hero",100,0,Defense.NORMAL),target("ally",100,0,Defense.NORMAL)));
    }
    private static Selection pick(String id,int distance,Boolean saved,int[] dice,int d8){
        return new Selection(id,distance,true,true,true,true,saved,dice,d8);
    }
    private static Selection pick(String id,Boolean saved){
        return pick(id,15,saved,new int[0],0);
    }
    private static Request req(Session s,Action a,String event,String turn,
            Choice melody,int ray,Selection... target){
        return new Request(a,"actor",event,turn,s.revision(),Arrays.asList(target),
            melody,ray,true,true,true);
    }
    private static Request req(Session s,Action a,String event,String turn,Selection... target){
        return req(s,a,event,turn,Choice.NONE,0,target);
    }
    private static boolean has(Result r,Rider rider){
        for(Effect e:r.pending)if(e.kind==rider)return true;
        return false;
    }
    private static Effect get(Result r,Rider rider){
        for(Effect e:r.pending)if(e.kind==rider)return e;
        throw new AssertionError("missing_effect_"+rider);
    }
    private static int count(Result r,Rider rider){
        int n=0;for(Effect e:r.pending)if(e.kind==rider)n++;return n;
    }
    private static int[] ones(int n){
        int[] values=new int[n];Arrays.fill(values,1);return values;
    }
    private static void sourceIdentities(){
        yes(Action.values().length==9);
        Set<String> unique=new HashSet<>();
        for(Action a:Action.values()){
            yes(a.pointer.startsWith("/monster/"));
            yes(unique.add(a.pointer));
            yes(Action.source(a.pointer)==a);
        }
        equal(Action.SATYR.saveDc,14);
        equal(Action.SPECTATOR.saveDc,12);
        equal(Action.SPHINX_VALOR.saveDc,20);
        equal(Action.UMBER_HULK.saveDc,14);
        equal(Action.YOUNG_COPPER.saveDc,14);
        equal(Action.YOUNG_GOLD.saveDc,17);
        equal(Action.SPHINX_VALOR.rangeFt,500);
        equal(Action.SPECTATOR.rangeFt,90);
        blocked(()->Action.source("/monster/999/action/0/entries/0"));
    }
    private static void revenant(){
        Session s=session();
        Proposal p=s.propose(req(s,Action.REVENANT,"vow1","day1",pick("hero",null)));
        yes(has(p.result,Rider.VOW_TRACK_DISTANCE_AND_DIRECTION_ANY_PLANE));
        yes(!p.result.integratedIntoCombat);
        yes(s.effects("hero").isEmpty());
        s.commit(p);
        yes(s.effects("hero").size()==1);
        blocked(()->s.commit(p));
        blocked(()->s.propose(req(s,Action.REVENANT,"vow2","day1",pick("ally",null))));
        s.newDayFor("actor");
        Result next=s.commit(s.propose(req(s,Action.REVENANT,"vow3","day2",pick("ally",null))));
        yes(has(next,Rider.VOW_TRACK_DISTANCE_AND_DIRECTION_ANY_PLANE));
        yes(s.effects("hero").isEmpty());
        yes(s.effects("ally").size()==1);
        blocked(()->s.propose(req(s,Action.REVENANT,"vow3","day2",pick("ally",null))));
    }
    private static void satyr(){
        Session s=session();
        Result charming=s.propose(req(s,Action.SATYR,"song1","turn1",Choice.CHARMING,0,
            pick("hero",false),pick("ally",true))).result;
        yes(has(charming,Rider.SATYR_CHARMED_INCAPACITATED_DANCE_UNTIL_DAMAGE_OR_ONE_MINUTE));
        equal(count(charming,Rider.SATYR_CHARMED_INCAPACITATED_DANCE_UNTIL_DAMAGE_OR_ONE_MINUTE),1);
        yes(charming.damage.isEmpty());
        Session f=session();
        Result fear=f.propose(req(f,Action.SATYR,"song2","turn1",Choice.FRIGHTENING,0,
            pick("hero",15,false,new int[]{2,3},0))).result;
        equal(fear.damage.get("hero"),8);
        yes(has(fear,Rider.SATYR_FRIGHTENED_ONE_MINUTE_OR_LINE_OF_SIGHT_LOST_AT_END_TURN));
        equal(f.target("hero").hp,100);
        f.commit(f.propose(req(f,Action.SATYR,"song2","turn1",Choice.FRIGHTENING,0,
            pick("hero",15,false,new int[]{2,3},0))));
        equal(f.target("hero").hp,92);
        blocked(()->s.propose(req(s,Action.SATYR,"bad","turn1",Choice.NONE,0,pick("hero",false))));
        blocked(()->s.propose(req(s,Action.SATYR,"bad-dice","turn1",Choice.CHARMING,0,
            pick("hero",15,false,new int[]{1,1},0))));
    }
    private static void spectator(){
        Session s=session();
        for(int ray=1;ray<=4;ray++){
            int[] dice=ray==1||ray==3?new int[]{1,1}:
                ray==4?new int[]{1,1,1}:new int[0];
            Request request=req(s,Action.SPECTATOR,"ray"+ray,"turn1",Choice.NONE,ray,
                pick("hero",15,false,dice,0));
            Proposal proposal=s.propose(request);
            yes(proposal.result.rayIndex==ray);
            if(ray==1){
                equal(proposal.result.damage.get("hero"),2);
                yes(has(proposal.result,Rider.SPECTATOR_CONFUSED_NO_REACTIONS_RANDOM_ATTACK_NEXT_TURN));
            }
            if(ray==2)yes(has(proposal.result,Rider.SPECTATOR_PARALYZED_REPEAT_SAVE_END_TURN_ONE_MINUTE));
            if(ray==3)yes(has(proposal.result,Rider.SPECTATOR_FRIGHTENED_END_NEXT_TURN));
            if(ray==4)equal(proposal.result.damage.get("hero"),3);
            s.commit(proposal);
            blocked(()->s.propose(req(s,Action.SPECTATOR,"rayagain"+ray,"turn1",
                Choice.NONE,ray,pick("hero",true))));
        }
        Session saved=session();
        Result half=saved.propose(req(saved,Action.SPECTATOR,"wounding","turn1",Choice.NONE,4,
            pick("hero",15,true,new int[]{1,1,1},0))).result;
        equal(half.damage.get("hero"),1);
        Session immune=new Session(Arrays.asList(target("actor",80,0,Defense.NORMAL),
            target("hero",100,0,Defense.IMMUNE)));
        Result zero=immune.propose(req(immune,Action.SPECTATOR,"necrotic","turn1",Choice.NONE,4,
            pick("hero",15,false,new int[]{8,8,8},0))).result;
        equal(zero.damage.get("hero"),0);
        blocked(()->saved.propose(req(saved,Action.SPECTATOR,"invalid","turn1",Choice.NONE,5,
            pick("hero",true))));
        blocked(()->saved.propose(req(saved,Action.SPECTATOR,"failed-with-no-roll","turn1",Choice.NONE,1,
            pick("hero",false))));
    }
    private static void valor(){
        Session s=session();
        equal(s.roarsUsed("actor"),0);
        Proposal first=s.propose(req(s,Action.SPHINX_VALOR,"roar1","turn1",
            pick("hero",100,false,new int[0],0),pick("ally",100,true,new int[0],0)));
        equal(first.result.roarNumber,1);
        equal(count(first.result,Rider.SPHINX_VALOR_FRIGHTENED_ONE_MINUTE),1);
        s.commit(first);
        equal(s.roarsUsed("actor"),1);
        Proposal second=s.propose(req(s,Action.SPHINX_VALOR,"roar2","turn2",pick("hero",false)));
        equal(second.result.roarNumber,2);
        yes(has(second.result,Rider.SPHINX_VALOR_PARALYZED_REPEAT_SAVE_ONE_MINUTE));
        s.commit(second);
        Proposal third=s.propose(req(s,Action.SPHINX_VALOR,"roar3","turn3",
            pick("hero",100,false,ones(8),0),pick("ally",100,true,ones(8),0)));
        equal(third.result.roarNumber,3);
        equal(third.result.damage.get("hero"),8);
        equal(third.result.damage.get("ally"),4);
        yes(has(third.result,Rider.SPHINX_VALOR_PRONE));
        s.commit(third);
        equal(s.roarsUsed("actor"),3);
        blocked(()->s.propose(req(s,Action.SPHINX_VALOR,"roar4","turn4",pick("hero",false))));
        s.longRestForSphinx("actor");
        equal(s.roarsUsed("actor"),0);
        equal(s.propose(req(s,Action.SPHINX_VALOR,"roarAfterRest","turn5",
            pick("hero",false))).result.roarNumber,1);
        blocked(()->s.propose(req(s,Action.SPHINX_VALOR,"far","turn5",
            pick("hero",501,false,new int[0],0))));
    }
    private static void wonderAndUnicorn(){
        Session s=session();
        for(int i=1;i<=2;i++){
            Result bonus=s.commit(s.propose(req(s,Action.SPHINX_WONDER,
                "ingenuity"+i,"turn"+i,pick("hero",null))));
            equal(get(bonus,Rider.SPHINX_WONDER_ADD_2_TO_TRIGGERING_ROLL).magnitude,2);
        }
        blocked(()->s.propose(req(s,Action.SPHINX_WONDER,"ingenuity3","turn3",
            pick("hero",null))));
        s.newDayFor("actor");
        yes(has(s.propose(req(s,Action.SPHINX_WONDER,"ingenuity4","turn4",
            pick("hero",null))).result,Rider.SPHINX_WONDER_ADD_2_TO_TRIGGERING_ROLL));
        Session unicorn=session();
        Proposal shield=unicorn.propose(req(unicorn,Action.UNICORN,"shield1","one",
            pick("hero",15,null,new int[]{2,4,5},0)));
        equal(get(shield.result,Rider.UNICORN_TEMP_HP_3D6).magnitude,11);
        equal(get(shield.result,Rider.UNICORN_AC_PLUS_TWO_UNTIL_END_NEXT_TURN).magnitude,2);
        equal(unicorn.target("hero").tempHp,0);
        unicorn.commit(shield);
        equal(unicorn.target("hero").tempHp,11);
        blocked(()->unicorn.propose(req(unicorn,Action.UNICORN,"shield2","one",
            pick("ally",15,null,ones(3),0))));
        yes(has(unicorn.propose(req(unicorn,Action.UNICORN,"shield3","two",
            pick("hero",15,null,ones(3),0))).result,
            Rider.UNICORN_AC_PLUS_TWO_UNTIL_END_NEXT_TURN));
        blocked(()->unicorn.propose(req(unicorn,Action.UNICORN,"too-far","two",
            pick("hero",61,null,ones(3),0))));
    }
    private static void umber(){
        for(int d8=1;d8<=8;d8++){
            Session s=session();
            Result r=s.propose(req(s,Action.UMBER_HULK,"gaze"+d8,"turn1",
                pick("hero",25,false,new int[0],d8))).result;
            yes(has(r,Rider.UMBER_NO_REACTIONS_UNTIL_START_NEXT_TURN));
            Rider expected=d8<=4?Rider.UMBER_NEXT_TURN_DO_NOTHING:
                d8<=6?Rider.UMBER_NEXT_TURN_RANDOM_MOVE_NO_ACTION:
                Rider.UMBER_NEXT_TURN_RANDOM_MELEE_OR_NOTHING;
            yes(has(r,expected));
            s.commit(s.propose(req(s,Action.UMBER_HULK,"gaze"+d8,"turn1",
                pick("hero",25,false,new int[0],d8))));
        }
        Session saved=session();
        yes(saved.propose(req(saved,Action.UMBER_HULK,"saved","turn1",
            pick("hero",true))).result.pending.isEmpty());
        blocked(()->saved.propose(req(saved,Action.UMBER_HULK,"no-d8","turn1",
            pick("hero",false))));
        blocked(()->saved.propose(req(saved,Action.UMBER_HULK,"bad-d8","turn1",
            pick("hero",20,false,new int[0],9))));
    }
    private static void youngDragons(){
        Session copper=session();
        Proposal p=copper.propose(req(copper,Action.YOUNG_COPPER,"copper","t1",
            pick("hero",false),pick("ally",true)));
        equal(count(p.result,Rider.COPPER_NO_REACTION_HALF_SPEED_ACTION_OR_BONUS_UNTIL_END_NEXT_TURN),1);
        copper.commit(p);
        blocked(()->copper.commit(p));
        Session gold=session();
        Proposal weak=gold.propose(req(gold,Action.YOUNG_GOLD,"gold","t1",
            pick("hero",false)));
        yes(has(weak.result,Rider.GOLD_STR_D20_DISADVANTAGE_DAMAGE_MINUS_1D6));
        equal(get(weak.result,Rider.GOLD_STR_D20_DISADVANTAGE_DAMAGE_MINUS_1D6).magnitude,6);
        yes(has(weak.result,Rider.GOLD_REPEAT_SAVE_END_TURN_UNTIL_ONE_MINUTE));
        gold.commit(weak);
        blocked(()->gold.propose(req(gold,Action.YOUNG_GOLD,"again","t2",
            pick("hero",true))));
        yes(gold.propose(req(gold,Action.YOUNG_GOLD,"unaffected","t2",
            pick("ally",true))).result.pending.isEmpty());
    }
    private static void genericGuards(){
        Session s=session();
        blocked(()->s.propose(req(s,Action.SPECTATOR,"bad-ray","t",Choice.NONE,0,
            pick("hero",false))));
        blocked(()->s.propose(req(s,Action.REVENANT,"too-far","t",
            pick("hero",31,null,new int[0],0))));
        blocked(()->s.propose(req(s,Action.YOUNG_GOLD,"bad-dice","t",
            pick("hero",20,false,new int[]{1},0))));
        blocked(()->s.propose(req(s,Action.YOUNG_COPPER,"bad-save","t",
            pick("hero",null))));
        blocked(()->s.propose(req(s,Action.SPHINX_WONDER,"bad-far","t",
            pick("hero",31,null,new int[0],0))));
        Proposal p=s.propose(req(s,Action.REVENANT,"good","day",pick("hero",null)));
        Session alien=session();
        blocked(()->alien.commit(p));
        s.commit(p);
        blocked(()->s.commit(p));
        blocked(()->s.propose(req(s,Action.REVENANT,"good","day",pick("hero",null))));
    }
    public static void main(String[] args){
        sourceIdentities();
        revenant();
        satyr();
        spectator();
        valor();
        wonderAndUnicorn();
        umber();
        youngDragons();
        genericGuards();
        System.out.println("PASS other_source_effect-03 "+checks+
            " Java assertions; 9 source-pinned contracts; Android/SQLite NOT integrated");
    }
}
