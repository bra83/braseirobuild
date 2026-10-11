package com.braseiro.dnd5e;

import org.json.JSONObject;

/** Pure time conversion for the optional offline catch-up preview (G11). */
public final class LivingWorldCatchUpRules {
    private static final long MILLIS_PER_HOUR = 3_600_000L;

    private LivingWorldCatchUpRules() {}

    /**
     * Builds the same preview plan consumed by LivingWorldCatchUpEngine. This
     * method deliberately does not mutate world state or persist a sync time.
     */
    public static JSONObject preview(boolean enabled, boolean adventureStarted,
            long lastSyncAt, long now, double configuredRate, int configuredMaxHours)
            throws Exception {
        JSONObject out = new JSONObject().put("enabled", enabled).put("preview_only", true);
        if (!enabled) return out;
        if (!adventureStarted) return out.put("reason", "adventure_not_started");
        if (lastSyncAt <= 0L || now <= lastSyncAt) return out.put("initialize_only", true);

        double realHours = (now - lastSyncAt) / (double) MILLIS_PER_HOUR;
        double rate = Math.max(1d, configuredRate);
        int maxHours = Math.max(1, configuredMaxHours);
        int gameHours = (int) Math.floor(realHours * rate / 24d);
        gameHours = Math.max(0, Math.min(maxHours, gameHours));
        return out.put("real_hours", round2(realHours))
                .put("rate_game_hours_per_real_day", rate)
                .put("game_hours", gameHours)
                .put("last_sync_at", lastSyncAt)
                .put("now", now);
    }

    private static double round2(double value) {
        return Math.round(value * 100d) / 100d;
    }
}
