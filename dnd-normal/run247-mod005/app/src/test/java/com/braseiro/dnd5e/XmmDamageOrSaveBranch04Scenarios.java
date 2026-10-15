package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmDamageOrSaveBranch04.*;

/** JVM-only regression suite. Tested from one source with plain javac/java. */
public final class XmmDamageOrSaveBranch04Scenarios {
    private static int checks;
    private static void yes(boolean v){checks++;if(!v)throw new AssertionError("check "+checks);}
    private static void equal(int actual,int expected){yes(actual==expected);}
    private static void blocked(Runnable r){
        checks++;try{r.run();}catch(IllegalArgumentException expected){return;}
        throw new AssertionError("missing rejection "+checks);
    }
    private static Map<Type,Defense> defenses(Defense defaultDefense){
        EnumMap<Type,Defense> d=new EnumMap<>(Type.class);
        for(Type type:Type.values())d.put(type,defaultDefense);
        return d;
    }
    private static Target target(int hp,int max,Kind kind,Defense defense){
        return new Target("victim",kind,hp,max,12,defenses(defense));
    }
    private static Session session(Target t){return new Session(Collections.singletonList(t));}
    private static int[] low(Dice dice) {
        if(dice==null)return new int[0];
        int[] rolls=new int[dice.count];
        Arrays.fill(rolls,1);
        return rolls;
    }
    private static boolean has(Result r,Rider kind){
        for(Effect e:r.effects)if(e.kind==kind)return true;
        return false;
    }
    private static Effect get(Result r,Rider kind){
        for(Effect e:r.effects)if(e.kind==kind)return e;
        throw new AssertionError("missing effect "+kind);
    }
    private static Request request(Session s,Action a,Boolean hit,Boolean saved,
            int[] first,int[] second,int[] extra,Choice choice,boolean ranged){
        return new Request(a,"monster","token-"+a.name(),"victim",s.revision(),
                ranged?100:5,ranged,hit,saved,first,second,extra,choice,true);
    }
    private static Request hit(Session s,Action a,Choice choice,int... extras){
        // Keep nullable Boolean objects: mixing primitive boolean and null in a
        // ternary causes javac to unbox null and throw NullPointerException.
        Boolean attackHit=a==Action.SPHINX?null:Boolean.TRUE;
        Boolean saveSucceeded=a==Action.SPHINX?Boolean.FALSE:
                (a==Action.RED_SLAAD?Boolean.TRUE:null);
        return request(s,a,attackHit,saveSucceeded,
                low(a.first),low(a.second),extras,choice,false);
    }
    private static int raw(Dice dice) {
        return dice==null?0:dice.count+dice.bonus;
    }
    private static void allContractsAndReplay(){
        yes(Action.values().length==12);
        Set<String> ids=new HashSet<>();
        for(Action a:Action.values()){
            yes(a.pointer.startsWith("/monster/"));
            yes(ids.add(a.pointer));
            yes(Action.fromPointer(a.pointer)==a);
            Session s=session(target(100,100,Kind.HUMANOID,Defense.NORMAL));
            Choice c=a==Action.PIRATE_ADMIRAL?Choice.POISONED:Choice.NONE;
            int[] extra=a==Action.SHADOW?new int[]{1}:new int[0];
            Request req=hit(s,a,c,extra);
            Result r=s.propose(req).result;
            yes(!r.combatPersisted);
            equal(s.target("victim").hp,100);
            equal(r.appliedDamage,raw(a.first)+raw(a.second));
            Result committed=s.execute(s.propose(req));
            equal(s.target("victim").hp,committed.hitPoints);
            equal(s.target("victim").maxHp,committed.maxHitPoints);
            yes(s.revision()==1);
            final Request replay=req;
            blocked(()->s.propose(replay));
            yes(committed.action==a);
        }
        blocked(()->Action.fromPointer("/unverified/entry"));
    }
    private static void failuresAndDefenses(){
        for(Action a:Action.values()){
            Session immune=session(target(100,100,Kind.HUMANOID,Defense.IMMUNE));
            int[] extra=a==Action.SHADOW?new int[]{1}:new int[0];
            Result zero=immune.propose(hit(immune,a,a==Action.PIRATE_ADMIRAL?Choice.CHARMED:Choice.NONE,
                    extra)).result;
            equal(zero.appliedDamage,0);
            if(a==Action.MUMMY)yes(has(zero,Rider.MUMMY_ROT_CURSE));
            if(a==Action.RED_SLAAD)yes(!has(zero,Rider.SLAAD_EGG_CURSE));
            if(a==Action.SPHINX)yes(has(zero,Rider.RIDDLE_CURSE));
            if(a!=Action.SPHINX){
                Session miss=session(target(100,100,Kind.HUMANOID,Defense.NORMAL));
                Request noHit=request(miss,a,false,null,new int[0],new int[0],
                        new int[0],Choice.NONE,false);
                Result missed=miss.execute(miss.propose(noHit));
                equal(missed.appliedDamage,0);
                yes(missed.effects.isEmpty());
            }else {
                Session saved=session(target(100,100,Kind.HUMANOID,Defense.NORMAL));
                Request success=request(saved,a,null,true,new int[0],new int[0],
                        new int[0],Choice.NONE,false);
                Result ok=saved.execute(saved.propose(success));
                equal(ok.appliedDamage,0);
                yes(ok.effects.isEmpty());
            }
        }
        Session tough=session(target(100,100,Kind.HUMANOID,Defense.RESISTANT));
        Result halved=tough.propose(hit(tough,Action.MUMMY,Choice.NONE)).result;
        equal(halved.appliedDamage,(raw(Action.MUMMY.first)/2)+(raw(Action.MUMMY.second)/2));
        Session fragile=session(target(100,100,Kind.HUMANOID,Defense.VULNERABLE));
        Result doubled=fragile.propose(hit(fragile,Action.MUMMY,Choice.NONE)).result;
        equal(doubled.appliedDamage,2*(raw(Action.MUMMY.first)+raw(Action.MUMMY.second)));
        Session both=session(target(100,100,Kind.HUMANOID,Defense.BOTH));
        Result mixed=both.propose(hit(both,Action.MUMMY,Choice.NONE)).result;
        equal(mixed.appliedDamage,(raw(Action.MUMMY.first)/2)*2+(raw(Action.MUMMY.second)/2)*2);
    }
    private static void riders(){
        Session mummy=session(target(100,100,Kind.HUMANOID,Defense.NORMAL));
        Result m=mummy.execute(mummy.propose(hit(mummy,Action.MUMMY,Choice.NONE)));
        yes(has(m,Rider.MUMMY_ROT_CURSE)&&has(m,Rider.CANNOT_REGAIN_HP));
        yes(has(m,Rider.LONG_REST_MAX_HP_NOT_RESTORED));
        yes(has(m,Rider.MAX_HP_LOSS_EVERY_24H));
        yes(mummy.removeCurse("victim",Rider.MUMMY_ROT_CURSE));
        yes(mummy.effects("victim").isEmpty());

        Session lord=session(target(100,100,Kind.HUMANOID,Defense.NORMAL));
        Result l=lord.execute(lord.propose(hit(lord,Action.MUMMY_LORD,Choice.NONE)));
        yes(has(l,Rider.MUMMY_LORD_ROT_CURSE)&&has(l,Rider.LONG_REST_NO_BENEFIT));
        yes(!has(l,Rider.LONG_REST_MAX_HP_NOT_RESTORED));

        Session peryton=session(target(1,50,Kind.HUMANOID,Defense.NORMAL));
        yes(has(peryton.propose(hit(peryton,Action.PERYTON,Choice.NONE)).result,Rider.HEART_REMOVED_ON_DEATH));

        Session admiral=session(target(100,100,Kind.CREATURE,Defense.NORMAL));
        yes(has(admiral.propose(hit(admiral,Action.PIRATE_ADMIRAL,Choice.CHARMED)).result,Rider.CHARMED));
        blocked(()->admiral.propose(hit(admiral,Action.PIRATE_ADMIRAL,Choice.NONE)));

        Session captain=session(target(100,100,Kind.CREATURE,Defense.NORMAL));
        Result c=captain.execute(captain.propose(hit(captain,Action.PIRATE_CAPTAIN,Choice.NONE)));
        yes(has(c,Rider.NEXT_ATTACK_ADVANTAGE));
        yes(get(c,Rider.NEXT_ATTACK_ADVANTAGE).target.equals("monster"));

        Session ooze=session(target(100,100,Kind.CREATURE,Defense.NORMAL));
        yes(has(ooze.propose(hit(ooze,Action.PSYCHIC_GRAY_OOZE,Choice.NONE)).result,Rider.INT_SAVE_DISADVANTAGE));

        Session rakshasa=session(target(100,100,Kind.CREATURE,Defense.NORMAL));
        Result rak=rakshasa.execute(rakshasa.propose(hit(rakshasa,Action.RAKSHASA,Choice.NONE)));
        yes(has(rak,Rider.RESTLESS_CURSE)&&has(rak,Rider.LONG_REST_NO_BENEFIT));
        yes(rakshasa.removeCurse("victim",Rider.RESTLESS_CURSE));

        Session slaad=session(target(100,100,Kind.HUMANOID,Defense.NORMAL));
        Request egg=request(slaad,Action.RED_SLAAD,true,false,low(Action.RED_SLAAD.first),
                new int[0],new int[]{1,2},Choice.NONE,false);
        Result eggResult=slaad.execute(slaad.propose(egg));
        yes(has(eggResult,Rider.SLAAD_EGG_CURSE));
        equal(get(eggResult,Rider.SLAAD_EGG_GESTATION_DAYS).value,30);
        yes(slaad.removeCurse("victim",Rider.SLAAD_EGG_CURSE));

        Session shadow=session(new Target("victim",Kind.HUMANOID,30,30,1,defenses(Defense.NORMAL)));
        Request drain=hit(shadow,Action.SHADOW,Choice.NONE,1,2);
        Result drained=shadow.execute(shadow.propose(drain));
        equal(drained.strength,0);
        yes(drained.dead);
        equal(get(drained,Rider.SHADOW_RISE_IN_HOURS).value,2);

        Session solar=session(target(100,100,Kind.CREATURE,Defense.NORMAL));
        Request sword=request(solar,Action.SOLAR,false,null,new int[0],new int[0],
                new int[0],Choice.SWORD_HOVERS,true);
        Result hover=solar.execute(solar.propose(sword));
        equal(hover.appliedDamage,0);
        yes(has(hover,Rider.SWORD_HOVERS));

        Session specter=session(target(1,1,Kind.CREATURE,Defense.NORMAL));
        Result life=specter.execute(specter.propose(hit(specter,Action.SPECTER,Choice.NONE)));
        equal(life.maxHitPoints,0);
        equal(life.hitPoints,0);
        yes(life.dead);
        yes(has(life,Rider.MAX_HP_REDUCTION));

        Session sphinx=session(target(100,100,Kind.HUMANOID,Defense.NORMAL));
        Result riddle=sphinx.execute(sphinx.propose(hit(sphinx,Action.SPHINX,Choice.NONE)));
        yes(has(riddle,Rider.RIDDLE_CURSE));
        yes(has(riddle,Rider.MAGIC_ACTION_SAVE_DC_15));
        yes(has(riddle,Rider.STUDY_ACTION_CHECK_DC_15));
        yes(sphinx.removeCurse("victim",Rider.RIDDLE_CURSE));
    }
    private static void rejects(){
        Session s=session(target(100,100,Kind.HUMANOID,Defense.NORMAL));
        blocked(()->s.propose(new Request(Action.MUMMY,"monster","bad","victim",s.revision(),
                10,false,true,null,low(Action.MUMMY.first),low(Action.MUMMY.second),
                new int[0],Choice.NONE,true)));
        blocked(()->s.propose(new Request(Action.MUMMY,"monster","bad","victim",s.revision(),
                5,false,true,null,new int[]{12},low(Action.MUMMY.second),
                new int[0],Choice.NONE,true)));
        blocked(()->s.propose(new Request(Action.SPHINX,"monster","bad","victim",s.revision(),
                5,false,null,null,low(Action.SPHINX.first),new int[0],
                new int[0],Choice.NONE,true)));
        blocked(()->s.propose(new Request(Action.MUMMY,"monster","bad","victim",s.revision(),
                5,false,true,null,low(Action.MUMMY.first),low(Action.MUMMY.second),
                new int[0],Choice.NONE,false)));
        Session other=session(target(100,100,Kind.HUMANOID,Defense.NORMAL));
        Proposal p=s.propose(hit(s,Action.PERYTON,Choice.NONE));
        blocked(()->other.execute(p));
        s.execute(p);
        blocked(()->s.execute(p));
        blocked(()->session(target(100,100,Kind.HUMANOID,Defense.NORMAL))
                .propose(new Request(Action.MUMMY,"monster","bad","victim",0,5,false,true,null,
                         low(Action.MUMMY.first),low(Action.MUMMY.second),new int[0],
                         Choice.SWORD_HOVERS,true)));
    }
    public static void main(String[] args){
        checks=0;
        allContractsAndReplay();
        failuresAndDefenses();
        riders();
        rejects();
        System.out.println("PASS branch-04 "+checks+" independent Java assertions; 12 pointer-specific core contracts; combat SQLite NOT integrated");
    }
}
