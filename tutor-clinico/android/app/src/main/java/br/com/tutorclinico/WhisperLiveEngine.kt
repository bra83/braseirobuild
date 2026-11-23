package br.com.tutorclinico

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import dev.ffmpegkit.whisper.Whisper
import dev.ffmpegkit.whisper.WhisperConfig
import dev.ffmpegkit.whisper.WhisperModel
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Experimental microphone-to-Whisper pipeline. AudioRecord captures mono 16kHz PCM
 * in a dedicated thread, a separate worker transcribes bounded ~18s audio chunks.
 *
 * The queue has a HARD cap of two chunks: overload ends recording with an error
 * instead of silently discarding speech or exhausting memory. All audio files are
 * private temporary WAV chunks and deleted in finally. No network or raw session recording.
 *
 * Critical constraint: offline Whisper may transcribe slower than real time on a
 * consumer phone. This code must be benchmarked on-device before clinical use.
 */
internal class WhisperLiveEngine(
    private val context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun state(text: String)
        fun segment(text: String, startMs: Long, endMs: Long)
        fun finished()
    }

    private data class Chunk(val pcm: ByteArray, val offsetMs: Long)
    private val queue = ArrayBlockingQueue<Chunk>(2)
    private val capturing = AtomicBoolean(false)
    private val running = AtomicBoolean(false)
    private val terminate = AtomicBoolean(false)
    @Volatile private var recorder: AudioRecord? = null
    @Volatile private var captureThread: Thread? = null
    @Volatile private var recognitionThread: Thread? = null

    companion object {
        private const val RATE = 16000
        private const val SEC = 18
        private const val MAX_BYTES = RATE * SEC * 2
        private const val CACHE_PREFIX = "eco_whisper_live_"
        @JvmStatic fun clearStaleFiles(context: Context) {
            context.cacheDir.listFiles()?.filter {
                it.isFile && it.name.startsWith(CACHE_PREFIX)
            }?.forEach { it.delete() }
        }
    }

    fun isBusy(): Boolean = running.get()

    @Synchronized fun start(modelName: String) {
        if (running.get()) return
        queue.clear()
        terminate.set(false)
        capturing.set(true)
        running.set(true)
        val asset = if (modelName == "small") "whisper/ggml-small-q5_1.bin"
                    else "whisper/ggml-base.bin"
        val display = if (modelName == "small") "WHISPER SMALL Q5" else "WHISPER BASE"
        recognitionThread = Thread({
            var model: WhisperModel? = null
            try {
                listener.state("MICROFONE · CARREGANDO " + display + " OFFLINE")
                model = runBlocking { Whisper.loadModelFromAsset(context, asset) }
                if (!capturing.get()) return@Thread
                launchCapture()
                listener.state("GRAVANDO · " + display + " · TRANSCRIÇÃO EM TRECHOS")
                var processed = 0
                while (!terminate.get()) {
                    val chunk = queue.poll(250, TimeUnit.MILLISECONDS)
                    if (chunk == null) {
                        if (!capturing.get() && captureThread?.isAlive != true) break
                        continue
                    }
                    if (!containsSpeech(chunk.pcm)) {
                        listener.state("MICROFONE · SILÊNCIO IGNORADO · " + processed + " TRECHOS")
                        continue
                    }
                    processed++
                    val file = File.createTempFile(CACHE_PREFIX, ".wav", context.cacheDir)
                    try {
                        createWav(file, chunk.pcm)
                        listener.state("MICROFONE · " + display + " · PROCESSANDO TRECHO " + processed +
                            " · " + (chunk.offsetMs / 1000) + "s")
                        val result = runBlocking {
                            Whisper.transcribe(model!!, file.absolutePath,
                                WhisperConfig(language = "pt", translate = false, threads = 4))
                        }
                        if (terminate.get()) break
                        if (result.segments.isNotEmpty()) {
                            result.segments.forEach { seg ->
                                if (seg.text.isNotBlank() && !terminate.get())
                                    listener.segment(seg.text.trim(), chunk.offsetMs + seg.startMs,
                                        chunk.offsetMs + seg.endMs)
                            }
                        } else if (result.text.isNotBlank()) {
                            listener.segment(result.text.trim(), chunk.offsetMs,
                                chunk.offsetMs + chunk.pcm.size * 1000L / (RATE * 2))
                        }
                    } finally {
                        if (!file.delete())
                            listener.state("AVISO: arquivo temporário pendente de limpeza")
                    }
                }
                if (!terminate.get()) listener.state("CONCLUÍDO · MICROFONE " + display + " · REVISE O TEXTO")
            } catch (e: Throwable) {
                listener.state("ERRO MICROFONE WHISPER: " + (e.message ?: e.javaClass.simpleName))
                capturing.set(false)
                stopRecorder()
            } finally {
                capturing.set(false)
                stopRecorder()
                model?.let { try { Whisper.releaseModel(it) } catch (_: Throwable) {} }
                queue.clear()
                running.set(false)
                listener.finished()
            }
        }, "eco-whisper-live-recognition")
        recognitionThread?.start()
    }

    private fun launchCapture() {
        val minBytes = AudioRecord.getMinBufferSize(
            RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBytes <= 0) throw IllegalStateException("Microfone PCM 16 kHz indisponível")
        val recording = AudioRecord.Builder()
            .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            .setAudioFormat(AudioFormat.Builder()
                .setSampleRate(RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build())
            .setBufferSizeInBytes(maxOf(minBytes * 2, 8192))
            .build()
        if (recording.state != AudioRecord.STATE_INITIALIZED) {
            recording.release()
            throw IllegalStateException("Falha ao inicializar AudioRecord")
        }
        recorder = recording
        recording.startRecording()
        if (recording.recordingState != AudioRecord.RECORDSTATE_RECORDING)
            throw IllegalStateException("Microfone não iniciou")
        captureThread = Thread({
            val buffer = ByteArray(8192)
            var frames = 0L
            val pending = ByteArrayOutputStream(MAX_BYTES + 8192)
            try {
                while (capturing.get() && !terminate.get()) {
                    val read = recording.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (read < 0) throw IllegalStateException("Falha na leitura do microfone: " + read)
                    if (read == 0) continue
                    pending.write(buffer, 0, read - read % 2)
                    while (pending.size() >= MAX_BYTES && capturing.get()) {
                        val all = pending.toByteArray()
                        val cut = findQuietCut(all)
                        val bytes = all.copyOfRange(0, cut)
                        pending.reset()
                        if (cut < all.size) pending.write(all, cut, all.size - cut)
                        val accepted = queue.offer(Chunk(bytes, frames * 1000L / RATE))
                        frames += cut / 2
                        if (!accepted) {
                            listener.state("ERRO MICROFONE: processamento mais lento que a gravação. Áudio NÃO foi descartado silenciosamente; capture em arquivo e importe-o.")
                            capturing.set(false)
                            break
                        }
                    }
                }
            } catch (e: Throwable) {
                if (capturing.get() && !terminate.get())
                    listener.state("ERRO MICROFONE: " + (e.message ?: e.javaClass.simpleName))
                capturing.set(false)
            } finally {
                // Drain final partial speech. If queue is already full we report the overflow.
                if (!terminate.get() && pending.size() >= RATE * 2) {
                    if (!queue.offer(Chunk(pending.toByteArray(), frames * 1000L / RATE)))
                        listener.state("ERRO MICROFONE: fila cheia; trecho final não transcrito")
                }
                stopRecorder()
            }
        }, "eco-whisper-live-capture")
        captureThread?.start()
    }

    /** Stop capture, but allow queued chunks to finish for text integrity. */
    @Synchronized fun finishRecording() {
        if (!running.get()) return
        capturing.set(false)
        stopRecorder()
        listener.state("FINALIZANDO MICROFONE · PROCESSANDO ÚLTIMOS TRECHOS")
    }

    /** Cancel and discard in-memory queued audio, e.g., on app destruction. */
    @Synchronized fun destroy() {
        terminate.set(true)
        capturing.set(false)
        queue.clear()
        stopRecorder()
    }

    private fun stopRecorder() {
        val rec = recorder ?: return
        recorder = null
        try { if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) rec.stop() }
        catch (_: Throwable) {}
        try { rec.release() } catch (_: Throwable) {}
    }

    private fun findQuietCut(data: ByteArray): Int {
        val from = (MAX_BYTES - RATE * 2 * 3).coerceAtLeast(0)
        val windowBytes = RATE * 2 / 10
        var best = data.size
        var quietest = Double.MAX_VALUE
        var pos = from
        while (pos + windowBytes < data.size) {
            var sum = 0L
            var p = pos
            while (p + 1 < pos + windowBytes) {
                val sample = ((data[p].toInt() and 255) or (data[p + 1].toInt() shl 8)).toShort().toInt()
                sum += kotlin.math.abs(sample).toLong()
                p += 2
            }
            val mean = sum.toDouble() / (windowBytes / 2)
            if (mean < quietest) { quietest = mean; best = pos + windowBytes / 2 }
            pos += windowBytes
        }
        return if (quietest < 350.0) best / 2 * 2 else data.size
    }

    private fun containsSpeech(pcm: ByteArray): Boolean {
        var audible = 0
        var pos = 0
        while (pos + 1 < pcm.size) {
            val sample = ((pcm[pos].toInt() and 255) or (pcm[pos + 1].toInt() shl 8)).toShort().toInt()
            if (kotlin.math.abs(sample) > 250) audible++
            if (audible >= RATE / 8) return true
            pos += 2
        }
        return false
    }

    private fun createWav(file: File, pcm: ByteArray) {
        val pad = RATE * 2 // 1s trailing silence
        val dataSize = pcm.size + pad
        val hdr = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        hdr.put("RIFF".toByteArray(Charsets.US_ASCII)); hdr.putInt(dataSize + 36)
        hdr.put("WAVE".toByteArray(Charsets.US_ASCII))
        hdr.put("fmt ".toByteArray(Charsets.US_ASCII)); hdr.putInt(16)
        hdr.putShort(1.toShort()); hdr.putShort(1.toShort())
        hdr.putInt(RATE); hdr.putInt(RATE * 2)
        hdr.putShort(2.toShort()); hdr.putShort(16.toShort())
        hdr.put("data".toByteArray(Charsets.US_ASCII)); hdr.putInt(dataSize)
        FileOutputStream(file).use { out ->
            out.write(hdr.array());out.write(pcm);out.write(ByteArray(pad))
        }
    }
}
