package com.braseiro.dnd5e;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Focused G11 tests for offline catch-up preview math and non-mutating plan states. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,manifest=Config.NONE)
public class LivingWorldCatchUpRulesTest {
    private static final long HOUR = 3_600_000L;

    @Test public void disabledAndUnstartedCampaignsDoNotProduceTickHours() throws Exception {
        JSONObject disabled = LivingWorldCatchUpRules.preview(false, true, 1, 25 * HOUR, 24, 72);
        assertTrue(disabled.optBoolean("preview_only"));
        assertFalse(disabled.optBoolean("enabled"));
        assertEquals(0, disabled.optInt("game_hours", 0));

        JSONObject unstarted = LivingWorldCatchUpRules.preview(true, false, 1, 25 * HOUR, 24, 72);
        assertEquals("adventure_not_started", unstarted.optString("reason"));
        assertEquals(0, unstarted.optInt("game_hours", 0));
    }

    @Test public void missingOrBackwardsSyncTimeOnlyInitializes() throws Exception {
        JSONObject missing = LivingWorldCatchUpRules.preview(true, true, 0, 25 * HOUR, 24, 72);
        assertTrue(missing.optBoolean("initialize_only"));
        assertFalse(missing.has("game_hours"));

        JSONObject backwards = LivingWorldCatchUpRules.preview(true, true, 20 * HOUR, 10 * HOUR, 24, 72);
        assertTrue(backwards.optBoolean("initialize_only"));
        assertFalse(backwards.has("game_hours"));
    }

    @Test public void convertsElapsedTimeByFloorAndRoundsReportedRealHours() throws Exception {
        JSONObject preview = LivingWorldCatchUpRules.preview(true, true,
                10 * HOUR, 22 * HOUR + 31 * 60_000L, 24, 72);
        assertEquals(12, preview.optInt("game_hours"));
        assertEquals(12.52d, preview.optDouble("real_hours"), 0.001d);
        assertTrue(preview.optBoolean("preview_only"));
    }

    @Test public void clampsRateAndConfiguredMaximumHours() throws Exception {
        JSONObject minimumRate = LivingWorldCatchUpRules.preview(true, true,
                1, 25 * HOUR + 1, 0.25d, 72);
        assertEquals(1d, minimumRate.optDouble("rate_game_hours_per_real_day"), 0.0d);
        assertEquals(1, minimumRate.optInt("game_hours"));

        JSONObject capped = LivingWorldCatchUpRules.preview(true, true,
                1, 24L * 100 * HOUR + 1, 24d, 3);
        assertEquals(3, capped.optInt("game_hours"));
    }
}
