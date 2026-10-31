package com.braseiro.dnd5e;

import java.text.Normalizer;
import java.util.Locale;
import org.json.JSONObject;

/** D&D 2024 alignment: optional characterization, never a mechanical rule or forced choice. */
public final class AlignmentNarrativePolicy {
    private AlignmentNarrativePolicy() {}

    public static String canonical(String raw) {
        if(raw==null)return "";
        String value=Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
                .replaceAll("\\p{M}","").replaceAll("[^a-z ]"," ").replaceAll("\\s+"," ").trim();
        if(value.equals("leal e bom")||value.equals("lawful good"))return "Leal e Bom";
        if(value.equals("neutro e bom")||value.equals("neutral good"))return "Neutro e Bom";
        if(value.equals("caotico e bom")||value.equals("chaotic good"))return "Caótico e Bom";
        if(value.equals("leal e neutro")||value.equals("lawful neutral"))return "Leal e Neutro";
        if(value.equals("neutro")||value.equals("true neutral")||value.equals("neutral"))return "Neutro";
        if(value.equals("caotico e neutro")||value.equals("chaotic neutral"))return "Caótico e Neutro";
        if(value.equals("leal e mau")||value.equals("lawful evil"))return "Leal e Mau";
        if(value.equals("neutro e mau")||value.equals("neutral evil"))return "Neutro e Mau";
        if(value.equals("caotico e mau")||value.equals("chaotic evil"))return "Caótico e Mau";
        return "";
    }

    /** No defaults or new persistent state: use only authoritatively supplied NPC facts. */
    public static String fromNpcMemory(JSONObject npc) {
        if(npc==null)return "";
        String direct=canonical(npc.optString("alignment",""));
        if(!direct.isEmpty())return direct;
        Object raw=npc.opt("stats_json");
        try{
            JSONObject stats=raw instanceof JSONObject?(JSONObject)raw:new JSONObject(raw==null?"{}":String.valueOf(raw));
            return canonical(stats.optString("alignment",""));
        }catch(Exception ignored){return "";}
    }
}
