package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** Regras OPCIONAIS de refeicoes originais, sem alterar D&D 2024 RAW. */
public final class WilemCookbookEffectRules {
    private WilemCookbookEffectRules(){}
    private static String norm(String s){
        return Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","")
          .toLowerCase(Locale.ROOT).replaceAll("[^a-z]+"," ").trim();
    }
    private static JSONObject sheet(JSONObject hero){
        if(hero==null)return new JSONObject();
        try{return new JSONObject(hero.optString("sheet_json","{}"));}catch(Exception ignored){return new JSONObject();}
    }
    public static double worldHour(JSONObject scene){
        if(scene==null)return 12d;
        return Math.max(0,scene.optInt("day_number",1)-1)*24d+scene.optDouble("clock_hour",12d);
    }
    public static int skillBonus(JSONObject hero,String ability,String skill,JSONObject scene){
        if(hero==null||scene==null)return 0;
        String n=norm(skill),a=norm(ability);if(n.isEmpty())return 0;
        double hour=worldHour(scene);
        JSONArray effects=sheet(hero).optJSONArray("meal_effects");int best=0;
        for(int i=0;effects!=null&&i<effects.length();i++){
            JSONObject e=effects.optJSONObject(i);if(e==null||e.optDouble("ends_world_hour",0)<=hour)continue;
            String key=e.optString("effect_key");boolean eligible=false;
            if(key.equals("sobrevivencia"))eligible=n.contains("sobreviv")||n.contains("survival");
            else if(key.equals("percepcao"))eligible=n.contains("percepc")||n.contains("perception");
            else if(key.equals("agilidade"))eligible=n.contains("acrobac")||n.contains("acrobat");
            else if(key.equals("vigor"))eligible=n.contains("atlet")||n.contains("athletic");
            if(eligible)best=Math.max(best,Math.min(5,e.optInt("effect_value",0)));
        }
        return best;
    }
    public static int concentrationSaveBonus(JSONObject hero,JSONObject scene){
        return typedBonus(hero,scene,"concentracao");
    }
    public static boolean coldResistance(JSONObject hero,JSONObject scene){
        return typedBonus(hero,scene,"resistencia_frio")>0;
    }
    private static int typedBonus(JSONObject hero,JSONObject scene,String key){
        if(hero==null||scene==null)return 0;
        double hour=worldHour(scene);JSONArray effects=sheet(hero).optJSONArray("meal_effects");int best=0;
        for(int i=0;effects!=null&&i<effects.length();i++){
            JSONObject e=effects.optJSONObject(i);if(e!=null&&key.equals(e.optString("effect_key"))&&
               e.optDouble("ends_world_hour",0)>hour)best=Math.max(best,Math.min(5,e.optInt("effect_value",0)));
        }
        return best;
    }
}
