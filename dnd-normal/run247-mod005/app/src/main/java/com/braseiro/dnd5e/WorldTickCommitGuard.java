package com.braseiro.dnd5e;

/** Fail-closed validation for a world-tick preview immediately before mutation. */
public final class WorldTickCommitGuard {
    private WorldTickCommitGuard() {}

    public static String rejection(boolean previewOnly, boolean committed,
            String expectedPayload, String actualPayload,
            String expectedScene, String actualScene,
            String expectedClocks, String actualClocks) {
        if (!previewOnly) return "world_tick_not_preview";
        if (committed) return "world_tick_already_committed";
        if (empty(expectedPayload) || !expectedPayload.equals(actualPayload))
            return "world_tick_payload_invalid_or_changed";
        if (empty(expectedScene) || !expectedScene.equals(actualScene))
            return "world_tick_scene_stale";
        if (empty(expectedClocks) || !expectedClocks.equals(actualClocks))
            return "world_tick_clocks_stale";
        return "";
    }

    private static boolean empty(String value) { return value == null || value.trim().length() == 0; }
}
