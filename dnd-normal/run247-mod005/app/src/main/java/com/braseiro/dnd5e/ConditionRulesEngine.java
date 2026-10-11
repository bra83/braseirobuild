package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** Efeitos determinísticos das condições nucleares de D&D. */
public final class ConditionRulesEngine {
    private ConditionRulesEngine(){}

    public static boolean has(String conditions,String wanted){
        return ConditionTokenGuard.has(conditions,wanted);
    }
    public static boolean incapacitated(String c){return has(c,"incapacitado")||has(c,"inconsciente")||has(c,"paralisado")||has(c,"petrificado")||has(c,"aturdido");}
    public static boolean speedZero(String c){return has(c,"agarrado")||has(c,"impedido")||has(c,"paralisado")||has(c,"petrificado")||has(c,"aturdido")||has(c,"inconsciente");}
    public static boolean breaksConcentration(String c){return incapacitated(c);}
    public static boolean sightCheckAutoFail(String c){return has(c,"cego")||has(c,"inconsciente");}
    public static boolean hearingCheckAutoFail(String c){return has(c,"surdo")||has(c,"inconsciente");}
    public static int standMovementCost(int speedFt){int safe=Math.max(0,speedFt);return safe/2+safe%2;}
    public static boolean canTargetCharmer(String c,boolean targetIsCharmer){return !targetIsCharmer||!has(c,"enfeiticado");}
    public static boolean canApproachFearSource(String c,boolean movingCloser,boolean fearSourceVisible){return !(movingCloser&&fearSourceVisible&&has(c,"amedrontado"));}
    public static boolean poisonImmune(String c){return has(c,"petrificado");}
    public static boolean resistantToAllDamage(String c){return has(c,"petrificado");}

    /** Perfil genérico de ataque. Vantagem e desvantagem se cancelam na resolução normal do motor. */
    public static JSONObject attackProfile(String attackerConditions,String targetConditions,double distanceFt,boolean attackerCanSeeTarget,boolean targetCanSeeAttacker,boolean fearSourceVisible,boolean targetIsGrappler){return attackProfile(attackerConditions,targetConditions,distanceFt,attackerCanSeeTarget,targetCanSeeAttacker,fearSourceVisible,targetIsGrappler,DndRulesEngine.PROFILE_2024);}
    public static JSONObject attackProfile(String attackerConditions,String targetConditions,double distanceFt,boolean attackerCanSeeTarget,boolean targetCanSeeAttacker,boolean fearSourceVisible,boolean targetIsGrappler,String rulesProfile){
        JSONObject out=new JSONObject();try{
            boolean adv=false,dis=false,criticalOnHit=false,blocked=false;
            if(incapacitated(attackerConditions))blocked=true;
            if(has(attackerConditions,"envenenado")||has(attackerConditions,"impedido")||has(attackerConditions,"caido"))dis=true;
            if(has(attackerConditions,"amedrontado")&&fearSourceVisible)dis=true;
            if(has(attackerConditions,"cego")||!attackerCanSeeTarget)dis=true;
            if(has(attackerConditions,"invisivel")&&!targetCanSeeAttacker)adv=true;
            if(has(attackerConditions,"agarrado")&&!targetIsGrappler&&new DndRulesEngine().is2024(rulesProfile))dis=true;

            if(has(targetConditions,"cego")||has(targetConditions,"impedido")||has(targetConditions,"paralisado")||has(targetConditions,"aturdido")||has(targetConditions,"inconsciente"))adv=true;
            if(has(targetConditions,"invisivel")&&!attackerCanSeeTarget)dis=true;
            if(has(targetConditions,"caido")){if(distanceFt<=5d)adv=true;else dis=true;}
            if(has(targetConditions,"paralisado")||has(targetConditions,"inconsciente"))criticalOnHit=distanceFt<=5d;
            out.put("advantage",adv).put("disadvantage",dis).put("blocked",blocked).put("critical_on_hit",criticalOnHit);
        }catch(Exception ignored){AppLog.ignored("ConditionRulesEngine","Falha ao calcular efeito de condição em ataque.",ignored);}return out;
    }

    public static JSONObject savingThrowProfile(String conditions,String ability){
        JSONObject out=new JSONObject();try{String a=norm(ability);boolean str=a.startsWith("for")||a.contains("strength"),dex=a.startsWith("des")||a.contains("dexterity");boolean autoFail=(str||dex)&&(has(conditions,"paralisado")||has(conditions,"petrificado")||has(conditions,"aturdido")||has(conditions,"inconsciente"));boolean dis=dex&&has(conditions,"impedido");out.put("automatic_failure",autoFail).put("disadvantage",dis);}catch(Exception ignored){}return out;
    }
    public static JSONObject abilityCheckProfile(String conditions,String ability,String skill,boolean fearSourceVisible){
        JSONObject out=new JSONObject();try{boolean autoFail=(sightCheckAutoFail(conditions)&&isSightDependent(skill))||(hearingCheckAutoFail(conditions)&&isHearingDependent(skill));boolean dis=has(conditions,"envenenado")||(has(conditions,"amedrontado")&&fearSourceVisible);out.put("automatic_failure",autoFail).put("disadvantage",dis);}catch(Exception ignored){}return out;
    }
    public static JSONObject socialInteractionProfile(String actorConditions,String targetConditions,boolean actorIsCharmer){JSONObject out=new JSONObject();try{out.put("advantage",actorIsCharmer&&has(targetConditions,"enfeiticado")).put("actor_speech_blocked",incapacitated(actorConditions));}catch(Exception ignored){}return out;}
    public static JSONObject initiativeProfile(String conditions){JSONObject out=new JSONObject();try{out.put("advantage",has(conditions,"invisivel")).put("disadvantage",incapacitated(conditions));}catch(Exception ignored){}return out;}

    public static String remove(String conditions,String condition){String wanted=canonical(condition);StringBuilder b=new StringBuilder();for(String part:(conditions==null?"":conditions).split("[,;•/\\n]+")){String p=part.trim();if(p.length()==0||canonical(p).equals(wanted))continue;if(b.length()>0)b.append(", ");b.append(p);}return b.toString();}
    public static String add(String conditions,String condition){String c=condition==null?"":condition.trim();if(c.length()==0||has(conditions,c))return conditions==null?"":conditions;String base=conditions==null?"":conditions.trim();return base.length()==0?c:base+", "+c;}

    private static boolean isSightDependent(String skill){String s=norm(skill);return s.contains("percepc")||s.contains("investig")||s.contains("observar")||s.contains("visao");}
    private static boolean isHearingDependent(String skill){String s=norm(skill);return s.contains("percepc")||s.contains("ouvir")||s.contains("audicao")||s.contains("som");}
    private static String canonical(String v){return ConditionTokenGuard.canonical(v);}

    private static String norm(String v){if(v==null)return "";String n=Normalizer.normalize(v.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");return n.replaceAll("[^a-z0-9]+"," ").trim();}
}
