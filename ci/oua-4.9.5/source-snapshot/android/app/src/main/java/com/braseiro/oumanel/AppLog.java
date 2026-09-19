package com.braseiro.oumanel;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro local rotativo e sem segredos. Serve ao diagnóstico sem depender
 * de Logcat, que geralmente não está disponível quando o erro ocorre no celular.
 */
public final class AppLog {
    private static final long MAX_BYTES = 1024L * 1024L;
    private static final Pattern GOOGLE_KEY = Pattern.compile("AIza[0-9A-Za-z_-]{20,}");
    private static final Pattern JSON_SECRET = Pattern.compile("(?i)(\\\"?(?:gemini_key|api[_-]?key|x-goog-api-key|access_token|refresh_token)\\\"?\\s*[:=]\\s*\\\"?)([^\\\"\\s,}]+)");
    private static final Pattern BEARER_TOKEN = Pattern.compile("(?i)(authorization\\s*[:=]\\s*bearer\\s+|bearer\\s+)([0-9A-Za-z._~+\\-/=]{12,})");
    private static final Object LOCK = new Object();
    private static final ConcurrentHashMap<String,Long> IGNORED_RATE_LIMIT = new ConcurrentHashMap<String,Long>();
    private static final long IGNORED_INTERVAL_MS = 30_000L;
    private static volatile Context context;
    private static volatile boolean handlerInstalled;

    private AppLog() {}

    public static void init(Context value) {
        if (value == null) return;
        context = value.getApplicationContext();
        installCrashHandler();
    }

    public static void d(String tag, String message) { write("D", tag, message, null); }
    public static void i(String tag, String message) { write("I", tag, message, null); }
    public static void w(String tag, String message) { write("W", tag, message, null); }
    public static void w(String tag, String message, Throwable error) { write("W", tag, message, error); }
    public static void e(String tag, String message, Throwable error) { write("E", tag, message, error); }
    public static void ignored(String tag,String operation,Throwable error) {
        String errorName=error==null?"erro":error.getClass().getSimpleName();String key=(tag==null?"Braseiro":tag)+"|"+errorName+"|"+(operation==null?"":operation);long now=System.currentTimeMillis();Long previous=IGNORED_RATE_LIMIT.put(key,now);if(previous!=null&&now-previous<IGNORED_INTERVAL_MS)return;write("D",tag,operation,error);
    }

    public static long bytes() {
        File file = logFile();
        if (file == null) return 0L;
        File previous = new File(file.getParentFile(), "braseiro_runtime.1.log");
        return (file.isFile() ? file.length() : 0L) + (previous.isFile() ? previous.length() : 0L);
    }

    public static String tail(int maxChars) {
        File file = logFile();
        if (file == null) return "";
        int limit = Math.max(512, Math.min(120000, maxChars));
        try {
            StringBuilder out = new StringBuilder();
            File previous = new File(file.getParentFile(), "braseiro_runtime.1.log");
            appendTail(previous, out, limit);
            appendTail(file, out, limit);
            if (out.length() > limit) out.delete(0, out.length() - limit);
            return redact(out.toString());
        } catch (Exception ignored) {
            return "";
        }
    }

    private static void appendTail(File file, StringBuilder out, int limit) throws Exception {
        if (file == null || !file.isFile()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line).append('\n');
                if (out.length() > limit * 2) out.delete(0, out.length() - limit);
            }
        }
    }

    private static void installCrashHandler() {
        if (handlerInstalled) return;
        synchronized (LOCK) {
            if (handlerInstalled) return;
            final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override public void uncaughtException(Thread thread, Throwable error) {
                    try { e("Crash", "Falha não tratada em " + (thread == null ? "thread" : thread.getName()), error); }
                    catch (Throwable ignored) {}
                    if (previous != null) previous.uncaughtException(thread, error);
                }
            });
            handlerInstalled = true;
        }
    }

    private static void write(String level, String tag, String message, Throwable error) {
        String safeTag = tag == null ? "Braseiro" : tag.replaceAll("[^A-Za-z0-9_.-]", "_");
        String safeMessage = redact(message == null ? "" : message);
        if ("E".equals(level)) Log.e(safeTag, safeMessage, error);
        else if ("W".equals(level)) Log.w(safeTag, safeMessage, error);
        else if ("I".equals(level)) Log.i(safeTag, safeMessage);
        else Log.d(safeTag, safeMessage);
        Context c = context;
        if (c == null) return;
        synchronized (LOCK) {
            try {
                File file = logFile();
                if (file == null) return;
                rotateIfNeeded(file);
                String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
                StringBuilder line = new StringBuilder(stamp).append(" ").append(level).append("/").append(safeTag).append(": ").append(safeMessage);
                if (error != null) line.append('\n').append(redact(Log.getStackTraceString(error)));
                line.append('\n');
                FileOutputStream out = new FileOutputStream(file, true);
                out.write(line.toString().getBytes("UTF-8"));
                out.close();
            } catch (Exception ignored) {}
        }
    }

    private static File logFile() {
        Context c = context;
        if (c == null) return null;
        File dir = new File(c.getFilesDir(), "diagnostics");
        if (!dir.exists() && !dir.mkdirs()) return null;
        return new File(dir, "braseiro_runtime.log");
    }

    private static void rotateIfNeeded(File file) {
        if (file == null || file.length() < MAX_BYTES) return;
        File previous = new File(file.getParentFile(), "braseiro_runtime.1.log");
        if (previous.exists()) previous.delete();
        if (!file.renameTo(previous)) file.delete();
    }

    private static String redact(String value) {
        String text = value == null ? "" : value;
        text = GOOGLE_KEY.matcher(text).replaceAll("AIza…REDIGIDO");
        text = JSON_SECRET.matcher(text).replaceAll("$1***REDIGIDO***");
        text = BEARER_TOKEN.matcher(text).replaceAll("$1***REDIGIDO***");
        return text;
    }
}
