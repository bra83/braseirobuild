package com.braseiro.pathfinder2e;

import org.json.JSONObject;

import java.io.File;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Barreira mecânica do Mundo Vivo.
 *
 * Atividades cotidianas podem sempre produzir passagem de tempo e ficção.
 * Alterações de ficha, inventário, moedas, XP ou recursos só são permitidas
 * quando uma regra identificável e pertencente ao mundo ativo foi registrada.
 */
public final class LivingWorldRulePolicy {
    private LivingWorldRulePolicy(){}

    public static boolean sourceBelongsToWorld(String worldProfile,String source){
        String profile=worldProfile==null?"":worldProfile.trim().toLowerCase(Locale.US);
        if(!(profile.equals("pf2")||profile.equals("pathfinder_2e")||profile.equals("pathfinder2e")))return false;
        String normalizedPath=(source==null?"":source.trim()).replace('\\','/');
        String name=new File(normalizedPath).getName();
        return name.length()>0&&ManualIdentityPolicy.isPf2AuthoritativeTitle(name)&&!ManualIdentityPolicy.isExternalSystemTitle(name);
    }

    public static boolean allowsMechanicalOutcome(String worldProfile,boolean authorized,String source,int page,String excerpt){
        return authorized&&page>0&&excerpt!=null&&excerpt.trim().length()>=8&&sourceBelongsToWorld(worldProfile,source);
    }

    public static JSONObject sanitizeMechanicalOutcome(String worldProfile,boolean authorized,String source,int page,String excerpt,JSONObject proposed){
        if(!allowsMechanicalOutcome(worldProfile,authorized,source,page,excerpt)||proposed==null)return new JSONObject();
        JSONObject safe=new JSONObject();
        copyNumber(proposed,safe,"gold_delta");
        copyNumber(proposed,safe,"xp_delta");
        copyNumber(proposed,safe,"provisions_delta");
        copyNumber(proposed,safe,"water_delta");
        copyNumber(proposed,safe,"downtime_days");
        copyText(proposed,safe,"item_add");
        copyText(proposed,safe,"item_remove");
        copyText(proposed,safe,"resource_key");
        copyNumber(proposed,safe,"resource_delta");
        return safe;
    }

    public static int ageYears(int initialAgeYears,int birthDay,int campaignDay,int daysPerYear){
        int yearLength=Math.max(1,daysPerYear);
        int elapsed=Math.max(0,campaignDay-birthDay);
        return Math.max(0,initialAgeYears)+(elapsed/yearLength);
    }

    public static int birthDayForAge(int currentAgeYears,int campaignDay,int daysPerYear){
        long value=(long)Math.max(1,campaignDay)-(long)Math.max(0,currentAgeYears)*Math.max(1,daysPerYear);
        return (int)Math.max(Integer.MIN_VALUE,Math.min(Integer.MAX_VALUE,value));
    }

    public static String normalizedActivityType(String value){
        String n=normalize(value);
        if(n.contains("cac"))return "caca";
        if(n.contains("pesc"))return "pesca";
        if(n.contains("colet")||n.contains("forrage"))return "coleta";
        if(n.contains("trabalh")||n.contains("oficio")||n.contains("artesan"))return "trabalho";
        if(n.contains("estud")||n.contains("trein"))return "estudo";
        if(n.contains("pesquis"))return "pesquisa";
        if(n.contains("comerc")||n.contains("negoci"))return "comercio";
        if(n.contains("administr"))return "administracao";
        if(n.contains("viaj")||n.contains("itiner"))return "viagem_itinerante";
        if(n.contains("social")||n.contains("famil")||n.contains("relacion"))return "vida_social";
        if(n.contains("descans"))return "descanso";
        return "livre";
    }

    public static boolean isMechanicalJson(JSONObject value){return value!=null&&value.length()>0;}

    private static String normalize(String value){
        String n=Normalizer.normalize(value==null?"":value.toLowerCase(new Locale("pt","BR")),Normalizer.Form.NFD).replaceAll("\\p{M}","");
        return n.replaceAll("[^a-z0-9]+"," ").trim();
    }
    private static void copyText(JSONObject from,JSONObject to,String key){try{if(from.has(key)&&from.optString(key).trim().length()>0)to.put(key,from.optString(key).trim());}catch(Exception ignored){AppLog.ignored("LivingWorldRulePolicy","Campo mecânico textual descartado.",ignored);}}
    private static void copyNumber(JSONObject from,JSONObject to,String key){try{if(from.has(key)&&from.opt(key) instanceof Number)to.put(key,from.optDouble(key));}catch(Exception ignored){AppLog.ignored("LivingWorldRulePolicy","Campo mecânico numérico descartado.",ignored);}}
}
