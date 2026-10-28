package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Narrow compatibility repair for provably stale foreign-species mechanics.
 * The player-owned biography, personality, equipment, spells, HP and legacy
 * backup are never rewritten. Only known generated defaults are repaired.
 */
public final class Character2024SheetConsistencyGuard {
    private Character2024SheetConsistencyGuard(){}

    private static String normalize(String s){
        return Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ROOT),
            Normalizer.Form.NFD).replaceAll("\\p{M}","")
            .replaceAll("[^a-z0-9]+"," ").trim();
    }
    private static String signed(int n){return n>=0?"+"+n:String.valueOf(n);}
    private static int modifier(int score){return Math.floorDiv(score-10,2);}
    private static int pb(int level){return 2+(Math.max(1,Math.min(20,level))-1)/4;}

    public static String monkAttacks(JSONObject sheet){
        int dex=modifier(sheet.optInt("dex",10));
        int prof=pb(sheet.optInt("level",1));
        String die=sheet.optString("martial_arts_die","d6");
        if(!die.matches("d(6|8|10|12)"))die="d6";
        return "Lança | "+signed(dex+prof)+" | 1d6"+signed(dex)
            +" perfurante (versátil 1d8"+signed(dex)+") | arma de Monge"
            +"\nAdaga | "+signed(dex+prof)+" | 1"+die+signed(dex)
            +" perfurante | leve, finesse, Artes Marciais"
            +"\nDesarmado | "+signed(dex+prof)+" / CD "+(8+prof+dex)
            +" | 1"+die+signed(dex)+" contundente; agarrar/empurrar | Artes Marciais";
    }

    private static String oldGeneratedMonkAttacks(JSONObject sheet){
        int str=modifier(sheet.optInt("str",10)),dex=modifier(sheet.optInt("dex",10));
        int prof=pb(sheet.optInt("level",1));
        return "Lança | "+signed(str+prof)+" | 1d6"+signed(str)+" perfurante | versátil"
             +"\nAdaga | "+signed(dex+prof)+" | 1d4"+signed(dex)+" perfurante | finesse, leve"
             +"\nDesarmado | "+signed(str+prof)+" / CD "+(8+str+prof)
             +" | dano, agarrar ou empurrar | corpo a corpo";
    }

    private static boolean containsString(JSONArray array,String text){
        for(int i=0;i<array.length();i++)
            if(text.equalsIgnoreCase(array.optString(i)))return true;
        return false;
    }

    private static void removeKnownUrdunnir(JSONObject sheet,JSONArray repairs)throws Exception{
        JSONObject resources=sheet.optJSONObject("species_resources");
        if(resources!=null){
            resources.remove("one_with_stone");
            resources.remove("urdunnir_stone_shape");
        }
        for(String key:new String[]{"one_with_stone_unworked_only",
                "one_with_stone_carry_multiplier","stone_shape_min_level",
                "shape_metal_min_level"}){
            sheet.remove(key);
        }
        JSONArray oldTools=sheet.optJSONArray("tool_proficiencies");
        if(oldTools!=null){
            JSONArray safe=new JSONArray();
            for(int i=0;i<oldTools.length();i++){
                String t=oldTools.optString(i);
                if(!"Ferramentas de ferreiro".equalsIgnoreCase(t) &&
                   !"Ferramentas de pedreiro".equalsIgnoreCase(t)) safe.put(oldTools.opt(i));
            }
            sheet.put("tool_proficiencies",safe);
        }
        // These strings were mechanically generated, not authored history.
        String features=sheet.optString("features","");
        StringBuilder clean=new StringBuilder();
        for(String line:features.split("\\n",-1)){
            if(line.toLowerCase(Locale.ROOT).contains("urdunnir"))continue;
            if(clean.length()>0)clean.append('\n');
            clean.append(line);
        }
        if(!clean.toString().equals(features))sheet.put("features",clean.toString());
        sheet.remove("volo_subrace_run139");
        sheet.remove("volo_subrace_auto_rolls");
        sheet.remove("volo_subrace_source");
        // The biography is intentionally untouched. Flag it for player review.
        if((sheet.optString("history","")+" "+sheet.optString("life_path",""))
                .toLowerCase(Locale.ROOT).contains("urdunnir")){
            sheet.put("species_narrative_review_required",true);
        }
        if(!containsString(repairs,"removed_foreign_urdunnir_mechanics"))
            repairs.put("removed_foreign_urdunnir_mechanics");
    }

    public static JSONObject reconcile(JSONObject sheet){
        if(sheet==null||!Character2024SheetReconciler.applies(sheet))return sheet;
        try{
            JSONArray repairs=sheet.optJSONArray("species_mechanics_repair_log");
            if(repairs==null)repairs=new JSONArray();
            String species=sheet.optString("species","");
            boolean staleUrdunnir=sheet.optBoolean("volo_subrace_run139",false) &&
               !VoloSubraceRun139Engine.isReady(species) &&
               (sheet.optJSONObject("species_resources")!=null &&
                (sheet.optJSONObject("species_resources").has("one_with_stone")||
                 sheet.optJSONObject("species_resources").has("urdunnir_stone_shape")));
            if(staleUrdunnir)removeKnownUrdunnir(sheet,repairs);
            // Unknown stale Volo variants must never be silently accepted.
            if(sheet.optBoolean("volo_subrace_run139",false) &&
                    !VoloSubraceRun139Engine.isReady(species)){
                sheet.put("species_mechanics_coherence_error",
                    "Mecânicas de sub-raça de Volo não correspondem à espécie selecionada.");
            }else sheet.remove("species_mechanics_coherence_error");

            if(normalize(sheet.optString("class_name","")).startsWith("monge")){
                String features=sheet.optString("features","");
                String generated="Recursos de classe de nível 1 conforme os manuais habilitados";
                if(sheet.optInt("level",1)==1&&features.contains(generated))
                    sheet.put("features",features.replace(generated,"Artes Marciais; Defesa sem Armadura"));
                String attacks=sheet.optString("attacks","");
                if(attacks.equals(oldGeneratedMonkAttacks(sheet))){
                    sheet.put("attacks",monkAttacks(sheet));
                    if(!containsString(repairs,"corrected_generated_monk_martial_arts"))
                        repairs.put("corrected_generated_monk_martial_arts");
                }
            }
            if(repairs.length()>0)sheet.put("species_mechanics_repair_log",repairs);
        }catch(Exception e){
            throw new IllegalStateException("Inconsistência mecânica não pode ser corrigida com segurança.",e);
        }
        return sheet;
    }
}
