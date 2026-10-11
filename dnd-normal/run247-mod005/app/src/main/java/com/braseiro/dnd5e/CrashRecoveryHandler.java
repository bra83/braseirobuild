package com.braseiro.dnd5e;

import android.content.Context;
import android.util.AtomicFile;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/** Captura falhas não tratadas, preserva diagnóstico e tenta salvar a campanha. */
public final class CrashRecoveryHandler implements Thread.UncaughtExceptionHandler {
    private final Context context;
    private final AppDb db;
    private final CampaignSafetyManager safety;
    private final Thread.UncaughtExceptionHandler previous;
    private final AtomicBoolean handling = new AtomicBoolean(false);

    private CrashRecoveryHandler(Context context, AppDb db, CampaignSafetyManager safety) {
        this.context = context.getApplicationContext();
        this.db = db;
        this.safety = safety;
        this.previous = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static CrashRecoveryHandler install(Context context, AppDb db, CampaignSafetyManager safety) {
        CrashRecoveryHandler handler = new CrashRecoveryHandler(context, db, safety);
        Thread.setDefaultUncaughtExceptionHandler(handler);
        return handler;
    }

    @Override public void uncaughtException(Thread thread, Throwable error) {
        if (handling.compareAndSet(false, true)) {
            try { writeReport(thread, error); }
            catch (Throwable reportError) { AppLog.e("CrashRecovery", "Falha ao registrar erro fatal.", reportError); }
            try { if (safety != null) safety.createSnapshot("Recuperação após falha não tratada"); }
            catch (Throwable snapshotError) { AppLog.e("CrashRecovery", "Falha ao criar snapshot de emergência.", snapshotError); }
        }
        if (previous != null && previous != this) previous.uncaughtException(thread, error);
    }

    private void writeReport(Thread thread, Throwable error) throws Exception {/*BR_JSON_WRAP*/try{
        JSONObject report = new JSONObject();
        report.put("format", "braseiro-crash-report");
        report.put("app_version", BuildConfig.VERSION_NAME);
        report.put("created_at", System.currentTimeMillis());
        report.put("thread", thread == null ? "desconhecida" : thread.getName());
        report.put("device", android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        report.put("android_api", android.os.Build.VERSION.SDK_INT);
        report.put("error_type", error == null ? "desconhecido" : error.getClass().getName());
        report.put("message", error == null || error.getMessage() == null ? "" : error.getMessage());
        report.put("stacktrace", stack(error));
        report.put("runtime_log_tail", AppLog.tail(32000));
        if (db != null) {
            try { report.put("database", db.databaseIntegrity()); }
            catch(Exception ignored){AppLog.ignored("CrashRecoveryHandler","Falha tolerada; a execução continuou.",ignored);}
            try { report.put("world_profile", db.getActiveMapProfile()); }
            catch(Exception ignored){AppLog.ignored("CrashRecoveryHandler","Falha tolerada; a execução continuou.",ignored);}
        }
        File dir = new File(context.getFilesDir(), "crash_reports");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Não foi possível criar a pasta de relatórios.");
        File target = new File(dir, "crash_latest.json");
        AtomicFile atomic = new AtomicFile(target);
        FileOutputStream out = null;
        try {
            out = atomic.startWrite();
            out.write(report.toString(2).getBytes("UTF-8"));
            out.getFD().sync();
            atomic.finishWrite(out);
        } catch (Exception e) {
            if (out != null) atomic.failWrite(out);
            throw e;
        }
        AppLog.e("CrashRecovery", "Falha não tratada registrada em " + new Date() + ".", error);
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static String stack(Throwable error) {
        if (error == null) return "";
        StringWriter buffer = new StringWriter();
        PrintWriter writer = new PrintWriter(buffer);
        error.printStackTrace(writer);
        writer.flush();
        return buffer.toString();
    }
}
