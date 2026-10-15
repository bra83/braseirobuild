package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmDamageOrSaveBranch05.*;

/** Standalone Java 8 tests: explicit input, no Android, no AI, no auto-rolls. */
public final class XmmDamageOrSaveBranch05Scenarios {
    private static int checks=0;
    private static void ok(boolean v){checks++;if(!v)throw new AssertionError("check_"+checks);}
    private static void eq(int got,int want){ok(got==want);}
    private static void blocked(Runnable action){
        checks++;
        try{action.run();}catch(IllegalArgumentException expected){return;}
        throw new AssertionError("missing_rejection_"+checks);
    }
    private static Map<Type,Defense> defenses(Defense d){
        Map<Type,Defense> map=new EnumMap<>(Type.class);
        for(Type t:Type.values())map.put(t,d);
        return map;
    }
    private static Target target(Kind kind,int hp,int max,Defense d){
        return new Target("victim",kind,hp,max,defenses(d));
    }
    private static Session session(Kind kind,int hp,int max,Defense d){
        return new Session(Collections.singletonList(target(kind,hp,max,d)));
    }
    private static int[] ones(Dice d){
        if(d==null)return new int[0];
        int[] a=new int[d.count];Arrays.fill(a,1);return a;
    }
    private static int raw(Dice d){return d==null?0:d.bonus+d.count;}
    private static Choice choice(Action a){return a==Action.WARRIOR_COMMANDER?Choice.SAP:Choice.NONE;}
    private static Request req(Session s,Action a,Boolean hit,Boolean save,int[] first,int[] second,
            Choice choice,Form form,String ally,int teleport,int zombies,boolean visible,
            boolean waterOnly,boolean allyReact,boolean allySees,boolean emptyDest){
        return new Request(a,"actor","victim","id-"+a.name(),s.revision(),5,
                hit,save,first,second,choice,form,ally,teleport,zombies,visible,waterOnly,
                allyReact,allySees,emptyDest);
    }
    private static Request normal(Session s,Action a){
        return req(s,a,a.savesInsteadOfAttack()?null:Boolean.TRUE,
            a.savesInsteadOfAttack()?Boolean.FALSE:(a.isLycanthrope()?Boolean.FALSE:null),
            ones(a.first),ones(a.second),choice(a),
            a.isLycanthrope()?Form.HYBRID:Form.OTHER,null,0,0,true,false,false,false,false);
    }
    private static boolean has(Result r,Rider rider){
        for(Effect effect:r.pending)if(effect.rider==rider)return true;
        return false;
    }
    private static Effect find(Result r,Rider rider){
        for(Effect e:r.pending)if(e.rider==rider)return e;
        throw new AssertionError("missing_"+rider);
    }
    private static void identitiesAndDamage(){
        ok(Action.values().length==12);
        Set<String> seen=new HashSet<>();
        for(Action a:Action.values()){
            ok(seen.add(a.pointer));
            ok(Action.source(a.pointer)==a);
            ok(a.pointer.startsWith("/monster/"));
            Session s=session(Kind.HUMANOID,100,100,Defense.NORMAL);
            Request r=normal(s,a);
            Proposal p=s.propose(r);
            eq(s.target("victim").hp,100);
            eq(p.result.totalDamage,raw(a.first)+raw(a.second));
            ok(!p.result.persistedToAndroid);
            if(a.isLycanthrope())ok(has(p.result,Rider.LYCANTHROPY_CURSE));
            if(a==Action.WIGHT)ok(has(p.result,Rider.MAX_HP_DRAIN_TOTAL));
            if(a==Action.STEAM_MEPHIT)ok(has(p.result,Rider.SPEED_MINUS_10_UNTIL_NEXT_TURN));
            Result after=s.execute(p);
            eq(s.target("victim").hp,after.hp);
            eq(s.revision()==1?1:0,1);
            blocked(()->s.execute(p));
            blocked(()->s.propose(r));
        }
        blocked(()->Action.source("/monster/454/action/0/entries/0"));
    }
    private static void specialEffects(){
        Session steam=session(Kind.HUMANOID,100,100,Defense.NORMAL);
        Result failed=steam.propose(normal(steam,Action.STEAM_MEPHIT)).result;
        ok(has(failed,Rider.SPEED_MINUS_10_UNTIL_NEXT_TURN));
        ok(has(failed,Rider.RECHARGE_SIX));
        ok(find(failed,Rider.RECHARGE_SIX).target.equals("actor"));
        Request steamOk=req(steam,Action.STEAM_MEPHIT,null,true,
            new int[]{1,2},new int[0],Choice.NONE,Form.OTHER,null,0,0,
            true,false,false,false,false);
        Result half=steam.propose(steamOk).result;
        eq(half.totalDamage,1);
        ok(!has(half,Rider.SPEED_MINUS_10_UNTIL_NEXT_TURN));
        Session water=session(Kind.HUMANOID,100,100,Defense.RESISTANT);
        Request underwater=req(water,Action.STEAM_MEPHIT,null,true,
            new int[]{3,2},new int[0],Choice.NONE,Form.OTHER,null,0,0,
            true,true,false,false,false);
        eq(water.propose(underwater).result.totalDamage,2);
        Session vuln=session(Kind.HUMANOID,100,100,Defense.VULNERABLE);
        Request vulnerable=req(vuln,Action.STEAM_MEPHIT,null,true,
            new int[]{3,2},new int[0],Choice.NONE,Form.OTHER,null,0,0,
            true,false,false,false,false);
        eq(vuln.propose(vulnerable).result.totalDamage,4);

        Session whip=session(Kind.HUMANOID,100,100,Defense.NORMAL);
        Request teleport=req(whip,Action.ULTROLOTH,true,null,
            ones(Action.ULTROLOTH.first),new int[0],Choice.TELEPORT,
            Form.OTHER,null,10,0,true,false,false,false,true);
        eq(find(whip.propose(teleport).result,Rider.TELEPORT_TARGET_UP_TO_10FT).amount,10);
        Request illegalTeleport=req(whip,Action.ULTROLOTH,true,null,
            ones(Action.ULTROLOTH.first),new int[0],Choice.TELEPORT,
            Form.OTHER,null,11,0,true,false,false,false,true);
        blocked(()->whip.propose(illegalTeleport));

        Session vamp=session(Kind.CREATURE,30,30,Defense.NORMAL);
        Result bite=vamp.execute(vamp.propose(normal(vamp,Action.VAMPIRE_NIGHTBRINGER)));
        eq(bite.necroticDamage,3);
        eq(bite.maxHp,27);
        eq(find(bite,Rider.ACTOR_HEALS_NECROTIC_DAMAGE).amount,3);
        ok(find(bite,Rider.ACTOR_HEALS_NECROTIC_DAMAGE).target.equals("actor"));
        Session lord=session(Kind.HUMANOID,30,30,Defense.NORMAL);
        Result drain=lord.propose(normal(lord,Action.VAMPIRE_UMBRAL_LORD)).result;
        ok(has(drain,Rider.MAX_HP_DRAIN_NECROTIC));
        Session construct=session(Kind.CONSTRUCT,30,30,Defense.NORMAL);
        blocked(()->construct.propose(normal(construct,Action.VAMPIRE_UMBRAL_LORD)));

        Session warrior=session(Kind.CREATURE,100,100,Defense.NORMAL);
        ok(has(warrior.propose(normal(warrior,Action.WARRIOR_COMMANDER)).result,
            Rider.WARRIOR_SAP_NEXT_ATTACK_DISADVANTAGE));
        Request maneuver=req(warrior,Action.WARRIOR_COMMANDER,true,null,
            ones(Action.WARRIOR_COMMANDER.first),new int[0],Choice.MANEUVER,
            Form.OTHER,"ally1",0,0,true,false,true,true,false);
        ok(find(warrior.propose(maneuver).result,Rider.WARRIOR_MANEUVER_ALLY_REACTION_HALF_SPEED).target.equals("ally1"));

        for(Action a:Action.values()){
            if(!a.isLycanthrope())continue;
            Session s=session(Kind.HUMANOID,50,50,Defense.NORMAL);
            Request success=req(s,a,true,true,ones(a.first),new int[0],
                Choice.NONE,Form.BEAST,null,0,0,true,false,false,false,false);
            Result resisted=s.execute(s.propose(success));
            ok(has(resisted,Rider.IMMUNE_TO_THIS_LYCANTHROPE_CURSE_24H));
            // A second attack is a NEW event. Replay protection correctly
            // rejects reuse of the previous event ID, even after revision++.
            Request later=new Request(a,"actor","victim","id-second-"+a.name(),
                s.revision(),5,Boolean.TRUE,null,ones(a.first),new int[0],
                Choice.NONE,Form.BEAST,null,0,0,true,false,false,false,false);
            ok(!has(s.propose(later).result,Rider.LYCANTHROPY_CURSE));
            Session dying=session(Kind.HUMANOID,1,50,Defense.NORMAL);
            Result transformed=dying.propose(normal(dying,a)).result;
            ok(has(transformed,Rider.TRANSFORM_TO_LYCANTHROPE_AT_ZERO_HP_DM_CONTROL));
            eq(find(transformed,Rider.TRANSFORM_TO_LYCANTHROPE_AT_ZERO_HP_DM_CONTROL).amount,10);
        }

        Session wight=session(Kind.HUMANOID,1,10,Defense.NORMAL);
        Result life=wight.propose(normal(wight,Action.WIGHT)).result;
        eq(life.maxHp,7);
        eq(life.hp,0);
        ok(has(life,Rider.ZOMBIE_RISES_AFTER_24H_CAP_12));
        Request atCap=req(wight,Action.WIGHT,null,false,ones(Action.WIGHT.first),new int[0],
                Choice.NONE,Form.OTHER,null,0,12,true,false,false,false,false);
        ok(!has(wight.propose(atCap).result,Rider.ZOMBIE_RISES_AFTER_24H_CAP_12));
        Session worg=session(Kind.CREATURE,30,30,Defense.NORMAL);
        ok(has(worg.propose(normal(worg,Action.WORG)).result,
            Rider.NEXT_ATTACK_AGAINST_TARGET_ADVANTAGE));
    }
    private static void missesAndGuards(){
        for(Action a:Action.values()){
            Session s=session(Kind.HUMANOID,100,100,Defense.NORMAL);
            Boolean hit=a.savesInsteadOfAttack()?null:Boolean.FALSE;
            Boolean saved=a.savesInsteadOfAttack()?Boolean.TRUE:null;
            Request noEffect=req(s,a,hit,saved,new int[0],new int[0],
                Choice.NONE,a.isLycanthrope()?Form.BEAST:Form.OTHER,
                null,0,0,true,false,false,false,false);
            if(a==Action.STEAM_MEPHIT){
                // A successful Steam Breath save takes half damage, never zero.
                blocked(()->s.propose(noEffect));
            }else{
                Result result=s.propose(noEffect).result;
                eq(result.totalDamage,0);
                ok(result.pending.isEmpty());
            }
        }
        Session s=session(Kind.CREATURE,100,100,Defense.IMMUNE);
        Result nullified=s.propose(normal(s,Action.WORG)).result;
        eq(nullified.totalDamage,0);
        ok(has(nullified,Rider.NEXT_ATTACK_AGAINST_TARGET_ADVANTAGE));
        Session reach=session(Kind.HUMANOID,20,20,Defense.NORMAL);
        blocked(()->reach.propose(req(reach,Action.ULTROLOTH,true,null,
            ones(Action.ULTROLOTH.first),new int[0],Choice.NONE,Form.OTHER,
            null,0,0,false,false,false,false,false)));
        Session other=session(Kind.HUMANOID,100,100,Defense.NORMAL);
        Proposal foreign=reach.propose(normal(reach,Action.WORG));
        blocked(()->other.execute(foreign));
    }
    public static void main(String[] args){
        identitiesAndDamage();
        specialEffects();
        missesAndGuards();
        System.out.println("PASS branch-05 "+checks+
            " independent JVM assertions; 12 contracts; combat/SQLite NOT integrated");
    }
}
