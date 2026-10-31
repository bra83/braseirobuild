package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

/**
 * Monotonic, low-overhead startup checkpoints. Records durations only, not any
 * campaign content, credentials or character data. The most recent run can be
 * copied from Settings without ADB or deleting the application's data.
 */
public final class DndStartupTrace {
    private static final String TAG = "DndStartupTrace";
    private static final String PREF = "dnd_startup_diagnostics";
    private static final String LAST = "last_report";
    private static long startedAt;
    private static StringBuilder checkpoints = new StringBuilder();

    private DndStartupTrace() {}

    public static synchronized void begin() {
        startedAt = SystemClock.elapsedRealtime();
        checkpoints = new StringBuilder(512);
        mark("APPLICATION_BEGIN");
    }

    public static synchronized void mark(String step) {
        if (startedAt == 0L) begin();
        long elapsed = Math.max(0L, SystemClock.elapsedRealtime() - startedAt);
        String safe = step == null ? "UNKNOWN" : step.replaceAll("[^A-Z0-9_]", "_");
        checkpoints.append(safe).append("_MS=").append(elapsed).append('\n');
        Log.i(TAG, safe + "_MS=" + elapsed);
    }

    public static synchronized void persist(Context context) {
        if (context == null) return;
        String report = "Braseiro D&D " + BuildConfig.VERSION_NAME + "\n"
                + "Relogio: elapsedRealtime; tempos acumulados desde APPLICATION_BEGIN.\n"
                + checkpoints.toString();
        context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(LAST, report).apply();
    }

    public static String report(Context context) {
        SharedPreferences pref = context.getApplicationContext()
                .getSharedPreferences(PREF, Context.MODE_PRIVATE);
        String saved = pref.getString(LAST, "");
        String startup = saved == null || saved.length() == 0
                ? "Ainda nao existe medicao. Feche e abra o aplicativo novamente."
                : saved;
        String stall = DndUiStallMonitor.lastReport(context);
        return stall.isEmpty() ? startup : startup + "\nULTIMO_TRAVAMENTO_DE_INTERFACE:\n" + stall;
    }
}
