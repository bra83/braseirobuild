package com.braseiro.wwn.bridge;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000N\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0007\u0018\u0000 \u001f2\u00020\u0001:\u0003\u001d\u001e\u001fB\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\u000e\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bJ\u0016\u0010\f\u001a\u00020\t2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010J\u0010\u0010\u0011\u001a\u0004\u0018\u00010\u00122\u0006\u0010\n\u001a\u00020\u000bJ(\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u00072\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u00192\u0006\u0010\u001a\u001a\u00020\u0019H\u0002J\u0010\u0010\u001b\u001a\u00020\u00102\u0006\u0010\u0015\u001a\u00020\u0007H\u0002J\u0012\u0010\u001c\u001a\u0004\u0018\u00010\u00172\u0006\u0010\r\u001a\u00020\u000eH\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006 "}, d2 = {"Lcom/braseiro/wwn/bridge/SceneImageRuntime;", "", "context", "Landroid/content/Context;", "<init>", "(Landroid/content/Context;)V", "directory", "Ljava/io/File;", "importImage", "Lorg/json/JSONObject;", "uri", "Landroid/net/Uri;", "saveGeneratedImage", "bytes", "", "mimeType", "", "openWebResource", "Landroid/webkit/WebResourceResponse;", "normalizeForMobile", "Lcom/braseiro/wwn/bridge/SceneImageRuntime$StoredImage;", "file", "type", "Lcom/braseiro/wwn/bridge/SceneImageRuntime$ImageType;", "width", "", "height", "sha256", "detectType", "StoredImage", "ImageType", "Companion", "app"})
public final class SceneImageRuntime {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final java.io.File directory = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String LOCAL_HOST = "appassets.androidplatform.net";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String LOCAL_ORIGIN = "https://appassets.androidplatform.net";
    private static final long MAX_IMAGE_BYTES = 12582912L;
    private static final int MAX_DIMENSION = 8192;
    private static final long MAX_PIXELS = 40000000L;
    private static final int MAX_STORED_DIMENSION = 2048;
    private static final long MAX_STORED_PIXELS = 4000000L;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex SAFE_NAME = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.braseiro.wwn.bridge.SceneImageRuntime.Companion Companion = null;
    
    public SceneImageRuntime(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject importImage(@org.jetbrains.annotations.NotNull()
    android.net.Uri uri) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject saveGeneratedImage(@org.jetbrains.annotations.NotNull()
    byte[] bytes, @org.jetbrains.annotations.NotNull()
    java.lang.String mimeType) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final android.webkit.WebResourceResponse openWebResource(@org.jetbrains.annotations.NotNull()
    android.net.Uri uri) {
        return null;
    }
    
    private final com.braseiro.wwn.bridge.SceneImageRuntime.StoredImage normalizeForMobile(java.io.File file, com.braseiro.wwn.bridge.SceneImageRuntime.ImageType type, int width, int height) {
        return null;
    }
    
    private final java.lang.String sha256(java.io.File file) {
        return null;
    }
    
    private final com.braseiro.wwn.bridge.SceneImageRuntime.ImageType detectType(byte[] bytes) {
        return null;
    }
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\u0010\u001a\u00020\u0011J\b\u0010\u0012\u001a\u00020\u0011H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\nX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/braseiro/wwn/bridge/SceneImageRuntime$Companion;", "", "<init>", "()V", "LOCAL_HOST", "", "LOCAL_ORIGIN", "MAX_IMAGE_BYTES", "", "MAX_DIMENSION", "", "MAX_PIXELS", "MAX_STORED_DIMENSION", "MAX_STORED_PIXELS", "SAFE_NAME", "Lkotlin/text/Regex;", "blockedResponse", "Landroid/webkit/WebResourceResponse;", "notFoundResponse", "app"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final android.webkit.WebResourceResponse blockedResponse() {
            return null;
        }
        
        private final android.webkit.WebResourceResponse notFoundResponse() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\b\u000b\b\u0082\u0081\u0002\u0018\u00002\b\u0012\u0004\u0012\u00020\u00000\u0001B\'\b\u0002\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00030\u0006\u00a2\u0006\u0004\b\u0007\u0010\bR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\nR\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00030\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010\u00a8\u0006\u0011"}, d2 = {"Lcom/braseiro/wwn/bridge/SceneImageRuntime$ImageType;", "", "mimeType", "", "extension", "acceptedMimeTypes", "", "<init>", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/util/Set;)V", "getMimeType", "()Ljava/lang/String;", "getExtension", "getAcceptedMimeTypes", "()Ljava/util/Set;", "PNG", "JPEG", "WEBP", "app"})
    static enum ImageType {
        /*public static final*/ PNG /* = new PNG(null, null, null) */,
        /*public static final*/ JPEG /* = new JPEG(null, null, null) */,
        /*public static final*/ WEBP /* = new WEBP(null, null, null) */;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String mimeType = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String extension = null;
        @org.jetbrains.annotations.NotNull()
        private final java.util.Set<java.lang.String> acceptedMimeTypes = null;
        
        ImageType(java.lang.String mimeType, java.lang.String extension, java.util.Set<java.lang.String> acceptedMimeTypes) {
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getMimeType() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getExtension() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Set<java.lang.String> getAcceptedMimeTypes() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public static kotlin.enums.EnumEntries<com.braseiro.wwn.bridge.SceneImageRuntime.ImageType> getEntries() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0010\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\b\u0082\b\u0018\u00002\u00020\u0001B\'\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0007\u00a2\u0006\u0004\b\t\u0010\nJ\t\u0010\u0012\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0013\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0007H\u00c6\u0003J1\u0010\u0016\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010\u0017\u001a\u00020\u00182\b\u0010\u0019\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001a\u001a\u00020\u0007H\u00d6\u0001J\t\u0010\u001b\u001a\u00020\u001cH\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0010\u00a8\u0006\u001d"}, d2 = {"Lcom/braseiro/wwn/bridge/SceneImageRuntime$StoredImage;", "", "file", "Ljava/io/File;", "type", "Lcom/braseiro/wwn/bridge/SceneImageRuntime$ImageType;", "width", "", "height", "<init>", "(Ljava/io/File;Lcom/braseiro/wwn/bridge/SceneImageRuntime$ImageType;II)V", "getFile", "()Ljava/io/File;", "getType", "()Lcom/braseiro/wwn/bridge/SceneImageRuntime$ImageType;", "getWidth", "()I", "getHeight", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "toString", "", "app"})
    static final class StoredImage {
        @org.jetbrains.annotations.NotNull()
        private final java.io.File file = null;
        @org.jetbrains.annotations.NotNull()
        private final com.braseiro.wwn.bridge.SceneImageRuntime.ImageType type = null;
        private final int width = 0;
        private final int height = 0;
        
        public StoredImage(@org.jetbrains.annotations.NotNull()
        java.io.File file, @org.jetbrains.annotations.NotNull()
        com.braseiro.wwn.bridge.SceneImageRuntime.ImageType type, int width, int height) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.io.File getFile() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.braseiro.wwn.bridge.SceneImageRuntime.ImageType getType() {
            return null;
        }
        
        public final int getWidth() {
            return 0;
        }
        
        public final int getHeight() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.io.File component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.braseiro.wwn.bridge.SceneImageRuntime.ImageType component2() {
            return null;
        }
        
        public final int component3() {
            return 0;
        }
        
        public final int component4() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.braseiro.wwn.bridge.SceneImageRuntime.StoredImage copy(@org.jetbrains.annotations.NotNull()
        java.io.File file, @org.jetbrains.annotations.NotNull()
        com.braseiro.wwn.bridge.SceneImageRuntime.ImageType type, int width, int height) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
    }
}