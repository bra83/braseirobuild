package br.com.tutorclinico;

import android.content.Context;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Reads an audio document through Android's Storage Access Framework without writing a copy.
 * Decodes compressed audio using MediaCodec; decodes PCM WAV directly.
 * Downmixes to mono, resamples to 16kHz, and streams frames into a real Vosk Recognizer.
 * No file bytes or unencrypted transcripts are persisted by this class.
 */
final class AudioFileTranscriber {
    static final long MAX_DURATION_US = 120L * 60L * 1000000L;
    static final long MAX_FILE_BYTES = 650L * 1024L * 1024L;
    private static final int TARGET_RATE = 16000;
    interface Listener {
        boolean isActive();
        void status(String message);
        void segment(String text);
    }
    private AudioFileTranscriber() {}

    static void transcribe(Context context, Uri uri, Model model, Listener listener) throws Exception {
        if (!listener.isActive()) return;
        listener.status("LENDO ARQUIVO · ÁUDIO LOCAL");
        try (Recognizer recognizer = new Recognizer(model, TARGET_RATE)) {
            Sink sink = new Sink(recognizer, listener);
            boolean wav;
            try (InputStream probe = context.getContentResolver().openInputStream(uri)) {
                if (probe == null) throw new IllegalArgumentException("Arquivo inacessível");
                byte[] hdr = new byte[12];
                int n = 0;
                while (n < hdr.length) {
                    int k = probe.read(hdr, n, hdr.length - n);
                    if (k < 0) break;
                    n += k;
                }
                wav = n == 12 && ascii(hdr, 0, 4).equals("RIFF") && ascii(hdr, 8, 4).equals("WAVE");
            }
            if (wav) decodeWav(context, uri, sink, listener);
            else decodeCompressed(context, uri, sink, listener);
            if (listener.isActive()) {
                sink.flush();
                listener.status(sink.segments > 0
                        ? "CONCLUÍDO · REVISE A TRANSCRIÇÃO"
                        : "CONCLUÍDO · NENHUMA FALA IDENTIFICADA");
            }
        }
    }

    private static String ascii(byte[] b, int p, int n) {
        return new String(b, p, n, StandardCharsets.US_ASCII);
    }
    private static int le16(byte[] b, int p) {
        return (b[p] & 255) | ((b[p + 1] & 255) << 8);
    }
    private static long le32(byte[] b, int p) {
        return ((long) b[p] & 255L) | (((long) b[p + 1] & 255L) << 8)
                | (((long) b[p + 2] & 255L) << 16) | (((long) b[p + 3] & 255L) << 24);
    }
    private static void skipFully(InputStream in, long amount) throws Exception {
        byte[] buffer = new byte[8192];
        while (amount > 0) {
            int n = in.read(buffer, 0, (int) Math.min(buffer.length, amount));
            if (n < 0) throw new IllegalArgumentException("Arquivo WAV incompleto");
            amount -= n;
        }
    }

    private static void decodeWav(Context ctx, Uri uri, Sink sink, Listener listener) throws Exception {
        try (InputStream input = new BufferedInputStream(ctx.getContentResolver().openInputStream(uri))) {
            if (input == null) throw new IllegalArgumentException("Arquivo WAV não disponível");
            byte[] top = new byte[12];
            new DataInputStream(input).readFully(top);
            int audioFormat = -1, channels = -1, rate = -1, bits = -1;
            boolean foundData = false;
            long remaining = 0;
            byte[] h = new byte[8];
            while (listener.isActive()) {
                int first = input.read();
                if (first == -1) break;
                h[0] = (byte) first;
                new DataInputStream(input).readFully(h, 1, 7);
                long len = le32(h, 4);
                if (len > MAX_FILE_BYTES) throw new IllegalArgumentException("WAV acima de 650 MB");
                String chunk = ascii(h, 0, 4);
                if ("fmt ".equals(chunk)) {
                    if (len < 16 || len > 4096) throw new IllegalArgumentException("Cabeçalho WAV inválido");
                    byte[] format = new byte[(int) len];
                    new DataInputStream(input).readFully(format);
                    audioFormat = le16(format, 0);
                    channels = le16(format, 2);
                    rate = (int) le32(format, 4);
                    bits = le16(format, 14);
                } else if ("data".equals(chunk)) {
                    if (rate <= 0) throw new IllegalArgumentException("WAV sem informações de áudio");
                    remaining = len;
                    foundData = true;
                    break;
                } else {
                    skipFully(input, len);
                }
                if ((len & 1L) != 0) skipFully(input, 1);
            }
            if (!foundData) throw new IllegalArgumentException("WAV sem bloco de áudio");
            if (!(audioFormat == 1 && (bits == 8 || bits == 16 || bits == 24 || bits == 32))
                    && !(audioFormat == 3 && bits == 32))
                throw new IllegalArgumentException("WAV: use PCM de 8/16/24/32 bits ou float 32 bits");
            if (channels < 1 || channels > 2 || rate < 8000 || rate > 96000)
                throw new IllegalArgumentException("WAV: canais ou frequência não suportados");
            int frameBytes = (bits / 8) * channels;
            if (remaining / frameBytes > (MAX_DURATION_US * rate / 1000000L))
                throw new IllegalArgumentException("Áudio excede 120 minutos");
            Listener l = listener;
            Resampler resample = new Resampler(rate, sink, l);
            byte[] buffer = new byte[32768];
            int spare = 0;
            long readBytes = 0;
            long lastNotify = 0;
            while (remaining > 0 && l.isActive()) {
                int n = input.read(buffer, spare, (int) Math.min(buffer.length - spare, remaining));
                if (n < 0) break;
                if (n == 0) continue;
                remaining -= n;
                readBytes += n;
                int usable = ((spare + n) / frameBytes) * frameBytes;
                ByteBuffer b = ByteBuffer.wrap(buffer, 0, usable).order(ByteOrder.LITTLE_ENDIAN);
                resample.consume(b, channels, bits, audioFormat == 3);
                spare = spare + n - usable;
                if (spare > 0) System.arraycopy(buffer, usable, buffer, 0, spare);
                if (readBytes - lastNotify > 3000000) {
                    lastNotify = readBytes;
                    l.status("PROCESSANDO ÁUDIO · " + (readBytes / 1024 / 1024) + " MB");
                }
            }
            if (remaining > 0 && l.isActive()) throw new IllegalArgumentException("Arquivo WAV truncado");
            if (l.isActive()) resample.finish();
        }
    }

    private static void decodeCompressed(Context ctx, Uri uri, Sink sink, Listener listener) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        boolean codecStarted = false;
        try {
            extractor.setDataSource(ctx, uri, null);
            int track = -1;
            MediaFormat trackFmt = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat candidate = extractor.getTrackFormat(i);
                String mime = candidate.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    track = i; trackFmt = candidate; break;
                }
            }
            if (track < 0 || trackFmt == null) throw new IllegalArgumentException("Não foi encontrada trilha de áudio");
            String mime = trackFmt.getString(MediaFormat.KEY_MIME);
            if (mime == null || "audio/raw".equals(mime))
                throw new IllegalArgumentException("Formato PCM não reconhecido; use WAV PCM");
            if (trackFmt.containsKey(MediaFormat.KEY_DURATION)
                    && trackFmt.getLong(MediaFormat.KEY_DURATION) > MAX_DURATION_US)
                throw new IllegalArgumentException("Áudio excede 120 minutos");
            extractor.selectTrack(track);
            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(trackFmt, null, null, 0);
            codec.start();
            codecStarted = true;
            int rate = trackFmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                    ? trackFmt.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
            int channels = trackFmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                    ? trackFmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
            int pcmType = AudioFormat.ENCODING_PCM_16BIT;
            Resampler resample = new Resampler(rate, sink, listener);
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false, outputDone = false;
            long previousNotice = 0;
            while (!outputDone && listener.isActive()) {
                if (!inputDone) {
                    int inId = codec.dequeueInputBuffer(10000);
                    if (inId >= 0) {
                        ByteBuffer in = codec.getInputBuffer(inId);
                        if (in == null) throw new IllegalStateException("Decodificador sem buffer de entrada");
                        in.clear();
                        int size = extractor.readSampleData(in, 0);
                        if (size < 0) {
                            codec.queueInputBuffer(inId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            long time = Math.max(0, extractor.getSampleTime());
                            if (time > MAX_DURATION_US) throw new IllegalArgumentException("Áudio excede 120 minutos");
                            codec.queueInputBuffer(inId, 0, size, time, 0);
                            extractor.advance();
                            if (time - previousNotice >= 2000000L) {
                                previousNotice = time;
                                listener.status("PROCESSANDO ÁUDIO · " + (time / 1000000) + " s");
                            }
                        }
                    }
                }
                int outId = codec.dequeueOutputBuffer(info, 10000);
                if (outId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat fmt = codec.getOutputFormat();
                    rate = fmt.containsKey(MediaFormat.KEY_SAMPLE_RATE)
                            ? fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE) : rate;
                    channels = fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT)
                            ? fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : channels;
                    pcmType = fmt.containsKey(MediaFormat.KEY_PCM_ENCODING)
                            ? fmt.getInteger(MediaFormat.KEY_PCM_ENCODING) : AudioFormat.ENCODING_PCM_16BIT;
                    if (pcmType != AudioFormat.ENCODING_PCM_16BIT
                            && pcmType != AudioFormat.ENCODING_PCM_FLOAT)
                        throw new IllegalArgumentException("Decodificador retornou PCM não suportado");
                    resample.finish();
                    resample = new Resampler(rate, sink, listener);
                } else if (outId >= 0) {
                    if (info.size > 0) {
                        ByteBuffer out = codec.getOutputBuffer(outId);
                        if (out == null) throw new IllegalStateException("Decodificador sem buffer de saída");
                        out.order(ByteOrder.LITTLE_ENDIAN);
                        out.position(info.offset);
                        out.limit(info.offset + info.size);
                        resample.consume(out.slice().order(ByteOrder.LITTLE_ENDIAN), channels,
                                pcmType == AudioFormat.ENCODING_PCM_FLOAT ? 32 : 16,
                                pcmType == AudioFormat.ENCODING_PCM_FLOAT);
                    }
                    outputDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    codec.releaseOutputBuffer(outId, false);
                }
            }
            if (listener.isActive()) resample.finish();
        } finally {
            if (codec != null) {
                try { if (codecStarted) codec.stop(); } catch (Exception ignored) {}
                try { codec.release(); } catch (Exception ignored) {}
            }
            extractor.release();
        }
    }

    /** Streaming mono downmix + linear resampling, with continuity across decoder buffers. */
    private static final class Resampler {
        private final int sourceRate;
        private final Sink sink;
        private final Listener listener;
        private long inFrame, outFrame;
        private float previous;
        private boolean hasPrevious;
        private final byte[] pcm = new byte[8192];
        private int bytes;
        Resampler(int rate, Sink sink, Listener listener) {
            if (rate < 8000 || rate > 96000) throw new IllegalArgumentException("Frequência não suportada");
            sourceRate = rate; this.sink = sink; this.listener = listener;
        }
        void consume(ByteBuffer buffer, int channels, int bits, boolean isFloat) throws Exception {
            if (channels < 1 || channels > 2) throw new IllegalArgumentException("Suporta áudio mono/estéreo");
            int sampleBytes = bits / 8;
            if (!((bits == 8 || bits == 16 || bits == 24 || bits == 32) && sampleBytes > 0))
                throw new IllegalArgumentException("Amostra PCM não suportada");
            int frameBytes = channels * sampleBytes;
            while (buffer.remaining() >= frameBytes && listener.isActive()) {
                float sum = 0;
                for (int ch = 0; ch < channels; ch++) {
                    float val;
                    if (isFloat) {
                        val = buffer.getFloat();
                        if (!Float.isFinite(val)) val = 0;
                    } else if (bits == 8) {
                        val = (buffer.get() & 255) / 128.0f - 1.0f;
                    } else if (bits == 16) {
                        val = buffer.getShort() / 32768.0f;
                    } else if (bits == 24) {
                        int a = buffer.get() & 255, b = buffer.get() & 255, c = buffer.get();
                        int sample = a | (b << 8) | (c << 16);
                        val = sample / 8388608.0f;
                    } else {
                        val = buffer.getInt() / 2147483648.0f;
                    }
                    sum += val;
                }
                sample(sum / channels);
            }
        }
        private void sample(float now) throws Exception {
            if (!hasPrevious) {
                previous = now;
                hasPrevious = true;
                inFrame = 0;
                write(now);
                outFrame = 1;
                return;
            }
            inFrame++;
            while (outFrame * (long) sourceRate <= inFrame * (long) TARGET_RATE
                    && listener.isActive()) {
                double position = (outFrame * (double) sourceRate) / TARGET_RATE;
                float t = (float) (position - (inFrame - 1));
                write(previous + t * (now - previous));
                outFrame++;
                if (outFrame > 120L * 60L * TARGET_RATE)
                    throw new IllegalArgumentException("Áudio excede 120 minutos");
            }
            previous = now;
        }
        private void write(float x) throws Exception {
            int v = Math.round(Math.max(-1f, Math.min(1f, x)) * 32767f);
            pcm[bytes++] = (byte) v;
            pcm[bytes++] = (byte) (v >>> 8);
            if (bytes == pcm.length) flush();
        }
        private void flush() throws Exception {
            if (bytes > 0) { sink.feed(pcm, bytes); bytes = 0; }
        }
        void finish() throws Exception { if (listener.isActive()) flush(); }
    }

    private static final class Sink {
        private final Recognizer recognizer;
        private final Listener listener;
        private int segments;
        Sink(Recognizer rec, Listener listener) { recognizer = rec; this.listener = listener; }
        void feed(byte[] pcm, int size) {
            if (!listener.isActive()) return;
            if (recognizer.acceptWaveForm(pcm, size)) emit(recognizer.getResult());
        }
        void flush() { if (listener.isActive()) emit(recognizer.getFinalResult()); }
        private void emit(String json) {
            try {
                String text = new JSONObject(json).optString("text", "").trim();
                if (!text.isEmpty() && listener.isActive()) { segments++; listener.segment(text); }
            } catch (Exception ignored) {}
        }
    }
}
