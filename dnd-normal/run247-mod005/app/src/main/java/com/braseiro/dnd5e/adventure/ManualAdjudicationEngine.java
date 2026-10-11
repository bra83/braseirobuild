package com.braseiro.dnd5e.adventure;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

/**
 * Adjudicação assistida entre Mestre de IA e jogador.
 * Não lança dados, não altera HP/itens/tempo/posições e não inventa CDs.
 * Somente a escolha explícita do jogador é incorporada à memória da aventura.
 */
public final class ManualAdjudicationEngine {
    private ManualAdjudicationEngine() {}

    public static JSONObject propose(AdventureState state, JSONObject spec) {
        if (state == null || spec == null) return blocked("Proposta de teste ausente.");
        if (state.getPendingManualAdjudication().length() != 0) return blocked("Existe um teste manual pendente.");
        String situation = normalized(spec.optString("situation", ""), 1600);
        String win = normalized(spec.optString("on_success", ""), 1600);
        String lose = normalized(spec.optString("on_failure", ""), 1600);
        if (situation.isEmpty() || win.isEmpty() || lose.isEmpty())
            return blocked("Informe situação e consequências de sucesso e fracasso.");
        String requestId = UUID.randomUUID().toString();
        JSONObject pending = new JSONObject();
        try {
            pending.put("request_id", requestId);
            pending.put("origin_node_id", state.getCurrentNodeId());
            pending.put("situation", situation);
            pending.put("check", normalized(spec.optString("check", ""), 800));
            pending.put("rule_source", normalized(spec.optString("rule_source", ""), 300));
            pending.put("on_success", win);
            pending.put("on_failure", lose);
            pending.put("status", "pending");
            pending.put("created_at", System.currentTimeMillis());
            state.setPendingManualAdjudication(pending);
            return new JSONObject().put("status", "requires_manual_result")
                    .put("committed", false).put("pending", pending).put("auto_rolls", false);
        } catch (Exception e) { return blocked("Não foi possível preparar a proposta: " + e.getMessage()); }
    }

    public static JSONObject resolve(AdventureState state, JSONObject input) {
        if (state == null || input == null) return blocked("Resultado ausente.");
        String requestId = input.optString("request_id", "");
        if (requestId.isEmpty()) return blocked("O ID do teste manual é obrigatório.");
        String choice = input.optString("outcome", "").trim();
        if (!("success".equals(choice) || "failure".equals(choice)
                || "mixed".equals(choice) || "custom".equals(choice)))
            return blocked("Resultado inválido. Use success, failure, mixed ou custom.");
        String rawNotes = input.optString("notes", "");
        if (rawNotes.length() > 2000) return blocked("Descrição do resultado maior que 2000 caracteres.");
        String notes = normalized(rawNotes, 2000);
        if (("mixed".equals(choice) || "custom".equals(choice)) && notes.isEmpty())
            return blocked("O resultado parcial ou personalizado exige uma descrição.");
        JSONObject pending = state.getPendingManualAdjudication();
        if (pending.length() == 0) {
            JSONArray history = state.getManualAdjudicationHistory();
            for (int i = history.length() - 1; i >= 0; i--) {
                JSONObject saved = history.optJSONObject(i);
                if (saved != null && requestId.equals(saved.optString("request_id"))) {
                    if (choice.equals(saved.optString("outcome")) && notes.equals(saved.optString("notes"))) {
                        try { return new JSONObject().put("status", "already_resolved")
                            .put("committed", true).put("resolution", saved).put("auto_rolls", false); }
                        catch (Exception e) { return blocked("Falha ao ler recibo."); }
                    }
                    return blocked("Resultado já confirmado; não é permitido substituí-lo silenciosamente.");
                }
            }
            return blocked("Nenhum teste manual pendente.");
        }
        if (!requestId.equals(pending.optString("request_id")))
            return blocked("ID diferente do teste pendente.");
        if (!state.getCurrentNodeId().equals(pending.optString("origin_node_id")))
            return blocked("A cena mudou. Não é seguro aplicar este resultado.");
        JSONArray facts = input.optJSONArray("observed_facts");
        if (facts != null && facts.length() > 20) return blocked("Limite de 20 fatos por resolução.");
        for (int i = 0; facts != null && i < facts.length(); i++) {
            if (!(facts.opt(i) instanceof String) || normalized(facts.optString(i), 300).isEmpty())
                return blocked("Um fato observado é inválido ou longo demais.");
            if (facts.optString(i).length() > 300) return blocked("Fato observado maior que 300 caracteres.");
        }
        JSONObject resolution = new JSONObject();
        try {
            resolution.put("request_id", requestId);
            resolution.put("origin_node_id", state.getCurrentNodeId());
            resolution.put("situation", pending.optString("situation"));
            resolution.put("check", pending.optString("check"));
            resolution.put("rule_source", pending.optString("rule_source"));
            resolution.put("outcome", choice);
            resolution.put("success", "success".equals(choice));
            resolution.put("notes", notes);
            resolution.put("narrative_consequence", "success".equals(choice)
                ? pending.optString("on_success") : "failure".equals(choice)
                ? pending.optString("on_failure") : notes);
            resolution.put("observed_facts", facts == null ? new JSONArray() : new JSONArray(facts.toString()));
            resolution.put("effects_applied", false); // alterações de recursos exigem executor tipado
            resolution.put("resolution_mode", "player_manual");
            resolution.put("confirmed_at", System.currentTimeMillis());
            // Só grava os fatos observados declarados pelo jogador; nunca executa um delta arbitrário da IA.
            for (int i = 0; facts != null && i < facts.length(); i++)
                state.addDiscoveredFact(facts.optString(i).trim());
            state.appendManualAdjudication(resolution);
            state.setPendingManualAdjudication(null);
            return new JSONObject().put("status", "resolved_manual")
                  .put("committed", true).put("resolution", resolution).put("auto_rolls", false);
        } catch (Exception e) { return blocked("Não foi possível registrar a escolha: " + e.getMessage()); }
    }

    private static String normalized(String value, int max) {
        if (value == null) return "";
        String v = value.trim();
        return v.length() <= max ? v : ""; // fail-closed, sem truncar significado
    }
    private static JSONObject blocked(String message) {
        JSONObject out = new JSONObject();
        try { out.put("status", "blocked").put("committed", false)
               .put("error", message).put("auto_rolls", false); } catch (Exception ignored) {}
        return out;
    }
}
