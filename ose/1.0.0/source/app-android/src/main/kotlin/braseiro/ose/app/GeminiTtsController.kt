package braseiro.ose.app

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class GeminiTtsController(
    context: Context,
    private val credentials: GeminiCredentialStore,
    private val settings: GeminiRuntimeSettings,
    private val client: GeminiInteractionsClient
) {
    enum class Result { STARTING, STOPPED, REPLAYING, MISSING_KEY, EMPTY_NARRATION, ERROR }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var lastFile: File? = null
    private var lastNarration: String? = null
    @Volatile var onState: ((Result, String?) -> Unit)? = null

    fun playVisibleNarration(text: String): Result {
        if (text.isBlank()) return Result.EMPTY_NARRATION
        val key = credentials.get(GeminiCredentialStore.Slot.TTS_AUDIO) ?: return Result.MISSING_KEY
        val runtime = settings.snapshot()
        val blob = client.generateTtsAudio(key, runtime.ttsModel, runtime.ttsVoice, text)
        val output = File(appContext.cacheDir, "gemini-visible-narration.wav")
        val bytes = if (blob.mimeType.contains("wav", ignoreCase = true)) blob.data else pcmToWav(blob.data, blob.sampleRate ?: 24000)
        output.writeBytes(bytes)
        lastFile = output
        lastNarration = text
        playFile(output)
        return Result.STARTING
    }

    fun replayVisibleNarration(currentText: String): Result {
        if (currentText.isBlank()) return Result.EMPTY_NARRATION
        val file = lastFile?.takeIf { it.isFile }
        if (file != null && lastNarration == currentText) {
            playFile(file)
            return Result.REPLAYING
        }
        return playVisibleNarration(currentText)
    }

    fun playTestPhrase(): Result = playVisibleNarration("Teste de voz do Mestre Bárbara. Se você está ouvindo esta frase, o TTS está funcionando.")

    fun stop(): Result {
        main.post {
            runCatching { player?.stop() }
            player?.release(); player = null
            onState?.invoke(Result.STOPPED, null)
        }
        return Result.STOPPED
    }

    fun shutdown() {
        main.post { player?.release(); player = null }
    }

    private fun playFile(file: File) {
        main.post {
            try {
                player?.release()
                player = MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    setOnPreparedListener { media ->
                        media.start()
                        onState?.invoke(Result.STARTING, null)
                    }
                    setOnCompletionListener { media ->
                        media.release(); if (player === media) player = null
                        onState?.invoke(Result.STOPPED, null)
                    }
                    setOnErrorListener { media, what, extra ->
                        media.release(); if (player === media) player = null
                        onState?.invoke(Result.ERROR, "MEDIA_PLAYER_${what}_$extra")
                        true
                    }
                    prepareAsync()
                }
            } catch (t: Throwable) {
                player?.release(); player = null
                onState?.invoke(Result.ERROR, t.message ?: t::class.java.simpleName)
            }
        }
    }

    private fun pcmToWav(pcm: ByteArray, sampleRate: Int): ByteArray {
        val out = ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN)
        out.put("RIFF".toByteArray(Charsets.US_ASCII)); out.putInt(36 + pcm.size)
        out.put("WAVEfmt ".toByteArray(Charsets.US_ASCII)); out.putInt(16); out.putShort(1); out.putShort(1)
        out.putInt(sampleRate); out.putInt(sampleRate * 2); out.putShort(2); out.putShort(16)
        out.put("data".toByteArray(Charsets.US_ASCII)); out.putInt(pcm.size); out.put(pcm)
        return out.array()
    }
}
