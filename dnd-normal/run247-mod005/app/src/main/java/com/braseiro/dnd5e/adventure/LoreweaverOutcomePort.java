package com.braseiro.dnd5e.adventure;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Java adaptation of the rules-neutral outcome wire contract:
 * 1A7432/loreweaver core/check_outcome.py (MIT).
 * No Python runtime, network server or dependencies required on Android.
 * Consumers branch on success/critical/fumble/tier, NEVER on id or label.
 */
public final class LoreweaverOutcomePort {
    private LoreweaverOutcomePort() {}
    public static JSONObject create(String id, String label, boolean success,
                                    boolean critical, boolean fumble, int tier,
                                    Integer margin) {
        if (id == null || id.trim().length() == 0 || id.length() > 72 ||
            label == null || label.trim().length() == 0 || label.length() > 160)
            throw new IllegalArgumentException("Resultado do sistema inválido");
        try {
            JSONObject wire = new JSONObject();
            wire.put("id", id);
            wire.put("label", label);
            wire.put("success", success);
            wire.put("critical", critical);
            wire.put("fumble", fumble);
            wire.put("tier", tier);
            if (margin != null) wire.put("margin", margin.intValue());
            return wire;
        } catch (JSONException error) {
            throw new IllegalArgumentException("Não foi possível serializar o resultado", error);
        }
    }
    public static JSONObject fromResolution(JSONObject resolution) {
        if (resolution == null || !resolution.optBoolean("resolved", false))
            throw new IllegalArgumentException("Uma resolução inválida não produz resultado");
        JSONObject result = resolution.optJSONObject("outcome");
        if (result == null || !result.has("success") || !result.has("tier"))
            throw new IllegalArgumentException("Contrato neutro ausente");
        try {
            return new JSONObject(result.toString());
        } catch (JSONException error) {
            throw new IllegalArgumentException("Não foi possível restaurar o resultado", error);
        }
    }
}
