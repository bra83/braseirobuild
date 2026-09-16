package com.braseiro.wwn.bridge;

/**
 * Presentation-only audio runtime. Game rules/state remain entirely host-owned.
 */
@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000|\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u0002\n\u0002\b\t\n\u0002\u0010\u0017\n\u0002\b\b\n\u0002\u0010\b\n\u0002\b\u0006\u0018\u0000 G2\u00020\u0001:\u0001GB\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\u0006\u0010%\u001a\u00020&J\u000e\u0010\'\u001a\u00020&2\u0006\u0010(\u001a\u00020\u001cJ\u000e\u0010)\u001a\u00020&2\u0006\u0010*\u001a\u00020\u001cJ\u000e\u0010+\u001a\u00020&2\u0006\u0010,\u001a\u00020#J\u0006\u0010-\u001a\u00020&J\u0006\u0010.\u001a\u00020/J\u0006\u00100\u001a\u00020/J\u0006\u00101\u001a\u00020/J\u0006\u00102\u001a\u00020#J\u0006\u00103\u001a\u00020#J\u0018\u00104\u001a\u00020/2\u0006\u00105\u001a\u00020\u001c2\u0006\u00106\u001a\u00020\u001cH\u0002J \u00107\u001a\u00020/2\u0006\u00105\u001a\u00020\u001c2\u0006\u00106\u001a\u00020\u001c2\u0006\u00108\u001a\u000209H\u0002J\"\u0010:\u001a\u00020/2\b\u0010;\u001a\u0004\u0018\u00010\u00162\u0006\u0010<\u001a\u00020\u00162\u0006\u00105\u001a\u00020\u001cH\u0002J\u0010\u0010=\u001a\u00020/2\u0006\u00106\u001a\u00020\u001cH\u0002J\u0010\u0010>\u001a\u00020/2\u0006\u00108\u001a\u000209H\u0002J \u0010?\u001a\u00020\u00162\u0006\u00108\u001a\u0002092\u0006\u0010@\u001a\u00020#2\u0006\u0010A\u001a\u00020BH\u0002J\b\u0010C\u001a\u00020/H\u0002J\u0010\u0010D\u001a\u00020\u001f2\u0006\u00105\u001a\u00020\u001cH\u0002J\b\u0010E\u001a\u00020\u001fH\u0002J\u0010\u0010F\u001a\u00020/2\u0006\u00105\u001a\u00020\u001cH\u0002R\u0016\u0010\u0006\u001a\n \u0007*\u0004\u0018\u00010\u00030\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\b\u001a\n \u0007*\u0004\u0018\u00010\t0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0011\u001a\n \u0007*\u0004\u0018\u00010\u00120\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0015\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0017\u001a\u0004\u0018\u00010\u0016X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010\u0018\u001a\u0012\u0012\u0004\u0012\u00020\u00160\u0019j\b\u0012\u0004\u0012\u00020\u0016`\u001aX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001b\u001a\u0004\u0018\u00010\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u001d\u001a\u0004\u0018\u00010\u001cX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001e\u001a\u00020\u001fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010 \u001a\u00020\u001fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\u001fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\"\u001a\u00020#X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010$\u001a\u00020#X\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006H"}, d2 = {"Lcom/braseiro/wwn/bridge/NativeAudioRuntime;", "", "context", "Landroid/content/Context;", "<init>", "(Landroid/content/Context;)V", "appContext", "kotlin.jvm.PlatformType", "prefs", "Landroid/content/SharedPreferences;", "main", "Landroid/os/Handler;", "ambienceSequence", "Ljava/util/concurrent/atomic/AtomicLong;", "musicSequence", "ambienceRenderSequence", "musicRenderSequence", "loopSynthesisExecutor", "Ljava/util/concurrent/ExecutorService;", "sfxSynthesisExecutor", "Ljava/util/concurrent/ThreadPoolExecutor;", "ambienceTrack", "Landroid/media/AudioTrack;", "musicTrack", "oneShots", "Ljava/util/LinkedHashSet;", "Lkotlin/collections/LinkedHashSet;", "ambienceAsset", "", "musicAsset", "requestedAmbienceGain", "", "requestedMusicGain", "requestedSfxGain", "narrationActive", "", "pausedForLifecycle", "status", "Lorg/json/JSONObject;", "configure", "requestJson", "applyScenePlan", "planJson", "setNarrationActive", "active", "stopAll", "pauseForLifecycle", "", "resumeFromLifecycle", "shutdown", "npcVoiceEnabled", "masterVoiceEnabled", "requestLoop", "channel", "asset", "installLoop", "pcm", "", "fadeSwap", "old", "next", "requestOneShot", "installOneShot", "createTrack", "looping", "usage", "", "applyVolumes", "layerVolume", "masterVolume", "stopLayer", "Companion", "app"})
public final class NativeAudioRuntime {
    private final android.content.Context appContext = null;
    private final android.content.SharedPreferences prefs = null;
    @org.jetbrains.annotations.NotNull()
    private final android.os.Handler main = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.atomic.AtomicLong ambienceSequence = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.atomic.AtomicLong musicSequence = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.atomic.AtomicLong ambienceRenderSequence = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.atomic.AtomicLong musicRenderSequence = null;
    private final java.util.concurrent.ExecutorService loopSynthesisExecutor = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.concurrent.ThreadPoolExecutor sfxSynthesisExecutor = null;
    @org.jetbrains.annotations.Nullable()
    private android.media.AudioTrack ambienceTrack;
    @org.jetbrains.annotations.Nullable()
    private android.media.AudioTrack musicTrack;
    @org.jetbrains.annotations.NotNull()
    private final java.util.LinkedHashSet<android.media.AudioTrack> oneShots = null;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile java.lang.String ambienceAsset;
    @kotlin.jvm.Volatile()
    @org.jetbrains.annotations.Nullable()
    private volatile java.lang.String musicAsset;
    private float requestedAmbienceGain = 1.0F;
    private float requestedMusicGain = 1.0F;
    private float requestedSfxGain = 1.0F;
    @kotlin.jvm.Volatile()
    private volatile boolean narrationActive = false;
    @kotlin.jvm.Volatile()
    private volatile boolean pausedForLifecycle = false;
    private static final int SAMPLE_RATE = 24000;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS = "wwn_audio_v1";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_AMBIENCE = "ambience_enabled";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_MUSIC = "music_enabled";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_SFX = "sfx_enabled";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_NPC_VOICE = "npc_voice_enabled";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_MASTER_VOICE = "master_voice_enabled";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_VOLUME = "master_volume";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_SAFE_AUDIO_DEFAULTS_APPLIED = "safe_audio_defaults_v2";
    @org.jetbrains.annotations.NotNull()
    public static final com.braseiro.wwn.bridge.NativeAudioRuntime.Companion Companion = null;
    
    public NativeAudioRuntime(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
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
    public final org.json.JSONObject applyScenePlan(@org.jetbrains.annotations.NotNull()
    java.lang.String planJson) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject setNarrationActive(boolean active) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject stopAll() {
        return null;
    }
    
    public final void pauseForLifecycle() {
    }
    
    public final void resumeFromLifecycle() {
    }
    
    public final void shutdown() {
    }
    
    public final boolean npcVoiceEnabled() {
        return false;
    }
    
    public final boolean masterVoiceEnabled() {
        return false;
    }
    
    private final void requestLoop(java.lang.String channel, java.lang.String asset) {
    }
    
    private final void installLoop(java.lang.String channel, java.lang.String asset, short[] pcm) {
    }
    
    private final void fadeSwap(android.media.AudioTrack old, android.media.AudioTrack next, java.lang.String channel) {
    }
    
    private final void requestOneShot(java.lang.String asset) {
    }
    
    private final void installOneShot(short[] pcm) {
    }
    
    private final android.media.AudioTrack createTrack(short[] pcm, boolean looping, int usage) {
        return null;
    }
    
    private final void applyVolumes() {
    }
    
    private final float layerVolume(java.lang.String channel) {
        return 0.0F;
    }
    
    private final float masterVolume() {
        return 0.0F;
    }
    
    private final void stopLayer(java.lang.String channel) {
    }
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\b\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0007X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u000f"}, d2 = {"Lcom/braseiro/wwn/bridge/NativeAudioRuntime$Companion;", "", "<init>", "()V", "SAMPLE_RATE", "", "PREFS", "", "KEY_AMBIENCE", "KEY_MUSIC", "KEY_SFX", "KEY_NPC_VOICE", "KEY_MASTER_VOICE", "KEY_VOLUME", "KEY_SAFE_AUDIO_DEFAULTS_APPLIED", "app"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}