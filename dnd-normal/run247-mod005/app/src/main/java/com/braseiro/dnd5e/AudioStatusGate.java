package com.braseiro.dnd5e;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Prevents scene-audio diagnostics from repeatedly replacing the player's
 * footer message. Audio playback, error reporting and stored diagnostics are
 * deliberately unaffected; this class gates UI status presentation only.
 */
public final class AudioStatusGate {
    private static final long REDISPLAY_MS = 20000L;
    private static final int MAX_KEYS = 48;
    private final LinkedHashMap<String, Long> seen = new LinkedHashMap<String, Long>();

    public synchronized boolean allow(String message, long elapsedMs) {
        if (message == null || message.trim().isEmpty()) return false;
        String key = message.trim().toLowerCase(Locale.ROOT);
        if (isRoutineSceneDiagnostic(key)) return false;
        Long previous = seen.get(key);
        if (previous != null && elapsedMs >= previous && elapsedMs - previous < REDISPLAY_MS)
            return false;
        seen.put(key, elapsedMs);
        while (seen.size() > MAX_KEYS) {
            String oldest = seen.keySet().iterator().next();
            seen.remove(oldest);
        }
        return true;
    }

    private static boolean isRoutineSceneDiagnostic(String key) {
        return key.startsWith("sfx: ")
            || key.startsWith("sfx não encontrado para ")
            || key.startsWith("áudio preparado pelo play: ")
            || key.startsWith("ambiência alternada para: ")
            || key.equals("clima pronto.")
            || key.equals("música pronta.")
            || key.equals("ambiência pronta.");
    }
}
