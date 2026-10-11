package com.braseiro.dnd5e;

import org.json.JSONObject;

/**
 * A fonte descreve fome macabra como elemento ficcional do Dhampir, mas não fornece
 * uma tabela universal de DC/frenesi. Este engine é deliberadamente fail-closed.
 */
public final class DhampirHungerRuntimeEngine {
    public JSONObject checkFeedingNeed(int daysWithoutFeeding) {
        JSONObject res = new JSONObject();
        try {
            res.put("days_without_feeding", Math.max(0, daysWithoutFeeding));
            res.put("mechanic_defined_by_source", false);
            res.put("auto_rolls", false);
            res.put("status", "narrative_only");
            res.put("authority", "Astarion's Book of Hungers descreve a fome, mas não define DC universal ou Blood Frenzy para esta espécie.");
        } catch (Exception ignored) {}
        return res;
    }
}
