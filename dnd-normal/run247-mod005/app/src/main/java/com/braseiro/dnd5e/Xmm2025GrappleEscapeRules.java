package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Locale;

/** Source-bound, manual XMM grapple escape checks; never rolls or mutates state. */
public final class Xmm2025GrappleEscapeRules {
    private static final java.util.Set<String> LINKED_RESTRAINT_ACTIONS=
        new java.util.HashSet<String>(java.util.Arrays.asList(
            "Aberrant Cultist|Tentacle Lash","Chain Devil|Chain","Crocodile|Bite",
            "Giant Octopus|Tentacles","Mezzoloth|Claws","Purple Worm|Bite",
            "Remorhaz|Bite","Roc|Talons","Water Weird|Surge",
            "Giant Crocodile|Bite","Kraken|Tentacle","Tyrannosaurus Rex|Bite"));
    private static final java.util.Set<String> SAVE_LINKED_RESTRAINT_ACTIONS=
        new java.util.HashSet<String>(java.util.Arrays.asList(
            "Behir|Constrict","Couatl|Constrict","Marilith|Constrict",
            "Salamander|Constrict","Yuan-ti Abomination|Constrict",
            "Yuan-ti Malison (Type 3)|Constrict"));
    private static final java.util.Map<String,String> SAVE_LINKED_RESTRAINT_SIZES;
    private static final java.util.Map<String,Integer> SAVE_LINKED_RESTRAINT_DCS;
    private static final java.util.Map<String,String> SAVE_LINKED_RESTRAINT_POINTERS;
    static {
        java.util.HashMap<String,String> sizes=new java.util.HashMap<String,String>();
        sizes.put("Behir|Constrict","large");sizes.put("Couatl|Constrict","medium");
        sizes.put("Marilith|Constrict","medium");sizes.put("Salamander|Constrict","large");
        sizes.put("Yuan-ti Abomination|Constrict","large");
        sizes.put("Yuan-ti Malison (Type 3)|Constrict","medium");
        SAVE_LINKED_RESTRAINT_SIZES=java.util.Collections.unmodifiableMap(sizes);
        java.util.HashMap<String,Integer> dcs=new java.util.HashMap<String,Integer>();
        dcs.put("Behir|Constrict",16);dcs.put("Couatl|Constrict",13);
        dcs.put("Marilith|Constrict",14);dcs.put("Salamander|Constrict",14);
        dcs.put("Yuan-ti Abomination|Constrict",14);
        dcs.put("Yuan-ti Malison (Type 3)|Constrict",13);
        SAVE_LINKED_RESTRAINT_DCS=java.util.Collections.unmodifiableMap(dcs);
        java.util.HashMap<String,String> pointers=new java.util.HashMap<String,String>();
        pointers.put("Behir|Constrict","/monster/59/action/2/entries/0");
        pointers.put("Couatl|Constrict","/monster/104/action/1/entries/0");
        pointers.put("Marilith|Constrict","/monster/292/action/2/entries/0");
        pointers.put("Salamander|Constrict","/monster/382/action/2/entries/0");
        pointers.put("Yuan-ti Abomination|Constrict","/monster/497/action/2/entries/0");
        pointers.put("Yuan-ti Malison (Type 3)|Constrict","/monster/501/action/2/entries/0");
        SAVE_LINKED_RESTRAINT_POINTERS=java.util.Collections.unmodifiableMap(pointers);
    }
    private static final java.util.Map<String,Integer> LINKED_RESTRAINT_CAPACITY;
    static {
        java.util.HashMap<String,Integer> capacities=new java.util.HashMap<String,Integer>();
        capacities.put("Aberrant Cultist|Tentacle Lash",2);
        capacities.put("Chain Devil|Chain",2);
        capacities.put("Giant Octopus|Tentacles",8);
        capacities.put("Mezzoloth|Claws",2);
        capacities.put("Crocodile|Bite",1);capacities.put("Purple Worm|Bite",1);
        capacities.put("Remorhaz|Bite",1);capacities.put("Roc|Talons",1);
        capacities.put("Water Weird|Surge",1);
        capacities.put("Giant Crocodile|Bite",1);capacities.put("Kraken|Tentacle",10);
        capacities.put("Tyrannosaurus Rex|Bite",1);
        LINKED_RESTRAINT_CAPACITY=java.util.Collections.unmodifiableMap(capacities);
    }
    private Xmm2025GrappleEscapeRules() { }

    /** One source action imposes disadvantage on the escape check. */
    public static boolean escapeCheckDisadvantage(String monster,JSONObject action) {
        return "Mimic".equals(monster)&&action!=null&&
            "Pseudopod".equals(action.optString("name",""))&&
            action.optBoolean("source_bound",false)&&
            action.optBoolean("escape_check_disadvantage",false);
    }

    public static boolean linkedRestraint(String monster,JSONObject action) {
        return action!=null&&(LINKED_RESTRAINT_ACTIONS.contains(monster+"|"+
            action.optString("name",""))||SAVE_LINKED_RESTRAINT_ACTIONS.contains(monster+"|"+
            action.optString("name","")));
    }
    public static int targetCapacity(String monster,JSONObject action) {
        if(action==null)return 0;
        if(SAVE_LINKED_RESTRAINT_ACTIONS.contains(monster+"|"+action.optString("name","")))
            return 0; // The source does not cap the number of simultaneous targets.
        Integer capacity=LINKED_RESTRAINT_CAPACITY.get(monster+"|"+action.optString("name",""));
        if(capacity!=null)return capacity;
        return validSource(monster,action)?1:0;
    }

    public static JSONArray conditions(String monster,JSONObject action) {
        JSONArray out=new JSONArray();
        if(!validSource(monster,action))return out;
        String[] values=action.optString(action.has("condition_on_fail")?
            "condition_on_fail":"condition_on_hit","").split(",");
        for(String value:values)out.put(value.trim());
        return out;
    }

    public static boolean validSource(String monster, JSONObject action) {
        if(monster==null||action==null||!action.optBoolean("source_bound",false)||
            !action.has("escape_dc")||!MonsterOnHitProneRules.validSize(action.optString("condition_max_target_size",""))||
            action.has("effect_on_fail")||action.has("effect_on_hit")||
            action.has("condition_duration_rounds")||action.has("condition_duration_tick")||
            (action.has("to_hit")&&(action.has("condition_on_fail")||action.has("save_dc")||action.has("save_ability"))))return false;
        try {
            int escapeDc=action.getInt("escape_dc");
            if(escapeDc<1||escapeDc>30)return false;
            JSONObject pinned=profile(monster,action.optString("name",""));
            boolean disadvantage=escapeCheckDisadvantage(monster,action);
            boolean linkedRestraint=linkedRestraint(monster,action);
            boolean saveLinked=SAVE_LINKED_RESTRAINT_ACTIONS.contains(monster+"|"+
                action.optString("name",""));
            if(saveLinked&&(!action.has("save_dc")||!action.has("save_ability")||
                !"str".equals(action.optString("save_ability",""))||
                !"grappled, restrained".equals(action.optString("condition_on_fail",""))||
                action.has("condition_on_hit")||action.optInt("save_dc",0)<1))return false;
            if(!saveLinked&&(action.has("save_dc")||action.has("save_ability")||action.has("condition_on_fail")))return false;
            String declared=action.optString(saveLinked?"condition_on_fail":"condition_on_hit","")
                .trim().toLowerCase(Locale.ROOT);
            if(!("grappled".equals(declared)&&!linkedRestraint||
                "grappled, restrained".equals(declared)&&linkedRestraint))return false;
            if(linkedRestraint&&!action.optBoolean("linked_condition_bundle_until_grapple_ends",false))
                return false;
            if(saveLinked&&(!action.optBoolean("linked_condition_bundle_until_grapple_ends",false)||
                !SAVE_LINKED_RESTRAINT_SIZES.get(monster+"|"+action.optString("name",""))
                    .equals(action.optString("condition_max_target_size",""))||
                SAVE_LINKED_RESTRAINT_DCS.get(monster+"|"+action.optString("name",""))!=escapeDc))return false;
            Integer capacity=LINKED_RESTRAINT_CAPACITY.get(monster+"|"+action.optString("name",""));
            if(linkedRestraint&&!saveLinked&&
                (capacity==null||action.optInt("grapple_target_capacity",0)!=capacity))
                return false;
            if(saveLinked&&action.has("grapple_target_capacity"))
                return false;
            return pinned.length()>0&&"grappled".equals(pinned.optString("condition",""))&&
                pinned.optBoolean("other",true)==(disadvantage||linkedRestraint)&&pinned.optInt("dc",0)==escapeDc&&
                pinned.optString("size","").equalsIgnoreCase(action.optString("condition_max_target_size",""));
        } catch(Exception invalid){return false;}
    }

    public static boolean targetEligible(String monster,JSONObject action,String targetSize) {
        return MonsterOnHitProneRules.validSize(targetSize)&&validSource(monster,action)&&
            MonsterOnHitProneRules.sizeRank(targetSize)<=
                MonsterOnHitProneRules.sizeRank(action.optString("condition_max_target_size",""));
    }

    public static String targetBlock(String monster,JSONObject action,String targetSize) {
        if(!MonsterOnHitProneRules.validSize(targetSize))return "target_size_not_verified";
        return validSource(monster,action)?"":"unsupported_grapple_escape_rider";
    }

    public static JSONObject binding(String monster,JSONObject action,long sourceActorId,String receipt)
        throws Exception {
        JSONObject pinned=profile(monster,
            action==null?"":action.optString("name",""));
        if(!validSource(monster,action)||sourceActorId<=0||receipt==null||receipt.trim().isEmpty())
            throw new IllegalArgumentException("invalid_xmm_grapple_binding");
        return new JSONObject().put("schema","xmm_grapple_escape_v1")
            .put("source_actor_id",sourceActorId).put("source_statblock",monster)
            .put("source_action",action.optString("name","")).put("escape_dc",action.optInt("escape_dc"))
            .put("max_target_size",action.optString("condition_max_target_size"))
            .put("escape_check_disadvantage",escapeCheckDisadvantage(monster,action))
            .put("target_capacity",targetCapacity(monster,action))
            .put("linked_conditions",conditions(monster,action))
            .put("source_pointer",pinned.optString("ptr"))
            .put("source_blob_sha1",Xmm2025AttackConditionEngine.SOURCE_BLOB)
            .put("source_receipt",receipt);
    }

    /** Source pin for all executable grapples, including save-based Constricts
     * that intentionally do not belong to the attack-hit rider catalog. */
    public static JSONObject profile(String monster,String actionName) {
        try{
            JSONObject attack=Xmm2025AttackConditionEngine.profile(monster,actionName);
            if(attack.length()>0)return attack;
        }catch(Exception ignored){return new JSONObject();}
        String key=monster+"|"+actionName;
        Integer dc=SAVE_LINKED_RESTRAINT_DCS.get(key);String size=SAVE_LINKED_RESTRAINT_SIZES.get(key);
        String pointer=SAVE_LINKED_RESTRAINT_POINTERS.get(key);
        if(dc==null||size==null||pointer==null)return new JSONObject();
        try{return new JSONObject().put("id",monster).put("name",actionName)
            .put("condition","grappled").put("dc",dc).put("size",size)
            .put("other",true).put("ptr",pointer);}catch(Exception ignored){return new JSONObject();}
    }

    /** The player supplies the check result; escape uses the XPHB action's two skills. */
    public static JSONObject resolveEscape(String skill,int checkTotal,int escapeDc) {
        return resolveEscape(skill,checkTotal,0,escapeDc,false);
    }

    /** For a source-imposed disadvantage, both actual d20 totals are required;
     * the lower total is used. No dice are rolled by this resolver. */
    public static JSONObject resolveEscape(String skill,int firstTotal,int secondTotal,
        int escapeDc,boolean disadvantage) {
        JSONObject out=new JSONObject();
        try {
            String selected=skill==null?"":skill.trim().toLowerCase(Locale.ROOT);
            if(!"athletics".equals(selected)&&!"acrobatics".equals(selected))
                return out.put("resolved",false).put("reason","invalid_grapple_escape_skill")
                    .put("auto_rolls",false).put("auto_mutation_allowed",false);
            if(firstTotal< -100||firstTotal>100||escapeDc<1||escapeDc>30||
                (disadvantage&&(secondTotal< -100||secondTotal>100))||
                (!disadvantage&&secondTotal!=0&&secondTotal!=Integer.MIN_VALUE))
                return out.put("resolved",false).put("reason","grapple_escape_total_or_dc_out_of_range")
                    .put("auto_rolls",false).put("auto_mutation_allowed",false);
            int checkTotal=disadvantage?Math.min(firstTotal,secondTotal):firstTotal;
            return out.put("resolved",true).put("check","Strength (Athletics) or Dexterity (Acrobatics)")
                .put("selected_skill",selected).put("check_total",checkTotal).put("escape_dc",escapeDc)
                .put("first_check_total",firstTotal).put("second_check_total",disadvantage?secondTotal:0)
                .put("disadvantage",disadvantage)
                .put("action_cost","action").put("success",checkTotal>=escapeDc)
                .put("auto_rolls",false).put("auto_mutation_allowed",false);
        } catch(Exception failure){return new JSONObject();}
    }
}
