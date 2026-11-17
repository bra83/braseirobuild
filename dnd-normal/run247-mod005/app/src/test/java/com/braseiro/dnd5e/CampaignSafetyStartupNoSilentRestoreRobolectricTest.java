package com.braseiro.dnd5e;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;

/**
 * Regression: a non-fatal logical ledger inconsistency must never trigger a
 * silent rollback to an older (though valid) campaign snapshot at startup.
 * Manual snapshot restoration remains separately available.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public final class CampaignSafetyStartupNoSilentRestoreRobolectricTest {
    @Test public void startupHealthFailurePreservesNewerSceneHeroAndJournal() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        String name = "qa_no_silent_restore_" +
            UUID.randomUUID().toString().substring(0, 8) + ".db";
        AppDb db = new AppDb(context, name);
        File snapshot = null;
        try {
            db.setActiveMapProfile("dnd");
            CampaignSafetyManager safety = new CampaignSafetyManager(context, db);
            // A snapshot of the empty initial campaign exists from before
            // the player's session, just like on a device after first install.
            snapshot = safety.createSnapshot("Before user progress");
            SQLiteDatabase sql = db.getWritableDatabase();
            sql.execSQL("INSERT INTO characters(world_profile,name,species,class_name," +
                "level,hp,max_hp,ac,sheet_json,active) VALUES" +
                "('dnd','QA Hero','Humano','Mago',2,11,11,13,'{}',1)");
            db.addJournal("QA Journal", "A campaign must remain on disk", "story");
            db.saveSceneState(new JSONObject()
                .put("location", "QA_PREVIOUS_PROGRESS")
                .put("day_number", 17).put("clock_hour", 18.75));
            // Deliberately inject an invalid legacy transaction: the database
            // remains structurally sound but business health rejects blank key.
            sql.execSQL("INSERT INTO pending_transactions(world_profile,title," +
                "payload_json,created_at) VALUES('dnd','QA invalid','{}',1)");
            assertFalse("Fixture must fail health check",
                db.healthCheck().optBoolean("healthy", true));

            String message = safety.startupCheck();
            assertTrue("Require explicit recovery, got: " + message,
                message.contains("não foram sobrescritos"));
            assertEquals("QA_PREVIOUS_PROGRESS",
                db.getSceneState().optString("location"));
            assertEquals(17, db.getSceneState().optInt("day_number"));
            assertEquals(18.75, db.getSceneState().optDouble("clock_hour"), 0.001);
            assertEquals("QA Hero", db.listCharacters().get(0).optString("name"));
            assertEquals(1, db.listJournal().size());
            assertFalse("Automatic startup must not wipe invalid ledger either",
                db.healthCheck().optBoolean("healthy", true));
        } finally {
            db.close();
            context.deleteDatabase(name);
            if (snapshot != null) snapshot.delete();
        }
    }
}
