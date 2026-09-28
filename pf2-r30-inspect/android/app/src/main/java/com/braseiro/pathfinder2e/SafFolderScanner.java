package com.braseiro.pathfinder2e;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.DocumentsContract;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * Repositório de leitura para árvores escolhidas com ACTION_OPEN_DOCUMENT_TREE.
 * Não depende de OAuth nem de uma conta específica: funciona com Google Drive,
 * armazenamento local e qualquer provedor DocumentsProvider compatível.
 */
public final class SafFolderScanner {
    public enum Kind { PDF, IMAGE, AUDIO, JSON, ANY_FILE }
    public interface Progress { void onProgress(int folders, int files, String path); }
    public interface Cancellation { boolean isCancelled(); }

    private static final int MAX_DEPTH = 32;
    private static final int MAX_FOLDERS = 10000;
    private static final int MAX_FILES = 50000;
    private static final int STABLE_QUERY_ATTEMPTS = 12;
    private static final int PAGE_SIZE = 128;
    private final ContentResolver resolver;

    public SafFolderScanner(Context context) {
        if (context == null) throw new IllegalArgumentException("Contexto ausente");
        resolver = context.getApplicationContext().getContentResolver();
    }

    public List<JSONObject> listPdfs(Uri treeUri) throws Exception { return list(treeUri, Kind.PDF, null, null); }
    /** Lista PDFs de manuais sem entrar em bibliotecas reservadas, como tokens. */
    public List<JSONObject> listManualPdfs(Uri treeUri, Progress progress, Cancellation cancellation) throws Exception {
        return listInternal(treeUri, Kind.PDF, progress, cancellation, true);
    }
    public List<JSONObject> listImages(Uri treeUri) throws Exception { return list(treeUri, Kind.IMAGE, null, null); }
    public List<JSONObject> listAudio(Uri treeUri) throws Exception { return list(treeUri, Kind.AUDIO, null, null); }
    public List<JSONObject> listJson(Uri treeUri) throws Exception { return list(treeUri, Kind.JSON, null, null); }

    public List<JSONObject> list(Uri treeUri, Kind kind, Progress progress, Cancellation cancellation) throws Exception {
        return listInternal(treeUri, kind, progress, cancellation, false);
    }

    private List<JSONObject> listInternal(Uri treeUri, Kind kind, Progress progress, Cancellation cancellation, boolean manualMode) throws Exception {
        Tree tree = validateTree(treeUri);
        ArrayList<JSONObject> out = new ArrayList<JSONObject>();
        HashSet<String> visitedFolders = new HashSet<String>();
        HashSet<String> visitedFiles = new HashSet<String>();
        ArrayDeque<FolderJob> queue = new ArrayDeque<FolderJob>();
        queue.add(new FolderJob(tree.id, "", 0));

        while (!queue.isEmpty()) {
            ensureNotCancelled(cancellation);
            FolderJob job = queue.removeFirst();
            if (job.depth > MAX_DEPTH || !visitedFolders.add(job.id)) continue;
            if (visitedFolders.size() > MAX_FOLDERS)
                throw new Exception("A pasta possui subpastas demais. Selecione uma pasta mais específica.");

            List<Row> rows = queryChildrenStable(tree.uri, job.id, cancellation, manualMode);
            for (Row row : rows) {
                ensureNotCancelled(cancellation);
                if (row.id == null || row.id.length() == 0) continue;
                String name = cleanName(row.name);
                String path = job.path.length() == 0 ? name : job.path + "/" + name;
                if (isDirectory(row.mime)) {
                    if (manualMode && isReservedManualFolder(name, path)) continue;
                    queue.addLast(new FolderJob(row.id, path, job.depth + 1));
                    continue;
                }
                if (!visitedFiles.add(row.id)) continue;
                if (visitedFiles.size() > MAX_FILES)
                    throw new Exception("A pasta possui arquivos demais para uma sincronização segura. Divida a biblioteca em pastas menores.");
                if (!matches(kind, row.mime, name)) continue;
                out.add(toJson(tree.uri, row, name, path));
            }
            if (progress != null) progress.onProgress(visitedFolders.size(), visitedFiles.size(), job.path);
        }

        Collections.sort(out, new Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                return a.optString("path", a.optString("name"))
                        .compareToIgnoreCase(b.optString("path", b.optString("name")));
            }
        });
        return out;
    }

    /** Testa permissão, leitura da raiz e abertura real de um arquivo, sem varrer a árvore inteira. */
    public JSONObject probe(Uri treeUri) throws Exception {
        Tree tree = validateTree(treeUri);
        List<Row> root = queryChildrenStable(tree.uri, tree.id, null, false);
        int folders = 0, files = 0;
        boolean opened = false;
        String sample = "";
        String sampleMime = "";
        for (Row row : root) {
            if (isDirectory(row.mime)) { folders++; continue; }
            files++;
            if (!opened) {
                Uri document = DocumentsContract.buildDocumentUriUsingTree(tree.uri, row.id);
                InputStream in = null;
                try {
                    in = resolver.openInputStream(document);
                    if (in != null) { in.read(); opened = true; sample = cleanName(row.name); sampleMime = row.mime == null ? "" : row.mime; }
                } catch (SecurityException e) {
                    throw new Exception("A permissão da pasta foi revogada pelo Android.", e);
                } finally {
                    if (in != null) try { in.close(); } catch (Exception closeError) { AppLog.w("SafFolderScanner", "Falha ao fechar arquivo de teste.", closeError); }
                }
            }
        }
        JSONObject out = new JSONObject();
        out.put("readable", true);
        out.put("root_entries", root.size());
        out.put("root_folders", folders);
        out.put("root_files", files);
        out.put("sample_opened", opened || files == 0);
        out.put("sample_name", sample);
        out.put("sample_mime", sampleMime);
        out.put("authority", tree.uri.getAuthority() == null ? "" : tree.uri.getAuthority());
        out.put("tree_id", tree.id);
        return out;
    }

    private Tree validateTree(Uri treeUri) throws Exception {
        if (treeUri == null) throw new Exception("Nenhuma pasta foi selecionada.");
        if (!"content".equalsIgnoreCase(treeUri.getScheme()))
            throw new Exception("A pasta selecionada não foi fornecida por um provedor de documentos do Android.");
        try {
            String id = DocumentsContract.getTreeDocumentId(treeUri);
            if (id == null || id.length() == 0) throw new Exception("Identificador da árvore vazio.");
            return new Tree(treeUri, id);
        } catch (SecurityException e) {
            throw new Exception("O Android perdeu a permissão de leitura da pasta. Selecione-a novamente.", e);
        } catch (Exception e) {
            throw new Exception("A pasta selecionada não é uma árvore de documentos válida.", e);
        }
    }

    private List<Row> queryChildrenStable(Uri treeUri, String parentId, Cancellation cancellation, boolean fastManualScan) throws Exception {
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId);
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_FLAGS
        };
        LinkedHashMap<String, Row> all = new LinkedHashMap<String, Row>();
        String previousSignature = "";
        int stablePasses = 0;

        int attemptLimit = STABLE_QUERY_ATTEMPTS;
        int requiredStablePasses = 2;
        for (int attempt = 0; attempt < attemptLimit && stablePasses < requiredStablePasses; attempt++) {
            ensureNotCancelled(cancellation);
            Cursor cursor = null;
            boolean loading = false;
            try {
                cursor = resolver.query(children, projection, null, null, null);
                if (cursor == null) throw new Exception("O provedor não permitiu ler a pasta escolhida.");
                collect(cursor, all);
                Bundle extras = cursor.getExtras();
                loading = extras != null && extras.getBoolean(DocumentsContract.EXTRA_LOADING, false);
            } catch (SecurityException e) {
                throw new Exception("O Android perdeu a permissão de leitura da pasta. Selecione-a novamente.", e);
            } finally {
                if (cursor != null) cursor.close();
            }
            String signature = signature(all);
            if (!loading && signature.equals(previousSignature)) stablePasses++; else stablePasses = 0;
            previousSignature = signature;
            if (loading || stablePasses < requiredStablePasses) sleepInterruptibly(fastManualScan ? 220L : 300L, cancellation);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            HashSet<String> pageSignatures = new HashSet<String>();
            for (int offset = 0; offset < MAX_FILES; offset += PAGE_SIZE) {
                ensureNotCancelled(cancellation);
                Cursor cursor = null;
                ArrayList<String> pageIds = new ArrayList<String>();
                try {
                    Bundle args = new Bundle();
                    args.putInt(ContentResolver.QUERY_ARG_LIMIT, PAGE_SIZE);
                    args.putInt(ContentResolver.QUERY_ARG_OFFSET, offset);
                    cursor = resolver.query(children, projection, args, new CancellationSignal());
                    if (cursor == null) break;
                    int idColumn = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
                    while (cursor.moveToNext()) {
                        String id = idColumn >= 0 ? cursor.getString(idColumn) : "";
                        if (id != null && id.length() > 0) pageIds.add(id);
                    }
                } catch (UnsupportedOperationException ignored) {
                    break;
                } catch (IllegalArgumentException ignored) {
                    break;
                } catch (SecurityException e) {
                    throw new Exception("O Android perdeu a permissão de leitura da pasta.", e);
                } finally {
                    if (cursor != null) cursor.close();
                }
                if (pageIds.isEmpty()) break;
                String pageSignature = pageIds.size() + ":" + pageIds.get(0) + ":" + pageIds.get(pageIds.size() - 1);
                if (!pageSignatures.add(pageSignature)) break;

                Cursor full = null;
                try {
                    Bundle args = new Bundle();
                    args.putInt(ContentResolver.QUERY_ARG_LIMIT, PAGE_SIZE);
                    args.putInt(ContentResolver.QUERY_ARG_OFFSET, offset);
                    full = resolver.query(children, projection, args, new CancellationSignal());
                    if (full != null) collect(full, all);
                } catch (UnsupportedOperationException ignored) {
                    break;
                } catch (IllegalArgumentException ignored) {
                    break;
                } finally {
                    if (full != null) full.close();
                }
                if (pageIds.size() < PAGE_SIZE) break;
            }
        }
        return new ArrayList<Row>(all.values());
    }

    private void collect(Cursor cursor, LinkedHashMap<String, Row> all) {
        int idCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
        int nameCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
        int mimeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
        int sizeCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
        int modifiedCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);
        int flagsCol = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS);
        while (cursor.moveToNext()) {
            String id = idCol >= 0 ? cursor.getString(idCol) : "";
            if (id == null || id.length() == 0) continue;
            Row row = new Row();
            row.id = id;
            row.name = nameCol >= 0 && !cursor.isNull(nameCol) ? cursor.getString(nameCol) : "";
            row.mime = mimeCol >= 0 && !cursor.isNull(mimeCol) ? cursor.getString(mimeCol) : "";
            row.size = sizeCol >= 0 && !cursor.isNull(sizeCol) ? cursor.getLong(sizeCol) : 0L;
            row.modified = modifiedCol >= 0 && !cursor.isNull(modifiedCol) ? cursor.getLong(modifiedCol) : 0L;
            row.flags = flagsCol >= 0 && !cursor.isNull(flagsCol) ? cursor.getInt(flagsCol) : 0;
            all.put(id, row);
        }
    }

    private JSONObject toJson(Uri treeUri, Row row, String name, String path) throws Exception {
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, row.id);
        JSONObject item = new JSONObject();
        item.put("documentId", row.id);
        item.put("sourceUri", documentUri.toString());
        item.put("name", name);
        item.put("path", path);
        item.put("mimeType", normalizeMime(row.mime, name));
        item.put("size", Math.max(0L, row.size));
        item.put("modifiedTime", Math.max(0L, row.modified));
        item.put("flags", row.flags);
        item.put("virtual", (row.flags & DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT) != 0);
        return item;
    }

    private static boolean matches(Kind kind, String mime, String name) {
        if (kind == null || kind == Kind.ANY_FILE) return true;
        String m = mime == null ? "" : mime.toLowerCase(Locale.US);
        String n = name == null ? "" : name.toLowerCase(Locale.US);
        if (kind == Kind.PDF) return "application/pdf".equals(m) || n.endsWith(".pdf");
        if (kind == Kind.IMAGE) return m.startsWith("image/") || n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".webp");
        if (kind == Kind.AUDIO) return m.startsWith("audio/") || n.endsWith(".mp3") || n.endsWith(".ogg") || n.endsWith(".wav") || n.endsWith(".m4a") || n.endsWith(".flac") || n.endsWith(".aac");
        if (kind == Kind.JSON) return "application/json".equals(m) || "text/json".equals(m) || n.endsWith(".json");
        return false;
    }

    private static String normalizeMime(String mime, String name) {
        String m = mime == null ? "" : mime.trim();
        if (m.length() > 0 && !"application/octet-stream".equalsIgnoreCase(m)) return m;
        String n = name == null ? "" : name.toLowerCase(Locale.US);
        if (n.endsWith(".pdf")) return "application/pdf";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".mp3")) return "audio/mpeg";
        if (n.endsWith(".ogg")) return "audio/ogg";
        if (n.endsWith(".wav")) return "audio/wav";
        if (n.endsWith(".m4a")) return "audio/mp4";
        if (n.endsWith(".flac")) return "audio/flac";
        if (n.endsWith(".json")) return "application/json";
        return m.length() == 0 ? "application/octet-stream" : m;
    }

    private static String cleanName(String value) {
        String name = value == null ? "" : value.trim();
        return name.length() == 0 ? "Sem nome" : name;
    }

    private static boolean isDirectory(String mime) {
        if (DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)) return true;
        String normalized = mime == null ? "" : mime.toLowerCase(Locale.US);
        return normalized.contains("google-apps.folder") || normalized.endsWith("/folder") || normalized.endsWith(" folder");
    }

    private static String signature(LinkedHashMap<String, Row> rows) {
        if (rows.isEmpty()) return "0";
        String first = rows.keySet().iterator().next();
        String last = first;
        long modified = 0L;
        for (String id : rows.keySet()) { last = id; Row row = rows.get(id); if (row != null) modified ^= row.modified; }
        return rows.size() + ":" + first + ":" + last + ":" + modified;
    }

    private static void ensureNotCancelled(Cancellation cancellation) throws Exception {
        if (Thread.currentThread().isInterrupted() || (cancellation != null && cancellation.isCancelled()))
            throw new Exception("Sincronização cancelada.");
    }

    private static void sleepInterruptibly(long millis, Cancellation cancellation) throws Exception {
        ensureNotCancelled(cancellation);
        try { Thread.sleep(millis); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new Exception("Sincronização cancelada.", e); }
    }

    private static final class Tree { final Uri uri; final String id; Tree(Uri uri, String id) { this.uri = uri; this.id = id; } }
    private static final class FolderJob { final String id, path; final int depth; FolderJob(String id, String path, int depth) { this.id = id; this.path = path; this.depth = depth; } }
    private static final class Row { String id, name, mime; long size, modified; int flags; }
    private static boolean isReservedManualFolder(String name, String path) {
        String n = normalizeFolderName(name);
        if ("tokens".equals(n) || "token".equals(n) || "tokens pdf".equals(n) || "pdf tokens".equals(n) ||
                "fichas tokens".equals(n) || "imagens tokens".equals(n)) return true;
        String p = normalizeFolderName(path);
        for (String part : p.split("/")) {
            String x = normalizeFolderName(part);
            if ("tokens".equals(x) || "token".equals(x) || "tokens pdf".equals(x) || "pdf tokens".equals(x)) return true;
        }
        return false;
    }

    private static String normalizeFolderName(String value) {
        if (value == null) return "";
        String n = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.US)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
        return n;
    }

}
