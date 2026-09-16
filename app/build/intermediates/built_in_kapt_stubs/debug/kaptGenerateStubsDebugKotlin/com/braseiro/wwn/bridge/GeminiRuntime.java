package com.braseiro.wwn.bridge;

@kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000p\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0010\u0012\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 H2\u00020\u0001:\u0002GHB\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\u0006\u0010\t\u001a\u00020\nJ\u0018\u0010\u000b\u001a\u00020\n2\u0006\u0010\f\u001a\u00020\r2\b\u0010\u000e\u001a\u0004\u0018\u00010\rJ\u000e\u0010\u000f\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\rJ\u000e\u0010\u0010\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\rJ\"\u0010\u0011\u001a\u00020\n2\u0006\u0010\u000e\u001a\u00020\r2\u0006\u0010\u0012\u001a\u00020\r2\n\b\u0002\u0010\u0013\u001a\u0004\u0018\u00010\rJ\u000e\u0010\u0014\u001a\u00020\n2\u0006\u0010\u0015\u001a\u00020\u0016J\u0006\u0010\u0017\u001a\u00020\nJ\u000e\u0010\u0018\u001a\u00020\n2\u0006\u0010\u0019\u001a\u00020\rJ\u000e\u0010\u001a\u001a\u00020\n2\u0006\u0010\u0019\u001a\u00020\rJ2\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\r2\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\r2\n\b\u0002\u0010\u0012\u001a\u0004\u0018\u00010\r2\n\b\u0002\u0010\u001e\u001a\u0004\u0018\u00010\rJ\u0006\u0010\u001f\u001a\u00020\rJ\u0006\u0010 \u001a\u00020\rJ\u0006\u0010!\u001a\u00020\rJ\u0006\u0010\"\u001a\u00020\rJ(\u0010#\u001a\u00020\n2\u0006\u0010$\u001a\u00020\r2\u0006\u0010%\u001a\u00020\r2\u0006\u0010&\u001a\u00020\n2\u0006\u0010\'\u001a\u00020(H\u0002J\u0018\u0010)\u001a\u00020*2\u0006\u0010+\u001a\u00020,2\u0006\u0010%\u001a\u00020\rH\u0002J\u0012\u0010-\u001a\u0004\u0018\u00010\n2\u0006\u0010.\u001a\u00020\rH\u0002J\u0012\u0010/\u001a\u0004\u0018\u00010\n2\u0006\u0010.\u001a\u00020\rH\u0002J\u001c\u00100\u001a\u000e\u0012\u0004\u0012\u00020\r\u0012\u0004\u0012\u000202012\u0006\u00103\u001a\u000204H\u0002J\u0010\u00105\u001a\u00020\r2\u0006\u00106\u001a\u00020\nH\u0002J\u0018\u00107\u001a\u00020\r2\u0006\u0010\t\u001a\u00020(2\u0006\u00108\u001a\u00020\rH\u0002J\b\u00109\u001a\u00020\rH\u0002J\u0018\u0010:\u001a\u00020\r2\u0006\u0010;\u001a\u00020\r2\u0006\u0010<\u001a\u00020\rH\u0002J\u0010\u0010=\u001a\u00020\r2\u0006\u0010;\u001a\u00020\rH\u0002J\u0010\u0010>\u001a\u00020\r2\u0006\u0010;\u001a\u00020\rH\u0002J\b\u0010?\u001a\u00020@H\u0002J\u0010\u0010A\u001a\u00020\r2\u0006\u0010;\u001a\u00020\rH\u0002J\b\u0010B\u001a\u00020\rH\u0002J\u0018\u0010C\u001a\u00020\r2\u0006\u0010D\u001a\u00020E2\u0006\u0010\'\u001a\u00020(H\u0002J\u0018\u0010F\u001a\u0002022\u0006\u0010D\u001a\u00020E2\u0006\u0010\'\u001a\u00020(H\u0002R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u0006\u001a\n \b*\u0004\u0018\u00010\u00070\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006I"}, d2 = {"Lcom/braseiro/wwn/bridge/GeminiRuntime;", "", "context", "Landroid/content/Context;", "<init>", "(Landroid/content/Context;)V", "preferences", "Landroid/content/SharedPreferences;", "kotlin.jvm.PlatformType", "status", "Lorg/json/JSONObject;", "setApiKey", "apiKey", "", "requestedModel", "setModel", "setImageModel", "setTtsConfig", "requestedVoice", "requestedProvider", "setVisionEnabled", "enabled", "", "clearApiKey", "generateText", "requestJson", "generateImage", "generateSpeech", "Lcom/braseiro/wwn/bridge/GeminiRuntime$SpeechPayload;", "text", "style", "configuredImageModel", "configuredTtsProvider", "configuredTtsModel", "configuredTtsVoice", "postGenerateContent", "model", "key", "body", "maxBytes", "", "configureConnection", "", "connection", "Ljava/net/HttpURLConnection;", "resolveSceneImagePart", "rawUrl", "resolveInteractionImageContent", "prepareImageForGemini", "Lkotlin/Pair;", "", "file", "Ljava/io/File;", "extractText", "response", "safeHttpReason", "responseBody", "configuredTextModel", "sanitizeModel", "value", "fallback", "sanitizeVoice", "sanitizeTtsProvider", "secretKey", "Ljavax/crypto/SecretKey;", "encrypt", "readSecret", "readBounded", "stream", "Ljava/io/InputStream;", "readBoundedBytes", "SpeechPayload", "Companion", "app"})
public final class GeminiRuntime {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    private final android.content.SharedPreferences preferences = null;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String DEFAULT_TEXT_MODEL = "gemini-3.8-flash";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String DEFAULT_IMAGE_MODEL = "gemini-3.1-flash-image";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String DEFAULT_TTS_MODEL = "gemini-3.1-flash-tts-preview";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String DEFAULT_TTS_VOICE = "Charon";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String DEFAULT_TTS_PROVIDER = "GEMINI";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREFS_NAME = "braseiro_wwn_secure_runtime_v1";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREF_KEY_SECRET = "gemini_api_key_encrypted";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREF_KEY_MODEL = "gemini_text_model";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREF_KEY_IMAGE_MODEL = "gemini_image_model";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREF_KEY_TTS_MODEL = "gemini_tts_model";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREF_KEY_TTS_VOICE = "gemini_tts_voice";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREF_KEY_TTS_PROVIDER = "gemini_tts_provider";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String PREF_KEY_VISION = "gemini_scene_vision_enabled";
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_ALIAS = "braseiro_wwn_gemini_secret_v1";
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex MODEL_PATTERN = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex SCENE_IMAGE_NAME = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Set<java.lang.String> VOICES = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Set<java.lang.String> IMAGE_ASPECT_RATIOS = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.util.Set<java.lang.String> IMAGE_SIZES = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.text.Regex INTERACTION_ID_PATTERN = null;
    private static final int MAX_REQUEST_JSON_CHARS = 260000;
    private static final int MAX_PROMPT_CHARS = 180000;
    private static final int MAX_SYSTEM_CHARS = 20000;
    private static final int MAX_TEXT_RESPONSE_BYTES = 2000000;
    private static final int MAX_AUDIO_RESPONSE_BYTES = 24000000;
    private static final int MAX_RETURNED_TEXT_CHARS = 60000;
    private static final int MAX_TTS_TEXT_CHARS = 1200;
    private static final int MAX_IMAGE_PROMPT_CHARS = 20000;
    private static final int MAX_REFERENCE_IMAGES = 5;
    private static final int MAX_IMAGE_RESPONSE_BYTES = 24000000;
    private static final int MAX_VISION_DIMENSION = 1536;
    private static final int MAX_VISION_BYTES = 3000000;
    private static final int CONNECT_TIMEOUT_MS = 20000;
    private static final int READ_TIMEOUT_MS = 120000;
    private static final int IMAGE_READ_TIMEOUT_MS = 180000;
    @org.jetbrains.annotations.NotNull()
    public static final com.braseiro.wwn.bridge.GeminiRuntime.Companion Companion = null;
    
    public GeminiRuntime(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject status() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject setApiKey(@org.jetbrains.annotations.NotNull()
    java.lang.String apiKey, @org.jetbrains.annotations.Nullable()
    java.lang.String requestedModel) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject setModel(@org.jetbrains.annotations.NotNull()
    java.lang.String requestedModel) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject setImageModel(@org.jetbrains.annotations.NotNull()
    java.lang.String requestedModel) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject setTtsConfig(@org.jetbrains.annotations.NotNull()
    java.lang.String requestedModel, @org.jetbrains.annotations.NotNull()
    java.lang.String requestedVoice, @org.jetbrains.annotations.Nullable()
    java.lang.String requestedProvider) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject setVisionEnabled(boolean enabled) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject clearApiKey() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject generateText(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final org.json.JSONObject generateImage(@org.jetbrains.annotations.NotNull()
    java.lang.String requestJson) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.braseiro.wwn.bridge.GeminiRuntime.SpeechPayload generateSpeech(@org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.Nullable()
    java.lang.String requestedModel, @org.jetbrains.annotations.Nullable()
    java.lang.String requestedVoice, @org.jetbrains.annotations.Nullable()
    java.lang.String style) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String configuredImageModel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String configuredTtsProvider() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String configuredTtsModel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String configuredTtsVoice() {
        return null;
    }
    
    private final org.json.JSONObject postGenerateContent(java.lang.String model, java.lang.String key, org.json.JSONObject body, int maxBytes) {
        return null;
    }
    
    private final void configureConnection(java.net.HttpURLConnection connection, java.lang.String key) {
    }
    
    private final org.json.JSONObject resolveSceneImagePart(java.lang.String rawUrl) {
        return null;
    }
    
    private final org.json.JSONObject resolveInteractionImageContent(java.lang.String rawUrl) {
        return null;
    }
    
    private final kotlin.Pair<java.lang.String, byte[]> prepareImageForGemini(java.io.File file) {
        return null;
    }
    
    private final java.lang.String extractText(org.json.JSONObject response) {
        return null;
    }
    
    private final java.lang.String safeHttpReason(int status, java.lang.String responseBody) {
        return null;
    }
    
    private final java.lang.String configuredTextModel() {
        return null;
    }
    
    private final java.lang.String sanitizeModel(java.lang.String value, java.lang.String fallback) {
        return null;
    }
    
    private final java.lang.String sanitizeVoice(java.lang.String value) {
        return null;
    }
    
    private final java.lang.String sanitizeTtsProvider(java.lang.String value) {
        return null;
    }
    
    private final javax.crypto.SecretKey secretKey() {
        return null;
    }
    
    private final java.lang.String encrypt(java.lang.String value) {
        return null;
    }
    
    private final java.lang.String readSecret() {
        return null;
    }
    
    private final java.lang.String readBounded(java.io.InputStream stream, int maxBytes) {
        return null;
    }
    
    private final byte[] readBoundedBytes(java.io.InputStream stream, int maxBytes) {
        return null;
    }
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u000e\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\"\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u000f\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u000e\u0010\u0004\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0005X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000e\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00050\u0017\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0014\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u00050\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u001b\u001a\b\u0012\u0004\u0012\u00020\u00050\u0017X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001c\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001d\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u001f\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010 \u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010!\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\"\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010#\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010$\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010%\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010&\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\'\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010(\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010)\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010*\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010+\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010,\u001a\u00020\u001eX\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006-"}, d2 = {"Lcom/braseiro/wwn/bridge/GeminiRuntime$Companion;", "", "<init>", "()V", "DEFAULT_TEXT_MODEL", "", "DEFAULT_IMAGE_MODEL", "DEFAULT_TTS_MODEL", "DEFAULT_TTS_VOICE", "DEFAULT_TTS_PROVIDER", "PREFS_NAME", "PREF_KEY_SECRET", "PREF_KEY_MODEL", "PREF_KEY_IMAGE_MODEL", "PREF_KEY_TTS_MODEL", "PREF_KEY_TTS_VOICE", "PREF_KEY_TTS_PROVIDER", "PREF_KEY_VISION", "KEY_ALIAS", "MODEL_PATTERN", "Lkotlin/text/Regex;", "SCENE_IMAGE_NAME", "VOICES", "", "getVOICES", "()Ljava/util/Set;", "IMAGE_ASPECT_RATIOS", "IMAGE_SIZES", "INTERACTION_ID_PATTERN", "MAX_REQUEST_JSON_CHARS", "", "MAX_PROMPT_CHARS", "MAX_SYSTEM_CHARS", "MAX_TEXT_RESPONSE_BYTES", "MAX_AUDIO_RESPONSE_BYTES", "MAX_RETURNED_TEXT_CHARS", "MAX_TTS_TEXT_CHARS", "MAX_IMAGE_PROMPT_CHARS", "MAX_REFERENCE_IMAGES", "MAX_IMAGE_RESPONSE_BYTES", "MAX_VISION_DIMENSION", "MAX_VISION_BYTES", "CONNECT_TIMEOUT_MS", "READ_TIMEOUT_MS", "IMAGE_READ_TIMEOUT_MS", "app"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.Set<java.lang.String> getVOICES() {
            return null;
        }
    }
    
    @kotlin.Metadata(mv = {2, 2, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0012\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0010\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\'\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u0012\u0006\u0010\u0007\u001a\u00020\u0005\u00a2\u0006\u0004\b\b\u0010\tJ\t\u0010\u0010\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0011\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0013\u001a\u00020\u0005H\u00c6\u0003J1\u0010\u0014\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u0015\u001a\u00020\u00162\b\u0010\u0017\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0018\u001a\u00020\u0019H\u00d6\u0001J\t\u0010\u001a\u001a\u00020\u0005H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u0011\u0010\u0007\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\r\u00a8\u0006\u001b"}, d2 = {"Lcom/braseiro/wwn/bridge/GeminiRuntime$SpeechPayload;", "", "pcm", "", "model", "", "voice", "mimeType", "<init>", "([BLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", "getPcm", "()[B", "getModel", "()Ljava/lang/String;", "getVoice", "getMimeType", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "", "toString", "app"})
    public static final class SpeechPayload {
        @org.jetbrains.annotations.NotNull()
        private final byte[] pcm = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String model = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String voice = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String mimeType = null;
        
        public SpeechPayload(@org.jetbrains.annotations.NotNull()
        byte[] pcm, @org.jetbrains.annotations.NotNull()
        java.lang.String model, @org.jetbrains.annotations.NotNull()
        java.lang.String voice, @org.jetbrains.annotations.NotNull()
        java.lang.String mimeType) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final byte[] getPcm() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getModel() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getVoice() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getMimeType() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final byte[] component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.braseiro.wwn.bridge.GeminiRuntime.SpeechPayload copy(@org.jetbrains.annotations.NotNull()
        byte[] pcm, @org.jetbrains.annotations.NotNull()
        java.lang.String model, @org.jetbrains.annotations.NotNull()
        java.lang.String voice, @org.jetbrains.annotations.NotNull()
        java.lang.String mimeType) {
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