package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Non-intrusive UI stall monitor.
 * One heartbeat may be queued at a time. A stalled Looper must never cause
 * a growing list of Handler messages or delayed diagnostic tasks.
 *
 * This is instrumentation, not an ANR-dialog bypass: only removing the
 * blocking operation can prevent Android from raising an ANR.
 */
public final class UiResponsivenessMonitor {
    private static final long TICK_MS = 500L;
    private static final long SLOW_MS = 1500L;
    private static final long ANR_RISK_MS = 4500L;
    private static final int MAX_STACK_CHARS = 5500;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "braseiro-ui-watchdog");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private final SharedPreferences prefs;
    private final AtomicBoolean heartbeatPending = new AtomicBoolean(false);
    private volatile long sentAtUptime;
    private volatile boolean slowLogged;
    private volatile boolean anrLogged;
    private volatile boolean running;

    public UiResponsivenessMonitor(Context context) {
        prefs = context.getSharedPreferences("ui_responsiveness_v5", Context.MODE_PRIVATE);
    }

    public void start() {
        if (running) return;
        running = true;
        worker.scheduleAtFixedRate(this::tick, 0L, TICK_MS, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        if (!running) return;
        try {
            if (heartbeatPending.compareAndSet(false, true)) {
                sentAtUptime = SystemClock.uptimeMillis();
                slowLogged = false;
                anrLogged = false;
                if (!main.post(() -> {
                    heartbeatPending.set(false);
                })) {
                    heartbeatPending.set(false);
                }
                return;
            }

            long lag = SystemClock.uptimeMillis() - sentAtUptime;
            if (lag >= ANR_RISK_MS && !anrLogged) {
                anrLogged = true;
                record(lag, true);
            } else if (lag >= SLOW_MS && !slowLogged) {
                slowLogged = true;
                record(lag, false);
            }
        } catch (Throwable error) {
            // Instrumentation cannot become another source of app crashes.
            android.util.Log.w("UiResponsiveness", "Watchdog unavailable", error);
        }
    }

    private void record(long lag, boolean serious) {
        String stack = "";
        if (serious) {
            Thread thread = Looper.getMainLooper().getThread();
            StringBuilder trace = new StringBuilder();
            for (StackTraceElement frame : thread.getStackTrace()) {
                if (trace.length() >= MAX_STACK_CHARS) break;
                trace.append("  at ").append(frame.toString()).append('\n');
            }
            stack = trace.toString();
        }
        final int count = prefs.getInt("lag_count", 0) + (serious && slowLogged ? 0 : 1);
        final long max = Math.max(lag, prefs.getLong("max_lag_ms", 0L));
        SharedPreferences.Editor edit = prefs.edit()
            .putLong("max_lag_ms", max)
            .putLong("last_lag_ms", lag)
            .putLong("last_lag_at", System.currentTimeMillis())
            .putInt("lag_count", count);
        if (serious) edit.putString("last_ui_block_stack", stack);
        edit.apply(); // asynchronous persistence, never on the main Looper
        android.util.Log.w("UiResponsiveness",
            "main_thread_lag_ms=" + lag + " serious=" + serious
                + (serious ? "\n" + stack : ""));
    }

    public void stop() {
        running = false;
        worker.shutdownNow();
        heartbeatPending.set(false);
    }

    public long maxLagMs() { return prefs.getLong("max_lag_ms", 0L); }
    public int lagCount() { return prefs.getInt("lag_count", 0); }
    public String lastUiBlockStack() { return prefs.getString("last_ui_block_stack", ""); }
}
