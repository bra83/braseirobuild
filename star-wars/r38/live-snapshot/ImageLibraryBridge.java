package com.braseiro.starwarsedge;

import android.app.Activity;
import android.net.Uri;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

final class ImageLibraryBridge {
    static final String RUNTIME_HOST = "braseiro.local";
    static final String RUNTIME_PREFIX = "/library-image/";
    static final String ALLOWED_HOST = "kainrath.wordpress.com";
    static final String ALLOWED_PATH_PREFIX = "/wp-content/uploads/";
    static final long MAX_REMOTE_BYTES = 25L * 1024L * 1024L;
    private static final int MAX_REDIRECTS = 3;

    private final Activity activity;
    private final WebView webView;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final File cacheRoot;
    private final Object cacheCommitLock = new Object();
    private final ConcurrentHashMap<String, Long> cacheGenerations = new ConcurrentHashMap<>();
    private final AtomicLong cacheGenerationCounter = new AtomicLong();

    ImageLibraryBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
        this.cacheRoot = new File(activity.getFilesDir(), "image-library-cache");
        if (!cacheRoot.exists()) cacheRoot.mkdirs();
    }

    void shutdown() {
        executor.shutdownNow();
    }

    @JavascriptInterface
    public String getStatus(String assetId) {
        try {
            String cleanId = normalizeAssetId(assetId);
            if (cleanId == null) return fail("ASSET_ID_INVALID", "assetId inválido.");
            return statusJson(cleanId).toString();
        } catch (Exception e) {
            return fail("IMAGE_CACHE_STATUS_FAILED", "Não foi possível consultar o cache da imagem.");
        }
    }

    @JavascriptInterface
    public String cacheRemoteImage(String assetId, String remoteUrl) {
        String cleanId = normalizeAssetId(assetId);
        if (cleanId == null) return fail("ASSET_ID_INVALID", "assetId inválido.");
        Uri remote = validateRemoteUri(remoteUrl);
        if (remote == null) return fail("REMOTE_URL_NOT_ALLOWED", "URL externa não permitida para a biblioteca.");
        try {
            JSONObject current = statusJson(cleanId);
            if (current.optBoolean("cached", false) && remote.toString().equals(current.optString("sourceUrl"))) {
                return new JSONObject().put("ok", true).put("status", "already_cached").put("assetId", cleanId).put("runtimeUrl", runtimeUrl(cleanId)).toString();
            }
            long generation;
            synchronized (cacheCommitLock) {
                generation = nextCacheGeneration(cleanId);
            }
            executor.submit(() -> fetchAndCache(cleanId, remote, generation));
            notifyEvent("queued", new JSONObject().put("assetId", cleanId));
            return new JSONObject().put("ok", true).put("status", "queued").put("assetId", cleanId).toString();
        } catch (Exception e) {
            return fail("IMAGE_CACHE_QUEUE_FAILED", "Não foi possível iniciar o download da imagem.");
        }
    }

    @JavascriptInterface
    public String clearCachedAsset(String assetId) {
        try {
            String cleanId = normalizeAssetId(assetId);
            if (cleanId == null) return fail("ASSET_ID_INVALID", "assetId inválido.");
            boolean ok;
            synchronized (cacheCommitLock) {
                nextCacheGeneration(cleanId);
                File image = imageFile(cleanId);
                File meta = metadataFile(cleanId);
                File temp = tempFile(cleanId);
                ok = (!image.exists() || image.delete())
                        && (!meta.exists() || meta.delete())
                        && (!temp.exists() || temp.delete());
            }
            if (!ok) return fail("IMAGE_CACHE_CLEAR_FAILED", "Não foi possível remover a imagem do cache.");
            notifyEvent("cleared", new JSONObject().put("assetId", cleanId));
            return new JSONObject().put("ok", true).put("assetId", cleanId).put("cached", false).toString();
        } catch (Exception e) {
            return fail("IMAGE_CACHE_CLEAR_FAILED", "Não foi possível remover a imagem do cache.");
        }
    }

    WebResourceResponse openCachedImageResponse(Uri uri) {
        try {
            if (uri == null || !"https".equalsIgnoreCase(uri.getScheme()) || !RUNTIME_HOST.equalsIgnoreCase(uri.getHost())) return null;
            String path = uri.getPath();
            if (path == null || !path.startsWith(RUNTIME_PREFIX)) return null;
            String encoded = path.substring(RUNTIME_PREFIX.length());
            String assetId = normalizeAssetId(Uri.decode(encoded));
            if (assetId == null) return response(400, "Bad Request", "text/plain", new byte[0]);
            synchronized (cacheCommitLock) {
                File image = imageFile(assetId);
                File meta = metadataFile(assetId);
                if (!image.exists() || !meta.exists()) return response(404, "Not Found", "text/plain", new byte[0]);
                JSONObject data = readJson(meta);
                String mime = safeMime(data == null ? null : data.optString("mimeType", null));
                if (mime == null) return response(415, "Unsupported Media Type", "text/plain", new byte[0]);
                return new WebResourceResponse(mime, null, new FileInputStream(image));
            }
        } catch (Exception e) {
            return response(500, "Error", "text/plain", new byte[0]);
        }
    }

    private void fetchAndCache(String assetId, Uri initialUri, long generation) {
        File temp = null;
        try {
            RemoteResponse remote = openAllowedConnection(initialUri, 0);
            String mime = safeMime(remote.connection.getContentType());
            if (mime == null) throw new IllegalStateException("MIME de imagem não permitido.");
            long announced = remote.connection.getContentLengthLong();
            if (announced > MAX_REMOTE_BYTES) throw new IllegalStateException("Imagem excede o limite do cache.");
            byte[] bytes;
            try (InputStream in = remote.connection.getInputStream()) {
                bytes = readLimited(in, MAX_REMOTE_BYTES);
            } finally {
                remote.connection.disconnect();
            }
            if (bytes.length == 0) throw new IllegalStateException("Imagem vazia.");
            String digest = sha256(bytes);
            temp = tempFile(assetId);
            try (FileOutputStream out = new FileOutputStream(temp)) { out.write(bytes); }
            JSONObject meta = new JSONObject()
                    .put("assetId", assetId)
                    .put("sourceUrl", remote.finalUri.toString())
                    .put("mimeType", mime)
                    .put("bytes", bytes.length)
                    .put("sha256", digest)
                    .put("cachedAt", Instant.now().toString())
                    .put("runtimeUrl", runtimeUrl(assetId));
            synchronized (cacheCommitLock) {
                if (!isCurrentCacheGeneration(assetId, generation)) {
                    if (temp.exists()) temp.delete();
                    return;
                }
                File target = imageFile(assetId);
                if (target.exists() && !target.delete()) throw new IllegalStateException("Falha ao substituir cache anterior.");
                if (!temp.renameTo(target)) {
                    try (FileOutputStream out = new FileOutputStream(target)) { out.write(bytes); }
                    if (!temp.delete()) temp.deleteOnExit();
                }
                writeJson(metadataFile(assetId), meta);
                notifyEvent("cached", meta);
            }
        } catch (Exception e) {
            if (temp != null && temp.exists()) temp.delete();
            if (isCurrentCacheGeneration(assetId, generation)) {
                notifyEvent("error", jsonObject("assetId", assetId, "message", safeMessage(e)));
            }
        }
    }

    private RemoteResponse openAllowedConnection(Uri uri, int redirects) throws Exception {
        Uri safe = validateRemoteUri(uri == null ? null : uri.toString());
        if (safe == null) throw new IllegalArgumentException("URL externa não permitida.");
        if (redirects > MAX_REDIRECTS) throw new IllegalStateException("Redirecionamentos demais.");
        HttpURLConnection connection = (HttpURLConnection) new URL(safe.toString()).openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(20000);
        connection.setRequestProperty("Accept", "image/png,image/jpeg,image/webp");
        connection.setRequestProperty("User-Agent", "BraseiroStarWarsVTT/0.36");
        int code = connection.getResponseCode();
        if (code >= 300 && code < 400) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || location.isBlank()) throw new IllegalStateException("Redirecionamento sem destino.");
            Uri next = Uri.parse(safe.toString()).buildUpon().encodedPath(safe.getEncodedPath()).build();
            URL resolved = new URL(new URL(safe.toString()), location);
            next = Uri.parse(resolved.toString());
            if (validateRemoteUri(next.toString()) == null) throw new IllegalStateException("Redirecionamento para host não permitido.");
            return openAllowedConnection(next, redirects + 1);
        }
        if (code < 200 || code >= 300) {
            connection.disconnect();
            throw new IllegalStateException("Servidor remoto respondeu HTTP " + code + ".");
        }
        return new RemoteResponse(connection, safe);
    }

    private JSONObject statusJson(String assetId) throws Exception {
        synchronized (cacheCommitLock) {
            File image = imageFile(assetId);
            File meta = metadataFile(assetId);
            if (!image.exists() || !meta.exists()) return new JSONObject().put("ok", true).put("assetId", assetId).put("cached", false);
            JSONObject data = readJson(meta);
            if (data == null || !assetId.equals(data.optString("assetId"))) return new JSONObject().put("ok", true).put("assetId", assetId).put("cached", false);
            return new JSONObject()
                    .put("ok", true)
                    .put("assetId", assetId)
                    .put("cached", true)
                    .put("sourceUrl", data.optString("sourceUrl", ""))
                    .put("mimeType", data.optString("mimeType", ""))
                    .put("bytes", data.optLong("bytes", image.length()))
                    .put("sha256", data.optString("sha256", ""))
                    .put("cachedAt", data.optString("cachedAt", ""))
                    .put("runtimeUrl", runtimeUrl(assetId));
            }
    }

    static Uri validateRemoteUri(String raw) {
        try {
            Uri uri = Uri.parse(String.valueOf(raw == null ? "" : raw).trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) return null;
            if (!ALLOWED_HOST.equalsIgnoreCase(uri.getHost())) return null;
            String path = uri.getPath();
            if (path == null || !path.startsWith(ALLOWED_PATH_PREFIX)) return null;
            if (uri.getUserInfo() != null) return null;
            return uri;
        } catch (Exception e) { return null; }
    }

    static String safeMime(String raw) {
        if (raw == null) return null;
        String mime = raw.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return switch (mime) {
            case "image/png", "image/jpeg", "image/webp" -> mime;
            default -> null;
        };
    }

    private static byte[] readLimited(InputStream in, long limit) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16384];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > limit) throw new IllegalStateException("Imagem excede o limite do cache.");
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }


    private long nextCacheGeneration(String assetId) {
        long generation = cacheGenerationCounter.incrementAndGet();
        cacheGenerations.put(assetId, generation);
        return generation;
    }

    private boolean isCurrentCacheGeneration(String assetId, long generation) {
        Long current = cacheGenerations.get(assetId);
        return current != null && current == generation;
    }

    private File tempFile(String assetId) {
        return new File(cacheRoot, cacheKey(assetId) + ".tmp");
    }

    private File imageFile(String assetId) { return new File(cacheRoot, cacheKey(assetId) + ".bin"); }
    private File metadataFile(String assetId) { return new File(cacheRoot, cacheKey(assetId) + ".json"); }

    private static String cacheKey(String assetId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return hex(md.digest(assetId.getBytes(StandardCharsets.UTF_8))).substring(0, 32);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private static String sha256(byte[] bytes) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) out.append(String.format(Locale.ROOT, "%02x", b & 0xff));
        return out.toString();
    }

    private static String normalizeAssetId(String raw) {
        String value = String.valueOf(raw == null ? "" : raw).trim();
        if (value.isEmpty() || value.length() > 180) return null;
        return value.matches("[A-Za-z0-9._:-]+") ? value : null;
    }

    private static String runtimeUrl(String assetId) {
        return "https://" + RUNTIME_HOST + RUNTIME_PREFIX + Uri.encode(assetId);
    }

    private void notifyEvent(String type, JSONObject payload) {
        try {
            JSONObject envelope = new JSONObject().put("type", type).put("data", payload == null ? new JSONObject() : payload);
            String script = "window.braseiroImageLibraryNativeEvent&&window.braseiroImageLibraryNativeEvent(" + JSONObject.quote(envelope.toString()) + ");";
            activity.runOnUiThread(() -> webView.evaluateJavascript(script, null));
        } catch (Exception ignored) { }
    }

    private static void writeJson(File file, JSONObject json) throws Exception {
        try (FileOutputStream out = new FileOutputStream(file)) { out.write(json.toString().getBytes(StandardCharsets.UTF_8)); }
    }

    private static JSONObject readJson(File file) {
        try (FileInputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
            return new JSONObject(out.toString(StandardCharsets.UTF_8));
        } catch (Exception e) { return null; }
    }

    private static WebResourceResponse response(int status, String reason, String mime, byte[] bytes) {
        return new WebResourceResponse(mime, "UTF-8", status, reason, null, new ByteArrayInputStream(bytes));
    }

    private static String fail(String code, String message) {
        try { return new JSONObject().put("ok", false).put("error", code).put("message", message).toString(); }
        catch (Exception e) { return "{\"ok\":false,\"error\":\"IMAGE_LIBRARY_FAILED\"}"; }
    }


    private static JSONObject jsonObject(Object... keyValues) {
        JSONObject object = new JSONObject();
        if (keyValues == null) return object;
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            try { object.put(String.valueOf(keyValues[i]), keyValues[i + 1]); }
            catch (Exception ignored) {}
        }
        return object;
    }

    private static String safeMessage(Exception e) {
        String message = e == null ? null : e.getMessage();
        return message == null || message.isBlank() ? "Falha ao obter a imagem externa." : message;
    }

    private record RemoteResponse(HttpURLConnection connection, Uri finalUri) { }
}
