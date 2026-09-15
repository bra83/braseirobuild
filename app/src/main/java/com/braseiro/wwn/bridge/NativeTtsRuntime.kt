package com.braseiro.wwn.bridge

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

class NativeTtsRuntime(
    context: Context,
    private val geminiRuntime: GeminiRuntime,
    private val onSpeakingChanged: (Boolean) -> Unit = {},
    private val onTtsEvent: (JSONObject) -> Unit = {},
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val appContext = context.applicationContext
    private val pendingUtterances = ConcurrentHashMap.newKeySet<String>()
    private val geminiExecutor = Executors.newSingleThreadExecutor()
    private val generation = AtomicLong(0)
    @Volatile private var state: String = "INITIALIZING"
    @Volatile private var languageStatus: Int = TextToSpeech.LANG_NOT_SUPPORTED
    @Volatile private var activeSessionId: String? = null
    @Volatile private var activePlayer: MediaPlayer? = null
    @Volatile private var activeWav: File? = null
    @Volatile private var activePlaybackLatch: CountDownLatch? = null
    private var engine: TextToSpeech? = null

    init {
        mainHandler.post {
            engine = TextToSpeech(appContext) { result ->
                if (result != TextToSpeech.SUCCESS) { state = "ERROR"; return@TextToSpeech }
                engine?.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { onSpeakingChanged(true); emitEvent("STARTED", utteranceId, "ANDROID") }
                    override fun onDone(utteranceId: String?) { completeAndroid(utteranceId, "DONE") }
                    @Deprecated("Deprecated in Java") override fun onError(utteranceId: String?) { completeAndroid(utteranceId, "ERROR") }
                    override fun onError(utteranceId: String?, errorCode: Int) { completeAndroid(utteranceId, "ERROR", "ANDROID_TTS_$errorCode") }
                    override fun onStop(utteranceId: String?, interrupted: Boolean) { completeAndroid(utteranceId, if (interrupted) "STOPPED" else "DONE") }
                })
                val status = engine?.setLanguage(PT_BR) ?: TextToSpeech.LANG_NOT_SUPPORTED
                languageStatus = status
                state = if (status == TextToSpeech.LANG_MISSING_DATA || status == TextToSpeech.LANG_NOT_SUPPORTED) "LANGUAGE_UNAVAILABLE" else "READY"
            }
        }
    }

    fun status(): JSONObject {
        val gemini = geminiRuntime.status()
        val provider = gemini.optString("ttsProvider", GeminiRuntime.DEFAULT_TTS_PROVIDER)
        val geminiReady = gemini.optBoolean("configured", false)
        val androidReady = state == "READY"
        return JSONObject()
            .put("status", if ((provider == "GEMINI" && geminiReady) || (provider == "ANDROID" && androidReady) || androidReady) "READY" else state)
            .put("ready", (provider == "GEMINI" && geminiReady) || (provider == "ANDROID" && androidReady) || androidReady)
            .put("provider", provider)
            .put("geminiReady", geminiReady)
            .put("androidReady", androidReady)
            .put("model", gemini.optString("ttsModel", GeminiRuntime.DEFAULT_TTS_MODEL))
            .put("voice", gemini.optString("ttsVoice", GeminiRuntime.DEFAULT_TTS_VOICE))
            .put("language", "pt-BR")
            .put("languageStatus", languageStatus)
            .put("speaking", pendingUtterances.isNotEmpty() || activePlayer != null)
            .put("activeSessionId", activeSessionId)
            .put("mechanicalMutation", false)
    }

    fun configure(requestJson: String): JSONObject {
        require(requestJson.length <= 4096) { "TTS configuration is too large" }
        val request = JSONObject(requestJson)
        return geminiRuntime.setTtsConfig(
            request.optString("model", geminiRuntime.configuredTtsModel()),
            request.optString("voice", geminiRuntime.configuredTtsVoice()),
            request.optString("provider", geminiRuntime.configuredTtsProvider()),
        ).let { status() }
    }

    fun speak(requestJson: String): JSONObject {
        require(requestJson.length <= 16_000) { "Pedido de voz grande demais" }
        val request = JSONObject(requestJson)
        val text = request.optString("text", "").trim()
        require(text.isNotBlank()) { "Texto de voz obrigatório" }
        require(text.length <= MAX_TEXT_CHARS) { "Trecho de voz grande demais" }
        val sessionId = request.optString("sessionId", "tts-${UUID.randomUUID()}").trim().take(128).ifBlank { "tts-${UUID.randomUUID()}" }
        val append = request.optBoolean("append", false)
        val role = request.optString("role", "NARRADOR").trim().uppercase().take(40)
        val requestedProvider = request.optString("provider", geminiRuntime.configuredTtsProvider()).trim().uppercase()
        val provider = if (requestedProvider == "ANDROID") "ANDROID" else "GEMINI"
        val allowFallback = request.optBoolean("allowFallback", true)
        val newSession = activeSessionId != sessionId
        val token = if (newSession || !append) {
            stopInternal(incrementGeneration = true)
            activeSessionId = sessionId
            generation.get()
        } else generation.get()

        return if (provider == "GEMINI" && geminiRuntime.status().optBoolean("configured", false)) {
            val utteranceId = "wwn-gemini-${UUID.randomUUID()}"
            pendingUtterances.add(utteranceId)
            onSpeakingChanged(true)
            val model = request.optString("model", geminiRuntime.configuredTtsModel())
            val voice = request.optString("voice", geminiRuntime.configuredTtsVoice())
            val style = request.optString("style", defaultStyleForRole(role))
            geminiExecutor.execute {
                if (token != generation.get()) { pendingUtterances.remove(utteranceId); return@execute }
                try {
                    val speech = geminiRuntime.generateSpeech(text, model, voice, style)
                    if (token != generation.get()) { pendingUtterances.remove(utteranceId); return@execute }
                    playGeminiPcmBlocking(speech.pcm, utteranceId, token)
                } catch (t: Throwable) {
                    pendingUtterances.remove(utteranceId)
                    emitEvent("ERROR", utteranceId, "GEMINI", t.message ?: t.javaClass.simpleName)
                    if (token == generation.get() && allowFallback) speakAndroidFallback(request, sessionId, role)
                    else if (pendingUtterances.isEmpty()) onSpeakingChanged(false)
                }
            }
            JSONObject().put("status", "QUEUED").put("accepted", true).put("provider", "GEMINI")
                .put("model", model).put("voice", voice).put("utteranceId", utteranceId).put("sessionId", sessionId)
                .put("role", role).put("language", "pt-BR").put("mechanicalMutation", false)
        } else {
            speakAndroidFallback(request, sessionId, role)
        }
    }

    private fun speakAndroidFallback(request: JSONObject, sessionId: String, role: String): JSONObject {
        if (state != "READY") return status().put("accepted", false)
        val text = request.optString("text", "").trim()
        val rate = request.optDouble("rate", 1.0).coerceIn(0.55, 1.45).toFloat()
        val pitch = request.optDouble("pitch", 1.0).coerceIn(0.70, 1.35).toFloat()
        val volume = request.optDouble("volume", 1.0).coerceIn(0.0, 1.0).toFloat()
        val append = request.optBoolean("append", false)
        val utteranceId = "wwn-android-${UUID.randomUUID()}"
        pendingUtterances.add(utteranceId)
        onSpeakingChanged(true)
        mainHandler.post {
            engine?.setLanguage(PT_BR)
            engine?.setSpeechRate(rate)
            engine?.setPitch(pitch)
            val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume) }
            val queueMode = if (!append) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val accepted = engine?.speak(text, queueMode, params, utteranceId) ?: TextToSpeech.ERROR
            if (accepted == TextToSpeech.ERROR) completeAndroid(utteranceId, "ERROR", "ANDROID_TTS_REJECTED")
        }
        return JSONObject().put("status", "QUEUED").put("accepted", true).put("provider", "ANDROID")
            .put("utteranceId", utteranceId).put("sessionId", sessionId).put("role", role).put("language", "pt-BR")
            .put("rate", rate.toDouble()).put("pitch", pitch.toDouble()).put("volume", volume.toDouble()).put("mechanicalMutation", false)
    }

    private fun playGeminiPcmBlocking(pcm: ByteArray, utteranceId: String, token: Long) {
        val wav = File.createTempFile("wwn-gemini-tts-", ".wav", appContext.cacheDir)
        FileOutputStream(wav).use { writeWav(it, pcm, 24_000, 1, 16) }
        val latch = CountDownLatch(1)
        activePlaybackLatch = latch
        mainHandler.post {
            if (token != generation.get()) { wav.delete(); latch.countDown(); return@post }
            try {
                activeWav = wav
                activePlayer = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    setDataSource(wav.absolutePath)
                    setOnCompletionListener { finishGeminiPlayback(utteranceId, latch, "DONE") }
                    setOnErrorListener { _, what, extra -> finishGeminiPlayback(utteranceId, latch, "ERROR", "MEDIA_$what:$extra"); true }
                    prepare()
                    start()
                }
                onSpeakingChanged(true); emitEvent("STARTED", utteranceId, "GEMINI")
            } catch (t: Throwable) { finishGeminiPlayback(utteranceId, latch, "ERROR", t.message ?: t.javaClass.simpleName) }
        }
        latch.await(90, TimeUnit.SECONDS)
        if (latch.count > 0) mainHandler.post { finishGeminiPlayback(utteranceId, latch, "STOPPED") }
    }

    private fun finishGeminiPlayback(utteranceId: String, latch: CountDownLatch, status: String, reason: String? = null) {
        runCatching { activePlayer?.stop() }
        runCatching { activePlayer?.release() }
        activePlayer = null
        activeWav?.delete(); activeWav = null
        activePlaybackLatch = null
        pendingUtterances.remove(utteranceId)
        latch.countDown(); emitEvent(status, utteranceId, "GEMINI", reason)
        if (pendingUtterances.isEmpty()) onSpeakingChanged(false)
    }

    fun stop(): JSONObject { stopInternal(incrementGeneration = true); return JSONObject().put("status", "OK").put("mechanicalMutation", false) }

    private fun stopInternal(incrementGeneration: Boolean) {
        if (incrementGeneration) generation.incrementAndGet()
        pendingUtterances.clear(); activeSessionId = null; onSpeakingChanged(false)
        activePlaybackLatch?.countDown(); activePlaybackLatch = null
        mainHandler.post {
            runCatching { activePlayer?.stop() }; runCatching { activePlayer?.release() }; activePlayer = null
            activeWav?.delete(); activeWav = null
            engine?.stop()
        }
    }

    fun shutdown() {
        stopInternal(incrementGeneration = true)
        geminiExecutor.shutdownNow()
        mainHandler.post { engine?.shutdown(); engine = null; state = "SHUTDOWN" }
    }

    private fun completeAndroid(utteranceId: String?, status: String, reason: String? = null) {
        if (utteranceId != null) pendingUtterances.remove(utteranceId)
        emitEvent(status, utteranceId, "ANDROID", reason)
        if (pendingUtterances.isEmpty() && activePlayer == null) { activeSessionId = null; onSpeakingChanged(false) }
    }

    private fun emitEvent(status: String, utteranceId: String?, provider: String, reason: String? = null) {
        val event=JSONObject().put("status",status).put("utteranceId",utteranceId ?: JSONObject.NULL).put("provider",provider).put("sessionId",activeSessionId ?: JSONObject.NULL)
        if (!reason.isNullOrBlank()) event.put("reason",reason.take(240))
        runCatching { onTtsEvent(event) }
    }

    private fun defaultStyleForRole(role: String): String = when (role) {
        "NPC" -> "Fale em português do Brasil, natural e expressivo, preservando exatamente o texto."
        else -> "Narre em português do Brasil como Mestre de RPG: voz clara, sombria e cinematográfica, ritmo natural, sem acrescentar nem remover palavras do texto."
    }

    private fun writeWav(output: FileOutputStream, pcm: ByteArray, sampleRate: Int, channels: Int, bitsPerSample: Int) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII)); header.putInt(36 + pcm.size)
        header.put("WAVE".toByteArray(Charsets.US_ASCII)); header.put("fmt ".toByteArray(Charsets.US_ASCII)); header.putInt(16)
        header.putShort(1); header.putShort(channels.toShort()); header.putInt(sampleRate); header.putInt(byteRate); header.putShort(blockAlign.toShort()); header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII)); header.putInt(pcm.size)
        output.write(header.array()); output.write(pcm)
    }

    companion object {
        private val PT_BR = Locale("pt", "BR")
        private const val MAX_TEXT_CHARS = 1_200
    }
}
