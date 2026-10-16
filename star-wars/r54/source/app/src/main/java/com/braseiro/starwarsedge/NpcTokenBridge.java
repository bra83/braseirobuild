package com.braseiro.starwarsedge;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class NpcTokenBridge {
    static final int PICK_NPC_TOKEN_REQUEST = 4192;
    private static final String TOKEN_HOST = "braseiro-npc-token.local";
    private static final String TOKEN_PATH = "/current";
    private static final long MAX_IMAGE_BYTES = 25L * 1024L * 1024L;

    private final Activity activity;
    private final WebView webView;
    private final File tokenRoot;
    private final ExecutorService tokenExecutor = Executors.newSingleThreadExecutor();
    private volatile String pendingImportNpcId;

    NpcTokenBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.tokenRoot = new File(activity.getFilesDir(), "npc_tokens");
        if (!tokenRoot.exists()) tokenRoot.mkdirs();
    }

    Object javascriptInterface() {
        return new Interface();
    }

    boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_NPC_TOKEN_REQUEST) return false;
        String npcId = pendingImportNpcId;
        pendingImportNpcId = null;
        if (npcId == null) return true;
        if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) {
            notifyEvent("token_import_cancelled", jsonObject("npcId", npcId));
            return true;
        }
        Uri uri = data.getData();
        tokenExecutor.submit(() -> {
            try { importTokenImage(uri, npcId); }
            catch (Exception e) { notifyEvent("token_error", jsonObject("npcId", npcId, "message", safeMessage(e))); }
        });
        return true;
    }

    boolean importSharedTokenImage(Uri uri, String npcId) {
        String cleanNpcId = normalizeNpcId(npcId);
        if (uri == null || cleanNpcId == null) return false;
        pendingImportNpcId = null;
        tokenExecutor.submit(() -> {
            try { importTokenImage(uri, cleanNpcId); }
            catch (Exception e) { notifyEvent("token_error", jsonObject("npcId", cleanNpcId, "message", safeMessage(e))); }
        });
        return true;
    }

    void shutdown() { tokenExecutor.shutdownNow(); }

    WebResourceResponse openTokenResponse(Uri uri) {
        try {
            if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return null;
            if (!TOKEN_HOST.equalsIgnoreCase(uri.getHost())) return null;
            if (!TOKEN_PATH.equals(uri.getPath())) return null;
            String npcId = normalizeNpcId(uri.getQueryParameter("npcId"));
            if (npcId == null) return null;
            File dir = tokenDir(npcId);
            File metaFile = new File(dir, "meta.json");
            File imageFile = new File(dir, "current.bin");
            if (!imageFile.exists() || !metaFile.exists()) {
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", null, new ByteArrayInputStream(new byte[0]));
            }
            JSONObject meta = readJson(metaFile);
            String mime = meta == null ? "image/png" : meta.optString("mimeType", "image/png");
            return new WebResourceResponse(mime, null, new FileInputStream(imageFile));
        } catch (Exception e) {
            return new WebResourceResponse("text/plain", "UTF-8", 500, "Error", null, new ByteArrayInputStream(new byte[0]));
        }
    }

    private final class Interface {
        @JavascriptInterface
        public String getTokenStatus(String npcId) {
            try {
                String cleanNpcId = normalizeNpcId(npcId);
                if (cleanNpcId == null) return fail("Identificador do NPC inválido.");
                JSONObject out = new JSONObject().put("ok", true).put("npcId", cleanNpcId);
                File dir = tokenDir(cleanNpcId);
                File imageFile = new File(dir, "current.bin");
                File metaFile = new File(dir, "meta.json");
                boolean hasToken = imageFile.exists() && metaFile.exists();
                out.put("hasToken", hasToken);
                out.put("awaitingImport", cleanNpcId.equals(pendingImportNpcId));
                if (hasToken) {
                    JSONObject meta = readJson(metaFile);
                    out.put("url", tokenUrl(cleanNpcId, imageFile.lastModified()));
                    if (meta != null) {
                        out.put("updatedAt", meta.optString("updatedAt", ""));
                        out.put("mimeType", meta.optString("mimeType", "image/png"));
                    }
                }
                return out.toString();
            } catch (Exception e) {
                return fail("Não foi possível ler o token do NPC.");
            }
        }

        @JavascriptInterface
        public String openGeminiTokenPrompt(String npcId, String prompt) {
            String cleanNpcId = normalizeNpcId(npcId);
            String cleanPrompt = String.valueOf(prompt == null ? "" : prompt).trim();
            if (cleanNpcId == null) return fail("Identificador do NPC inválido.");
            if (cleanPrompt.length() < 20 || cleanPrompt.length() > 12000) return fail("Prompt do NPC inválido.");
            try {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("Prompt do NPC — Braseiro VTT", cleanPrompt));
                }
                Intent geminiApp = new Intent(Intent.ACTION_SEND);
                geminiApp.setType("text/plain");
                geminiApp.putExtra(Intent.EXTRA_TEXT, cleanPrompt);
                geminiApp.putExtra(Intent.EXTRA_TITLE, "Retrato do NPC — Braseiro VTT");
                geminiApp.setPackage("com.google.android.apps.bard");
                final boolean hasGeminiApp = geminiApp.resolveActivity(activity.getPackageManager()) != null;
                final Intent launchIntent = hasGeminiApp ? geminiApp : new Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com/app"));
                pendingImportNpcId = cleanNpcId;
                ExternalImageShareState.setNpc(activity, cleanNpcId);
                activity.runOnUiThread(() -> activity.startActivity(launchIntent));
                notifyEvent("token_prompt_opened", jsonObject("npcId", cleanNpcId, "target", hasGeminiApp ? "gemini_app" : "gemini_web"));
                return new JSONObject().put("ok", true).put("npcId", cleanNpcId).put("target", hasGeminiApp ? "gemini_app" : "gemini_web").put("clipboard", true).toString();
            } catch (Exception e) {
                pendingImportNpcId = null;
                ExternalImageShareState.clear(activity);
                return fail("Não foi possível abrir o Gemini.");
            }
        }

        @JavascriptInterface
        public String pickTokenImage(String npcId) {
            String cleanNpcId = normalizeNpcId(npcId);
            if (cleanNpcId == null) return fail("Identificador do NPC inválido.");
            try {
                pendingImportNpcId = cleanNpcId;
                ExternalImageShareState.clear(activity);
                activity.runOnUiThread(() -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("image/*");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                        activity.startActivityForResult(intent, PICK_NPC_TOKEN_REQUEST);
                    } catch (Exception e) {
                        pendingImportNpcId = null;
                        notifyEvent("token_error", jsonObject("npcId", cleanNpcId, "message", "Não foi possível abrir o seletor de imagem."));
                    }
                });
                return new JSONObject().put("ok", true).put("status", "picker_opened").put("npcId", cleanNpcId).toString();
            } catch (Exception e) {
                pendingImportNpcId = null;
                return fail("Não foi possível abrir o seletor de imagem.");
            }
        }
    }

    private void importTokenImage(Uri uri, String npcId) throws Exception {
        String cleanNpcId = normalizeNpcId(npcId);
        if (cleanNpcId == null) throw new IllegalArgumentException("NPC inválido.");
        byte[] bytes;
        try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
            bytes = readLimited(in, MAX_IMAGE_BYTES);
        }
        String mime = detectImageMime(bytes);
        if (mime == null) throw new IllegalStateException("Formato de imagem não suportado.");
        File dir = tokenDir(cleanNpcId);
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Não foi possível criar a pasta do NPC.");
        File imageFile = new File(dir, "current.bin");
        File metaFile = new File(dir, "meta.json");
        File tmpFile = new File(dir, "current.tmp");
        writeBytes(tmpFile, bytes);
        if (imageFile.exists() && !imageFile.delete()) throw new IllegalStateException("Não foi possível substituir o token anterior.");
        if (!tmpFile.renameTo(imageFile)) throw new IllegalStateException("Não foi possível salvar o token.");
        JSONObject meta = new JSONObject()
                .put("npcId", cleanNpcId)
                .put("mimeType", mime)
                .put("updatedAt", Instant.now().toString());
        writeBytes(metaFile, meta.toString().getBytes(StandardCharsets.UTF_8));
        notifyEvent("token_imported", jsonObject("npcId", cleanNpcId, "mimeType", mime, "url", tokenUrl(cleanNpcId, imageFile.lastModified())));
    }

    private String tokenUrl(String npcId, long stamp) {
        return "https://" + TOKEN_HOST + TOKEN_PATH + "?npcId=" + Uri.encode(npcId) + "&v=" + stamp;
    }

    private File tokenDir(String npcId) {
        return new File(tokenRoot, normalizeNpcId(npcId));
    }

    private String normalizeNpcId(String npcId) {
        String clean = String.valueOf(npcId == null ? "" : npcId).trim();
        return clean.matches("[A-Za-z0-9._:-]{1,120}") ? clean : null;
    }

    private String detectImageMime(byte[] bytes) {
        if (bytes == null || bytes.length < 12) return null;
        if ((bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47) return "image/png";
        if ((bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F' && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
        return null;
    }

    private void notifyEvent(String type, JSONObject payload) {
        try {
            String raw = new JSONObject().put("type", type).put("payload", payload == null ? new JSONObject() : payload).toString();
            String js = "window.braseiroNpcTokenNativeEvent&&window.braseiroNpcTokenNativeEvent(" + JSONObject.quote(raw) + ");";
            activity.runOnUiThread(() -> webView.evaluateJavascript(js, null));
        } catch (Exception ignored) {}
    }

    private JSONObject readJson(File file) {
        if (file == null || !file.exists()) return null;
        try {
            return new JSONObject(new String(readLimited(new FileInputStream(file), 64 * 1024), StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] readLimited(InputStream input, long maxBytes) throws Exception {
        if (input == null) return new byte[0];
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[64 * 1024];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxBytes) throw new IllegalArgumentException("Arquivo de imagem excede o limite local permitido.");
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private void writeBytes(File file, byte[] bytes) throws Exception {
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(file, false)) {
            output.write(bytes);
            output.flush();
        }
    }

    private String safeMessage(Exception error) {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return "Falha ao manipular o token do NPC.";
        message = message.trim();
        return message.length() <= 240 ? message : message.substring(0, 239) + "…";
    }

    private JSONObject jsonObject(Object... keyValues) {
        JSONObject object = new JSONObject();
        if (keyValues == null) return object;
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            try {
                object.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
            } catch (Exception ignored) {}
        }
        return object;
    }

    private String fail(String message) {
        try {
            return new JSONObject().put("ok", false).put("message", message).toString();
        } catch (Exception ignored) {
            return "{\"ok\":false,\"message\":\"Falha no host de token do NPC.\"}";
        }
    }
}
