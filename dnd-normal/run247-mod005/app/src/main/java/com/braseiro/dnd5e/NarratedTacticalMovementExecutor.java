package com.braseiro.dnd5e;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * G08/G09 bridge for a narrated tactical position update. The Master supplies
 * only a combatant identity and a normalized destination; the executor derives
 * the destination cell, asks the persisted grid for a route, and delegates the
 * actual budget/position/receipt transaction to AppDb's established executor.
 * Unsupported opportunity attacks are rejected until the reaction UI can
 * supply their explicit choices.
 */
public final class NarratedTacticalMovementExecutor {
    private NarratedTacticalMovementExecutor() {}

    public static JSONObject preflight(AppDb db, JSONObject proposal, String sceneCommitId,
            int ordinal) throws Exception {
        JSONObject blocked = new JSONObject();
        if (db == null || proposal == null) return blocked.put("narrated_tactical_move_blocked", "missing_input");
        String commitId = sceneCommitId == null ? "" : sceneCommitId.trim();
        long actorId = proposal.optLong("combatant_id", 0);
        if (commitId.length() < 16 || commitId.length() > 160 || ordinal < 0 || ordinal >= 40)
            return blocked.put("narrated_tactical_move_blocked", "invalid_scene_binding");
        if (actorId <= 0) return blocked.put("narrated_tactical_move_blocked", "combatant_identity_required");
        JSONObject actor = db.getCombatant(actorId);
        if (actor.length() == 0) return blocked.put("narrated_tactical_move_blocked", "combatant_not_found");
        if (proposal.has("name") && !actor.optString("name").equals(proposal.optString("name")))
            return blocked.put("narrated_tactical_move_blocked", "combatant_identity_mismatch");
        if (db.getCombatState().optInt("running", 0) != 1)
            return blocked.put("narrated_tactical_move_blocked", "encounter_not_running");
        if (!proposal.has("x") || !proposal.has("y") || !(proposal.opt("x") instanceof Number) ||
                !(proposal.opt("y") instanceof Number))
            return blocked.put("narrated_tactical_move_blocked", "normalized_destination_required");
        double nx = proposal.optDouble("x", Double.NaN), ny = proposal.optDouble("y", Double.NaN);
        if (!finite(nx) || !finite(ny) || nx < 0d || nx >= 1d || ny < 0d || ny >= 1d)
            return blocked.put("narrated_tactical_move_blocked", "destination_out_of_bounds");

        JSONObject scene = new ProceduralTacticalSceneGenerator(db).ensureForCurrentScene();
        int width = scene.optInt("width_cells", 0), height = scene.optInt("height_cells", 0);
        if (width < 1 || height < 1 || width > TacticalGridMovementRouteRules.MAX_DIM ||
                height > TacticalGridMovementRouteRules.MAX_DIM)
            return blocked.put("narrated_tactical_move_blocked", "invalid_grid");
        int destX = (int) Math.floor(nx * width), destY = (int) Math.floor(ny * height);
        String token = operationToken(commitId, ordinal, actorId, nx, ny);
        if (db.hasEffectReceipt("combat-v1:" + actorId + ":ui-grid-" + token + "-position"))
            return blocked.put("narrated_tactical_move_blocked", "position_replayed");
        JSONObject route = db.previewTacticalGridMovement(actorId, destX, destY, false, false);
        if (!route.optBoolean("grid_valid", false))
            return blocked.put("narrated_tactical_move_blocked", route.optString("grid_blocked", "route_unavailable"));
        JSONArray opportunityAttacks = route.optJSONArray("opportunity_attacks");
        if (route.optBoolean("opportunity_segment_required", false) ||
                (opportunityAttacks != null && opportunityAttacks.length() > 0))
            return blocked.put("narrated_tactical_move_blocked", "opportunity_resolution_required")
                    .put("route_preview", route);
        return new JSONObject().put("narrated_tactical_move_preflight", true)
                .put("scene_commit_id", commitId).put("ordinal", ordinal)
                .put("combatant_id", actorId).put("destination_x", destX).put("destination_y", destY)
                .put("operation_token", token).put("route_preview", route);
    }

    /** Execute only after the owning scene transaction has passed its normal approval boundary. */
    public static JSONObject executeChecked(AppDb db, JSONObject proposal, String sceneCommitId,
            int ordinal) throws Exception {
        JSONObject plan = preflight(db, proposal, sceneCommitId, ordinal);
        if (!plan.optBoolean("narrated_tactical_move_preflight", false))
            throw new IllegalStateException(plan.optString("narrated_tactical_move_blocked", "movement_preflight_failed"));
        JSONObject route = plan.optJSONObject("route_preview");
        long actorId = plan.optLong("combatant_id", 0);
        int destX = plan.optInt("destination_x", -1), destY = plan.optInt("destination_y", -1);
        JSONObject moved = db.moveCombatantToGridCell(actorId, destX, destY, false, false,
                route.optString("grid_fingerprint"), "ui-grid-" + plan.optString("operation_token"));
        if (!moved.optBoolean("grid_movement_committed", false))
            throw new IllegalStateException(moved.optString("grid_blocked", "movement_commit_failed"));
        String receiptKey = "combat-v1:" + actorId + ":ui-grid-" + plan.optString("operation_token") + "-position";
        if (!db.hasEffectReceipt(receiptKey)) throw new IllegalStateException("movement_receipt_missing");
        JSONObject position = db.tacticalGridPosition(actorId);
        if (position.optInt("cell_x", -1) != destX || position.optInt("cell_y", -1) != destY)
            throw new IllegalStateException("movement_position_not_persisted");
        return moved.put("narrated_tactical_move_committed", true)
                .put("narrated_tactical_receipt", receiptKey)
                .put("scene_commit_id", sceneCommitId).put("ordinal", ordinal);
    }

    private static boolean finite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }

    private static String operationToken(String commitId, int ordinal, long actorId, double x, double y)
            throws Exception {
        String canonical = commitId + "|tactical-move|" + ordinal + "|" + actorId + "|" +
                Double.toHexString(x) + "|" + Double.toHexString(y);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(32);
        for (int i = 0; i < 16; i++) hex.append(String.format(java.util.Locale.ROOT, "%02x", digest[i]));
        return "narrative-" + hex;
    }
}
