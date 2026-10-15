package com.braseiro.dnd5e;

import java.util.*;
import static com.braseiro.dnd5e.XmmDamageOrSaveBranch01.*;

/** Same assertions run with plain javac/java and through the JUnit bridge. */
public final class XmmDamageOrSaveBranch01Scenarios {
    private static int checks;
    static void check(boolean value) { checks++; if(!value) throw new AssertionError("check "+checks); }
    static void rejects(Runnable call) {
        checks++; try { call.run(); } catch(IllegalArgumentException expected) { return; }
        throw new AssertionError("expected rejection "+checks);
    }
    static Target target(Contract c,String id,int save,boolean hit,Defense defense) {
        List<Integer> rolls=new ArrayList<>(); Map<Type,Defense> defenses=new EnumMap<>(Type.class);
        for(Component part:c.damage) { rolls.add(part.dice+part.bonus+1); defenses.put(part.type,defense); }
        return new Target(id,5,30,true,true,true,true,false,false,save,hit,rolls,defenses);
    }
    static Session session(Contract c) {
        Session s=new Session(); s.startTurn("actor",2,1,3);
        if(c.economy==Economy.LEGENDARY) { s.endTurn("actor"); s.startTurn("other",1,1,0); s.openLegendaryWindow("other"); }
        return s;
    }
    static Request request(Session s,Contract c,String event,Target... targets) {
        return new Request(c,"actor",event,s.revision(),true,true,Form.HUNTER,Arrays.asList(targets));
    }
    static int total(TargetResult r) { int sum=0; for(int n:r.damage.values()) sum+=n; return sum; }
    public static void run() {
        checks=0; check(contracts().size()==12);
        rejects(()->contract("Balor","Hurl Flame","/monster/54/action/3/entries/0"));
        for(Contract c:contracts()) {
            check(contract(c.monster,c.action,c.pointer)==c);
            if(c.kind==Kind.AIM) continue;
            for(boolean success:new boolean[]{false,true}) for(Defense defense:Defense.values()) {
                Session s=session(c); Target t=target(c,"target",success?c.dc:0,success,defense);
                Proposal p=s.propose(request(s,c,"event",t)); check(!p.combatApplied());
                int expected=0;
                for(int roll:t.rolledDamage) {
                    int n=c.toHit>0&&!success?0:c.dc>0&&success?(c.halfOnSuccess?roll/2:0):roll;
                    if(defense==Defense.IMMUNE) n=0;
                    if(defense==Defense.RESISTANT||defense==Defense.RESISTANT_AND_VULNERABLE) n/=2;
                    if(defense==Defense.VULNERABLE||defense==Defense.RESISTANT_AND_VULNERABLE) n*=2;
                    expected+=n;
                }
                check(total(p.results.get(0))==expected);
                check(p.results.get(0).damage.keySet().equals(t.defenses.keySet()));
                if(c.kind==Kind.WAVE) check(p.results.get(0).effects.get(0).kind==EffectKind.CURSED_NO_REACTIONS);
                if(c.kind==Kind.BLADE) check(p.results.get(0).effects.size()==(success?1:0));
                if(c.kind==Kind.INSECTS||c.kind==Kind.SANDS||c.kind==Kind.GALE)
                    check(p.results.get(0).effects.size()==(success?0:1));
                if(c.kind==Kind.SPIRIT) check(p.results.get(0).effects.get(0).kind==EffectKind.PREY_ADVANTAGE);
                s.commit(p); rejects(()->s.commit(p));
                rejects(()->s.propose(new Request(c,"actor","event",s.revision(),true,true,Form.HUNTER,Arrays.asList(t))));
            }
            Session s=session(c); Target good=target(c,"target",0,true,Defense.NORMAL);
            rejects(()->s.propose(new Request(c,"actor","missing",s.revision(),false,true,Form.HUNTER,Arrays.asList(good))));
            rejects(()->s.propose(request(s,c,"duplicate",good,good)));
            Target missing=new Target("target",5,0,true,true,true,true,false,false,null,null,good.rolledDamage,Collections.emptyMap());
            rejects(()->s.propose(request(s,c,"missing",missing)));
            Target far=new Target("target",c.range+1,0,true,true,true,true,false,false,0,true,good.rolledDamage,good.defenses);
            rejects(()->s.propose(request(s,c,"far",far)));
            Target badRoll=new Target("target",1,0,true,true,true,true,false,false,0,true,Arrays.asList(999),good.defenses);
            rejects(()->s.propose(request(s,c,"bad-roll",badRoll)));
            check(s.revision()==request(s,c,"unused",good).revision);
            if(c.kind==Kind.GALE||c.kind==Kind.WAVE||c.kind==Kind.WITCH) {
                Proposal multi=s.propose(request(s,c,"multi",good,target(c,"second",c.dc,true,Defense.IMMUNE)));
                check(multi.results.size()==2); check(total(multi.results.get(1))==0);
                s.commit(multi);
            }
        }
        durationsAndEconomy(); aim(); specialContexts(); atomicAndExpiry();
        System.out.println("damage_or_save_branch-01: "+checks+" assertions passed; 12 exact contracts");
    }
    static Contract kind(Kind kind) { for(Contract c:contracts()) if(c.kind==kind) return c; throw new AssertionError(); }
    static void durationsAndEconomy() {
        Contract wave=kind(Kind.WAVE); Session s=session(wave);
        s.commit(s.propose(request(s,wave,"wave",target(wave,"target",99,true,Defense.IMMUNE))));
        check(s.effects().size()==1); s.endTurn("actor"); check(s.effects().size()==1);
        s.startTurn("target",1,1,0); s.endTurn("target"); check(s.effects().size()==1);
        s.startTurn("actor",1,1,0); check(s.effects().size()==1); s.endTurn("actor"); check(s.effects().isEmpty());
        Contract insects=kind(Kind.INSECTS); s=session(insects); final Session ledger=s;
        s.commit(s.propose(request(s,insects,"insects",target(insects,"other",0,true,Defense.IMMUNE))));
        check(s.effects().size()==1);
        rejects(()->ledger.propose(request(ledger,insects,"second",target(insects,"other",0,true,Defense.NORMAL))));
        s.endTurn("other"); check(s.effects().size()==1); // next turn, not the current turn's end
        s.startTurn("other",1,1,0); s.endTurn("other"); check(s.effects().isEmpty());
        s.startTurn("actor",1,1,3); s.endTurn("actor"); s.startTurn("other",1,1,0); s.openLegendaryWindow("other");
        s.commit(s.propose(request(s,insects,"new-turn",target(insects,"other",0,true,Defense.NORMAL))));
        Contract blade=kind(Kind.BLADE); Session b=session(blade);
        Proposal p=b.propose(request(b,blade,"blade",target(blade,"target",0,true,Defense.NORMAL)));
        b.commit(p); b.endTurn("actor"); b.startTurn("target",1,1,0); check(b.effects().size()==1);
        b.endTurn("target"); b.startTurn("actor",1,1,0); check(b.effects().isEmpty());
        Session none=new Session(); none.startTurn("actor",0,0,0);
        rejects(()->none.propose(request(none,blade,"no-action",target(blade,"target",0,true,Defense.NORMAL))));
        Session stale=session(blade); Proposal old=stale.propose(request(stale,blade,"old",target(blade,"target",0,true,Defense.NORMAL)));
        stale.endTurn("actor"); rejects(()->stale.commit(old));
        rejects(()->session(blade).commit(old));
        Session early=new Session(); early.startTurn("actor",1,1,3); early.endTurn("actor"); early.startTurn("other",1,1,0);
        rejects(()->early.propose(request(early,insects,"early",target(insects,"target",0,true,Defense.NORMAL))));
    }
    static void aim() {
        Contract aim=kind(Kind.AIM);
        for(boolean hit:new boolean[]{false,true}) {
            Session s=session(aim); s.commit(s.propose(request(s,aim,"aim")));
            check(s.effects().get(0).kind==EffectKind.NEXT_ATTACK_ADVANTAGE);
            long rev=s.revision();
            if(hit) { rejects(()->s.nextAttack("actor","shot",rev,true,99,Defense.NORMAL)); check(s.revision()==rev); }
            check(s.nextAttack("actor","shot",s.revision(),hit,29,Defense.RESISTANT)==(hit?14:0));
            check(s.effects().isEmpty()); rejects(()->s.nextAttack("actor","again",s.revision(),true,28,Defense.NORMAL));
            rejects(()->s.propose(request(s,aim,"another-aim")));
        }
        Session s=session(aim); s.commit(s.propose(request(s,aim,"expires"))); s.endTurn("actor");
        check(s.effects().isEmpty()); s.startTurn("actor",1,1,0);
        rejects(()->s.nextAttack("actor","late",s.revision(),true,28,Defense.NORMAL));
    }
    static void specialContexts() {
        Contract spirit=kind(Kind.SPIRIT);
        for(Form f:Form.values()) {
            Session s=session(spirit); Target t=target(spirit,"target",99,true,Defense.IMMUNE);
            Proposal p=s.propose(new Request(spirit,"actor","spirit",s.revision(),true,true,f,Arrays.asList(t)));
            check(p.results.get(0).effects.size()==1);
            Effect e=p.results.get(0).effects.get(0);
            check(e.kind==(f==Form.FORAGER?EffectKind.TEMPORARY_HP:f==Form.HUNTER?EffectKind.PREY_ADVANTAGE:EffectKind.ATTACK_AND_CHECK_DISADVANTAGE));
            if(f==Form.FORAGER) check(e.amount==20&&e.target.equals("actor"));
        }
        Session ss=session(spirit);
        rejects(()->ss.propose(new Request(spirit,"actor","no-form",ss.revision(),true,true,null,Arrays.asList(target(spirit,"t",0,true,Defense.NORMAL)))));
        Contract witch=kind(Kind.WITCH); Session w=session(witch); Target t=target(witch,"t",0,true,Defense.NORMAL);
        Target uncursed=new Target("t",5,0,true,true,false,true,false,false,null,null,t.rolledDamage,t.defenses);
        rejects(()->w.propose(request(w,witch,"uncursed",uncursed)));
        Contract flame=kind(Kind.FLAME);
        for(boolean hit:new boolean[]{false,true}) for(boolean worn:new boolean[]{false,true}) {
            Session s=session(flame); Target base=target(flame,"object",0,hit,Defense.IMMUNE);
            Target object=new Target("object",150,0,false,false,false,false,true,worn,null,hit,base.rolledDamage,base.defenses);
            Proposal p=s.propose(request(s,flame,"flame",object));
            check(p.results.get(0).effects.size()==(hit&&!worn?1:0));
        }
        Contract gale=kind(Kind.GALE); Session g=session(gale); Target base=target(gale,"t",0,true,Defense.NORMAL);
        Target push=new Target("t",1,31,true,true,true,true,false,false,0,true,base.rolledDamage,base.defenses);
        rejects(()->g.propose(request(g,gale,"push",push)));
    }
    static void atomicAndExpiry() {
        Contract wave=kind(Kind.WAVE); Session s=session(wave);
        Target valid=target(wave,"valid",0,true,Defense.NORMAL);
        Target invalid=new Target("invalid",61,0,true,true,true,true,false,false,0,true,valid.rolledDamage,valid.defenses);
        long revision=s.revision();
        rejects(()->s.propose(request(s,wave,"retry",valid,invalid)));
        check(s.revision()==revision&&s.effects().isEmpty());
        // Same event remains usable after validation failed; no partial commit.
        s.commit(s.propose(request(s,wave,"retry",valid)));
        for(Kind kind:Arrays.asList(Kind.SANDS,Kind.SPIRIT)) {
            Contract c=kind(kind); Session ledger=session(c);
            Target t=target(c,"target",0,true,Defense.IMMUNE);
            Proposal p=ledger.propose(new Request(c,"actor","duration",ledger.revision(),true,true,Form.SAGE,Arrays.asList(t)));
            ledger.commit(p); check(ledger.effects().size()==1);
            if(c.economy==Economy.LEGENDARY) ledger.endTurn("other"); else ledger.endTurn("actor");
            ledger.startTurn("target",1,1,0); check(ledger.effects().size()==1);
            ledger.endTurn("target"); check(ledger.effects().isEmpty());
        }
        Contract aim=kind(Kind.AIM);
        for(Defense d:Defense.values()) {
            Session ledger=session(aim); ledger.commit(ledger.propose(request(ledger,aim,"arm")));
            int result=ledger.nextAttack("actor","attack",ledger.revision(),true,29,d);
            int expected=d==Defense.IMMUNE?0:d==Defense.RESISTANT?14:d==Defense.VULNERABLE?58:d==Defense.RESISTANT_AND_VULNERABLE?28:29;
            check(result==expected);
        }
    }
    public static void main(String[] args) {
        if(args.length==1&&args[0].equals("--contracts")) {
            for(Contract c:contracts()) {
                StringBuilder damage=new StringBuilder();
                for(Component part:c.damage) {
                    if(damage.length()>0) damage.append(";");
                    damage.append(part.type).append(":").append(part.formula);
                }
                System.out.println(c.pointer+"|"+c.monster+"|"+c.action+"|"+c.economy+"|"+c.dc+"|"+c.range+"|"+c.width+"|"+c.toHit+"|"+c.halfOnSuccess+"|"+damage);
            }
        } else run();
    }
}
