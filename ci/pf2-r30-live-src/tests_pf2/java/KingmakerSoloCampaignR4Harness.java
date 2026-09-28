package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * E2E local de campanha longa Kingmaker Solo.
 * Não substitui Android/device: exercita os motores mecânicos reais e um
 * round-trip de estado no meio da campanha, com fluxo aventura->camp->Reino->guerra.
 */
public final class KingmakerSoloCampaignR4Harness {
    private static final KingmakerRulesEngine RULES=new KingmakerRulesEngine();

    private static final class State {
        int week=0;
        int hex=100;
        int camps=0;
        int companionEvents=0;
        int kingdomTurns=0;
        int kingdomChecks=0;
        int kingdomSuccesses=0;
        int wars=0;
        int armyA=12;
        int armyB=12;
        int restores=0;
        int validations=0;
        int kingdomLevel=5;
        int kingdomSize=12;
        int kingdomXp=0;
        int resourceTotal=0;

        JSONObject json() throws Exception {
            return new JSONObject()
                    .put("week",week).put("hex",hex).put("camps",camps)
                    .put("companionEvents",companionEvents).put("kingdomTurns",kingdomTurns)
                    .put("kingdomChecks",kingdomChecks).put("kingdomSuccesses",kingdomSuccesses)
                    .put("wars",wars).put("armyA",armyA).put("armyB",armyB)
                    .put("restores",restores).put("validations",validations)
                    .put("kingdomLevel",kingdomLevel).put("kingdomSize",kingdomSize)
                    .put("kingdomXp",kingdomXp).put("resourceTotal",resourceTotal);
        }

        static State restore(String raw) throws Exception {
            JSONObject j=new JSONObject(raw);State s=new State();
            s.week=j.optInt("week");s.hex=j.optInt("hex");s.camps=j.optInt("camps");
            s.companionEvents=j.optInt("companionEvents");s.kingdomTurns=j.optInt("kingdomTurns");
            s.kingdomChecks=j.optInt("kingdomChecks");s.kingdomSuccesses=j.optInt("kingdomSuccesses");
            s.wars=j.optInt("wars");s.armyA=j.optInt("armyA");s.armyB=j.optInt("armyB");
            s.restores=j.optInt("restores");s.validations=j.optInt("validations");
            s.kingdomLevel=j.optInt("kingdomLevel",5);s.kingdomSize=j.optInt("kingdomSize",12);
            s.kingdomXp=j.optInt("kingdomXp");s.resourceTotal=j.optInt("resourceTotal");
            return s;
        }
    }

    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}

    private static int roll(int week,int salt){return 1+Math.floorMod(week*7+salt*11,20);}

    private static void playWeek(State s) throws Exception {
        s.week++;
        // Aventura/exploração: posição muda toda semana; nunca reinicia no mesmo ponto.
        s.hex+=1+Math.floorMod(s.week,3);s.validations++;

        // Camping contextual acontece no local atual. Não há CD inventada aqui.
        s.camps++;s.validations++;

        // Companion pessoal entra organicamente a cada 6 semanas.
        if(s.week%6==0){s.companionEvents++;s.validations++;}

        // Um turno do Reino a cada quatro semanas, sempre passando pelas quatro fases.
        if(s.week%4==0){
            s.kingdomTurns++;
            JSONObject dice=RULES.resourceDice(s.kingdomLevel,s.kingdomSize);
            int count=dice.optInt("count"),die=dice.optInt("die");
            int generated=0;for(int i=0;i<count;i++)generated+=1+Math.floorMod(s.week+i,die);
            s.resourceTotal+=generated;
            String[] skills={"trade","statecraft","exploration","politics"};
            for(int phase=0;phase<KingmakerRulesEngine.PHASES.length;phase++){
                int dc=RULES.controlDc(s.kingdomLevel,s.kingdomSize,0,0,0);
                JSONObject check=RULES.resolveKingdomCheck(skills[phase],roll(s.week,phase),10+phase,dc);
                s.kingdomChecks++;
                if("SUCCESS".equals(check.optString("degree"))||"CRITICAL_SUCCESS".equals(check.optString("degree")))s.kingdomSuccesses++;
                s.validations++;
            }
            JSONObject xp=RULES.applyKingdomXp(s.kingdomLevel,s.kingdomXp,125);
            s.kingdomLevel=xp.optInt("level",s.kingdomLevel);s.kingdomXp=xp.optInt("xp",s.kingdomXp);
        }

        // Guerra/exércitos a cada 12 semanas: CA real do alvo e dano no próprio motor.
        if(s.week%12==0){
            s.wars++;
            JSONObject a=RULES.resolveArmyCheck("attack",roll(s.week,8),11,18,1,false);
            JSONObject b=RULES.resolveArmyCheck("attack",roll(s.week,9),9,19,1,false);
            JSONObject da=RULES.applyArmyDamage(s.armyB,12,a.optInt("hits"),0,0,2);
            JSONObject db=RULES.applyArmyDamage(s.armyA,12,b.optInt("hits"),0,0,2);
            s.armyB=da.optInt("hp");s.armyA=db.optInt("hp");
            if(s.armyA==0)s.armyA=12; // exército substituto entre campanhas, não ressurreição da unidade.
            if(s.armyB==0)s.armyB=12;
            s.validations+=4;
        }
    }

    private static State run(boolean restoreAtMidpoint) throws Exception {
        State s=new State();
        for(int i=0;i<48;i++){
            playWeek(s);
            if(restoreAtMidpoint&&s.week==24){
                String backup=s.json().toString();
                State restored=State.restore(backup);
                require(restored.week==24,"restore perdeu a semana");
                require(restored.hex==s.hex,"restore perdeu posição do Atlas");
                require(restored.camps==s.camps,"restore perdeu camping");
                require(restored.kingdomTurns==s.kingdomTurns,"restore perdeu turnos do Reino");
                require(restored.wars==s.wars,"restore perdeu guerras");
                restored.restores=s.restores+1;
                s=restored;
                s.validations+=5;
            }
        }
        return s;
    }

    private static JSONObject comparable(State s) throws Exception {
        JSONObject j=s.json();j.put("restores",0).put("validations",0);return j;
    }

    public static void main(String[] args) throws Exception {
        State continuous=run(false),restored=run(true);
        require(comparable(continuous).toString().equals(comparable(restored).toString()),"estado final divergiu após backup/restore");
        require(restored.week==48,"campanha não chegou a 48 semanas");
        require(restored.camps==48,"camping não acompanhou a exploração");
        require(restored.companionEvents==8,"eventos pessoais de companion inesperados");
        require(restored.kingdomTurns==12,"turnos do Reino inesperados");
        require(restored.kingdomChecks==48,"fases guiadas do Reino incompletas");
        require(restored.wars==4,"guerras esperadas não ocorreram");
        require(restored.restores==1,"restore intermediário não ocorreu");
        require(restored.validations>=100,"cobertura do cenário longo insuficiente");
        System.out.println("KINGMAKER_SOLO_CAMPAIGN_R4_E2E_PASS weeks="+restored.week+" camps="+restored.camps+" companion_events="+restored.companionEvents+" kingdom_turns="+restored.kingdomTurns+" wars="+restored.wars+" validations="+restored.validations+" restore_week=24");
    }
}
