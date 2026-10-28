package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * First-level 2024 class decisions for brand-new, fully random sheets.
 * All four options are legal PHB 2024 choices. This helper MUST NOT touch
 * imported, edited, persisted or player-authored characters.
 */
public final class Character2024StartingClassChoices {
    private Character2024StartingClassChoices(){}

    private static String norm(String s) {
        return Normalizer.normalize(s==null?"":s.toLowerCase(Locale.ROOT),
            Normalizer.Form.NFD).replaceAll("\\p{M}","").replaceAll("[^a-z0-9]+"," ").trim();
    }
    private static String base(String s){
        int dash=s==null?-1:s.indexOf(" — ");
        return norm(dash<0?s:s.substring(0,dash));
    }
    public static boolean resolved(JSONObject sheet,String pendingKey){
        if(sheet==null)return false;
        String c=base(sheet.optString("class_name"));
        if("ordem_divina".equals(pendingKey)&&c.equals("clerigo"))
            return norm(sheet.optString("cleric_divine_order_choice")).equals("protetor");
        if("ordem_primal".equals(pendingKey)&&c.equals("druida"))
            return norm(sheet.optString("druid_primal_order_choice")).equals("guardiao");
        if("fighting_style".equals(pendingKey)&&c.equals("guerreiro"))
            return norm(sheet.optString("fighter_fighting_style_choice")).equals("defesa") &&
                sheet.optBoolean("fighter_defense_armor_bonus_applied",false);
        if("invocations".equals(pendingKey)&&c.equals("bruxo")){
            JSONArray selections=sheet.optJSONArray("warlock_eldritch_invocations");
            return selections!=null&&selections.length()==1&&
                norm(selections.optString(0)).equals("mente sobrenatural")&&
                sheet.optBoolean("concentration_constitution_save_advantage",false);
        }
        return false;
    }
    private static void addTraining(JSONObject sheet,String marker)throws Exception{
        String prof=sheet.optString("proficiencies","");
        if(!norm(prof).contains(norm(marker)))
            sheet.put("proficiencies",prof+"\n"+marker);
    }
    public static void chooseForNewRandom(JSONObject sheet)throws Exception{
        if(!DndRulesEngine.PROFILE_2024.equals(DndRulesEngine.canonicalProfile(sheet.optString("rules_source"))))
            throw new IllegalStateException("Escolhas 2024 requerem perfil 2024 explícito.");
        String c=base(sheet.optString("class_name"));
        if(c.equals("clerigo")){
            sheet.put("cleric_divine_order_choice","Protetor");
            sheet.put("cleric_heavy_armor_training",true);
            sheet.put("cleric_martial_weapon_training",true);
            addTraining(sheet,"Ordem Divina — Protetor: armaduras pesadas e armas marciais");
        }else if(c.equals("druida")){
            sheet.put("druid_primal_order_choice","Guardião");
            sheet.put("druid_medium_armor_training",true);
            sheet.put("druid_martial_weapon_training",true);
            addTraining(sheet,"Ordem Primal — Guardião: armaduras médias e armas marciais");
        }else if(c.equals("guerreiro")){
            // Starter equipment includes chain mail. Apply Defense only while
            // actually wearing armor. Never double-apply on repeated repairs.
            String equipment=norm(sheet.optString("inventory"));
            if(!equipment.contains("cota de malha")&&!equipment.contains("armadura"))
                throw new IllegalStateException("Estilo Defesa requer armadura vestida.");
            sheet.put("fighter_fighting_style_choice","Defesa");
            sheet.put("fighter_fighting_style_feat","Defesa");
            if(!sheet.optBoolean("fighter_defense_armor_bonus_applied",false)){
                int before=sheet.optInt("ac",10);
                sheet.put("fighter_defense_base_ac",before);
                sheet.put("ac",before+1);
                sheet.put("fighter_defense_armor_bonus_applied",true);
            }
            addTraining(sheet,"Talento de Estilo de Luta — Defesa (+1 CA usando armadura)");
        }else if(c.equals("bruxo")){
            // Eldritch Mind is available at level 1 and has no prerequisite.
            // Applies to Concentration saves only, not all Constitution saves.
            sheet.put("warlock_eldritch_invocations",new JSONArray().put("Mente Sobrenatural"));
            sheet.put("concentration_constitution_save_advantage",true);
            addTraining(sheet,"Invocação Mística — Mente Sobrenatural (vantagem em testes de concentração)");
        }
        JSONArray pending=sheet.optJSONArray("class_choices_pending");
        if(pending!=null){
            JSONArray remaining=new JSONArray();
            for(int i=0;i<pending.length();i++){
                String key=pending.optString(i);
                if(!resolved(sheet,key))remaining.put(key);
            }
            sheet.put("class_choices_pending",remaining);
        }
    }
}
