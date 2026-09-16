package com.braseiro.wwn.bridge;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000^\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\b\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0010$\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\"\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0006\n\u0002\u0010 \n\u0002\b\u000e\u0018\u0000 52\u00020\u0001:\u00015B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\u0006\u0010\t\u001a\u00020\u0007J\u0006\u0010\n\u001a\u00020\u0007J\u000e\u0010\u000b\u001a\u00020\u00072\u0006\u0010\f\u001a\u00020\rJ>\u0010\u000e\u001a\u00020\u00072\u0006\u0010\u000f\u001a\u00020\u00102\b\u0010\u0011\u001a\u0004\u0018\u00010\r2\u0006\u0010\u0012\u001a\u00020\r2\u001c\b\u0002\u0010\u0013\u001a\u0016\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0016\u0018\u00010\u0014J\u000e\u0010\u0017\u001a\u00020\u00072\u0006\u0010\u0018\u001a\u00020\rJN\u0010\u0019\u001a\u000e\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\r0\u001a2\u0006\u0010\u001b\u001a\u00020\u001c2\f\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00150\u001e2\u0006\u0010\u001f\u001a\u00020\u00152\u001a\u0010\u0013\u001a\u0016\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0015\u0012\u0004\u0012\u00020\u0016\u0018\u00010\u0014H\u0002J\u0018\u0010 \u001a\u00020!2\u0006\u0010\"\u001a\u00020\u00072\u0006\u0010#\u001a\u00020\rH\u0002J\u0010\u0010$\u001a\u00020\r2\u0006\u0010\"\u001a\u00020\u0007H\u0002J\u0010\u0010%\u001a\u00020\r2\u0006\u0010&\u001a\u00020\rH\u0002J\u0016\u0010\'\u001a\b\u0012\u0004\u0012\u00020\r0(2\u0006\u0010&\u001a\u00020\rH\u0002J\u0016\u0010)\u001a\b\u0012\u0004\u0012\u00020\r0(2\u0006\u0010*\u001a\u00020\rH\u0002J\u0010\u0010+\u001a\u00020\r2\u0006\u0010,\u001a\u00020\rH\u0002J\b\u0010-\u001a\u00020\u0007H\u0002J\u0010\u0010.\u001a\u00020\u00162\u0006\u0010/\u001a\u00020\u0007H\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0006\u001a\u0004\u0018\u00010\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0001X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u00100\u001a\u00020\u001c8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b1\u00102R\u0014\u00103\u001a\u00020\u001c8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b4\u00102\u00a8\u00066"}, d2 = {"Lcom/braseiro/wwn/bridge/RagRuntime;", "", "context", "Landroid/content/Context;", "<init>", "(Landroid/content/Context;)V", "cachedRoot", "Lorg/json/JSONObject;", "indexLock", "status", "listDocuments", "removeDocument", "documentId", "", "importPdf", "uri", "Landroid/net/Uri;", "displayName", "requestedAuthorityProfile", "progress", "Lkotlin/Function2;", "", "", "search", "requestJson", "ocrPages", "", "file", "Ljava/io/File;", "pageIndexes", "", "totalPages", "allowed", "", "d", "scope", "allChunkText", "normalize", "s", "tokens", "", "chunkText", "text", "normalizeProfile", "value", "loadIndex", "saveIndex", "root", "ragDir", "getRagDir", "()Ljava/io/File;", "indexFile", "getIndexFile", "Companion", "app"})
public final class RagRuntime {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile org.json.JSONObject cachedRoot;
    @org.jetbrains.annotations.NotNull()
    private final java.lang.Object indexLock = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String INDEX_SCHEMA = "braseiro.wwn.rag.native.v1";
    private static final long MAX_PDF_BYTES = 83886080L;
    private static final int MAX_PAGES = 2500;
    private static final int MAX_OCR_PAGES = 600;
    private static final int MIN_TEXT_CHARS_PER_PAGE = 24;
    private static final int OCR_TARGET_WIDTH = 1500;
    private static final long OCR_TIMEOUT_SECONDS = 25L;
    private static final int MAX_EXTRACTED_CHARS = 12000000;
    private static final int MAX_INDEX_JSON_CHARS = 45000000;
    private static final int CHUNK_CHARS = 1200;
    private static final int CHUNK_OVERLAP = 160;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Set<java.lang.String> STOPWORDS = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.braseiro.wwn.bridge.RagRuntime.Companion Companion = null;
    
    public RagRuntime(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject status() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject listDocuments() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject removeDocument(@org.jetbrains.annotations.NotNull()
    java.lang.String documentId) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject importPdf(@org.jetbrains.annotations.NotNull()
    android.net.Uri uri, @org.jetbrains.annotations.Nullable()
    java.lang.String displayName, @org.jetbrains.annotations.NotNull()
    java.lang.String requestedAuthorityProfile, @org.jetbrains.annotations.Nullable()
    kotlin.jvm.functions.Function2<? super java.lang.Integer, ? super java.lang.Integer, kotlin.Unit> progress) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject search(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    private final java.util.Map<java.lang.Integer, java.lang.String> ocrPages(java.io.File file, java.util.Set<java.lang.Integer> pageIndexes, int totalPages, kotlin.jvm.functions.Function2<? super java.lang.Integer, ? super java.lang.Integer, kotlin.Unit> progress) {
        return null;
    }
    
    private final boolean allowed(org.json.JSONObject d, java.lang.String scope) {
        return false;
    }
    
    private final java.lang.String allChunkText(org.json.JSONObject d) {
        return null;
    }
    
    private final java.lang.String normalize(java.lang.String s) {
        return null;
    }
    
    private final java.util.List<java.lang.String> tokens(java.lang.String s) {
        return null;
    }
    
    private final java.util.List<java.lang.String> chunkText(java.lang.String text) {
        return null;
    }
    
    private final java.lang.String normalizeProfile(java.lang.String value) {
        return null;
    }
    
    private final org.json.JSONObject loadIndex() {
        return null;
    }
    
    private final void saveIndex(org.json.JSONObject root) {
    }
    
    private final java.io.File getRagDir() {
        return null;
    }
    
    private final java.io.File getIndexFile() {
        return null;
    }
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\t\n\u0002\u0010\"\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\tX\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00050\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0014"}, d2 = {"Lcom/braseiro/wwn/bridge/RagRuntime$Companion;", "", "<init>", "()V", "INDEX_SCHEMA", "", "MAX_PDF_BYTES", "", "MAX_PAGES", "", "MAX_OCR_PAGES", "MIN_TEXT_CHARS_PER_PAGE", "OCR_TARGET_WIDTH", "OCR_TIMEOUT_SECONDS", "MAX_EXTRACTED_CHARS", "MAX_INDEX_JSON_CHARS", "CHUNK_CHARS", "CHUNK_OVERLAP", "STOPWORDS", "", "app"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}