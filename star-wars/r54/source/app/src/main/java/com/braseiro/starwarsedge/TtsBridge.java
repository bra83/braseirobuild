package com.braseiro.starwarsedge;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Android TTS bridge with a dedicated slow-neural Sherpa/Kokoro flow. */
public final class TtsBridge implements TextToSpeech.OnInitListener {
    private static final int MAX_TEXT_CHARS = 12000;
    private static final int TARGET_ANDROID_CHUNK_CHARS = 2800;
    private static final String PREFS = "braseiro_tts_bridge_v2";
    private static final String PREF_ENGINE = "engine_package";
    private static final String PREF_VOICE = "voice_name";
    private static final String LEGACY_BRASEIRO_ENGINE = "com.braseiro.kokoro.ptbr";
    private static final String SHERPA_ENGINE = "com.k2fsa.sherpa.onnx.tts.engine";
    private static final String SHERPA_ENGINE_DEBUG = "com.k2fsa.sherpa.onnx.tts.engine.debug";
    private static final String BRASEIRO_KOKORO_LABEL = "Braseiro Kokoro PT-BR · neural";

    private final Activity activity;
    private final Context appContext;
    private final SharedPreferences preferences;
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();
    private final Object speechLock = new Object();
    private final Object catalogLock = new Object();
    private final AtomicInteger engineGeneration = new AtomicInteger(0);
    private final AtomicInteger catalogGeneration = new AtomicInteger(0);
    private final ArrayDeque<String> neuralQueue = new ArrayDeque<>();
    private final List<EngineRecord> engineCatalog = new ArrayList<>();

    private volatile TextToSpeech tts;
    private volatile String detail = "Inicializando TTS.";
    private volatile String pendingText = null;
    private volatile float pendingRate = 1.0f;
    private volatile float neuralRate = 1.0f;
    private volatile String activeLanguageTag = "pt-BR";
    private volatile String activeEnginePackage = "";
    private volatile String activeEngineLabel = "Android TTS";
    private volatile String activeVoiceName = "";
    private volatile String activeProfile = "android";
    private volatile String systemDefaultEngine = "";
    private volatile String requestedEnginePackage = "";
    private volatile long speechSession = 0L;
    private volatile boolean neuralSpeaking = false;
    private volatile String lastPrewarmKey = "";

    public TtsBridge(Activity activity) {
        this.activity = activity;
        this.appContext = activity.getApplicationContext();
        this.preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String savedEngine = preferences.getString(PREF_ENGINE, "");
        String migratedEngine = migrateLegacyEnginePreference(savedEngine);
        String savedVoice = preferences.getString(PREF_VOICE, "");
        boolean migrated = !normalize(savedEngine).equals(normalize(migratedEngine));
        activeVoiceName = migrated ? "" : (savedVoice == null ? "" : savedVoice);
        if (migrated) preferences.edit().putString(PREF_ENGINE, migratedEngine).putString(PREF_VOICE, "").apply();
        activity.runOnUiThread(() -> switchEngineInternal(migratedEngine, false));
    }

    @Override
    public void onInit(int status) {
        synchronized (lifecycleLock) {
            if (closed.get()) return;
        }
        onEngineInit(status, engineGeneration.get());
    }

    @JavascriptInterface
    public String getStatus() {
        try {
            TextToSpeech local = tts;
            return new JSONObject()
                    .put("ok", true)
                    .put("available", ready.get())
                    .put("language", activeLanguageTag)
                    .put("enginePackage", activeEnginePackage)
                    .put("engineLabel", activeEngineLabel)
                    .put("voiceName", activeVoiceName)
                    .put("profile", activeProfile)
                    .put("neural", "kokoro".equals(activeProfile))
                    .put("speaking", local != null && (local.isSpeaking() || neuralSpeaking))
                    .put("queuedDuringInit", pendingText != null)
                    .put("androidMaxChunkChars", androidChunkLimit())
                    .put("neuralTargetChars", TtsChunker.NEURAL_TARGET)
                    .put("neuralMinChars", TtsChunker.NEURAL_MIN)
                    .put("neuralMaxChars", TtsChunker.NEURAL_MAX)
                    .put("detail", detail)
                    .toString();
        } catch (Exception e) {
            return "{\"ok\":false,\"available\":false,\"detail\":\"Falha ao consultar TTS.\"}";
        }
    }

    /** All installed engines plus voices discovered by opening each engine briefly. */
    @JavascriptInterface
    public String getCatalog() {
        try {
            JSONArray engines = new JSONArray();
            synchronized (catalogLock) {
                for (EngineRecord record : engineCatalog) engines.put(record.toJson());
            }
            return new JSONObject()
                    .put("ok", true)
                    .put("activeEnginePackage", activeEnginePackage)
                    .put("activeVoiceName", activeVoiceName)
                    .put("systemDefaultEngine", systemDefaultEngine)
                    .put("engines", engines)
                    .toString();
        } catch (Exception e) {
            return fail("TTS_CATALOG_FAILED", "Não foi possível listar os motores TTS instalados.");
        }
    }

    @JavascriptInterface
    public String refreshCatalog() {
        if (closed.get()) return fail("TTS_CLOSED", "Motor TTS encerrado.");
        activity.runOnUiThread(this::refreshEngineCatalog);
        return "{\"ok\":true,\"status\":\"refreshing\"}";
    }

    @JavascriptInterface
    public String selectEngine(String rawPackage) {
        if (closed.get()) return fail("TTS_CLOSED", "Motor TTS encerrado.");
        String requested = migrateLegacyEnginePreference(rawPackage);
        if (!requested.isEmpty() && !isInstalledEngine(requested)) {
            return fail("TTS_ENGINE_NOT_INSTALLED", "O motor TTS selecionado não está instalado.");
        }
        preferences.edit().putString(PREF_ENGINE, requested).putString(PREF_VOICE, "").apply();
        activeVoiceName = "";
        activity.runOnUiThread(() -> switchEngineInternal(requested, true));
        try {
            return new JSONObject().put("ok", true).put("status", "initializing").put("enginePackage", requested).toString();
        } catch (Exception e) { return "{\"ok\":true}"; }
    }

    @JavascriptInterface
    public String selectVoice(String rawVoiceName) {
        if (closed.get()) return fail("TTS_CLOSED", "Motor TTS encerrado.");
        if (!ready.get() || tts == null) return fail("TTS_UNAVAILABLE", detail);
        String requested = normalize(rawVoiceName);
        final Voice requestedVoice = requested.isEmpty() ? null : findVoiceByName(tts, requested);
        if (!requested.isEmpty() && requestedVoice == null) {
            return fail("TTS_VOICE_NOT_FOUND", "A voz selecionada não pertence ao motor ativo.");
        }
        activity.runOnUiThread(() -> {
            TextToSpeech local = tts;
            if (local == null || closed.get()) return;
            boolean applied;
            if (requestedVoice == null) {
                int language = setPreferredPortugueseLanguage(local);
                applied = isLanguageSupported(language);
                if (applied) activeVoiceName = currentVoiceName(local);
            } else {
                applied = local.setVoice(requestedVoice) == TextToSpeech.SUCCESS;
                if (applied) {
                    activeVoiceName = requestedVoice.getName();
                    Locale locale = requestedVoice.getLocale();
                    if (locale != null) activeLanguageTag = locale.toLanguageTag();
                }
            }
            if (applied) {
                preferences.edit().putString(PREF_VOICE, activeVoiceName).apply();
                activeProfile = detectProfile(activeEnginePackage, activeEngineLabel, activeVoiceName);
                lastPrewarmKey = "";
                detail = statusReadyText();
                if ("kokoro".equals(activeProfile)) prewarmInternal();
            }
        });
        try {
            return new JSONObject().put("ok", true).put("status", "applying")
                    .put("voiceName", requested).put("profile", detectProfile(activeEnginePackage, activeEngineLabel, requested)).toString();
        } catch (Exception e) { return "{\"ok\":true}"; }
    }

    @JavascriptInterface
    public String speak(String rawText, String rawRate) {
        String visibleText = rawText == null ? "" : rawText.trim();
        if (visibleText.isEmpty()) return fail("INVALID_INPUT", "Não há narração visível para ler.");
        if (closed.get()) return fail("TTS_CLOSED", "Motor TTS encerrado.");

        String audioText = capAudioText(TtsChunker.normalizeForAudio(visibleText), MAX_TEXT_CHARS);
        float rate = parseRate(rawRate);
        final String finalText = audioText;
        final float finalRate = rate;

        if (!ready.get()) {
            synchronized (lifecycleLock) {
                if (closed.get()) return fail("TTS_CLOSED", "Motor TTS encerrado.");
                if (detail.startsWith("Inicializando")) {
                    pendingText = finalText;
                    pendingRate = finalRate;
                    return ok(finalText, finalRate, "queued_initializing");
                }
            }
            return fail("TTS_UNAVAILABLE", detail);
        }

        activity.runOnUiThread(() -> {
            if (closed.get()) return;
            if ("kokoro".equals(activeProfile)) startNeuralSequence(finalText, finalRate);
            else queueAndroidSpeech(finalText, finalRate);
        });
        return ok(finalText, finalRate, "started");
    }

    /** Neural-only prewarm without audible playback. */
    @JavascriptInterface
    public String prewarm() {
        if (closed.get()) return fail("TTS_CLOSED", "Motor TTS encerrado.");
        if (!ready.get()) return fail("TTS_UNAVAILABLE", detail);
        if (!"kokoro".equals(activeProfile)) return "{\"ok\":true,\"status\":\"not_required\"}";
        activity.runOnUiThread(this::prewarmInternal);
        return "{\"ok\":true,\"status\":\"scheduled\"}";
    }

    @JavascriptInterface
    public String stop() {
        if (closed.get()) return fail("TTS_CLOSED", "Motor TTS encerrado.");
        pendingText = null;
        synchronized (speechLock) {
            speechSession++;
            neuralQueue.clear();
            neuralSpeaking = false;
        }
        activity.runOnUiThread(() -> {
            if (tts == null) return;
            if (!closed.get()) tts.stop();
        });
        return "{\"ok\":true}";
    }

    public void shutdown() {
        synchronized (lifecycleLock) {
            if (!closed.compareAndSet(false, true)) return;
            ready.set(false);
            pendingText = null;
            detail = "Motor TTS encerrado.";
            synchronized (speechLock) {
                speechSession++;
                neuralQueue.clear();
                neuralSpeaking = false;
            }
            if (tts != null) {
                try { tts.stop(); } catch (Exception ignored) { }
                try { tts.shutdown(); } catch (Exception ignored) { }
                tts = null;
            }
        }
    }

    private void switchEngineInternal(String requestedPackage, boolean userInitiated) {
        if (closed.get()) return;
        final int generation = engineGeneration.incrementAndGet();
        ready.set(false);
        detail = userInitiated ? "Inicializando motor TTS selecionado." : "Inicializando TTS.";
        lastPrewarmKey = "";
        synchronized (speechLock) {
            speechSession++;
            neuralQueue.clear();
            neuralSpeaking = false;
        }

        TextToSpeech old = tts;
        tts = null;
        if (old != null) {
            try { old.stop(); } catch (Exception ignored) { }
            try { old.shutdown(); } catch (Exception ignored) { }
        }

        String requested = normalize(requestedPackage);
        requestedEnginePackage = requested;
        tts = requested.isEmpty()
                ? new TextToSpeech(appContext, status -> onEngineInit(status, generation))
                : new TextToSpeech(appContext, status -> onEngineInit(status, generation), requested);
    }

    private void onEngineInit(int status, int generation) {
        String queued = null;
        float queuedRate = 1.0f;
        synchronized (lifecycleLock) {
            if (closed.get() || generation != engineGeneration.get()) return;
            TextToSpeech local = tts;
            if (local == null) return;
            if (status != TextToSpeech.SUCCESS) {
                detail = "Motor TTS do Android indisponível.";
                ready.set(false);
                pendingText = null;
                return;
            }

            local.setOnUtteranceProgressListener(new BridgeUtteranceListener(generation));
            try { systemDefaultEngine = normalize(local.getDefaultEngine()); }
            catch (Exception ignored) { systemDefaultEngine = ""; }
            // TextToSpeech has no public getCurrentEngine() API. Track the exact package
            // used to construct this instance; when the default constructor was used,
            // the public getDefaultEngine() value is the active engine by definition.
            String requestedEngine = normalize(requestedEnginePackage);
            activeEnginePackage = requestedEngine.isEmpty() ? systemDefaultEngine : requestedEngine;
            activeEngineLabel = engineLabel(activeEnginePackage);

            int language = setPreferredPortugueseLanguage(local);
            boolean supported = isLanguageSupported(language);
            String savedVoice = preferences.getString(PREF_VOICE, "");
            if (savedVoice != null && !savedVoice.isBlank()) {
                Voice voice = findVoiceByName(local, savedVoice);
                if (voice != null && local.setVoice(voice) == TextToSpeech.SUCCESS) {
                    activeVoiceName = voice.getName();
                    if (voice.getLocale() != null) activeLanguageTag = voice.getLocale().toLanguageTag();
                    supported = true;
                } else {
                    activeVoiceName = currentVoiceName(local);
                    preferences.edit().putString(PREF_VOICE, activeVoiceName).apply();
                }
            } else {
                activeVoiceName = currentVoiceName(local);
            }

            activeProfile = detectProfile(activeEnginePackage, activeEngineLabel, activeVoiceName);
            ready.set(supported);
            detail = supported ? statusReadyText() : "Voz em português não instalada no motor selecionado.";
            if (supported && pendingText != null && !pendingText.isBlank()) {
                queued = pendingText;
                queuedRate = pendingRate;
                pendingText = null;
            }
        }

        refreshEngineCatalog();
        if (ready.get() && "kokoro".equals(activeProfile)) prewarmInternal();
        if (queued != null) {
            final String text = queued;
            final float rate = queuedRate;
            activity.runOnUiThread(() -> {
                if ("kokoro".equals(activeProfile)) startNeuralSequence(text, rate);
                else queueAndroidSpeech(text, rate);
            });
        }
    }

    private void queueAndroidSpeech(String text, float rate) {
        TextToSpeech local = tts;
        if (local == null || closed.get() || !ready.get()) return;
        List<String> chunks = splitForSpeech(text);
        if (chunks.isEmpty()) return;

        // Google/Samsung/other Android engines retain the previous behavior.
        local.stop();
        local.setSpeechRate(rate);
        String session = "braseiro-android-" + UUID.randomUUID();
        for (int i = 0; i < chunks.size(); i++) {
            if (closed.get()) return;
            local.speak(chunks.get(i), i==0?TextToSpeech.QUEUE_FLUSH:TextToSpeech.QUEUE_ADD,
                    null, session + "-" + i);
        }
    }

    private void startNeuralSequence(String text, float rate) {
        TextToSpeech local = tts;
        if (local == null || closed.get() || !ready.get()) return;
        List<String> chunks = TtsChunker.splitNeural(text);
        if (chunks.isEmpty()) return;

        final long session;
        synchronized (speechLock) {
            // Stop only when the user starts a NEW narration over an old one. Never between chunks.
            if (neuralSpeaking || local.isSpeaking()) {
                try { local.stop(); } catch (Exception ignored) { }
            }
            speechSession++;
            session = speechSession;
            neuralQueue.clear();
            neuralQueue.addAll(chunks);
            neuralSpeaking = true;
            neuralRate = rate;
        }

        // Keep Android's global speech-rate neutral for Kokoro. The requested speed is sent
        // exactly once through the engine-specific Bundle in neuralParams().
        local.setSpeechRate(1.0f);
        speakNextNeural(session, chunks.size());
    }

    private void speakNextNeural(long session, int totalChunks) {
        if (closed.get()) return;
        TextToSpeech local = tts;
        if (local == null || !ready.get()) return;
        final String chunk;
        final int index;
        synchronized (speechLock) {
            if (session != speechSession || !neuralSpeaking) return;
            chunk = neuralQueue.pollFirst();
            index = totalChunks - neuralQueue.size() - 1;
            if (chunk == null) {
                neuralSpeaking = false;
                detail = statusReadyText();
                return;
            }
        }

        Bundle params = neuralParams(neuralRate);
        String id = "braseiro-neural-" + session + "-" + index + "-of-" + totalChunks;
        // Queue is empty because the next request is submitted only after real onDone().
        int result = local.speak(chunk, TextToSpeech.QUEUE_ADD, params, id);
        if (result != TextToSpeech.SUCCESS) {
            synchronized (speechLock) {
                if (session == speechSession) {
                    neuralQueue.clear();
                    neuralSpeaking = false;
                }
            }
            detail = "Falha ao enviar bloco ao motor Kokoro/Sherpa.";
        }
    }

    private Bundle neuralParams(float rate) {
        Bundle params = new Bundle();
        // Compatibility contract consumed by the Braseiro Sherpa/Kokoro engine. Other engines ignore it.
        params.putFloat("speed", rate);
        params.putFloat("braseiro.kokoro.speed", rate);
        params.putInt("maxNumSentences", 2);
        params.putInt("braseiro.kokoro.maxNumSentences", 2);
        params.putFloat("silenceScale", 0.06f);
        params.putFloat("braseiro.kokoro.silenceScale", 0.06f);
        params.putInt("numThreads", 4);
        params.putInt("braseiro.kokoro.numThreads", 4);
        params.putInt("leadingSilenceMs", 50);
        params.putInt("braseiro.kokoro.leadingSilenceMs", 50);
        params.putInt("streamingAudioChunkMs", 125);
        params.putInt("braseiro.kokoro.streamingAudioChunkMs", 125);
        params.putString("streamingMode", "generateWithConfigAndCallback");
        params.putString("braseiro.kokoro.streamingMode", "generateWithConfigAndCallback");
        params.putString("audioDelivery", "SynthesisCallback.audioAvailable");
        params.putString("braseiro.kokoro.audioDelivery", "SynthesisCallback.audioAvailable");
        return params;
    }

    private void prewarmInternal() {
        TextToSpeech local = tts;
        if (local == null || closed.get() || !ready.get() || !"kokoro".equals(activeProfile)) return;
        if (local.isSpeaking() || neuralSpeaking) return;
        String key = activeEnginePackage + "|" + activeVoiceName + "|" + engineGeneration.get();
        if (key.equals(lastPrewarmKey)) return;
        lastPrewarmKey = key;
        try {
            File output = new File(activity.getCacheDir(), "tts-prewarm-" + engineGeneration.get() + ".wav");
            local.synthesizeToFile("Olá.", neuralParams(1.0f), output, "braseiro-prewarm-" + engineGeneration.get());
        } catch (Exception ignored) {
            lastPrewarmKey = "";
        }
    }

    private final class BridgeUtteranceListener extends UtteranceProgressListener {
        private final int generation;
        BridgeUtteranceListener(int generation) { this.generation = generation; }

        @Override public void onStart(String utteranceId) {
            if (generation != engineGeneration.get() || utteranceId == null) return;
            if (utteranceId.startsWith("braseiro-neural-")) detail = "Kokoro/Sherpa narrando.";
        }

        @Override public void onDone(String utteranceId) {
            if (generation != engineGeneration.get() || utteranceId == null) return;
            if (utteranceId.startsWith("braseiro-prewarm-")) {
                try { new File(activity.getCacheDir(), "tts-prewarm-" + generation + ".wav").delete(); }
                catch (Exception ignored) { }
                detail = statusReadyText();
                return;
            }
            if (!utteranceId.startsWith("braseiro-neural-")) return;
            long session = parseNeuralSession(utteranceId);
            if (session < 0) return;
            int total = parseNeuralTotal(utteranceId);
            activity.runOnUiThread(() -> speakNextNeural(session, total));
        }

        @Override public void onError(String utteranceId) {
            onNeuralError(utteranceId, "O motor TTS informou erro durante a síntese.");
        }

        @Override public void onError(String utteranceId, int errorCode) {
            onNeuralError(utteranceId, "O motor TTS informou erro " + errorCode + " durante a síntese.");
        }
    }

    private void onNeuralError(String utteranceId, String message) {
        if (utteranceId == null || !utteranceId.startsWith("braseiro-neural-")) return;
        long session = parseNeuralSession(utteranceId);
        synchronized (speechLock) {
            if (session == speechSession) {
                neuralQueue.clear();
                neuralSpeaking = false;
                detail = message;
            }
        }
    }

    private long parseNeuralSession(String id) {
        try {
            String rest = id.substring("braseiro-neural-".length());
            return Long.parseLong(rest.substring(0, rest.indexOf('-')));
        } catch (Exception ignored) { return -1L; }
    }

    private int parseNeuralTotal(String id) {
        try {
            int marker = id.lastIndexOf("-of-");
            return marker < 0 ? 1 : Math.max(1, Integer.parseInt(id.substring(marker + 4)));
        } catch (Exception ignored) { return 1; }
    }

    private int setPreferredPortugueseLanguage(TextToSpeech local) {
        int language = local.setLanguage(Locale.forLanguageTag("pt-BR"));
        if (isLanguageSupported(language)) {
            activeLanguageTag = "pt-BR";
            return language;
        }
        language = local.setLanguage(Locale.forLanguageTag("pt"));
        if (isLanguageSupported(language)) activeLanguageTag = "pt";
        return language;
    }

    private void refreshEngineCatalog() {
        TextToSpeech local = tts;
        if (closed.get() || local == null) return;
        final int revision = catalogGeneration.incrementAndGet();
        List<TextToSpeech.EngineInfo> infos;
        try { infos = local.getEngines(); }
        catch (Exception e) { return; }
        if (infos == null) infos = new ArrayList<>();

        List<EngineRecord> base = new ArrayList<>();
        for (TextToSpeech.EngineInfo info : infos) {
            if (info == null || info.name == null) continue;
            String rawLabel = info.label == null ? info.name : String.valueOf(info.label);
            String label = displayEngineLabel(info.name, rawLabel);
            base.add(new EngineRecord(info.name, label, info.name.equals(systemDefaultEngine), "probing", new ArrayList<>()));
        }
        base.sort(Comparator.comparing((EngineRecord e) -> !e.isDefault)
                .thenComparing(e -> e.label.toLowerCase(Locale.ROOT)));
        synchronized (catalogLock) {
            engineCatalog.clear();
            engineCatalog.addAll(base);
        }
        for (EngineRecord record : base) {
            if (record.packageName.equals(activeEnginePackage)) {
                updateCatalogRecord(record.packageName, "ready", collectVoiceRecords(local));
            } else {
                probeEngineVoices(record.packageName, revision);
            }
        }
    }

    private void probeEngineVoices(String packageName, int revision) {
        if (closed.get()) return;
        final TextToSpeech[] holder = new TextToSpeech[1];
        try {
            holder[0] = new TextToSpeech(appContext, status -> {
                TextToSpeech probe = holder[0];
                if (probe == null) return;
                try {
                    if (closed.get() || revision != catalogGeneration.get()) return;
                    List<VoiceRecord> voices = status == TextToSpeech.SUCCESS
                            ? collectVoiceRecords(probe) : new ArrayList<>();
                    String state = status == TextToSpeech.SUCCESS ? "ready" : "error";
                    updateCatalogRecord(packageName, state, voices);
                } catch (Exception e) {
                    updateCatalogRecord(packageName, "error", new ArrayList<>());
                } finally {
                    try { probe.shutdown(); } catch (Exception ignored) { }
                }
            }, packageName);
        } catch (Exception e) {
            updateCatalogRecord(packageName, "error", new ArrayList<>());
        }
    }


    private List<VoiceRecord> collectVoiceRecords(TextToSpeech engine) {
        List<VoiceRecord> voices = new ArrayList<>();
        if (engine == null) return voices;
        try {
            Set<Voice> available = engine.getVoices();
            if (available != null) {
                for (Voice voice : available) {
                    if (voice == null) continue;
                    Locale locale = voice.getLocale();
                    voices.add(new VoiceRecord(
                            voice.getName(),
                            locale == null ? "" : locale.toLanguageTag(),
                            voice.getQuality(),
                            voice.getLatency(),
                            voice.isNetworkConnectionRequired()));
                }
            }
        } catch (Exception ignored) { }
        voices.sort(Comparator
                .comparing((VoiceRecord v) -> !v.locale.toLowerCase(Locale.ROOT).startsWith("pt"))
                .thenComparing(v -> v.locale)
                .thenComparing(v -> v.name.toLowerCase(Locale.ROOT)));
        return voices;
    }

    private void updateCatalogRecord(String packageName, String state, List<VoiceRecord> voices) {
        synchronized (catalogLock) {
            for (int i = 0; i < engineCatalog.size(); i++) {
                EngineRecord current = engineCatalog.get(i);
                if (!current.packageName.equals(packageName)) continue;
                engineCatalog.set(i, new EngineRecord(current.packageName, current.label, current.isDefault, state, voices));
                return;
            }
        }
    }

    private boolean isInstalledEngine(String packageName) {
        TextToSpeech local = tts;
        if (local != null) {
            try {
                List<TextToSpeech.EngineInfo> infos = local.getEngines();
                if (infos != null) for (TextToSpeech.EngineInfo info : infos)
                    if (info != null && packageName.equals(info.name)) return true;
            } catch (Exception ignored) { }
        }
        synchronized (catalogLock) {
            for (EngineRecord record : engineCatalog) if (packageName.equals(record.packageName)) return true;
        }
        return false;
    }

    private String engineLabel(String packageName) {
        synchronized (catalogLock) {
            for (EngineRecord record : engineCatalog) if (packageName.equals(record.packageName)) return record.label;
        }
        TextToSpeech local = tts;
        if (local != null) {
            try {
                List<TextToSpeech.EngineInfo> infos = local.getEngines();
                if (infos != null) for (TextToSpeech.EngineInfo info : infos)
                    if (info != null && packageName.equals(info.name)) return displayEngineLabel(packageName, info.label == null ? packageName : String.valueOf(info.label));
            } catch (Exception ignored) { }
        }
        return packageName.isEmpty() ? "Android TTS" : packageName;
    }

    private Voice findVoiceByName(TextToSpeech local, String name) {
        try {
            Set<Voice> voices = local.getVoices();
            if (voices != null) for (Voice voice : voices)
                if (voice != null && name.equals(voice.getName())) return voice;
        } catch (Exception ignored) { }
        return null;
    }

    private String currentVoiceName(TextToSpeech local) {
        try {
            Voice voice = local.getVoice();
            return voice == null || voice.getName() == null ? "" : voice.getName();
        } catch (Exception ignored) { return ""; }
    }

    private List<String> splitForSpeech(String text) {
        return TtsChunker.splitAndroid(text, androidChunkLimit());
    }

    private int androidChunkLimit() {
        int engineMax;
        try { engineMax = TextToSpeech.getMaxSpeechInputLength(); }
        catch (Exception ignored) { engineMax = 4000; }
        return Math.max(500, Math.min(TARGET_ANDROID_CHUNK_CHARS, engineMax - 128));
    }

    private String ok(String text, float rate, String status) {
        try {
            boolean neural = "kokoro".equals(activeProfile);
            int chunks = neural ? TtsChunker.splitNeural(text).size() : TtsChunker.splitAndroid(text, androidChunkLimit()).size();
            return new JSONObject()
                    .put("ok", true)
                    .put("status", status)
                    .put("rate", rate)
                    .put("language", activeLanguageTag)
                    .put("enginePackage", activeEnginePackage)
                    .put("voiceName", activeVoiceName)
                    .put("profile", activeProfile)
                    .put("chunks", chunks)
                    .put("characters", text.length())
                    .toString();
        } catch (Exception e) { return "{\"ok\":true}"; }
    }

    private float parseRate(String rawRate) {
        float rate = 1.0f;
        try { rate = Float.parseFloat(rawRate == null ? "1" : rawRate); }
        catch (Exception ignored) { }
        return Math.max(0.6f, Math.min(1.4f, rate));
    }

    private static String capAudioText(String text, int max) {
        if (text == null || text.length() <= max) return text == null ? "" : text;
        int cut = max;
        while (cut > max - 400 && cut > 0 && !Character.isWhitespace(text.charAt(cut - 1))) cut--;
        if (cut <= max - 400) cut = max;
        return text.substring(0, cut).trim();
    }

    private String statusReadyText() {
        String label = activeEngineLabel == null || activeEngineLabel.isBlank() ? activeEnginePackage : activeEngineLabel;
        if ("kokoro".equals(activeProfile)) return BRASEIRO_KOKORO_LABEL + " pronto.";
        return "TTS Android pronto · " + label + " · " + activeLanguageTag + ".";
    }

    private static String detectProfile(String packageName, String label, String voiceName) {
        String pkg = normalize(packageName);
        if (SHERPA_ENGINE.equals(pkg) || SHERPA_ENGINE_DEBUG.equals(pkg)) return "kokoro";
        String combined = (pkg + " " + normalize(label) + " " + normalize(voiceName)).toLowerCase(Locale.ROOT);
        if (combined.contains("kokoro") || combined.contains("sherpa")) return "kokoro";
        return "android";
    }

    private String migrateLegacyEnginePreference(String rawPackage) {
        String requested = normalize(rawPackage);
        String lower = requested.toLowerCase(Locale.ROOT);
        boolean legacy = LEGACY_BRASEIRO_ENGINE.equals(requested) || lower.contains("db next-gen") || lower.contains("db next gen");
        if (!legacy) return requested;
        if (isPackageInstalled(SHERPA_ENGINE)) return SHERPA_ENGINE;
        if (isPackageInstalled(SHERPA_ENGINE_DEBUG)) return SHERPA_ENGINE_DEBUG;
        return "";
    }

    private boolean isPackageInstalled(String packageName) {
        try { appContext.getPackageManager().getPackageInfo(packageName, 0); return true; }
        catch (Exception ignored) { return false; }
    }

    private static String displayEngineLabel(String packageName, String rawLabel) {
        String pkg = normalize(packageName);
        if (SHERPA_ENGINE.equals(pkg) || SHERPA_ENGINE_DEBUG.equals(pkg)) return BRASEIRO_KOKORO_LABEL;
        return normalize(rawLabel).isEmpty() ? pkg : normalize(rawLabel);
    }

    private static boolean isLanguageSupported(int result) {
        return result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED;
    }

    private static String normalize(String value) { return value == null ? "" : value.trim(); }

    private static String fail(String error, String message) {
        try { return new JSONObject().put("ok", false).put("error", error).put("message", message).toString(); }
        catch (Exception e) { return "{\"ok\":false,\"error\":\"TTS_FAILED\"}"; }
    }

    private static final class VoiceRecord {
        final String name;
        final String locale;
        final int quality;
        final int latency;
        final boolean network;

        VoiceRecord(String name, String locale, int quality, int latency, boolean network) {
            this.name = name == null ? "" : name;
            this.locale = locale == null ? "" : locale;
            this.quality = quality;
            this.latency = latency;
            this.network = network;
        }

        JSONObject toJson() throws Exception {
            return new JSONObject()
                    .put("name", name)
                    .put("locale", locale)
                    .put("quality", quality)
                    .put("latency", latency)
                    .put("networkRequired", network);
        }
    }

    private static final class EngineRecord {
        final String packageName;
        final String label;
        final boolean isDefault;
        final String status;
        final List<VoiceRecord> voices;

        EngineRecord(String packageName, String label, boolean isDefault, String status, List<VoiceRecord> voices) {
            this.packageName = packageName == null ? "" : packageName;
            this.label = label == null ? this.packageName : label;
            this.isDefault = isDefault;
            this.status = status == null ? "unknown" : status;
            this.voices = voices == null ? new ArrayList<>() : voices;
        }

        JSONObject toJson() throws Exception {
            JSONArray list = new JSONArray();
            for (VoiceRecord voice : voices) list.put(voice.toJson());
            return new JSONObject()
                    .put("packageName", packageName)
                    .put("label", label)
                    .put("isDefault", isDefault)
                    .put("profile", detectProfile(packageName, label, ""))
                    .put("status", status)
                    .put("voices", list);
        }
    }
}
