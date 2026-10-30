package com.braseiro.dnd5e;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

/**
 * One-time, map-authoritative entry into Faerun after Strixhaven.
 * Yartar/DM28 is a verified city/hex in the bundled 2025 atlas.
 * A deferred token move is never silently replaced with a fictional
 * scene location when the atlas is still being indexed.
 */
public final class StrixhavenFaerunArrivalEngine {
    public static final String ARRIVAL_HEX = "DM28";
    public static final String ARRIVAL_CITY = "Yartar";
    public static final String RENDEZVOUS_HEX = NewarreAndWaystoneEngine.HEX_ID; // DO40
    public static final String DONE_PREF = "dnd_strixhaven_yartar_dm28_arrived_v1";
    public static final String PENDING_PREF = "dnd_strixhaven_yartar_dm28_pending_v1";

    private StrixhavenFaerunArrivalEngine() {}

    /**
     * Schedule only the first exit to Faerun; a later campus revisit must
     * restore the real Faerun journey instead of teleporting the character.
     */
    public static boolean requestFirstArrival(SharedPreferences prefs) {
        if (prefs == null || prefs.getBoolean(DONE_PREF, false)) return false;
        return prefs.edit().putBoolean(PENDING_PREF, true).commit();
    }

    public static boolean pending(SharedPreferences prefs) {
        return prefs != null && !prefs.getBoolean(DONE_PREF, false)
            && prefs.getBoolean(PENDING_PREF, false);
    }

    /**
     * Runs after the Faerun scene is restored and whenever the map opens.
     * Returns false without touching either position if the atlas is not ready.
     * The canonical map position, token, scene and optional rendezvous are
     * persisted together as a one-time transition (no passage of travel time).
     */
    public static boolean applyPending(AppDb db, SharedPreferences prefs) throws Exception {
        if (db == null || !pending(prefs)) return false;
        if (!"dnd".equalsIgnoreCase(db.getActiveMapProfile())) return false;
        JSONObject state = db.getMapState();
        String packageId = state.optString("map_package_id", "").trim();
        if (packageId.isEmpty() || !db.isMapPackageUsable(packageId)
                || !db.hasAtlasContractMetadata(packageId)
                || !db.isValidMapHex(packageId, ARRIVAL_HEX)) return false;

        // Do not rely on labels or a guessed coordinate: verify that Yartar
        // really is catalogued at DM28 inside the installed atlas.
        boolean verifiedCity = false;
        List<JSONObject> cityRows = db.getMapLocationsForHex(packageId, ARRIVAL_HEX);
        for (JSONObject candidate : cityRows) {
            if (ARRIVAL_CITY.equalsIgnoreCase(candidate.optString("name"))
                    && ARRIVAL_HEX.equalsIgnoreCase(candidate.optString("hex_id"))
                    && !candidate.optString("placement_status", "").contains("review")) {
                verifiedCity = true;
                break;
            }
        }
        if (!verifiedCity) return false;

        JSONObject row = db.getRawMapHex(packageId, ARRIVAL_HEX);
        double u = row.optDouble("center_u", Double.NaN);
        double v = row.optDouble("center_v", Double.NaN);
        if (!(u >= 0d && u <= 1d && v >= 0d && v <= 1d)
                || Double.isInfinite(u) || Double.isInfinite(v)) return false;

        String frame = db.getCoordinateFrameIdForPackage(packageId);
        db.saveAtlasPositionForProfile("dnd", packageId, ARRIVAL_HEX,
            u, v, frame, "ready", "{}");
        db.saveMapTravelStateForProfile("dnd", false, "", "[]", "Jogador");

        JSONObject next = StrixhavenSceneBoundary.enterNewLocation(db.getSceneState());
        next.remove("strixhaven_map_id");
        next.remove("strixhaven_poi_id");
        next.remove("adventure_profile");
        next.put("location", ARRIVAL_CITY);
        next.put("hex_id", ARRIVAL_HEX);
        next.put("world_region", "Vale do Dessarin");
        next.put("strixhaven_return_entry", "yartar_dm28_first_arrival");
        next.put("rendezvous_hex", RENDEZVOUS_HEX);
        next.put("rendezvous_status", "opcional_ainda_nao_realizado");
        next.put("present_npcs", new JSONArray());
        next.put("present_npcs_json", "[]");
        db.saveSceneState(next);

        // Only mark completion when the map and scene really agree.
        if (!ARRIVAL_HEX.equalsIgnoreCase(
                db.getMapState().optString("current_hex_id"))
                || !ARRIVAL_HEX.equalsIgnoreCase(db.getSceneState().optString("hex_id"))
                || !ARRIVAL_CITY.equalsIgnoreCase(db.getSceneState().optString("location"))) {
            throw new IllegalStateException("A chegada a Yartar nao foi persistida");
        }
        if (!prefs.edit().putBoolean(DONE_PREF, true).remove(PENDING_PREF).commit())
            throw new IllegalStateException("Nao foi possivel salvar o marcador da primeira chegada");
        return true;
    }
}
