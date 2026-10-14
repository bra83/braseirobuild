package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.text.Normalizer;
import java.util.Locale;

/** Source-declared no-save, on-hit Prone with a maximum target size.
 * Persistent until the target stands; never an invented one-round timer.
 * Target size must come from persisted character or verified statblock data.
 */
public final class MonsterOnHitProneRules {
    private MonsterOnHitProneRules() { }
    private static String norm(String text) {
        if(text==null)return "";
        return Normalizer.normalize(text.trim().toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
            .replaceAll("\\p{M}","");
    }
    public static int sizeRank(String input) {
        String size=norm(input);
        switch(size) {
            case "tiny":case "miudo":case "minusculo":return 1;
            case "small":case "pequeno":return 2;
            case "medium":case "medio":return 3;
            case "large":case "grande":return 4;
            case "huge":case "enorme":return 5;
            case "gargantuan":case "colossal":return 6;
            default:return 0;
        }
    }
    public static boolean validSize(String value){return sizeRank(value)>0;}
    public static boolean validSource(JSONObject action) {
        MonsterForcedMovementRules.Spec forced=action==null?null:
            MonsterForcedMovementRules.compiled(action.optString("effect_on_hit",""),"prone");
        String movementLimit=action==null?"":action.optString("effect_on_hit_max_target_size","");
        boolean compoundKnockdown=forced!=null&&
            (("pull".equals(forced.direction)&&forced.maximumFeet==25)||
             ("push".equals(forced.direction)&&forced.maximumFeet==20))&&
            "huge".equals(norm(movementLimit))&&
            movementLimit.equalsIgnoreCase(action.optString("condition_max_target_size",""));
        return action!=null&&action.has("to_hit")&&
            "prone".equals(action.optString("condition_on_hit",""))&&
            "stands_up".equals(action.optString("condition_ends_when",""))&&
            validSize(action.optString("condition_max_target_size",""))&&
            !action.has("condition_duration_rounds")&&!action.has("condition_duration_tick")&&
            !action.has("save_dc")&&!action.has("save_ability")&&
            !action.has("condition_on_fail")&&!action.has("effect_on_fail")&&
            (!action.has("effect_on_hit")||compoundKnockdown);
    }
    /** A creature that is too large still takes a legal attack's damage;
     * it merely avoids the knockdown. Unknown size is a genuine preflight block. */
    public static String targetBlock(JSONObject action,String actualSize){
        if(!validSource(action))return "unsupported_untimed_on_hit_rider";
        return validSize(actualSize)?"":"target_size_not_verified";
    }
    public static boolean eligible(JSONObject action,String actualSize){
        return validSource(action)&&validSize(actualSize)&&
            sizeRank(actualSize)<=sizeRank(action.optString("condition_max_target_size",""));
    }
}
