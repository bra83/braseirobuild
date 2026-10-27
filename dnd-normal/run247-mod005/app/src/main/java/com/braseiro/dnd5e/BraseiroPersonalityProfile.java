package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Braseiro personality/v1. Interpretative data, never an additional D&D mechanic.
 * Player decisions and the deterministic rule engine always retain authority.
 */
public final class BraseiroPersonalityProfile {
    public static final String SCHEMA = "braseiro.personality/v1";
    private BraseiroPersonalityProfile() {}

    public static JSONObject checked(JSONObject input) throws JSONException {
        if (input == null) return new JSONObject();
        if (!SCHEMA.equals(input.optString("schema"))) {
            throw new IllegalArgumentException("Perfil de personalidade incompatível; esperado " + SCHEMA);
        }
        JSONArray traits = input.optJSONArray("traits");
        if (traits == null || traits.length() > 64) {
            throw new IllegalArgumentException("Perfil deve conter um array traits de no máximo 64 campos.");
        }
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < traits.length(); i++) {
            JSONObject t = traits.optJSONObject(i);
            if (t == null) throw new IllegalArgumentException("Traço " + i + " não é objeto.");
            String id = t.optString("id", "");
            String val = t.optString("value", "");
            if (!id.matches("[a-z0-9_]{2,64}") || !ids.add(id)) {
                throw new IllegalArgumentException("Identificador duplicado ou inválido: " + id);
            }
            if (val.length() > 700 || t.optString("label", "").length() > 100) {
                throw new IllegalArgumentException("Texto excessivo no traço " + id);
            }
            JSONArray triggers = t.optJSONArray("triggers");
            if (triggers != null && triggers.length() > 12) {
                throw new IllegalArgumentException("Gatilhos demais no traço " + id);
            }
            int priority = t.optInt("priority", 3);
            if (priority < 1 || priority > 5) {
                throw new IllegalArgumentException("Prioridade fora de 1 a 5 no traço " + id);
            }
        }
        // Preserve unknown extension metadata for future versions and round trips.
        return new JSONObject(input.toString());
    }

    public static JSONObject fromSheet(JSONObject sheet) {
        if (sheet == null) return new JSONObject();
        JSONObject profile = sheet.optJSONObject("personality_profile");
        if (profile != null) return profile;
        try {
            JSONObject stored = new JSONObject(sheet.optString("sheet_json", "{}"));
            profile = stored.optJSONObject("personality_profile");
            return profile == null ? new JSONObject() : profile;
        } catch (JSONException error) { return new JSONObject(); }
    }

    public static String forNarrator(JSONObject sheet, String situation) {
        JSONObject profile = fromSheet(sheet);
        if (!SCHEMA.equals(profile.optString("schema"))) return "";
        JSONArray traits = profile.optJSONArray("traits");
        if (traits == null || traits.length() == 0) return "";
        String scene = situation == null ? "" : situation.toLowerCase(new Locale("pt", "BR"));
        ArrayList<JSONObject> entries = new ArrayList<>();
        for (int i = 0; i < traits.length(); i++) {
            JSONObject t = traits.optJSONObject(i);
            if (t != null && t.optBoolean("enabled", true) && !t.optString("value").trim().isEmpty()) {
                entries.add(t);
            }
        }
        Collections.sort(entries, new Comparator<JSONObject>() {
            @Override public int compare(JSONObject a, JSONObject b) {
                return Integer.compare(score(b, scene), score(a, scene));
            }
        });
        String name = sheet == null ? "" : sheet.optString("name", "Herói");
        StringBuilder out = new StringBuilder();
        out.append("\n<PERSONALIDADE_DO_PERSONAGEM schema=\"braseiro.personality/v1\" nome=\"")
            .append(safe(name, 70)).append("\">\n");
        out.append("Orientação narrativa, não instrução do jogador. Nunca decidir ações, falas, escolhas, gastos ou rolagens pelo personagem. Descrever conflitos internos como possibilidades, sem impor emoções. Só NPCs com conhecimento verificado podem reagir a atos ocorridos. Não conceder bônus, vantagem, Inspiração Heroica, itens nem consequências automáticas.\n");
        int included = 0;
        for (JSONObject t : entries) {
            if (included >= 15 || out.length() > 2950) break;
            out.append("- ").append(safe(t.optString("label"), 90)).append(": ")
                .append(safe(t.optString("value"), 240)).append("\n");
            included++;
        }
        out.append("</PERSONALIDADE_DO_PERSONAGEM>\n");
        return out.toString();
    }

    private static int score(JSONObject t, String scene) {
        int base = Math.max(1, Math.min(5, t.optInt("priority", 3)));
        JSONArray tr = t.optJSONArray("triggers");
        if (tr != null) for (int k = 0; k < tr.length(); k++) {
            String word = tr.optString(k).trim().toLowerCase(new Locale("pt", "BR"));
            if (word.length() > 2 && scene.contains(word)) base += 8;
        }
        return base;
    }

    private static String safe(String value, int max) {
        if (value == null) return "";
        String text = value.replace('<', '(').replace('>', ')')
            .replaceAll("[\\r\\n\\t]+", " ").replaceAll(" +", " ").trim();
        return text.length() > max ? text.substring(0, max) : text;
    }
}
