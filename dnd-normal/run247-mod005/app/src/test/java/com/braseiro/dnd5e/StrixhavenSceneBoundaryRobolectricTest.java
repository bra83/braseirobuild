package com.braseiro.dnd5e;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** A changed scene must not carry the previous location's narrative cast. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class StrixhavenSceneBoundaryRobolectricTest {

    private static JSONObject npc(String name) throws Exception {
        return new JSONObject().put("name", name).put("present", true);
    }

    private static JSONObject faerun() throws Exception {
        JSONArray visible = new JSONArray().put(npc("Guarda de Faerûn"));
        return new JSONObject().put("location", "Estrada de Faerûn")
            .put("hex_id", "AB42").put("day_number", 6).put("clock_hour", 14)
            .put("present_npcs", visible).put("present_npcs_json", visible.toString());
    }

    private static JSONObject campus() throws Exception {
        JSONArray visible = new JSONArray().put(npc("Biblioplex — bibliotecária"));
        return new JSONObject().put("location", "Strixhaven • Biblioplex")
            .put("strixhaven_map_id", "university")
            .put("strixhaven_poi_id", "biblioplex")
            .put("last_travel_kind", "poi")
            .put("present_npcs", visible).put("present_npcs_json", visible.toString());
    }

    @Test public void travelRemovesNpcPresenceWithoutChangingClockOrLocation() throws Exception {
        JSONObject next = StrixhavenSceneBoundary.enterNewLocation(campus());
        assertEquals("Strixhaven • Biblioplex", next.optString("location"));
        assertEquals("university", next.optString("strixhaven_map_id"));
        assertEquals(0, next.optJSONArray("present_npcs").length());
        assertEquals("[]", next.optString("present_npcs_json"));
    }

    @Test public void returningRestoresOwnFaerunCastAndSceneNotBiblioplex() throws Exception {
        JSONObject restored = StrixhavenSceneBoundary.returnToFaerun(
            faerun(), campus(), "estrada do Atlas", "AB42");
        assertEquals("Estrada de Faerûn", restored.optString("location"));
        assertEquals("AB42", restored.optString("hex_id"));
        assertFalse(restored.has("strixhaven_map_id"));
        assertFalse(restored.has("strixhaven_poi_id"));
        assertEquals("Guarda de Faerûn",
            restored.optJSONArray("present_npcs").getJSONObject(0).optString("name"));
        assertFalse(restored.toString().contains("Biblioplex"));
        assertEquals(14d, restored.optDouble("clock_hour", 0), 0.0001d);
    }

    @Test public void oldApkWithoutSnapshotMustNotKeepCampusPeopleOrLocation()
            throws Exception {
        JSONObject restored = StrixhavenSceneBoundary.returnToFaerun(
            new JSONObject(), campus(), "uma região do Atlas", "AA13");
        assertEquals("uma região do Atlas", restored.optString("location"));
        assertEquals("AA13", restored.optString("hex_id"));
        assertFalse(restored.toString().contains("Biblioplex"));
        assertEquals(0, restored.optJSONArray("present_npcs").length());
        assertEquals("[]", restored.optString("present_npcs_json"));
        assertFalse(restored.has("last_travel_kind"));
    }

    @Test public void previouslyContaminatedSnapshotIsRejected() throws Exception {
        JSONObject restored = StrixhavenSceneBoundary.returnToFaerun(
            campus(), campus(), "Faerûn", "BC31");
        assertEquals("Faerûn", restored.optString("location"));
        assertEquals(0, restored.optJSONArray("present_npcs").length());
        assertFalse(restored.has("strixhaven_map_id"));
        assertEquals("BC31", restored.optString("hex_id"));
    }

    @Test public void threeSqliteReopensPreserveReturnedSceneAndNpcScope() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "strixscene_" + Long.toHexString(System.nanoTime() & 0xFFFFF) + ".db";
        AppDb db = new AppDb(context, name);
        try {
            db.setActiveMapProfile("dnd");
            JSONObject prior = faerun();
            db.saveSceneState(prior);
            db.saveSceneState(campus());
            JSONObject returned = StrixhavenSceneBoundary.returnToFaerun(
                prior, db.getSceneState(), "Faerûn", "AB42");
            db.saveSceneState(returned);
            for (int i = 0; i < 3; i++) {
                db.close();
                db = new AppDb(context, name);
                db.setActiveMapProfile("dnd");
                JSONObject stored = db.getSceneState();
                assertEquals("Estrada de Faerûn", stored.optString("location"));
                assertEquals("AB42", stored.optString("hex_id"));
                assertFalse(stored.toString().contains("Biblioplex"));
                assertFalse(stored.has("strixhaven_map_id"));
                assertEquals("Guarda de Faerûn",
                    new JSONArray(stored.optString("present_npcs_json", "[]"))
                        .getJSONObject(0).optString("name"));
            }
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }

    @Test public void fallbackArrivalNeverInventsAnNpc() {
        String label = StrixhavenSceneBoundary.waitingForArrival("pátio da Academia");
        assertTrue(label.contains("pátio da Academia"));
        assertFalse(label.contains("Biblioplex"));
    }
}
