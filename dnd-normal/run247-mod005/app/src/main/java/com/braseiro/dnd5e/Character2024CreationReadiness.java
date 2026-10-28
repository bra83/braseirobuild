package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/**
 * Reports unresolved player/mechanical choices without deciding for the player.
 * This is a progress report, not a destructive "auto complete" migration.
 */
public final class Character2024CreationReadiness {
    private Character2024CreationReadiness(){}

    private static String norm(String value){
        return Normalizer.normalize(value==null?"":value.toLowerCase(Locale.ROOT),
            Normalizer.Form.NFD).replaceAll("\\p{M}","")
            .replaceAll("[^a-z0-9]+"," ").trim();
    }
    private static boolean has(JSONArray a,String value){
        if(a==null)return false;
        for(int i=0;i<a.length();i++)if(norm(value).equals(norm(a.optString(i))))return true;
        return false;
    }
    private static void missing(JSONArray pending,String id){if(!has(pending,id))pending.put(id);}
    private static int selectedDistinct(JSONArray feats){
        if(feats==null)return 0;
        java.util.HashSet<String> set=new java.util.HashSet<>();
        for(int i=0;i<feats.length();i++){String f=norm(feats.optString(i));if(!f.isEmpty())set.add(f);}
        return set.size();
    }

    public static JSONObject evaluate(JSONObject sheet){
        if(sheet==null || !Character2024SheetReconciler.applies(sheet))return sheet;
        try{
            JSONArray pending=new JSONArray();
            if(sheet.optBoolean("origin_ability_choice_pending",false))
                missing(pending,"background_ability_plus2_plus1");

            if(norm(sheet.optString("species")).equals("humano")){
                String size=norm(sheet.optString("size"));
                if(!size.equals("pequeno")&&!size.equals("medio"))
                    missing(pending,"human_size");
                if(norm(sheet.optString("human_skill_choice")).isEmpty())
                    missing(pending,"human_skill");
                JSONArray feats=sheet.optJSONArray("selected_origin_feats");
                // Human 2024 gains one additional Origin feat beyond the background feat.
                if(selectedDistinct(feats)<2 || sheet.optBoolean("human_extra_origin_feat_pending",false))
                    missing(pending,"human_additional_origin_feat");
            }else if(!sheet.optBoolean("species_choices_complete",true))
                missing(pending,"species_choice");

            if(sheet.optBoolean("origin_feat_choice_pending",false))
                missing(pending,"origin_feat_specialization");
            int masteries=sheet.optInt("weapon_masteries_known",0);
            JSONArray chosenMasteries=sheet.optJSONArray("selected_weapon_masteries");
            if(masteries>0&&(chosenMasteries==null||chosenMasteries.length()<masteries))
                missing(pending,"weapon_masteries");
            JSONArray classChoices=sheet.optJSONArray("class_choices_pending");
            if(classChoices!=null)for(int i=0;i<classChoices.length();i++){
                String key=norm(classChoices.optString(i));
                if(!key.isEmpty())missing(pending,"class_"+key);
            }
            // A missing pending-array must never certify a new first-level hero.
            // These fields encode mechanically executable decisions, not prose.
            if(sheet.optInt("level",1)>=1){
                String cls=norm(sheet.optString("class_name",""));
                if(cls.equals("clerigo")&&!Character2024StartingClassChoices.resolved(sheet,"ordem_divina"))
                    missing(pending,"class_ordem_divina");
                else if(cls.equals("druida")&&!Character2024StartingClassChoices.resolved(sheet,"ordem_primal"))
                    missing(pending,"class_ordem_primal");
                else if(cls.equals("guerreiro")&&!Character2024StartingClassChoices.resolved(sheet,"fighting_style"))
                    missing(pending,"class_fighting_style");
                else if(cls.equals("bruxo")&&!Character2024StartingClassChoices.resolved(sheet,"invocations"))
                    missing(pending,"class_invocations");
            }

            if(norm(sheet.optString("class_name")).startsWith("monge")){
                // One artisan tool OR musical instrument is selected at level 1;
                // an herbalism proficiency from Hermit is not this class choice.
                String selected=sheet.optString("monk_class_tool_choice","");
                if(selected.trim().isEmpty())
                    missing(pending,"monk_class_tool_or_instrument");
            }
            String inventory=norm(sheet.optString("inventory",""));
            if(inventory.contains("a escolha")||inventory.contains("conforme o antecedente")||
               inventory.contains("equipamento inicial da classe confirmado"))
                missing(pending,"equipment_package_or_choice");
            JSONArray ledger=sheet.optJSONArray("inventory_ledger");
            if((ledger==null||ledger.length()==0)&&!inventory.isEmpty()){
                // Do not convert a prose inventory into fictitious owned items.
                sheet.put("inventory_review_required",true);
                sheet.put("inventory_ledger_status","legacy_text_pending_review");
            }
            sheet.put("creation_missing_choices",pending);
            sheet.put("creation_missing_choice_count",pending.length());
            sheet.put("creation_mechanically_ready",pending.length()==0);
            sheet.put("creation_completion_status",
                pending.length()==0?"mechanically_ready":"requires_player_choices");
            // These are legitimately optional/deliberate narrative decisions.
            JSONArray narrative=new JSONArray();
            for(String key:new String[]{"languages_pending","life_path_pending","alignment_pending"})
                if(sheet.optBoolean(key,false))narrative.put(key);
            sheet.put("creation_narrative_pending",narrative);
        }catch(Exception e){
            throw new IllegalStateException("Falha ao listar escolhas de criação pendentes.",e);
        }
        return sheet;
    }
}
