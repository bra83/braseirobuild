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
    private val client: GeminiInteractionsClient
) {
    enum class Result { STARTING, STOPPED, REPLAYING, MISSING_KEY, EMPTY_NARRATION }

    private val appContext = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var lastFile: File? = null
    private var lastNarration: String? = null

    fun playVisibleNarration(text: String): Result {
        if (text.isBlank()) return Result.EMPTY_NARRATION
        val key = credentials.get(GeminiCredentialStore.Slot.TTS_AUDIO) ?: return Result.MISSING_KEY
        val blob = client.generateTtsAudio(key, text)
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

    fun stop(): Result {
        main.post { player?.stop(); player?.release(); player = null }
        return Result.STOPPED
    }

    fun shutdown() {
        main.post { player?.release(); player = null }
    }

    private fun playFile(file: File) {
        main.post {
            player?.release()
            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener { it.release(); if (player === it) player = null }
                prepare()
                start()
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
