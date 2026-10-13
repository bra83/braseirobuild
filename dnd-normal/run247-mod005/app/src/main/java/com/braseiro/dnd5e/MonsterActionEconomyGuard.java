package com.braseiro.dnd5e;

import org.json.JSONObject;
import java.util.Locale;

/**
 * RUN238 / G05. Compiled monster actions must spend the resource declared by
 * their own category, never the resource chosen by a UI or an inferred default.
 * Legendary actions and reactions need independent trigger/budget handling and
 * are intentionally rejected by the ordinary action and multiattack paths.
 */
public final class MonsterActionEconomyGuard {
    private MonsterActionEconomyGuard() { }

    /** Returns an empty string for categories that cannot use a normal turn. */
    public static String standaloneCost(String rawCategory) {
        if(rawCategory==null)return "";
        String category=rawCategory.trim().toLowerCase(Locale.ROOT);
        if("action".equals(category))return "action";
        if("bonus_action".equals(category)||"bonus".equals(category))return "bonus_action";
        return "";
    }

    /** Multiattack spends one action; no component may spend a second action,
     * bonus action, reaction or legendary point under the same receipt. */
    public static String componentBlock(String rawCategory) {
        return "action".equals(standaloneCost(rawCategory))?"":
            "unsupported_multiattack_component_economy";
    }

    /** A hit rider without a compiled saving throw cannot silently vanish.
     * This is deliberately conservative: future typed on-hit riders can use
     * their own handler instead of treating condition_on_fail as on-hit text. */
    public static String attackRiderBlock(int saveDc,String saveAbility,
        String conditionOnFail,String effectOnFail) {
        String saveBlock=saveAbilityBlock(saveDc,saveAbility);
        if(!saveBlock.isEmpty())return saveBlock;
        if(saveDc==0&&(!blank(conditionOnFail)||!blank(effectOnFail)))
            return "unresolved_attack_hit_rider_requires_typed_handler";
        return "";
    }
    /** Full-action variant. A no-save on-hit condition is executable only when
     * the source action declares one core condition and its complete duration
     * trigger. Multiattack currently cannot commit such riders per component. */
    public static String attackRiderBlock(JSONObject action) {
        return attackRiderBlock(action,true);
    }
    /** XMM standalone Grappled actions are executable only for the source-indexed
     * simple subset whose escape DC and size cap match the compiled block. */
    public static String attackRiderBlock(JSONObject action,String statblockId) {
        String blocked=attackRiderBlock(action);
        if("unsupported_grapple_escape_rider".equals(blocked)&&
            Xmm2025GrappleEscapeRules.validSource(statblockId,action))return "";
        return blocked;
    }
    public static String attackRiderBlock(JSONObject action,boolean allowTypedOnHit) {
        if(action==null)return "unresolved_attack_hit_rider_requires_typed_handler";
        if(action.optBoolean("manual_unmodeled_source_effect_required",false))
            return "manual_source_action_resolution_required";
        String block=attackRiderBlock(action.optInt("save_dc",0),
            action.optString("save_ability",""),action.optString("condition_on_fail",""),
            action.optString("effect_on_fail",""));
        if(!block.isEmpty())return block;
        String condition=action.optString("condition_on_hit","").trim();
        if(!action.optString("effect_on_hit","").trim().isEmpty())
            return "unresolved_attack_hit_rider_requires_typed_handler";
        boolean timing=action.has("condition_duration_rounds")||action.has("condition_duration_tick");
        boolean untilStand=action.has("condition_ends_when");
        if(condition.isEmpty()&&!timing&&!untilStand)return "";
        if(!allowTypedOnHit)return "unsupported_multiattack_on_hit_rider";
        if(!action.has("to_hit")||condition.isEmpty())
            return "unresolved_attack_hit_rider_requires_typed_handler";
        String canonical=CompiledMonsterActionGuard.canonicalCondition(condition);
        if(action.has("escape_dc")&&condition.toLowerCase(Locale.ROOT).contains("grappled"))
            return action.optInt("escape_dc",0)>0?"unsupported_grapple_escape_rider":
                "invalid_compiled_escape_dc";
        if(canonical.isEmpty()||!canonical.equals(ConditionTokenGuard.canonical(condition)))
            return "unsupported_on_hit_condition";
        // An untimed Prone rider has an explicit stand-up recovery mechanism.
        // Never grant other conditions an indefinite duration by inference.
        if(untilStand)return !timing&&MonsterOnHitProneRules.validSource(action)?"":
            "unsupported_untimed_on_hit_rider";
        if(action.has("condition_max_target_size"))
            return "unsupported_untimed_on_hit_rider";
        try {
            int duration=MonsterConditionTimerLedger.compiledDuration(action);
            if(duration<1)return "unresolved_attack_hit_rider_requires_typed_handler";
            return "";
        } catch(IllegalArgumentException invalid) {
            return "unresolved_attack_hit_rider_requires_typed_handler";
        }
    }
    /** A numeric DC without an ability cannot authorize a saving throw.
     * The source may be an escape DC or other DC; never infer its purpose. */
    public static String saveAbilityBlock(int saveDc,String saveAbility) {
        if(saveDc<0)return "invalid_compiled_rider_save_dc";
        if(saveDc==0)return "";
        String ability=saveAbility==null?"":saveAbility.trim().toLowerCase(Locale.ROOT);
        if(!("str".equals(ability)||"dex".equals(ability)||"con".equals(ability)||
            "int".equals(ability)||"wis".equals(ability)||"cha".equals(ability)))
            return "missing_or_invalid_compiled_save_ability";
        return "";
    }
    private static boolean blank(String value){return value==null||value.trim().isEmpty();}
}
