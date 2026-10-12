package com.braseiro.pathfinder2e;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Registro leve e persistente dos acontecimentos do áudio.
 *
 * Cada linha é um JSON independente. Falhas de diagnóstico nunca interrompem
 * voz, SFX, ambiência ou a interface.
 */
public final class AudioDiagnosticsRepository {
    private static final long MAX_BYTES = 2L * 1024L * 1024L;
    private final File file;
    private final File previous;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();

    public AudioDiagnosticsRepository(Context context) {
        File dir = new File(context.getFilesDir(), "diagnostics");
        if (!dir.exists()) dir.mkdirs();
        file = new File(dir, "audio_events_v3.jsonl");
        previous = new File(dir, "audio_events_v3.previous.jsonl");
    }

    public String newSessionId(String sceneId) {
        String scene = safe(sceneId).replaceAll("[^A-Za-z0-9_-]+", "_");
        if (scene.length() > 32) scene = scene.substring(0, 32);
        return "audio_" + System.currentTimeMillis() + "_" + scene + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void event(final String sessionId, final String type, JSONObject details) {
        final String serialized;
        try { serialized = details == null ? "{}" : details.toString(); }
        catch (Throwable ignored) { return; }
        try {
            writer.execute(new Runnable() {
                @Override public void run() { writeEvent(sessionId, type, serialized); }
            });
        } catch (RejectedExecutionException ignored) {
            // O aplicativo está encerrando; diagnóstico nunca bloqueia áudio.
        }
    }

    private synchronized void writeEvent(String sessionId, String type, String serializedDetails) {
        try {
            rotateIfNeeded();
            JSONObject line = new JSONObject();
            line.put("at", System.currentTimeMillis());
            line.put("session", safe(sessionId));
            line.put("event", safe(type));
            try { line.put("details", new JSONObject(serializedDetails)); }
            catch (Throwable ignored) { line.put("details", new JSONObject()); }
            byte[] bytes = (line.toString() + "\n").getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream out = new FileOutputStream(file, true)) {
                out.write(bytes);
                out.getFD().sync();
            }
        } catch (Throwable ignored) {
            // Diagnóstico nunca é causa de falha operacional.
        }
    }

    public void event(String sessionId, String type, String key, Object value) {
        JSONObject details = new JSONObject();
        try { details.put(key, value == null ? JSONObject.NULL : value); } catch(Exception ignored){AppLog.ignored("AudioDiagnosticsRepository","Falha tolerada; a execução continuou.",ignored);}
        event(sessionId, type, details);
    }

    public void latency(String sessionId,String stage,long startedAt){
        JSONObject details=new JSONObject();
        try{details.put("stage",safe(stage)).put("latencyMs",Math.max(0L,System.currentTimeMillis()-startedAt));}catch(Exception ignored){AppLog.ignored("AudioDiagnosticsRepository","Falha tolerada; a execução continuou.",ignored);}
        event(sessionId,"latency",details);
    }

    public synchronized String recentReport(int limit) {
        int max = Math.max(1, Math.min(500, limit));
        ArrayList<String> lines = new ArrayList<String>();
        readInto(previous, lines);
        readInto(file, lines);
        int start = Math.max(0, lines.size() - max);
        StringBuilder out = new StringBuilder();
        for (int i = start; i < lines.size(); i++) {
            if (out.length() > 0) out.append('\n');
            out.append(lines.get(i));
        }
        return out.toString();
    }

    public void clear() {
        try {
            writer.execute(new Runnable() {
                @Override public void run() { clearFiles(); }
            });
        } catch (RejectedExecutionException ignored) {
            // Encerramento em andamento.
        }
    }

    private synchronized void clearFiles() {
        try { if (file.exists()) file.delete(); } catch(Throwable ignored){AppLog.ignored("AudioDiagnosticsRepository","Falha tolerada; a execução continuou.",ignored);}
        try { if (previous.exists()) previous.delete(); } catch(Throwable ignored){AppLog.ignored("AudioDiagnosticsRepository","Falha tolerada; a execução continuou.",ignored);}
    }

    public void shutdown() { writer.shutdown(); }

    private void rotateIfNeeded() {
        if (!file.exists() || file.length() < MAX_BYTES) return;
        try { if (previous.exists()) previous.delete(); } catch(Throwable ignored){AppLog.ignored("AudioDiagnosticsRepository","Falha tolerada; a execução continuou.",ignored);}
        if (!file.renameTo(previous)) {
            try { file.delete(); } catch(Throwable ignored){AppLog.ignored("AudioDiagnosticsRepository","Falha tolerada; a execução continuou.",ignored);}
        }
    }

    private static void readInto(File source, List<String> out) {
        if (source == null || !source.isFile()) return;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(source), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) if (!line.trim().isEmpty()) out.add(line);
        } catch(Throwable ignored){AppLog.ignored("AudioDiagnosticsRepository","Falha tolerada; a execução continuou.",ignored);}
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
