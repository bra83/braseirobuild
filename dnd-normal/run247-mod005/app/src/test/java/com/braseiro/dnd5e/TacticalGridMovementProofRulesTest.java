package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public final class TacticalGridMovementProofRulesTest {
    private JSONObject proof(int... xy) throws Exception {
        JSONArray cells = new JSONArray();
        for (int i = 0; i < xy.length; i += 2)
            cells.put(new JSONObject().put("x", xy[i]).put("y", xy[i + 1]));
        return new JSONObject().put("schema", TacticalGridMovementProofRules.SCHEMA)
            .put("actor_id", 7).put("scene_key", "scene-a").put("grid_fingerprint", "abc123")
            .put("from_x", xy[0]).put("from_y", xy[1])
            .put("to_x", xy[xy.length - 2]).put("to_y", xy[xy.length - 1])
            .put("distance_ft", (cells.length() - 1) * 5).put("movement_cost_ft", (cells.length() - 1) * 5)
            .put("route_cells", cells);
    }

    @Test public void onlyStraightCommittedCellPathsQualifyAndTheyMustApproachTarget() throws Exception {
        JSONObject straight = proof(0, 0, 1, 0, 2, 0, 3, 0, 4, 0);
        assertTrue(TacticalGridMovementProofRules.valid(straight, 7));
        assertTrue(TacticalGridMovementProofRules.qualifiesToward(straight, 7, 8, 0, 20));
        assertFalse(TacticalGridMovementProofRules.qualifiesToward(straight, 7, -4, 0, 20));
        assertFalse(TacticalGridMovementProofRules.qualifiesToward(straight, 7, 8, 0, 25));
        assertFalse(TacticalGridMovementProofRules.valid(straight, 8));
    }

    @Test public void bentOrForgedPathCannotQualify() throws Exception {
        JSONObject bent = proof(0, 0, 1, 0, 2, 0, 2, 1, 2, 2);
        assertTrue(TacticalGridMovementProofRules.valid(bent, 7));
        assertFalse(TacticalGridMovementProofRules.straight(bent, 7));
        assertFalse(TacticalGridMovementProofRules.qualifiesToward(bent, 7, 8, 8, 20));
        JSONObject forged = proof(0, 0, 1, 0, 2, 0, 3, 0, 4, 0);
        forged.put("distance_ft", 30);
        assertFalse(TacticalGridMovementProofRules.valid(forged, 7));
    }
}
