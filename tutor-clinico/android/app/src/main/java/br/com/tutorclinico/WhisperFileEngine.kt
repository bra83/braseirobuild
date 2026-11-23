package br.com.tutorclinico

import android.content.Context
import android.net.Uri
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import dev.ffmpegkit.whisper.WhisperModel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream

/**
 * Offline Whisper Base recognition of imported audio. A private temporary copy
 * exists only while the file is being transcribed and is deleted in finally.
 */
object WhisperFileEngine {
    private const val MAX_BYTES = 650L * 1024 * 1024
    private const val MODEL_ASSET = "whisper/ggml-base.bin"
    interface Listener {
        fun isActive(): Boolean
        fun state(value: String)
        fun segment(value: String)
    }
    @JvmStatic
    fun cleanupAbandonedAudio(context: Context) {
        context.cacheDir.listFiles()?.filter {
            it.name.startsWith("eco_whisper_audio_")
        }?.forEach { it.delete() }
    }
    @JvmStatic
    fun transcribe(context: Context, uri: Uri, listener: Listener) {
        if (!listener.isActive()) return
        var temp: File? = null
        var model: WhisperModel? = null
        try {
            listener.state("WHISPER · PREPARANDO ARQUIVO")
            val mime = context.contentResolver.getType(uri).orEmpty()
            val ext = when {
                mime.contains("flac", ignoreCase = true) -> ".flac"
                mime.contains("wav", ignoreCase = true) -> ".wav"
                else -> ".mp3"
            }
            val file = File.createTempFile("eco_whisper_audio_", ext, context.cacheDir)
            temp = file
            var total = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    while (listener.isActive()) {
                        val bytes = input.read(buffer)
                        if (bytes < 0) break
                        total += bytes
                        if (total > MAX_BYTES) throw IllegalArgumentException("Arquivo acima de 650 MB")
                        output.write(buffer, 0, bytes)
                    }
                    output.fd.sync()
                }
            } ?: throw IllegalArgumentException("Não foi possível abrir o áudio")
            if (!listener.isActive()) return
            if (total == 0L) throw IllegalArgumentException("Arquivo vazio")
            listener.state("WHISPER BASE · CARREGANDO MODELO OFFLINE")
            model = runBlocking { Whisper.loadModelFromAsset(context, MODEL_ASSET) }
            if (!listener.isActive()) return
            listener.state("WHISPER BASE · TRANSCRIBINDO OFFLINE")
            val result = runBlocking {
                Whisper.transcribe(
                    model!!,
                    file.absolutePath,
                    WhisperConfig(language = "pt", translate = false, threads = 4)
                )
            }
            if (!listener.isActive()) return
            if (result.segments.isNotEmpty()) {
                for (segment in result.segments) {
                    if (listener.isActive() && segment.text.isNotBlank())
                        listener.segment(segment.text.trim())
                }
            } else if (result.text.isNotBlank()) listener.segment(result.text)
            if (listener.isActive()) listener.state("CONCLUÍDO · WHISPER BASE · REVISE A TRANSCRIÇÃO")
        } catch (e: Exception) {
            if (listener.isActive()) listener.state("ERRO WHISPER: " + (e.message ?: e.javaClass.simpleName))
        } finally {
            model?.let { try { Whisper.releaseModel(it) } catch (_: Exception) {} }
            temp?.let {
                if (it.exists() && !it.delete())
                    listener.state("AVISO: limpeza do arquivo temporário pendente")
            }
        }
    }
}
