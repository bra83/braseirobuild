package com.braseiro.pathfinder2e;

import org.json.JSONArray;
import org.json.JSONObject;

public final class KingmakerTurnGuideRulesHarness {
    public static void main(String[] args) throws Exception {
        KingmakerRulesEngine rules = new KingmakerRulesEngine();

        JSONArray phases = rules.turnProcedureGuide();
        require(phases.length() == 4, "turnProcedureGuide precisa conter 4 fases");
        require("UPKEEP".equals(phases.optJSONObject(0).optString("key")), "fase 1");
        require("COMMERCE".equals(phases.optJSONObject(1).optString("key")), "fase 2");
        require("ACTIVITY".equals(phases.optJSONObject(2).optString("key")), "fase 3");
        require("EVENT".equals(phases.optJSONObject(3).optString("key")), "fase 4");

        JSONArray catalog = rules.activityGuideCatalog();
        require(catalog.length() == KingmakerRulesEngine.ACTIVITIES.length, "catálogo didático deve espelhar ACTIVITIES");
        require(catalog.length() >= 29, "catálogo incompleto");

        for (int i=0;i<catalog.length();i++) {
            JSONObject a=catalog.optJSONObject(i);
            require(a.optBoolean("mechanical_authority", false), "atividade sem autoridade mecânica: "+a.optString("key"));
            require("KingmakerRepository.resolveKingdomActivity".equals(a.optString("resolver")), "resolver divergente: "+a.optString("key"));
            require(a.optString("name").length()>0, "nome ausente");
            require(a.optString("changes").length()>0, "changes ausente: "+a.optString("key"));
            require(a.optJSONArray("parameters")!=null, "parameters ausente: "+a.optString("key"));
        }

        JSONObject taxes=rules.activityGuide("collect_taxes");
        require(taxes.optString("changes").contains("RP"), "Collect Taxes deve informar RP");
        require(taxes.optString("changes").contains("Unrest"), "Collect Taxes deve informar Unrest");

        JSONObject build=rules.activityGuide("build_structure");
        require(build.optString("requirements").contains("lotes"), "Build Structure deve informar lotes");
        require(build.optString("changes").contains("commodities"), "Build Structure deve informar commodities");

        require(!rules.canPerformActivity("claim_hex", 20), "Anarquia não deve liberar claim_hex");
        require(rules.canPerformActivity("quell_unrest", 20), "Anarquia deve permitir quell_unrest");

        System.out.println("KINGMAKER_TURN_GUIDE_RULES_HARNESS_PASS");
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new IllegalStateException(message);
    }
}
