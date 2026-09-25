package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Contrato estável e fail-closed de leitura/escrita entre o Mestre e o estado do jogo. */
public final class MasterProtocolV1 {
    public static final int VERSION = 1;

    private static final Set<String> ARRAY_MUTATIONS = new HashSet<>(Arrays.asList(
            "state_updates", "npc_updates", "npc_cognition_updates", "knowledge_updates",
            "quest_updates", "faction_updates", "guild_updates", "clock_updates",
            "world_events", "event_resolutions", "pending_resolutions", "map_updates",
            "tactical_updates", "journal_updates"
    ));
    private static final Set<String> OBJECT_MUTATIONS = new HashSet<>(Arrays.asList(
            "scene_state", "combat"
    ));
    private static final Set<String> SAFE_TOP_LEVEL = new HashSet<>(Arrays.asList(
            "protocol_version", "narration", "visual_prompt", "options", "pending_test",
            "audio_plan", "narrative_state", "master_support", "sources", "rules_verified", "warnings", "narrative_quality_repair", "narrative_quality_repair_failed", "quality_recovered_locally", "scene_state", "combat",
            "state_updates", "npc_updates", "npc_cognition_updates", "knowledge_updates",
            "quest_updates", "faction_updates", "guild_updates", "clock_updates", "world_events",
            "event_resolutions", "pending_resolutions", "map_updates", "tactical_updates",
            "journal_updates"
    ));

    private MasterProtocolV1() {}

    public static JSONObject descriptor() {
        JSONObject o = new JSONObject();
        try {
            o.put("version", VERSION)
                    .put("mode", "structured_fail_closed")
                    .put("unknown_mutations", "reject")
                    .put("prose_is_authoritative", false)
                    .put("source_of_truth", SourceOfTruthRegistry.manifest());
            JSONArray a = new JSONArray(); for (String k : ARRAY_MUTATIONS) a.put(k);
            JSONArray b = new JSONArray(); for (String k : OBJECT_MUTATIONS) b.put(k);
            o.put("array_mutations", a).put("object_mutations", b);
        } catch(Exception ignored){AppLog.ignored("MasterProtocolV1","Falha tolerada registrada na consolidação 5.0.",ignored);}
        return o;
    }

    /**
     * Aceita narrativa mesmo quando o protocolo estiver ausente/incompatível, mas zera TODA
     * mutação estruturada. Assim um modelo antigo nunca consegue corromper o estado.
     */
    public static JSONObject enforce(JSONObject env) {
        if (env == null) return new JSONObject();
        final int version = env.optInt("protocol_version", 0);
        final boolean compatible = version == VERSION;
        try {
            JSONArray warnings = env.optJSONArray("warnings");
            if (warnings == null) warnings = new JSONArray();

            // Campos desconhecidos com aparência de mutação são sempre rejeitados, mesmo em v1.
            Iterator<String> it = env.keys();
            java.util.ArrayList<String> remove = new java.util.ArrayList<>();
            while (it.hasNext()) {
                String key = it.next();
                if (SAFE_TOP_LEVEL.contains(key) || key.startsWith("_")) continue;
                String lower = key.toLowerCase(java.util.Locale.ROOT);
                if (lower.endsWith("_updates") || lower.endsWith("_resolutions") ||
                        lower.contains("mutation") || lower.startsWith("set_") || lower.startsWith("delete_")) {
                    remove.add(key);
                }
            }
            for (String key : remove) {
                env.remove(key);
                warnings.put("Canal estrutural desconhecido rejeitado: " + key + ".");
            }

            if (!compatible) {
                env.put("protocol_rejected", true)
                        .put("protocol_expected", VERSION)
                        .put("protocol_received", version);
                closeAllMutations(env);
                warnings.put("Resposta estrutural ignorada: protocolo do Mestre incompatível.");
            }
            env.put("warnings", warnings);
        } catch (Exception e) {
            AppLog.w("MasterProtocolV1", "Falha ao fechar resposta incompatível.", e);
            try { closeAllMutations(env); } catch(Exception ignored){AppLog.ignored("MasterProtocolV1","Falha tolerada registrada na consolidação 5.0.",ignored);}
        }
        return env;
    }

    private static void closeAllMutations(JSONObject env) throws Exception {
        for (String key : ARRAY_MUTATIONS) env.put(key, new JSONArray());
        env.put("scene_state", new JSONObject());
        env.put("combat", new JSONObject().put("action", "none"));
        env.put("pending_test", JSONObject.NULL);
    }
}
