package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Mecânicas pré-compiladas do Animal Handlers Manual.pdf. */
public final class AnimalCompanionRulesEngine {
    public static final String BOOK="Animal Handlers Manual.pdf";
    private AnimalCompanionRulesEngine(){}

    public static int maxRememberedTricks(int intelligenceRating){return Math.max(0,intelligenceRating)*2;}
    public static int trickTrainingDowntimeDays(){return 5;}
    public static int maxAnimalsTrainedTogether(int wisdomModifier){return Math.max(1,wisdomModifier);}
    public static int normalAcclimationDowntimeDays(){return 20;}
    public static int captiveAcclimationDowntimeDays(){return normalAcclimationDowntimeDays()*2;}
    public static int maxLoyalty(int handlerWisdomScore){return Math.max(0,Math.min(20,handlerWisdomScore));}
    public static int startingDomesticLoyalty(int handlerWisdomScore){return maxLoyalty(handlerWisdomScore)/2;}
    public static int startingWildLoyalty(boolean acclimated,int handlerWisdomScore){return acclimated?startingDomesticLoyalty(handlerWisdomScore):0;}
    public static int applyLoyaltyEvent(int current,int handlerWisdomScore,int d4,boolean gain){
        int delta=Math.max(1,Math.min(4,d4))*(gain?1:-1);return Math.max(0,Math.min(maxLoyalty(handlerWisdomScore),current+delta));
    }
    public static double dailyFoodPounds(String size){
        String n=normalize(size);if(n.contains("tiny")||n.contains("miud")||n.contains("minuscul"))return .25d;
        if(n.contains("large")||n.contains("grande"))return 4d;if(n.contains("huge")||n.contains("enorme"))return 16d;
        if(n.contains("garg"))return 64d;return 1d;
    }
    public static double dailyWaterGallons(String size){return dailyFoodPounds(size);}
    public static double uptimeDowntimeCredit(boolean longRestTaken){return longRestTaken?.5d:0d;}

    public static JSONArray sidekickProgression(){JSONArray a=new JSONArray();try{String[] features={
        "Companion Life; Defining Role","Role Feature; Role Trick","Indomitable (1)","Role Feature; Second Wind (1)","Ability Score Increase; Role Trick","Role Feature; Saving Throw","Role Feature","Role Trick","Ability Score Increase","Role Feature","Indomitable (2)","Role Feature; Role Trick","Ability Score Increase","Second Wind (2)","Role Feature","Role Trick","Ability Score Increase","Save Mastery","Indomitable (3)","Ability Score Increase","Trick Mastery"};
        for(int level=0;level<=20;level++)a.put(new JSONObject().put("level",level).put("extra_hit_dice",level).put("features",features[level]));
    }catch(Exception ignored){AppLog.ignored("AnimalCompanionRulesEngine","Falha ao montar progressão animal.",ignored);}return a;}

    public static JSONObject roleTricks(){JSONObject o=new JSONObject();try{
        o.put("Loveable Lump",new JSONArray().put("1: Use Skill (Perception)").put("4: Scout").put("7: Perform").put("11: Use Skill (Stealth)").put("15: Go Home"));
        o.put("Heavy Bulwark",new JSONArray().put("1: Move").put("4: Use Skill (Athletics)").put("7: Behave").put("11: Defend").put("15: Attack"));
        o.put("Raptor",new JSONArray().put("1: Scout").put("4: Use Skill (Perception)").put("7: Hunt").put("11: role trick").put("15: role trick"));
        o.put("Slinker or Loper",new JSONArray().put("1: Attack").put("4: Hunt").put("7: Move").put("11: Behave").put("15: Harry"));
        o.put("Trusted Mount",new JSONArray().put("1: Move").put("4: Behave").put("7: Discriminate").put("11: Go Home").put("15: Perform"));
    }catch(Exception ignored){AppLog.ignored("AnimalCompanionRulesEngine","Falha ao montar truques de papel.",ignored);}return o;}

    public static boolean validRole(String role){String n=normalize(role);return n.contains("loveable")||n.contains("lovable")||n.contains("bulwark")||n.contains("raptor")||n.contains("slinker")||n.contains("loper")||n.contains("trusted mount")||n.contains("montaria");}
    public static boolean companionSurvivesZeroHp(int sidekickLevel){return sidekickLevel>=0;}
    public static boolean massiveDamageInstantDeathApplies(int sidekickLevel){return false;}
    public static boolean rolledDeathSaveAutomaticallySucceeds(int sidekickLevel){return sidekickLevel>=0;}
    public static int indomitableUses(int level){if(level>=18)return 3;if(level>=10)return 2;if(level>=2)return 1;return 0;}
    public static int secondWindUses(int level){if(level>=13)return 2;if(level>=3)return 1;return 0;}
    public static boolean trickLimitRemoved(int level){return level>=20;}

    public static JSONObject descriptor(){
        JSONObject out=new JSONObject();
        try{
            out.put("pack_id","animal_handlers").put("book",BOOK).put("status","compiled_phase_1");
            out.put("training",new JSONObject().put("handler_requires_animal_handling_proficiency",true).put("days_per_trick",5)
                    .put("max_tricks","2 × intelligence_rating").put("max_animals_same_trick","max(1, wisdom_modifier)"));
            out.put("acclimation",new JSONObject().put("animal_handling_proficiency_required",true).put("normal_days",20).put("captivity_multiplier",2)
                    .put("domestic_previously_tamed_infant_or_feature_companion_exempt",true));
            out.put("loyalty",new JSONObject().put("optional_rule",true).put("range","0..20").put("maximum","handler_wisdom_score")
                    .put("domestic_start_after_adjustment","half_maximum").put("purchased_domestic_adjustment_days",7)
                    .put("wild_start",0).put("wild_after_acclimation","half_maximum").put("gain_or_loss_event","1d4")
                    .put("companionship_gain_period_days",30).put("separation_check_period_days",30));
            out.put("care",new JSONObject().put("food_lb_by_size",new JSONObject().put("Tiny",.25).put("Small",1).put("Medium",1).put("Large",4).put("Huge",16).put("Gargantuan",64))
                    .put("water_gallons_equal_food_pounds",true).put("insufficient_sustenance","apply_exhaustion_using_core_rules"));
            out.put("uptime",new JSONObject().put("optional_rule",true).put("credit_per_adventuring_day_with_long_rest",.5).put("requires_costs_tools_materials",true));
            out.put("systems",new JSONArray().put("animal_sidekick").put("tricks").put("templates").put("loyalty").put("animal_handling").put("mounted_combat").put("animal_market").put("care").put("weather").put("terrain"));
            out.put("sidekick_progression",sidekickProgression()).put("role_tricks",roleTricks());
            out.put("companion_life",new JSONObject().put("level",0).put("zero_hp","unconscious_not_dead").put("instant_death_from_massive_damage",false).put("rolled_death_save","automatic_success"));
        }catch(Exception ignored){AppLog.ignored("AnimalCompanionRulesEngine","Falha ao montar descritor compilado.",ignored);}return out;
    }

    private static String normalize(String value){return value==null?"":java.text.Normalizer.normalize(value.toLowerCase(java.util.Locale.ROOT),java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+","");}
}
