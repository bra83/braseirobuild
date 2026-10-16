package com.braseiro.starwarsedge;

import android.app.Activity;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Native JSON export for character saves and campaign backups. */
public final class FileBridge {
    private static final int MAX_JSON_BYTES = 16 * 1024 * 1024;
    private final Activity activity;

    public FileBridge(Activity activity) { this.activity = activity; }

    @JavascriptInterface
    public String saveJson(String rawName, String rawJson) {
        try {
            String json = rawJson == null ? "" : rawJson.trim();
            if (json.isEmpty()) return fail("INVALID_INPUT", "Arquivo JSON vazio.");
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_JSON_BYTES) return fail("FILE_TOO_LARGE", "O arquivo JSON excede 16 MB.");
            new JSONObject(json); // fail closed: exports from this bridge are JSON objects

            String name = safeName(rawName);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, name);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Braseiro Star Wars");
                values.put(MediaStore.Downloads.IS_PENDING, 1);
                Uri uri = activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri == null) return fail("SAVE_FAILED", "Não foi possível criar o arquivo em Downloads.");
                try {
                    try (OutputStream out = activity.getContentResolver().openOutputStream(uri, "w")) {
                        if (out == null) throw new IllegalStateException("Saída de arquivo indisponível.");
                        out.write(bytes);
                    }
                    ContentValues done = new ContentValues();
                    done.put(MediaStore.Downloads.IS_PENDING, 0);
                    int published = activity.getContentResolver().update(uri, done, null, null);
                    if (published != 1) throw new IllegalStateException("Não foi possível publicar o arquivo em Downloads.");
                    return new JSONObject().put("ok", true).put("fileName", name).put("uri", uri.toString()).put("location", "Downloads/Braseiro Star Wars").toString();
                } catch (Exception e) {
                    try { activity.getContentResolver().delete(uri, null, null); } catch (Exception ignored) { }
                    throw e;
                }
            }

            File root = activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (root == null) root = activity.getFilesDir();
            File dir = new File(root, "Braseiro Star Wars");
            if (!dir.exists() && !dir.mkdirs()) return fail("SAVE_FAILED", "Não foi possível criar a pasta de documentos.");
            File file = uniqueFile(dir, name);
            try (FileOutputStream out = new FileOutputStream(file)) { out.write(bytes); }
            return new JSONObject().put("ok", true).put("fileName", file.getName()).put("path", file.getAbsolutePath()).put("location", "Documentos do aplicativo").toString();
        } catch (Exception e) {
            return fail("SAVE_FAILED", "Falha ao salvar o arquivo JSON.");
        }
    }

    private static String safeName(String raw) {
        String name = String.valueOf(raw == null ? "backup.json" : raw).trim().replaceAll("[^A-Za-z0-9._-]+", "_");
        if (name.isEmpty()) name = "backup.json";
        if (!name.toLowerCase().endsWith(".json")) name += ".json";
        if (name.length() > 120) name = name.substring(0, 115) + ".json";
        return name;
    }

    private static File uniqueFile(File dir, String name) {
        File candidate = new File(dir, name);
        if (!candidate.exists()) return candidate;
        String stem = name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
        for (int i = 2; i < 10000; i++) {
            candidate = new File(dir, stem + "_" + i + ".json");
            if (!candidate.exists()) return candidate;
        }
        return new File(dir, stem + "_novo.json");
    }

    private static String fail(String error, String message) {
        try { return new JSONObject().put("ok", false).put("error", error).put("message", message).toString(); }
        catch (Exception e) { return "{\"ok\":false,\"error\":\"SAVE_FAILED\"}"; }
    }
}
