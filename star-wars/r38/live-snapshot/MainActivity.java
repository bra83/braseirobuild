package com.braseiro.starwarsedge;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.View;
import android.view.WindowInsets;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_PDF_REQUEST = 4187;
    private static final int PICK_JSON_REQUEST = 4188;
    private static final long MAX_PDF_BYTES = 512L * 1024L * 1024L;
    private WebView webView;
    private GeminiBridge geminiBridge;
    private ImageLibraryBridge imageLibraryBridge;
    private TtsBridge ttsBridge;
    private FileBridge fileBridge;
    private ValueCallback<Uri[]> pendingJsonChooser;
    private final ExecutorService libraryExecutor = Executors.newSingleThreadExecutor();
    private final Object libraryMetadataLock = new Object();
    private File libraryRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PDFBoxResourceLoader.init(getApplicationContext());
        libraryRoot = new File(getFilesDir(), "library");
        if (!libraryRoot.exists()) libraryRoot.mkdirs();

        webView = new WebView(this);
        WebView.setWebContentsDebuggingEnabled(false);
        webView.setFitsSystemWindows(true);
        webView.setOnApplyWindowInsetsListener((view, insets) -> {
            int left = insets.getSystemWindowInsetLeft();
            int top = insets.getSystemWindowInsetTop();
            int right = insets.getSystemWindowInsetRight();
            int bottom = insets.getSystemWindowInsetBottom();
            view.setPadding(left, top, right, bottom);
            return insets;
        });
        setContentView(webView);
        webView.requestApplyInsets();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setDatabaseEnabled(true);
        settings.setTextZoom(100);
        geminiBridge = new GeminiBridge(this, webView);
        imageLibraryBridge = new ImageLibraryBridge(this, webView);
        ttsBridge = new TtsBridge(this);
        fileBridge = new FileBridge(this);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (pendingJsonChooser != null) pendingJsonChooser.onReceiveValue(null);
                pendingJsonChooser = callback;
                try {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/json", "text/plain"});
                    startActivityForResult(intent, PICK_JSON_REQUEST);
                    return true;
                } catch (Exception e) {
                    pendingJsonChooser = null;
                    callback.onReceiveValue(null);
                    return false;
                }
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                WebResourceResponse sceneImage = geminiBridge.openSceneImageResponse(request.getUrl());
                if (sceneImage != null) return sceneImage;
                WebResourceResponse libraryImage = imageLibraryBridge.openCachedImageResponse(request.getUrl());
                return libraryImage != null ? libraryImage : super.shouldInterceptRequest(view, request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                String scheme = uri == null ? "" : String.valueOf(uri.getScheme());
                if ("file".equalsIgnoreCase(scheme) && String.valueOf(uri.getPath()).startsWith("/android_asset/vtt/")) return false;
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
                }
                return true;
            }
        });
        webView.addJavascriptInterface(new LibraryBridge(), "BraseiroLibrary");
        webView.addJavascriptInterface(geminiBridge.javascriptInterface(), "BraseiroGemini");
        webView.addJavascriptInterface(imageLibraryBridge, "BraseiroImages");
        webView.addJavascriptInterface(ttsBridge, "BraseiroTts");
        webView.addJavascriptInterface(fileBridge, "BraseiroFiles");
        webView.loadUrl("file:///android_asset/vtt/r46-shell.html");
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        libraryExecutor.shutdownNow();
        if (geminiBridge != null) geminiBridge.shutdown();
        if (imageLibraryBridge != null) imageLibraryBridge.shutdown();
        if (ttsBridge != null) ttsBridge.shutdown();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (geminiBridge != null && geminiBridge.handleActivityResult(requestCode, resultCode, data)) return;
        if (requestCode == PICK_JSON_REQUEST) {
            ValueCallback<Uri[]> callback = pendingJsonChooser;
            pendingJsonChooser = null;
            if (callback != null) callback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            return;
        }
        if (requestCode != PICK_PDF_REQUEST) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            notifyLibraryEvent("cancelado", new JSONObject());
            return;
        }
        Uri uri = data.getData();
        libraryExecutor.submit(() -> importPdf(uri));
    }

    private final class LibraryBridge {
        @JavascriptInterface
        public String pickPdf() {
            try {
                runOnUiThread(() -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("application/pdf");
                        startActivityForResult(intent, PICK_PDF_REQUEST);
                    } catch (Exception e) {
                        try {
                            notifyLibraryEvent("erro", new JSONObject().put("message", "Não foi possível abrir o seletor de PDF."));
                        } catch (Exception ignored) { }
                    }
                });
                return new JSONObject().put("ok", true).put("status", "picker_opened").toString();
            } catch (Exception e) {
                return "{\"ok\":false,\"message\":\"Não foi possível abrir o seletor de PDF.\"}";
            }
        }

        @JavascriptInterface
        public String listDocuments() {
            try { return listDocumentsJson().toString(); } catch (Exception e) { return "[]"; }
        }

        @JavascriptInterface
        public String searchDocuments(String query, int limit) {
            try { return searchDocumentsJson(query, limit).toString(); } catch (Exception e) { return "{\"results\":[],\"grounded\":false}"; }
        }

        @JavascriptInterface
        public String setDocumentPolicy(String docId, String authorityType, boolean enabled) {
            try {
                File dir = safeDocumentDir(docId);
                JSONObject metadata;
                synchronized (libraryMetadataLock) {
                    metadata = readMetadata(dir);
                    if (metadata == null) return new JSONObject().put("ok", false).put("message", "Documento não encontrado.").toString();
                    metadata.put("authorityType", normalizeAuthorityType(authorityType));
                    metadata.put("enabled", enabled);
                    metadata.put("policyUpdatedAt", Instant.now().toString());
                    writeMetadata(dir, metadata);
                }
                notifyLibraryEvent("politica", new JSONObject().put("document", metadata));
                return new JSONObject().put("ok", true).put("document", metadata).toString();
            } catch (Exception e) {
                return "{\"ok\":false,\"message\":\"Falha ao atualizar a autoridade da fonte.\"}";
            }
        }

        @JavascriptInterface
        public String reindexDocument(String docId) {
            try {
                File dir = safeDocumentDir(docId);
                JSONObject metadata;
                File sourcePdf = new File(dir, "source.pdf");
                synchronized (libraryMetadataLock) {
                    metadata = readMetadata(dir);
                    if (metadata == null || !sourcePdf.exists()) return new JSONObject().put("ok", false).put("message", "PDF local não encontrado.").toString();
                    metadata.put("status", "INDEXANDO").remove("lastError");
                    metadata.put("reindexRequestedAt", Instant.now().toString());
                    writeMetadata(dir, metadata);
                }
                notifyLibraryEvent("indexando", new JSONObject().put("document", metadata));
                libraryExecutor.submit(() -> {
                    try { indexPdf(dir, sourcePdf, metadata); }
                    catch (Exception e) { markIndexError(dir, metadata, e); }
                });
                return new JSONObject().put("ok", true).put("document", metadata).toString();
            } catch (Exception e) {
                return "{\"ok\":false,\"message\":\"Falha ao iniciar a reindexação.\"}";
            }
        }

        @JavascriptInterface
        public String removeDocument(String docId) {
            try {
                File dir = safeDocumentDir(docId);
                synchronized (libraryMetadataLock) {
                    if (!dir.exists()) return new JSONObject().put("ok", false).put("message", "Documento não encontrado.").toString();
                    deleteRecursively(dir);
                    if (dir.exists()) throw new IllegalStateException("Não foi possível remover completamente o documento.");
                }
                notifyLibraryEvent("removido", new JSONObject().put("docId", docId));
                return new JSONObject().put("ok", true).put("docId", docId).toString();
            } catch (Exception e) {
                return "{\"ok\":false,\"message\":\"Falha ao remover documento.\"}";
            }
        }
    }

    private void importPdf(Uri uri) {
        File tempDir = new File(libraryRoot, "import_" + UUID.randomUUID());
        tempDir.mkdirs();
        File tempPdf = new File(tempDir, "source.pdf");
        try {
            String displayName = displayName(uri);
            long declaredSize = declaredSize(uri);
            if (declaredSize > MAX_PDF_BYTES) throw new IllegalArgumentException("O PDF excede o limite local de 512 MB.");

            try (InputStream in = getContentResolver().openInputStream(uri); FileOutputStream out = new FileOutputStream(tempPdf)) {
                if (in == null) throw new IllegalArgumentException("Não foi possível abrir o PDF selecionado.");
                byte[] buffer = new byte[64 * 1024];
                long total = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_PDF_BYTES) throw new IllegalArgumentException("O PDF excede o limite local de 512 MB.");
                    out.write(buffer, 0, read);
                }
            }

            String sha = sha256(tempPdf);
            String docId = "pdf_" + sha.substring(0, 20);
            File finalDir = new File(libraryRoot, docId);
            if (finalDir.exists()) {
                deleteRecursively(tempDir);
                JSONObject existing = readMetadata(finalDir);
                notifyLibraryEvent("duplicado", new JSONObject().put("document", existing));
                return;
            }
            if (!tempDir.renameTo(finalDir)) throw new IllegalStateException("Não foi possível preservar o PDF na biblioteca local.");
            File sourcePdf = new File(finalDir, "source.pdf");

            JSONObject metadata = new JSONObject()
                    .put("docId", docId)
                    .put("fileName", displayName)
                    .put("title", stripPdfExtension(displayName))
                    .put("sha256", sha)
                    .put("sizeBytes", sourcePdf.length())
                    .put("importedAt", Instant.now().toString())
                    .put("status", "INDEXANDO")
                    .put("pageCount", 0)
                    .put("indexedPages", 0)
                    .put("textChars", 0)
                    .put("storage", "privado_offline")
                    .put("authorityType", "SUPLEMENTO")
                    .put("enabled", true);
            writeMetadata(finalDir, metadata);
            notifyLibraryEvent("indexando", new JSONObject().put("document", metadata));

            try { indexPdf(finalDir, sourcePdf, metadata); }
            catch (Exception e) { markIndexError(finalDir, metadata, e); }
        } catch (Exception e) {
            try {
                JSONObject error = new JSONObject().put("message", safeMessage(e));
                notifyLibraryEvent("erro", error);
            } catch (Exception ignored) {}
            if (tempDir.exists()) deleteRecursively(tempDir);
        }
    }

    private void indexPdf(File dir, File pdf, JSONObject metadata) throws Exception {
        File pagesFile = new File(dir, "pages.jsonl");
        int indexedPages = 0;
        long chars = 0;
        try (PDDocument document = PDDocument.load(pdf); BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(pagesFile), StandardCharsets.UTF_8))) {
            int pageCount = document.getNumberOfPages();
            metadata.put("pageCount", pageCount);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = sanitizeExtractedText(stripper.getText(document));
                if (!text.trim().isEmpty()) indexedPages++;
                chars += text.length();
                writer.write(new JSONObject().put("page", page).put("text", text).toString());
                writer.newLine();
                if (page % 10 == 0 || page == pageCount) {
                    metadata.put("indexedPages", indexedPages).put("textChars", chars);
                    writeIndexMetadataPreservingPolicy(dir, metadata);
                }
            }
        }
        metadata.put("indexedPages", indexedPages).put("textChars", chars);
        metadata.put("status", chars >= 40 ? "INDEXADO" : "ANEXADO_SEM_TEXTO");
        metadata.put("indexedAt", Instant.now().toString());
        writeIndexMetadataPreservingPolicy(dir, metadata);
        notifyLibraryEvent("concluido", new JSONObject().put("document", metadata));
    }

    private JSONArray listDocumentsJson() {
        JSONArray out = new JSONArray();
        File[] dirs = libraryRoot.listFiles(File::isDirectory);
        if (dirs == null) return out;
        List<JSONObject> rows = new ArrayList<>();
        for (File dir : dirs) {
            if (dir.getName().startsWith("import_")) continue;
            JSONObject metadata = readMetadata(dir);
            if (metadata != null) rows.add(metadata);
        }
        rows.sort((a,b) -> b.optString("importedAt", "").compareTo(a.optString("importedAt", "")));
        for (JSONObject row : rows) out.put(row);
        return out;
    }

    private JSONObject searchDocumentsJson(String query, int limit) throws Exception {
        JSONObject out = new JSONObject();
        JSONArray results = new JSONArray();
        String normalizedQuery = normalize(query);
        if (normalizedQuery.trim().isEmpty()) return out.put("results", results).put("grounded", false);
        String[] tokens = normalizedQuery.split("\\s+");
        int safeLimit = Math.max(1, Math.min(20, limit));
        List<SearchHit> hits = new ArrayList<>();
        File[] dirs = libraryRoot.listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                JSONObject meta = readMetadata(dir);
                if (meta == null || !"INDEXADO".equals(meta.optString("status")) || !meta.optBoolean("enabled", true)) continue;
                File pages = new File(dir, "pages.jsonl");
                if (!pages.exists()) continue;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(pages), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        JSONObject page = new JSONObject(line);
                        String text = page.optString("text", "");
                        String hay = normalize(meta.optString("title", "") + " " + text);
                        double score = lexicalScore(hay, normalizedQuery, tokens, normalize(meta.optString("title", "")));
                        if (score <= 0) continue;
                        hits.add(new SearchHit(meta, page.optInt("page", 0), text, score, normalizedQuery, tokens));
                    }
                } catch (Exception ignored) {}
            }
        }
        hits.sort(Comparator.comparingDouble((SearchHit h) -> h.score).reversed());
        for (int i = 0; i < Math.min(safeLimit, hits.size()); i++) results.put(hits.get(i).toJson());
        return out.put("results", results).put("grounded", results.length() > 0);
    }

    private static double lexicalScore(String hay, String query, String[] tokens, String title) {
        double score = hay.contains(query) ? 8.0 : 0.0;
        for (String token : tokens) {
            if (token.length() < 2 || isStopWord(token)) continue;
            int occurrences = countOccurrences(hay, token);
            if (occurrences > 0) score += Math.min(3.0, 0.8 + Math.log1p(occurrences));
            if (title.contains(token)) score += 1.8;
        }
        return score;
    }

    private static boolean isStopWord(String value) {
        return value.matches("a|o|as|os|de|da|do|das|dos|e|em|um|uma|para|por|com|como|que|qual|quais|no|na|nos|nas");
    }

    private static int countOccurrences(String text, String token) {
        int count = 0, from = 0;
        while ((from = text.indexOf(token, from)) >= 0) { count++; from += token.length(); }
        return count;
    }

    private static String normalizeAuthorityType(String value) {
        String type = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (type.matches("ERRATA|BASE|SUPLEMENTO|AVENTURA|REFERENCIA")) return type;
        return "SUPLEMENTO";
    }

    private static String normalize(String value) {
        String n = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String excerpt(String text, String normalizedQuery, String[] tokens) {
        String compact = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (compact.length() <= 420) return compact;
        String normalized = normalize(compact);
        int at = normalized.indexOf(normalizedQuery);
        if (at < 0) {
            for (String token : tokens) { at = normalized.indexOf(token); if (at >= 0) break; }
        }
        if (at < 0) return compact.substring(0, 420) + "…";
        int start = Math.max(0, at - 120);
        int end = Math.min(compact.length(), start + 420);
        return (start > 0 ? "…" : "") + compact.substring(start, end) + (end < compact.length() ? "…" : "");
    }

    private static final class SearchHit {
        final JSONObject meta; final int page; final String text; final double score; final String query; final String[] tokens;
        SearchHit(JSONObject meta, int page, String text, double score, String query, String[] tokens) {
            this.meta=meta; this.page=page; this.text=text; this.score=score; this.query=query; this.tokens=tokens;
        }
        JSONObject toJson() throws Exception {
            String docId=meta.optString("docId");
            return new JSONObject()
                    .put("id", "LIB-" + docId + "-P" + page)
                    .put("docId", docId)
                    .put("title", meta.optString("title", meta.optString("fileName", "PDF importado")))
                    .put("fileName", meta.optString("fileName"))
                    .put("source", "Biblioteca · " + meta.optString("fileName"))
                    .put("page", page)
                    .put("excerpt", excerpt(text, query, tokens))
                    .put("score", Math.round(score * 10000.0) / 10000.0)
                    .put("authorityType", normalizeAuthorityType(meta.optString("authorityType", "SUPLEMENTO")))
                    .put("enabled", meta.optBoolean("enabled", true));
        }
    }

    private void markIndexError(File dir, JSONObject metadata, Exception error) {
        try {
            metadata.put("status", "ERRO");
            metadata.put("lastError", safeMessage(error));
            metadata.put("indexFailedAt", Instant.now().toString());
            writeIndexMetadataPreservingPolicy(dir, metadata);
            notifyLibraryEvent("erro", new JSONObject().put("document", metadata).put("message", safeMessage(error)));
        } catch (Exception ignored) {}
    }

    private void writeIndexMetadataPreservingPolicy(File dir, JSONObject metadata) throws Exception {
        synchronized (libraryMetadataLock) {
            if (!dir.exists()) throw new IllegalStateException("Documento removido durante a indexação.");
            JSONObject latest = readMetadata(dir);
            if (latest != null) {
                if (latest.has("authorityType")) metadata.put("authorityType", normalizeAuthorityType(latest.optString("authorityType", "SUPLEMENTO")));
                if (latest.has("enabled")) metadata.put("enabled", latest.optBoolean("enabled", true));
                if (latest.has("policyUpdatedAt")) metadata.put("policyUpdatedAt", latest.optString("policyUpdatedAt", ""));
            }
            writeMetadata(dir, metadata);
        }
    }

    private JSONObject readMetadata(File dir) {
        synchronized (libraryMetadataLock) {
            try {
                File file = new File(dir, "metadata.json");
                if (!file.exists()) return null;
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                try (FileInputStream in = new FileInputStream(file)) {
                    byte[] chunk = new byte[8192]; int read;
                    while ((read = in.read(chunk)) != -1) buffer.write(chunk, 0, read);
                }
                return new JSONObject(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
            } catch (Exception e) { return null; }
        }
    }

    private void writeMetadata(File dir, JSONObject metadata) throws Exception {
        synchronized (libraryMetadataLock) {
            File tmp = new File(dir, "metadata.tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) { out.write(metadata.toString(2).getBytes(StandardCharsets.UTF_8)); }
            File target = new File(dir, "metadata.json");
            if (target.exists() && !target.delete()) throw new IllegalStateException("Falha ao atualizar metadados da biblioteca.");
            if (!tmp.renameTo(target)) throw new IllegalStateException("Falha ao confirmar metadados da biblioteca.");
        }
    }

    private File safeDocumentDir(String docId) {
        if (docId == null || !docId.matches("pdf_[a-f0-9]{20}")) throw new IllegalArgumentException("Identificador de documento inválido.");
        return new File(libraryRoot, docId);
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } catch (Exception ignored) {}
        return "manual-importado.pdf";
    }

    private long declaredSize(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.SIZE}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getLong(0);
        } catch (Exception ignored) {}
        return -1;
    }

    private static String stripPdfExtension(String name) { return name == null ? "PDF importado" : name.replaceFirst("(?i)\\.pdf$", ""); }
    private static String sanitizeExtractedText(String text) { return (text == null ? "" : text).replace("\u0000", "").replaceAll("[\\t ]+", " ").replaceAll("\\n{3,}", "\n\n").trim(); }
    private static String safeMessage(Exception e) { return e.getMessage() == null || e.getMessage().trim().isEmpty() ? "Falha ao importar ou indexar o PDF." : e.getMessage(); }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[64 * 1024]; int read;
            while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder sb = new StringBuilder(); for (byte b : digest.digest()) sb.append(String.format(Locale.ROOT, "%02x", b)); return sb.toString();
    }

    private static void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles(); if (children != null) for (File child : children) deleteRecursively(child);
        file.delete();
    }

    private void notifyLibraryEvent(String type, JSONObject payload) {
        try {
            JSONObject envelope = new JSONObject().put("type", type).put("payload", payload);
            String encoded = JSONObject.quote(envelope.toString());
            runOnUiThread(() -> {
                if (webView != null) webView.evaluateJavascript("window.braseiroLibraryNativeEvent && window.braseiroLibraryNativeEvent(" + encoded + ");", null);
            });
        } catch (Exception ignored) {}
    }

}
