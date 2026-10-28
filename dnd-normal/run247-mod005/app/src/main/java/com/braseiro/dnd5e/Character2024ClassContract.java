package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;

/**
 * PHB 2024: the two starting save proficiencies are fixed by the INITIAL class.
 * This is a mechanical invariant, not a choice for random generation.
 * Higher-level and multiclass exceptions are deliberately outside this level-1 gate.
 * Reference: https://www.dndbeyond.com/sources/dnd/br-2024/character-classes
 */
public final class Character2024ClassContract {
    private Character2024ClassContract() {}
    private static String norm(String s){
        return Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ROOT),
            Normalizer.Form.NFD).replaceAll("\\p{M}","").trim();
    }
    public static String[] initialSavingThrows(String rawClass) {
        String cls=norm(rawClass);
        int subclass=cls.indexOf(" — ");
        if(subclass>=0)cls=cls.substring(0,subclass);
        switch(cls){
            case "barbaro": return new String[]{"Força","Constituição"};
            case "bardo": return new String[]{"Destreza","Carisma"};
            case "bruxo": return new String[]{"Sabedoria","Carisma"};
            case "clerigo": return new String[]{"Sabedoria","Carisma"};
            case "druida": return new String[]{"Inteligência","Sabedoria"};
            case "feiticeiro": return new String[]{"Constituição","Carisma"};
            case "guerreiro": return new String[]{"Força","Constituição"};
            case "ladino": return new String[]{"Destreza","Inteligência"};
            case "mago": return new String[]{"Inteligência","Sabedoria"};
            case "monge": return new String[]{"Força","Destreza"};
            case "paladino": return new String[]{"Sabedoria","Carisma"};
            case "patrulheiro": return new String[]{"Força","Destreza"};
            default: return null; // extensions must supply their own verified contract
        }
    }
    public static boolean hasCorrectLevelOneSaves(JSONObject sheet) {
        if(sheet==null || sheet.optInt("level",1)!=1)return true;
        String[] required=initialSavingThrows(sheet.optString("class_name"));
        if(required==null)return true;
        JSONArray rows=sheet.optJSONArray("saving_throw_proficiencies");
        if(rows==null||rows.length()!=2)return false;
        HashSet<String> actual=new HashSet<>();
        for(int i=0;i<rows.length();i++){
            JSONObject row=rows.optJSONObject(i);
            String name=row!=null?row.optString("name"):rows.optString(i);
            actual.add(norm(name));
        }
        return actual.size()==2 && actual.contains(norm(required[0])) && actual.contains(norm(required[1]));
    }
    public static boolean isGeneratedLevelOne(JSONObject sheet){
        return sheet!=null && sheet.optInt("level",1)==1 &&
            "mechanically_valid".equals(sheet.optString("generation_status")) &&
            DndRulesEngine.PROFILE_2024.equals(DndRulesEngine.canonicalProfile(sheet.optString("rules_source"))) &&
            initialSavingThrows(sheet.optString("class_name"))!=null;
    }
}
