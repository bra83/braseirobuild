package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;

/** Source of truth for a committed, cell-by-cell voluntary movement path. */
public final class TacticalGridMovementProofRules {
    public static final String SCHEMA = "tactical_grid_path_v1";
    private TacticalGridMovementProofRules() { }

    public static JSONObject receipt(long actorId, JSONObject committedPlan) throws Exception {
        if (actorId <= 0 || committedPlan == null ||
            !committedPlan.optBoolean("grid_valid", false) ||
            !committedPlan.has("scene_key") || !committedPlan.has("grid_fingerprint"))
            throw new IllegalArgumentException("invalid_committed_grid_path");
        JSONObject proof = new JSONObject()
            .put("schema", SCHEMA)
            .put("actor_id", actorId)
            .put("scene_key", committedPlan.getString("scene_key"))
            .put("grid_fingerprint", committedPlan.getString("grid_fingerprint"))
            .put("from_x", committedPlan.getInt("from_x"))
            .put("from_y", committedPlan.getInt("from_y"))
            .put("to_x", committedPlan.getInt("to_x"))
            .put("to_y", committedPlan.getInt("to_y"))
            .put("distance_ft", committedPlan.getInt("distance_ft"))
            .put("movement_cost_ft", committedPlan.getInt("movement_cost_ft"))
            .put("route_cells", new JSONArray(committedPlan.getJSONArray("route_cells").toString()));
        if (!valid(proof, actorId)) throw new IllegalArgumentException("invalid_committed_grid_path");
        return proof;
    }

    public static boolean valid(JSONObject proof, long actorId) {
        if (proof == null || !SCHEMA.equals(proof.optString("schema")) ||
            proof.optLong("actor_id", 0) != actorId || actorId <= 0 ||
            proof.optString("scene_key", "").isEmpty() ||
            proof.optString("grid_fingerprint", "").isEmpty() ||
            proof.optInt("distance_ft", -1) < 0 || proof.optInt("movement_cost_ft", -1) < 0)
            return false;
        JSONArray cells = proof.optJSONArray("route_cells");
        if (cells == null || cells.length() < 1 || cells.length() > 201) return false;
        JSONObject first = cells.optJSONObject(0), last = cells.optJSONObject(cells.length() - 1);
        if (first == null || last == null || first.optInt("x", Integer.MIN_VALUE) != proof.optInt("from_x", Integer.MAX_VALUE) ||
            first.optInt("y", Integer.MIN_VALUE) != proof.optInt("from_y", Integer.MAX_VALUE) ||
            last.optInt("x", Integer.MIN_VALUE) != proof.optInt("to_x", Integer.MAX_VALUE) ||
            last.optInt("y", Integer.MIN_VALUE) != proof.optInt("to_y", Integer.MAX_VALUE) ||
            proof.optInt("distance_ft", -1) != (cells.length() - 1) * 5) return false;
        for (int i = 1; i < cells.length(); i++) {
            JSONObject before = cells.optJSONObject(i - 1), after = cells.optJSONObject(i);
            if (before == null || after == null) return false;
            int sx = after.optInt("x", Integer.MIN_VALUE) - before.optInt("x", Integer.MAX_VALUE);
            int sy = after.optInt("y", Integer.MIN_VALUE) - before.optInt("y", Integer.MAX_VALUE);
            if (Math.abs(sx) > 1 || Math.abs(sy) > 1 || (sx == 0 && sy == 0)) return false;
        }
        return true;
    }

    public static boolean straight(JSONObject proof, long actorId) {
        if (!valid(proof, actorId)) return false;
        JSONArray cells = proof.optJSONArray("route_cells");
        if (cells.length() < 2) return false;
        int dx = 0, dy = 0;
        for (int i = 1; i < cells.length(); i++) {
            JSONObject before = cells.optJSONObject(i - 1), after = cells.optJSONObject(i);
            int sx = after.optInt("x") - before.optInt("x");
            int sy = after.optInt("y") - before.optInt("y");
            if (i == 1) { dx = sx; dy = sy; }
            else if (sx != dx || sy != dy) return false;
        }
        return true;
    }

    /** The committed straight path must move closer to the selected target. */
    public static boolean qualifiesToward(JSONObject proof, long actorId, int targetX, int targetY,
        int requiredFeet) {
        if (requiredFeet < 1 || !straight(proof, actorId) || proof.optInt("distance_ft") < requiredFeet)
            return false;
        long sx = (long) proof.optInt("from_x") - targetX;
        long sy = (long) proof.optInt("from_y") - targetY;
        long ex = (long) proof.optInt("to_x") - targetX;
        long ey = (long) proof.optInt("to_y") - targetY;
        long before = sx * sx + sy * sy, after = ex * ex + ey * ey;
        return before > 0 && after < before;
    }
}
