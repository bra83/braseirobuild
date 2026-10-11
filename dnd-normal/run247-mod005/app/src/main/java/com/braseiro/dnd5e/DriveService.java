package com.braseiro.dnd5e;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * Cliente REST legado do Google Drive. O fluxo principal do app usa SAF; esta
 * classe existe apenas para instalações pessoais que já tenham OAuth válido.
 */
public final class DriveService {
    public interface FilesCallback { void onSuccess(List<JSONObject> files); void onError(String message); }
    private static final String MIME_FOLDER = "application/vnd.google-apps.folder";
    private static final String MIME_SHORTCUT = "application/vnd.google-apps.shortcut";
    private static final int MAX_ATTEMPTS = 6;
    private static final int MAX_FOLDER_COUNT = 10000;
    private static final int MAX_FILE_COUNT = 100000;
    private final SharedPreferences prefs;
    private final SecureSecretStore secrets;

    public DriveService(Context context) {
        Context app = context.getApplicationContext();
        prefs = app.getSharedPreferences("cloud", Context.MODE_PRIVATE);
        secrets = new SecureSecretStore(app, prefs);
        secrets.migratePlaintext("drive_access_token");
    }

    public void saveAccessToken(String token) { saveAccessToken(token, System.currentTimeMillis() + 50L * 60L * 1000L); }

    public void saveAccessToken(String token, long expiresAt) {/*BR_JSON_WRAP*/try{
        String safe = token == null ? "" : token.trim();
        if (safe.length() == 0) { disconnect(); return; }
        if (!secrets.put("drive_access_token", safe)) {
            AppLog.e("DriveService", "O token OAuth não pôde ser protegido pelo Android Keystore.", null);
            disconnect();
            return;
        }
        long safeExpiry = Math.max(System.currentTimeMillis() + 60_000L, expiresAt);
        prefs.edit().putLong("drive_expiry", safeExpiry).commit();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public boolean isConnected() {
        return secrets.has("drive_access_token") && prefs.getLong("drive_expiry", 0L) > System.currentTimeMillis() + 60_000L;
    }

    public long tokenExpiresAt() { return prefs.getLong("drive_expiry", 0L); }

    public void disconnect() {/*BR_JSON_WRAP*/try{
        secrets.put("drive_access_token", "");
        prefs.edit().remove("drive_expiry").commit();
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private String validAccessToken() throws Exception {
        String token = secrets.get("drive_access_token");
        long expiry = prefs.getLong("drive_expiry", 0L);
        if (token.length() > 20 && expiry > System.currentTimeMillis() + 60_000L) return token;
        if (token.length() > 0 && expiry <= System.currentTimeMillis() + 60_000L) disconnect();
        throw new Exception("A autorização avançada do Google Drive expirou. Use novamente o login API ou prefira a pasta vinculada pelo Android.");
    }

    public void listFolder(final String folderId, final FilesCallback callback) {
        runAsync(new CheckedSupplier<List<JSONObject>>() { public List<JSONObject> get() throws Exception { return listFolderBlocking(folderId); } }, callback);
    }

    public void listFolderRecursive(final String folderId, final FilesCallback callback) {
        runAsync(new CheckedSupplier<List<JSONObject>>() { public List<JSONObject> get() throws Exception { return listFolderRecursiveBlocking(folderId); } }, callback);
    }

    private void runAsync(final CheckedSupplier<List<JSONObject>> supplier, final FilesCallback callback) {
        AppExecutors.network().execute(new Runnable() { public void run() {
            try { if (callback != null) callback.onSuccess(supplier.get()); }
            catch (Exception e) { if (callback != null) callback.onError(cleanError(e)); }
        }});
    }

    public List<JSONObject> listFolderBlocking(String folderId) throws Exception {
        String token = validAccessToken();
        String clean = extractFolderId(folderId);
        if (clean.length() < 10) throw new Exception("Pasta do Google Drive inválida.");
        return listChildren(clean, token);
    }

    public List<JSONObject> listFolderRecursiveBlocking(String folderId) throws Exception {/*BR_JSON_WRAP*/try{
        String token = validAccessToken();
        String root = extractFolderId(folderId);
        if (root.length() < 10) throw new Exception("Pasta do Google Drive inválida.");
        ArrayList<JSONObject> all = new ArrayList<JSONObject>();
        ArrayDeque<FolderJob> queue = new ArrayDeque<FolderJob>();
        queue.add(new FolderJob(root, ""));
        HashSet<String> visitedFolders = new HashSet<String>();
        HashSet<String> seenFiles = new HashSet<String>();
        while (!queue.isEmpty()) {
            if (Thread.currentThread().isInterrupted()) throw new Exception("Sincronização cancelada.");
            FolderJob job = queue.removeFirst();
            if (!visitedFolders.add(job.id)) continue;
            if (visitedFolders.size() > MAX_FOLDER_COUNT) throw new Exception("A pasta possui subpastas demais para a API avançada.");
            List<JSONObject> children = listChildren(job.id, token);
            for (JSONObject original : children) {
                JSONObject file = new JSONObject(original.toString());
                String id = file.optString("id"), name = file.optString("name"), mime = file.optString("mimeType");
                String effectiveId = id, effectiveMime = mime;
                JSONObject shortcut = file.optJSONObject("shortcutDetails");
                if (MIME_SHORTCUT.equals(mime) && shortcut != null) {
                    effectiveId = shortcut.optString("targetId", id);
                    effectiveMime = shortcut.optString("targetMimeType", mime);
                    file.put("shortcutId", id).put("id", effectiveId).put("mimeType", effectiveMime);
                }
                String path = job.path.length() == 0 ? name : job.path + "/" + name;
                file.put("path", path).put("parentFolderId", job.id);
                if (MIME_FOLDER.equals(effectiveMime)) {
                    if (effectiveId.length() > 0) queue.addLast(new FolderJob(effectiveId, path));
                    continue;
                }
                if (effectiveId.length() > 0 && seenFiles.add(effectiveId)) all.add(file);
                if (seenFiles.size() > MAX_FILE_COUNT) throw new Exception("A pasta possui arquivos demais para uma sincronização única.");
            }
        }
        return all;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONObject getFileMetadata(String fileId) throws Exception {
        String token = validAccessToken();
        String fields = "id,name,mimeType,size,modifiedTime,md5Checksum,trashed,webViewLink";
        return getJson("https://www.googleapis.com/drive/v3/files/" + enc(fileId)
                + "?supportsAllDrives=true&fields=" + enc(fields), token);
    }

    private List<JSONObject> listChildren(String folderId, String token) throws Exception {
        ArrayList<JSONObject> all = new ArrayList<JSONObject>();
        String page = "";
        int pageCount = 0;
        do {
            if (++pageCount > 10000) throw new Exception("A paginação da pasta excedeu o limite seguro.");
            String query = "'" + folderId.replace("'", "\\'") + "' in parents and trashed=false";
            String fields = "nextPageToken,files(id,name,mimeType,size,modifiedTime,md5Checksum,webViewLink,thumbnailLink,shortcutDetails(targetId,targetMimeType))";
            String endpoint = "https://www.googleapis.com/drive/v3/files?q=" + enc(query)
                    + "&pageSize=1000&orderBy=name&spaces=drive&supportsAllDrives=true&includeItemsFromAllDrives=true&fields=" + enc(fields);
            if (page.length() > 0) endpoint += "&pageToken=" + enc(page);
            JSONObject json = getJson(endpoint, token);
            JSONArray files = json.optJSONArray("files");
            if (files != null) for (int i = 0; i < files.length(); i++) all.add(files.getJSONObject(i));
            page = json.optString("nextPageToken", "");
        } while (page.length() > 0);
        return all;
    }

    public static final class DownloadStream {
        public final InputStream stream;
        public final boolean resumed;
        public final long totalBytes;
        public final String contentType;
        DownloadStream(InputStream stream, boolean resumed, long totalBytes, String contentType) {
            this.stream = stream; this.resumed = resumed; this.totalBytes = totalBytes; this.contentType = contentType == null ? "" : contentType;
        }
    }

    public InputStream downloadFile(String fileId) throws Exception { return openDownloadFile(fileId, 0L).stream; }

    public DownloadStream openDownloadFile(String fileId, long offset) throws Exception {
        final String token = validAccessToken();
        final String cleanId = extractFolderId(fileId);
        if (cleanId.length() < 8) throw new Exception("Arquivo do Google Drive inválido.");
        final long safeOffset = Math.max(0L, offset);
        Exception last = null;
        String[] hosts = {"www.googleapis.com", "content.googleapis.com"};
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpURLConnection connection = null;
            try {
                String host = hosts[(attempt - 1) % hosts.length];
                connection = (HttpURLConnection) new URL("https://" + host + "/drive/v3/files/" + enc(cleanId) + "?alt=media&supportsAllDrives=true").openConnection();
                connection.setRequestProperty("Authorization", "Bearer " + token);
                connection.setRequestProperty("Accept", "application/pdf,image/*,audio/*,*/*");
                if (safeOffset > 0L) connection.setRequestProperty("Range", "bytes=" + safeOffset + "-");
                configure(connection, 30_000, 180_000);
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw httpError(connection, code);
                final HttpURLConnection owned = connection;
                final boolean resumed = safeOffset > 0L && code == HttpURLConnection.HTTP_PARTIAL;
                long content = connection.getContentLengthLong();
                long total = content < 0L ? 0L : (resumed ? safeOffset + content : content);
                String type = connection.getContentType();
                InputStream wrapped = new FilterInputStream(connection.getInputStream()) {
                    @Override public void close() throws IOException { try { super.close(); } finally { owned.disconnect(); } }
                };
                return new DownloadStream(wrapped, resumed, total, type);
            } catch (Exception e) {
                last = e;
                if (connection != null) connection.disconnect();
                if (!shouldRetry(e, attempt)) throw normalizeAuthError(e);
                sleepBackoff(e, attempt);
            }
        }
        throw normalizeAuthError(last == null ? new Exception("Não foi possível baixar o arquivo do Google Drive.") : last);
    }

    private JSONObject getJson(String endpoint, String bearer) throws Exception {
        Exception last = null;
        String[] hosts = {"www.googleapis.com", "content.googleapis.com"};
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            HttpURLConnection connection = null;
            try {
                String candidate = endpoint.replace("www.googleapis.com", hosts[(attempt - 1) % hosts.length]);
                connection = (HttpURLConnection) new URL(candidate).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", "Bearer " + bearer);
                connection.setRequestProperty("Accept", "application/json");
                configure(connection, 20_000, 60_000);
                int code = connection.getResponseCode();
                if (code < 200 || code >= 300) throw httpError(connection, code);
                return parseBody(connection.getInputStream());
            } catch (Exception e) {
                last = e;
                if (!shouldRetry(e, attempt)) throw normalizeAuthError(e);
                sleepBackoff(e, attempt);
            } finally {
                if (connection != null) connection.disconnect();
            }
        }
        throw normalizeAuthError(last == null ? new Exception("Não foi possível consultar o Google Drive.") : last);
    }

    private void configure(HttpURLConnection connection, int connectTimeout, int readTimeout) {
        connection.setRequestProperty("Connection", "close");
        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);
        connection.setUseCaches(false);
    }

    private DriveHttpException httpError(HttpURLConnection connection, int code) throws Exception {
        String body = readLimited(connection.getErrorStream(), 8192);
        String detail = body;
        try { detail = extractError(new JSONObject(body), body); } catch(Exception ignored){AppLog.ignored("DriveService","Falha tolerada; a execução continuou.",ignored);}
        long retryAfter = parseRetryAfter(connection.getHeaderField("Retry-After"));
        return new DriveHttpException(code, trim(detail, 900), retryAfter);
    }

    private Exception normalizeAuthError(Exception e) {
        if (e instanceof DriveHttpException) {
            DriveHttpException http = (DriveHttpException) e;
            if (http.code == 401) {
                disconnect();
                return new Exception("A autorização avançada do Google Drive expirou ou foi revogada. Faça login novamente ou use o seletor de pasta do Android.");
            }
            if (http.code == 403) return new Exception("O Google Drive recusou o acesso. Verifique a conta, o compartilhamento e o escopo OAuth.");
            if (http.code == 404) return new Exception("O arquivo ou pasta não existe mais no Google Drive, ou a conta não tem acesso.");
            if (http.code == 429) return new Exception("O Google Drive limitou temporariamente as solicitações. Aguarde e tente novamente.");
            if (http.code >= 500) return new Exception("O Google Drive está temporariamente indisponível (HTTP " + http.code + ").");
        }
        return e;
    }

    private boolean shouldRetry(Exception e, int attempt) {
        if (attempt >= MAX_ATTEMPTS) return false;
        if (e instanceof DriveHttpException) {
            int code = ((DriveHttpException) e).code;
            return code == 429 || code == 500 || code == 502 || code == 503 || code == 504;
        }
        return isTransientNetwork(e);
    }

    private void sleepBackoff(Exception error, int attempt) throws Exception {
        long server = error instanceof DriveHttpException ? ((DriveHttpException) error).retryAfterMillis : 0L;
        long calculated = Math.min(30_000L, 900L * attempt * attempt + (attempt * 137L));
        long wait = Math.max(server, calculated);
        try { Thread.sleep(wait); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new Exception("Operação do Drive cancelada.", interrupted); }
    }

    public static String extractFolderId(String input) {
        if (input == null) return "";
        String value = input.trim();
        int marker = value.indexOf("/folders/");
        if (marker >= 0) value = value.substring(marker + 9);
        else {
            marker = value.indexOf("/d/");
            if (marker >= 0) value = value.substring(marker + 3);
        }
        int end = value.length();
        for (char delimiter : new char[]{'/', '?', '#', '&'}) {
            int position = value.indexOf(delimiter);
            if (position >= 0) end = Math.min(end, position);
        }
        value = value.substring(0, end).trim();
        return value.matches("[A-Za-z0-9_-]{8,}") ? value : "";
    }

    private static JSONObject parseBody(InputStream input) throws Exception {
        String text = readLimited(input, 16 * 1024 * 1024);
        if (text.trim().length() == 0) return new JSONObject();
        return new JSONObject(text);
    }

    private static String extractError(JSONObject json, String fallback) {
        JSONObject error = json.optJSONObject("error");
        if (error != null) return error.optString("message", fallback);
        return json.optString("error_description", json.optString("error", fallback));
    }

    private static String readLimited(InputStream input, int maxChars) throws Exception {
        if (input == null) return "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        StringBuilder out = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        try {
            while ((read = reader.read(buffer)) > 0) {
                int allowed = Math.min(read, maxChars - out.length());
                if (allowed > 0) out.append(buffer, 0, allowed);
                if (out.length() >= maxChars) break;
            }
        } finally { reader.close(); }
        return out.toString();
    }

    private static long parseRetryAfter(String value) {
        if (value == null) return 0L;
        try { return Math.max(0L, Long.parseLong(value.trim()) * 1000L); }
        catch (Exception ignored) { return 0L; }
    }

    private static String enc(String value) throws Exception { return URLEncoder.encode(value == null ? "" : value, "UTF-8"); }
    private static String cleanError(Exception error) { return error == null ? "Falha desconhecida" : error.getMessage() == null ? error.toString() : error.getMessage(); }
    private static String trim(String value, int max) { String v = value == null ? "" : value.trim(); return v.length() <= max ? v : v.substring(0, max) + "…"; }

    private static boolean isTransientNetwork(Exception error) {
        String message = error == null || error.getMessage() == null ? "" : error.getMessage().toLowerCase(Locale.US);
        return message.contains("unable to resolve host") || message.contains("no address associated")
                || message.contains("unknownhost") || message.contains("network is unreachable")
                || message.contains("failed to connect") || message.contains("connection reset")
                || message.contains("connection aborted") || message.contains("timeout")
                || message.contains("timed out") || message.contains("temporarily unavailable")
                || message.contains("unexpected end of stream") || message.contains("connection closed");
    }

    private interface CheckedSupplier<T> { T get() throws Exception; }
    private static final class FolderJob { final String id, path; FolderJob(String id, String path) { this.id = id; this.path = path; } }
    private static final class DriveHttpException extends Exception {
        final int code; final long retryAfterMillis;
        DriveHttpException(int code, String detail, long retryAfterMillis) { super("Drive HTTP " + code + ": " + detail); this.code = code; this.retryAfterMillis = retryAfterMillis; }
    }
}
