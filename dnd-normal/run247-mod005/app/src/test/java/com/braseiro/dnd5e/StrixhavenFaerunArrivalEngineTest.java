package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.SQLiteMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
public class StrixhavenFaerunArrivalEngineTest {

    @Test public void firstExitWaitsForVerifiedAtlasWithoutTeleportingOrClearingProgress()
            throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        String name="yartar-exit-"+Long.toHexString(System.nanoTime())+".db";
        SharedPreferences prefs=ctx.getSharedPreferences(name,Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        AppDb db=new AppDb(ctx,name);
        try {
            db.setActiveMapProfile("dnd");
            JSONObject original=new JSONObject().put("location","Estrada antes da Universidade")
                .put("hex_id","AB42").put("day_number",17)
                .put("clock_hour",21);
            db.saveSceneState(original);
            assertTrue(StrixhavenFaerunArrivalEngine.requestFirstArrival(prefs));
            assertTrue(StrixhavenFaerunArrivalEngine.pending(prefs));
            assertFalse(StrixhavenFaerunArrivalEngine.applyPending(db,prefs));
            assertFalse(prefs.getBoolean(StrixhavenFaerunArrivalEngine.DONE_PREF,false));
            assertEquals("Estrada antes da Universidade",
                db.getSceneState().optString("location"));
            assertEquals("AB42",db.getSceneState().optString("hex_id"));
            assertEquals(17,db.getSceneState().optInt("day_number"));
            assertEquals(21,db.getSceneState().optInt("clock_hour"));
            assertTrue("Do not fabricate a map token while the atlas is absent",
                db.getMapState().optString("current_hex_id","").isEmpty());
        } finally {
            db.close();ctx.deleteDatabase(name);prefs.edit().clear().commit();
        }
    }

    @Test public void afterFirstVerifiedArrivalLaterCampusVisitsNeverResetTheJourney()
            throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        String name="yartar-done-"+Long.toHexString(System.nanoTime());
        SharedPreferences prefs=ctx.getSharedPreferences(name,Context.MODE_PRIVATE);
        prefs.edit().clear().putBoolean(StrixhavenFaerunArrivalEngine.DONE_PREF,true).commit();
        assertFalse(StrixhavenFaerunArrivalEngine.requestFirstArrival(prefs));
        assertFalse(StrixhavenFaerunArrivalEngine.pending(prefs));
        assertEquals("Yartar",StrixhavenFaerunArrivalEngine.ARRIVAL_CITY);
        assertEquals("DM28",StrixhavenFaerunArrivalEngine.ARRIVAL_HEX);
        assertEquals("DO40",StrixhavenFaerunArrivalEngine.RENDEZVOUS_HEX);
        prefs.edit().clear().commit();
    }

    @Test public void realStrixhavenProfileExitSchedulesOnlyFirstVerifiedLanding()
            throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        SharedPreferences settings=ctx.getSharedPreferences("settings",Context.MODE_PRIVATE);
        settings.edit().remove(StrixhavenFaerunArrivalEngine.DONE_PREF)
            .remove(StrixhavenFaerunArrivalEngine.PENDING_PREF)
            .remove(StrixhavenAdventureProfile.PREF_PROFILE).commit();
        try {
            StrixhavenAdventureProfile profile=new StrixhavenAdventureProfile(ctx);
            profile.activate();
            assertTrue(profile.isActive());
            profile.exitToFaerun();
            assertFalse(profile.isActive());
            assertTrue(StrixhavenFaerunArrivalEngine.pending(settings));
            // A later campus visit must preserve the previous Faerun journey.
            settings.edit().putBoolean(StrixhavenFaerunArrivalEngine.DONE_PREF,true)
                .remove(StrixhavenFaerunArrivalEngine.PENDING_PREF).commit();
            profile.activate();
            profile.exitToFaerun();
            assertFalse(StrixhavenFaerunArrivalEngine.pending(settings));
            assertTrue(settings.getBoolean(StrixhavenFaerunArrivalEngine.DONE_PREF,false));
        } finally {
            settings.edit().remove(StrixhavenFaerunArrivalEngine.DONE_PREF)
                .remove(StrixhavenFaerunArrivalEngine.PENDING_PREF)
                .remove(StrixhavenAdventureProfile.PREF_PROFILE).commit();
        }
    }

    @Test public void noPositionIsCreatedWithoutAnExplicitPlayerExit() throws Exception {
        Context ctx=RuntimeEnvironment.getApplication();
        String name="yartar-unrequested-"+Long.toHexString(System.nanoTime())+".db";
        SharedPreferences prefs=ctx.getSharedPreferences(name,Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        AppDb db=new AppDb(ctx,name);
        try {
            db.setActiveMapProfile("dnd");
            assertFalse(StrixhavenFaerunArrivalEngine.applyPending(db,prefs));
            assertFalse(StrixhavenFaerunArrivalEngine.pending(prefs));
        } finally {
            db.close();ctx.deleteDatabase(name);prefs.edit().clear().commit();
        }
    }
}
