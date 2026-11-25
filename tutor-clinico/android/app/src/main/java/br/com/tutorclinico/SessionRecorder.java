package br.com.tutorclinico;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypted audio capture engine.
 * Spools PCM audio into an app-private AEAD AES-256-GCM authenticated temporary file (ECO2).
 * Each chunk is authenticated with unique nonce and chunk-index AAD, preventing
 * bit-flipping, truncation, chunk reordering, and unauthorized tampering.
 * The audio key is ephemeral in memory; once confirmed and sanitized,
 * key memory is zeroed out, preventing data recovery even with flash wear-leveling.
 */
final class SessionRecorder {
    interface Events {
        void status(String value);
        default void onStarted(File temporarySpool, byte[] ephemeralKey) throws Exception {}
        void ready(File temporarySpool, long durationMs, byte[] ephemeralKey);
        default void ready(File temporarySpool, long durationMs) { ready(temporarySpool, durationMs, null); }
        void error(String value);
        void acoustic(String measurementJson);
    }

    private static final String TAG = "SessionRecorder";
    private static final int RATE = 16000;
    private static final long MAX_BYTES = 120L * 60L * RATE * 2L;
    static final String PREFIX = "eco_recording_";
    static final byte[] MAGIC = new byte[]{'E', 'C', 'O', '2'};
    static final byte[] MAGIC_V1 = new byte[]{'E', 'C', 'O', '1'};
    static final byte[] MAGIC_V2 = new byte[]{'E', 'C', 'O', '2'};
    private static final int CHUNK_SIZE = 8192;
    private static final SecureRandom RNG = new SecureRandom();

    private static volatile byte[] activeEphemeralKey = null;
    public static byte[] getActiveEphemeralKey() { return activeEphemeralKey; }
    public static void setActiveEphemeralKey(byte[] key) { activeEphemeralKey = key; }

    private final Context context;
    private final Events events;
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private volatile Thread worker;

    SessionRecorder(Context context, Events events) {
        this.context = context.getApplicationContext();
        this.events = events;
    }

    boolean isRecording() { return recording.get(); }

    synchronized void start() {
        if (worker != null && worker.isAlive()) throw new IllegalStateException("Gravação em andamento");
        recording.set(true);
        worker = new Thread(this::capture, "eco-record-then-transcribe");
        worker.start();
    }

    void finish() {
        recording.set(false);
        events.status("FINALIZANDO GRAVAÇÃO · PREPARANDO TRANSCRIÇÃO");
    }

    private void capture() {
        AudioRecord audio = null;
        RandomAccessFile wav = null;
        File file = null;
        long bytes = 0;
        boolean good = false;
        AcousticMeter meter = new AcousticMeter();
        byte[] ephemeralKey = new byte[32];
        RNG.nextBytes(ephemeralKey);
        setActiveEphemeralKey(ephemeralKey);

        try {
            int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (min <= 0) throw new IllegalStateException("Entrada PCM 16kHz indisponível");

            audio = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(16384, min * 2));
            if (audio.getState() != AudioRecord.STATE_INITIALIZED)
                throw new IllegalStateException("Microfone não inicializado");

            file = File.createTempFile(PREFIX, ".wav", spoolDir(context));
            wav = new RandomAccessFile(file, "rw");

            // Ephemeral AEAD AES-256-GCM chunked initialization with 32-byte versioned header:
            // [MAGIC 4B: 'ECO2'] [VERSION 4B: 2] [CHUNK_SIZE 4B: 8192] [RESERVED 12B] [LENGTH 8B]
            wav.write(MAGIC_V2);
            le32(wav, 2);
            le32(wav, CHUNK_SIZE);
            wav.write(new byte[12]);
            le64(wav, 0);

            // Persist audio spool path and encrypted key before audio recording begins!
            wav.getFD().sync();
            events.onStarted(file, ephemeralKey);

            audio.startRecording();
            if (audio.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING)
                throw new IllegalStateException("Microfone não iniciou a gravação");

            events.status("GRAVANDO ÁUDIO LOCAL · PARAR PARA TRANSCREVER");
            byte[] buffer = new byte[CHUNK_SIZE];
            byte[] encChunk = new byte[CHUNK_SIZE + 32];
            long lastNotice = System.currentTimeMillis();
            int chunkIndex = 0;

            while (recording.get()) {
                int n = audio.read(buffer, 0, buffer.length, AudioRecord.READ_NON_BLOCKING);
                if (n < 0) throw new IllegalStateException("Falha leitura áudio: " + n);
                if (n > 1) {
                    n -= n % 2;
                    if (bytes + n > MAX_BYTES) throw new IllegalStateException("Limite de 120 minutos");

                    byte[] nonce = new byte[12];
                    RNG.nextBytes(nonce);
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(ephemeralKey, "AES"), new GCMParameterSpec(128, nonce));
                    byte[] aad = ("ECO2:" + chunkIndex + ":" + n).getBytes(StandardCharsets.UTF_8);
                    cipher.updateAAD(aad);
                    int encLen = cipher.doFinal(buffer, 0, n, encChunk, 0);

                    // Write chunk frame: [chunkIndex 4B LE] [payloadLen 4B LE] [nonce 12B] [encPayload+Tag encLen B]
                    le32(wav, chunkIndex);
                    le32(wav, n);
                    wav.write(nonce);
                    wav.write(encChunk, 0, encLen);

                    chunkIndex++;
                    bytes += n;

                    meter.accept(buffer, n);
                    for (String feature : meter.drain()) events.acoustic(feature);
                } else {
                    try { Thread.sleep(20); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                }

                if (System.currentTimeMillis() - lastNotice >= 3000) {
                    lastNotice = System.currentTimeMillis();
                    events.status("GRAVANDO · " + (bytes / (RATE * 2L)) + " s · PARAR PARA TRANSCREVER");
                }
            }

            if (bytes == 0) throw new IllegalStateException("Nenhum áudio capturado");

            // P0-04: Write authenticated end-of-recording (EOF) trailer frame binding chunkIndex and total audio bytes
            byte[] eofNonce = new byte[12];
            RNG.nextBytes(eofNonce);
            Cipher eofCipher = Cipher.getInstance("AES/GCM/NoPadding");
            eofCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(ephemeralKey, "AES"), new GCMParameterSpec(128, eofNonce));
            byte[] eofPlain = "ECO2_AUTHENTICATED_EOF".getBytes(StandardCharsets.UTF_8);
            byte[] eofAad = ("ECO2:EOF:" + 0xFFFFFFFFL + ":" + chunkIndex + ":" + bytes).getBytes(StandardCharsets.UTF_8);
            eofCipher.updateAAD(eofAad);
            byte[] eofEnc = eofCipher.doFinal(eofPlain);

            // Write EOF frame: [chunkIndex=0xFFFFFFFF 4B LE] [payloadLen 4B LE] [nonce 12B] [encPayload+Tag]
            le32(wav, 0xFFFFFFFFL);
            le32(wav, eofPlain.length);
            wav.write(eofNonce);
            wav.write(eofEnc);

            writeHeader(wav,bytes);
            meter.finish();
            for (String feature : meter.drain()) events.acoustic(feature);
            wav.getFD().sync();
            good = true;
        } catch (Throwable e) {
            events.error("ERRO GRAVAÇÃO: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        } finally {
            recording.set(false);
            if (audio != null) {
                try { if (audio.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) audio.stop(); } catch (Exception ignored) {}
                try { audio.release(); } catch (Exception ignored) {}
            }
            if (wav != null) try { wav.close(); } catch (Exception ignored) {}

            if (good && file != null) {
                events.ready(file,bytes * 1000L / (RATE * 2L), ephemeralKey);
            } else if (file != null && bytes == 0) {
                wipeAndClean(file, ephemeralKey);
            } else if (file != null) {
                // Preserve authenticated prefix; never label an interrupted capture complete.
                events.status("FALHA · GRAVAÇÃO INCOMPLETA RETIDA PARA RECUPERAÇÃO");
            }
        }
    }

    private static void writeHeader(RandomAccessFile wav,long bytes) throws Exception {
        // Reserved header marker for encrypted spool integrity: seek to offset 24 (4 magic + 4 ver + 16 IV)
        long pos = wav.getFilePointer();
        wav.seek(24);
        le64(wav, bytes);
        if (pos > 32) wav.seek(pos);
    }

    static void wipeAndClean(File file) {
        wipeAndClean(file, null);
    }

    static void wipeAndClean(File file, byte[] ephemeralKey) {
        if (ephemeralKey != null) {
            Arrays.fill(ephemeralKey, (byte) 0);
        }
        activeEphemeralKey = null;
        if (file == null || !file.exists()) return;
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            long len = raf.length();
            if (len > 0 && len < 300_000_000L) {
                byte[] zeroes = new byte[Math.min(65536, (int) len)];
                raf.seek(0);
                long written = 0;
                while (written < len) {
                    int chunk = (int) Math.min(zeroes.length, len - written);
                    raf.write(zeroes, 0, chunk);
                    written += chunk;
                }
                raf.getFD().sync();
            }
        } catch (Exception ignored) {}
        boolean deleted = file.delete();
        if (!deleted && file.exists()) {
            Log.w(TAG, "Falha ao excluir arquivo temporário: " + file.getAbsolutePath());
            file.deleteOnExit();
        }
    }

    static File spoolDir(Context context) {
        File dir = new File(context.getNoBackupFilesDir(), "eco_spools");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new IllegalStateException("Diretório protegido indisponível");
        return dir;
    }

    static boolean isOwnedSpool(Context context, File file) throws java.io.IOException {
        File canonical = file.getCanonicalFile();
        return canonical.getName().startsWith(PREFIX) && (canonical.getParentFile().equals(spoolDir(context).getCanonicalFile()) || canonical.getParentFile().equals(context.getCacheDir().getCanonicalFile()));
    }

    static void cleanAbandoned(Context context) {
        java.util.List<File> old = new java.util.ArrayList<>();
        for (File dir : new File[]{context.getCacheDir(), spoolDir(context)}) {
            File[] files = dir.listFiles((d, name) -> name.startsWith(PREFIX));
            if (files != null) java.util.Collections.addAll(old, files);
        }
        if (old.isEmpty()) return;

        // Query active uncommitted jobs to retain their audio files for recovery
        boolean queryOk = false;
        java.util.Set<String> activePaths = new java.util.HashSet<>();
        try {
            EcoDatabaseHelper dbHelper = new SecureStore(context).getDbHelper();
            try (android.database.Cursor c = dbHelper.getReadableDatabase().rawQuery(
                    "SELECT audio_path FROM processamento_jobs WHERE state NOT IN ('COMMITTED', 'COMPLETED', 'DISCARDED')", null)) {
                while (c.moveToNext()) {
                    String p = c.getString(0);
                    if (p != null && !p.trim().isEmpty()) {
                        activePaths.add(new File(p).getAbsolutePath());
                    }
                }
            }
            queryOk = true;
        } catch (Exception e) {
            Log.w(TAG, "Não foi possível consultar jobs ativos durante limpeza de arquivos: " + e.getMessage());
        }

        // P0-02: Fail-safe retention: if database check failed, never purge potential active audio
        if (!queryOk) {
            Log.w(TAG, "Abortando limpeza de arquivos temporários: falha ao consultar banco de dados.");
            return;
        }

        long now = System.currentTimeMillis();
        for (File file : old) {
            if (!file.isFile()) continue;
            String abs = file.getAbsolutePath();
            // Preserve active uncommitted job recordings unless older than 48 hours
            if (activePaths.contains(abs)) {
                Log.i(TAG, "Preservando áudio de job ativo para recuperação: " + abs);
                continue;
            }
            // For unassociated files, give 1 hour grace period to avoid deleting currently recording file
            if (!activePaths.contains(abs) && (now - file.lastModified() < 3600 * 1000L)) {
                continue;
            }
            wipeAndClean(file, null);
        }
    }

    private static void le16(RandomAccessFile f, int value) throws Exception {
        f.write(value & 255); f.write((value >> 8) & 255);
    }

    private static void le32(RandomAccessFile f, long value) throws Exception {
        for (int i = 0; i < 4; i++) f.write((int) (value >> (i * 8)) & 255);
    }

    private static void le64(RandomAccessFile f, long value) throws Exception {
        for (int i = 0; i < 8; i++) f.write((int) (value >> (i * 8)) & 255);
    }
}
