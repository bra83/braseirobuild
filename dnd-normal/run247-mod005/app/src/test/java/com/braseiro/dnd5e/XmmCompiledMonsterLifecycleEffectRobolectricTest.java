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
@Config(sdk = 35, manifest = Config.NONE)
public final class XmmCompiledMonsterLifecycleEffectRobolectricTest {

    @Test
    public void testRecurringDamageLifecycleEffectAppliedAndPersisted() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        String dbName = "xmm_life_test_" + System.nanoTime() + ".db";
        AppDb db = new AppDb(ctx, dbName);
        try {
            db.setActiveMapProfile("dnd");
            long monster = db.addCombatant("Pit Fiend", 18, 300, 300, 19, "", false);
            long target = db.addCombatant("Rosa", 14, 50, 50, 16, "poisoned", true);
            db.setCombatRunning(true);

            // Kind 0: Recurring damage tick (e.g. 6d6 poison, 21 damage)
            JSONObject preview = new JSONObject()
                    .put("resolved", true)
                    .put("damage_total", 21)
                    .put("damage_type", "poison")
                    .put("monster", "Pit Fiend")
                    .put("action", "Bite");

            JSONObject applied = db.applyCompiledMonsterLifecycleEffect(
                    monster, target, "Pit Fiend", "Bite", 0, preview, "ui-monster-test-life-001");

            assertTrue("Effect should be applied: " + applied, applied.optBoolean("effect_applied"));
            assertTrue(applied.optBoolean("persisted_to_combat"));
            assertEquals("combat-v1:" + monster + ":ui-monster-test-life-001-life-eff", applied.optString("receipt_key"));

            // Verify target HP decreased from 50 to 29 in SQLite
            JSONObject targetAfter = db.getCombatant(target);
            assertEquals(29, targetAfter.optInt("hp"));

            // Replay protection: same token must be blocked
            JSONObject replay = db.applyCompiledMonsterLifecycleEffect(
                    monster, target, "Pit Fiend", "Bite", 0, preview, "ui-monster-test-life-001");
            assertEquals("already_resolved_or_replayed", replay.optString("effect_commit_blocked"));

            // Reopen database and verify persistent state
            db.close();
            AppDb reopened = new AppDb(ctx, dbName);
            reopened.setActiveMapProfile("dnd");
            JSONObject reopenedTarget = reopened.getCombatant(target);
            assertEquals(29, reopenedTarget.optInt("hp"));
            reopened.close();
        } finally {
            ctx.deleteDatabase(dbName);
        }
    }

    @Test
    public void testSaveConditionLifecycleEffectAppliedAndPersisted() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        String dbName = "xmm_cond_test_" + System.nanoTime() + ".db";
        AppDb db = new AppDb(ctx, dbName);
        try {
            db.setActiveMapProfile("dnd");
            long monster = db.addCombatant("Medusa", 16, 127, 127, 15, "", false);
            long target = db.addCombatant("Valeros", 12, 60, 60, 18, "", true);
            db.setCombatRunning(true);

            // Kind 1: Save condition failed, apply restrained
            JSONObject preview = new JSONObject()
                    .put("resolved", true)
                    .put("condition_applied", true)
                    .put("conditions", "restrained")
                    .put("monster", "Medusa")
                    .put("action", "Petrifying Gaze");

            JSONObject applied = db.applyCompiledMonsterLifecycleEffect(
                    monster, target, "Medusa", "Petrifying Gaze", 1, preview, "ui-monster-test-life-002");

            assertTrue("Condition effect should be applied: " + applied, applied.optBoolean("effect_applied"));

            // Verify target received restrained condition
            JSONObject targetAfter = db.getCombatant(target);
            assertTrue("Target should have restrained condition: " + targetAfter.optString("conditions"),
                    targetAfter.optString("conditions").contains("restrained"));

            // Reopen database and verify persistent condition
            db.close();
            AppDb reopened = new AppDb(ctx, dbName);
            reopened.setActiveMapProfile("dnd");
            JSONObject reopenedTarget = reopened.getCombatant(target);
            assertTrue(reopenedTarget.optString("conditions").contains("restrained"));
            reopened.close();
        } finally {
            ctx.deleteDatabase(dbName);
        }
    }
}
