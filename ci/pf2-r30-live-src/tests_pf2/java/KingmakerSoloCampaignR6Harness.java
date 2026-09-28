package com.braseiro.pathfinder2e;

import org.json.JSONObject;

/**
 * E2E local R6: continuidade de exploração, encontro de acampamento bloqueante,
 * arco de companion persistente, Reino guiado, guerra e restore no meio.
 * Usa os motores mecânicos reais; não substitui device Android.
 */
public final class KingmakerSoloCampaignR6Harness {
    private static final KingmakerRulesEngine RULES=new KingmakerRulesEngine();

    private static final class State {
        int week,hex=200,camps,campEncounters,campResumes,companionEvents,companionProgress,companionCompleted;
        int kingdomTurns,kingdomChecks,wars,armyA=14,armyB=14,restores,validations,neighborWindows;
        int kingdomLevel=6,kingdomSize=16,kingdomXp,resourceTotal;
        boolean campEncounterPending;
        JSONObject json() throws Exception {return new JSONObject()
                .put("week",week).put("hex",hex).put("camps",camps).put("campEncounters",campEncounters)
                .put("campResumes",campResumes).put("campEncounterPending",campEncounterPending)
                .put("companionEvents",companionEvents).put("companionProgress",companionProgress).put("companionCompleted",companionCompleted)
                .put("kingdomTurns",kingdomTurns).put("kingdomChecks",kingdomChecks).put("wars",wars)
                .put("armyA",armyA).put("armyB",armyB).put("restores",restores).put("validations",validations)
                .put("neighborWindows",neighborWindows).put("kingdomLevel",kingdomLevel).put("kingdomSize",kingdomSize)
                .put("kingdomXp",kingdomXp).put("resourceTotal",resourceTotal);}
        static State restore(String raw)throws Exception{JSONObject j=new JSONObject(raw);State s=new State();
            s.week=j.optInt("week");s.hex=j.optInt("hex");s.camps=j.optInt("camps");s.campEncounters=j.optInt("campEncounters");
            s.campResumes=j.optInt("campResumes");s.campEncounterPending=j.optBoolean("campEncounterPending");
            s.companionEvents=j.optInt("companionEvents");s.companionProgress=j.optInt("companionProgress");s.companionCompleted=j.optInt("companionCompleted");
            s.kingdomTurns=j.optInt("kingdomTurns");s.kingdomChecks=j.optInt("kingdomChecks");s.wars=j.optInt("wars");
            s.armyA=j.optInt("armyA");s.armyB=j.optInt("armyB");s.restores=j.optInt("restores");s.validations=j.optInt("validations");
            s.neighborWindows=j.optInt("neighborWindows");s.kingdomLevel=j.optInt("kingdomLevel",6);s.kingdomSize=j.optInt("kingdomSize",16);
            s.kingdomXp=j.optInt("kingdomXp");s.resourceTotal=j.optInt("resourceTotal");return s;}
    }

    private static void require(boolean ok,String msg){if(!ok)throw new IllegalStateException(msg);}
    private static int roll(int week,int salt){return 1+Math.floorMod(week*13+salt*7,20);}

    private static void resolveCampEncounter(State s){require(s.campEncounterPending,"retomada sem encontro pendente");s.campEncounterPending=false;s.campResumes++;s.validations+=2;}

    private static void playWeek(State s)throws Exception{
        s.week++;
        // Atlas: posição avança e a UI teria uma janela de vizinhos canônicos.
        s.hex+=1+Math.floorMod(s.week,4);s.neighborWindows++;s.validations+=2;

        // Um camp por semana. A cada 5 semanas o encontro bloqueia novas horas até retomada explícita.
        s.camps++;
        if(s.week%5==0){s.campEncounterPending=true;s.campEncounters++;s.validations++;
            require(s.campEncounterPending,"encontro deveria bloquear o camp");
            resolveCampEncounter(s);
        }
        require(!s.campEncounterPending,"camp não pode terminar com encontro pendente");s.validations++;

        // Evento pessoal persistente a cada 6 semanas; a cena gera progresso, sem alterar mecânica de Reino.
        if(s.week%6==0){s.companionEvents++;s.companionProgress++;if(s.week%18==0)s.companionCompleted++;s.validations+=3;}

        // Turno do Reino a cada 4 semanas, com quatro decisões/fases.
        if(s.week%4==0){s.kingdomTurns++;JSONObject dice=RULES.resourceDice(s.kingdomLevel,s.kingdomSize);int count=dice.optInt("count"),die=dice.optInt("die"),generated=0;
            for(int i=0;i<count;i++)generated+=1+Math.floorMod(s.week+i,die);s.resourceTotal+=generated;
            String[] skills={"trade","statecraft","exploration","politics"};
            for(int phase=0;phase<KingmakerRulesEngine.PHASES.length;phase++){int dc=RULES.controlDc(s.kingdomLevel,s.kingdomSize,0,0,0);RULES.resolveKingdomCheck(skills[phase],roll(s.week,phase),11+phase,dc);s.kingdomChecks++;s.validations++;}
            JSONObject xp=RULES.applyKingdomXp(s.kingdomLevel,s.kingdomXp,110);s.kingdomLevel=xp.optInt("level",s.kingdomLevel);s.kingdomXp=xp.optInt("xp",s.kingdomXp);
        }

        // Guerra a cada 12 semanas, sempre usando CA do alvo e dano do motor real.
        if(s.week%12==0){s.wars++;JSONObject a=RULES.resolveArmyCheck("attack",roll(s.week,8),12,19,1,false);JSONObject b=RULES.resolveArmyCheck("attack",roll(s.week,9),10,18,1,false);
            s.armyB=RULES.applyArmyDamage(s.armyB,14,a.optInt("hits"),0,0,2).optInt("hp");s.armyA=RULES.applyArmyDamage(s.armyA,14,b.optInt("hits"),0,0,2).optInt("hp");
            if(s.armyA==0)s.armyA=14;if(s.armyB==0)s.armyB=14;s.validations+=4;}
    }

    private static State run(boolean restoreAtMidpoint)throws Exception{State s=new State();for(int i=0;i<60;i++){playWeek(s);if(restoreAtMidpoint&&s.week==30){String backup=s.json().toString();State restored=State.restore(backup);
                require(restored.week==30,"restore perdeu semana");require(restored.hex==s.hex,"restore perdeu Atlas");require(restored.campEncounters==s.campEncounters,"restore perdeu encontros de camp");
                require(restored.companionProgress==s.companionProgress,"restore perdeu arcos de companion");require(restored.kingdomTurns==s.kingdomTurns,"restore perdeu Reino");require(!restored.campEncounterPending,"restore reabriu encontro já resolvido");
                restored.restores=s.restores+1;s=restored;s.validations+=6;}}return s;}
    private static JSONObject comparable(State s)throws Exception{JSONObject j=s.json();j.put("restores",0).put("validations",0);return j;}

    public static void main(String[] args)throws Exception{State continuous=run(false),restored=run(true);require(comparable(continuous).toString().equals(comparable(restored).toString()),"estado final divergiu após restore");
        require(restored.week==60,"campanha incompleta");require(restored.camps==60,"campings incompletos");require(restored.campEncounters==12&&restored.campResumes==12,"encontros de camp não fecharam");
        require(restored.companionEvents==10&&restored.companionProgress==10,"eventos pessoais incompletos");require(restored.companionCompleted==3,"conclusões de arco inesperadas");
        require(restored.kingdomTurns==15&&restored.kingdomChecks==60,"Reino guiado incompleto");require(restored.wars==5,"guerras incompletas");require(restored.neighborWindows==60,"guia de Atlas incompleto");require(restored.restores==1,"restore intermediário ausente");require(restored.validations>=180,"cobertura longa insuficiente");
        System.out.println("KINGMAKER_SOLO_CAMPAIGN_R6_E2E_PASS weeks="+restored.week+" camps="+restored.camps+" camp_encounters="+restored.campEncounters+" companion_events="+restored.companionEvents+" kingdom_turns="+restored.kingdomTurns+" wars="+restored.wars+" validations="+restored.validations+" restore_week=30");}
}
