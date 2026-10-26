package braseiro.ose.tts.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import braseiro.ose.tts.BraseiroSpokenTextSanitizer
import braseiro.ose.tts.TtsPort
import braseiro.ose.tts.TtsState
import braseiro.ose.tts.TtsTextSegmenter
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Android TTS adapter used by BRASEIRO OSE with TTS Universal V3 routing.
 *
 * Supported operational modes:
 * - GEMINI_TTS: Remote neural Gemini TTS synthesis
 * - KOKORO_INSTALLED: Local neural Kokoro/Sherpa-onnx Android TTS engine
 * - ANDROID_TTS: System default or user-selected installed Android TTS engine
 * - OFF: Muted speech output
 *
 * Narration is sanitized by BraseiroSpokenTextSanitizer before audio synthesis.
 * Canonical visibleNarration in UI and state remains unaltered.
 */
class AndroidTtsController(context: Context) : TtsPort, TextToSpeech.OnInitListener {

    enum class Mode(val wireName: String) {
        GEMINI_TTS("GEMINI_TTS"),
        KOKORO_INSTALLED("KOKORO_INSTALLED"),
        ANDROID_TTS("ANDROID_TTS"),
        OFF("OFF");

        companion object {
            fun fromString(name: String?): Mode = when (name?.trim()?.uppercase()) {
                "GEMINI_TTS" -> GEMINI_TTS
                "KOKORO_INSTALLED", "KOKORO" -> KOKORO_INSTALLED
                "OFF" -> OFF
                else -> ANDROID_TTS
            }
        }
    }

    data class EngineOption(
        val packageName: String,
        val label: String,
        val system: Boolean,
        val preferredKokoro: Boolean
    )

    data class VoiceOption(
        val name: String,
        val localeTag: String,
        val quality: Int,
        val latency: Int,
        val networkRequired: Boolean
    )

    companion object {
        private const val PREFS = "braseiro_ose_android_tts_v2"
        private const val KEY_MODE = "tts_mode_v3"
        private const val KEY_ENGINE = "engine_package"
        private const val KEY_VOICE = "voice_name"
        private const val KEY_RATE = "speech_rate"
        private const val DEFAULT_RATE = 1.0f
        val KOKORO_ENGINES = setOf(
            "com.k2fsa.sherpa.onnx.tts.engine",
            "com.k2fsa.sherpa.onnx.tts.engine.debug"
        )
    }

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val generation = AtomicLong(0L)
    private var engine: TextToSpeech? = null
    private var activeEnginePackage: String? = null
    private var pendingNarration: String? = null
    private var lastNarration: String? = null
    private var chunks: List<String> = emptyList()
    private var completedChunks = 0
    private var activeGeneration = 0L

    var geminiSpeaker: ((String) -> Unit)? = null
    var geminiStopper: (() -> Unit)? = null

    @Volatile override var state: TtsState = TtsState.PREPARING
        private set

    @Volatile var lastError: String? = null
        private set

    var onState: ((TtsState, String?) -> Unit)? = null

    init {
        restartEngine(prefs.getString(KEY_ENGINE, null))
    }

    fun mode(): Mode {
        val stored = prefs.getString(KEY_MODE, null)
        return if (stored != null) Mode.fromString(stored) else Mode.ANDROID_TTS
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            fail("TTS_INIT_FAILED_$status")
            return
        }
        val tts = engine ?: run {
            fail("TTS_ENGINE_MISSING")
            return
        }
        activeEnginePackage = activeEnginePackage ?: tts.defaultEngine

        if (!prefs.contains(KEY_ENGINE)) {
            val kokoro = tts.engines.firstOrNull { it.name in KOKORO_ENGINES }?.name
            if (kokoro != null && kokoro != activeEnginePackage) {
                prefs.edit().putString(KEY_ENGINE, kokoro).apply()
                restartEngine(kokoro)
                return
            }
        }

        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (isActiveUtterance(utteranceId)) updateState(TtsState.PLAYING)
            }

            override fun onDone(utteranceId: String?) {
                if (!isActiveUtterance(utteranceId)) return
                completedChunks += 1
                if (completedChunks >= chunks.size) {
                    chunks = emptyList()
                    completedChunks = 0
                    updateState(TtsState.IDLE)
                }
            }

            @Deprecated("Deprecated by Android")
            override fun onError(utteranceId: String?) {
                if (isActiveUtterance(utteranceId)) fail("TTS_UTTERANCE_ERROR")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (isActiveUtterance(utteranceId)) fail("TTS_UTTERANCE_ERROR_$errorCode")
            }
        })

        val languageResult = tts.setLanguage(Locale("pt", "BR"))
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            if (tts.voices.orEmpty().none { it.locale.language.equals("pt", ignoreCase = true) }) {
                fail("TTS_PT_BR_NOT_AVAILABLE")
                return
            }
        }

        applyPreferredVoice(tts)
        tts.setSpeechRate(speechRate())
        lastError = null
        updateState(TtsState.IDLE)

        pendingNarration?.takeIf { it.isNotBlank() }?.let { pending ->
            pendingNarration = null
            playVisibleNarration(pending)
        }
    }

    fun engineOptions(): List<EngineOption> {
        val tts = engine ?: return emptyList()
        return tts.engines
            .map {
                val isKokoro = it.name in KOKORO_ENGINES
                val rawLabel = it.label?.toString().orEmpty().ifBlank { it.name }
                val displayLabel = if (isKokoro) "Kokoro PT-BR Neural ($rawLabel)" else rawLabel
                EngineOption(
                    packageName = it.name,
                    label = displayLabel,
                    system = runCatching {
                        val info = appContext.packageManager.getApplicationInfo(it.name, 0)
                        (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                            (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    }.getOrDefault(false),
                    preferredKokoro = isKokoro
                )
            }
            .sortedWith(compareByDescending<EngineOption> { it.preferredKokoro }.thenBy { it.label.lowercase() })
    }

    fun voiceOptions(): List<VoiceOption> {
        val tts = engine ?: return emptyList()
        return tts.voices.orEmpty()
            .filter { it.locale.language.equals("pt", ignoreCase = true) }
            .map(::voiceOption)
            .sortedWith(
                compareByDescending<VoiceOption> { it.localeTag.equals("pt-BR", ignoreCase = true) }
                    .thenBy { it.networkRequired }
                    .thenByDescending { it.quality }
                    .thenBy { it.name.lowercase() }
            )
    }

    fun selectedEnginePackage(): String? =
        prefs.getString(KEY_ENGINE, null) ?: activeEnginePackage ?: engine?.defaultEngine

    fun selectedVoiceName(): String? =
        prefs.getString(KEY_VOICE, null) ?: engine?.voice?.name

    fun speechRate(): Float = prefs.getFloat(KEY_RATE, DEFAULT_RATE).coerceIn(0.65f, 1.50f)

    fun isReady(): Boolean = (mode() == Mode.OFF || mode() == Mode.GEMINI_TTS) ||
        (state != TtsState.PREPARING && state != TtsState.ERROR && engine != null)

    fun configure(enginePackage: String?, voiceName: String?, rate: Float, mode: String? = null) {
        val cleanEngine = enginePackage?.trim()?.takeIf { it.isNotBlank() }
        val cleanVoice = voiceName?.trim()?.takeIf { it.isNotBlank() }
        val cleanRate = rate.coerceIn(0.65f, 1.50f)
        val cleanMode = mode?.let { Mode.fromString(it) }

        val editor = prefs.edit().putFloat(KEY_RATE, cleanRate)
        if (cleanEngine == null) editor.remove(KEY_ENGINE) else editor.putString(KEY_ENGINE, cleanEngine)
        if (cleanVoice == null) editor.remove(KEY_VOICE) else editor.putString(KEY_VOICE, cleanVoice)
        if (cleanMode != null) editor.putString(KEY_MODE, cleanMode.wireName)
        editor.apply()

        if (cleanEngine != null && cleanEngine != activeEnginePackage) {
            restartEngine(cleanEngine)
            return
        }
        engine?.let {
            applyPreferredVoice(it)
            it.setSpeechRate(cleanRate)
        }
        notifyState()
    }

    override fun playVisibleNarration(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) {
            fail("EMPTY_VISIBLE_NARRATION")
            return
        }
        lastNarration = clean

        val activeMode = mode()
        if (activeMode == Mode.OFF) {
            updateState(TtsState.IDLE)
            return
        }

        // Sanitize narration for speech before passing to synthesis
        val spoken = BraseiroSpokenTextSanitizer.sanitize(clean)
        if (spoken.isBlank()) {
            fail("EMPTY_VISIBLE_NARRATION")
            return
        }

        if (activeMode == Mode.GEMINI_TTS) {
            updateState(TtsState.PLAYING)
            try {
                geminiSpeaker?.invoke(spoken)
            } catch (t: Throwable) {
                fail(t.message ?: "GEMINI_TTS_FAILED")
            }
            return
        }

        if (state == TtsState.PREPARING || engine == null) {
            pendingNarration = clean
            return
        }
        if (state == TtsState.ERROR) {
            pendingNarration = clean
            restartEngine(prefs.getString(KEY_ENGINE, null))
            return
        }

        val tts = engine ?: run {
            fail("TTS_ENGINE_MISSING")
            return
        }
        // Sherpa/Kokoro has costly synthesis startup for every speak() call.
        // Queue fewer, longer utterances (still under Android's max input
        // length) so sentence boundaries do not trigger another cold start.
        // Google/Samsung engines keep the previously validated segment sizes.
        val localKokoro = activeMode == Mode.KOKORO_INSTALLED ||
            (activeEnginePackage in KOKORO_ENGINES)
        val nextChunks = if (localKokoro)
            TtsTextSegmenter.segment(spoken, targetChars = 3000, maxChars = 3500, minMergeChars = 900)
        else
            TtsTextSegmenter.segment(spoken, targetChars = 450, maxChars = 650, minMergeChars = 220)
        if (nextChunks.isEmpty()) {
            fail("EMPTY_VISIBLE_NARRATION")
            return
        }

        generation.incrementAndGet()
        tts.stop()
        chunks = nextChunks
        completedChunks = 0
        activeGeneration = generation.get()
        updateState(TtsState.PREPARING)

        for ((index, chunk) in chunks.withIndex()) {
            val id = utteranceId(activeGeneration, index)
            val result = tts.speak(chunk, TextToSpeech.QUEUE_ADD, null, id)
            if (result != TextToSpeech.SUCCESS) {
                fail("TTS_SPEAK_FAILED_$result")
                return
            }
        }
    }

    fun replayVisibleNarration(fallbackVisibleNarration: String? = null) {
        val text = lastNarration?.takeIf { it.isNotBlank() }
            ?: fallbackVisibleNarration?.takeIf { it.isNotBlank() }
        if (text == null) {
            fail("EMPTY_VISIBLE_NARRATION")
            return
        }
        playVisibleNarration(text)
    }

    fun playTestPhrase() {
        playVisibleNarration("Teste de voz do Mestre Bárbara. Se você está ouvindo esta frase, o motor de voz selecionado está funcionando.")
    }

    override fun stop() {
        generation.incrementAndGet()
        pendingNarration = null
        chunks = emptyList()
        completedChunks = 0
        runCatching { geminiStopper?.invoke() }
        engine?.stop()
        if (state != TtsState.ERROR) updateState(TtsState.STOPPED)
    }

    override fun shutdown() {
        generation.incrementAndGet()
        pendingNarration = null
        chunks = emptyList()
        completedChunks = 0
        runCatching { geminiStopper?.invoke() }
        engine?.stop()
        engine?.shutdown()
        engine = null
        state = TtsState.IDLE
        lastError = null
    }

    private fun restartEngine(requestedPackage: String?) {
        generation.incrementAndGet()
        chunks = emptyList()
        completedChunks = 0
        engine?.stop()
        engine?.shutdown()
        engine = null
        activeEnginePackage = requestedPackage?.takeIf { it.isNotBlank() }
        updateState(TtsState.PREPARING)
        engine = if (activeEnginePackage == null) {
            TextToSpeech(appContext, this)
        } else {
            TextToSpeech(appContext, this, activeEnginePackage)
        }
    }

    private fun applyPreferredVoice(tts: TextToSpeech) {
        val requested = prefs.getString(KEY_VOICE, null)
        val voices = tts.voices.orEmpty()
        val chosen = requested?.let { name -> voices.firstOrNull { it.name == name } }
            ?: voices
                .filter { it.locale.language.equals("pt", ignoreCase = true) }
                .sortedWith(
                    compareByDescending<Voice> { it.locale.toLanguageTag().equals("pt-BR", ignoreCase = true) }
                        .thenBy { it.isNetworkConnectionRequired }
                        .thenByDescending { it.quality }
                )
                .firstOrNull()
        chosen?.let {
            tts.voice = it
            prefs.edit().putString(KEY_VOICE, it.name).apply()
        }
    }

    private fun utteranceId(generationId: Long, index: Int) = "braseiro-ose-$generationId-$index"

    private fun isActiveUtterance(utteranceId: String?): Boolean =
        utteranceId?.startsWith("braseiro-ose-$activeGeneration-") == true

    private fun voiceOption(voice: Voice) = VoiceOption(
        name = voice.name,
        localeTag = voice.locale.toLanguageTag(),
        quality = voice.quality,
        latency = voice.latency,
        networkRequired = voice.isNetworkConnectionRequired
    )

    private fun updateState(next: TtsState) {
        state = next
        notifyState()
    }

    private fun fail(message: String) {
        lastError = message
        state = TtsState.ERROR
        notifyState()
    }

    private fun notifyState() {
        onState?.invoke(state, lastError)
    }
}
