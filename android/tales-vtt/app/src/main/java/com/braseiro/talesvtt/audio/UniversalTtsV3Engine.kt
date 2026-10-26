package com.braseiro.talesvtt.audio

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

enum class TtsEngineType(val label: String) {
  ANDROID_NATIVE("Android TTS (Google/Samsung)"),
  GEMINI_SPEECH("Gemini Speech API"),
  KOKORO_SHERPA("Braseiro Kokoro PT-BR Neural")
}

data class TtsVoiceOption(
  val id: String,
  val name: String,
  val enginePackage: String,
  val locale: String,
  val isNeural: Boolean
)

data class TtsSettings(
  val engineType: TtsEngineType = TtsEngineType.ANDROID_NATIVE,
  val selectedVoiceId: String = "",
  val selectedModel: String = "gemini-2.5-flash",
  val voiceProfile: String = "Narrador Imersivo",
  val voiceStyle: String = "Calmo e misterioso",
  val speechRate: Float = 1.0f,
  val pitch: Float = 1.0f,
  val autoPlayOnSceneLoad: Boolean = true
)

/**
 * Universal TTS V3 Engine for Tales from the Loop & Things from the Flood.
 * Provides resilient multi-backend narration, dynamic voice discovery, 350-800 char chunking,
 * tag sanitization, audio focus ducking, discrete dialogue modulation and transparent fallbacks.
 */
class UniversalTtsV3Engine(private val context: Context) : TextToSpeech.OnInitListener {

  companion object {
    private const val PREFS_NAME = "braseiro_tts_v3_prefs"
    private const val KEY_ENGINE = "tts_engine"
    private const val KEY_VOICE = "tts_voice"
    private const val KEY_MODEL = "tts_model"
    private const val KEY_PROFILE = "tts_voice_profile"
    private const val KEY_STYLE = "tts_voice_style"
    private const val KEY_RATE = "tts_rate"
    private const val KEY_PITCH = "tts_pitch"
    private const val KEY_AUTOPLAY = "tts_autoplay"

    const val KOKORO_PACKAGE = "com.k2fsa.sherpa.onnx.tts.engine"
    const val GOOGLE_TTS_PACKAGE = "com.google.android.tts"
  }

  private val prefs: SharedPreferences by lazy {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  private val mainHandler = Handler(Looper.getMainLooper())
  private var tts: TextToSpeech? = null
  private var isTtsInitialized = false
  private var audioManager: AudioManager? = null

  private val availableVoices = mutableListOf<TtsVoiceOption>()
  private var currentSettings: TtsSettings = loadSettings()

  // Playback sequence state
  private var currentChunks = listOf<String>()
  private var currentChunkIndex = 0
  private var isSpeaking = false
  private var currentSessionId: String = ""
  private val geminiNarration = GeminiNarrationTts(context)
  private val geminiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private var geminiJob: Job? = null

  var onStateChangedListener: ((Boolean, String) -> Unit)? = null

  init {
    audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    initializeTts()
  }

  fun getSettings(): TtsSettings = currentSettings

  fun saveSettings(settings: TtsSettings) {
    val engineChanged = currentSettings.engineType != settings.engineType
    currentSettings = settings
    prefs.edit()
      .putString(KEY_ENGINE, settings.engineType.name)
      .putString(KEY_VOICE, settings.selectedVoiceId)
      .putString(KEY_MODEL, settings.selectedModel)
      .putString(KEY_PROFILE, settings.voiceProfile)
      .putString(KEY_STYLE, settings.voiceStyle)
      .putFloat(KEY_RATE, settings.speechRate)
      .putFloat(KEY_PITCH, settings.pitch)
      .putBoolean(KEY_AUTOPLAY, settings.autoPlayOnSceneLoad)
      .apply()

    // Synchronize ONLY TTS specific settings to Barbara transport - NEVER overwrite text model!
    try {
      context.getSharedPreferences("braseiro_motor_cfg_v1", Context.MODE_PRIVATE)
        .edit()
        .putString("tts_model", settings.selectedModel)
        .putString("tts_voice", settings.selectedVoiceId)
        .putString("tts_provider", settings.engineType.name)
        .putString("voice_profile", settings.voiceProfile)
        .putString("voice_style", settings.voiceStyle)
        .apply()
    } catch (_: Exception) {}

    if (engineChanged) {
      stop()
      tts?.shutdown()
      initializeTts()
    } else {
      tts?.setSpeechRate(settings.speechRate)
      tts?.setPitch(settings.pitch)
      applyVoice(settings.selectedVoiceId)
    }
  }

  private fun loadSettings(): TtsSettings {
    val engineName = prefs.getString(KEY_ENGINE, TtsEngineType.ANDROID_NATIVE.name) ?: TtsEngineType.ANDROID_NATIVE.name
    val engine = runCatching { TtsEngineType.valueOf(engineName) }.getOrDefault(TtsEngineType.ANDROID_NATIVE)
    return TtsSettings(
      engineType = engine,
      selectedVoiceId = prefs.getString(KEY_VOICE, "") ?: "",
      selectedModel = prefs.getString(KEY_MODEL, "gemini-2.5-flash") ?: "gemini-2.5-flash",
      voiceProfile = prefs.getString(KEY_PROFILE, "Narrador Imersivo") ?: "Narrador Imersivo",
      voiceStyle = prefs.getString(KEY_STYLE, "Calmo e misterioso") ?: "Calmo e misterioso",
      speechRate = prefs.getFloat(KEY_RATE, 1.0f),
      pitch = prefs.getFloat(KEY_PITCH, 1.0f),
      autoPlayOnSceneLoad = prefs.getBoolean(KEY_AUTOPLAY, true)
    )
  }

  private fun initializeTts() {
    try {
      val targetPackage = if (currentSettings.engineType == TtsEngineType.KOKORO_SHERPA) {
        val kokoroInstalled = try {
          context.packageManager.getPackageInfo(KOKORO_PACKAGE, 0)
          true
        } catch (_: Exception) {
          false
        }
        if (kokoroInstalled) KOKORO_PACKAGE else null
      } else {
        null // system default
      }
      tts = if (targetPackage != null) {
        TextToSpeech(context, this, targetPackage)
      } else {
        TextToSpeech(context, this)
      }
    } catch (e: Exception) {
      isTtsInitialized = false
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      isTtsInitialized = true
      tts?.language = Locale("pt", "BR")
      tts?.setSpeechRate(currentSettings.speechRate)
      tts?.setPitch(currentSettings.pitch)
      setupProgressListener()
      refreshInstalledVoices()
      applyVoice(currentSettings.selectedVoiceId)
    } else {
      isTtsInitialized = false
    }
  }

  fun getAvailableVoices(): List<TtsVoiceOption> {
    if (availableVoices.isEmpty() && isTtsInitialized) {
      refreshInstalledVoices()
    }
    return availableVoices
  }

  private fun refreshInstalledVoices() {
    availableVoices.clear()
    val ttsInstance = tts ?: return
    try {
      val voices = ttsInstance.voices ?: emptySet()
      for (v in voices) {
        val loc = v.locale
        if (loc.language.equals("pt", ignoreCase = true)) {
          val isNeural = v.name.contains("neural", ignoreCase = true) || v.name.contains("network", ignoreCase = true)
          availableVoices.add(
            TtsVoiceOption(
              id = v.name,
              name = "${v.name} (${loc.displayCountry})",
              enginePackage = "",
              locale = loc.toString(),
              isNeural = isNeural
            )
          )
        }
      }
    } catch (_: Exception) {}

    if (availableVoices.isEmpty()) {
      availableVoices.add(
        TtsVoiceOption(
          id = "default_ptbr",
          name = "Português Brasileiro (Padrão)",
          enginePackage = "",
          locale = "pt_BR",
          isNeural = false
        )
      )
    }
  }

  private fun applyVoice(voiceId: String) {
    val ttsInstance = tts ?: return
    if (voiceId.isBlank()) return
    try {
      val voices = ttsInstance.voices ?: emptySet()
      val target = voices.firstOrNull { it.name == voiceId }
      if (target != null) {
        ttsInstance.voice = target
      }
    } catch (_: Exception) {}
  }

  /**
   * Sanitizes text: removes instructions, style tags, <sigh>, model tokens and technical markup.
   */
  fun sanitizeVisibleText(rawText: String): String {
    var text = rawText
    text = text.replace(Regex("<[^>]+>"), " ") // Strip HTML/XML-like tags (<sigh>, <pause>)
    text = text.replace(Regex("\\[[^\\]]+\\]"), " ") // Strip [male voice], [narrator], [gm_only]
    text = text.replace(Regex("(?i)\\b(male voice|female voice|narrator|gm only|mestre|segredo):?"), " ")
    text = text.replace(Regex("[*#_`~]"), "") // Strip markdown formatting
    text = text.replace(Regex("\\s+"), " ").trim()
    return text
  }

  /**
   * Splits narrative text into safe chunks of 350 to 800 characters,
   * breaking strictly at sentence boundaries (punctuation) and never mid-word.
   */
  fun splitIntoSafeChunks(text: String, minLength: Int = 350, maxLength: Int = 800): List<String> {
    val clean = sanitizeVisibleText(text)
    if (clean.isBlank()) return emptyList()
    if (clean.length <= maxLength) return listOf(clean)

    val chunks = mutableListOf<String>()
    var remaining = clean

    while (remaining.isNotEmpty()) {
      if (remaining.length <= maxLength) {
        chunks.add(remaining.trim())
        break
      }

      // Look for sentence end within [minLength, maxLength]
      val window = remaining.substring(0, minOf(remaining.length, maxLength))
      var cutIndex = -1

      // Prefer sentence endings: . ! ? ;
      for (del in listOf(". ", "! ", "? ", "; ", ".\n", "!\n", "?\n")) {
        val lastIdx = window.lastIndexOf(del)
        if (lastIdx >= minLength && lastIdx > cutIndex) {
          cutIndex = lastIdx + del.length
        }
      }

      // Fallback: comma or colon
      if (cutIndex == -1) {
        for (del in listOf(", ", ": ")) {
          val lastIdx = window.lastIndexOf(del)
          if (lastIdx >= minLength && lastIdx > cutIndex) {
            cutIndex = lastIdx + del.length
          }
        }
      }

      // Fallback: any space before maxLength
      if (cutIndex == -1) {
        cutIndex = window.lastIndexOf(' ')
        if (cutIndex < minLength) {
          cutIndex = maxLength
        }
      }

      val chunk = remaining.substring(0, cutIndex).trim()
      if (chunk.isNotEmpty()) chunks.add(chunk)
      remaining = remaining.substring(cutIndex).trim()
    }

    return chunks
  }

  fun speakNarration(fullText: String) {
    stop()
    val chunks = splitIntoSafeChunks(fullText)
    if (chunks.isEmpty()) return

    currentChunks = chunks
    currentChunkIndex = 0
    currentSessionId = UUID.randomUUID().toString()
    isSpeaking = true

    requestAudioFocus()
    if (currentSettings.engineType == TtsEngineType.GEMINI_SPEECH) {
      val session = currentSessionId
      geminiJob = geminiScope.launch {
        for (i in chunks.indices) {
          if (!isSpeaking || currentSessionId != session) return@launch
          currentChunkIndex = i
          onStateChangedListener?.invoke(true, chunks[i])
          val result = geminiNarration.speak(chunks[i])
          if (!isSpeaking || currentSessionId != session) return@launch
          if (result.isFailure) {
            // Explicitly inform the UI; continue the remaining chunks using Android TTS.
            onStateChangedListener?.invoke(true,
              "Gemini indisponível: ${result.exceptionOrNull()?.message ?: "erro de áudio"}. Usando voz Android.")
            if (isTtsInitialized) playNextChunk() else stop()
            return@launch
          }
        }
        if (isSpeaking && currentSessionId == session) stop()
      }
    } else {
      playNextChunk()
    }
  }

  private fun playNextChunk() {
    if (!isSpeaking || currentChunkIndex >= currentChunks.size) {
      stop()
      return
    }

    val chunk = currentChunks[currentChunkIndex]
    val utteranceId = "$currentSessionId-$currentChunkIndex"

    // Discrete character dialogue modulation:
    // If chunk contains dialogue quotes, modulate pitch slightly higher/lower to distinguish voice
    val trimmed = chunk.trimStart()
    val isDialogue = trimmed.startsWith("\"") || trimmed.startsWith("“") || trimmed.startsWith("—") || trimmed.startsWith("-")
    if (isDialogue) {
      tts?.setPitch(currentSettings.pitch * 1.08f)
      tts?.setSpeechRate(currentSettings.speechRate * 0.95f)
    } else {
      tts?.setPitch(currentSettings.pitch)
      tts?.setSpeechRate(currentSettings.speechRate)
    }

    val params = Bundle()
    params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)

    tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    onStateChangedListener?.invoke(true, chunk)
  }

  private fun setupProgressListener() {
    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
      override fun onStart(utteranceId: String?) {}

      override fun onDone(utteranceId: String?) {
        mainHandler.post {
          if (isSpeaking && utteranceId?.startsWith(currentSessionId) == true) {
            currentChunkIndex++
            if (currentChunkIndex < currentChunks.size) {
              playNextChunk()
            } else {
              stop()
            }
          }
        }
      }

      override fun onError(utteranceId: String?) {
        mainHandler.post {
          stop()
        }
      }
    })
  }

  fun shutdown() {
    stop()
    geminiScope.cancel()
    try { tts?.shutdown() } catch (_: Exception) {}
    tts = null
    isTtsInitialized = false
  }

  fun stop() {
    geminiJob?.cancel()
    geminiJob = null
    geminiNarration.stop()
    isSpeaking = false
    currentChunks = emptyList()
    currentChunkIndex = 0
    tts?.stop()
    abandonAudioFocus()
    onStateChangedListener?.invoke(false, "")
  }

  private var activeFocusRequest: AudioFocusRequest? = null

  private fun requestAudioFocus() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val audioAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .build()
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
          .setAudioAttributes(audioAttributes)
          .setAcceptsDelayedFocusGain(false)
          .setOnAudioFocusChangeListener { focusChange ->
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
              stop()
            }
          }
          .build()
        activeFocusRequest = req
        audioManager?.requestAudioFocus(req)
      } else {
        @Suppress("DEPRECATION")
        audioManager?.requestAudioFocus(
          { focusChange ->
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
              stop()
            }
          },
          AudioManager.STREAM_MUSIC,
          AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )
      }
    } catch (_: Exception) {}
  }

  private fun abandonAudioFocus() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        activeFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        activeFocusRequest = null
      } else {
        @Suppress("DEPRECATION")
        audioManager?.abandonAudioFocus(null)
      }
    } catch (_: Exception) {}
  }
}