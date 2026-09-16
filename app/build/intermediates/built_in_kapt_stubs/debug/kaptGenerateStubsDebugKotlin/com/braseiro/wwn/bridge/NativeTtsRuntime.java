package com.braseiro.wwn.bridge;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u0086\u0001\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\r\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u0000 D2\u00020\u0001:\u0001DBC\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0014\b\u0002\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\t0\u0007\u0012\u0014\b\u0002\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\t0\u0007\u00a2\u0006\u0004\b\f\u0010\rJ\u0006\u0010%\u001a\u00020\u000bJ\u000e\u0010&\u001a\u00020\u000b2\u0006\u0010\'\u001a\u00020\u0014J\u000e\u0010(\u001a\u00020\u000b2\u0006\u0010\'\u001a\u00020\u0014J \u0010)\u001a\u00020\u000b2\u0006\u0010*\u001a\u00020\u000b2\u0006\u0010+\u001a\u00020\u00142\u0006\u0010,\u001a\u00020\u0014H\u0002J \u0010-\u001a\u00020\t2\u0006\u0010.\u001a\u00020/2\u0006\u00100\u001a\u00020\u00142\u0006\u00101\u001a\u000202H\u0002J,\u00103\u001a\u00020\t2\u0006\u00100\u001a\u00020\u00142\u0006\u00104\u001a\u00020\"2\u0006\u0010%\u001a\u00020\u00142\n\b\u0002\u00105\u001a\u0004\u0018\u00010\u0014H\u0002J\u0006\u00106\u001a\u00020\u000bJ\u0010\u00107\u001a\u00020\t2\u0006\u00108\u001a\u00020\bH\u0002J\u0006\u00109\u001a\u00020\tJ&\u0010:\u001a\u00020\t2\b\u00100\u001a\u0004\u0018\u00010\u00142\u0006\u0010%\u001a\u00020\u00142\n\b\u0002\u00105\u001a\u0004\u0018\u00010\u0014H\u0002J.\u0010;\u001a\u00020\t2\u0006\u0010%\u001a\u00020\u00142\b\u00100\u001a\u0004\u0018\u00010\u00142\u0006\u0010<\u001a\u00020\u00142\n\b\u0002\u00105\u001a\u0004\u0018\u00010\u0014H\u0002J\u0010\u0010=\u001a\u00020\u00142\u0006\u0010,\u001a\u00020\u0014H\u0002J0\u0010>\u001a\u00020\t2\u0006\u0010?\u001a\u00020@2\u0006\u0010.\u001a\u00020/2\u0006\u0010A\u001a\u00020\u001b2\u0006\u0010B\u001a\u00020\u001b2\u0006\u0010C\u001a\u00020\u001bH\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\b\u0012\u0004\u0012\u00020\t0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\t0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0010\u001a\n \u0011*\u0004\u0018\u00010\u00030\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000RN\u0010\u0012\u001aB\u0012\f\u0012\n \u0011*\u0004\u0018\u00010\u00140\u0014\u0012\f\u0012\n \u0011*\u0004\u0018\u00010\b0\b \u0011* \u0012\f\u0012\n \u0011*\u0004\u0018\u00010\u00140\u0014\u0012\f\u0012\n \u0011*\u0004\u0018\u00010\b0\b\u0018\u00010\u00130\u0013X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0015\u001a\n \u0011*\u0004\u0018\u00010\u00160\u0016X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0018X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001a\u001a\u00020\u001bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001c\u001a\u0004\u0018\u00010\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001d\u001a\u0004\u0018\u00010\u001eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001f\u001a\u0004\u0018\u00010 X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010!\u001a\u0004\u0018\u00010\"X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010#\u001a\u0004\u0018\u00010$X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006E"}, d2 = {"Lcom/braseiro/wwn/bridge/NativeTtsRuntime;", "", "context", "Landroid/content/Context;", "geminiRuntime", "Lcom/braseiro/wwn/bridge/GeminiRuntime;", "onSpeakingChanged", "Lkotlin/Function1;", "", "", "onTtsEvent", "Lorg/json/JSONObject;", "<init>", "(Landroid/content/Context;Lcom/braseiro/wwn/bridge/GeminiRuntime;Lkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;)V", "mainHandler", "Landroid/os/Handler;", "appContext", "kotlin.jvm.PlatformType", "pendingUtterances", "Ljava/util/concurrent/ConcurrentHashMap$KeySetView;", "", "geminiExecutor", "Ljava/util/concurrent/ExecutorService;", "generation", "Ljava/util/concurrent/atomic/AtomicLong;", "state", "languageStatus", "", "activeSessionId", "activePlayer", "Landroid/media/MediaPlayer;", "activeWav", "Ljava/io/File;", "activePlaybackLatch", "Ljava/util/concurrent/CountDownLatch;", "engine", "Landroid/speech/tts/TextToSpeech;", "status", "configure", "requestJson", "speak", "speakAndroidFallback", "request", "sessionId", "role", "playGeminiPcmBlocking", "pcm", "", "utteranceId", "token", "", "finishGeminiPlayback", "latch", "reason", "stop", "stopInternal", "incrementGeneration", "shutdown", "completeAndroid", "emitEvent", "provider", "defaultStyleForRole", "writeWav", "output", "Ljava/io/FileOutputStream;", "sampleRate", "channels", "bitsPerSample", "Companion", "app"})
public final class NativeTtsRuntime {
    @org.jetbrains.annotations.NotNull()
    private final com.braseiro.wwn.bridge.GeminiRuntime geminiRuntime = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<java.lang.Boolean, kotlin.Unit> onSpeakingChanged = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.jvm.functions.Function1<org.json.JSONObject, kotlin.Unit> onTtsEvent = null;
    @org.jetbrains.annotations.NotNull()
    private final android.os.Handler mainHandler = null;
    private final android.content.Context appContext = null;
    private final java.util.concurrent.ConcurrentHashMap.KeySetView<java.lang.String, java.lang.Boolean> pendingUtterances = null;
    private final java.util.concurrent.ExecutorService geminiExecutor = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.atomic.AtomicLong generation = null;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.NotNull()
    private volatile java.lang.String state = "INITIALIZING";
    @kotlin.jvm.Volatile()
    private volatile int languageStatus = android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile java.lang.String activeSessionId;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile android.media.MediaPlayer activePlayer;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile java.io.File activeWav;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile java.util.concurrent.CountDownLatch activePlaybackLatch;
    @org.jetbrains.annotations.Nullable()
    private android.speech.tts.TextToSpeech engine;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Locale PT_BR = null;
    private static final int MAX_TEXT_CHARS = 1200;
    @org.jetbrains.annotations.NotNull()
    public static final com.braseiro.wwn.bridge.NativeTtsRuntime.Companion Companion = null;
    
    public NativeTtsRuntime(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.braseiro.wwn.bridge.GeminiRuntime geminiRuntime, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.Boolean, kotlin.Unit> onSpeakingChanged, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super org.json.JSONObject, kotlin.Unit> onTtsEvent) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject status() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject configure(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject speak(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    private final org.json.JSONObject speakAndroidFallback(org.json.JSONObject request, java.lang.String sessionId, java.lang.String role) {
        return null;
    }
    
    private final void playGeminiPcmBlocking(byte[] pcm, java.lang.String utteranceId, long token) {
    }
    
    private final void finishGeminiPlayback(java.lang.String utteranceId, java.util.concurrent.CountDownLatch latch, java.lang.String status, java.lang.String reason) {
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject stop() {
        return null;
    }
    
    private final void stopInternal(boolean incrementGeneration) {
    }
    
    public final void shutdown() {
    }
    
    private final void completeAndroid(java.lang.String utteranceId, java.lang.String status, java.lang.String reason) {
    }
    
    private final void emitEvent(java.lang.String status, java.lang.String utteranceId, java.lang.String provider, java.lang.String reason) {
    }
    
    private final java.lang.String defaultStyleForRole(java.lang.String role) {
        return null;
    }
    
    private final void writeWav(java.io.FileOutputStream output, byte[] pcm, int sampleRate, int channels, int bitsPerSample) {
    }
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\b"}, d2 = {"Lcom/braseiro/wwn/bridge/NativeTtsRuntime$Companion;", "", "<init>", "()V", "PT_BR", "Ljava/util/Locale;", "MAX_TEXT_CHARS", "", "app"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}