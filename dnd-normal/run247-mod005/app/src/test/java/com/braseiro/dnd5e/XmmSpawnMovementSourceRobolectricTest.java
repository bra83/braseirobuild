package com.braseiro.dnd5e;

import android.content.Context;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35, manifest=Config.NONE)
public class XmmSpawnMovementSourceRobolectricTest {

    @Test
    public void compiledSpawnUsesPublishedWalkingSpeed() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        String database = "xmm_speed_" + System.nanoTime() + ".db";
        AppDb db = AppDbTestHelper.createSandbox(ctx, database);

        try {
            db.setActiveMapProfile("dnd");
            Xmm2025CompiledBestiaryEngine.install();

            JSONObject profile =
                CompiledStatblockEngine.runtimeProfile(
                    "Aarakocra Skirmisher");

            assertTrue(profile.optBoolean("found"));
            assertEquals(20,
                profile.getJSONObject("movement").getInt("walk"));
            assertEquals(50,
                profile.getJSONObject("movement").getInt("fly"));

            JSONObject spawned = db.addCompiledStatblockCombatant(
                "Aarakocra Skirmisher", "Batedor", 12,
                "ui-monster-spawn-speed-test-1");

            assertTrue(spawned.toString(),
                spawned.optBoolean("statblock_spawn_committed"));

            long id = spawned.getLong("combatant_id");
            JSONObject resources = new JSONObject(
                db.getCombatant(id).getString("resources_json"));

            assertEquals(20, resources.getInt("speed_ft"));

            JSONObject goblin = db.addCompiledStatblockCombatant(
                "Goblin Warrior", "Goblin", 8,
                "ui-monster-spawn-speed-test-2");

            assertTrue(goblin.toString(),
                goblin.optBoolean("statblock_spawn_committed"));

            JSONObject goblinResources = new JSONObject(
                db.getCombatant(goblin.getLong("combatant_id"))
                    .getString("resources_json"));

            assertEquals(30, goblinResources.getInt("speed_ft"));
        } finally {
            db.close();
            ctx.deleteDatabase(database);
        }
    }
}
