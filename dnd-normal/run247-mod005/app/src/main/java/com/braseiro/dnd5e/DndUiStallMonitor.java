package com.braseiro.dnd5e;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight Android main-looper stall diagnostics.
 * Never attempts to hide the system ANR dialog, alter Android timeouts or restart the app.
 * Captures ONLY Java class/method/line names; no story, character, prompts or user text.
 */
public final class DndUiStallMonitor {
    private static final String PREF = "dnd_ui_stall_diagnostics";
    private static final String LAST = "last_stall";
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean PENDING = new AtomicBoolean(false);
    private static final AtomicBoolean RECORDED = new AtomicBoolean(false);
    private static volatile long sentAtMs;
    private static volatile int stallCount;

    private DndUiStallMonitor() {}

    public static void start(Context context) {
        if (context == null || Looper.getMainLooper() == null || !STARTED.compareAndSet(false,true)) return;
        final Context app = context.getApplicationContext();
        final Thread main = Looper.getMainLooper().getThread();
        final Handler handler = new Handler(Looper.getMainLooper());
        final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r,"braseiro-ui-stall-watchdog");
            t.setDaemon(true);
            return t;
        });
        worker.scheduleAtFixedRate(() -> {
            try {
                long now = SystemClock.uptimeMillis();
                if (PENDING.compareAndSet(false,true)) {
                    sentAtMs = now;
                    RECORDED.set(false);
                    handler.post(() -> PENDING.set(false));
                } else if (now - sentAtMs >= 2500L && RECORDED.compareAndSet(false,true)) {
                    stallCount++;
                    StringBuilder report = new StringBuilder(768);
                    report.append("UI_STALL_OBSERVED_MS=").append(now-sentAtMs)
                          .append("\nUI_STALL_COUNT_PROCESS=").append(stallCount)
                          .append("\nUI_THREAD_TOP_FRAMES:\n");
                    StackTraceElement[] trace = main.getStackTrace();
                    for (int i=0;i<trace.length && i<24;i++) {
                        StackTraceElement frame=trace[i];
                        report.append(frame.getClassName()).append('.')
                              .append(frame.getMethodName()).append(':')
                              .append(frame.getLineNumber()).append('\n');
                    }
                    String evidence=report.toString();
                    app.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit()
                        .putString(LAST,evidence).apply();
                    Log.w("BraseiroUiStall",evidence);
                }
            } catch (Throwable error) {
                Log.w("BraseiroUiStall","Monitor falhou; o jogo continua sem alteração.",error);
            }
        },500L,500L, TimeUnit.MILLISECONDS);
    }

    public static String lastReport(Context context) {
        if (context==null) return "";
        return context.getApplicationContext().getSharedPreferences(PREF,Context.MODE_PRIVATE)
            .getString(LAST,"");
    }
}
