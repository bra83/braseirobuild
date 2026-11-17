package com.braseiro.dnd5e;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/** A missing saved hex is not proof of travel: protect narrative sublocations. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class AtlasLegacySceneLocationPersistenceRobolectricTest {
    private static AppDb createWithAtlas(Context context, String name) {
        AppDb db = new AppDb(context, name);
        db.setActiveMapProfile("dnd");
        SQLiteDatabase sql = db.getWritableDatabase();
        sql.execSQL("INSERT INTO map_hex_atlas(package_id,hex_id,terrain_primary) " +
                    "VALUES('qa_atlas','D040','planície')");
        sql.execSQL("UPDATE map_world_state SET map_package_id='qa_atlas', " +
                    "current_hex_id='D040' WHERE world_profile='dnd'");
        return db;
    }

    @Test public void missingHexKeepsUnknownNarrativeLocationAndBindsMapPosition() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "qa_scene_legacy_" + UUID.randomUUID().toString().substring(0,8) + ".db";
        AppDb db = createWithAtlas(context, name);
        try {
            db.saveSceneState(new JSONObject().put("location", "Quarto acima da estalagem")
                    .put("hex_id", "").put("day_number", 17).put("clock_hour", 18.75));
            assertEquals("Quarto acima da estalagem",
                    AtlasScenePositionReconciler.authoritativeSceneSnapshot(db)
                            .optString("location"));
            JSONObject reconciled = AtlasScenePositionReconciler.reconcilePersistedScene(
                    db, "qa primeiro vínculo de Atlas");
            assertEquals("Quarto acima da estalagem", reconciled.optString("location"));
            assertEquals("D040", reconciled.optString("hex_id"));
            assertEquals(17, reconciled.optInt("day_number"));
            assertEquals(18.75, reconciled.optDouble("clock_hour"), 0.001);
            db.close();
            db = new AppDb(context, name);
            db.setActiveMapProfile("dnd");
            assertEquals("Quarto acima da estalagem", db.getSceneState().optString("location"));
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }

    @Test public void genuineMapMoveStillReconcilesStaleSceneLocation() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "qa_scene_moved_" + UUID.randomUUID().toString().substring(0,8) + ".db";
        AppDb db = createWithAtlas(context, name);
        try {
            db.saveSceneState(new JSONObject().put("location", "A rua anterior")
                    .put("hex_id", "D039").put("day_number", 17).put("clock_hour", 18.75));
            JSONObject after = AtlasScenePositionReconciler.reconcilePersistedScene(db, "qa real map travel");
            assertEquals("D040", after.optString("hex_id"));
            assertEquals("Esta região", after.optString("location"));
            assertEquals(17, after.optInt("day_number"));
            assertEquals(18.75, after.optDouble("clock_hour"), 0.001);
        } finally {
            db.close();
            context.deleteDatabase(name);
        }
    }
}
