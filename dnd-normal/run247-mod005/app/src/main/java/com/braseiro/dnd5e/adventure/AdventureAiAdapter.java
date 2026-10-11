package com.braseiro.dnd5e.adventure;

import com.braseiro.dnd5e.MasterOrchestrator;
import org.json.JSONObject;

/**
 * Adaptador de IA para o Adventure Engine.
 * Garante a regra fundamental de arquitetura:
 * A IA propõe intent e narra apenas após resolução mecânica e confirmação transacional do estado.
 * A IA NÃO PODE alterar o estado diretamente.
 */
public final class AdventureAiAdapter {
    private final MasterOrchestrator orchestrator;

    public AdventureAiAdapter(MasterOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Monta o contexto descritivo e diretivas de Mestre para a narração da IA
     * baseado estritamente no nó atual confirmado e no resultado mecânico.
     */
    public JSONObject buildNarrationPayload(AdventureNode node, AdventureState state, JSONObject resolution) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("authority", "O aplicativo é a autoridade mecânica e persistente. O Gemini não aplica mudanças diretamente.");
            if (node != null) {
                payload.put("node_id", node.getId());
                payload.put("node_title", node.getTitle());
                payload.put("node_description", node.getDescription());
                payload.put("gm_directives", node.getGmDirectives());
                payload.put("location_id", node.getLocationId());
            }
            if (state != null) {
                payload.put("current_node", state.getCurrentNodeId());
                payload.put("state", state.toJson());
            }
            if (resolution != null) {
                payload.put("mechanical_resolution", resolution);
                payload.put("resolution_success", resolution.optBoolean("success", true));
            }
            if (orchestrator != null) {
                JSONObject gate = orchestrator.validateEnvelope(payload, "adventure_narration");
                payload.put("orchestrator_gate", gate);
            }
        } catch (Exception ignored) {}
        return payload;
    }
}
