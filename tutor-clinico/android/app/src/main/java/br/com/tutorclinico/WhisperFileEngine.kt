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
    private const val MODEL_BASE = "whisper/ggml-base.bin"
    private const val MODEL_SMALL = "whisper/ggml-small-q5_1.bin"
    private const val SAMPLE_RATE = 16000
    private const val PCM_BYTES_PER_SECOND = SAMPLE_RATE * 2
    private const val SEGMENT_SECONDS = 25
    private const val SEGMENT_SIZE = PCM_BYTES_PER_SECOND * SEGMENT_SECONDS
    private const val TRAILING_SILENCE_BYTES = PCM_BYTES_PER_SECOND
    private const val CACHE_PREFIX = "eco_whisper_audio_"

    interface Listener {
        fun isActive(): Boolean
        fun state(value: String)
        fun segment(value: String, startMs: Long, endMs: Long)
        fun segmentWithSpeaker(value: String, startMs: Long, endMs: Long, cluster: String)
        fun progress(processedMs: Long)
        fun acoustic(measurementJson: String)
        fun chunksProcessed(count: Int) {}
    }

    @JvmStatic
    fun cleanupAbandonedAudio(context: Context) {
        context.cacheDir.listFiles()?.filter {
            it.isFile && it.name.startsWith(CACHE_PREFIX)
        }?.forEach { it.delete() }
    }

    @JvmStatic
    fun transcribe(context: Context, uri: Uri, listener: Listener, requestedModel: String): Boolean =
        transcribe(context, uri, listener, requestedModel, 0L, null)

    @JvmStatic
    fun transcribe(context: Context, uri: Uri, listener: Listener, requestedModel: String,
                   resumeFromMs: Long): Boolean = transcribe(context,uri,listener,requestedModel,resumeFromMs,null)

    @JvmStatic
    fun transcribe(context: Context, uri: Uri, listener: Listener, requestedModel: String,
                   resumeFromMs: Long, clusters: SpeakerClusters?): Boolean {
        if (!listener.isActive()) return false
        var model: WhisperModel? = null
        var tagarela: TagarelaEngine? = null
        var diarizer: SegmentDiarizer? = null
        val mode = if(requestedModel=="tagarela")"tagarela" else if(requestedModel=="base")"base" else "small"
        val modelName = if(mode=="tagarela") "TAGARELA" else if(mode=="base") "WHISPER BASE" else "WHISPER SMALL Q5"
        val asset = if(mode=="base") MODEL_BASE else MODEL_SMALL
        try {
            listener.state(modelName+" · PREPARANDO MODELO OFFLINE")
            if(mode=="tagarela") tagarela = TagarelaEngine.getOrCreate(context) else model = runBlocking { Whisper.loadModelFromAsset(context, asset) }
            if (!listener.isActive()) return false

            // Skip already-committed PCM during resume. No duplicate segments are emitted.
            // The file is decoded again from the beginning, but Whisper starts at this offset.
            val skipFromMs = resumeFromMs.coerceAtLeast(0L)
            var toSkipBytes = skipFromMs * PCM_BYTES_PER_SECOND / 1000L
            if(clusters!=null)try{diarizer=SegmentDiarizer(context,clusters)}catch(e: Exception){listener.state("WHISPER · SEPARAÇÃO DE FALANTES INDISPONÍVEL")}
            val chunks = ChunkedPcmTranscriber(context, listener, model, modelName, skipFromMs,diarizer,tagarela)
            listener.state(modelName+" · LENDO ÁUDIO · RETOMADA "+(skipFromMs/1000L)+"s")
            val chunksCount = AudioFileTranscriber.streamPcmWithCount(context, uri, object : AudioFileTranscriber.Listener {
                override fun isActive() = listener.isActive()
                override fun status(message: String) {
                    if (listener.isActive()) listener.state(modelName+" · DECODIFICANDO ÁUDIO")
                }
                override fun segment(text: String) { /* no Vosk used on this path */ }
            }, AudioFileTranscriber.PcmListener { pcm, size ->
                if (toSkipBytes >= size) {
                    toSkipBytes -= size
                } else {
                    val offset = toSkipBytes.toInt()
                    toSkipBytes = 0L
                    val remaining = size - offset
                    if (remaining > 0) chunks.accept(pcm.copyOfRange(offset, size), remaining)
                }
            }, MlSessionEngine.getActiveEphemeralKey())
            listener.chunksProcessed(chunksCount)
            if (!listener.isActive()) return false
            chunks.finish()
            return listener.isActive()
        } catch (e: Throwable) {
            if (listener.isActive()) {
                val detail = e.message ?: e.javaClass.simpleName
                listener.state("ERRO "+modelName+": " + detail)
            }
            return false
        } finally {
            try {diarizer?.close()}catch(_:Throwable){}
            // Tagarela instance is preserved warm in TagarelaEngine.cachedInstance for instant reuse
            model?.let { try { Whisper.releaseModel(it) } catch (_: Throwable) { } }
        }
    }

    private class ChunkedPcmTranscriber(
        private val context: Context,
        private val listener: Listener,
        private val model: WhisperModel?,
        private val modelName: String,
        resumeFromMs: Long,
        private val diarizer: SegmentDiarizer?,
        private val tagarela: TagarelaEngine?
    ) {
        private val pcm = ByteArrayOutputStream(SEGMENT_SIZE + 8192)
        private val acoustic = AcousticMeter(resumeFromMs)
        private var chunkIndex = 0
        private val wallStart=android.os.SystemClock.elapsedRealtime()
        private val initialMs=resumeFromMs
        private var processedFrames = resumeFromMs.coerceAtLeast(0L) * SAMPLE_RATE / 1000L
        var recognizedCount = 0
            private set

        fun accept(data: ByteArray, size: Int) {
            if (size < 0 || size > data.size)
                throw IllegalArgumentException("Bloco de áudio inválido")
            acoustic.accept(data,size)
            for (feature in acoustic.drain()) {
                if(listener.isActive())listener.acoustic(feature)
            }
            var offset = 0
            while (offset < size && listener.isActive()) {
                val take = minOf(size - offset, SEGMENT_SIZE - pcm.size())
                pcm.write(data, offset, take)
                offset += take
                if (pcm.size() == SEGMENT_SIZE) transcribeChunk(false)
            }
        }

        fun finish() {
            if (listener.isActive() && pcm.size() > 0) transcribeChunk(true)
            if(listener.isActive()){
                acoustic.finish()
                for(feature in acoustic.drain())listener.acoustic(feature)
            }
        }

        // Energy-based pause detection (a conservative filter, NOT a neural VAD).
        // Prefer a quiet window at a sentence boundary near 25s rather than cutting
        // in the middle of an audible word. The remaining PCM stays in memory.
        private fun cutAtPause(data: ByteArray): Int {
            if(data.size < PCM_BYTES_PER_SECOND * 22) return data.size
            val win = PCM_BYTES_PER_SECOND / 10 // 100 ms
            var best = data.size
            var quietest = Double.MAX_VALUE
            val from = PCM_BYTES_PER_SECOND * 20
            val to = data.size - PCM_BYTES_PER_SECOND
            var pos = from
            while (pos + win <= to) {
                var sum = 0L
                var j = pos
                while (j + 1 < pos + win) {
                    val sample = ((data[j].toInt() and 255) or (data[j + 1].toInt() shl 8)).toShort().toInt()
                    sum += kotlin.math.abs(sample).toLong()
                    j += 2
                }
                val mean = sum.toDouble() / (win / 2)
                if (mean < quietest) {quietest = mean; best = pos + win / 2}
                pos += win
            }
            return if (quietest <= 350.0) (best / 2) * 2 else data.size
        }
        private fun containsSpeechLikeSignal(data: ByteArray): Boolean {
            var audible = 0
            var i = 0
            while (i + 1 < data.size) {
                val sample = ((data[i].toInt() and 255) or (data[i+1].toInt() shl 8)).toShort().toInt()
                if(kotlin.math.abs(sample) > 250) audible++
                if(audible >= SAMPLE_RATE / 8) return true // >=125 ms above threshold
                i += 2
            }
            return false
        }
        private fun transcribeChunk(finalChunk: Boolean) {
            if (!listener.isActive()) return
            val all = pcm.toByteArray()
            val cut = if(finalChunk)all.size else cutAtPause(all)
            if(cut == 0)return
            val bytes = all.copyOfRange(0,cut)
            pcm.reset()
            if(cut < all.size)pcm.write(all,cut,all.size-cut)
            val baseMs = processedFrames * 1000L / SAMPLE_RATE
            processedFrames += cut / 2L
            chunkIndex++
            if(!containsSpeechLikeSignal(bytes)){
                if(listener.isActive()){
                    listener.state(modelName+" · SILÊNCIO IGNORADO NO TRECHO "+chunkIndex)
                    listener.progress(processedFrames * 1000L / SAMPLE_RATE)
                }
                return
            }
            if(tagarela!=null){
                listener.state("TAGARELA · TRANSCRIBINDO "+(baseMs/1000)+"s")
                val voice=diarizer?.analyze(bytes,java.util.function.BooleanSupplier { listener.isActive() })
                if(!listener.isActive())return
                val spans=tagarela.decode(bytes)
                if(!listener.isActive())return
                for(s in spans){
                    recognizedCount++
                    listener.segmentWithSpeaker(s.text,baseMs+s.start,baseMs+s.end,voice?.label(s.start,s.end)?:"")
                }
                listener.progress(processedFrames*1000L/SAMPLE_RATE)
                val elapsed=(android.os.SystemClock.elapsedRealtime()-wallStart).coerceAtLeast(1L)
                val speed=(processedFrames*1000L/SAMPLE_RATE-initialMs).toDouble()/elapsed
                listener.state("TAGARELA · TRECHO "+chunkIndex+" CONCLUÍDO · "+String.format(java.util.Locale.US,"%.2fx",speed)+" TEMPO REAL")
                return
            }
            val wav = File.createTempFile(CACHE_PREFIX, ".wav", context.cacheDir)
            try {
                writeWav(wav, bytes)
                if (!listener.isActive()) return
                val offsetSec = baseMs / 1000L
                val clock = (offsetSec / 60L).toString() + "m" +
                    (offsetSec % 60L).toString().padStart(2, '0') + "s"
                listener.state(modelName+" · TRANSCRIBINDO " + clock + " · TRECHO " + chunkIndex)
                val result = runBlocking {
                    Whisper.transcribe(requireNotNull(model), wav.absolutePath,
                        WhisperConfig(language = "pt", translate = false, threads = 4))
                }
                if (!listener.isActive()) return
                val voiceTimeline=diarizer?.analyze(bytes,java.util.function.BooleanSupplier { listener.isActive() })
                if (!listener.isActive()) return
                if (result.segments.isNotEmpty()) {
                    for (seg in result.segments) {
                        if (listener.isActive() && seg.text.isNotBlank()) {
                            recognizedCount++
                            val maxMs = baseMs + bytes.size * 1000L / PCM_BYTES_PER_SECOND
                            val start=(baseMs+seg.startMs).coerceIn(baseMs,maxMs)
                            val end=(baseMs+seg.endMs).coerceIn(start,maxMs)
                            val cluster=voiceTimeline?.label(start-baseMs,end-baseMs)?:""
                            listener.segmentWithSpeaker(seg.text.trim(),start,end,cluster)
                        }
                    }
                } else if (result.text.isNotBlank()) {
                    recognizedCount++
                    val end=baseMs + bytes.size * 1000L / PCM_BYTES_PER_SECOND
                    listener.segmentWithSpeaker(result.text.trim(),baseMs,end,voiceTimeline?.label(0L,end-baseMs)?:"")
                }
                if (listener.isActive()) {
                    listener.progress(processedFrames * 1000L / SAMPLE_RATE)
                    val elapsed=(android.os.SystemClock.elapsedRealtime()-wallStart).coerceAtLeast(1L)
                    val speed=(processedFrames*1000L/SAMPLE_RATE-initialMs).toDouble()/elapsed
                    listener.state("WHISPER · TRECHO " + chunkIndex + " CONCLUÍDO · "+String.format(java.util.Locale.US,"%.2fx",speed)+" TEMPO REAL")
                }
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
