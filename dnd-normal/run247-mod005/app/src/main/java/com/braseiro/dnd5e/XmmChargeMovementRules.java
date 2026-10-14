package com.braseiro.dnd5e;

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Source-bound movement gates for XMM charge attacks. */
public final class XmmChargeMovementRules {
    private static final Pattern MOVEMENT = Pattern.compile(
        "^moved_(10|20|30)ft_straight_toward_target_immediately_before_hit$");

    private XmmChargeMovementRules() { }

    public static boolean supported(JSONObject action) {
        if (action == null || !action.has("to_hit") || !action.optBoolean("source_bound", false))
            return false;
        if (action.has("save_dc") || action.has("save_ability") ||
            !action.optString("condition_on_fail", "").isEmpty() ||
            !action.optString("condition_on_hit", "").isEmpty() ||
            !action.optString("effect_on_fail", "").isEmpty() ||
            !action.optString("effect_on_hit", "").isEmpty() ||
            action.has("condition_ends_when") || action.has("condition_max_target_size") ||
            action.has("escape_dc") || action.has("detach_dc") ||
            action.has("condition_duration_rounds") || action.has("condition_duration_tick"))
            return false;
        String condition = action.optString("extra_damage_condition",
            action.optString("conditional_condition", action.optString("damage_alternative_condition", "")));
        Matcher matcher = MOVEMENT.matcher(condition);
        if (!matcher.matches()) return false;
        boolean damage = !action.optString("extra_damage", "").trim().isEmpty() ||
            !action.optString("damage_alternative_expression", "").trim().isEmpty();
        boolean prone = "prone".equals(action.optString("extra_damage_condition_on_hit",
            action.optString("conditional_condition_on_hit", "")));
        return damage || prone;
    }

    public static int requiredFeet(JSONObject action) {
        if (!supported(action)) return 0;
        String condition = action.optString("extra_damage_condition",
            action.optString("conditional_condition", action.optString("damage_alternative_condition", "")));
        Matcher matcher = MOVEMENT.matcher(condition);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    public static boolean qualifies(JSONObject action, JSONObject proof, long actorId,
        String targetScene, int targetX, int targetY) {
        int feet = requiredFeet(action);
        return feet > 0 && proof != null &&
            targetScene != null && targetScene.equals(proof.optString("scene_key", "")) &&
            TacticalGridMovementProofRules.qualifiesToward(proof, actorId, targetX, targetY, feet);
    }

    public static boolean hasConditionalProne(JSONObject action) {
        return supported(action) && "prone".equals(action.optString("extra_damage_condition_on_hit",
            action.optString("conditional_condition_on_hit", "")));
    }

    public static String proneMaximumSize(JSONObject action) {
        if (!hasConditionalProne(action)) return "";
        return action.optString("extra_damage_max_target_size",
            action.optString("conditional_max_target_size", ""));
    }

    public static boolean proneEligible(JSONObject action, String targetSize) {
        String maximum = proneMaximumSize(action);
        if (maximum.isEmpty()) return hasConditionalProne(action) && MonsterOnHitProneRules.validSize(targetSize);
        return hasConditionalProne(action) && MonsterOnHitProneRules.validSize(targetSize) &&
            MonsterOnHitProneRules.sizeRank(targetSize) <= MonsterOnHitProneRules.sizeRank(maximum);
    }
}
