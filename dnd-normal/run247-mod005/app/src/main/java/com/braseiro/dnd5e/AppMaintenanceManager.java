package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

/** Manutenção local: snapshots periódicos, backup SAF e contenção de caches. */
public final class AppMaintenanceManager {
    private static final long SNAPSHOT_INTERVAL = 15L * 60L * 1000L;
    private static final long BACKUP_INTERVAL = 6L * 60L * 60L * 1000L;
    private final Context context;
    private final SharedPreferences prefs;
    private final CampaignSafetyManager safety;
    private final PortableBackupManager portableBackup;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private volatile boolean running;
    private final AtomicBoolean maintenanceRunning = new AtomicBoolean(false);

    public AppMaintenanceManager(Context context, CampaignSafetyManager safety, PortableBackupManager portableBackup) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        this.safety = safety;
        this.portableBackup = portableBackup;
    }

    public void start() { if (running) return; running = true; handler.postDelayed(tick, SNAPSHOT_INTERVAL); }
    public void stop() { running = false; handler.removeCallbacks(tick); }
    public void onBackground() { runMaintenance("Aplicativo enviado ao segundo plano", true); }

    private final Runnable tick = new Runnable() { public void run() { if (!running) return; runMaintenance("Snapshot periódico", true); handler.postDelayed(this, SNAPSHOT_INTERVAL); } };

    public void runMaintenance(final String reason, final boolean allowPortable) {
        if (!maintenanceRunning.compareAndSet(false, true)) return;
        AppExecutors.io().execute(new Runnable() { public void run() {
            long now = System.currentTimeMillis();
            try {
                try {
                    long last = prefs.getLong("maintenance_last_snapshot", 0L);
                    if (safety != null && now - last >= SNAPSHOT_INTERVAL) {
                        safety.createSnapshot(reason);
                        prefs.edit().putLong("maintenance_last_snapshot", now).apply();
                    }
                } catch (Exception e) {
                    AppLog.e("Maintenance", "Snapshot automático falhou.", e);
                }

                try {
                    pruneCaches();
                } catch (Exception e) {
                    AppLog.w("Maintenance", "Limpeza de cache falhou.", e);
                }

                if (allowPortable && portableBackup != null && prefs.getBoolean("automatic_drive_backup", false)) {
                    long last = prefs.getLong("maintenance_last_portable_backup", 0L);
                    if (now - last >= BACKUP_INTERVAL) {
                        try {
                            JSONObject result = portableBackup.createAutomaticBackup(prefs.getInt("automatic_backup_retention", 5));
                            prefs.edit().putLong("maintenance_last_portable_backup", now)
                                    .putString("maintenance_last_portable_name", result.optString("name"))
                                    .putString("maintenance_last_portable_error", "").apply();
                        } catch (Exception e) {
                            prefs.edit().putString("maintenance_last_portable_error", safe(e)).apply();
                            AppLog.e("Maintenance", "Backup automático externo falhou.", e);
                        }
                    }
                }
            } finally {
                maintenanceRunning.set(false);
            }
        }});
    }

    private void pruneCaches() {
        int tokenMb = Math.max(100, prefs.getInt("token_cache_mb", 500));
        pruneDirectory(new File(context.getCacheDir(), "pdf_token_library"), tokenMb * 1024L * 1024L);
        pruneDirectory(new File(context.getFilesDir(), "tts_cache_v7"), 180L * 1024L * 1024L);
        StatFs fs = new StatFs(context.getFilesDir().getAbsolutePath());
        if (fs.getAvailableBytes() < 256L * 1024L * 1024L) {
            pruneDirectory(context.getCacheDir(), 250L * 1024L * 1024L);
        }
    }

    private void pruneDirectory(File root, long maximumBytes) {
        if (root == null || !root.exists()) return;
        ArrayList<File> files = new ArrayList<File>(); collect(root, files);
        long total = 0L; for (File file : files) total += file.length();
        if (total <= maximumBytes) return;
        Collections.sort(files, new Comparator<File>() { public int compare(File a, File b) { return Long.compare(a.lastModified(), b.lastModified()); } });
        for (File file : files) {
            if (total <= maximumBytes) break;
            long length = file.length();
            if (file.delete()) total -= length;
        }
    }

    private void collect(File file, ArrayList<File> out) {
        if (file == null || !file.exists()) return;
        if (file.isFile()) { out.add(file); return; }
        File[] children = file.listFiles(); if (children == null) return;
        for (File child : children) collect(child, out);
    }

    private static String safe(Exception e) { return e == null || e.getMessage() == null ? "Falha desconhecida" : e.getMessage(); }
}
