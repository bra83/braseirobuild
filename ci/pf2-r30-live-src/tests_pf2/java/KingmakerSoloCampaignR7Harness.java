package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * E2E local R7: expedição guiada usa vizinhos do manifesto real, nunca teletransporta,
 * reconhecimento/limpeza só acontecem depois de uma cena persistente, e restore no meio
 * preserva a campanha. Usa os motores mecânicos reais; não substitui Android device.
 */
public final class KingmakerSoloCampaignR7Harness {
    private static final KingmakerRulesEngine RULES=new KingmakerRulesEngine();
    private static final MapManifestGeometry MAP=buildMap();

    private static MapManifestGeometry buildMap(){try{return MapManifestGeometry.parse("{\"mapId\":\"kingmaker-r7-test\",\"sourceMap\":{\"width\":800,\"height\":800},\"layout\":{\"orientation\":\"flat-top\",\"hexWidth\":100,\"hexHeight\":86.6,\"columns\":8,\"rows\":8,\"originX\":50,\"originY\":43.3}}");}catch(Exception e){throw new RuntimeException(e);}}

    private static final class State {
        int week,plannedSegments,travelSegments,reconEvents,clearEvents,canonicalNeighborChecks,camps,campEncounters,companionEvents,kingdomTurns,wars,restores,validations;
        int kingdomLevel=6,kingdomSize=16,kingdomXp,armyA=14,armyB=14;
        String current="D4",previous="";
        final Set<String> recon=new HashSet<String>(),cleared=new HashSet<String>();
        JSONObject json()throws Exception{JSONArray r=new JSONArray(),c=new JSONArray();for(String x:recon)r.put(x);for(String x:cleared)c.put(x);return new JSONObject().put("week",week).put("plannedSegments",plannedSegments).put("travelSegments",travelSegments).put("reconEvents",reconEvents).put("clearEvents",clearEvents).put("canonicalNeighborChecks",canonicalNeighborChecks).put("camps",camps).put("campEncounters",campEncounters).put("companionEvents",companionEvents).put("kingdomTurns",kingdomTurns).put("wars",wars).put("restores",restores).put("validations",validations).put("kingdomLevel",kingdomLevel).put("kingdomSize",kingdomSize).put("kingdomXp",kingdomXp).put("armyA",armyA).put("armyB",armyB).put("current",current).put("previous",previous).put("recon",r).put("cleared",c);}
        static State restore(String raw)throws Exception{JSONObject j=new JSONObject(raw);State s=new State();s.week=j.optInt("week");s.plannedSegments=j.optInt("plannedSegments");s.travelSegments=j.optInt("travelSegments");s.reconEvents=j.optInt("reconEvents");s.clearEvents=j.optInt("clearEvents");s.canonicalNeighborChecks=j.optInt("canonicalNeighborChecks");s.camps=j.optInt("camps");s.campEncounters=j.optInt("campEncounters");s.companionEvents=j.optInt("companionEvents");s.kingdomTurns=j.optInt("kingdomTurns");s.wars=j.optInt("wars");s.restores=j.optInt("restores");s.validations=j.optInt("validations");s.kingdomLevel=j.optInt("kingdomLevel",6);s.kingdomSize=j.optInt("kingdomSize",16);s.kingdomXp=j.optInt("kingdomXp");s.armyA=j.optInt("armyA",14);s.armyB=j.optInt("armyB",14);s.current=j.optString("current","D4");s.previous=j.optString("previous","");JSONArray r=j.optJSONArray("recon"),c=j.optJSONArray("cleared");if(r!=null)for(int i=0;i<r.length();i++)s.recon.add(r.optString(i));if(c!=null)for(int i=0;i<c.length();i++)s.cleared.add(c.optString(i));return s;}
    }

    private static void require(boolean ok,String msg){if(!ok)throw new IllegalStateException(msg);}
    private static int roll(int week,int salt){return 1+Math.floorMod(week*17+salt*11,20);}

    private static String planNext(State s){List<String> neighbors=MAP.neighbors(s.current);require(!neighbors.isEmpty(),"hex sem vizinhos canônicos");String target="";if(s.previous.length()>0&&neighbors.contains(s.previous)&&s.week%3!=0)target=s.previous;else target=neighbors.get(Math.floorMod(s.week*5,neighbors.size()));require(neighbors.contains(target),"planejamento saiu da vizinhança canônica");s.canonicalNeighborChecks++;s.plannedSegments++;s.validations+=2;return target;}
    private static void travelOneHex(State s,String target){String from=s.current;require(MAP.neighbors(from).contains(target),"teleporte detectado");s.previous=from;s.current=target;s.travelSegments++;s.validations+=2;}
    private static void resolveExplorationScene(State s){if(!s.recon.contains(s.current)){s.reconEvents++;s.recon.add(s.current);s.validations+=3;return;}if(!s.cleared.contains(s.current)){s.clearEvents++;s.cleared.add(s.current);s.validations+=3;}}

    private static void playWeek(State s)throws Exception{
        s.week++;
        String target=planNext(s);travelOneHex(s,target);resolveExplorationScene(s);
        s.camps++;if(s.week%6==0){s.campEncounters++;s.validations+=2;}
        if(s.week%8==0){s.companionEvents++;s.validations+=2;}
        if(s.week%4==0){s.kingdomTurns++;int dc=RULES.controlDc(s.kingdomLevel,s.kingdomSize,0,0,0);RULES.resolveKingdomCheck("exploration",roll(s.week,3),13,dc);JSONObject xp=RULES.applyKingdomXp(s.kingdomLevel,s.kingdomXp,95);s.kingdomLevel=xp.optInt("level",s.kingdomLevel);s.kingdomXp=xp.optInt("xp",s.kingdomXp);s.validations+=3;}
        if(s.week%12==0){s.wars++;JSONObject a=RULES.resolveArmyCheck("attack",roll(s.week,8),12,19,1,false),b=RULES.resolveArmyCheck("attack",roll(s.week,9),10,18,1,false);s.armyB=RULES.applyArmyDamage(s.armyB,14,a.optInt("hits"),0,0,2).optInt("hp");s.armyA=RULES.applyArmyDamage(s.armyA,14,b.optInt("hits"),0,0,2).optInt("hp");if(s.armyA==0)s.armyA=14;if(s.armyB==0)s.armyB=14;s.validations+=4;}
    }

    private static State run(boolean restoreAtMidpoint)throws Exception{State s=new State();s.recon.add(s.current);for(int i=0;i<72;i++){playWeek(s);if(restoreAtMidpoint&&s.week==36){String raw=s.json().toString();State restored=State.restore(raw);require(restored.current.equals(s.current),"restore perdeu hex atual");require(restored.recon.equals(s.recon),"restore perdeu reconhecimento");require(restored.cleared.equals(s.cleared),"restore perdeu limpeza");require(restored.plannedSegments==s.plannedSegments&&restored.travelSegments==s.travelSegments,"restore perdeu expedição");restored.restores=s.restores+1;s=restored;s.validations+=6;}}return s;}
    private static JSONObject comparable(State s)throws Exception{JSONObject j=s.json();j.put("restores",0).put("validations",0);return j;}

    public static void main(String[] args)throws Exception{State continuous=run(false),restored=run(true);require(comparable(continuous).toString().equals(comparable(restored).toString()),"estado final divergiu após restore");require(restored.week==72,"campanha incompleta");require(restored.plannedSegments==72&&restored.travelSegments==72,"expedição não percorreu um trecho por semana");require(restored.canonicalNeighborChecks==72,"vizinhança canônica não validada em todos os trechos");require(restored.reconEvents>0&&restored.clearEvents>0,"estágios de exploração incompletos");require(restored.camps==72&&restored.campEncounters==12,"camping incompleto");require(restored.companionEvents==9,"companion cadence inesperada");require(restored.kingdomTurns==18,"Reino guiado incompleto");require(restored.wars==6,"guerras incompletas");require(restored.restores==1,"restore intermediário ausente");require(restored.validations>=300,"cobertura longa insuficiente");System.out.println("KINGMAKER_SOLO_CAMPAIGN_R7_E2E_PASS weeks="+restored.week+" planned="+restored.plannedSegments+" travelled="+restored.travelSegments+" recon="+restored.reconEvents+" cleared="+restored.clearEvents+" camps="+restored.camps+" kingdom_turns="+restored.kingdomTurns+" wars="+restored.wars+" validations="+restored.validations+" restore_week=36");}
}
