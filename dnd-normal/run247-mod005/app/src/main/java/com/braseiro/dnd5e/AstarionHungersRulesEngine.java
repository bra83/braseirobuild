package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Regras fonte-fiéis de Astarion's Book of Hungers. Nenhum RNG interno. */
public final class AstarionHungersRulesEngine {
    public static final String SUPPLEMENT_ID="astarions_book_of_hungers";

    /** Compatibilidade: sem dano/target/choice explícitos não aplica empowerment. */
    public JSONObject resolveVampiricBite(int conMod,int proficiencyBonus,int currentHp,int maxHp){
        JSONObject res=new JSONObject();try{
            res.put("attack_type","unarmed_strike_bite").put("damage_dice","1d4").put("damage_bonus",conMod).put("damage_type","piercing")
               .put("empowerment_requires_explicit_damage_and_choice",true).put("auto_rolls",false).put("source_verified",true);
        }catch(Exception ignored){}return res;
    }

    /** Resolve somente depois de dano perfurante já determinado e escolha explícita do jogador. */
    public JSONObject resolveVampiricBiteEmpowerment(int piercingDamage,String targetCreatureType,String choice,int proficiencyBonus,int usesSpent){JSONObject res=new JSONObject();try{
        String type=targetCreatureType==null?"":targetCreatureType.trim().toLowerCase();String pick=choice==null?"":choice.trim().toLowerCase();int pb=Math.max(1,proficiencyBonus),spent=Math.max(0,usesSpent);
        boolean eligible=!"construct".equals(type)&&!"constructo".equals(type)&&!"undead".equals(type)&&!"morto-vivo".equals(type)&&!"morto vivo".equals(type);
        boolean useAvailable=spent<pb,validChoice="drain".equals(pick)||"drenar".equals(pick)||"strengthen".equals(pick)||"fortalecer".equals(pick);
        res.put("eligible_target",eligible).put("uses_max",pb).put("uses_spent_before",spent).put("use_available",useAvailable).put("auto_rolls",false).put("source_verified",true);
        if(!eligible||!useAvailable||!validChoice){res.put("resolved",false).put("requires_choice",eligible&&useAvailable&&!validChoice);return res;}
        res.put("resolved",true).put("uses_spent_after",spent+1).put("choice",pick);
        if("drain".equals(pick)||"drenar".equals(pick))res.put("healing",Math.max(0,piercingDamage));
        else res.put("next_check_or_attack_bonus",Math.max(0,piercingDamage)).put("duration_minutes",1);
    }catch(Exception ignored){}return res;}

    /** Blood Frenzy não é uma regra da espécie apresentada na fonte. */
    public JSONObject evaluateBloodFrenzy(int hungerLevel,int targetCurrentHp,int targetMaxHp){JSONObject res=new JSONObject();try{res.put("resolved",false).put("mechanic_defined_by_source",false).put("auto_rolls",false).put("status","blocked_unsupported_rule");}catch(Exception ignored){}return res;}

    public JSONArray listVampiricFeats(){JSONArray feats=new JSONArray();try{
        add(feats,"Tireless Reveler","Origin",1);add(feats,"Vampire Hunter","Origin",1);add(feats,"Vampire's Plaything","Origin",1);
        for(String n:new String[]{"Bloodlust","Bomber","Cloying Mists","Delicious Pain","Light Bringer","Love Bites","Putrefy","Rebuke","Treacherous Allure","Vampire Touched"})add(feats,n,"General",4);
        add(feats,"Boon of Blazing Dawn","Epic Boon",19);add(feats,"Boon of Looming Shadows","Epic Boon",19);add(feats,"Boon of Misty Escape","Epic Boon",19);
    }catch(Exception ignored){}return feats;}

    public JSONObject getEpicBoon(String boonName){JSONObject b=new JSONObject();try{String n=boonName==null?"":boonName.trim();b.put("boon",n).put("level_required",19).put("source_verified",true).put("auto_rolls",false);
        if("Boon of Blazing Dawn".equalsIgnoreCase(n))b.put("ability_score_increase","+1 até 30").put("radiant_immunity",true).put("weapon_damage_may_be_radiant",true).put("radiant_hit_sunlight_burst","30 ft bright + 30 ft dim até o início do próximo turno, 1/turn");
        else if("Boon of Looming Shadows".equalsIgnoreCase(n))b.put("ability_score_increase","+1 até 30").put("melee_reach_bonus_on_attack_action_ft",10).put("dodge_as_bonus_action",true);
        else if("Boon of Misty Escape".equalsIgnoreCase(n))b.put("ability_score_increase","INT/WIS/CHA +1 até 30").put("trigger","cair a 0 PV sem morrer instantaneamente").put("instead_hp",1).put("casts_gaseous_form_self",true).put("fly_speed_ft",20).put("healing_each_turn",10).put("uses_per_long_rest",1);
        else b.put("source_verified",false).put("status","unknown_boon");
    }catch(Exception ignored){}return b;}

    private static void add(JSONArray a,String name,String category,int level)throws Exception{a.put(new JSONObject().put("name",name).put("category",category).put("level_required",level).put("source_verified",true));}
}
