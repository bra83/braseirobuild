package com.braseiro.dnd5e.adventure;

import com.braseiro.dnd5e.DndRulesEngine;
import org.json.JSONObject;

/**
 * Ponte do Adventure Engine para o motor de regras canônico de D&D.
 * Nunca gera dados. Vantagem/desvantagem exige dois d20 explicitamente fornecidos.
 */
public final class AdventureRulesAdapter {
    private final DndRulesEngine rulesEngine;

    public AdventureRulesAdapter() { this(new DndRulesEngine()); }
    public AdventureRulesAdapter(DndRulesEngine rules) { this.rulesEngine = rules != null ? rules : new DndRulesEngine(); }

    public JSONObject determineCheckRequirement(AdventureNode node, AdventureTransition transition) {
        JSONObject req = new JSONObject();
        try {
            JSONObject checkObj = null;
            if (transition != null && transition.getCheckOverride() != null && transition.getCheckOverride().length() > 0) checkObj = transition.getCheckOverride();
            else if (node != null && node.getCheckRequirement() != null && node.getCheckRequirement().length() > 0) checkObj = node.getCheckRequirement();
            if (checkObj == null || checkObj.length() == 0) {
                req.put("requires_check", false).put("auto_rolls", false);
                return req;
            }
            int dc = checkObj.optInt("dc", 10);
            req.put("requires_check", true)
               .put("auto_rolls", false)
               .put("dc", dc)
               .put("ability", checkObj.optString("ability", "Destreza"))
               .put("skill", checkObj.optString("skill", ""))
               .put("descriptor", checkObj.optString("description", "CD " + dc));
        } catch (Exception ignored) {}
        return req;
    }

    /** Compatibilidade: um único d20 só resolve modo normal. */
    public JSONObject evaluateExplicitRoll(JSONObject checkReq, int naturalD20, int modifier, boolean advantage, boolean disadvantage) {
        if (advantage != disadvantage) return missingSecondRoll(checkReq, naturalD20, modifier, advantage, disadvantage);
        return evaluateExplicitRoll(checkReq, naturalD20, naturalD20, modifier, advantage, disadvantage);
    }

    public JSONObject evaluateExplicitRoll(JSONObject checkReq, int firstD20, int secondD20, int modifier, boolean advantage, boolean disadvantage) {
        JSONObject result = new JSONObject();
        try {
            if (checkReq == null || !checkReq.optBoolean("requires_check", false)) {
                result.put("resolved", true).put("success", true).put("margin", 0)
                      .put("first_roll", firstD20).put("second_roll", secondD20)
                      .put("natural_roll", firstD20).put("total", firstD20 + modifier)
                      .put("auto_rolls", false);
                return result;
            }
            if (firstD20 < 1 || firstD20 > 20 || secondD20 < 1 || secondD20 > 20) {
                result.put("resolved", false).put("requires_input", true).put("input_type", "d20")
                      .put("error", "Informe resultados de d20 válidos entre 1 e 20.").put("auto_rolls", false);
                return result;
            }
            int dc = checkReq.optInt("dc", 10);
            result = rulesEngine.resolveAbilityCheck(firstD20, secondD20, modifier, dc, advantage, disadvantage);
            result.put("resolved", true).put("requires_input", false).put("auto_rolls", false);
        } catch (Exception ignored) {}
        return result;
    }

    private JSONObject missingSecondRoll(JSONObject checkReq, int firstD20, int modifier, boolean advantage, boolean disadvantage) {
        JSONObject out = new JSONObject();
        try {
            out.put("resolved", false).put("requires_input", true).put("auto_rolls", false)
               .put("input_type", "two_d20_results")
               .put("first_roll", firstD20)
               .put("modifier", modifier)
               .put("advantage", advantage && !disadvantage)
               .put("disadvantage", disadvantage && !advantage)
               .put("dc", checkReq == null ? 0 : checkReq.optInt("dc", 0))
               .put("error", "Vantagem ou desvantagem exige dois resultados de d20 informados pelo jogador.");
        } catch (Exception ignored) {}
        return out;
    }
}
