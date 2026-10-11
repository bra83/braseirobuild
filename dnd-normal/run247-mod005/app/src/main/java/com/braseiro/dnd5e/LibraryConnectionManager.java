package com.braseiro.dnd5e;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fonte única de verdade para as pastas externas do aplicativo. Impede que uma
 * URI apenas salva seja exibida como conectada quando a permissão foi revogada.
 */
public final class LibraryConnectionManager {
    public static final String MANUALS = "manuals";
    public static final String TOKEN_PDFS = "token_pdfs";
    public static final String TOKEN_IMAGES = "token_images";
    public static final String AUDIO = "audio";
    public static final String BACKUPS = "backups";

    private final Context context;
    private final SharedPreferences prefs;
    private final ContentResolver resolver;
    private final SafFolderScanner scanner;

    public LibraryConnectionManager(Context context) {
        if (context == null) throw new IllegalArgumentException("Contexto ausente");
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        this.resolver = this.context.getContentResolver();
        this.scanner = new SafFolderScanner(this.context);
    }

    public String prefKey(String source) {
        if (TOKEN_PDFS.equals(source)) return "token_pdf_tree_uri";
        if (TOKEN_IMAGES.equals(source)) return "token_image_tree_uri";
        if (AUDIO.equals(source)) return "audio_tree_uri";
        if (BACKUPS.equals(source)) return "backup_tree_uri";
        return "manual_tree_uri";
    }

    public String label(String source) {
        if (TOKEN_PDFS.equals(source)) return "PDFs de tokens";
        if (TOKEN_IMAGES.equals(source)) return "Imagens de tokens";
        if (AUDIO.equals(source)) return "Biblioteca de áudio";
        if (BACKUPS.equals(source)) return "Backups automáticos";
        return "Manuais";
    }

    public String storedUri(String source) { return prefs.getString(prefKey(source), "").trim(); }

    public boolean hasStoredUri(String source) { return storedUri(source).length() > 0; }

    public boolean hasPersistedReadPermission(String source) {
        return hasPersistedPermission(source, true, false);
    }

    public boolean hasPersistedWritePermission(String source) {
        return hasPersistedPermission(source, false, true);
    }

    private boolean hasPersistedPermission(String source, boolean read, boolean write) {
        String stored = storedUri(source);
        if (stored.length() == 0) return false;
        for (UriPermission permission : resolver.getPersistedUriPermissions()) {
            if (permission == null || permission.getUri() == null || !stored.equals(permission.getUri().toString())) continue;
            if (read && !permission.isReadPermission()) continue;
            if (write && !permission.isWritePermission()) continue;
            return true;
        }
        return false;
    }

    public JSONObject status(String source, boolean deepProbe) {/*BR_JSON_WRAP*/try{
        JSONObject out = new JSONObject();
        String stored = storedUri(source);
        try {
            out.put("source", source);
            out.put("label", label(source));
            out.put("uri", stored);
            out.put("configured", stored.length() > 0);
            out.put("last_sync", prefs.getLong("library_" + source + "_last_sync", 0L));
            out.put("last_success", prefs.getLong("library_" + source + "_last_success", 0L));
            out.put("last_count", prefs.getInt("library_" + source + "_last_count", 0));
            out.put("last_added", prefs.getInt("library_" + source + "_last_added", 0));
            out.put("last_changed", prefs.getInt("library_" + source + "_last_changed", 0));
            out.put("last_missing", prefs.getInt("library_" + source + "_last_missing", 0));
            out.put("last_error", prefs.getString("library_" + source + "_last_error", ""));
            if (stored.length() == 0) {
                out.put("code", "not_configured");
                out.put("healthy", true);
                out.put("detail", "Pasta opcional ainda não vinculada.");
                return out;
            }
            Uri uri = Uri.parse(stored);
            out.put("authority", uri.getAuthority() == null ? "" : uri.getAuthority());
            boolean permission = hasPersistedReadPermission(source);
            boolean writePermission = !BACKUPS.equals(source) || hasPersistedWritePermission(source);
            out.put("persisted_permission", permission);
            out.put("persisted_write_permission", writePermission);
            if (!permission || !writePermission) {
                out.put("code", "permission_revoked");
                out.put("healthy", false);
                out.put("detail", !permission ? "A URI está salva, mas a permissão de leitura foi revogada." : "A pasta de backup perdeu a permissão de gravação.");
                return out;
            }
            if (!deepProbe) {
                out.put("code", "permission_ok");
                out.put("healthy", true);
                out.put("detail", "Permissão persistente confirmada.");
                return out;
            }
            JSONObject probe = scanner.probe(uri);
            out.put("probe", probe);
            boolean readable = probe.optBoolean("readable", false) && probe.optBoolean("sample_opened", false);
            out.put("code", readable ? "connected" : "provider_unavailable");
            out.put("healthy", readable);
            out.put("detail", readable
                    ? "Pasta aberta e arquivo de teste lido pelo provedor."
                    : "A pasta existe, mas o provedor não entregou um arquivo de teste.");
            return out;
        } catch (SecurityException e) {
            putError(out, "permission_revoked", "A permissão da pasta foi revogada.", e);
        } catch (Exception e) {
            String message = safe(e).toLowerCase();
            String code = message.contains("rede") || message.contains("offline") || message.contains("conex")
                    ? "provider_offline" : message.contains("não é uma árvore") || message.contains("inexist")
                    ? "folder_missing" : "provider_error";
            putError(out, code, safe(e), e);
        }
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public JSONArray allStatuses(boolean deepProbe) {/*BR_JSON_WRAP*/try{
        JSONArray out = new JSONArray();
        out.put(status(MANUALS, deepProbe));
        out.put(status(TOKEN_PDFS, deepProbe));
        out.put(status(TOKEN_IMAGES, deepProbe));
        out.put(status(AUDIO, deepProbe));
        out.put(status(BACKUPS, deepProbe));
        return out;
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    public void recordSyncStart(String source) {
        prefs.edit().putLong("library_" + source + "_last_sync", System.currentTimeMillis())
                .putString("library_" + source + "_last_error", "").apply();
    }

    public void recordSyncSuccess(String source, int count, int added, int changed, int missing) {
        long now = System.currentTimeMillis();
        prefs.edit().putLong("library_" + source + "_last_sync", now)
                .putLong("library_" + source + "_last_success", now)
                .putInt("library_" + source + "_last_count", Math.max(0, count))
                .putInt("library_" + source + "_last_added", Math.max(0, added))
                .putInt("library_" + source + "_last_changed", Math.max(0, changed))
                .putInt("library_" + source + "_last_missing", Math.max(0, missing))
                .putInt("library_" + source + "_failures", 0)
                .putString("library_" + source + "_last_error", "").apply();
    }

    public void recordSyncFailure(String source, String error) {
        int failures = prefs.getInt("library_" + source + "_failures", 0) + 1;
        prefs.edit().putLong("library_" + source + "_last_sync", System.currentTimeMillis())
                .putInt("library_" + source + "_failures", failures)
                .putString("library_" + source + "_last_error", trim(error, 900)).apply();
    }

    public void saveTree(String source, Uri treeUri) throws Exception {
        saveTree(source, treeUri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    public void saveTree(String source, Uri treeUri, int grantFlags) throws Exception {
        if (treeUri == null) throw new Exception("Pasta ausente.");
        int persistable = grantFlags & (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if ((persistable & android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) == 0) persistable |= android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
        resolver.takePersistableUriPermission(treeUri, persistable);
        prefs.edit().putString(prefKey(source), treeUri.toString())
                .putString("library_" + source + "_last_error", "").apply();
    }

    public void disconnect(String source) {
        String stored = storedUri(source);
        if (stored.length() > 0) {
            try { resolver.releasePersistableUriPermission(Uri.parse(stored), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION); }
            catch (Exception e) { AppLog.w("LibraryConnection", "A permissão já não estava disponível para " + source + ".", e); }
        }
        prefs.edit().remove(prefKey(source)).remove("library_" + source + "_last_error").apply();
    }

    public Set<String> activeReadGrants() {
        HashSet<String> out = new HashSet<String>();
        List<UriPermission> permissions = resolver.getPersistedUriPermissions();
        for (UriPermission permission : permissions) if (permission != null && permission.isReadPermission()
                && permission.getUri() != null) out.add(permission.getUri().toString());
        return out;
    }

    public String humanStatus(JSONObject status) {
        if (status == null) return "Estado desconhecido";
        String code = status.optString("code");
        if ("connected".equals(code)) return "Conectada e testada";
        if ("permission_ok".equals(code)) return "Permissão confirmada";
        if ("not_configured".equals(code)) return "Não vinculada";
        if ("permission_revoked".equals(code)) return "Permissão revogada";
        if ("provider_offline".equals(code)) return "Provedor offline";
        if ("folder_missing".equals(code)) return "Pasta não encontrada";
        return "Falha de acesso";
    }

    private static void putError(JSONObject out, String code, String detail, Exception error) {/*BR_JSON_WRAP*/try{
        try {
            out.put("code", code).put("healthy", false).put("detail", detail)
                    .put("error_type", error == null ? "" : error.getClass().getSimpleName());
        } catch(Exception ignored){AppLog.ignored("LibraryConnectionManager","Falha tolerada; a execução continuou.",ignored);}
    }catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException(e);}}

    private static String safe(Exception e) { return e == null ? "Falha desconhecida" : e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
    private static String trim(String value, int max) { String v = value == null ? "" : value.trim(); return v.length() <= max ? v : v.substring(0, max) + "…"; }
}
