package br.com.tutorclinico

import android.content.Context
import android.net.Uri
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import dev.ffmpegkit.whisper.WhisperModel
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Android offline Whisper Base PT-BR transcription.
 *
 * Audio is decoded from its content URI incrementally through MediaCodec
 * (PCM WAV is parsed directly) into mono 16 kHz PCM. Only one 25-second
 * WAV segment at a time is written to private cache. Whisper processes that
 * short segment, it is deleted, and the next segment is decoded.
 *
 * The native Whisper JNI implementation decodes the entire supplied file
 * into memory. Giving it a full recording can terminate the Android process
 * with an out-of-memory error. Never pass full sessions to Whisper.
 *
 * No network or cloud. No copy of the full source recording.
 */
object WhisperFileEngine {
    private const val MODEL_ASSET = "whisper/ggml-base.bin"
    private const val SAMPLE_RATE = 16000
    private const val PCM_BYTES_PER_SECOND = SAMPLE_RATE * 2
    private const val SEGMENT_SECONDS = 25
    private const val SEGMENT_SIZE = PCM_BYTES_PER_SECOND * SEGMENT_SECONDS
    private const val TRAILING_SILENCE_BYTES = PCM_BYTES_PER_SECOND
    private const val CACHE_PREFIX = "eco_whisper_audio_"

    interface Listener {
        fun isActive(): Boolean
        fun state(value: String)
        fun segment(value: String)
    }

    @JvmStatic
    fun cleanupAbandonedAudio(context: Context) {
        context.cacheDir.listFiles()?.filter {
            it.isFile && it.name.startsWith(CACHE_PREFIX)
        }?.forEach { it.delete() }
    }

    @JvmStatic
    fun transcribe(context: Context, uri: Uri, listener: Listener) {
        if (!listener.isActive()) return
        var model: WhisperModel? = null
        try {
            listener.state("WHISPER · PREPARANDO MODELO OFFLINE")
            model = runBlocking { Whisper.loadModelFromAsset(context, MODEL_ASSET) }
            if (!listener.isActive()) return

            val chunks = ChunkedPcmTranscriber(context, listener, model)
            listener.state("WHISPER · LENDO ÁUDIO DIRETAMENTE DO ARQUIVO")
            AudioFileTranscriber.streamPcm(context, uri, object : AudioFileTranscriber.Listener {
                override fun isActive() = listener.isActive()
                override fun status(message: String) {
                    if (listener.isActive()) listener.state("WHISPER · DECODIFICANDO ÁUDIO")
                }
                override fun segment(text: String) { /* no Vosk used on this path */ }
            }, AudioFileTranscriber.PcmListener { pcm, size ->
                chunks.accept(pcm, size)
            })
            if (!listener.isActive()) return
            chunks.finish()
            if (listener.isActive()) {
                listener.state(if (chunks.recognizedCount > 0)
                    "CONCLUÍDO · WHISPER BASE · REVISE A TRANSCRIÇÃO"
                    else "CONCLUÍDO · NENHUMA FALA IDENTIFICADA")
            }
        } catch (e: Throwable) {
            if (listener.isActive()) {
                val detail = e.message ?: e.javaClass.simpleName
                listener.state("ERRO WHISPER: " + detail)
            }
        } finally {
            model?.let { try { Whisper.releaseModel(it) } catch (_: Throwable) { } }
        }
    }

    private class ChunkedPcmTranscriber(
        private val context: Context,
        private val listener: Listener,
        private val model: WhisperModel
    ) {
        private val pcm = ByteArrayOutputStream(SEGMENT_SIZE + 8192)
        private var chunkIndex = 0
        var recognizedCount = 0
            private set

        fun accept(data: ByteArray, size: Int) {
            if (size < 0 || size > data.size)
                throw IllegalArgumentException("Bloco de áudio inválido")
            var offset = 0
            while (offset < size && listener.isActive()) {
                val take = minOf(size - offset, SEGMENT_SIZE - pcm.size())
                pcm.write(data, offset, take)
                offset += take
                if (pcm.size() == SEGMENT_SIZE) transcribeChunk()
            }
        }

        fun finish() {
            if (listener.isActive() && pcm.size() > 0) transcribeChunk()
        }

        private fun transcribeChunk() {
            if (!listener.isActive()) return
            val bytes = pcm.toByteArray()
            pcm.reset()
            if (bytes.isEmpty()) return
            chunkIndex++
            val wav = File.createTempFile(CACHE_PREFIX, ".wav", context.cacheDir)
            try {
                writeWav(wav, bytes)
                if (!listener.isActive()) return
                val offsetSec = (chunkIndex - 1) * SEGMENT_SECONDS
                val clock = (offsetSec / 60).toString() + "m" +
                    (offsetSec % 60).toString().padStart(2, '0') + "s"
                listener.state("WHISPER BASE · TRANSCRIBINDO " + clock + " · TRECHO " + chunkIndex)
                val result = runBlocking {
                    Whisper.transcribe(model, wav.absolutePath,
                        WhisperConfig(language = "pt", translate = false, threads = 4))
                }
                if (!listener.isActive()) return
                if (result.segments.isNotEmpty()) {
                    for (seg in result.segments) {
                        if (listener.isActive() && seg.text.isNotBlank()) {
                            recognizedCount++
                            listener.segment(seg.text.trim())
                        }
                    }
                } else if (result.text.isNotBlank()) {
                    recognizedCount++
                    listener.segment(result.text.trim())
                }
                if (listener.isActive())
                    listener.state("WHISPER · TRECHO " + chunkIndex + " CONCLUÍDO · CONTINUANDO")
            } finally {
                if (wav.exists() && !wav.delete())
                    listener.state("AVISO: limpeza de trecho temporário pendente")
            }
        }

        private fun writeWav(file: File, audio: ByteArray) {
            val dataSize = audio.size + TRAILING_SILENCE_BYTES
            val riff = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            riff.put("RIFF".toByteArray(Charsets.US_ASCII))
            riff.putInt(dataSize + 36)
            riff.put("WAVE".toByteArray(Charsets.US_ASCII))
            riff.put("fmt ".toByteArray(Charsets.US_ASCII))
            riff.putInt(16)
            riff.putShort(1) // PCM
            riff.putShort(1) // Mono
            riff.putInt(SAMPLE_RATE)
            riff.putInt(PCM_BYTES_PER_SECOND)
            riff.putShort(2)
            riff.putShort(16)
            riff.put("data".toByteArray(Charsets.US_ASCII))
            riff.putInt(dataSize)
            FileOutputStream(file).use { output ->
                output.write(riff.array())
                output.write(audio)
                output.write(ByteArray(TRAILING_SILENCE_BYTES))
            }
        }
    }
}
