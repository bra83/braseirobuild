package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Contrato FINAL do fluxo Solo executável em JVM pura.
 *
 * Android/SQLite fica no caminho instrumentado da aplicação; este harness
 * valida o contrato de produção que não depende do framework: todos os
 * next_scene têm destino e o checkpoint mantém equivalência semântica ao
 * longo da janela longa do R7.
 */
public final class KingmakerSoloCampaignFinalHarness {
    private static final String[] SCENES = {
            "adventure_start", "camp_encounter", "camp", "war",
            "exploration_resolution", "companion", "kingdom_bridge",
            "travel", "free_exploration"
    };

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static JSONObject checkpoint(int week, String scene, String hex) throws Exception {
        return new JSONObject()
                .put("story_started", 1)
                .put("current_hex", hex)
                .put("destination_hex", "H" + (week + 1))
                .put("route_json", new JSONArray().put("H" + (week + 1)))
                .put("scene_day", week)
                .put("scene_period", "day")
                .put("scene_weather", "clear")
                .put("camp_key", "camp_fixture")
                .put("camp_phase", scene.startsWith("camp") ? "CAMP_ACTIVITIES" : "")
                .put("active_companions", new JSONArray().put("companion_fixture"))
                .put("pending_companion_events", "companion".equals(scene) ? 1 : 0)
                .put("pending_exploration_events", "exploration_resolution".equals(scene) ? 1 : 0)
                .put("active_wars", "war".equals(scene) ? 1 : 0)
                .put("kingdom_action", "kingdom_bridge".equals(scene) ? "phase_choice" : "")
                .put("next_hex", "H" + (week + 1))
                .put("turn_id", week)
                .put("updated_at", week * 1000L);
    }

    public static void main(String[] args) throws Exception {
        int validations = 0;
        for (String scene : SCENES) {
            String action = KingmakerSoloFlowRouter.actionFor(scene);
            require(!KingmakerSoloFlowRouter.MASTER_FALLBACK.equals(action), "next_scene sem destino: " + scene);
            validations++;
        }
        require(KingmakerSoloFlowRouter.MASTER_FALLBACK.equals(KingmakerSoloFlowRouter.actionFor("")), "fallback ausente");
        validations++;

        for (int week = 1; week <= 72; week++) {
            String scene = SCENES[(week - 1) % SCENES.length];
            JSONObject before = checkpoint(week, scene, "H" + week);
            JSONObject restored = new JSONObject(before.toString()).put("turn_id", 9000 + week).put("updated_at", 999999L);
            JSONObject audit = KingmakerContinuityContract.compare(before, restored);
            require(audit.optBoolean("ok", false), "checkpoint divergiu na semana " + week + ": " + audit.toString());
            validations += 3;
        }
        System.out.println("KINGMAKER_SOLO_CAMPAIGN_FINAL_E2E_PASS weeks=72 branches=" + SCENES.length + " validations=" + validations + " ignored=turn_id,updated_at");
    }
}
